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

trait RestrictedDownloadsService[F[_]]{
  type Error = String
  def downloadFile(id: FileId, downloadCode: DownloadCode): F[Either[Error, Resource]]
}

final class RestrictedDownloadsModule[F[_]: Monad](DCR: DownloadCodesRepository[F], DFR: DownloadFilesRepository[F])
  extends RestrictedDownloadsService[F] {

  private def getFile(id: FileId): F[Either[Error, Resource]] =
    for {
      file <- DFR.getFile(id)
      fileResult = file match {
        case Left(_) => Left("File not found")
        case Right(f) if f.expiryDate.isAfter(LocalDateTime.now()) => Left("Too late, the file is not available anymore")
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
        case (Left(DoesNotExist), _) => Left("The code you provided is not correct")
        case (Left(UseLimitReached), _) => Left("The code you provided was already used")
        case (_, fileError) => fileError
      }
    }
}
