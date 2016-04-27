package actors

import java.io._

import akka.actor.{Actor, ActorLogging, Props}


sealed trait GlimpseTaggerProtocol
case class TagGlimpse(id: String, glimpses: Seq[Int]) extends GlimpseTaggerProtocol


class GlimpseTagger(outfile: String) extends Actor with ActorLogging {

  val pw = new PrintWriter(new FileWriter(outfile, true))

  def receive = {
    case TagGlimpse(id, glimpses) =>
      pw.append(id + "," + glimpses.mkString(",") + "\n")
      pw.flush()
  }

  override def postStop = pw.close()

}


object GlimpseTagger {

  def props(outfile: String) = Props(classOf[GlimpseTagger], outfile)

}