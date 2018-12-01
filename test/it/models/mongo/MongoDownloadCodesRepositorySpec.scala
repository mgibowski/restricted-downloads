package it.models.mongo

import models.core.DownloadCodesRepository
import models.core.DownloadCodesRepository._
import models.mongo.{DownloadCodeItem, MongoDownloadCodesRepository}
import org.scalatest.BeforeAndAfter
import org.scalatest.Matchers._
import play.api.libs.json.JsObject
import play.api.test.Helpers._
import reactivemongo.play.json.collection.JSONCollection
import play.api.libs.json._
import reactivemongo.api.ReadPreference
import reactivemongo.play.json._

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
        DownloadCodeItem(fileId = "file1", code = "file1code1", useCount = 1),
        DownloadCodeItem(fileId = "file1", code = "file1code2", useCount = 2),
        DownloadCodeItem(fileId = "file1", code = "limitExhausted", useCount = 3),
        DownloadCodeItem(fileId = "file2", code = "file1code1", useCount = 1),
      )))
    }
  }

  after {
    //clean DB
    codes.flatMap(_.drop(failIfNotFound = false))
  }

  "MongoDownloadCodesRepository isCodeValid" should {
    "respond with error when download code does not exist" in {
      val sut: MongoDownloadCodesRepository = app.injector.instanceOf(classOf[MongoDownloadCodesRepository])
      val f = sut.isCodeValid("file1", "not-valid")
      val response: Either[DownloadCodesRepository.DownloadCodesRepositoryError, DownloadCodesRepository.OK] = Await.result(f, Duration.Inf)
      response shouldBe Left(DoesNotExist)
    }
    "respond with error when code use limit is exhausted" in {
      val sut: MongoDownloadCodesRepository = app.injector.instanceOf(classOf[MongoDownloadCodesRepository])
      val f = sut.isCodeValid("file1", "limitExhausted")
      val response: Either[DownloadCodesRepository.DownloadCodesRepositoryError, DownloadCodesRepository.OK] = Await.result(f, Duration.Inf)
      response shouldBe Left(UseLimitReached)
    }
    "respond OK when all is fine" in {
      val sut: MongoDownloadCodesRepository = app.injector.instanceOf(classOf[MongoDownloadCodesRepository])
      val f = sut.isCodeValid("file1", "file1code1")
      val response: Either[DownloadCodesRepository.DownloadCodesRepositoryError, DownloadCodesRepository.OK] = Await.result(f, Duration.Inf)
      response shouldBe 'right
    }
  }

  "MongoDownloadCodesRepository bumpUseLimit" should {
    "increase use limit for a given code" in {
      val sut: MongoDownloadCodesRepository = app.injector.instanceOf(classOf[MongoDownloadCodesRepository])

      val f = for {
        _ <- sut.bumpUseLimit("file1", "file1code1")
        collection <- codes
        code <- collection
          .find[JsObject, DownloadCodeItem](Json.obj("fileId" -> "file1", "code" -> "file1code1"))
          .cursor[DownloadCodeItem](ReadPreference.primary)
          .headOption
      } yield code

      val code = Await.result(f, Duration.Inf)
      code.get.useCount shouldBe 2
    }
  }

}
