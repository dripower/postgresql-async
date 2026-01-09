package com.github.mauricio.async.db.util

import java.util.LinkedHashMap

// --- Single-Threaded W-TinyLFU Implementation inspired by caffeine ---
private[db] class WTinyLFUCache[V](capacity: Int) {
  // Ratios: Window 1%, Protected 80% of Main, Probation 20% of Main
  // Caffeine ratios: Window 1%, Main 99% (Main is 80% Protected / 20% Probation)
  private val windowCap    = Math.max(1, (capacity * 0.01).toInt)
  private val mainCap      = capacity - windowCap
  private val protectedCap = (mainCap * 0.8).toInt
  private val probationCap = mainCap - protectedCap

  private val window    = new LinkedHashMap[String, V](windowCap + 1, 0.75f, true)
  private val probation =
    new LinkedHashMap[String, V](probationCap + 1, 0.75f, true)
  private val protectedArea =
    new LinkedHashMap[String, V](protectedCap + 1, 0.75f, true)
  private val sketch = new FrequencySketch(capacity)

  def get(key: String): Option[V] = {
    sketch.increment(key)
    if (window.containsKey(key)) return Option(window.get(key))
    if (protectedArea.containsKey(key)) return Option(protectedArea.get(key))
    if (probation.containsKey(key)) {
      val value = probation.remove(key)
      promoteToProtected(key, value)
      return Option(value)
    }
    None
  }

  /**
   * @return
   *   (OldValue, EvictedValue)
   */
  def put(key: String, value: V): (Option[V], Option[V]) = {
    sketch.increment(key)

    // 1. If key already exists, update it and return (Some(old), None)
    val existing = findAndRemove(key)
    if (existing.isDefined) {
      window.put(
        key,
        value
      ) // Re-insert updated item into window (standard Caffeine behavior)
      (existing, None)
    } else {
      // 2. New Entry: Admit to Window
      window.put(key, value)

      // 3. Handle Window Overflow
      if (window.size() > windowCap) {
        val winIt     = window.entrySet().iterator()
        val winVictim = winIt.next()
        winIt.remove()

        val evicted = admitToMain(winVictim.getKey, winVictim.getValue)
        return (None, evicted)
      }

      (None, None)
    }
  }

  private def findAndRemove(key: String): Option[V] = {
    Option(window.remove(key))
      .orElse(Option(protectedArea.remove(key)))
      .orElse(Option(probation.remove(key)))
  }

  private def promoteToProtected(key: String, value: V): Unit = {
    protectedArea.put(key, value)
    if (protectedArea.size() > protectedCap) {
      val demoted = protectedArea.entrySet().iterator().next()
      protectedArea.remove(demoted.getKey)
      probation.put(demoted.getKey, demoted.getValue)
      // If probation was already at capacity, it might now be size probationCap + 1.
      // This is handled during the next admitToMain call or can be capped here.
      if (probation.size() > probationCap + (mainCap * 0.05)) { // Small buffer
        val probIt = probation.entrySet().iterator()
        probIt.next(); probIt.remove()
      }
    }
  }

  private def admitToMain(key: String, value: V): Option[V] = {
    if ((probation.size() + protectedArea.size()) < mainCap) {
      probation.put(key, value)
      return None
    }

    val probIt = probation.entrySet().iterator()
    if (!probIt.hasNext) return Some(value)

    val probVictim = probIt.next()
    if (sketch.frequency(key) > sketch.frequency(probVictim.getKey)) {
      probation.remove(probVictim.getKey)
      probation.put(key, value)
      Some(probVictim.getValue)
    } else {
      Some(value)
    }
  }

  def contains(key: String): Boolean =
    window.containsKey(key) || probation.containsKey(key) || protectedArea
      .containsKey(key)

  def size: Int = window.size() + probation.size() + protectedArea.size()

  def remove(key: String) = {
    window.remove(key)
    probation.remove(key)
    protectedArea.remove(key)
  }
}

class FrequencySketch(capacity: Int) {
  private val size = {
    var v = capacity - 1; v |= v >> 1; v |= v >> 2; v |= v >> 4; v |= v >> 8;
    v |= v >> 16; v + 1
  }
  private val table                = new Array[Byte](size)
  private val seeds                = Array(0xc3a5c85c, 0x41c64e6d, 0x14f5698d, 0x944f24f6)
  def increment(key: String): Unit = seeds.foreach(s => {
    val h = (key.hashCode ^ s) & (size - 1);
    if (table(h) < 15) table(h) = (table(h) + 1).toByte
  })
  def frequency(key: String): Int =
    seeds.map(s => table((key.hashCode ^ s) & (size - 1)) & 0xf).min
}
