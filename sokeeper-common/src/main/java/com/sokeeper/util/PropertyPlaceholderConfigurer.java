/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by
 * applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package com.sokeeper.util;

import java.util.HashSet;
import java.util.Properties;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public class PropertyPlaceholderConfigurer extends org.springframework.beans.factory.config.PropertyPlaceholderConfigurer {

    private Properties resolvedProperties;

    private String     placeholderPrefix = DEFAULT_PLACEHOLDER_PREFIX;

    /**
     * Set the prefix that a placeholder string starts with. The default is "${".
     *
     * @see #DEFAULT_PLACEHOLDER_PREFIX
     */
    public void setPlaceholderPrefix(String placeholderPrefix) {
        this.placeholderPrefix = placeholderPrefix;
    }

    protected void processProperties(ConfigurableListableBeanFactory beanFactoryToProcess, Properties superProps)
                                                                                                                 throws BeansException {
        // 1, resolve the properties file self
        {
            super.setPlaceholderPrefix(DEFAULT_PLACEHOLDER_PREFIX);
            resolvedProperties = new Properties();
            for (Object key : superProps.keySet()) {
                String strKey = (String) key;
                String strVal = superProps.getProperty(strKey);
                strVal = parseStringValue(strVal, superProps, new HashSet<String>());
                resolvedProperties.put(strKey, strVal);
            }
        }
        // 2, resolve the bean definition
        {
            super.setPlaceholderPrefix(placeholderPrefix);
            super.processProperties(beanFactoryToProcess, resolvedProperties);
        }
    }

    public String getProperty(String key) {
        Assert.hasText(key, "key can not be null.");
        return resolvedProperties.getProperty(key);
    }
}
