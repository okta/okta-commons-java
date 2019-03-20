/*
 * Copyright 2018-Present Okta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.okta.commons.lang;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class Threads {

    private static final ExecutorService SYNCHRONOUS_EXECUTOR_SERVICE = createSynchronousExecutorService();

    private Threads() {}

    public static ExecutorService synchronousExecutorService() {
        return SYNCHRONOUS_EXECUTOR_SERVICE;
    }

    private static ExecutorService createSynchronousExecutorService() {
        ThreadPoolExecutor.CallerRunsPolicy runOnCurrentThreadHandler = new ThreadPoolExecutor.CallerRunsPolicy();
        return new ThreadPoolExecutor(0, 1, 0L, TimeUnit.SECONDS, new SynchronousQueue<>(), runOnCurrentThreadHandler) {
            @Override
            public void execute(Runnable command) {
                runOnCurrentThreadHandler.rejectedExecution(command, this);
            }
        };
    }
}