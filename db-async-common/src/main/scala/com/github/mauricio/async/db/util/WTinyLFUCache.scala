package com.github.mauricio.async.db.util

import java.util.{LinkedHashMap => JLinkedHashMap}
import java.util.Map.Entry

import java.util.{LinkedHashMap => JLinkedHashMap}
import java.util.Map.Entry

/**
 * Caffeine WTinyLFU like cache, capacity should not be too small
 */
private[db] class WTinyLFUCache[V](capacity: Int) {
  private val windowCap    = Math.max(1, capacity / 100)
  private val mainCap      = Math.max(1, capacity - windowCap)
  private val protectedCap = Math.max(1, (mainCap * 0.8).toInt)
  private val probationCap = Math.max(1, mainCap - protectedCap)

  private val sketch = new FrequencySketch(capacity)

  private def createLRU[T](cap: Int) = new JLinkedHashMap[String, T](cap, 0.75f, true) {
    override def removeEldestEntry(eldest: Entry[String, T]): Boolean = size() > cap
  }

  private val window       = createLRU[V](windowCap)
  private val probation    = createLRU[V](probationCap)
  private val protectedSeg = createLRU[V](protectedCap)

  def get(key: String): Option[V] = {
    sketch.increment(key.hashCode)

    val w = window.get(key)
    if (w != null) return Some(w)

    val p1 = protectedSeg.get(key)
    if (p1 != null) return Some(p1)

    val p2 = probation.get(key)
    if (p2 != null) {
      promote(key, p2)
      return Some(p2)
    }
    None
  }

  def put(key: String, value: V): Option[V] = {
    sketch.increment(key.hashCode)

    if (window.containsKey(key)) { window.put(key, value); None }
    else if (protectedSeg.containsKey(key)) { protectedSeg.put(key, value); None }
    else if (probation.containsKey(key)) { promote(key, value); None }
    else {
      var evicted: Option[V] = None
      if (window.size() >= windowCap) {
        val it = window.entrySet().iterator()
        if (it.hasNext) {
          val oldest = it.next()
          it.remove()
          evicted = admitToMain(oldest.getKey, oldest.getValue)
        }
      }
      window.put(key, value)
      evicted
    }
  }

  private def admitToMain(key: String, value: V): Option[V] = {
    if (probation.size() < probationCap) {
      probation.put(key, value)
      None
    } else {
      val it = probation.entrySet().iterator()
      if (!it.hasNext) return Some((value)) // Should not happen with cap >= 1

      val victim = it.next()
      if (sketch.frequency(key.hashCode) > sketch.frequency(victim.getKey.hashCode)) {
        it.remove()
        probation.put(key, value)
        Some(victim.getValue)
      } else {
        Some(value)
      }
    }
  }

  private def promote(key: String, value: V): Unit = {
    probation.remove(key)
    if (protectedSeg.size() >= protectedCap) {
      val it = protectedSeg.entrySet().iterator()
      if (it.hasNext) {
        val demoted = it.next()
        it.remove()
        probation.put(demoted.getKey, demoted.getValue)
      }
    }
    protectedSeg.put(key, value)
  }
}

class FrequencySketch(size: Int) {
  private val tableSize = if (size < 2) 2 else Integer.highestOneBit(size - 1) << 1
  private val mask      = tableSize - 1
  private val table     = new Array[Byte](tableSize)

  def increment(h: Int): Unit = {
    for (i <- 0 until 4) {
      val idx = (h ^ (h >>> (i * 8))) & mask
      if (table(idx) < 127) table(idx) = (table(idx) + 1).toByte
    }
  }

  def frequency(h: Int): Int = {
    var min = 127
    for (i <- 0 until 4) {
      val idx = (h ^ (h >>> (i * 8))) & mask
      val f   = table(idx).toInt
      if (f < min) min = f
    }
    min
  }
}
