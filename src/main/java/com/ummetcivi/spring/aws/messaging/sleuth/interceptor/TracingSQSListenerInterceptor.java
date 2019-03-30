package com.ummetcivi.spring.aws.messaging.sleuth.interceptor;

import brave.Span;
import brave.Tracer;
import brave.propagation.TraceContextOrSamplingFlags;
import com.ummetcivi.spring.aws.messaging.sleuth.tracing.AWSMessageTracing;
import lombok.RequiredArgsConstructor;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.messaging.Message;

@RequiredArgsConstructor
public class TracingSQSListenerInterceptor implements MethodInterceptor {

    private final AWSMessageTracing amazonMessageTracing;

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        if (!invocation.getMethod().getName().equals("handleMessage")) {
            return invocation.proceed();
        }

        Message message = (Message) invocation.getArguments()[0];

        TraceContextOrSamplingFlags extracted = amazonMessageTracing.extract(message.getHeaders());
        Span consumerSpan = amazonMessageTracing.tracing().tracer().nextSpan(extracted).kind(Span.Kind.CONSUMER)
                .name("receive");
        Span listenerSpan = amazonMessageTracing.tracing().tracer().newChild(consumerSpan.context());

        try (Tracer.SpanInScope ws = amazonMessageTracing.tracing().tracer().withSpanInScope(listenerSpan)) {
            return invocation.proceed();
        } catch (Throwable t) {
            listenerSpan.error(t);
            throw t;
        } finally {
            listenerSpan.finish();
        }
    }
}
