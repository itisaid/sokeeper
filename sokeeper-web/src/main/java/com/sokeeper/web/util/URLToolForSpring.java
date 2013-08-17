package com.sokeeper.web.util;

import java.util.Map;

import org.apache.velocity.tools.view.context.ViewContext;
import org.springframework.context.ApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import com.sokeeper.util.Assert;
import com.sokeeper.util.PropertyPlaceholderConfigurer;

public class URLToolForSpring {

    protected ViewContext                 viewContext;

    protected ApplicationContext          springWebApplicationContext;
    private String                        configBeanName = "";
    private PropertyPlaceholderConfigurer ppConfigurer   = null;

    public void init(Object obj) {
        if (!(obj instanceof ViewContext)) {
            throw new IllegalArgumentException("Tool can only be initialized with a ViewContext");
        }
        viewContext = (ViewContext) obj;
        springWebApplicationContext = (ApplicationContext) viewContext.getRequest().getAttribute(
                                                                                                 DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE);
        ppConfigurer = (PropertyPlaceholderConfigurer) springWebApplicationContext.getBean(configBeanName);
        Assert.notNull(ppConfigurer, "can not find the configurer bean through name:" + configBeanName);
    }

    public void configure(Map<Object, Object> params) {
        configBeanName = (String) params.get("configBeanName");
        Assert.hasText(configBeanName, "configBeanName have not been setted for:" + URLToolForSpring.class.getName());
    }

    public String get(String key) {
        String prop = ppConfigurer.getProperty(key);
        if (prop == null) {
        	if ("contextPath".equals(key)) {
        		prop = viewContext.getRequest().getContextPath();
        	}
        }
        return prop;
    }
}
