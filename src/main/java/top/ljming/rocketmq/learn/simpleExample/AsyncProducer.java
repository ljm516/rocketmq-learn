package top.ljming.rocketmq.learn.simpleExample;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.common.RemotingHelper;

/**
 * 消息生产者，以异步的方式发出数据.
 * 异步传输通常用于响应时间敏感的业务场景。
 *
 * @author ljming
 */
public class AsyncProducer {
    public static void main(String[] args) throws Exception {
        // 实例化producer，并给出group name
        DefaultMQProducer producer = new DefaultMQProducer("async_producer");

        // 指定name server 地址
        producer.setNamesrvAddr("localhost:9876");

        // 启动实例
        producer.start();
        producer.setRetryTimesWhenSendAsyncFailed(0);
        for (int i = 0; i < 10; i++) {
            final int index = 1;
            // 创建message实例，并指定Topic，tag和 消息体
            Message message = new Message("TopicTest", "TagA", "OrderID188",
                    ("Hello RocketMQ " + i).getBytes(RemotingHelper.DEFAULT_CHARSET));

            // 调用send方法，将消息传送到一个消息服务器（broker）
            producer.send(message, new SendCallback() {
                public void onSuccess(SendResult sendResult) {
                    System.out.printf("%-10d ok %s %n", index, sendResult.getMsgId());
                }

                public void onException(Throwable e) {
                    System.out.printf("%-10d ok %s %n", index, e);
                    e.printStackTrace();
                }
            });
        }
        producer.shutdown();
    }
}
