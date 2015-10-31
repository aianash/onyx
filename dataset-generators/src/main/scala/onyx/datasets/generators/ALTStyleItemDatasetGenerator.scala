package onyx
package datasets
package generators

import scala.io.Source

import java.net.URL
import javax.swing.{JDialog, JLabel, ImageIcon}
import java.awt.Dimension
import java.awt.Toolkit

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

    // set dialog to display the product image
    val dialog = new JDialog
    val label  = new JLabel

    dialog.setUndecorated(false)
    dialog.setAlwaysOnTop(true)
    dialog.setFocusable(false)
    dialog.setFocusableWindowState(false)
    dialog.add(label)
    dialog.setVisible(true)

    ALTItemRelevanceRECL(intentDataset, itemjson).start(itemFeatureDataset, dialog, label)
  }

  /**
   * RECL for relevance of an ALT to a clothing item
   * @param {IntentDataset} intent dataset to get random ALTs
   * @param {String} path to clothing item json file
   */
  case class ALTItemRelevanceRECL(intentDataset: IntentDataset, path: String)
    extends RECL[(ALTItemRelevanceDataset, JDialog, JLabel), (ALT, String), Char, (ALT, ItemFeature)] {

    val source  = new RECL.Source[(ALT, String), (ALTItemRelevanceDataset, JDialog, JLabel)] {
      def apply(context: (ALTItemRelevanceDataset, JDialog, JLabel)) =
        intentDataset.generateRandomALT.asInstanceOf[RandomIterator[ALT]] <+> Source.fromFile(path).getLines.toList
    }

    val eval = new RECL.Eval[(ALTItemRelevanceDataset, JDialog, JLabel), (ALT, String), (ALT, ItemFeature)] {
      def apply(context: (ALTItemRelevanceDataset, JDialog, JLabel)) = (altItemT : (ALT, String)) => {
        val itemJson = Json.parse(altItemT._2)
        val itg      = ItemTypeGroup((itemJson \ "itemTypeGroup").as[String])
        val styles   = ClothingStyles((itemJson \ "styles").as[Seq[String]] map { x => ClothingStyle(x) })
        val fabric   = ApparelFabric((itemJson \ "fabric").as[String])
        val fit      = ApparelFit((itemJson \ "fit").as[String])
        val color    = Colors((itemJson \ "colors").as[Seq[String]])
        val tips     = StylingTips((itemJson \ "stylingTips").as[String])
        val descr    = Description((itemJson \ "descr").as[String])
        val feature  = ItemFeature(itg, styles, fabric, fit, color, tips, descr)
        val imageUrl = new URL((itemJson \ "images" \ "primary").as[String])

        // show image in dialog
        val img = new ImageIcon(imageUrl)
        context._3.setIcon(img)
        context._2.pack

        // set window position
        val toolkil    = Toolkit.getDefaultToolkit
        val screenSize = toolkil.getScreenSize
        context._2.setLocation(screenSize.width - context._2.getWidth, screenSize.height - context._2.getHeight)

        // confirmation message
        var msg = s"How relevant is the dress shown in the image for ${altItemT._1.activity.value.toUpperCase} to look "
        msg += s"${altItemT._1.look.value.toUpperCase} on/in ${altItemT._1.timeWeather.value.toUpperCase}?\n"
        msg += s"Item Features:\n"
        msg += s"${feature}\n"

        Some(s"\033[H\033[2J\n\n${msg}" -> (altItemT._1, feature))
      }
    }

    val confirm = RECL.one23Confirm

    val sink = new RECL.Sink[(ALTItemRelevanceDataset, JDialog, JLabel), (ALT, String), Char, (ALT, ItemFeature)] {
      def apply(context: (ALTItemRelevanceDataset, JDialog, JLabel)) = {
        case Left((_, altFeatureT, choice)) => context._1 += ((altFeatureT._1, altFeatureT._2, relevance(choice)))
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