package models

import java.time.LocalDateTime

import models.DownloadFilesRepository.FileNotFound
import models.RestrictedDownloads.{DownloadCode, DownloadableFile, FileId}
import models.RestrictedDownloadsService._
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

  def testFileRepo(expiryDate: LocalDateTime = LocalDateTime.now().plusDays(1)) = new DownloadFilesRepository[Id] {
    override def listFiles: Scalaz.Id[Seq[RestrictedDownloads.DownloadableFile]] = ???

    override def getFile(id: FileId): Scalaz.Id[Either[FileNotFound, RestrictedDownloads.DownloadableFile]] =
      id match {
        case "NotFound" => Left("File not found")
        case _ => Right(DownloadableFile(id, "Name", "Resource", expiryDate))
      }
  }

  val sut = new RestrictedDownloadsModule[Id](testCodesRepo, testFileRepo())

  behavior of "RestrictedDownloadsModule"

  it should "not allow to download expired file" in {
    // Given
    val expiredDate = LocalDateTime.now().minusDays(3)
    val sut = new RestrictedDownloadsModule[Id](testCodesRepo, testFileRepo(expiredDate))
    // When
    val downloadResult = sut.downloadFile("ok-id", "ok-code")
    // Then
    downloadResult shouldBe Left(FileNotAvailableAnymore)
  }

  it should "not allow to download a file that does not exist" in {
    val result = sut.downloadFile("NotFound", "ok-code")
    result shouldBe Left(FileNotFound)
  }

  it should "not allow to download a file with the wrong code" in {
    val result = sut.downloadFile("correct-id", "DoesNotExist")
    result shouldBe Left(CodeNotCorrect)
  }

  it should "not allow to download a file with the exhausted code" in {
    val result = sut.downloadFile("correct-id", "UseLimitReached")
    result shouldBe Left(CodeLimitUsageExhausted)
  }

  it should "allow to download a file when everything is OK" in {
    val result = sut.downloadFile("Ok", "Ok")
    result shouldBe 'right
  }

}
