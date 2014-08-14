package com.thilinamb.flume.sink;

import com.thilinamb.flume.sink.util.TestUtil;
import kafka.message.MessageAndMetadata;
import org.apache.flume.*;
import org.apache.flume.channel.MemoryChannel;
import org.apache.flume.conf.Configurables;
import org.apache.flume.event.EventBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Unit tests for Kafka Sink
 */
public class KafkaSinkTest {

    private static TestUtil testUtil = TestUtil.getInstance();

    @BeforeClass
    public static void setup(){
        testUtil.prepare();
        List<String> topics = new ArrayList<String>(3);
        topics.add(Constants.DEFAULT_TOPIC);
        topics.add(TestConstants.STATIC_TOPIC);
        topics.add(TestConstants.CUSTOM_TOPIC);
        testUtil.initTopicList(topics);
    }

    @AfterClass
    public static void tearDown(){
        testUtil.tearDown();
    }

    @Test
    public void testDefaultTopic(){
        Sink kafkaSink = new KafkaSink();
        Context context = prepareDefaultContext();
        Configurables.configure(kafkaSink, context);
        Channel memoryChannel = new MemoryChannel();
        Configurables.configure(memoryChannel, context);
        kafkaSink.setChannel(memoryChannel);
        kafkaSink.start();

        String msg = "default-topic-test";
        Transaction tx = memoryChannel.getTransaction();
        tx.begin();
        Event event = EventBuilder.withBody(msg.getBytes());
        memoryChannel.put(event);
        tx.commit();
        tx.close();

        try {
            Sink.Status status = kafkaSink.process();
            if (status == Sink.Status.BACKOFF) {
                fail("Error Occurred");
            }
        } catch (EventDeliveryException ex) {
            // ignore
        }

        String fetchedMsg = new String((byte[])testUtil.getNextMessageFromConsumer(Constants.DEFAULT_TOPIC).message());
        assertEquals(msg, fetchedMsg);
    }

    @Test
    public void testStaticTopic(){
        Sink kafkaSink = new KafkaSink();
        Context context = prepareDefaultContext();
        // add the static topic
        context.put(Constants.TOPIC, TestConstants.STATIC_TOPIC);
        Configurables.configure(kafkaSink, context);
        Channel memoryChannel = new MemoryChannel();
        Configurables.configure(memoryChannel, context);
        kafkaSink.setChannel(memoryChannel);
        kafkaSink.start();

        String msg = "static-topic-test";
        Transaction tx = memoryChannel.getTransaction();
        tx.begin();
        Event event = EventBuilder.withBody(msg.getBytes());
        memoryChannel.put(event);
        tx.commit();
        tx.close();

        try {
            Sink.Status status = kafkaSink.process();
            if (status == Sink.Status.BACKOFF) {
                fail("Error Occurred");
            }
        } catch (EventDeliveryException ex) {
            // ignore
        }

        String fetchedMsg = new String((byte[])testUtil.getNextMessageFromConsumer(
                TestConstants.STATIC_TOPIC).message());
        assertEquals(msg, fetchedMsg);
    }

    @Test
    public void testPreprocessorForCustomKey(){
        Sink kafkaSink = new KafkaSink();
        Context context = prepareDefaultContext();
        // configure the static topic
        context.put(Constants.TOPIC, TestConstants.STATIC_TOPIC);
        // configure the preprocessor
        context.put(Constants.PREPROCESSOR, "com.thilinamb.flume.sink.preprocessor.ModifyKeyPreprocessor");
        Configurables.configure(kafkaSink, context);
        Channel memoryChannel = new MemoryChannel();
        Configurables.configure(memoryChannel, context);
        kafkaSink.setChannel(memoryChannel);
        kafkaSink.start();

        String msg = "custom-key-test";
        Transaction tx = memoryChannel.getTransaction();
        tx.begin();
        Event event = EventBuilder.withBody(msg.getBytes());
        memoryChannel.put(event);
        tx.commit();
        tx.close();

        try {
            Sink.Status status = kafkaSink.process();
            if (status == Sink.Status.BACKOFF) {
                fail("Error Occurred");
            }
        } catch (EventDeliveryException ex) {
            // ignore
        }

        MessageAndMetadata message = testUtil.getNextMessageFromConsumer(
                TestConstants.STATIC_TOPIC);
        String msgBody = new String((byte[]) message.message());
        // check the message body and the key. Only the key should be changed. topic has already been verified by
        // consuming from the correct topic.
        assertEquals(msg, msgBody);
        assertEquals(TestConstants.CUSTOM_KEY, new String((byte[])message.key()));
    }

    @Test
    public void testPreprocessorForCustomTopic(){
        Sink kafkaSink = new KafkaSink();
        Context context = prepareDefaultContext();
        // configure the static topic
        context.put(Constants.TOPIC, TestConstants.STATIC_TOPIC);
        // configure the preprocessor
        context.put(Constants.PREPROCESSOR, "com.thilinamb.flume.sink.preprocessor.ModifyTopicPreprocessor");
        Configurables.configure(kafkaSink, context);
        Channel memoryChannel = new MemoryChannel();
        Configurables.configure(memoryChannel, context);
        kafkaSink.setChannel(memoryChannel);
        kafkaSink.start();

        String msg = "custom-topic-test";
        Transaction tx = memoryChannel.getTransaction();
        tx.begin();
        Event event = EventBuilder.withBody(msg.getBytes());
        memoryChannel.put(event);
        tx.commit();
        tx.close();

        try {
            Sink.Status status = kafkaSink.process();
            if (status == Sink.Status.BACKOFF) {
                fail("Error Occurred");
            }
        } catch (EventDeliveryException ex) {
            // ignore
        }
        // when the message is modified from the preprocessor, it should be published
        // to the custom topic.
        MessageAndMetadata message = testUtil.getNextMessageFromConsumer(
                TestConstants.CUSTOM_TOPIC);
        String msgBody = new String((byte[]) message.message());
        // check the message body. Topic has already been verified by consuming the message from the custom topic.
        assertEquals(msg, msgBody);
    }

    @Test
    public void testPreprocessorForCustomMessageBody(){
        Sink kafkaSink = new KafkaSink();
        Context context = prepareDefaultContext();
        // configure the static topic
        context.put(Constants.TOPIC, TestConstants.STATIC_TOPIC);
        // configure the preprocessor
        context.put(Constants.PREPROCESSOR, "com.thilinamb.flume.sink.preprocessor.ModifyMessageBodyPreprocessor");
        Configurables.configure(kafkaSink, context);
        Channel memoryChannel = new MemoryChannel();
        Configurables.configure(memoryChannel, context);
        kafkaSink.setChannel(memoryChannel);
        kafkaSink.start();

        String msg = "original-message-body";
        Transaction tx = memoryChannel.getTransaction();
        tx.begin();
        Event event = EventBuilder.withBody(msg.getBytes());
        memoryChannel.put(event);
        tx.commit();
        tx.close();

        try {
            Sink.Status status = kafkaSink.process();
            if (status == Sink.Status.BACKOFF) {
                fail("Error Occurred");
            }
        } catch (EventDeliveryException ex) {
            // ignore
        }
        // when the message is modified from the preprocessor, it should be published
        // to the custom topic.
        MessageAndMetadata message = testUtil.getNextMessageFromConsumer(
                TestConstants.STATIC_TOPIC);
        String msgBody = new String((byte[]) message.message());
        // check the message body.
        assertEquals(TestConstants.CUSTOM_MSG_BODY, msgBody);
    }

    private Context prepareDefaultContext(){ // Prepares a default context with Kafka Server Properties
        Context context = new Context();
        context.put("kafka.metadata.broker.list", "localhost:9092");
        context.put("kafka.serializer.class", "kafka.serializer.StringEncoder");
        context.put("kafka.request.required.acks", "1");
        return context;
    }

}