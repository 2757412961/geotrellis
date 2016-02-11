package geotrellis.raster.op.zonal.summary

import geotrellis.raster._
import geotrellis.vector._
import geotrellis.raster.rasterize._
import geotrellis.raster.histogram._

object Histogram extends TileIntersectionHandler[Histogram[Int]] {
  def handlePartialTile(raster: Raster[Tile], polygon: Polygon): Histogram[Int] = {
    val Raster(tile, _) = raster
    val rasterExtent = raster.rasterExtent
    val histogram = FastMapHistogram()
    Rasterizer.foreachCellByGeometry(polygon, rasterExtent) { (col: Int, row: Int) =>
      val z = tile.get(col, row)
      if (isData(z)) histogram.countItem(z, 1)
    }
    histogram
  }

  def handleFullTile(tile: Tile): Histogram[Int] = {
    val histogram = FastMapHistogram()
    tile.foreach { (z: Int) => if (isData(z)) histogram.countItem(z, 1) }
    histogram
  }

  def combineResults(rs: Seq[Histogram[Int]]): Histogram[Int] =
    FastMapHistogram.fromHistograms(rs)
}
