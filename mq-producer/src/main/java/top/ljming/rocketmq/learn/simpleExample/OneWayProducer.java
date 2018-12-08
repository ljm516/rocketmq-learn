package top.ljming.rocketmq.learn.simpleExample;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.common.RemotingHelper;

/**
 * 消息生产者，以单向的方式发出数据.
 *
 * 单向发送是指只负责发送消息而不等待服务器回应且没有回调函数触发，
 * 适用于某些耗时非常短但对可靠性要求并不高的场景，例如日志收集
 *
 * @author ljming
 */
public class OneWayProducer {
    public static void main(String[] args) throws Exception {
        // 实例化producer，并给出group name
        DefaultMQProducer producer = new DefaultMQProducer("sync_producer");

        // 指定name server 地址
        producer.setNamesrvAddr("localhost:9876");

        // 启动实例
        producer.start();
        for (int i = 0; i < 10; i++) {

            // 创建message实例，并指定Topic，tag和 消息体
            Message message = new Message("TopicTest", "TagA",
                    ("Hello RocketMQ " + i).getBytes(RemotingHelper.DEFAULT_CHARSET));
            // 调用send方法，将消息传送到一个消息服务器（broker）
            producer.sendOneway(message);
        }
        producer.shutdown();
    }
}
