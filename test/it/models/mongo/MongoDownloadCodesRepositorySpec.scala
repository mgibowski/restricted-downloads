package it.models.mongo

import models.core.DownloadCodesRepository
import models.core.DownloadCodesRepository._
import models.mongo.{DownloadCodeItem, MongoDownloadCodesRepository}
import org.scalatest.BeforeAndAfter
import org.scalatest.Matchers._
import play.api.test.Helpers._
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class MongoDownloadCodesRepositorySpec extends PlayWithMongoSpec with BeforeAndAfter {

  var codes: Future[JSONCollection] = _

  before {
    //Init DB
    await {
      codes = reactiveMongoApi.database.map(_.collection("download-codes"))

      codes.flatMap(_.insert[DownloadCodeItem](ordered = false).many(List(
        DownloadCodeItem(fileId = "file1", code = "file1code1"),
        DownloadCodeItem(fileId = "file1", code = "file1code2"),
        DownloadCodeItem(fileId = "file1", code = "file1code3"),
        DownloadCodeItem(fileId = "file2", code = "file1code1"),
      )))
    }
  }

  after {
    //clean DB
    codes.flatMap(_.drop(failIfNotFound = false))
  }

  "MongoDownloadCodesRepository" should {
    "respond with error when download code does not exist" in {
      val sut: MongoDownloadCodesRepository = app.injector.instanceOf(classOf[MongoDownloadCodesRepository])
      val f = sut.isCodeValid("file1", "not-valid")
      val response: Either[DownloadCodesRepository.DownloadCodesRepositoryError, DownloadCodesRepository.OK] = Await.result(f, Duration.Inf)
      response shouldBe Left(DoesNotExist)
    }

  }

}
