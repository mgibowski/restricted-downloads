package models

import scalaz._, Scalaz._
import simulacrum._
import java.time.LocalDateTime

object RestrictedDownloads {
  type DownloadCode = String
  type Resource = String
  type FileName = String
  type FileId = String
  case class DownloadableFile(id: FileId, name: FileName, resource: Resource, expiryDate: LocalDateTime)
}

import RestrictedDownloads._

object DownloadCodesRepository{
  sealed trait DownloadCodesRepositoryError
  case object DoesNotExist extends DownloadCodesRepositoryError
  case object UseLimitReached extends DownloadCodesRepositoryError
  sealed trait OK
  case object ResultOK extends OK
}

import DownloadCodesRepository._

trait DownloadCodesRepository[F[_]] {
  def isCodeValid(id: FileId, code: DownloadCode): F[Either[DownloadCodesRepositoryError,OK]]
}

object DownloadFilesRepository {
  type FileNotFound = String
}

import DownloadFilesRepository._

trait DownloadFilesRepository[F[_]] {
  def listFiles: F[Seq[DownloadableFile]]
  def getFile(id: FileId): F[Either[FileNotFound, DownloadableFile]]
}

object RestrictedDownloadsService {
  sealed trait Error {
    def en: String
  }
  case object FileNotFound extends Error {def en: String = "File not found"}
  case object FileNotAvailableAnymore extends Error {def en: String = "Too late, the file is not available anymore"}
  case object CodeNotCorrect extends Error {def en: String = "The code you provided is not correct"}
  case object CodeLimitUsageExhausted extends Error {def en: String = "The code you provided was already used"}
}

import RestrictedDownloadsService._

trait RestrictedDownloadsService[F[_]]{
  def downloadFile(id: FileId, downloadCode: DownloadCode): F[Either[Error, Resource]]
}

final class RestrictedDownloadsModule[F[_]: Monad](DCR: DownloadCodesRepository[F], DFR: DownloadFilesRepository[F])
  extends RestrictedDownloadsService[F] {

  private def getFile(id: FileId): F[Either[Error, Resource]] =
    for {
      file <- DFR.getFile(id)
      fileResult = file match {
        case Left(_) => Left(FileNotFound)
        case Right(f) if f.expiryDate.isBefore(LocalDateTime.now()) => Left(FileNotAvailableAnymore)
        case Right(f) => Right(f.resource)
      }
    } yield fileResult

  override def downloadFile(id: FileId, downloadCode: DownloadCode): F[Either[Error, Resource]] =
    for {
      codeState <- DCR.isCodeValid(id, downloadCode)
      file <- getFile(id)
    } yield {
      (codeState, file) match {
        case (Right(_), Right(r)) => Right(r)
        case (Left(DoesNotExist), _) => Left(CodeNotCorrect)
        case (Left(UseLimitReached), _) => Left(CodeLimitUsageExhausted)
        case (_, fileError) => fileError
      }
    }
}
