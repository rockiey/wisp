package com.quantifind.charts

import com.quantifind.charts.highcharts._
import com.quantifind.charts.repl._
import scala.collection.immutable.ListMap
import scala.language.implicitConversions
import Highchart._
import org.apache.commons.math3.special.Gamma
import org.apache.commons.math3.util.ArithmeticUtils

/**
* User: austin
* Date: 12/2/14
*
* Highcharts implementation of plotting functionality. Includes several highcharts specific plots
*
* I rely on the fact that an implicit method defined in an object takes precedence over one
* defined in a trait to have Iterable[T] with PartialFunction[Int, T] resolve to this method
*/

object Highcharts extends IterablePairLowerPriorityImplicits with BinnedDataLowerPriorityImplicits with HighchartsStyles {

  implicit def mkIterableIterable[A: Numeric, B: Numeric](ab: (Iterable[A], Iterable[B])) = new IterableIterable(ab._1, ab._2)
  implicit def mkIterableIterable[A: Numeric, B: Numeric](ab: (Iterable[(A, B)])) = new IterableIterable(ab.map(_._1), ab.map(_._2))
  implicit def mkIterableIterable[B: Numeric](b: (Iterable[B])) = new IterableIterable((0 until b.size), b)

  implicit def binIterableNumBins[A: Numeric](data: Iterable[A], numBins: Int): BinnedData = new IterableBinned[A](data, numBins)
  implicit def mkPair[A, B: Numeric](data: Iterable[(A, B)]) = new PairBinned(data)
  implicit def mkTrueTriplet[A, B, C: Numeric](data: Iterable[(A, B, C)]) = new TrueTripletBinned(data)
  implicit def mkCoupledTriplet[A, B, C: Numeric](data: Iterable[((A, B), C)]) = new CoupledTripletBinned(data)

  def stopServer = stopWispServer
  def startServer() = startWispServer()
  def setPort(port: Int) = setWispPort(port)

  def area[A, B, C: Numeric, D: Numeric](xy: IterablePair[A, B, C, D]) = {
    val (xr, yr) = xy.toIterables
    xyToSeries(xr, yr, SeriesType.area)
  }

  def areaspline[A, B, C: Numeric, D: Numeric](xy: IterablePair[A, B, C, D]) = {
    val (xr, yr) = xy.toIterables
    xyToSeries(xr, yr, SeriesType.areaspline)
  }

  def bar[A, B, C: Numeric, D: Numeric](xy: IterablePair[A, B, C, D]) = {
    val (xr, yr) = xy.toIterables
    xyToSeries(xr, yr, SeriesType.bar)
  }

  def column[A, B, C: Numeric, D: Numeric](xy: IterablePair[A, B, C, D]) = {
    val (xr, yr) = xy.toIterables
    xyToSeries(xr, yr, SeriesType.column)
  }

  def bernoulli(p: Double, min: Double = Double.MaxValue, max: Double = Double.MinValue, points: Int = 1000) = {
    binomial(p, 1, min, max, points)
  }

  def binomial(n: Double, p: Double, min: Double = Double.MaxValue, max: Double = Double.MinValue, points: Int = 1000) = {
    // todo check n < 140 or don't use gamma function
    val stddev = math.sqrt(n*p*(1-p))
    val right = math.min(n, 140) // 141 is the largest valid integral Gamma.gamma() argument
    val left = if(min != Double.MaxValue) math.max(min, right / points) else right / points // 0 is not defined for gamma(0)
    def binomialPoint(k: Double) = {
      (math.pow(p, k) * math.pow(1 - p, n-k)) * (Gamma.gamma(n+1) / (Gamma.gamma(k+1) * Gamma.gamma((n-k)+1)))
    }
    val stepSize = (right - left) / points
    val data = (left to right by stepSize).map(p => p -> binomialPoint(p))
    val hc = Highchart(Series(data, chart = SeriesType.line, name = s"binomial($n, $p)"))
    plot(hc)
  }

  def gaussian(mean: Double, variance: Double, min: Double = Double.MaxValue, max: Double = Double.MinValue, points: Int = 1000) = { // todo number of points? and center points at mean or 0?
    // todo check input
    val rootTwoPi = math.sqrt(2*math.Pi)
    val twoVariance = 2 * variance
    val stddev = math.sqrt(variance)
    def gaussianPoint(p: Double) = {
      (1 / (stddev*rootTwoPi)) * math.pow(math.E, - math.pow(p - mean, 2) / twoVariance)
    }
    val left = if(min != Double.MaxValue) min else mean - 3*stddev
    val right = if(max != Double.MinValue) max else mean + 3*stddev
    val stepSize = (right - left) / points
    val data = (left to right by stepSize).map(p => p -> gaussianPoint(p))
    val hc = Highchart(Series(data, chart = SeriesType.line, name = s"gaussian($mean, $variance)"))
    plot(hc)
  }

  def poisson(lambda: Double, min: Double = Double.MaxValue, max: Double = Double.MinValue, points: Int = 1000) = {
    // todo check lambda < 140
    val stddev = math.sqrt(lambda)
    val right = math.min(if(max != Double.MinValue) max else lambda + 3*stddev, 140) // 141 is the largest valid integral Gamma.gamma() argument
    val left = if(min != Double.MaxValue) math.max(min, right / points) else right / points // 0 is not defined for gamma(0)
    val stepSize = (right - left) / points
    val eToTheNegativeLambda = math.pow(math.E, -lambda)
    def poissonPoint(k: Double) = { // Double
      math.pow(lambda, k) * eToTheNegativeLambda / Gamma.gamma(k+1)
    }
    val data = (left to right by stepSize).map(k => k -> poissonPoint(k))
    val hc = Highchart(Series(data, chart = SeriesType.line, name = s"poisson($lambda)"))
    plot(hc)
  }

  def histogram[A: Numeric](data: Iterable[A], numBins: Int) = {
    val binCounts = binIterableNumBins(data, numBins).toBinned().toSeq
    plot(Histogram.histogram(binCounts))
  }

  def histogram(data: BinnedData) = {
    val binCounts = data.toBinned().toSeq
    plot(Histogram.histogram(binCounts))
  }

  def line[A, B, C: Numeric, D: Numeric](xy: IterablePair[A, B, C, D]) = {
    val (xr, yr) = xy.toIterables
    xyToSeries(xr, yr, SeriesType.line)
  }

  def pie[A, B, C: Numeric, D: Numeric](xy: IterablePair[A, B, C, D]) = {
    val (xr, yr) = xy.toIterables
    xyToSeries(xr, yr, SeriesType.pie)
  }

  def regression[A, B, C: Numeric, D: Numeric](xy: IterablePair[A, B, C, D]) = {
    def numericToDouble[X](x: X)(implicit ev: Numeric[X]): Double = ev.toDouble(x)
    val (xr, yr) = xy.toIterables
    LeastSquareRegression.leastSquareRegression(xr.toSeq.map(numericToDouble(_)), yr.toSeq.map(numericToDouble(_)))
  }

  def scatter[A, B, C: Numeric, D: Numeric](xy: IterablePair[A, B, C, D]) = {
    val (xr, yr) = xy.toIterables
    xyToSeries(xr, yr, SeriesType.scatter)
  }

  def spline[A, B, C: Numeric, D: Numeric](xy: IterablePair[A, B, C, D]) = {
    val (xr, yr) = xy.toIterables
    xyToSeries(xr, yr, SeriesType.spline)
  }

  // Todo: can we disclose this information through reflection, instead of hardcoding it?
  /**
   * Output the basic usage of Highcharts
   */
  def help(): Unit = {
    println("\nAvailable Plot Types: Takes an Iterable, an Iterable of pairs, a pair of Iterables, or an Iterable and a Function\n")
    Seq("area", "areaspline", "bar", "column", "line", "pie", "scatter", "spline", "regression")
      .map(s => "\t" + s)
      .foreach(println)
    println("\nOther plotting options:\n")
    Map("histogram" -> "Iterable of Numerics or Pairs")
      .map{case(plot, description) =>"\t%-35s%s".format(plot, description)}
      .foreach(println)
    println("\nStylistic changes:\n")
    ListMap(
      "hold" -> "plots the next plot on top of the existing plot",
      "unhold" -> "plots the next plot in a new chart",
      "title(String)" -> "add a title to the most recent plot",
      "xAxis(String)" -> "adds a label to the x-axis",
      "yAxis(String)" -> "adds a label to y-axis",
      "legend(Iterable[String])" -> "adds a legend to the most recent plot",
      """stack(["normal", "percent"])""" -> "stacks bars, columns, and lines relative to each other"
    ).foreach{case(method, description) => println("\t%-35s%s".format(method, description))}

    println("\nServer Controls:\n")
    ListMap(
      "undo" -> "undoes the most recent action",
      "redo" -> "the opposite of undo",
      "delete" -> "wipes the most recent chart from the page",
      "deleteAll" -> "wipes all plots from the page"
    ).foreach{case(method, description) => println("\t%-35s%s".format(method, description))}
  }
}

