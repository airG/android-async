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

package com.airg.android.async.future;

import android.support.annotation.Nullable;

/**
 * The <code>Promise</code> interface is very similar to a {@link java.util.concurrent.Future}, but it provides
 * callbacks for completion, failure, and cancellation
 *
 * @author Mahram Z. Foadi
 * @author Jaap Sutter
 */

@SuppressWarnings({"UnusedDeclaration", "WeakerAccess"})
public interface Promise<RESULT> {

    /**
     * Report task completion
     *
     * @param result Task result
     */
    void success(RESULT result);

    /**
     * Report task failure
     *
     * @param t cause of the failure
     */
    void failed(@Nullable Throwable t);

    /**
     * Report task cancellation
     */
    void cancelled();

    /**
     * Add an {@link OnCompleteListener} callback to be notified of completion
     *
     * @param listener listener to notify on completion
     * @return this instance to chain more callbacks
     */
    Promise<RESULT> onComplete(final OnCompleteListener<RESULT> listener);

    /**
     * Add an {@link OnFailListener} callback to be notified on failure
     *
     * @param listener listener to notify on failure.
     * @return this instance to chain more callbacks
     */
    Promise<RESULT> onFail(final OnFailListener listener);

    /**
     * Add on {@link OnCancelListener} callback to be notified on cancellation
     *
     * @param listener listener to notify on cancellation
     * @return this instance to chain more callbacks
     */
    Promise<RESULT> onCancel(final OnCancelListener listener);

    /**
     * Are we there yet?
     *
     * @return <code>true</code> if task has run to completion (including due to failure or cancellation), <code>false</code> otherwise
     */
    boolean isDone();

    /**
     * Was the promise successfully completed?
     *
     * @return <code>true</code> if task was able to successfully obtain a result, <code>false</code> otherwise
     */
    boolean succeeded();

    /**
     * Did you keep your promise?
     *
     * @return <code>true</code> if task has failed, <code>false</code> otherwise
     */
    boolean isFailed();

    /**
     * Is the task cancelled?
     *
     * @return <code>true</code> if task has been cancelled, <code>false</code> otherwise
     */
    boolean isCancelled();

    /**
     * Task completion callback
     *
     * @param <RESULT> Expected result
     */
    interface OnCompleteListener<RESULT> {

        /**
         * Task completed
         *
         * @param result task result
         */
        void onComplete(RESULT result);
    }

    /**
     * Task failure callback
     */
    interface OnFailListener {
        /**
         * Task failed
         *
         * @param error failure cause
         */
        void onFailed(Throwable error);
    }

    /**
     * Task cancellation callback
     */
    interface OnCancelListener {
        /**
         * Task was cancelled
         */
        void onCancelled();
    }
}
