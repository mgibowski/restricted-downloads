package models.mongo

import javax.inject.Inject
import models.core.DownloadCodesRepository
import models.core.RestrictedDownloads.{DownloadCode, FileId}
import play.api.Configuration
import play.modules.reactivemongo._
import reactivemongo.bson.BSONObjectID

import scala.concurrent.Future

class MongoDownloadCodesRepository @Inject() (val reactiveMongoApi: ReactiveMongoApi, config: Configuration)
  extends DownloadCodesRepository[Future] with ReactiveMongoComponents{

  override def isCodeValid(id: FileId, code: DownloadCode): Future[Either[DownloadCodesRepository.DownloadCodesRepositoryError, DownloadCodesRepository.OK]] = ???

}

case class DownloadCodeItem(_id: Option[BSONObjectID] = None, fileId: FileId, code: DownloadCode, useCount: Int)

object DownloadCodeItem{
  import play.api.libs.json._
  import reactivemongo.play.json._

  implicit val fmt: OFormat[DownloadCodeItem] = Json.format[DownloadCodeItem]
}
