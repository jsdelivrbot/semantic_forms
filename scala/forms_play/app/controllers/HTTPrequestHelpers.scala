package controllers

import play.api.mvc.Request
import deductions.runtime.core.HTTPrequest
import deductions.runtime.core.Cookie
import play.api.mvc.AnyContentAsFormUrlEncoded
import play.api.mvc.RequestHeader
import play.api.mvc.AnyContent
import play.api.mvc.Security

/** copy PLay! classes into SF classes, to avoid dependencies in other project senantic_forms */
trait HTTPrequestHelpers {
  
	/** a copy of the request with no Play dependency :) */
  def getRequestCopy()(implicit request: Request[_]): HTTPrequest = copyRequest(request)

  def getRequestCopyAnyContent()(implicit request: Request[AnyContent]): HTTPrequest = copyRequest(request)

  def copyRequest(request: Request[_]): HTTPrequest =
    copyRequestHeader(request) . copy( formMap = getFormMap(request) )

  def copyRequestHeader(request: RequestHeader): HTTPrequest = {
    import request._
    val cookiesMap = cookies.map { cookie => (cookie.name -> copyCookie(cookie)) } . toMap
//    val formMap = getFormMap(request)
    val username = request.session.get(Security.username)
    logger.debug(s"copyRequestHeader: username '$username'")
    val res = HTTPrequest(host, remoteAddress,
      rawQueryString, queryString,
      headers = headers.toMap,
      cookies = cookiesMap,
      acceptLanguages = request.acceptLanguages . map {
        al => al.language
      },
      path = request.path,
//      formMap = formMap,
      uri = uri,
      to_string = request.toString(),
      secure = secure,
      domain = domain,
      session = session.data,
      username = username
    )
    // println(s"copyRequest: headers: " + headers.toMap)
//    println(s"copyRequest: cookiesMap $cookiesMap , userId ${res.userId()}")
//    println(s"""===========>>>>>>>>> copyRequest request $request
//        tags ${request.tags}""")
    res
  }

  def copyCookie(cookie: play.api.mvc.Cookie): Cookie = {
    import cookie._
    Cookie(name, value: String, maxAge: Option[Int], path: String,
      domain: Option[String], secure: Boolean, httpOnly)
  }

  /** get Form Map from HTTP body in case of HTTP POST with Url Encoded Form, that is
   *  application/x-www-form-urlencoded */
  def getFormMap(request: Request[_]):  Map[String, Seq[String]] = {
    val body = request.body
    body match {
      case form: AnyContentAsFormUrlEncoded => form.data
      case _ => Map()
    }
  }
}

