package top.ljming.rocketmq.learn.broadcatingExample;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;

/**
 * 广播消费.
 * 广播消费消息会发给消费者组中的每一个消费者进行消费.
 *
 * @author ljming
 */
public class BroadcastConsumer {
    public static void main(String[] args) throws Exception {
        DefaultMQPushConsumer broadcastConsumer = new DefaultMQPushConsumer("broadcastConsumer");
        broadcastConsumer.setNamesrvAddr("localhost:9876");
        broadcastConsumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);

        broadcastConsumer.setMessageModel(MessageModel.BROADCASTING);

        broadcastConsumer.subscribe("TopicTest", "TagA || TagC || TagD");
        broadcastConsumer.registerMessageListener((MessageListenerConcurrently) (msgs, context) -> {
            System.out.printf(Thread.currentThread().getName() + " Receive New Messages: " + msgs + "%n");
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        });
        broadcastConsumer.start();
        System.out.printf("Broadcast Consumer Started.%n");
    }
}
