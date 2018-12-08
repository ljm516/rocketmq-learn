package top.ljming.mqconsumer.factory;

import org.apache.rocketmq.client.consumer.DefaultMQPullConsumer;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;

/**
 * 消费者的基类.
 *
 * @author lijm
 */
public class ConsumerFactory {

    public static DefaultMQPushConsumer getPushConsumer(String consumerGroupName) {
        return new DefaultMQPushConsumer(consumerGroupName);
    }

    public static DefaultMQPullConsumer getPullConsumer(String consumerGroupName) {
        return new DefaultMQPullConsumer(consumerGroupName);
    }
}
