package top.ljming.rocketmq.learn.clients;

import com.alibaba.fastjson.JSONObject;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.MessageQueueSelector;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.remoting.common.RemotingHelper;
import org.omg.PortableInterceptor.INACTIVE;

import java.util.List;

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
        JSONObject msgBody = new JSONObject();
        msgBody.put("name", "ljming");
        msgBody.put("learning", "rocketmq");
        String[] tags = new String[] {"TagA", "TagB", "TagC", "TagD", "TagE"};
        for (int i = 0; i < 10; i++) {
            int orderId = i % 10;
            msgBody.put("id", 100 + i);
            Message msg = new Message("simple_topic", tags[i % tags.length],
                    msgBody.toJSONString().getBytes(RemotingHelper.DEFAULT_CHARSET));
            producer.send(msg, (mqs, msg1, arg) -> {
                Integer id = (Integer) arg;
                int index = id % mqs.size();

                return mqs.get(index);
            }, orderId);
        }

        producer.shutdown();
    }
}
