package com.google.pubsub.proxy.actions.publish;

import com.google.api.core.ApiFuture;
import com.google.api.gax.grpc.GrpcStatusCode;
import com.google.api.gax.rpc.ApiExceptionFactory;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.pubsub.proxy.entities.Message;
import com.google.pubsub.proxy.entities.Request;
import com.google.pubsub.proxy.exceptions.MissingRequiredFieldsException;
import com.google.pubsub.v1.PubsubMessage;
import io.grpc.Status;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@RunWith(MockitoJUnitRunner.class)
public class PublishMessageUnitTest {

    private static final String TOPIC = "PUBSUB_TOPIC";
    private static final String MESSAGE_ID = "MESSAGE_ID";
    private static final String DATA = "MESSAGE_DATA";
    private static final LinkedHashMap<String, String> ATTRIBUTES = new LinkedHashMap<>();
    private static final String PUBLISH_TIME = ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    @Mock
    ConcurrentHashMap<String, Publisher> publisherList;
    @Mock
    Publisher publisher;
    @Captor
    ArgumentCaptor<PubsubMessage> captor;
    private PublishMessage publishMessage;
    private Request request;
    private Message message;
    private ApiFuture<String> goodFuture;
    private ApiFuture<String> badFuture;
    private ApiFuture<String> badApiFuture;

    @Before
    public void setUp() {
        publishMessage = new PublishMessage();
        setupRequest();
        setupMockPublisher();
        setupFutures();
    }

    private void setupRequest() {
        request = new Request();
        request.setTopic(TOPIC);
        setupMessage();
        request.setMessages(Collections.singletonList(message));
    }

    private void setupMessage() {
        message = new Message();
        message.setMessageId(MESSAGE_ID);
        message.setData(DATA);
        message.setPublishTime(PUBLISH_TIME);
        message.setAttributes(ATTRIBUTES);
        publishMessage.publishers = publisherList;
    }

    private void setupFutures() {
        goodFuture = getSuccessfulPublishFuture();
        badFuture = getFailedPublishFuture();
        badApiFuture = getFailedAPIPublishFuture();
    }

    @After
    public void tearDown() {
    }

    @Test(expected = NullPointerException.class)
    public void WhenNoRequestIsNotPresentThendoPostThrowsNPE() throws Exception {
        publishMessage.doPost(null);

    }

    @Test(expected = MissingRequiredFieldsException.class)
    public void WhenNoRequestTopicIsNullThendoPostThrowsMissingRequiredFieldsException() throws Exception {
        request.setTopic(null);
        publishMessage.doPost(request);

    }

    @Test(expected = MissingRequiredFieldsException.class)
    public void WhenNoRequestMessagesIsNullThendoPostThrowsMissingRequiredFieldsException() throws Exception {
        request.setMessages(null);
        publishMessage.doPost(request);

    }

    @Test(expected = MissingRequiredFieldsException.class)
    public void WhenNoRequestMessagesIsEmptyThendoPostThrowsMissingRequiredFieldsException() throws Exception {
        request.setMessages(new ArrayList<>());
        publishMessage.doPost(request);
    }

    @Test
    public void WhenRequestIsValidAndTopicExistsThenRespectivePublisherIsReturned() throws Exception {
        when(publisher.publish(any())).thenReturn(goodFuture);
        publishMessage.doPost(request);
        verify(publisher).publish(Mockito.any());
    }

    @Test
    public void WhenRequestIsValidAndSingleMessageExistsThenPublishIsInvokedOnce() throws Exception {
        when(publisher.publish(any())).thenReturn(goodFuture);
        publishMessage.doPost(request);
        verify(publisher, times(1)).publish(Mockito.any());
    }

    @Test
    public void WhenRequestIsValidAndTwoMessagesExistsThenPublishIsInvokedTwice() throws Exception {
        ArrayList<Message> messages = new ArrayList<>();
        messages.add(message);
        messages.add(message);
        request.setMessages(messages);
        when(publisher.publish(any())).thenReturn(goodFuture);
        publishMessage.doPost(request);
        verify(publisher, times(2)).publish(Mockito.any());
    }

    @Test
    public void WhenRequestIsValidAndPublisherDoNotExistThenANewPublisherIsCreated() throws Exception {
        when(publisherList.containsKey(TOPIC)).thenReturn(Boolean.FALSE);
        publishMessage.doPost(request);
        verify(publisherList).put(eq(TOPIC), Mockito.any());
    }

    @Test
    public void WhenRequestIsValidAndPublisherInitializedThenPubSubMessagesArePublished() throws Exception {
        when(publisher.publish(any())).thenReturn(goodFuture);
        publishMessage.doPost(request);
        verify(publisher).publish(captor.capture());
        assertEquals(DATA, captor.getAllValues().get(0).getData().toStringUtf8());
    }

    @Test
    public void WhenPublishIsSuccessfulThenOnSuccessCallbackIsInvokedOntheFuture() throws Exception {
        when(publisher.publish(any())).thenReturn(goodFuture);
        publishMessage.doPost(request);
        verify(goodFuture, times(1)).addListener(any(Runnable.class), any(Executor.class));
    }

    @Test
    public void WhenPublishFailsThenOnFailureCallbackIsInvokedOntheFuture() throws Exception {
        when(publisher.publish(any())).thenReturn(badFuture);
        publishMessage.doPost(request);
        verify(badFuture, times(1)).addListener(any(Runnable.class), any(Executor.class));
    }

    @Test(expected = Exception.class)
    public void WhenMessageDataIsNullThenGenericApiExceptionIsThrown() throws Exception {
        message.setData(null);
        publishMessage.doPost(request);
    }

    @Test(expected = Exception.class)
    public void WhenMessageIdIsNullThenGenericApiExceptionIsThrown() throws Exception {
        message.setMessageId(null);
        publishMessage.doPost(request);
    }

    @Test(expected = Exception.class)
    public void WhenMessageAttributesAreNullThenGenericApiExceptionIsThrown() throws Exception {
        message.setAttributes(null);
        publishMessage.doPost(request);
    }

    @Test(expected = Exception.class)
    public void WhenMessagePublishTimeIsNullThenGenericApiExceptionIsThrown() throws Exception {
        message.setPublishTime(null);
        publishMessage.doPost(request);
    }

    private void setupMockPublisher() {
        when(publisherList.containsKey(TOPIC)).thenReturn(Boolean.TRUE);
        when(publisherList.get(TOPIC)).thenReturn(publisher);
    }

    private ApiFuture<String> getSuccessfulPublishFuture() {
        SpyableFuture<String> future = new SpyableFuture("success");
        return spy(future);
    }

    private ApiFuture<String> getFailedPublishFuture() {
        SpyableFuture<String> future = new SpyableFuture(new Exception());
        return spy(future);
    }

    private ApiFuture<String> getFailedAPIPublishFuture() {
        SpyableFuture<String> future = new SpyableFuture(ApiExceptionFactory.createException(new Exception(), GrpcStatusCode.of(Status.Code.INTERNAL), false));
        return spy(future);
    }

    private class SpyableFuture<V> implements ApiFuture<V> {
        private V value = null;
        private Throwable exception = null;

        public SpyableFuture(V value) {
            this.value = value;
        }

        public <V> SpyableFuture(Throwable exception) {
            this.exception = exception;
        }

        @Override
        public V get() throws ExecutionException {
            if (exception != null) {
                throw new ExecutionException(exception);
            }
            return value;
        }

        @Override
        public V get(long timeout, TimeUnit unit) throws ExecutionException {
            return get();
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return true;
        }

        @Override
        public void addListener(Runnable listener, Executor executor) {
            executor.execute(listener);
        }
    }

}