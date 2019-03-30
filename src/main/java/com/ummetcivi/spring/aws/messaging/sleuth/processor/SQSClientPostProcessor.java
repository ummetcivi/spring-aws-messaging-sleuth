package com.ummetcivi.spring.aws.messaging.sleuth.processor;

import com.amazonaws.services.sqs.AmazonSQS;
import com.ummetcivi.spring.aws.messaging.sleuth.tracing.AWSMessageTracing;
import com.ummetcivi.spring.aws.messaging.sleuth.interceptor.TracingSQSClientInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

@RequiredArgsConstructor
public class SQSClientPostProcessor implements BeanPostProcessor {

    private final AWSMessageTracing amazonMessageTracing;

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (!(bean instanceof AmazonSQS)) {
            return bean;
        }

        ProxyFactoryBean proxyFactoryBean = new ProxyFactoryBean();
        proxyFactoryBean.setTarget(bean);
        proxyFactoryBean.setProxyTargetClass(true);
        proxyFactoryBean.addAdvice(new TracingSQSClientInterceptor((AmazonSQS) bean, amazonMessageTracing));

        return proxyFactoryBean.getObject();
    }
}
