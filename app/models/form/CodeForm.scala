package models.form

import play.api.data.Forms._
import play.api.data._

case class CodeForm(code: String)

object CodeForm{
  val downloadCodeForm = Form(mapping("code" -> nonEmptyText)(CodeForm.apply)(CodeForm.unapply))
}
