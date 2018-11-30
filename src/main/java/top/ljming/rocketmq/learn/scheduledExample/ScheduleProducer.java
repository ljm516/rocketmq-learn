package top.ljming.rocketmq.learn.scheduledExample;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.common.RemotingHelper;

/**
 * 定时任务模式的生产者.
 * 定时任务模式的消息和普通消息不同之处在于，消息发送在给定的时间执行
 *
 * @author ljming
 */
public class ScheduleProducer {
    public static void main(String[] args) throws Exception {
        DefaultMQProducer scheduleProducer = new DefaultMQProducer("scheduleProducer");
        scheduleProducer.setNamesrvAddr("localhost:9876");
        scheduleProducer.start();

        int totalMessagesToSend = 100;
        for (int i = 0; i < totalMessagesToSend; i++) {
            Message message = new Message("TestTopic",
                    ("Hello scheduled message " + i).getBytes(RemotingHelper.DEFAULT_CHARSET));
            // 消息将在 10s 后发送到消费者
            message.setDelayTimeLevel(3);

            scheduleProducer.send(message);
        }
        scheduleProducer.shutdown();

    }
}
