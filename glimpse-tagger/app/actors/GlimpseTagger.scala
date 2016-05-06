package actors

import java.io._

import akka.actor.{Actor, ActorLogging, Props}


sealed trait GlimpseTaggerProtocol
case class TagGlimpse(id: String, glimpses: Seq[Int]) extends GlimpseTaggerProtocol


class GlimpseTagger(outfile: String) extends Actor with ActorLogging {

  def receive = {
    case TagGlimpse(id, glimpses) => addOrUpdate(id, glimpses)
  }

  private def addOrUpdate(id: String, glimpses: Seq[Int]) {
    val tmpfile = File.createTempFile("tmp", "")
    val br = new BufferedReader(new FileReader(outfile))
    val bw = new BufferedWriter(new FileWriter(tmpfile))
    var line = br.readLine
    while(line != null) {
      if(line.split(",").head != id) bw.write(line + "\n")
      line = br.readLine
    }
    bw.write(id + "," + glimpses.mkString(",") + "\n")
    br.close()
    bw.close()

    val oldfile = new File(outfile)
    if(oldfile.delete()) tmpfile.renameTo(oldfile)
  }

}


object GlimpseTagger {

  def props(outfile: String) = Props(classOf[GlimpseTagger], outfile)

}