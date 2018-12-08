package top.ljming.mqconsumer.clients;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.remoting.common.RemotingHelper;
import top.ljming.mqconsumer.factory.ConsumerFactory;

import java.io.UnsupportedEncodingException;
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
            try {
                processMsg(msgs);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        });

        // 启动消费者实例
        consumer.start();
        System.out.printf("Consumer started.%n");
    }

    private static void processMsg(List<MessageExt> msgs) throws UnsupportedEncodingException {
        MessageExt messageExt = msgs.get(0);
        String msgBody = new String(messageExt.getBody(), RemotingHelper.DEFAULT_CHARSET);
        System.out.printf("msgBody: %s", msgBody);
    }
}
