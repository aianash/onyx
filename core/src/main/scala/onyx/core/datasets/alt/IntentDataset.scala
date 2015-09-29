package onyx
package core
package datasets
package alt

import scala.util.Random
import scala.collection.Set
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

import java.io.File
import java.util.{Set => JavaSet}

import org.mapdb._

import hemingway.dictionary.FileBasedDictionary
import hemingway.dictionary.similarity._


class IntentDataset(db: DB) {

  import IntentDataset._

  private val dictionary = FileBasedDictionary(db)

  private val activities   = db.hashSetCreate("_activities").makeOrGet[String].asScala
  private val looks        = db.hashSetCreate("_looks").makeOrGet[String].asScala
  private val timeWeathers = db.hashSetCreate("_timeWeathers").makeOrGet[String].asScala

  private val alts = db.treeSetCreate("_alts_relevance")
                       .serializer(ARRAY5)
                       .makeOrGet[Array[Object]]

  var similarity: Similarity = Cosine(0.3)

  def +=(intent: Intent): Unit = add(intent)
  def +=(rel: (ALT, Float)) = add(rel)

  def add(intent: Intent): Unit = intent match {
    case Activity(activity) =>
      dictionary += (activity -> Map("type" -> intent.intentType))
      activities.add(activity)
    case Look(look) =>
      dictionary += (look -> Map("type" -> intent.intentType))
      looks.add(look)
    case TimeWeather(timeWeather) =>
      dictionary += (timeWeather -> Map("type" -> intent.intentType))
      timeWeathers.add(timeWeather)
  }

  def add(rel: (ALT, Float)): Unit = {
    val alt = rel._1
    val relevance: java.lang.Float = rel._2
    var count: java.lang.Integer = 1
    val entry = Array[Object](alt.activity.value, alt.look.value, alt.timeWeather.value, relevance, count)
    val existingItr = Fun.filter(alts, alt.activity.value, alt.look.value, alt.timeWeather.value, relevance).iterator
    if(existingItr.hasNext) {
      val existing = existingItr.next
      alts.remove(existing)
      val existingCnt = existing(4).asInstanceOf[java.lang.Integer]
      val newCnt: java.lang.Integer = existingCnt.intValue() + 1
      entry(4) = newCnt
    }
    alts.add(entry)
  }

  def findSimilar(intent: Intent, topK: Int): Set[Intent] =
    dictionary.findSimilar(intent.value, similarity, topK)
              .filter(_.payload("type").equals(intent.intentType))
              .map(_.str.map(intent.copy(_)))
              .flatten

  def altIterator: Iterator[(ALT, Float, Int)] = alts.iterator.map { arr =>
    val alt =
      ALT(Activity(arr(0).asInstanceOf[String]),
          Look(arr(1).asInstanceOf[String]),
          TimeWeather(arr(2).asInstanceOf[String]))
    val relevance = arr(3).asInstanceOf[java.lang.Float].floatValue()
    val count = arr(4).asInstanceOf[java.lang.Integer].intValue()
    (alt, relevance, count)
  }

  def generateRandomALT: Iterator[ALT] =
    (WindowedRandomIterator(activities) <+> looks |+| timeWeathers)((x, t) => ALT(Activity(x._1), Look(x._2), TimeWeather(t)))

}

object IntentDataset {

  def apply(filePath: String): IntentDataset = apply(mkDB(filePath))
  def apply(db: DB) = new IntentDataset(db)

  private val ARRAY5 = new BTreeKeySerializer.ArrayKeySerializer(
    Array(Fun.COMPARATOR, Fun.COMPARATOR, Fun.COMPARATOR, Fun.COMPARATOR, Fun.COMPARATOR),
    Array(Serializer.BASIC, Serializer.BASIC, Serializer.BASIC, Serializer.BASIC, Serializer.BASIC))

  private def mkDB(dbPath: String) =
    DBMaker.fileDB(new File(dbPath))
          .asyncWriteFlushDelay(1)
          .cacheHardRefEnable
          .transactionDisable
          .closeOnJvmShutdown
          .compressionEnable
          .fileMmapEnableIfSupported
          .make

}