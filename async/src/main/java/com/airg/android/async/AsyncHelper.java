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

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Looper;

import com.airg.android.device.ApiLevel;

/**
 * Multi-thread utility methods
 * @author Mahram Z. Foadi
 */
@SuppressWarnings ( {"UnusedDeclaration", "WeakerAccess"})
public final class AsyncHelper {
    @TargetApi (Build.VERSION_CODES.M)
    public static boolean isMainThread () {
        return ApiLevel.atLeast(Build.VERSION_CODES.M)
               ? Looper.getMainLooper ().isCurrentThread ()
               : Looper.myLooper () == Looper.getMainLooper ();
    }

    /**
     Ensures that the caller is not running on the main thread

     @throws IllegalStateException
     if the caller is running on the main thread
     */
    public static void assertNotMainThread () {
        if (!isMainThread ())
            return;

        throw new IllegalStateException ("This method should not be called from the main/UI thread.");
    }

    /**
     Ensures that the caller is running on the main thread

     @throws IllegalStateException
     if the caller is not running on the main thread
     */
    public static void assertMainThread () {
        if (isMainThread ())
            return;

        throw new IllegalStateException ("This method should be called from the main/UI thread.");
    }
}
