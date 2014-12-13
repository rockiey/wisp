package com.quantifind.charts

import unfiltered.util.Port
//import com.typesafe.config.ConfigFactory
import com.quantifind.sumac.{ArgMain, FieldArgs}
import com.quantifind.charts.UnfilteredWebApp.Arguments

/**
 * build up a little web app that serves static files from the resource directory
 * and other stuff from the provided plan
 * User: pierre
 * Date: 10/3/13
 */
trait UnfilteredWebApp[T <: Arguments] extends ArgMain[T] {

  def htmlRoot: String

  def setup(args: T): unfiltered.filter.Plan

  def get(parsed: T) = {
    val root = parsed.altRoot match {
      case Some(path) => new java.io.File(path).toURL
      case _ => getClass.getResource(htmlRoot)
    }
//    implicit val conf = ConfigFactory.load()
    println("serving resources from: " + root)
    val server = unfiltered.jetty.Http(parsed.port)
      .resources(root) //whatever is not matched by our filter will be served from the resources folder (html, css, ...)
      .filter(setup(parsed))
    val connector = server.underlying.getConnectors.head
    connector.setRequestHeaderSize(parsed.headerSize)
    server
  }

  override def main(parsed: T) {
    get(parsed).run()
  }

}

object UnfilteredWebApp {

  trait Arguments extends FieldArgs {
    var headerSize: Int = (2 * 10e6).toInt // 2 megabytes
    var port = Port.any
    var altRoot: Option[String] = None
  }

}
