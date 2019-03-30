package com.ummetcivi.spring.aws.messaging.sleuth.tracing;

import brave.Span;
import brave.Tracing;
import brave.propagation.Propagation;
import brave.propagation.TraceContext;
import brave.propagation.TraceContextOrSamplingFlags;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.springframework.messaging.MessageHeaders;

import java.util.Map;

@Accessors(fluent = true)
public final class AWSMessageTracing {

    private static final Propagation.Setter<Map<String, MessageAttributeValue>, String> SQS_SETTER = (carrier, key, value) ->
            carrier.put(key, new MessageAttributeValue().withStringValue(value).withDataType("String"));

    private static final Propagation.Setter<Map<String, com.amazonaws.services.sns.model.MessageAttributeValue>, String> SNS_SETTER = (carrier, key, value) ->
            carrier.put(key, new com.amazonaws.services.sns.model.MessageAttributeValue()
                    .withStringValue(value)
                    .withDataType("String")
            );

    private static final Propagation.Getter<MessageHeaders, String> GETTER = (carrier, key) -> {
        if (!carrier.containsKey(key)) {
            return null;
        }
        return String.valueOf(carrier.get(key));
    };


    @Getter
    private final Tracing tracing;
    private final TraceContext.Extractor<MessageHeaders> extractor;
    private final TraceContext.Injector<Map<String, MessageAttributeValue>> sqsInjector;
    private final TraceContext.Injector<Map<String, com.amazonaws.services.sns.model.MessageAttributeValue>> snsInjector;

    @Builder
    private AWSMessageTracing(Tracing tracing) {
        this.tracing = tracing;
        this.extractor = tracing.propagation().extractor(GETTER);
        this.sqsInjector = tracing.propagation().injector(SQS_SETTER);
        this.snsInjector = tracing.propagation().injector(SNS_SETTER);
    }

    public void injectSQSHeaders(Map<String, MessageAttributeValue> messageAttributes) {
        this.sqsInjector.inject(getSpan().context(), messageAttributes);
    }

    public void injectSNSHeaders(
            Map<String, com.amazonaws.services.sns.model.MessageAttributeValue> messageAttributes) {
        this.snsInjector.inject(getSpan().context(), messageAttributes);
    }

    public TraceContextOrSamplingFlags extract(MessageHeaders headers) {
        return extractor.extract(headers);
    }

    private Span getSpan() {
        TraceContext maybeParent = tracing.currentTraceContext().get();

        Span span;
        if (maybeParent == null) {
            span = tracing.tracer().nextSpan();
        } else {
            span = tracing.tracer().currentSpan();
        }
        return span;
    }
}
