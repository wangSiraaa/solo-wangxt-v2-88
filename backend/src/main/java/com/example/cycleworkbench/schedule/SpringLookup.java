package com.example.cycleworkbench.schedule;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/** Lets plain Quartz job instances fetch Spring beans. */
@Component
public class SpringLookup implements ApplicationContextAware {

    private static ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringLookup.context = applicationContext;
    }

    public static <T> T get(Class<T> type) {
        return context.getBean(type);
    }
}
