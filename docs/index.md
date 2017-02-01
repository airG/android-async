 [![Build Status](https://travis-ci.org/airG/android-async.svg?branch=master)](https://travis-ci.org/airG/android-async) [ ![Download](https://api.bintray.com/packages/airgoss/airGOss/async/images/download.svg) ](https://bintray.com/airgoss/airGOss/async/_latestVersion)

# Android Async
The airG android async library is a group of utilities for easier management of asynchronous tasks. This library provides an [Executor Service](https://developer.android.com/reference/java/util/concurrent/ExecutorService.html) as an appropriately sized thread pool for background execution of tasks (`Threadpool.bg()`). An [Executor](https://developer.android.com/reference/java/util/concurrent/Executor.html) is provided for executing tasks on the UI thread (`Threadpool.fg()`). 

To learn more, review the [javadoc](https://airg.github.io/android-async/javadoc/) for android-async.

There are also a few utility methods (in `AsyncHelper`) to ensure that code is running __on__ or __off__ the UI thread.

## [Threadpool](/javadoc/com/airg/android/async/ThreadPool.html)
The `Threadpool` class creates an appropriately sized thread pool (based on the number of CPU cores available) on which you can schedule background tasks. If you'd like to override the size of this thread pool, call the `Threadpool.init(Threadpool.Config config)` method ___prior___ to using the executors.

### Background Tasks
An [Executor Service](https://developer.android.com/reference/java/util/concurrent/ExecutorService.html) is initialized and available for background task execution. To schedule a task for background execution, use `Threadpool.bg()` or any of the various `Threadpool.submit()` methods. To interact directly with the `ExecutorService`, use `Threadpool.background()` to get the instance.

### Foreground Tasks
To run a task on the foreground, use the `Threadpool.fg()` method. Alternatively, you can use the `Threadpool.foreground()` method which returns the main thread [Executor](https://developer.android.com/reference/java/util/concurrent/Executor.html).

## [AsyncHelper](/javadoc/com/airg/android/async/AsyncHelper.html)
The `AsyncHelper` class includes a few static utility methods to determine whether code is running on the main thread as well as methods that ensure (by throwing exceptions) that certain code is running _on_ or _off_ the UI thread.

## [Promise](/javadoc/com/airg/android/async/future/Promise.html)
Think of `Promise` as a `Future` or `Runnable` that informs you when the execution is complete, cancelled, or failed. There are currently 2 implementations of the `Promise` interface

* [SimplePromise] (/javadoc/com/airg/android/async/future/SimplePromise.html) can be passed in to your background runnables, which will set the result (or error). The `SimplePromise` implementation will inform your callbacks of the result, error, or cancellation.
* [FuturePromise] (/javadoc/com/airg/android/async/future/FuturePromise.html) can be used exactly as you would use a `Future`. In fact, this class extends `FutureTask` to obtain the result and internally uses a `SimplePromise` to report the results.
* If neither class meets your exact needs, you can implement your own version of `Promise`.

## Usage
To use the _android-async_ library in your builds, add the following line to your Gradle build script:

`compile 'com.airg.android:async:+@aar'`

Or to download the library ![Download](https://api.bintray.com/packages/airgoss/airGOss/async/images/download.svg) ](https://bintray.com/airgoss/airGOss/async/_latestVersion)

## Contributions
Please refer to the [contribution instructions](https://airg.github.io/#contribute).
