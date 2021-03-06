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

import java.util.Locale;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Threadfactory for background executor
 * @author Mahram Z. Foadi
 */

@SuppressWarnings ( {"UnusedDeclaration", "WeakerAccess"})
final class CPUWorkerThreadFactory
  implements ThreadFactory {
    private final AtomicInteger nameCounter = new AtomicInteger ();
    private final String namePrefix;
    private final int threadPriority;

    CPUWorkerThreadFactory () {
        this ("Worker");
    }

    CPUWorkerThreadFactory (final String name) {
        this (name, Thread.NORM_PRIORITY);
    }

    CPUWorkerThreadFactory (final String name, final int priority) {
        namePrefix = name;
        threadPriority = priority;
    }

    @Override public Thread newThread (@NonNull final Runnable runnable) {
        final Thread thread = new Thread (runnable);
        thread.setDaemon (true);
        thread.setName (workerName(namePrefix, nameCounter.getAndIncrement ()));
        thread.setPriority (threadPriority);
        return thread;
    }

    static String workerName (final String name, final int number) {
        return String.format (Locale.ENGLISH, "%s[%s]", name, number);
    }
}
