package it.models.mongo

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.modules.reactivemongo.ReactiveMongoApi

trait PlayWithMongoSpec extends PlaySpec with GuiceOneAppPerSuite {

  override def fakeApplication = new GuiceApplicationBuilder()
    .configure(
      "mongodb.uri" -> "mongodb://localhost:27017/restricted-downloads-test",
      "app.downloadCodeUseLimit" -> 2
    )
    .build()

  lazy val reactiveMongoApi: ReactiveMongoApi = app.injector.instanceOf[ReactiveMongoApi]

}
