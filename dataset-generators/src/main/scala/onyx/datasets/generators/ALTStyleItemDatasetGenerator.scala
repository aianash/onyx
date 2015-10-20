package onyx
package datasets
package generators

import org.rogach.scallop._

import core.repl._
import creed.query._, models._, datasets._


object ALTStyleItemDatasetGenerator {

  class Conf(args: Seq[String]) extends ScallopConf(args) {
    val datasetfile = opt[String]("datasetfile", descr = "source dataset file ", required = true)
    val itemjsonfile = opt[String]("item-json", descr = "item json file ", required = true)
  }

  def main(args: Array[String]) {
    val conf = new Conf(args)
    val dataset = ALTItemRelevanceDataset(conf.datasetfile(), conf.itemjsonfile())
    ALTItemRelevanceRECL.start(dataset)
  }

  case object ALTItemRelevanceRECL extends RECL[ALTItemRelevanceDataset, (ALT, DatasetItemFeature), Char, (ALT, DatasetItemFeature)] {

    val source  = new RECL.Source[(ALT, DatasetItemFeature), ALTItemRelevanceDataset] {
      def apply(dataset: ALTItemRelevanceDataset) = dataset.generateRandomALTFeatureCombo
    }

    val eval = new RECL.Eval[ALTItemRelevanceDataset, (ALT, DatasetItemFeature), (ALT, DatasetItemFeature)] {
      def apply(dataset: ALTItemRelevanceDataset) = (altFeatureT : (ALT, DatasetItemFeature)) =>
        Some(s"How relevant is ALT ${altFeatureT._1} for item ${altFeatureT._2} ?" -> altFeatureT)
    }

    val confirm = RECL.one23Confirm

    val sink = new RECL.Sink[ALTItemRelevanceDataset, (ALT, DatasetItemFeature), Char, (ALT, DatasetItemFeature)] {
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