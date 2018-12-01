package controllers

import java.io.File
import java.time.LocalDateTime

import it.models.mongo.PlayWithMongoSpec
import models.core.RestrictedDownloads.DownloadableFile
import models.mongo.DownloadCodeItem
import models.mongo.MongoDownloadFilesRepository._
import org.scalatest.BeforeAndAfter
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers._
import play.api.test._
import reactivemongo.api.ReadPreference
import reactivemongo.play.json._
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class DownloadControllerSpec extends PlayWithMongoSpec with Injecting with BeforeAndAfter {

  var codes: Future[JSONCollection] = _
  var files: Future[JSONCollection] = _
  val fileName = "restricted-test-file1.txt"
  val localFileFullPath = s"/tmp/$fileName"

  before {
    //Init DB
    await {
      codes = reactiveMongoApi.database.map(_.collection("download-codes"))
      files = reactiveMongoApi.database.map(_.collection("download-files"))

      codes.flatMap(_.insert[DownloadCodeItem](ordered = false).many(List(
        DownloadCodeItem(fileId = "file1", code = "correctCode", useCount = 1),
        DownloadCodeItem(fileId = "file1", code = "correctCode2", useCount = 1),
        DownloadCodeItem(fileId = "file1", code = "usedCode", useCount = 5),
        DownloadCodeItem(fileId = "file2", code = "correctCode", useCount = 1),
        DownloadCodeItem(fileId = "xyz", code = "codeWithoutFile", useCount = 1)
      )))

      files.flatMap(_.insert[DownloadableFile](ordered = false).many(List(
        DownloadableFile(fileId = "file1", name = fileName, resource = localFileFullPath,
        expiryDate = LocalDateTime.now().plusDays(3)),
        DownloadableFile(fileId = "file2", name = fileName, resource = localFileFullPath,
        expiryDate = LocalDateTime.now().minusDays(1)),
      )))
    }

    // Create local file
    import java.io._
    val pw = new PrintWriter(new File(localFileFullPath))
    pw.write("File has some contents")
    pw.close()
  }

  after {
    //clean DB
    codes.flatMap(_.drop(failIfNotFound = false))
    files.flatMap(_.drop(failIfNotFound = false))

    // Delete local file
    new File(localFileFullPath).delete()
  }

  "DownloadController" should {

    "respond with a file given correct fileId and download code" in {
      val request = FakeRequest(POST, "/download/file1").withFormUrlEncodedBody("code" -> "correctCode")
      val result = route(app, request).get

      status(result) mustBe OK
      header("Content-Disposition", result).get must include (fileName)
    }

    "increment code use counter after correctly downloading file" in {
      val request = FakeRequest(POST, "/download/file1").withFormUrlEncodedBody("code" -> "correctCode2")
      val result = route(app, request).get

      val f = for {
        _ <- result
        collection <- codes
        code <- collection.find[JsObject, DownloadCodeItem](
          Json.obj("fileId" -> "file1", "code" -> "correctCode2"))
          .cursor[DownloadCodeItem](ReadPreference.primary)
          .headOption
      } yield code

      status(result) mustBe OK
      val code = Await.result(f, Duration.Inf)
      code.get.useCount mustBe 2
    }

    "respond with error when file does not exist" in {
      val request = FakeRequest(POST, "/download/xyz").withFormUrlEncodedBody("code" -> "codeWithoutFile")
      val result = route(app, request).get

      status(result) mustBe NOT_FOUND
      contentAsString(result) must include ("The file you ask for does not exist")
    }

    "respond with error when file is no longer available" in {
      val request = FakeRequest(POST, "/download/file2").withFormUrlEncodedBody("code" -> "correctCode")
      val result = route(app, request).get

      status(result) mustBe NOT_FOUND
      contentAsString(result) must include ("File is no longer available")
    }

    "respond with error when provided code is not correct" in {
      val request = FakeRequest(POST, "/download/file1").withFormUrlEncodedBody("code" -> "wrongCode")
      val result = route(app, request).get

      status(result) mustBe BAD_REQUEST
      contentAsString(result) must include ("The provided code is not correct")
    }

    "respond with error when code use limit is reached" in {
      val request = FakeRequest(POST, "/download/file1").withFormUrlEncodedBody("code" -> "usedCode")
      val result = route(app, request).get

      status(result) mustBe BAD_REQUEST
      contentAsString(result) must include ("The provided code has reached it's limit usage")
    }

  }
}
