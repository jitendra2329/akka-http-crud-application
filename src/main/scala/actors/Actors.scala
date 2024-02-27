package actors

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import database.Database._
import models.Models.MobileForm

object Actors {

  implicit val actorSystem: ActorSystem = ActorSystem("system")

  object MobileDbActorMessages {
    case class CreateMobile(mobile: MobileForm)

    case class MobileCreated(id: Int)

    case object GetAllMobiles

    case class GetMobileById(id: Int)
    case class DeleteById(id:Int)
  }

  private class MobileDbActor extends Actor with ActorLogging {

    import MobileDbActorMessages._

    override def receive: Receive = {
      case CreateMobile(mobile) =>
        log.info("Inserting mobile data.")
        sender() ! MobileCreated(createNewMobile(mobile).head.id)
      case GetAllMobiles =>
        log.info("getting the mobile information.")
        sender() ! getMobiles
      case GetMobileById(id) =>
        log.info(s"getting the mobile information by id: $id")
        sender() ! getMobileById(id)
      case DeleteById(id) =>
        sender() ! deleteById(id)
    }
  }

  val mobileDbActor: ActorRef = actorSystem.actorOf(Props[MobileDbActor], "mobileDb")
}
