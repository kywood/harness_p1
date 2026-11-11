package com.example.torrentstreamplayer.engine
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger
class NativeSessionLeaseTest {
    @Test fun concurrentOwnersShareOneCreationAndCloseOnlyAfterLastRelease() {
        val created = AtomicInteger(); val closed = AtomicInteger()
        val pool = NativeSessionLease<Any> { closed.incrementAndGet() }
        val barrier = CyclicBarrier(3)
        val values = CopyOnWriteArrayList<Any>()
        val failures = CopyOnWriteArrayList<Throwable>()
        val threads = List(3) {
            Thread {
                val owner = Any()
                try {
                    values += pool.acquire(owner) { created.incrementAndGet(); Thread.sleep(20); Any() }
                    barrier.await(3, TimeUnit.SECONDS)
                    pool.release(owner)
                } catch (error: Throwable) { failures += error }
            }.apply { start() }
        }
        threads.forEach { it.join(5000); assertFalse(it.isAlive) }
        assertTrue(failures.toString(), failures.isEmpty())
        assertEquals(1, created.get()); assertEquals(1, closed.get())
        assertTrue(values.all { it === values.first() }); assertEquals(0, pool.ownerCount())
    }
    @Test fun stoppingOneTorrentDoesNotCloseAnotherTorrentSession() {
        var closed = 0; val pool = NativeSessionLease<Any> { closed++ }
        val first = Any(); val second = Any()
        val session = pool.acquire(first) { Any() }
        assertSame(session, pool.acquire(second) { error("Must reuse session") })
        pool.release(first); assertEquals(0, closed); assertEquals(1, pool.ownerCount())
        pool.release(second); assertEquals(1, closed); assertEquals(0, pool.ownerCount())
        pool.release(second); assertEquals(1, closed)
    }
    @Test fun failedCreationDoesNotLeaveOwnerOrBrokenSession() {
        val pool = NativeSessionLease<Any> {}
        assertThrows(IllegalStateException::class.java) { pool.acquire(Any()) { error("Failed") } }
        assertEquals(0, pool.ownerCount())
        val owner = Any(); pool.acquire(owner) { Any() }; pool.release(owner)
    }
    @Test fun failedCloseKeepsTheSessionAvailableForSafeRetry() {
        var fail = true
        val pool = NativeSessionLease<Any> { if (fail) error("Busy") }
        val owner = Any(); val session = pool.acquire(owner) { Any() }
        assertThrows(IllegalStateException::class.java) { pool.release(owner) }
        assertSame(session, pool.acquire(owner) { error("Do not create another session") })
        fail = false; pool.release(owner); assertEquals(0, pool.ownerCount())
    }
}
