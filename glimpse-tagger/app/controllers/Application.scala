package controllers

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await

import java.io._

import akka.actor.{ActorRef, Props, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import akka.util.ByteString
import akka.stream.scaladsl._
import akka.stream.Attributes

import play.api.mvc._
import play.api.Play.current
import play.api.libs.json._
import play.api.Configuration
import play.api.http.HttpEntity

import javax.inject._

import actors._

@Singleton
class Application @Inject()(system: ActorSystem, config: Configuration) extends Controller {

  val imgDir   = config.underlying.getString("application.img-dir")
  val tagsFile = config.underlying.getString("application.tags-file")
  val tagger   = system.actorOf(GlimpseTagger.props(imgDir, tagsFile))

  def img(id: Int) = Action { implicit req =>
    implicit val timeout = Timeout(1 second)
    val imgF = tagger ? GetImage(id)
    val img =  Await.result(imgF, timeout.duration).asInstanceOf[String]
    val path = "img/partial/" + img
    Ok(views.html.img(id, path))
  }

  def tag(id: Int, glimpses: String) = Action { implicit req =>
    val csv = Json.parse(glimpses)
    tagger ! TagImage(id, csv.as[Seq[Int]])
    Ok("Glimpse tags will be added !")
  }

  def configGenerator = Action { implicit req =>
    Ok(views.html.config())
  }

  def saveConfig(config: String) = Action { implicit req =>
    val entity = HttpEntity.Streamed(
      Source.fromIterator({() => Iterator(ByteString(config))}),
      Some(config.length()),
      None
    )
    Ok.sendEntity(entity).withHeaders(CONTENT_DISPOSITION->"attachment; filename=\"config.json\"")
  }

}