package controllers

import javax.inject._
import models.core.RestrictedDownloads.FileId
import models.core.RestrictedDownloadsModule
import models.core.RestrictedDownloadsService._
import models.mongo.{MongoDownloadCodesRepository, MongoDownloadFilesRepository}
import models.form.CodeForm._
import play.api.mvc._
import scalaz.Scalaz._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class DownloadController @Inject()(val filesRepo: MongoDownloadFilesRepository,
                                   codesRepo: MongoDownloadCodesRepository)(cc: ControllerComponents)
  extends AbstractController(cc) {

  val coreLogic = new RestrictedDownloadsModule[Future](codesRepo, filesRepo)

  def download(fileId: FileId) = Action.async(parse.form(downloadCodeForm)) { implicit request =>
    val code = request.body.code
    for {
      result <- coreLogic.downloadFile(fileId, code)
      _ <- codesRepo.bumpUseLimit(fileId, code)
    } yield {
      result match {
        case Left(FileNotFound) => NotFound(views.html.error("The file you ask for does not exist"))
        case Left(FileNotAvailableAnymore) => NotFound(views.html.error("File is no longer available"))
        case Left(CodeNotCorrect) => BadRequest(views.html.error("The provided code is not correct"))
        case Left(CodeLimitUsageExhausted) => BadRequest(views.html.error("The provided code has reached it's limit usage"))
        case Right(f) =>
          Ok.sendFile(
            content = new java.io.File(f.resource),
            fileName = _ => f.name,
            inline = false
          )
      }
    }
  }

}
