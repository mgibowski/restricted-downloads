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

  lazy val files: Future[JSONCollection] = reactiveMongoApi.database.map(_.collection("download-files"))
  import MongoDownloadFilesRepository.fmt

  override def listFiles: Future[Seq[RestrictedDownloads.DownloadableFile]] = ???

  override def getFile(id: FileId): Future[Either[FileNotFound, RestrictedDownloads.DownloadableFile]] =
    for {
      collection <- files
      maybeFile <- collection
        .find[JsObject, DownloadableFile](Json.obj("fileId" -> id))
          .cursor[DownloadableFile](ReadPreference.primary)
          .headOption
    } yield {
      maybeFile match {
        case Some(f) => Right(f)
        case _ => Left("File not found")
      }
    }

}

object MongoDownloadFilesRepository{
  implicit lazy val fmt: OFormat[DownloadableFile] = Json.format[DownloadableFile]
}
