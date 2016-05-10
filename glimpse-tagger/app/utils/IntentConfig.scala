package utils

import scala.io.Source
import scala.util.Random

import play.api.libs.json._

class IntentConfig(conffile: String) {

  val configjson = Json.parse(Source.fromFile(conffile).mkString)
  val config     = configjson.as[Seq[String]]
  val rg         = new Random

  def nextRandomIntent = config(rg.nextInt(config.size))

}