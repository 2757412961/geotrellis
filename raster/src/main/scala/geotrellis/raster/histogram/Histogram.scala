/*
 * Copyright (c) 2014 Azavea.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.raster.histogram

import geotrellis.raster.summary.Statistics
import geotrellis.raster.NODATA
import math.{abs, round, sqrt}

import spire.syntax.cfor._

/**
  * Data object representing a histogram of values.
  */
abstract trait Histogram[@specialized (Int, Double) T <: AnyVal] extends Serializable {
  /**
   * Return the number of occurances for 'item'.
   */
  def getItemCount(item: T): Int

  /**
   * Return the total number of occurances for all items.
   */
  def getTotalCount(): Int

  /**
   * Return the smallest item seen.
   */
  def getMinValue(): T

  /**
   * Return the largest item seen.
   */
  def getMaxValue(): T

  /**
   * Return the smallest and largest items seen as a tuple.
   */
  def getMinMaxValues(): (T, T) = (getMinValue, getMaxValue)

  /**
   * Return a mutable copy of this histogram.
   */
  def mutable(): MutableHistogram[T]

  def getValues(): Array[T]

  def rawValues(): Array[T]

  def foreach(f: (T, Int) => Unit): Unit

  def foreachValue(f: T => Unit): Unit

  def getQuantileBreaks(num: Int): Array[T]

  def getMode(): T

  def getMedian(): T

  def getMean(): Double

  def generateStatistics(): Statistics[T]
}

abstract trait HistogramInt extends Histogram[Int] {
  def foreach(f: (Int, Int) => Unit): Unit = {
    getValues.foreach(z => f(z, getItemCount(z)))
  }

  def getMode(): Int = {
    if(getTotalCount == 0) { return NODATA }
    val values = getValues()
    var mode = values(0)
    var count = getItemCount(mode)
    val len = values.length
    cfor(1)(_ < len, _ + 1) { i =>
      val z = values(i)
      val c = getItemCount(z)
      if (c > count) {
        count = c
        mode = z
      }
    }
    mode
  }

  def getMedian() = if (getTotalCount == 0) {
    NODATA
  } else {
    val values = getValues()
    val middle: Int = getTotalCount() / 2
    var total = 0
    var i = 0
    while (total <= middle) {
      total += getItemCount(values(i))
      i += 1
    }
    values(i-1)
  }

  def getMean(): Double = {
    if(getTotalCount == 0) { return NODATA }

    val values = rawValues()
    var mean = 0.0
    var total = 0.0
    val len = values.length

    cfor(0)(_ < len, _ + 1) { i =>
      val value = values(i)
      val count = getItemCount(value)
      val delta = value - mean
      total += count
      mean += (count * delta) / total
    }
    mean
  }

  def generateStatistics() = {
    val values = getValues()
    if (values.length == 0) {
      Statistics.EMPTYInt
    } else {

      var dataCount: Long = 0

      var mode = 0
      var modeCount = 0

      var mean = 0.0
      var total = 0

      var median = 0
      var needMedian = true
      val limit = getTotalCount() / 2

      val len = values.length

      cfor(0)(_ < len, _ + 1) { i =>
        val value = values(i)
        val count = getItemCount(value)
        dataCount = dataCount + count
        if (count != 0) {
          // update the mode
          if (count > modeCount) {
            mode = value
            modeCount = count
          }

          // update the mean
          val delta = value - mean
          total += count
          mean += (count * delta) / total

          // update median if needed
          if (needMedian && total > limit) {
            median = values(i)
            needMedian = false
          }
        }
      }

      // find the min value
      val zmin = values(0)

      // find the max value
      val zmax = values(len - 1)

      // find stddev
      total = 0
      var mean2 = 0.0
      cfor(0)(_ < len, _ + 1) { i =>
        val value = values(i)
        val count = getItemCount(value)

        if (count > 0) {
          val x = value - mean
          val y = x * x

          val delta = y - mean2
          total += count
          mean2 += (count * delta) / total
        }
      }
      val stddev = sqrt(mean2)

      Statistics[Int](dataCount, mean, median, mode, stddev, zmin, zmax)
    }
  }
}
