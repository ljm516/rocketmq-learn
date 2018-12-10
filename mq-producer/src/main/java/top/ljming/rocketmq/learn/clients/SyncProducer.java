package top.ljming.rocketmq.learn.clients;

import com.alibaba.fastjson.JSONObject;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.common.RemotingHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * 消息生产者，以同步的方式发出数据.
 * 可靠的同步传输用于广泛的场景，
 * 如重要的通知消息，短信通知，短信营销系统等
 *
 * @author ljming
 */
public class SyncProducer {

    public static void main(String[] args) throws Exception {

        // 实例化producer，并给出group name
        DefaultMQProducer producer = new DefaultMQProducer("sync_group");

        // 指定name server 地址
        producer.setNamesrvAddr("localhost:9876");
//        producer.setRetryTimesWhenSendFailed(3);
        // 启动实例
        producer.start();

        JSONObject msgBody = new JSONObject();
        msgBody.put("id", 6);
        msgBody.put("name", "ljming");
        msgBody.put("learning", "rocketmq");

        Message message = new Message("simple_topic", "*",
                msgBody.toJSONString().getBytes(RemotingHelper.DEFAULT_CHARSET));

        // 调用send方法，将消息传送到一个消息服务器（broker）
        SendResult result = producer.send(message);
        System.out.printf("%s%n", result);
        producer.shutdown();
    }
}
