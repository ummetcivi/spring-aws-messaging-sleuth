package com.ummetcivi.spring.aws.messaging.sleuth.processor;

import com.amazonaws.services.sns.AmazonSNS;
import com.ummetcivi.spring.aws.messaging.sleuth.interceptor.TracingSNSClientInterceptor;
import com.ummetcivi.spring.aws.messaging.sleuth.tracing.AWSMessageTracing;
import lombok.RequiredArgsConstructor;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

@RequiredArgsConstructor
public class SNSClientPostProcessor implements BeanPostProcessor {

    private final AWSMessageTracing amazonMessageTracing;

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (!(bean instanceof AmazonSNS)) {
            return bean;
        }

        ProxyFactoryBean proxyFactoryBean = new ProxyFactoryBean();
        proxyFactoryBean.setTarget(bean);
        proxyFactoryBean.setProxyTargetClass(true);
        proxyFactoryBean.addAdvice(new TracingSNSClientInterceptor((AmazonSNS) bean, amazonMessageTracing));

        return proxyFactoryBean.getObject();
    }
}
