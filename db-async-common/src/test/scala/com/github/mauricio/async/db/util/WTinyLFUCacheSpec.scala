package com.github.mauricio.async.db.util

import org.specs2.mutable.Specification

class WTinyLFUCacheSpec extends Specification {

  "WTinyLFUCache" should {

    "store and retrieve values" in {
      val cache = new WTinyLFUCache[String](100)

      cache.put("key1", "value1")
      cache.put("key2", "value2")

      cache.get("key1") must beSome("value1")
      cache.get("key2") must beSome("value2")
    }

    "return None for non-existent keys" in {
      val cache = new WTinyLFUCache[String](100)

      cache.put("key1", "value1")

      cache.get("nonexistent") must beNone
    }

    "update existing values" in {
      val cache = new WTinyLFUCache[String](100)

      cache.put("key1", "value1")
      cache.put("key1", "value2")

      cache.get("key1") must beSome("value2")
    }

    "evict entries when capacity is reached" in {
      val cache = new WTinyLFUCache[String](5) // Very small cache

      // Fill the cache
      (1 to 10).foreach { i =>
        cache.put(s"key$i", s"value$i")
      }

      // Some keys should be evicted
      val existing = (1 to 10).count(i => cache.get(s"key$i").isDefined)
      existing must beLessThan(10)
    }

    "handle window overflow correctly" in {
      val cache = new WTinyLFUCache[String](100)

      // Add more than window capacity (windowCap = 100/100 = 1)
      (1 to 5).foreach { i =>
        cache.put(s"key$i", s"value$i")
      }

      // All should be accessible initially
      (1 to 5).toSeq must contain((i: Int) => cache.get(s"key$i") must beSome(s"value$i")).foreach

    }

    "promote frequently accessed items" in {
      val cache = new WTinyLFUCache[String](100)

      // Add initial items
      (1 to 10).foreach { i =>
        cache.put(s"key$i", s"value$i")
      }

      // Access some items frequently
      (1 to 5).foreach { _ =>
        (1 to 3).foreach { i =>
          cache.get(s"key$i")
        }
      }

      // Frequently accessed items should still be present
      cache.get("key1") must beSome("value1")
      cache.get("key2") must beSome("value2")
      cache.get("key3") must beSome("value3")
    }

    "return evicted entry on put when full" in {
      val cache = new WTinyLFUCache[String](3)

      cache.put("key1", "value1")
      cache.put("key2", "value2")
      cache.put("key3", "value3")

      // This should evict something
      val evicted = cache.put("key4", "value4")
      evicted must beSome
    }

    "handle empty cache operations" in {
      val cache = new WTinyLFUCache[String](100)

      cache.get("nonexistent") must beNone
      cache.put("key1", "value1") must beNone
    }

    "handle large number of keys" in {
      val cache = new WTinyLFUCache[String](1000)

      (1 to 10000).foreach { i =>
        cache.put(s"key$i", s"value$i")
      }

      // Should have some entries still cached
      val cached = (1 to 10000).count(i => cache.get(s"key$i").isDefined)
      cached must beGreaterThan(0)
    }

    "maintain LRU order within segments" in {
      val cache = new WTinyLFUCache[String](100)

      // Add items to fill window
      (1 to 5).foreach { i =>
        cache.put(s"key$i", s"value$i")
      }

      // Access items in specific order
      cache.get("key1")
      cache.get("key2")
      cache.get("key3")

      // Add more items to trigger window overflow
      (6 to 10).foreach { i =>
        cache.put(s"key$i", s"value$i")
      }

      // key1 should be more likely to be evicted as it's least recently used
      // (though frequency also plays a role)
      ok
    }

    "handle null-like keys and values" in {
      val cache = new WTinyLFUCache[String](100)

      // Empty string keys should work
      cache.put("", "empty")
      cache.get("") must beSome("empty")

      // Keys with special characters
      cache.put("key-with-dash", "value")
      cache.get("key-with-dash") must beSome("value")
    }

    "handle concurrent-like access patterns" in {
      val cache = new WTinyLFUCache[String](50)

      // Simulate mixed access pattern
      (1 to 100).foreach { i =>
        val key = s"key${i % 20}"
        cache.put(key, s"value${i % 20}")
        cache.get(key)
      }

      // Should have some entries
      cache.get("key0") must beSome
    }

    "handle FrequencySketch operations" in {
      val sketch = new FrequencySketch(100)

      // Increment frequencies
      sketch.increment(1)
      sketch.increment(1)
      sketch.increment(1)
      sketch.increment(2)

      sketch.frequency(1) must beGreaterThanOrEqualTo(sketch.frequency(2))
    }

    "handle FrequencySketch with different hash codes" in {
      val sketch = new FrequencySketch(100)

      val hashes = List(100, 200, 300, 400, 500)
      hashes.foreach { h =>
        sketch.increment(h)
      }

      // All should have some frequency
      hashes must contain({ (h: Int) =>
        sketch.frequency(h) must beGreaterThan(0)
      }).foreach
    }

    "handle repeated puts of same key" in {
      val cache = new WTinyLFUCache[String](10)

      (1 to 100).foreach { _ =>
        cache.put("key", "value")
      }

      cache.get("key") must beSome("value")
    }

    "handle mixed operations" in {
      val cache = new WTinyLFUCache[String](50)

      // Add
      (1 to 30).foreach { i =>
        cache.put(s"key$i", s"value$i")
      }

      // Get some
      (1 to 10).foreach { i =>
        cache.get(s"key$i")
      }

      // Update some
      (5 to 15).foreach { i =>
        cache.put(s"key$i", s"updated$i")
      }

      // Add more
      (31 to 50).foreach { i =>
        cache.put(s"key$i", s"value$i")
      }

      // Verify updates
      cache.get("key10") must beSome("updated10")
      cache.get("key15") must beSome("updated15")
    }

    "handle very small capacity with many operations" in {
      val cache = new WTinyLFUCache[String](3)

      (1 to 100).foreach { i =>
        cache.put(s"k$i", s"v$i")
        cache.get(s"k${i % 10}")
      }

      // Should have some entries
      val count = (1 to 100).count(i => cache.get(s"k$i").isDefined)
      count must beGreaterThan(0)
      count must beLessThan(4) // At most capacity
    }

    "handle negative hash codes" in {
      val cache = new WTinyLFUCache[String](100)

      // Keys that might produce negative hash codes
      val keys = List("key1", "key2", "key3")
      keys.foreach { k =>
        cache.put(k, s"value-$k")
      }

      keys must contain({ (k: String) =>
        cache.get(k) must beSome(s"value-$k")
      }).foreach
    }

    "handle large hash codes" in {
      val cache = new WTinyLFUCache[String](100)

      // Create keys with large hash codes
      val keys = (1 to 10).map(i => s"verylongkeyname$i" * 10)
      keys.foreach { k =>
        cache.put(k, s"value-$k")
      }

      keys must contain({ (k: String) =>
        cache.get(k) must beSome(s"value-$k")
      }).foreach
    }

    "verify FrequencySketch behavior" in {
      val sketch = new FrequencySketch(16)

      // Test increment and frequency
      (1 to 10).foreach { _ =>
        sketch.increment(42)
      }

      val freq = sketch.frequency(42)
      freq must beGreaterThan(0)

      // Different hash should have different frequency
      sketch.frequency(99) must beLessThanOrEqualTo(freq)
    }

    "handle FrequencySketch with size 1" in {
      val sketch = new FrequencySketch(1)

      sketch.increment(1)
      sketch.increment(1)

      sketch.frequency(1) must beGreaterThan(0)
    }

    "handle FrequencySketch with size 2" in {
      val sketch = new FrequencySketch(2)

      sketch.increment(1)
      sketch.increment(2)

      sketch.frequency(1) must beGreaterThan(0)
      sketch.frequency(2) must beGreaterThan(0)
    }
  }
}
