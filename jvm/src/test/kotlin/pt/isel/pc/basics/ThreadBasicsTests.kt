package pt.isel.pc.basics

import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.time.Duration
import kotlin.test.assertEquals

private val log = LoggerFactory.getLogger(ThreadBasicsTests::class.java)
private fun threadMethod() {
    val name = Thread.currentThread().name
    log.info("Running on thread '{}'", name)
    Thread.sleep(Duration.ofSeconds(2).toMillis())
}

class ThreadBasicsTests {

    @Test
    fun thread_create_and_synchronization() {
        var anInteger = 0
        val th = Thread {
            anInteger = 1
        }
        th.start()
        th.join()
        assertEquals(1, anInteger)
    }

    @Test
    fun create_start_and_join_with_new_thread() {
        val name = Thread.currentThread().name
        log.info("Starting test on thread '{}'", name)

        // We create a thread by creating a Thread object, passing in a method reference
        // Notice that we aren't calling `threadMethod`; we are passing a *reference* to the method into the
        // `Thread` constructor.
        val th = Thread(::threadMethod)
        log.info("New thread created but not yet started")

        // By default, threads are not ready to run after they are created.
        // Only after Thread#start is called is the thread considered in the "ready" state.
        th.start()
        log.info("New thread started, waiting for it to end")

        // The Thread#join can be used to synchronize with the thread termination.
        // Thread#join will only return after
        // - the *referenced* thread ends
        // - or the *calling* thread is interrupted
        // - or the optional timeout elapses

        th.join()
        log.info("New thread ended, finishing test")
    }
    // When running this example, notice:
    // - The log messages contain the thread name in bracket.
    // - The test method is started on a "main" (or "Test worker") thread.
    // - However the log inside `threadMethod` is issued on a "Thread-" thread

    @Test
    fun we_can_have_multiple_threads_running_the_same_method() {
        val name = Thread.currentThread().name
        log.info("Starting test on thread '{}'", name)

        // We can create multiples threads referencing the same method
        val ths = listOf(
            Thread(::threadMethod),
            Thread(::threadMethod),
            Thread(::threadMethod)
        )
        ths.forEach { thread -> thread.start() }
        log.info("New threads started, waiting for them to end")

        ths.forEach { thread -> thread.join() }
        log.info("New threads ended, finishing test")
    }

    @Test
    fun create_thread_using_a_lambda() {
        val localVariableOfMainThread = 42
        log.info("Starting test on thread '{}'", Thread.currentThread().name)

        // Threads can be created by providing a lambda expression (here we use Kotlin's trailing lambda syntax)
        // Note that a lambda expression can use variables from the *enclosing scope*,
        // such as `localVariableOfMainThread`
        // This is simultaneously useful and dangerous, since those *local* variables will now
        // be accessible from *multiple* threads.
        val th = Thread {
            // Notice how in this thread we are using a local variable from a different thread,
            // (the main thread).
            log.info(
                "Running on thread '{}', localVariableOfMainThread = {}",
                Thread.currentThread().name,
                localVariableOfMainThread
            )
            Thread.sleep(Duration.ofSeconds(2).toMillis())
        }
        th.start()
        th.join()
        log.info("New thread ended, finishing test")
    }

    internal class IntHolder(var value: Int)

    @Test
    fun create_thread_using_a_lambda_and_a_mutable_shared_variable() {
        var localVariableOfMainThread = 42
        log.info("Starting test on thread '{}'", Thread.currentThread().name)

        // Threads can be created by providing a lambda expression
        // Note that a lambda expression can use variables from the *enclosing scope*,
        // such as `localVariableOfMainThread`
        // This is simultaneously useful and dangerous, since those *local* variables will now
        // be accessible from *multiple* threads.
        val th = Thread {

            // Notice how in this thread we are using a local variable from a different thread,
            // (the main thread).
            log.info(
                "Running on thread '{}', localVariableOfMainThread = {}",
                Thread.currentThread().name,
                localVariableOfMainThread
            )
            Thread.sleep(Duration.ofSeconds(2).toMillis())
        }
        th.start()
        // Here we mutate `localVariableOfMainThread` "at the same time" the created thread observes that field
        // So, what will be the observed value? 42 or 43?
        // Do we have any guarantees that the same value will be observed for all runs of this test?
        localVariableOfMainThread = 43
        th.join()
        log.info("New thread ended, finishing test")
    }

    // Threads can also be defined by deriving from the Thread class (this is the JVM after all)
    internal class MyThread : Thread() {
        override fun run() {
            log.info("Running on thread MyThread - '{}'", currentThread().name)
            sleep(Duration.ofSeconds(2).toMillis())
        }
    }

    // Don't be like me - don't mix up `run` with `start`
    // - `start` - method that transitions the thread into the ready state
    // - `run` - method that is executed on the thread

    @Test
    fun create_thread_using_derived_classes() {
        log.info("Starting test on thread '{}'", Thread.currentThread().name)
        val th = MyThread()
        th.start()
        th.join()
        log.info("New thread ended, finishing test")
    }
}