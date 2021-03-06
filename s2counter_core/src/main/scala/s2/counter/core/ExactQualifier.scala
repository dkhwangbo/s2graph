package s2.counter.core

import java.util

import com.google.common.cache.{CacheBuilder, CacheLoader, LoadingCache}
import s2.counter.core.TimedQualifier.IntervalUnit.IntervalUnit

import scala.collection.JavaConversions._

/**
 * Created by hsleep(honeysleep@gmail.com) on 15. 5. 27..
 */
case class ExactQualifier(tq: TimedQualifier, dimKeyValues: Map[String, String], dimension: String) {
  def checkDimensionEquality(dimQuery: Map[String, Set[String]]): Boolean = {
//    println(s"self: $dimKeyValues, query: $dimQuery")
    dimQuery.size == dimKeyValues.size && {
      for {
        (k, v) <- dimKeyValues
      } yield {
        dimQuery.get(k).exists(qv => qv.isEmpty || qv.contains(v))
      }
    }.forall(x => x)
  }
}

object ExactQualifier {
  val cache: LoadingCache[String, Map[String, String]] = CacheBuilder.newBuilder()
    .maximumSize(10000)
    .build(
      new CacheLoader[String, Map[String, String]]() {
        def load(s: String): Map[String, String] = {
          strToDimensionMap(s)
        }
      }
    )

  def apply(tq: TimedQualifier, dimension: String): ExactQualifier = {
    ExactQualifier(tq, cache.get(dimension), dimension)
  }

  def apply(tq: TimedQualifier, dimKeyValues: Map[String, String]): ExactQualifier = {
    ExactQualifier(tq, dimKeyValues, makeDimensionStr(dimKeyValues))
  }

  def makeSortedDimension(dimKeyValues: Map[String, String]): Iterator[String] = {
    val sortedDimKeyValues = new util.TreeMap[String, String](dimKeyValues)
    sortedDimKeyValues.keysIterator ++ sortedDimKeyValues.valuesIterator
  }

  def makeDimensionStr(dimKeyValues: Map[String, String]): String = {
    makeSortedDimension(dimKeyValues).mkString(".")
  }

  def getQualifiers(intervals: Seq[IntervalUnit], ts: Long, dimKeyValues: Map[String, String]): Seq[ExactQualifier] = {
    for {
      tq <- TimedQualifier.getQualifiers(intervals, ts)
    } yield {
      ExactQualifier(tq, dimKeyValues, makeDimensionStr(dimKeyValues))
    }
  }

  def strToDimensionMap(dimension: String): Map[String, String] = {
    val dimSp = {
      val sp = dimension.split('.')
      if (dimension == ".") {
        Array("", "")
      }
      else if (dimension.nonEmpty && dimension.last == '.') {
        sp ++ Array("")
      } else {
        sp
      }
    }
    val dimKey = dimSp.take(dimSp.length / 2)
    val dimVal = dimSp.takeRight(dimSp.length / 2)
    dimKey.zip(dimVal).toMap
  }
}
