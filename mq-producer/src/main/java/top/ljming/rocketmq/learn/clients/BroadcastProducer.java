package top.ljming.rocketmq.learn.clients;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.common.RemotingHelper;

/**
 * 广播模式消息生产者.
 * 广播模式会发送消息给所有订阅了该topic的消费者
 *
 * @author ljming
 */
public class BroadcastProducer {
    public static void main(String[] args) throws Exception {
        DefaultMQProducer broadcastProducer = new DefaultMQProducer("broadcastProducer");
        broadcastProducer.setNamesrvAddr("localhost:9876");
        broadcastProducer.start();

        for (int i = 0; i < 10; i++) {
            Message message = new Message("TopicTest", "TagA", "OrderID188",
                    "Hello word".getBytes(RemotingHelper.DEFAULT_CHARSET));
            SendResult result = broadcastProducer.send(message);
            System.out.printf("%s%n", result);
        }

        broadcastProducer.shutdown();
    }
}
