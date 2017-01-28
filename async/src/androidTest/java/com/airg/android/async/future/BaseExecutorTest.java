/*
 * ****************************************************************************
 *   Copyright  2017 airG Inc.                                                 *
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

package com.airg.android.async.future;

import android.support.test.runner.AndroidJUnit4;

import org.junit.runner.RunWith;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by mahramf.
 */
class BaseExecutorTest {
    static final class EchoTask<VALUE> implements Callable<VALUE> {

        private final VALUE value;
        private final Exception exception;
        private final long duration;

        EchoTask(final VALUE v, final long dur) {
            this(v, dur, null);
        }

        EchoTask(final VALUE v, final long dur, final Exception e) {
            value = v;
            duration = dur;
            exception = e;
        }

        @Override
        public VALUE call() throws Exception {
            Thread.sleep(duration);

            if (null != exception) throw exception;

            return value;
        }
    }
}
