package controllers

import java.io.File
import java.time.LocalDateTime

import it.models.mongo.PlayWithMongoSpec
import models.core.RestrictedDownloads.DownloadableFile
import models.mongo.DownloadCodeItem
import models.mongo.MongoDownloadFilesRepository._
import org.scalatest.BeforeAndAfter
import play.api.test.Helpers._
import play.api.test._
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

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
        DownloadCodeItem(fileId = "file1", code = "correctCode", useCount = 1)
      )))

      files.flatMap(_.insert[DownloadableFile](ordered = false).many(List(
        DownloadableFile(fileId = "file1", name = fileName, resource = localFileFullPath,
        expiryDate = LocalDateTime.now().plusDays(3))
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

  }
}
