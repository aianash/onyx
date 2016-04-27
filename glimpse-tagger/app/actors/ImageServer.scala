package actors

import java.io.File

import akka.actor.{Actor, ActorLogging, Props}


sealed trait ImageServerProtocol
case class GetImage(idx: Int) extends ImageServerProtocol


class ImageServer(imgdir: String) extends Actor with ActorLogging {

  val dir  = new File(imgdir)
  val imgs = dir.listFiles.filter(_.isFile).toList

  def receive = {
    case GetImage(idx) =>
      val img = if(idx > 0 && idx < imgs.length) imgs(idx) else imgs(0)
      sender() ! img.getName
  }

}


object ImageServer {

  def props(imgdir: String) = Props(classOf[ImageServer], imgdir)

}