package controllers

import play.api._
import play.api.mvc._
import play.api.mvc.Request
import deductions.runtime.html.TableView
import play.api.libs.iteratee.Enumerator
import play.api.libs.iteratee.Iteratee
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import deductions.runtime.jena.JenaHelpers
import deductions.runtime.jena.RDFStoreLocalJena1Provider
import scala.xml.NodeSeq
import play.api.i18n.Lang
import deductions.runtime.services.LDP
import org.w3.banana.jena.Jena
import com.hp.hpl.jena.query.Dataset
import deductions.runtime.services.Lookup
import play.api.libs.json.Json
import org.w3.banana.jena.JenaModule
import org.w3.banana.io.JsonLd
import org.w3.banana.io.JsonLdExpanded
import org.w3.banana.io.JsonLdFlattened
import java.io.InputStream
import java.io.Reader
import org.w3.banana.io.RDFReader
import org.w3.banana.io.RDFWriter
import java.io.OutputStream

object Application extends Controller
    with JenaModule
with TableView
    with JenaHelpers
    with RDFStoreLocalJena1Provider
    with LanguageManagement
    with LDP[Jena, Dataset]
    with Lookup[Jena, Dataset]
    {

  import scala.util.Try
  // Members declared in org.w3.banana.JsonLDReaderModule
  implicit val jsonldReader = new  RDFReader[Rdf, Try,JsonLd] {
	  override def read(is: InputStream, base: String): Try[Rdf#Graph] = ???
  	override def read(reader: Reader, base: String): Try[Rdf#Graph] = ???
  }
  // Members declared in org.w3.banana.JsonLDWriterModule
  implicit val jsonldExpandedWriter = new RDFWriter[Rdf,scala.util.Try,JsonLdExpanded] {
    override def write(graph: Rdf#Graph, os: OutputStream, base: String): Try[Unit] = ???
    override def asString(graph: Rdf#Graph, base: String): Try[String]= ???
  }
  implicit val jsonldFlattenedWriter = new RDFWriter[Rdf,scala.util.Try,JsonLdFlattened] {
    override def write(graph: Rdf#Graph, os: OutputStream, base: String): Try[Unit] = ???
    override def asString(graph: Rdf#Graph, base: String): Try[String]= ???
  }


  val glob = _root_.global1.Global

  def index() = {
    Action { implicit request =>
      Ok(views.html.index(glob.form)(lang = chooseLanguageObject(request)))
    }
  }

  def displayURI(uri: String, blanknode: String = "", Edit: String = "") = {
    Action { implicit request =>
      println("displayURI: " + request)
      println("displayURI: " + Edit)
      Ok(views.html.index(glob.htmlForm(uri, blanknode, editable = Edit != "",
        lang = chooseLanguage(request)))).
        withHeaders("Access-Control-Allow-Origin" -> "*") // for dbpedia lookup
    }
  }

  //  def displayURI2(uri: String) = {
  //    Action.async { implicit request =>
  //      println("displayURI2: " + request)
  //      //      Ok.chunked( glob.displayURI2(uri) )
  //      glob.displayURI2(uri) map { x =>
  //        Ok(x.mkString).as("text/html")
  //      }
  //    }
  //  }

  def wordsearch(q: String = "") = Action.async {
    val fut = glob.wordsearchFuture(q)
    fut.map(r => Ok(views.html.index(r)))
  }

  def download(url: String) = {
    Action { Ok(glob.downloadAsString(url)).as("text/turtle; charset=utf-8") }
  }

  /** cf https://www.playframework.com/documentation/2.3.x/ScalaStream */
  def download_chunked(url: String) = {
    Action { Ok.chunked(glob.download(url)).as("text/turtle; charset=utf-8") }
  }

  //  def chooseLanguage(request: Request[_]): String = {
  //    chooseLanguageObject(request).language
  //  }
  //  def chooseLanguageObject(request: Request[_]): Lang = {
  //    val languages = request.acceptLanguages
  //    val res = if (languages.length > 0) languages(0) else Lang("en")
  //    println("chooseLanguage: " + request + "\n\t" + res)
  //    res
  //  }

  def edit(url: String) = {
    Action { request =>
      Ok(views.html.index(glob.htmlForm(
        url,
        editable = true,
        lang = chooseLanguage(request)))).
        withHeaders("Access-Control-Allow-Origin" -> "*") // TODO dbpedia only
    }
  }

  def save() = {
    Action { implicit request =>
      Ok(views.html.index(glob.save(request)))
    }
  }

  def create() = {
    Action { implicit request =>
      println("create: " + request)
      val uri = getFirstNonEmptyInMap(request.queryString).get
      println("create: " + uri)
      Ok(views.html.index(glob.createElem2(uri, chooseLanguage(request))))
    }
  }

  /** TODO move to FormSaver */
  def getFirstNonEmptyInMap(map: Map[String, Seq[String]]): Option[String] = {
    val uriArgs = map.getOrElse("uri", Seq())
    uriArgs.find { uri => uri != "" }
  }

  def sparql(query: String) = {
    Action { implicit request =>
      println("sparql: " + request)
      println("sparql: " + query)
      Ok(views.html.index(glob.sparql(query, chooseLanguage(request))))
    }
  }

  def select(query: String) = {
    Action { implicit request =>
      println("sparql: " + request)
      println("sparql: " + query)
      Ok(views.html.index(glob.select(query, chooseLanguage(request))))
    }
  }

  def backlinks(q: String = "") = Action.async {
    val fut = glob.backlinksFuture(q)
    val extendedSearchLink = <p>
                               <a href={ "/esearch?q=" + q }>
                                 Extended Search for &lt;{ q }
                                 &gt;
                               </a>
                             </p>
    fut.map { res =>
      Ok(views.html.index(NodeSeq
        fromSeq Seq(extendedSearchLink, res)))
    }

  }

  def extSearch(q: String = "") = Action.async {
    val fut = glob.esearchFuture(q)
    fut.map(r => Ok(views.html.index(r)))
  }

  def ldp(uri: String) = {
    Action { implicit request =>
      println("LDP GET: " + request)
      val contentType = request.contentType
      val AcceptsTurtle = Accepting("text/turtle")
      val turtle = AcceptsTurtle.mimeType
      val accepts = Accepting(contentType.getOrElse(turtle))
      val r = getTriples(uri, accepts.mimeType)
      println("LDP: GET: " + r)
      render {
        case AcceptsTurtle() =>
          Ok(r.get).as( turtle + "; charset=utf-8")
        case Accepts.Json() => Ok(Json.toJson(r.get))
      }
    }
  }

  /** TODO: this is blocking code !!! */
  def ldpPOST(uri: String) = {
    Action { implicit request =>
      println("LDP: " + request)
      val slug = request.headers.get("Slug")
      val link = request.headers.get("Link")
      val contentType = request.contentType
      val content = {
        val asText = request.body.asText
        if (asText != None) asText
        else {
          val raw = request.body.asRaw.get
          println(s"""LDP: raw: "$raw" size ${raw.size}""")
          raw.asBytes(raw.size.toInt).map {
            arr => new String(arr, "UTF-8")
          }
        }
      }
      println(s"LDP: content: $content")
      val serviceCalled =
        putTriples(uri, link, contentType, slug, content).getOrElse("default")
      Ok(serviceCalled).as("text/plain; charset=utf-8")
    }
  }

  def lookupService(search: String) = {
    Action { implicit request =>
      println("Lookup: " + request)
      Ok(lookup(search)).as("text/json-ld; charset=utf-8")
    }
  }

}
