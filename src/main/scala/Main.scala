import actors.Actors.actorSystem.dispatcher
import actors.Actors.actorSystem
import controller.Routes

import scala.io.StdIn
object Main extends App {

  private val server = Routes.server
  println("Server is running on http://localhost:9000")
  StdIn.readLine()
  server
    .flatMap(_.unbind())
    .onComplete(_ => actorSystem.terminate())

}
