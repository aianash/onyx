package utils

import scala.io.Source

class GlimpseConfig(conffile: String) {

  val config = Source.fromFile(conffile).mkString

  def get = config

}