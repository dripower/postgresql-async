package com.github.mauricio.async.db.util

import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import scala.util.Random

class WTinyLFUCacheSpec extends Specification {

  trait Context extends Scope {
    val capacity = 2000
    val cache    = new WTinyLFUCache[String](capacity)
  }

  "WTinyLFUCache" should {

    "retain hot elements" in new Context {
      val totalRequests = 100000
      val hotKeyRange   = 500  // These keys should mostly be retained
      val coldKeyRange  = 5000 // These should mostly be evicted
      for (_ <- 1 to totalRequests) {
        val key =
          if (Random.nextDouble() < 0.8) s"hot-${Random.nextInt(hotKeyRange)}"
          else s"cold-${Random.nextInt(coldKeyRange)}"
        // Put or access hot key
        if (cache.get(key).isEmpty) cache.put(key, "data")
      }
      val retainedHot =
        (0 until hotKeyRange).count(i => cache.contains(s"hot-$i"))

      val retainedCold =
        (0 until coldKeyRange).count(i => cache.contains(s"cold-$i"))

      val total = cache.size
      println(s"Capacity:${capacity}, hot retain: ${retainedHot}/${hotKeyRange}, cold: ${retainedCold}/${coldKeyRange}")
      val coldKeys = (0 until coldKeyRange).filter(i => cache.contains(s"cold-$i"))
      (retainedHot * 1.0 / hotKeyRange) must >=(0.99)
      (total * 1.0 / capacity) must >=(0.9)
    }
  }
}
