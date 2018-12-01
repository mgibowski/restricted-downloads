package it.models.mongo

import java.time.LocalDateTime

import models.core.DownloadFilesRepository
import models.core.DownloadFilesRepository._
import models.core.RestrictedDownloads.DownloadableFile
import models.core.RestrictedDownloadsService.FileNotFound
import models.mongo.MongoDownloadFilesRepository
import models.mongo.MongoDownloadFilesRepository._
import org.scalatest.BeforeAndAfter
import org.scalatest.Matchers._
import play.api.test.Helpers._
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class MongoDownloadFilesRepositorySpec extends PlayWithMongoSpec with BeforeAndAfter {

  var files: Future[JSONCollection] = _

  before {
    //Init DB
    await {
      files = reactiveMongoApi.database.map(_.collection("download-files"))

      files.flatMap(_.insert[DownloadableFile](ordered = false).many(List(
        DownloadableFile(fileId = "file1", name = "file.zip", resource = "./file.zip",
          expiryDate = LocalDateTime.now().plusDays(3)),
      )))
    }
  }

  after {
    //clean DB
    files.flatMap(_.drop(failIfNotFound = false))
  }

  "MongoDownloadFilesRepository" should {
    "respond with error if asked for non existing file" in {
      val sut = app.injector.instanceOf(classOf[MongoDownloadFilesRepository])
      val f = sut.getFile("non-existing")
      val response = Await.result(f, Duration.Inf)
      response shouldBe Left("File not found")
    }
    "respond OK if file exists" in {
      val sut = app.injector.instanceOf(classOf[MongoDownloadFilesRepository])
      val f = sut.getFile("file1")
      val response = Await.result(f, Duration.Inf)
      response shouldBe 'right
    }
  }

  // TODO: listing files only that did not expire

}
