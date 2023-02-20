# High Level Weekly Outline

## Work assignments calendar

* First exercise set: published until 20 March (week 4) and delivered until 16 April (end of week 7).
* Second exercise set: published until 17 April (week 8) and delivered until 7 May (end of week 10).
* Third exercise set: published until 15 May (week 12) and delivered until 11 June (end of week 15).

## Weekly tentative schedule

### W1 - 2023-02-27
- Course introduction.
- Setting the knowledge baseline.
- Why have multiple sequential computations - e.g. use the chat server.
- Thread creation and basic synchronization in the JVM.

### W2 - 2023-03-06
- OS/Platform threads.
- JVM usage of platform threads.
- _uthreads_: context, context switch, scheduling, thread states

### W3 - 2023-03-13
- Data sharing between threads: identification (args, locals, fields, statics, captured context).
- Data sharing hazards and data synchronization.
- Mutual exclusion and locks in Java and in the JVM.
- Examples: echo server - count number of echos per client, count number of echos globally, caching.
- Thread coordination and control synchronization.
- The _synchronizer_ concept.
- The semaphore synchronizer

### W4 - 2023-03-20
- The monitor concept.
- Implementing custom synchronizers using monitors.

### W5 - 2023-03-27
- Implementing custom synchronizers using monitors.

### W6 - 2023-04-03
- What is a memory model and why do we need one?
- Easter break on Thursday

### W7 - 2023-04-10
- Easter break on Monday
- The Java Memory Model.

### W8 - 2023-04-17
- Lock-free algorithms and data structures.

### W9 - 2023-04-24
- The problem with blocking/synchronous I/O and coordination.
- Asynchronous I/O on the JVM - NIO2.
- The problem of defining computations using the callback model. Examples using state machines.

### W10 - 2023-05-01
- National holiday on Monday
- Futures and promises.
- `CompletableFuture` and its methods (`map`, `flatMap`, ...)
- Promises in Javascript and asynchronous functions.

### W11 - 2023-05-08
- Kotlin coroutines as a way to have suspendable sequential computations.
- Structured concurrency.
- Continuation Passing Style, coroutine suspension, and converting callbacks to suspend functions.

### W12 - 2023-05-15
- More of the above.
- Asynchronous coordination (_asynchronizers_).

### W13 - 2023-05-22
- Handling streams of data: kotlin flow.

### W14 - 2023-05-29
- More of the above.

### W15 - 2023-06-05
- Revisions and project support
- National holiday on Thursday

