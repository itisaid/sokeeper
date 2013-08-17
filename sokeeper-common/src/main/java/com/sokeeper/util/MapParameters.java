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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public class MapParameters implements Serializable {
    private static final long   serialVersionUID = 2465438884350063380L;
    private Map<String, String> parameters       = new HashMap<String, String>();

    public MapParameters() {

    }

    public String toString() {
        return parameters.toString();
    }

    public boolean hasParameter(String paramName) {
        Assert.hasText(paramName, "paramName can not be empty.");
        return parameters.containsKey(paramName);
    }

    public void addParameter(String paramName, String paramValue) {
        Assert.hasText(paramName, "paramName can not be empty.");
        Assert.notNull(paramValue, "paramValue can not be null.");

        parameters.put(paramName, paramValue);
    }

    public String getParameter(String paramName, String defaultVal) {
        Assert.notNull(paramName, "paramName can not be null.");
        String val = parameters.get(paramName);
        if (val == null) {
            val = defaultVal;
        }
        return val;
    }

    public int getParameter(String paramName, int defaultVal) {
        String val = getParameter(paramName, "" + defaultVal);
        try {
            return Integer.valueOf(val);
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    public boolean getParameter(String paramName, boolean defaultVal) {
        String val = getParameter(paramName, "" + defaultVal);
        return Boolean.valueOf(val);
    }

    public int getIndexedParameter(String paramName, String[] values, int defaultIndex,
                                   boolean ignoreCase) {
        Assert.notEmpty(values, "values can not be null.");
        String val = getParameter(paramName, "");
        if (!val.equals("")) {
            for (int i = 0; i < values.length; i++) {
                if (values[i].equals(val) || (ignoreCase && values[i].equalsIgnoreCase(val))) {
                    defaultIndex = i;
                    break;
                }
            }
        }
        return defaultIndex;
    }

}
