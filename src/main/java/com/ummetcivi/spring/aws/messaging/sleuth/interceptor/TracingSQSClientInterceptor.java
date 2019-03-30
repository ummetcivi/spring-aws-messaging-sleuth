package com.ummetcivi.spring.aws.messaging.sleuth.interceptor;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.SendMessageBatchRequest;
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.ummetcivi.spring.aws.messaging.sleuth.tracing.AWSMessageTracing;
import lombok.RequiredArgsConstructor;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor
public class TracingSQSClientInterceptor implements MethodInterceptor {

    private static final String SEND_MESSAGE_METHOD = "sendMessage";
    private static final String SEND_MESSAGE_BATCH_METHOD = "sendMessageBatch";

    private static final Set<String> METHOD_NAMES = new HashSet<>(
            Arrays.asList(SEND_MESSAGE_METHOD, SEND_MESSAGE_BATCH_METHOD));

    private final AmazonSQS delegating;
    private final AWSMessageTracing amazonMessageTracing;

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Method method = invocation.getMethod();

        if (!METHOD_NAMES.contains(method.getName())) {
            return invocation.proceed();
        }

        Object[] arguments = invocation.getArguments();

        if (arguments.length == 0 || arguments.length > 2) {
            return invocation.proceed();
        }

        if (arguments.length == 2) {
            Map<String, MessageAttributeValue> headers = new HashMap<>();
            amazonMessageTracing.injectSQSHeaders(headers);

            if (SEND_MESSAGE_METHOD.equals(method.getName())) {
                String queueUrl = (String) arguments[0];
                String messageBody = (String) arguments[1];

                SendMessageRequest sendMessageRequest = new SendMessageRequest()
                        .withQueueUrl(queueUrl)
                        .withMessageBody(messageBody)
                        .withMessageAttributes(headers);

                return delegating.sendMessage(sendMessageRequest);
            }

            if (SEND_MESSAGE_BATCH_METHOD.equals(method.getName())) {
                String queueUrl = (String) arguments[0];
                List<SendMessageBatchRequestEntry> entries = (List<SendMessageBatchRequestEntry>) arguments[1];

                entries.forEach(
                        sendMessageBatchRequestEntry -> sendMessageBatchRequestEntry.setMessageAttributes(headers));

                SendMessageBatchRequest sendMessageBatchRequest = new SendMessageBatchRequest()
                        .withQueueUrl(queueUrl)
                        .withEntries(entries);

                return delegating.sendMessageBatch(sendMessageBatchRequest);
            }
        }

        Object argument = arguments[0];

        if (argument instanceof SendMessageRequest) {
            amazonMessageTracing.injectSQSHeaders(((SendMessageRequest) argument).getMessageAttributes());
        }

        if (argument instanceof SendMessageBatchRequest) {
            ((SendMessageBatchRequest) argument).getEntries().forEach(
                    sendMessageBatchRequestEntry -> amazonMessageTracing
                            .injectSQSHeaders(sendMessageBatchRequestEntry.getMessageAttributes()));
        }

        return invocation.proceed();
    }
}
