/*
 * ****************************************************************************
 *   Copyright  2016 airG Inc.                                                 *
 *                                                                             *
 *   Licensed under the Apache License, Version 2.0 (the "License");           *
 *   you may not use this file except in compliance with the License.          *
 *   You may obtain a copy of the License at                                   *
 *                                                                             *
 *       http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                             *
 *   Unless required by applicable law or agreed to in writing, software       *
 *   distributed under the License is distributed on an "AS IS" BASIS,         *
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  *
 *   See the License for the specific language governing permissions and       *
 *   limitations under the License.                                            *
 * ***************************************************************************
 */

package com.airg.android.async;

import android.support.annotation.NonNull;

import com.airg.android.device.Device;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Synchronized;

/**
 This class contains an {@link Executor} that executes code on the UI Thread and an {@link ExecutorService} that allows
 for code to be executed on a background thread via a Threadpool. This class does not require a configuration (i.e. is
 usable immediately without calling {@link ThreadPool#init(Config)}), but if you'd like to override the default
 behavior, you may do so as long as the initialization is done before any tasks have been submitted for background
 execution.

 @author Mahram Z. Foadi
 */
@SuppressWarnings ( {"UnusedDeclaration", "WeakerAccess"})
public final class ThreadPool {

    private static final Executor UI = new UIThreadExecutor ();
    private static ExecutorService BACKGROUND;

    /**
     Submit a {@link Runnable} to be executed on the UI thread.

     @param runnable
     task to execute
     */
    public static void fg (@NonNull final Runnable runnable) {
        UI.execute (runnable);
    }

    /**
     Submit a {@link Runnable} to be executed on a background thread

     @param runnable
     */
    @Synchronized
    public static void bg (@NonNull final Runnable runnable) {
        background ().execute (runnable);
    }

    /**
     Executes a {@link Runnable} on the background thread.
     See {@link ExecutorService#submit(Runnable)}
     */
    @Synchronized
    public static Future<?> submit (@NonNull final Runnable runnable) {
        return background ().submit (runnable);
    }

    /**
     Executes a {@link Runnable} on the background thread.
     See {@link ExecutorService#submit(Callable)}
     */
    @Synchronized
    public static <T> Future<T> submit (@NonNull final Callable<T> callable) {
        return background ().submit (callable);
    }

    /**
     Executes a {@link Runnable} on the background thread.
     See {@link ExecutorService#submit(Runnable, Object)}
     */
    @Synchronized
    public static <T> Future<T> submit (@NonNull final Runnable runnable, T result) {
        return background ().submit (runnable, result);
    }

    /**
     Get the background {@link ExecutorService}

     @return the current background <code>ExecutorService</code>
     */
    @Synchronized
    public static ExecutorService background () {
        if (null == BACKGROUND)
            init (null);

        return BACKGROUND;
    }

    /**
     Get the foreground (UI Thread) {@link Executor}

     @return the UI Thread <code>Executor</code>
     */
    @Synchronized
    public static Executor foreground () {
        return UI;
    }

    /**
     Initialize the Threadpool with non-default values.

     @param config
     Threadpool configuration

     @throws IllegalStateException
     if the Threadpool has already been initialized via either calling {@link #init(Config)} or {@link #background()}
     */
    @Synchronized
    public static void init (final Config config) {
        if (null != BACKGROUND)
            throw new IllegalStateException ("Thread pool already initialized. You should call this method before " +
                                             "any other calls to this class' methods");

        final Config initConfig = null == config ? new Config () : config;

        final int poolSize = initConfig.overridePoolSize > 0
                             ? initConfig.overridePoolSize
                             : Math.max (1, Device.CPU_COUNT - 1);

        if (poolSize <= 0)
            throw new IllegalArgumentException ("Invalid pool size: " + poolSize);

        BACKGROUND = Executors.newFixedThreadPool (poolSize,
                                                   new CPUWorkerThreadFactory (initConfig.workerThreadNamePrefix,
                                                                               initConfig.backgroundThreadPriority));
    }

    private static final String DEFAULT_THREAD_PREFIX   = "AsyncWorker";
    private static final int    DEFAULT_THREAD_PRIORITY = Thread.NORM_PRIORITY;
    private static final int    NO_POOL_SIZE_OVERRIDE   = 0;

    /**
     Threadpool configuration. Use {@link ThreadPool.Config.ConfigBuilder} to create a different configuration.
     */
    @NoArgsConstructor (access = AccessLevel.PRIVATE)
    @AllArgsConstructor (access = AccessLevel.PACKAGE)
    public static class Config {
        private String workerThreadNamePrefix   = DEFAULT_THREAD_PREFIX;
        private int    overridePoolSize         = NO_POOL_SIZE_OVERRIDE;
        private int    backgroundThreadPriority = DEFAULT_THREAD_PRIORITY;

        /**
         Get a new {@link ConfigBuilder}

         @return a {@link ConfigBuilder}
         */
        public static ConfigBuilder builder () {
            return new ConfigBuilder ();
        }

        /**
         Threadpool {@link Config} builder. Use to customize the behavior of the {@link ThreadPool} instance.
         */
        public static class ConfigBuilder {
            private String prefix         = DEFAULT_THREAD_PREFIX;
            private int    poolSize       = NO_POOL_SIZE_OVERRIDE;
            private int    threadPriority = DEFAULT_THREAD_PRIORITY;

            ConfigBuilder () {
            }

            /**
             The thread name prefix for the worker threads. Default is <code>AsyncWorker</code>

             @param workerThreadNamePrefix
             override prefix value

             @return this builder instance
             */
            public Config.ConfigBuilder workerThreadNamePrefix (final String workerThreadNamePrefix) {
                this.prefix = workerThreadNamePrefix;
                return this;
            }

            /**
             Use a custom thread pool size

             @param overridePoolSize
             custom value. The ideal value is the number of cores - 1.

             @return this builder instance
             */
            public Config.ConfigBuilder overridePoolSize (final int overridePoolSize) {
                this.poolSize = overridePoolSize;
                return this;
            }

            /**
             Override the thread priority.

             @param backgroundThreadPriority
             thread priority value. Either {@link Thread#MIN_PRIORITY}, {@link Thread#NORM_PRIORITY}, or {@link
             Thread#MAX_PRIORITY}.

             @return this builder instance
             */
            public Config.ConfigBuilder backgroundThreadPriority (final int backgroundThreadPriority) {
                this.threadPriority = backgroundThreadPriority;
                return this;
            }

            /**
             Create the specified configuration parameters.

             @return the generated {@link Config}
             */
            public Config build () {
                return new Config (prefix, poolSize, threadPriority);
            }
        }
    }
}
