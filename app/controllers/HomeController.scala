package controllers

import javax.inject._
import models.core.RestrictedDownloads.FileId
import models.mongo.MongoDownloadFilesRepository
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class HomeController @Inject()(filesRepo:  MongoDownloadFilesRepository, cc: ControllerComponents) extends AbstractController(cc) {

  def index() = Action.async{ implicit request: Request[AnyContent] =>
    for {
      files <- filesRepo.listFiles
    } yield {
      Ok(views.html.index(files))
    }
  }

  def renderCodeForm(fileId: FileId) = Action{ implicit request: Request[AnyContent] =>
    Ok("")
  }
}
