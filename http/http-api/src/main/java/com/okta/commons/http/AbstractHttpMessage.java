/*
 * Copyright 2014 Stormpath, Inc.
 * Modifications Copyright 2018 Okta, Inc.
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
package com.okta.commons.http;

import java.io.IOException;
import java.io.InputStream;

/**
 * @since 0.5.0
 */
abstract class AbstractHttpMessage implements HttpMessage {

    @Override
    public boolean hasBody() {
        InputStream is = getBody();
        return is != null
            && getHeaders().getContentLength() != 0
            && available(is);
    }

    private static boolean available(InputStream inputStream) {
        try {
            // NOTE: this will NOT work for all input stream types, currently this
            // project only uses ByteArrayInputStreams which supports 'available' calls
            return inputStream.available() > 0;
        } catch (IOException e) {
            // ignore exception nothing to read
            return false;
        }
    }
}
