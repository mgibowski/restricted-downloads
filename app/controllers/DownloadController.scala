package controllers

import javax.inject._
import models.core.RestrictedDownloads.FileId
import models.core.RestrictedDownloadsModule
import models.core.RestrictedDownloadsService._
import models.mongo.{MongoDownloadCodesRepository, MongoDownloadFilesRepository}
import play.api.data.Forms._
import play.api.data._
import play.api.mvc._
import scalaz.Scalaz._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class DownloadController @Inject()(val filesRepo: MongoDownloadFilesRepository,
                                   codesRepo: MongoDownloadCodesRepository)(cc: ControllerComponents)
  extends AbstractController(cc) {

  val coreLogic = new RestrictedDownloadsModule[Future](codesRepo, filesRepo)

  case class DownloadForm(code: String)
  val downloadForm = Form(mapping("code" -> nonEmptyText)(DownloadForm.apply)(DownloadForm.unapply))

  def download(fileId: FileId) = Action.async(parse.form(downloadForm)) { implicit request =>
    for {
      result <- coreLogic.downloadFile(fileId, request.body.code)
    } yield {
      result match {
        case Left(FileNotFound) => NotFound("The file you ask for does not exist")
        case Left(FileNotAvailableAnymore) => NotFound("File is no longer available")
        case Left(CodeNotCorrect) => BadRequest("The provided code is not correct")
        case Left(CodeLimitUsageExhausted) => BadRequest("The provided code has reached it's limit usage")
        case Right(f) =>
          Ok.sendFile(
            content = new java.io.File(f.resource),
            fileName = _ => f.name
          )
      }

    }
  }
}
