package top.ljming.rocketmq.learn.simpleExample;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.*;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;
import top.ljming.rocketmq.learn.ConsumerFactory;

import java.util.List;

/**
 * 简单例子，消息消费者.
 *
 * @author ljming
 */
public class SimpleConsumer {
    public static void main(String[] args) throws MQClientException {
        // 实例化一个consumer，并指定group
        DefaultMQPushConsumer consumer = ConsumerFactory.getPushConsumer("simpleConsumer");

        // 指定name server地址
        consumer.setNamesrvAddr("localhost:9876");

        // 订阅一个或多个topic
        consumer.subscribe("TestTopic", "*");

        // 注册回调，在从消息服务器获取到消息到达后执行
        consumer.registerMessageListener((MessageListenerConcurrently) (msgs, context) -> {
            System.out.printf("%s Receive New message: %s %n", Thread.currentThread().getName(), msgs);
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        });

        // 启动消费者实例
        consumer.start();
        System.out.printf("Consumer started.%n");
    }
}
