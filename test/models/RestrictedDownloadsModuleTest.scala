package models

import java.time.LocalDateTime

import models.DownloadFilesRepository.FileNotFound
import models.RestrictedDownloads.{DownloadCode, DownloadableFile, FileId}
import org.scalatest.Matchers._
import org.scalatest._
import scalaz.Scalaz._
import scalaz.{Scalaz, _}

class RestrictedDownloadsModuleTest extends FlatSpec {

  val testCodesRepo = new DownloadCodesRepository[Id] {
    override def isCodeValid(id: FileId, code: DownloadCode): Scalaz.Id[Either[DownloadCodesRepository.DownloadCodesRepositoryError, DownloadCodesRepository.OK]] =
      (id, code) match {
        case (_, "DoesNotExist") => Left(DownloadCodesRepository.DoesNotExist)
        case (_, "UseLimitReached") => Left(DownloadCodesRepository.UseLimitReached)
        case _ => Right(DownloadCodesRepository.ResultOK)
      }
  }

  def testFileRepo(expiryDate: LocalDateTime) = new DownloadFilesRepository[Id] {
    override def listFiles: Scalaz.Id[Seq[RestrictedDownloads.DownloadableFile]] = ???

    override def getFile(id: FileId): Scalaz.Id[Either[FileNotFound, RestrictedDownloads.DownloadableFile]] =
      id match {
        case "NotFound" => Left("File not found")
        case _ => Right(DownloadableFile(id, "Name", "Resource", expiryDate))
      }
  }

  behavior of "RestrictedDownloadsModule"

  it should "not allow to download expired file" in {
    // Given
    val expiredDate = LocalDateTime.now().minusDays(3)
    val sut = new RestrictedDownloadsModule[Id](testCodesRepo, testFileRepo(expiredDate))

    // When
    val downloadResult = sut.downloadFile("ok-id", "ok-code")

    // Then
    downloadResult shouldBe Left("Too late, the file is not available anymore")
  }

}
