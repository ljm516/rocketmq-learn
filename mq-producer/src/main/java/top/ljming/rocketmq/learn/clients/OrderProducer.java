package top.ljming.rocketmq.learn.clients;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.common.RemotingHelper;

/**
 * rocketmq 提供了有序的消息，用户FIFO（先进先出）队列.
 *
 * 本例演示发送/接收全局和局部有序的消息
 *
 * @author ljming
 */
public class OrderProducer {

    public static void main(String[] args) throws Exception {
        // 实例化producer，并给出group name
        DefaultMQProducer producer = new DefaultMQProducer("order_producer");

        // 指定name server 地址
        producer.setNamesrvAddr("localhost:9876");

        // 启动实例
        producer.start();

        String[] tags = new String[] {"TagA", "TagB", "TagC", "TagD", "TagE"};

        for (int i = 0; i < 10; i++) {
            // 创建message实例，并指定Topic，tag和 消息体
            Message message = new Message("TopicTestjjj", tags[i % tags.length], "KEY" + i,
                    ("Hello RocketMQ " + i).getBytes(RemotingHelper.DEFAULT_CHARSET));

            // 调用send方法，将消息传送到一个消息服务器（broker）
            SendResult result = producer.send(message, (mqs, msg, arg) -> {
                Integer id = (Integer) arg;
                int index = id % mqs.size();
                return mqs.get(index);
            }, i);
            System.out.printf("%s%n", result);
        }
        producer.shutdown();
    }
}
