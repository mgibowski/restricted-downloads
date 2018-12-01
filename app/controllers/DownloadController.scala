package controllers

import javax.inject._
import models.core.RestrictedDownloads.FileId
import play.api.data._
import play.api.data.Forms._
import play.api.mvc._

@Singleton
class DownloadController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  case class DownloadForm(code: String)
  val downloadForm = Form(mapping("code" -> nonEmptyText)(DownloadForm.apply)(DownloadForm.unapply))

  def download(fileId: FileId) = Action(parse.form(downloadForm)) { implicit request =>
    Ok(views.html.index())
  }
}
