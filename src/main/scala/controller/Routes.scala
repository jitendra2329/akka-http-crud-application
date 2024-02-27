package controller

import actors.Actors.MobileDbActorMessages._
import actors.Actors.{actorSystem, mobileDbActor}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.pattern.ask
import akka.util.Timeout
import models.Models.{Mobile, MobileForm, MobileUpdateForm}
import spray.json._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

trait MobileJsonProtocol extends DefaultJsonProtocol {
  implicit val mobileFormat: RootJsonFormat[Mobile] = jsonFormat4(Mobile)
  implicit val mobileFormFormat: RootJsonFormat[MobileForm] = jsonFormat3(MobileForm)
  implicit val mobileUpdateFormFormat: RootJsonFormat[MobileUpdateForm] = jsonFormat1(MobileUpdateForm)
}

object Routes extends MobileJsonProtocol {

  import actorSystem.dispatcher

  implicit val defaultTimeout: Timeout = Timeout(2 seconds)

  private def getMobile(query: Uri.Query): Future[HttpResponse] = {
    val mobileId = query.get("id")

    mobileId match {
      case Some(idString) =>

        Try(idString.toInt) match {
          case Failure(ex) =>
            Future(
              HttpResponse(
                StatusCodes.NoContent,
                entity = HttpEntity(ContentTypes.`text/plain(UTF-8)`, ex.getCause.toString)
              )
            )

          case Success(id) =>

            val futureListMobile = (mobileDbActor ? GetMobileById(id)).mapTo[List[Mobile]]
            for {
              listMobile <- futureListMobile
            } yield {
              HttpResponse(
                StatusCodes.OK,
                entity = HttpEntity(
                  ContentTypes.`application/json`,
                  listMobile.toJson.prettyPrint
                )
              )
            }
        }

      case None => Future(HttpResponse(StatusCodes.NoContent))
    }
  }


  def deleteById(query: Uri.Query): Future[Option[String]] = {

    val mobileId = query.get("id")
    mobileId match {
      case Some(idString) =>

        Try(idString.toInt) match {
          case Failure(_) => Future(None)
          case Success(id) =>
            val futureOptionalString = (mobileDbActor ? DeleteById(id)).mapTo[Option[String]]
            for {
              optionalString <- futureOptionalString
            } yield optionalString match {
              case Some(string) => Some(string)
              case None => None
            }
        }

      case None => Future(None)
    }
  }

  def updateById(query: Uri.Query, mobileUpdateForm: MobileUpdateForm): Future[Option[String]] = {
    query.get("id") match {
      case Some(idString) =>

        Try(idString.toInt) match {
          case Failure(_) => Future(None)
          case Success(id) => (mobileDbActor ? UpdateById(id, mobileUpdateForm.price)).mapTo[Option[String]]
        }
      case None => Future(None)
    }
  }

  private val httpRequestHandler: HttpRequest => Future[HttpResponse] = {

    case HttpRequest(HttpMethods.POST, Uri.Path("/api/mobile"), _, entity, _) =>
      val strictFuture = entity.toStrict(2 seconds)
      strictFuture.flatMap { strictEntity =>
        val mobileJsonString = strictEntity.data.utf8String

        println(s"Received data from client: $mobileJsonString")

        val mobile = mobileJsonString.parseJson.convertTo[MobileForm]
        val futureMobileCreated = (mobileDbActor ? CreateMobile(mobile)).mapTo[MobileCreated]

        for {
          mobileCreated <- futureMobileCreated
        } yield {
          HttpResponse(
            StatusCodes.OK,
            entity = HttpEntity(
              ContentTypes.`text/plain(UTF-8)`,
              s"New mobile is added into the db with id: ${mobileCreated.id}"
            )
          )
        }
      }

    case HttpRequest(HttpMethods.GET, uri@Uri.Path("/api/mobile"), _, _, _) =>
      val query = uri.query()
      if (query.isEmpty) {
        val futureListAllOfMobiles = (mobileDbActor ? GetAllMobiles).mapTo[List[Mobile]]

        for {
          listOfAllMobiles <- futureListAllOfMobiles
        } yield HttpResponse(
          StatusCodes.OK,
          entity = HttpEntity(
            ContentTypes.`application/json`,
            listOfAllMobiles.toJson.prettyPrint
          )
        )
      } else {
        getMobile(query)
      }

    case HttpRequest(HttpMethods.DELETE, Uri.Path("/api/mobile/delete/all"), _, _, _) =>
      val futureOptionalString = (mobileDbActor ? DeleteAll).mapTo[Option[String]]

      for {
        optionalString <- futureOptionalString
      } yield {
        optionalString match {
          case Some(stringValue) =>
            HttpResponse(
              StatusCodes.OK,
              entity = HttpEntity(ContentTypes.`text/plain(UTF-8)`, stringValue))
          case None => HttpResponse(StatusCodes.NoContent)
        }
      }

    case HttpRequest(HttpMethods.DELETE, uri@Uri.Path("/api/mobile"), _, _, _) =>
      val query = uri.query()

      if (query.isEmpty) {
        Future(HttpResponse(
          StatusCodes.NoContent,
          entity = HttpEntity(
            ContentTypes.`text/plain(UTF-8)`,
            "Id is required to delete any particular record!"
          )))
      } else {
        for {
          optionalString <- deleteById(query)
        } yield {
          optionalString match {
            case Some(stringValue) =>
              HttpResponse(
                StatusCodes.OK,
                entity = HttpEntity(ContentTypes.`text/plain(UTF-8)`, stringValue)
              )
            case None => HttpResponse(StatusCodes.BadRequest)
          }
        }
      }

    case HttpRequest(HttpMethods.PUT, uri@Uri.Path("/api/mobile"), _, entity, _) =>

      val strictFuture = entity.toStrict(2 seconds)
      strictFuture.flatMap { strictEntity =>
        val mobileJsonString = strictEntity.data.utf8String

        println(s"Received data from client: $mobileJsonString")

        val mobile = mobileJsonString.parseJson.convertTo[MobileUpdateForm]

        val futureOptionalString = updateById(uri.query(), mobile)

        for {
          optionalString <- futureOptionalString
        } yield {
          HttpResponse(
            StatusCodes.OK,
            entity = HttpEntity(
              ContentTypes.`text/plain(UTF-8)`,
              s"${optionalString.get}"
            )
          )
        }
      }

    case _ => Future(HttpResponse(StatusCodes.NoContent))
  }

  val server: Future[Http.ServerBinding] = Http().newServerAt("localhost", 9000).bind(httpRequestHandler)
}
