package top.ljming.rocketmq.learn.scheduledExample;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;

/**
 * 定时模式的消费者.
 *
 * @author ljming
 */
public class ScheduleConsumer {

    public static void main(String[] args) throws MQClientException {
        DefaultMQPushConsumer scheduleConsumer = new DefaultMQPushConsumer("scheduleConsumer");

        scheduleConsumer.setNamesrvAddr("localhost:9876");
        scheduleConsumer.subscribe("TestTopic", "*");
        scheduleConsumer.registerMessageListener((MessageListenerConcurrently) (msgs, context) -> {
            for (MessageExt message : msgs) {
                System.out.println("Receive message[msgId=" + message.getMsgId() + "] "
                        + (System.currentTimeMillis() - message.getStoreTimestamp()) + " ms later");
            }
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        });
        scheduleConsumer.start();
    }
}
