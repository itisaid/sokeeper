/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package com.sokeeper.util;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public class UrlParser extends URLStreamHandler {
    private URL           url        = null;
    private String        protocol   = null;
    private String        host       = null;
    private int           port       = 0;
    private MapParameters parameters = new MapParameters();

    public UrlParser(String url) {
        Assert.notNull(url, "url can not be null.");
        try {
            this.url = new URL(null, url, this);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e.getMessage());
        }

        protocol = this.url.getProtocol();
        Assert.notNull(protocol, "the protocol can not be null.");
        protocol = protocol.toLowerCase();

        host = this.url.getHost();
        Assert.notNull(host, "the host can not be null.");
        host = host.toLowerCase();

        port = this.url.getPort();
        Assert.isTrue(port > 0, "port should > 0.");

        String query = this.url.getQuery();
        if (query != null) {
            String[] tokens = query.split("&");
            for (String token : tokens) {
                String[] pair = token.split("=");
                if (pair.length == 2) {
                    parameters.addParameter(pair[0], pair[1]);
                }
            }
        }
    }

    public String getProtocol() {
        return protocol;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public MapParameters getParameters() {
        return parameters;
    }

    public URL getUrl() {
        return url;
    }

    protected URLConnection openConnection(URL u) throws IOException {
        throw new UnsupportedOperationException("openConnection");
    }

}
