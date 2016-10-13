 [ ![Download](https://api.bintray.com/packages/airgoss/airGOss/async/images/download.svg) ](https://bintray.com/airgoss/airGOss/async/_latestVersion)

#Android Async
The airG android async library is a group of utilities for easier management of asynchronous tasks. This library provides an [Executor Service](https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ExecutorService.html) as an appropriately sized thread pool for background execution of tasks (`Threadpool.BACKGROUND`). An [Executor](https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/Executor.html) is provided for executing tasks on the UI thread (`Threadpool.UI`).

There are also a few utility methods (in `AsyncHelper`) to ensure that code is running __on__ or __off__ the UI thread.

##`Threadpool`
The `Threadpool` class creates an appropriately sized thread pool on which you can schedule background tasks. If you'd like to override the size of this thread pool, call the `Threadpool.init(Threadpool.Config config)` method ___prior___ to using the executors.

### Background Tasks
An [Executor Service](https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ExecutorService.html) is initialized and available for background task execution. To schedule a task for background execution, use `Threadpool.bg()`.

### Foreground Tasks
To run a task on the foreground, use the `Threadpool.fg()` method, which returns an [Executor](https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/Executor.html) that executes tasks on the UI thread.

##`AsyncHelper`
The `AsyncHelper` class includes a few static utility methods to determine whether code is running on the main thread as well as methods that ensure (by throwing exceptions) that certain code is running _on_ or _off_ the UI thread.

##Contributions
Contributions are appreciated and welcome. In order to contribute to this repo please follow these steps:

1. Fork the repo
1. Add this repo as the `upstream` repo in your fork (`git remote add upstream git@github.com:airG/android-async.git`)
1. Contribute (Be sure to format your code according to th included code style settings)
1. IMPORTANT: Rebase with upstream (`git pull --rebase upstream`)
1. Submit a pull request
