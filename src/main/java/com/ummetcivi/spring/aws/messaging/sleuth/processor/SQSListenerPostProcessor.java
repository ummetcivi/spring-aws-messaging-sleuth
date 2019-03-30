package com.ummetcivi.spring.aws.messaging.sleuth.processor;

import com.ummetcivi.spring.aws.messaging.sleuth.tracing.AWSMessageTracing;
import com.ummetcivi.spring.aws.messaging.sleuth.interceptor.TracingSQSListenerInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.messaging.MessageHandler;

@RequiredArgsConstructor
public class SQSListenerPostProcessor implements BeanPostProcessor {

    private final AWSMessageTracing amazonMessageTracing;

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (!(bean instanceof MessageHandler)) {
            return bean;
        }

        ProxyFactoryBean proxyFactoryBean = new ProxyFactoryBean();
        proxyFactoryBean.setProxyTargetClass(true);
        proxyFactoryBean.setTarget(bean);
        proxyFactoryBean.addAdvice(new TracingSQSListenerInterceptor(amazonMessageTracing));
        return proxyFactoryBean.getObject();
    }
}
