package deductions.apps

import scala.xml.NodeSeq
import deductions.runtime.views.MainXml

import deductions.runtime.utils.I18NMessages
import org.w3.banana.RDF
import deductions.runtime.core.HTTPrequest

/** HTML Front page skeleton for the Contacts SF application */
trait ContactsFrontPage[Rdf <: RDF, DATASET] extends MainXml with ContactsDashboard[Rdf, DATASET] {

	import ops._

  override val featureURI: String = fromUri(dbpedia("Contact_manager")) //  + "/index"

  override def result(request: HTTPrequest): NodeSeq = {
    val userURI = request.userId()
    val content = contactsDashboardHTML(request)

    mainPage(content, userInfo = <div/> // TODO
      , lang = "en", title = "Contacts Mgnt",
      displaySearch = true,
      messages = <p/> )
	}

  /**
   * main Page Header for generic app:
   *  enter URI, search, create instance
   */
  override def mainPageHeader(implicit lang: String = "en", userInfo: NodeSeq,
      displaySearch: Boolean = true): NodeSeq = {
    <header class="col col-sm-12">
      <div class="raw">
        <div class="col col-sm-9">
          <a href="/" title="Open a new Semantic_forms in a new tab." target="_blank">
            <h1>
              { messageI18N("Welcome") }
            </h1>
          </a>
        </div>
        <div class="col col-sm-3">
          { userInfo }
        </div>
      </div>
    </header>
    <div> {
      enterSearchTerm()
    } </div>
    <hr></hr>
  }

}

