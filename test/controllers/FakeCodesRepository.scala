package controllers

import javax.inject.Singleton
import models.core.DownloadCodesRepository
import models.core.RestrictedDownloads.{DownloadCode, FileId}
import models.mongo.MongoDownloadCodesRepository

import scala.concurrent.Future

@Singleton
class FakeCodesRepository extends MongoDownloadCodesRepository(null, null) {
  var limit = 0

  import DownloadCodesRepository._

  override def isCodeValid(id: FileId, code: DownloadCode): Future[Either[DownloadCodesRepository.DownloadCodesRepositoryError, DownloadCodesRepository.OK]] = {
    val result: Either[DownloadCodesRepository.DownloadCodesRepositoryError, DownloadCodesRepository.OK] = (id, code) match {
      case (_, "correctCode") | (_, "codeWithoutFile") => Right(ResultOK)
      case (_, "usedCode") => Left(UseLimitReached)
      case _ => Left(DoesNotExist)
    }
    Future.successful(result)
  }

  // Note: in this case the limit is not per code, by global for the whole repository...
  override def bumpUseLimit(fileId: FileId, code: DownloadCode): Future[Unit] = Future.successful({limit += 1})

}
