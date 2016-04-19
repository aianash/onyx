package controllers

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await

import java.io.File

import akka.actor.{ActorRef, Props, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout

import play.api.mvc._
import play.api.Play.current
import play.api.libs.json._
import play.api.Configuration

import javax.inject._

import actors._

@Singleton
class Application @Inject()(system: ActorSystem, config: Configuration) extends Controller {

  val imgDir = config.underlying.getString("application.img-dir")
  val tagger = system.actorOf(GlimpseTagger.props(imgDir))

  def img(id: Option[Int]) = Action { implicit req =>
    id match {
      case Some(id) =>
        implicit val timeout = Timeout(1 second)
        val imgF = tagger ? GetImage(id)
        val img =  Await.result(imgF, timeout.duration).asInstanceOf[String]
        val path = "img/partial/" + img
        Ok(views.html.img(id, path))

      case None => Ok("Nothing here")
    }
  }

}