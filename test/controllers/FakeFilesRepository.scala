package controllers

import java.time.LocalDateTime

import models.core.DownloadFilesRepository.FileNotFound
import models.core.RestrictedDownloads
import models.core.RestrictedDownloads.{DownloadableFile, FileId}
import models.mongo.MongoDownloadFilesRepository

import scala.concurrent.Future

object FakeFilesRepository {
  val fileName = "restricted-test-file1.txt"
  val localFileFullPath = s"/tmp/$fileName"
}

import controllers.FakeFilesRepository._

class FakeFilesRepository extends MongoDownloadFilesRepository(null, null) {

  val afterThreeDays: LocalDateTime = LocalDateTime.now().plusDays(3)
  val yesterday: LocalDateTime = LocalDateTime.now().minusDays(1)

  private val _files = Seq(
    DownloadableFile(fileId = "file1", name = fileName, resource = localFileFullPath, expiryDate = afterThreeDays),
    DownloadableFile(fileId = "file2", name = fileName, resource = localFileFullPath, expiryDate = yesterday)
  )

  override def listFiles: Future[Seq[RestrictedDownloads.DownloadableFile]] = ???

  override def getFile(id: FileId): Future[Either[FileNotFound, RestrictedDownloads.DownloadableFile]] = {
    val result = _files.find(_.fileId == id).map(Right(_)).getOrElse(Left("File not found"))
    Future.successful(result)
  }
}
