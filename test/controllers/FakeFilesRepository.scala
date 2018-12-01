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
  val afterThreeDays: LocalDateTime = LocalDateTime.now().plusDays(3)
  val yesterday: LocalDateTime = LocalDateTime.now().minusDays(1)
  val defaultFiles: Seq[DownloadableFile] = Seq(
    DownloadableFile(fileId = "file1", title = "file1", name = fileName, resource = localFileFullPath, expiryDate = afterThreeDays),
    DownloadableFile(fileId = "file3", title = "file3", name = fileName, resource = localFileFullPath, expiryDate = afterThreeDays),
    DownloadableFile(fileId = "file2", title = "file2", name = fileName, resource = localFileFullPath, expiryDate = yesterday)
  )
}

class FakeFilesRepository(_files: Seq[DownloadableFile] = FakeFilesRepository.defaultFiles) extends MongoDownloadFilesRepository(null, null) {

  override def listFiles: Future[Seq[RestrictedDownloads.DownloadableFile]] = Future.successful(_files)

  override def getFile(id: FileId): Future[Either[FileNotFound, RestrictedDownloads.DownloadableFile]] = {
    val result = _files.find(_.fileId == id).map(Right(_)).getOrElse(Left("File not found"))
    Future.successful(result)
  }
}

class FakeFilesRepositoryComponent extends FakeFilesRepository
