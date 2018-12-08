package top.ljming.rocketmq.learn.filterExample;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.MessageSelector;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;

/**
 * 过滤消费.
 * 当消费消息时，使用 MessageSelector.bySql 通过 sql92 来选择消息
 *
 * @author ljming
 */
public class FilterConsumer {

    // 貌似不支持 sql92
    public static void main(String[] args) throws MQClientException {
        DefaultMQPushConsumer filterConsumer = new DefaultMQPushConsumer("filterConsumer");
        filterConsumer.setNamesrvAddr("localhost:9876");

        filterConsumer.subscribe("TopicTest", MessageSelector.bySql("a between 0 and 3"));

        filterConsumer.registerMessageListener((MessageListenerConcurrently) (msgs, context) -> ConsumeConcurrentlyStatus.CONSUME_SUCCESS);
        filterConsumer.start();
    }

}
