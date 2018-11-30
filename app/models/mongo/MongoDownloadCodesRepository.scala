package models.mongo

import scala.concurrent.ExecutionContext.Implicits.global
import javax.inject.Inject
import models.core.DownloadCodesRepository
import models.core.DownloadCodesRepository._
import models.core.RestrictedDownloads.{DownloadCode, FileId}
import play.api.Configuration
import play.modules.reactivemongo._
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json.collection.JSONCollection
import play.api.libs.json._
import reactivemongo.api.ReadPreference
import reactivemongo.play.json._

import scala.concurrent.Future

class MongoDownloadCodesRepository @Inject() (val reactiveMongoApi: ReactiveMongoApi, config: Configuration)
  extends DownloadCodesRepository[Future] with ReactiveMongoComponents{

  lazy val codes: Future[JSONCollection] = reactiveMongoApi.database.map(_.collection("download-codes"))
  lazy val maxUse: Int = config.getOptional[Int]("app.downloadCodeUseLimit").getOrElse(3)

  override def isCodeValid(id: FileId, code: DownloadCode): Future[Either[DownloadCodesRepository.DownloadCodesRepositoryError, DownloadCodesRepository.OK]] =
    for {
      collection <- codes
      maybeCode <- collection.find[JsObject, DownloadCodeItem](Json.obj("fileId" -> id, "code" -> code))
        .cursor[DownloadCodeItem](ReadPreference.primary)
        .headOption
    } yield {
      maybeCode match {
        case None => Left(DoesNotExist)
        case Some(DownloadCodeItem(_, _, _, count)) if count >= maxUse => Left(UseLimitReached)
        case _ => Right(ResultOK)
      }
    }

}

case class DownloadCodeItem(_id: Option[BSONObjectID] = None, fileId: FileId, code: DownloadCode, useCount: Int)

object DownloadCodeItem{
  implicit val fmt: OFormat[DownloadCodeItem] = Json.format[DownloadCodeItem]
}
