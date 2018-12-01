package models.mongo

import scala.concurrent.ExecutionContext.Implicits.global
import java.time.LocalDateTime
import javax.inject.Inject
import models.core.{DownloadFilesRepository, RestrictedDownloads}
import models.core.DownloadFilesRepository._
import models.core.DownloadFilesRepository.FileNotFound
import models.core.RestrictedDownloads._
import play.api.Configuration
import play.modules.reactivemongo._
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json.collection.JSONCollection
import play.api.libs.json._
import reactivemongo.api.ReadPreference
import reactivemongo.play.json._

import scala.concurrent.Future

class MongoDownloadFilesRepository @Inject() (val reactiveMongoApi: ReactiveMongoApi, config: Configuration)
  extends DownloadFilesRepository[Future] with ReactiveMongoComponents{

  override def listFiles: Future[Seq[RestrictedDownloads.DownloadableFile]] = ???
  override def getFile(id: FileId): Future[Either[FileNotFound, RestrictedDownloads.DownloadableFile]] = ???

}

object MongoDownloadFilesRepository{
  implicit lazy val fmt: OFormat[DownloadableFile] = Json.format[DownloadableFile]
}
