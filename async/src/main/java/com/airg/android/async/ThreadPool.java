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

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Synchronized;

/**
 Created by mahra on 2016-09-22.
 */
@SuppressWarnings ( {"UnusedDeclaration", "WeakerAccess"})
public final class ThreadPool {

    private static final Executor UI         = new UIThreadExecutor ();
    private static ExecutorService BACKGROUND;

    public static void fg (@NonNull final Runnable runnable) {
        UI.execute (runnable);
    }

    @Synchronized
    public static void bg (@NonNull final Runnable runnable) {
        background ().execute (runnable);
    }

    @Synchronized
    public static Future<?> submit (@NonNull final Runnable runnable) {
        return background ().submit (runnable);
    }

    @Synchronized
    public static <T> Future<T> submit (@NonNull final Callable<T> callable) {
        return background ().submit (callable);
    }

    @Synchronized
    public static <T> Future<T> submit (@NonNull final Runnable runnable, T result) {
        return background ().submit (runnable, result);
    }

    @Synchronized
    public static ExecutorService background () {
        if (null == BACKGROUND)
            init (null);

        return BACKGROUND;
    }

    @Synchronized
    public static void init (final Config config) {
        if (null != BACKGROUND)
            throw new IllegalStateException ("Thread pool already initialized. You should call this method before " +
                                             "any other calls to this class' methods" );

        final Config initConfig = null == config ? new Config () : config;

        final int poolSize = initConfig.overRidePoolSize > 0
                             ? initConfig.overRidePoolSize
                             : Math.max (1, Runtime.getRuntime ().availableProcessors () - 1);
        BACKGROUND = Executors.newFixedThreadPool (poolSize,
                                                   new CPUWorkerThreadFactory (initConfig.workerThreadNamePrefix,
                                                                               initConfig.backgroundThreadPriority));
    }

    @Builder
    @NoArgsConstructor (access = AccessLevel.PRIVATE)
    @AllArgsConstructor (access = AccessLevel.PACKAGE)
    public static class Config {
        private String workerThreadNamePrefix   = "AsyncWorker";
        private int    overRidePoolSize         = 0;
        private int    backgroundThreadPriority = Thread.NORM_PRIORITY;
    }
}
