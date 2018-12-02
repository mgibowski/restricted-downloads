package controllers

import java.io.File

import controllers.FakeFilesRepository._
import models.mongo.{MongoDownloadCodesRepository, MongoDownloadFilesRepository}
import org.scalatest.BeforeAndAfter
import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import play.api.test._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class DownloadControllerSpec extends PlaySpec with GuiceOneAppPerTest with Injecting with BeforeAndAfter {

  before {
    // Create local file
    import java.io._
    val pw = new PrintWriter(new File(localFileFullPath))
    pw.write("File has some contents")
    pw.close()
  }

  after {
    // Delete local file
    new File(localFileFullPath).delete()
  }

  override def fakeApplication = new GuiceApplicationBuilder()
    .overrides(bind[MongoDownloadFilesRepository].to[FakeFilesRepositoryComponent])
    .overrides(bind[MongoDownloadCodesRepository].to[FakeCodesRepository])
    .disable[play.modules.reactivemongo.ReactiveMongoModule]
    .build()

  "DownloadController" should {

    "respond with a file given correct fileId and download code" in {
      val request = FakeRequest(POST, "/download/file1").withFormUrlEncodedBody("code" -> "correctCode")
      val result = route(app, request).get

      status(result) mustBe OK
      header("Content-Disposition", result).get must include (fileName)
    }

    "increment code use counter after correctly downloading file" in {
      // Given
      val codesRepo = inject[FakeCodesRepository]
      codesRepo.limit = 0

      // When
      val request = FakeRequest(POST, "/download/file1").withFormUrlEncodedBody("code" -> "correctCode")
      val result = route(app, request).get
      status(result) mustBe OK
      Await.result(result, Duration.Inf)

      // Then
      codesRepo.limit mustBe 1
    }

    "respond with error when file does not exist" in {
      val request = FakeRequest(POST, "/download/xyz").withFormUrlEncodedBody("code" -> "codeWithoutFile")
      val result = route(app, request).get

      contentAsString(result) must include ("The file you ask for does not exist")
      status(result) mustBe NOT_FOUND
    }

    "respond with error when file is no longer available" in {
      val request = FakeRequest(POST, "/download/file2").withFormUrlEncodedBody("code" -> "correctCode")
      val result = route(app, request).get

      contentAsString(result) must include ("File is no longer available")
      status(result) mustBe NOT_FOUND
    }

    "respond with error when code use limit is reached" in {
      val request = FakeRequest(POST, "/download/file1").withFormUrlEncodedBody("code" -> "usedCode")
      val result = route(app, request).get

      contentAsString(result) must include ("The provided code has reached")
      status(result) mustBe BAD_REQUEST
    }

  }
}
