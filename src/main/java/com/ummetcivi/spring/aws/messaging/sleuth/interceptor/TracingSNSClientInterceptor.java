package com.ummetcivi.spring.aws.messaging.sleuth.interceptor;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.PublishRequest;
import com.ummetcivi.spring.aws.messaging.sleuth.tracing.AWSMessageTracing;
import lombok.RequiredArgsConstructor;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

@RequiredArgsConstructor
public class TracingSNSClientInterceptor implements MethodInterceptor {

    private final AmazonSNS delegating;
    private final AWSMessageTracing amazonMessageTracing;

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        if (!"publish".equals(invocation.getMethod().getName())) {
            return invocation.proceed();
        }

        Object[] arguments = invocation.getArguments();

        PublishRequest publishRequest;

        switch (arguments.length) {
            case 1:
                publishRequest = (PublishRequest) arguments[0];
                break;
            case 2:
                publishRequest = new PublishRequest()
                        .withTopicArn((String) arguments[0])
                        .withMessage((String) arguments[1]);
                break;
            case 3:
                publishRequest = new PublishRequest()
                        .withTopicArn((String) arguments[0])
                        .withMessage((String) arguments[1])
                        .withSubject((String) arguments[2]);
                break;
            default:
                return invocation.proceed();
        }

        amazonMessageTracing.injectSNSHeaders(publishRequest.getMessageAttributes());

        return delegating.publish(publishRequest);
    }
}
