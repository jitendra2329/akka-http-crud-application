package services

import actors.Actors.MobileDbActorMessages._
import actors.Actors.{actorSystem, mobileDbActor}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.pattern.ask
import akka.util.Timeout
import models.Models.{Mobile, MobileForm}
import spray.json._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.io.StdIn
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

trait MobileJsonProtocol extends DefaultJsonProtocol {
  implicit val mobileFormat: RootJsonFormat[Mobile] = jsonFormat4(Mobile)
  implicit val mobileFormFormat: RootJsonFormat[MobileForm] = jsonFormat3(MobileForm)
}

object Main extends App with MobileJsonProtocol {

  import actorSystem.dispatcher

  implicit val defaultTimeout: Timeout = Timeout(2 seconds)

  println("Inside main ")

  private def getMobile(query: Uri.Query): Future[HttpResponse] = {
    val mobileId = query.get("id")

    mobileId match {
      case Some(id) =>
        Try(id.toInt) match {
          case Failure(ex) =>
            Future(
              HttpResponse(
                StatusCodes.NoContent,
                entity = HttpEntity(ContentTypes.`text/plain(UTF-8)`, ex.getCause.toString)
              )
            )
          case Success(id) =>
            val mobileFuture = (mobileDbActor ? GetMobileById(id)).mapTo[List[Mobile]]
            for {
              mob <- mobileFuture
            } yield {
              HttpResponse(
                StatusCodes.OK,
                entity = HttpEntity(
                  ContentTypes.`application/json`,
                  mob.toJson.prettyPrint
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
      case Some(value) =>
        Try(value.toInt) match {
          case Failure(_) => Future(None)
          case Success(value) =>
            val result = (mobileDbActor ? DeleteById(value)).mapTo[Option[String]]
            for {
              res <- result
            } yield res match {
              case Some(value) => Some(value)
              case None => None
            }
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
        val mobileCreated = (mobileDbActor ? CreateMobile(mobile)).mapTo[MobileCreated]
        for {
          mob <- mobileCreated
        } yield {
          HttpResponse(
            StatusCodes.OK,
            entity = HttpEntity(
              ContentTypes.`text/plain(UTF-8)`,
              s"New mobile is added into the db with id: ${mob.id}"
            )
          )
        }
      }
    case HttpRequest(HttpMethods.GET, uri@Uri.Path("/api/mobile"), _, _, _) =>
      val query = uri.query()
      if (query.isEmpty) {
        val allMobiles = (mobileDbActor ? GetAllMobiles).mapTo[List[Mobile]]
        for {
          mob <- allMobiles
        } yield HttpResponse(
          StatusCodes.OK,
          entity = HttpEntity(
            ContentTypes.`application/json`,
            mob.toJson.prettyPrint
          )
        )
      } else {
        getMobile(query)
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
          value <- deleteById(query)
        } yield {
          value match {
            case Some(value) =>
              HttpResponse(
                StatusCodes.OK,
                entity = HttpEntity(ContentTypes.`text/plain(UTF-8)`, value)
              )
            case None => HttpResponse(StatusCodes.BadRequest)
          }
        }
      }
    case _ => Future(HttpResponse(StatusCodes.BadRequest))
  }

  println("Above the server start")
  val res = Http().newServerAt("localhost", 9000).bind(httpRequestHandler)
  println("Server is running on http://localhost:9000")

  StdIn.readLine()

}
