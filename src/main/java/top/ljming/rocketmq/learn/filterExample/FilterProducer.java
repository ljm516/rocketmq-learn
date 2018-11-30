package top.ljming.rocketmq.learn.filterExample;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.common.RemotingHelper;

/**
 * 过滤消息.
 * 大多数情况下， tag 是一个简单且很有用，对于筛选你要的message
 * 但是有局限性，就是说一个message只能有一个tag，这对一些复杂的业务
 * 支持就不太友好，在这种情况下，你可以使用 sql 表达式来过滤message
 *
 * 原理：
 *  当你发送messge时，sql 的特性会通过你加入的一些属性进行计算。
 *  基于rocketmq定义的语法，你可以实现一些很有趣的逻辑
 *
 * 使用约束：
 *  只有 push 型的消费者可以通过 sql92 筛选message
 *
 * @author ljming
 */
public class FilterProducer {

    public static void main(String[] args) throws Exception {
        DefaultMQProducer filterProducer = new DefaultMQProducer("filterProducer");
        filterProducer.setNamesrvAddr("localhost:9876");

        filterProducer.start();
        for (int i = 0; i < 10; i++) {
            Message message = new Message("TopicTest", "filterTest",
                    ("Hello RocketMQ " + i).getBytes(RemotingHelper.DEFAULT_CHARSET));
            // 设置一些属性
            message.putUserProperty("a", String.valueOf(i));

            SendResult result = filterProducer.send(message);
            System.out.println("result: " + result);
        }
        filterProducer.shutdown();
    }
}

