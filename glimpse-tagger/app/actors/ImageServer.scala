package actors

import scala.util.Random

import java.io.File

import akka.actor.{Actor, ActorLogging, Props}


sealed trait ImageServerProtocol
case class GetImage(idx: Int) extends ImageServerProtocol
case object GetRandomImage extends ImageServerProtocol
case object GetNextImage extends ImageServerProtocol


class ImageServer(imgdir: String) extends Actor with ActorLogging {

  val dir  = new File(imgdir)
  val imgs = dir.listFiles.filter(_.isFile).toList
  val iter = Iterator.continually(imgs).flatten
  val rg   = new Random

  def receive = {
    case GetImage(idx) =>
      val img = if(idx > 0 && idx < imgs.length) imgs(idx) else imgs(0)
      sender() ! img.getName

    case GetRandomImage =>
      val idx = rg.nextInt(imgs.size)
      val img = imgs(idx)
      sender() ! img.getName

    case GetNextImage =>
      sender() ! iter.next.getName
  }

}


object ImageServer {

  def props(imgdir: String) = Props(classOf[ImageServer], imgdir)

}