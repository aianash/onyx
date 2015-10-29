package onyx
package datasets
package generators

import scala.io.Source

import org.rogach.scallop._

import play.api.libs.json._

import creed.core._
import core.repl._
import creed.query._, models._, datasets._

import commons.catalogue._, attributes._

/**
 * Dataset generator for relevance of an ALT to a clothing item
 */
object ALTStyleItemDatasetGenerator {

  class Conf(args: Seq[String]) extends ScallopConf(args) {
    val intentdataset = opt[String]("intent-dataset", descr = "intent dataset file ", required = true)
    val itemjson = opt[String]("item-json", descr = "catalogue item json ", required = true)
    val output = opt[String]("output", descr = "output dataset file ", required = true)
  }

  def main(args: Array[String]) {
    val conf               = new Conf(args)
    val itemjson           = conf.itemjson()
    val intentDataset      = IntentDataset(conf.intentdataset())
    val itemFeatureDataset = ALTItemRelevanceDataset(conf.output())

    ALTItemRelevanceRECL(intentDataset, itemjson).start(itemFeatureDataset)
  }

  /**
   * RECL for relevance of an ALT to a clothing item
   * @param {IntentDataset} intent dataset to get random ALTs
   * @param {String} path to clothing item json file
   */
  case class ALTItemRelevanceRECL(intentDataset: IntentDataset, path: String)
    extends RECL[ALTItemRelevanceDataset, (ALT, String), Char, (ALT, ItemFeature)] {

    val source  = new RECL.Source[(ALT, String), ALTItemRelevanceDataset] {
      def apply(dataset: ALTItemRelevanceDataset) =
        intentDataset.generateRandomALT.asInstanceOf[RandomIterator[ALT]] <+> Source.fromFile(path).getLines.toList
    }

    val eval = new RECL.Eval[ALTItemRelevanceDataset, (ALT, String), (ALT, ItemFeature)] {
      def apply(dataset: ALTItemRelevanceDataset) = (altItemT : (ALT, String)) => {
        val itemJson = Json.parse(altItemT._2)
        val itg      = ItemTypeGroup((itemJson \ "itemTypeGroup").as[String])
        val styles   = ClothingStyles((itemJson \ "styles").as[Seq[String]] map { x => ClothingStyle(x) })
        val fabric   = ApparelFabric((itemJson \ "fabric").as[String])
        val fit      = ApparelFit((itemJson \ "fit").as[String])
        val color    = Colors((itemJson \ "colors").as[Seq[String]])
        val tips     = StylingTips((itemJson \ "stylingTips").as[String])
        val descr    = Description((itemJson \ "descr").as[String])
        val feature  = ItemFeature(itg, styles, fabric, fit, color, tips, descr)
        Some(s"How relevant is ALT ${altItemT._1} for item ${altItemT._2} ?" -> (altItemT._1, feature))
      }
    }

    val confirm = RECL.one23Confirm

    val sink = new RECL.Sink[ALTItemRelevanceDataset, (ALT, String), Char, (ALT, ItemFeature)] {
      def apply(dataset: ALTItemRelevanceDataset) = {
        case Left((_, altFeatureT, choice)) => dataset += ((altFeatureT._1, altFeatureT._2, relevance(choice)))
        case _ =>
      }
    }

    private def relevance(choice: Char) = choice match {
      case '1' => 0.1f
      case '2' => 0.5f
      case '3' => 0.9f
      case _   => 0.0f
    }

  }

}