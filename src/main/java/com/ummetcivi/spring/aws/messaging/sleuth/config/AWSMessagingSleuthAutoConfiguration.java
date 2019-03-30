package com.ummetcivi.spring.aws.messaging.sleuth.config;

import brave.Tracing;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sqs.AmazonSQS;
import com.ummetcivi.spring.aws.messaging.sleuth.processor.SNSClientPostProcessor;
import com.ummetcivi.spring.aws.messaging.sleuth.processor.SQSClientPostProcessor;
import com.ummetcivi.spring.aws.messaging.sleuth.processor.SQSListenerPostProcessor;
import com.ummetcivi.spring.aws.messaging.sleuth.tracing.AWSMessageTracing;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(Tracing.class)
public class AWSMessagingSleuthAutoConfiguration {

    @Bean
    public AWSMessageTracing amazonMessageTracing(Tracing tracing) {
        return AWSMessageTracing.builder()
                .tracing(tracing)
                .build();
    }

    @ConditionalOnClass(AmazonSQS.class)
    @ConditionalOnBean(AmazonSQS.class)
    protected static class SQSSleuthAutoConfiguration {

        @Bean
        public static BeanPostProcessor sqsClientPostProcessor(AWSMessageTracing amazonMessageTracing) {
            return new SQSClientPostProcessor(amazonMessageTracing);
        }

        @Bean
        public static BeanPostProcessor sqsListenerPostProcessor(AWSMessageTracing amazonMessageTracing) {
            return new SQSListenerPostProcessor(amazonMessageTracing);
        }

    }

    @ConditionalOnClass(AmazonSNS.class)
    @ConditionalOnBean(AmazonSNS.class)
    protected static class SNSSleuthAutoConfiguration {

        @Bean
        public static BeanPostProcessor snsClientPostProcessor(AWSMessageTracing amazonMessageTracing) {
            return new SNSClientPostProcessor(amazonMessageTracing);
        }
    }

}
