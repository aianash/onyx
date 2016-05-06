package controllers

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.io.{Source => ScalaSource}
import scala.collection.immutable.HashMap

import java.net.URLEncoder

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

  val imgdir          = config.underlying.getString("application.img-dir")
  val glimpseoutfile  = config.underlying.getString("application.glimpse-outfile")
  val intentoutfile   = config.underlying.getString("application.intent-outfile")
  val glimpseconffile = config.underlying.getString("application.glimpse-conffile")
  val intentconffile  = config.underlying.getString("application.intent-conffile")

  val glimpseconf     = ScalaSource.fromFile(glimpseconffile).mkString
  val intentsjson     = Json.parse(ScalaSource.fromFile(intentconffile).mkString)

  val imageserver     = system.actorOf(ImageServer.props(imgdir))
  val glimpsetagger   = system.actorOf(GlimpseTagger.props(glimpseoutfile))
  val intenttagger    = system.actorOf(IntentTagger.props(intentoutfile))

  val glimpsetags     = readGlimpseTagsToMap(glimpseoutfile)
  val intenttags      = readIntentTagsToMap(intentoutfile)

  /**
   * Action to get image
   * @param {Int} id: Location of image in list
   */
  def img(idx: Int) = Action { implicit req =>
    implicit val timeout = Timeout(1 second)
    val imgF = imageserver ? GetImage(idx)
    val img  =  Await.result(imgF, timeout.duration).asInstanceOf[String]
    val path = "img/partial/" + img

    val intentsconf = intentsjson.as[Array[Array[String]]]

    val selectedglimpses = glimpsetags.getOrElse(img, Array.empty[String]).mkString(",")
    val selectedintents  = intenttags.getOrElse(img, Array.empty[String])

    Ok(views.html.img(idx, img, path, URLEncoder.encode(glimpseconf, "UTF-8"), intentsconf, selectedglimpses, selectedintents))
  }

  /**
   * Action to save glimpse tags and intents for given image
   * @param {String} id: id of the image
   * @param {String} glimpses: glimpse tags, json string of 1s and 0s
   */
  def tag(id: String, glimpses: String, intents: String) = Action { implicit req =>
    val glimpsejson = Json.parse(glimpses)
    val intentjson = Json.parse(intents)

    glimpsetagger ! TagGlimpse(id, glimpsejson.as[Seq[Int]])
    intenttagger ! TagIntent(id, intentjson.as[Seq[String]])

    Ok("Glimpse tags will be added !")
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

  private def readGlimpseTagsToMap(file: String) =
    ScalaSource.fromFile(file).getLines.toList.foldLeft(HashMap.empty[String, Array[String]])((map, line) => {
      val els = line.split(",")
      map + ((els.head, els.tail))
    })

  private def readIntentTagsToMap(file: String) =
    ScalaSource.fromFile(file).getLines.toList.foldLeft(HashMap.empty[String, Array[String]])((map, line) => {
      val els = line.split(",")
      if(map.contains(els.head)) map + ((els.head, els.tail.mkString +: map.get(els.head).get))
      else map + ((els.head, els.tail))
    })

}