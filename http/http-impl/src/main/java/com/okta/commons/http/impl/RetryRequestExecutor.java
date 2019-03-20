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
package com.okta.commons.http.impl;

import com.okta.commons.http.HttpClientConfiguration;
import com.okta.commons.http.HttpHeaders;
import com.okta.commons.http.QueryString;
import com.okta.commons.http.Request;
import com.okta.commons.http.Response;
import com.okta.commons.http.RestException;
import com.okta.commons.lang.Assert;
import com.okta.commons.lang.Collections;
import com.okta.commons.lang.Strings;
import com.okta.commons.lang.Threads;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.okta.commons.http.RequestExecutor;

public final class RetryRequestExecutor implements RequestExecutor {

    private static final Logger log = LoggerFactory.getLogger(RetryRequestExecutor.class);

    /**
     * Maximum exponential back-off time before retrying a request
     */
    private static final int DEFAULT_MAX_BACKOFF_IN_MILLISECONDS = 20 * 1000;

    private static final int DEFAULT_MAX_RETRIES = 4;

    private int maxRetries = DEFAULT_MAX_RETRIES;

    private int maxElapsedMillis = 0;

    private final RequestExecutor delegate;

    public RetryRequestExecutor(HttpClientConfiguration clientConfiguration, RequestExecutor delegate) {
        this.delegate = delegate;

        if (clientConfiguration.getRetryMaxElapsed() >= 0) {
            maxElapsedMillis = clientConfiguration.getRetryMaxElapsed() * 1000;
        }

        if (clientConfiguration.getRetryMaxAttempts() > 0) {
            maxRetries = clientConfiguration.getRetryMaxAttempts();
        }
    }

    @Override
    public CompletableFuture<Response> executeRequestAsync(Request request, ExecutorService executorService) throws RestException {

        Assert.notNull(request, "Request argument cannot be null.");

        // Make a copy of the original request params and headers so that we can
        // permute them in the loop and start over with the original every time.
        final QueryString originalQuery = new QueryString();
        originalQuery.putAll(request.getQueryString());

        final HttpHeaders originalHeaders = new HttpHeaders();
        originalHeaders.putAll(request.getHeaders());

        final Timer timer = new Timer();

        return executeAsyncRequest(request, originalHeaders, originalQuery, 0, null, timer, executorService);

    }

    private CompletableFuture<Response> doRequest(final Request request,
                                                  final Response previousResponse,
                                                  final HttpHeaders originalHeaders,
                                                  final QueryString originalQuery,
                                                  int retryCount,
                                                  String requestId,
                                                  Timer timer,
                                                  ExecutorService executorService) {
        if (retryCount > 0) {
            // only the first request gets the callers executor, after that we continue on that same thread for each retry
            executorService = Threads.synchronousExecutorService();
            request.setQueryString(originalQuery);
            request.setHeaders(originalHeaders);

            try {
                InputStream content = request.getBody();
                if (content != null && content.markSupported()) {
                    content.reset();
                }
            } catch (IOException e) {
                throw new RestException("Unable to execute HTTP request: " + e.getMessage(), e);
            }

            try {
                // if we cannot pause, then return the original response
                pauseBeforeRetry(retryCount, previousResponse, timer.split());
            } catch (RestException e) {
                if (log.isDebugEnabled()) {
                    log.warn("Unable to pause for retry: {}", e.getMessage(), e);
                } else {
                    log.warn("Unable to pause for retry: {}", e.getMessage());
                }

                return CompletableFuture.completedFuture(previousResponse);
            }
        }

        // include X-Okta headers when retrying
        setOktaHeaders(request, requestId, retryCount);

        return delegate.executeRequestAsync(request, executorService);
    }

    private CompletableFuture<Response> executeAsyncRequest(final Request request,
                                                            final HttpHeaders originalHeaders,
                                                            final QueryString originalQuery,
                                                            final int retryCount,
                                                            final String requestId,
                                                            final Timer timer,
                                                            final ExecutorService executorService) {

        return doRequest(request, null, originalHeaders, originalQuery, retryCount, requestId, timer, executorService)
            .thenApply(response -> {

                if (shouldRetry(response, retryCount, timer.split())) {
                    String reqId = (requestId != null) ? requestId : getRequestId(response);
                    return doRequest(request, response, originalHeaders, originalQuery, retryCount+1, reqId, timer, executorService);
                }
                return CompletableFuture.completedFuture(response);
            })
            .exceptionally(t -> retry(t, t, request, originalHeaders, originalQuery, retryCount, requestId, timer, executorService))
            .thenCompose(Function.identity());
    }

    private CompletableFuture<Response> retry(Throwable first,
                                              Throwable last,
                                              final Request request,
                                              final HttpHeaders originalHeaders,
                                              final QueryString originalQuery,
                                              final int retryCount,
                                              final String requestId,
                                              final Timer timer,
                                              final ExecutorService executorService) {

        if (isRetryable(last, retryCount, timer)) {
            return failedFuture(new RestException("Unable to execute HTTP request: " + first.getMessage(), first));
        }

        return executeAsyncRequest(request, originalHeaders, originalQuery, retryCount, requestId, timer, executorService)
            .thenApply(CompletableFuture::completedFuture)
            .exceptionally(t -> { first.addSuppressed(t); return retry(first, t, request, originalHeaders, originalQuery, retryCount+1, requestId, timer, executorService); })
            .thenCompose(Function.identity());
    }

    private boolean isRetryable(Throwable e, int retryCount, Timer timer) {
        if (e instanceof SocketException || e instanceof SocketTimeoutException) {
            if (!shouldRetry(retryCount, timer.split())) {
                return false;
            }
            log.debug("Retrying on {}: {}", e.getClass().getName(), e.getMessage());
        } else if (e instanceof RestException) {
            RestException restException = (RestException) e;
            return restException.isRetryable() && shouldRetry(retryCount, timer.split());
        }
        return true;
    }

    private static <T> CompletableFuture<T> failedFuture(Throwable t) {
        final CompletableFuture<T> cf = new CompletableFuture<>();
        cf.completeExceptionally(t);
        return cf;
    }


    /**
     * Exponential sleep on failed request to avoid flooding a service with
     * retries.
     *
     * @param retries           Current retry count.
     */
    private void pauseBeforeRetry(int retries, Response response, long timeElapsed) throws RestException {
        long delay = -1;
        long timeElapsedLeft = maxElapsedMillis - timeElapsed;

        // check before continuing
        if (!shouldRetry(retries, timeElapsed)) {
            throw failedToRetry();
        }

        if (response != null && response.getHttpStatus() == 429) {
            delay = get429DelayMillis(response);
            if (!shouldRetry(retries, timeElapsed + delay)) {
                throw failedToRetry();
            }
            log.debug("429 detected, will retry in {}ms, attempt number: {}", delay, retries);
        }

        // default / fallback strategy (backwards compatible implementation)
        if (delay < 0) {
            delay = Math.min(getDefaultDelayMillis(retries), timeElapsedLeft);
        }

        // this shouldn't happen, but guard against a negative delay at this point
        if (delay < 0) {
            throw failedToRetry();
        }

        log.debug("Retryable condition detected, will retry in {}ms, attempt number: {}", delay, retries);

        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RestException(e.getMessage(), e);
        }
    }

    private long get429DelayMillis(Response response) {

        // the time at which the rate limit will reset, specified in UTC epoch time.
        String resetLimit = getOnlySingleHeaderValue(response,"X-Rate-Limit-Reset");
        if (resetLimit == null || !resetLimit.chars().allMatch(Character::isDigit)) {
            return -1;
        }

        // If the Date header is not set, do not continue
        Date requestDate = dateFromHeader(response);
        if (requestDate == null) {
            return -1;
        }

        long waitUntil = Long.parseLong(resetLimit) * 1000L;
        long requestTime = requestDate.getTime();
        long delay = waitUntil - requestTime + 1000;
        log.debug("429 wait: {} - {} + {} = {}", waitUntil, requestTime, 1000, delay);

        return delay;
    }

    private Date dateFromHeader(Response response) {
        Date result = null;
        long dateLong = response.getHeaders().getDate();
        if (dateLong > 0) {
            result = new Date(dateLong);
        }
        return result;
    }

    private long getDefaultDelayMillis(int retries) {
        long scaleFactor = 300;
        long result = (long) (Math.pow(2, retries) * scaleFactor);
        return Math.min(result, DEFAULT_MAX_BACKOFF_IN_MILLISECONDS);
    }

    private boolean shouldRetry(int retryCount, long timeElapsed) {
               // either maxRetries or maxElapsedMillis is enabled
        return (maxRetries > 0 || maxElapsedMillis > 0)

               // maxRetries count is disabled OR if set check it
               && (maxRetries <= 0 || retryCount <= this.maxRetries)

               // maxElapsedMillis is disabled OR if set check it
               && (maxElapsedMillis <= 0 || timeElapsed < maxElapsedMillis);
    }

    private boolean shouldRetry(Response response, int retryCount, long timeElapsed) {
        int httpStatus = response.getHttpStatus();

        // supported status codes
        return shouldRetry(retryCount, timeElapsed)
            && (httpStatus == 429
             || httpStatus == 503
             || httpStatus == 504);
    }

    private RestException failedToRetry() {
        return new RestException("Cannot retry request, next request will exceed retry configuration.");
    }

    private String getOnlySingleHeaderValue(Response response, String name) {

        if (response.getHeaders() != null) {
            List<String> values = response.getHeaders().get(name);
            if (!Collections.isEmpty(values) && values.size() == 1) {
                return values.get(0);
            }
        }
        return null;
    }

    private String getRequestId(Response response) {
        if (response != null) {
            return response.getHeaders().getFirst("X-Okta-Request-Id");
        }
        return null;
    }

    /**
     * Adds {@code X-Okta-Retry-For} and {@code X-Okta-Retry-Count} headers to request if not null/empty or zero.
     *
     * @param request the request to add headers too
     * @param requestId request ID of the original request that failed
     * @param retryCount the number of times the request has been retried
     */
    private void setOktaHeaders(Request request, String requestId, int retryCount) {
        if (Strings.hasText(requestId)) {
            request.getHeaders().add("X-Okta-Retry-For", requestId);
        }
        if (retryCount > 0) {
            request.getHeaders().add("X-Okta-Retry-Count", Integer.toString(retryCount+1));
        }
    }

    private static class Timer {

        private long startTime = System.currentTimeMillis();

        long split() {
            return System.currentTimeMillis() - startTime;
        }
    }
}