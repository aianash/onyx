package actors

import scala.io.Source
import scala.collection.mutable.HashMap

import java.io._
import java.nio.file.{Paths, Files}

import akka.actor.{Actor, ActorLogging, Props}


sealed trait GlimpseTaggerProtocol
case class TagGlimpse(id: String, glimpses: Seq[Int]) extends GlimpseTaggerProtocol
case class GetGlimpse(id: String) extends GlimpseTaggerProtocol


class GlimpseTagger(outfile: String) extends Actor with ActorLogging {

  val glimpsetags = readGlimpseTagsToMap(outfile)

  def receive = {
    case TagGlimpse(id, glimpses) => addOrUpdate(id, glimpses)

    case GetGlimpse(id) =>
      val glimpses = glimpsetags.getOrElse(id, Seq.empty[String])
      sender() ! glimpses
  }

  private def addOrUpdate(id: String, glimpses: Seq[Int]) {
    glimpsetags += ((id, glimpses))

    val oldfile = new File(outfile)
    val tmpfile = File.createTempFile("tmp", "", new File(oldfile.getParent))
    val bw = new BufferedWriter(new FileWriter(tmpfile))

    glimpsetags foreach { ig =>
      bw.write(ig._1 + "," + ig._2.mkString(",") + "\n")
    }
    bw.close()

    if(!oldfile.exists() || oldfile.delete()) tmpfile.renameTo(oldfile)
  }

  private def readGlimpseTagsToMap(file: String) =
    if(!Files.exists(Paths.get(file)))
      HashMap.empty[String, Seq[Int]]
    else
      Source.fromFile(file).getLines.toList.foldLeft(HashMap.empty[String, Seq[Int]])((map, line) => {
        val els = line.split(",")
        map += ((els.head, els.tail.map{x => x.toInt}))
        map
      })

}


object GlimpseTagger {

  def props(outfile: String) = Props(classOf[GlimpseTagger], outfile)

}