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

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import java.util.concurrent.Executor;

/**
 Created by mahra on 2016-09-22.
 */
final class UIThreadExecutor
  implements Executor {
    private final Handler handler;

    UIThreadExecutor () {
        handler = new Handler (Looper.getMainLooper ());
    }

    @Override
    public void execute (@NonNull final Runnable runnable) {

        if (handler.getLooper () == Looper.myLooper ()) {
            runnable.run ();
        } else {
            handler.post (runnable);
        }
    }
}
