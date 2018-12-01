package it.models.mongo

import java.time.LocalDateTime

import models.core.RestrictedDownloads.DownloadableFile
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
  val file1 = DownloadableFile(fileId = "file1", name = "file.zip", resource = "./file.zip",
    expiryDate = LocalDateTime.now().plusDays(3))
  val file2 = DownloadableFile(fileId = "file2", name = "file2.zip", resource = "./file2.zip",
    expiryDate = LocalDateTime.now().plusDays(4))
  val file3 = DownloadableFile(fileId = "file3", name = "file3.zip", resource = "./file3.zip",
    expiryDate = LocalDateTime.now().minusDays(1))

  before {
    //Init DB
    await {
      files = reactiveMongoApi.database.map(_.collection("download-files"))
      files.flatMap(_.insert[DownloadableFile](ordered = false).many(List(file1,file2,file3)))
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

  "MongoDownloadFilesRepository" should {
    "list only files that did not expire" in {
      val sut = app.injector.instanceOf(classOf[MongoDownloadFilesRepository])
      val f = sut.listFiles
      val response = Await.result(f, Duration.Inf)
      response should have length 2
      response should contain (file1)
      response should contain (file2)
    }
  }

}
