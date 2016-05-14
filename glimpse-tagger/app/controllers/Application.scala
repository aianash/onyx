package controllers

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.io.{Source => ScalaSource}
import scala.collection.immutable.HashMap

import java.net.URLEncoder
import java.nio.file.{Paths, Files}

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
import utils._

@Singleton
class Application @Inject()(system: ActorSystem, config: Configuration) extends Controller {

  val imgdir          = config.underlying.getString("application.img-dir")
  val glimpseoutfile  = config.underlying.getString("application.glimpse-outfile")
  val intentoutfile   = config.underlying.getString("application.intent-outfile")
  val glimpseconffile = config.underlying.getString("application.glimpse-conffile")
  val intentconffile  = config.underlying.getString("application.intent-conffile")

  val glimpseconf     = new GlimpseConfig(glimpseconffile)
  val intentconf      = new IntentConfig(intentconffile)

  val imageserver     = system.actorOf(ImageServer.props(imgdir))
  val glimpsetagger   = system.actorOf(GlimpseTagger.props(glimpseoutfile))
  val intenttagger    = system.actorOf(IntentTagger.props(intentoutfile))


  /**
   * Action to get image
   * @param {Int} id: Location of image in list
   */
  def imgForGlimpse(idx: Int) = Action { implicit req =>
    implicit val timeout = Timeout(1 second)
    val imgF = imageserver ? GetImage(idx)
    val img  =  Await.result(imgF, timeout.duration).asInstanceOf[String]
    val path = "img/partial/" + img

    val selectedglimpsesF = glimpsetagger ? GetGlimpse(img)
    val selectedglimpses = Await.result(selectedglimpsesF, timeout.duration).asInstanceOf[Seq[Int]]

    Ok(views.html.imgForGlimpse(idx, img, path, URLEncoder.encode(glimpseconf.get, "UTF-8"), selectedglimpses.mkString(",")))
  }

  /**
   * Action to save glimpse tags for given image
   * @param {String} glimpses: glimpse tags, json string of 1s and 0s
   */
  def tagGlimpse(id: String, glimpses: String) = Action { implicit req =>
    val glimpsejson = Json.parse(glimpses)
    glimpsetagger ! TagGlimpse(id, glimpsejson.as[Seq[Int]])
    Ok("Glimpse tags will be added !")
  }

  /**
   * Action to random get image for intent tagging
   */
  def imgForIntent = Action { implicit req =>
    implicit val timeout = Timeout(1 second)
    val imgF = imageserver ? GetNextImage
    val img  =  Await.result(imgF, timeout.duration).asInstanceOf[String]
    val path = "img/partial/" + img
    val intent = intentconf.nextRandomIntent

    Ok(views.html.imgForIntent(img, path, intent))
  }

  /**
   * Action to tag intents for a given image
   * @param  {String} id: Id of image
   * @param  {String} intent: Intent to be added
   */
  def tagIntent(id: String, intent: String, resp: String) = Action { implicit req =>
    intenttagger ! TagIntent(id, intent, resp)
    Ok("Successfully tagged.")
  }

  /**
   * Action to display config generator page
   */
  def configGenerator = Action { implicit req =>
    Ok(views.html.config())
  }

  /**
   * Action to save glimpse config
   * @param  {String} config: Glimpse config to save
   */
  def saveConfig(config: String) = Action { implicit req =>
    val entity = HttpEntity.Streamed(
      Source.fromIterator({() => Iterator(ByteString(config))}),
      Some(config.length()),
      None
    )
    Ok.sendEntity(entity).withHeaders(CONTENT_DISPOSITION->"attachment; filename=\"config.json\"")
  }

}