package onyx.querymodelling.dataset

import scala.collection.mutable.{HashSet, HashMap}

import java.io.{BufferedWriter, FileWriter, PrintWriter, StringReader}
import java.nio.file.FileSystems

import play.api.libs.json._

import org.apache.lucene.index._
import org.apache.lucene.search._
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute
import org.apache.lucene.store.FSDirectory
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.search.similarities.BM25Similarity
import org.apache.lucene.document.{Document, StringField, TextField, Field}

import org.rogach.scallop._


object StyleDatasetCreator {

  val analyzer = new StemAnalyzer

  class Conf(arguments: Seq[String]) extends ScallopConf(arguments) {
    val task = opt[String]("task", descr = "what task to perform", required = true)
  }

// bandeau dress     party, chic, stylish, casual, summer, evening, date,
// bardot dress      party, chic, stylish, casual, summer, evening, date,
// cami dress        outdoor, summer, casual, evening,
// chiffon hem dress   work, casual, evening
// denim dress     summer, casual, outdoor
// gypsy dress     summer, outdoor, date, evening, casual
// kaftan dress    party, summer, casual
// maxi dress      party, evening, date, casual
// midi dress      evening, summer, party, date, work
// mini dress      evening, party, date, casual
// pencil dress    work, casual
// pinafore dress  summer, casual
// shift dress,     summer, casual, outdoor, evening
// shoulder dress,  evening, outdoor, summer, casual, party, 
// skater dress ,   work, casual, evening
// sun dress      , summer, casual, outdoor
// swing dress     ,work, casual, party, date, evening, outdoor
// wrap dress      ,evening, party, casual, 


// bandeau tops     , party, chic, stylish, casual, summer, evening, date,
// bardot tops      , party, chic, stylish, casual, summer, outdoor,
// block top       ,  work, summer, 
// blouses         ,  work, summer, casual, 
// boxy top        ,  summer, casual, outdoor, evening,
// crop tops       ,  summer, stylish, casual, outdoor, evening, 
// edge tops        , summer, outdoor, evening, casual, date, party
// embroidered top ,  summer, outdoor, work, casual
// fringe top     ,   stylish, date, evening, casual, chic
// hem top         ,  work, casual, summer, 
// jersey top      ,  summer, stylish, casual, outdoor, evening, 
// maxi top        ,  summer, casual, work, evening
// printed top     ,  summer, work, casual, outdoor
// shell top       ,  casual, outdoor, work, 
// shoulder top    ,  party, chic, stylish, casual, summer, evening, date,
// tube top        ,  party, chic, stylish
// v neck blouse   ,  work, casual

// boyfriend jeans  casual, summer, outdoor
// cropped jeans    casual, summer, outdoor, party
// flared jeans     work, summer, casual, outdoor, evening
// jeans            work, summer, casual, outdoor,
// skinny jeans     summer, casual, party, date, outdoor
// straight jeans   work, summer, casual, party, date

// day shorts
// denim shorts
// smart shorts

// co-ord skirt
// fringe skirt
// maxi skirt
// midi skirt
// mini skirt
// pencil skirt
// skater skirt
// skirt

// shirt dress
// lantern dress
// lace mini dress  

// dungarees

// denim bodycon

// jeggings
// jumpsuit

// street style


  def main(args: Array[String]) {
    val conf = new Conf(args)

    val filename = "allstyles.jsonl"
    val indexDir = "styleindex"

    conf.task() match {
      case "add" | "a" =>
        val writer = new PrintWriter(new BufferedWriter(new FileWriter(filename, true)))

        Iterator.continually(io.StdIn.readLine)
          .takeWhile(s => s != null && s.nonEmpty)
          .map(_.split(",").map(_.trim).filter(!_.isEmpty).toList)
          .foreach {
            case style :: context if !context.isEmpty =>
              val data = Json.obj(
                "type" -> style,
                "context" -> JsArray(context.map(x => Json.toJson(x)))
              )
              val json = Json.stringify(data)
              writer.println(json)

            case _ => println("ignoring")
          }

        sys.ShutdownHookThread(writer.close)

      case "index" | "i" =>
        val styles = HashMap[String, HashSet[String]]()

        io.Source.fromFile(filename).getLines
          .map(s => Json.parse(s))
          .foreach { json =>
            val style = (json \ "type").as[String]
            val context = (json \ "context").as[JsArray].value.map(_.as[String])
            // small
            context.map(_.replace("top", ""))
            styles.get(style) match {
              case Some(contexts) => contexts ++= context
              case None =>
                val contexts = HashSet[String](context: _*)
                styles.put(style, contexts)
            }
          }

        scala.util.Sorting.stableSort(styles.map(_._1).toSeq).foreach(println(_))
        styles.foreach { style =>
          println(s"style = ${style._1} \ncontext = ${style._2}\n")
        }

        val writer = {
          val dir = FSDirectory.open(FileSystems.getDefault.getPath("./", indexDir))
          val config = new IndexWriterConfig(analyzer)
          config.setSimilarity(new BM25Similarity)
          new IndexWriter(dir, config)
        }

        styles.foreach {
          case (style, context) =>
            val document = new Document
            document.add(new StringField("style", style, Field.Store.YES))
            document.add(new TextField("direct", new StringReader(style)))
            document.add(new TextField("context", new StringReader(context.mkString(" "))))
            writer.addDocument(document)
        }

        sys.ShutdownHookThread(writer.close)

      case "search" | "s" =>
        val searcher = {
          val dir = FSDirectory.open(FileSystems.getDefault.getPath("./", indexDir))
          val reader = DirectoryReader.open(dir)
          new IndexSearcher(reader)
        }

        println("SEARCHING FOR WOMEN STYLES")
        println("I M LOOKING FOR ...")
        Iterator.continually(io.StdIn.readLine)
          .takeWhile(s => s != null && s.nonEmpty)
          .foreach { queryStr =>
            val query = expandedQuery(queryStr).foldLeft(new BooleanQuery) { (query, token) =>
              query.add(new TermQuery(new Term("context", token)), BooleanClause.Occur.SHOULD)
              val styleQuery = new TermQuery(new Term("direct", token))
              styleQuery.setBoost(1.5f)
              query.add(styleQuery, BooleanClause.Occur.SHOULD)
              query
            }


            val collector = TopScoreDocCollector.create(10)
            searcher.search(query, collector)
            val hits = collector.topDocs.scoreDocs
            val styleScores = hits.map { hit =>
              val doc = searcher.doc(hit.doc)
              doc.get("style") -> hit.score
            }
            println("SUGGESTED STYLES = ")
            styleScores.foreach(x => println(s"${x._2} \t ${x._1}"))
            println("\nI M LOOKING FOR ...")
          }
    }

  }

  def expandedQuery(query: String) = {
    var result = List.empty[String]
    val stream = analyzer.tokenStream("context", new StringReader(query))
    stream.reset
    while(stream.incrementToken) {
      result = stream.getAttribute(classOf[CharTermAttribute]).toString :: result
    }
    stream.end
    stream.close
    result
  }

}

import org.apache.lucene.analysis._, standard._, Analyzer.TokenStreamComponents, snowball._
import org.apache.lucene.analysis.util.ClasspathResourceLoader
import org.apache.lucene.analysis.core.LowerCaseFilter
import org.apache.lucene.analysis.core.StopAnalyzer
import org.apache.lucene.analysis.core.StopFilter
import org.apache.lucene.analysis.util.CharArraySet
import org.apache.lucene.analysis.util.StopwordAnalyzerBase

import java.io.Reader

class StemAnalyzer extends StopwordAnalyzerBase(StopAnalyzer.ENGLISH_STOP_WORDS_SET) {

  /** Default maximum allowed token length */
  val DEFAULT_MAX_TOKEN_LENGTH = 255

  val maxTokenLength = DEFAULT_MAX_TOKEN_LENGTH

  override protected def createComponents(fieldName: String): TokenStreamComponents = {
    val src = new StandardTokenizer()
    src.setMaxTokenLength(maxTokenLength)
    var tok: TokenStream = new StandardFilter(src)
    tok = new LowerCaseFilter(tok)
    tok = new StopFilter(tok, stopwords)
    val args = new java.util.HashMap[String, String]()
    args.put("language", "English")
    val factory = new SnowballPorterFilterFactory(args)
    factory.inform(new ClasspathResourceLoader)
    tok = factory.create(tok)

    return new TokenStreamComponents(src, tok) {
      override protected def setReader(reader: Reader) {
        src.setMaxTokenLength(maxTokenLength)
        super.setReader(reader)
      }
    };
  }
}