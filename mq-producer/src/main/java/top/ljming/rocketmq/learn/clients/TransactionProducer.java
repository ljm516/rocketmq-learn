package top.ljming.rocketmq.learn.clients;

import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.common.RemotingHelper;
import top.ljming.rocketmq.learn.listener.TransactionListenerImpl;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 具有事务功能的消息.
 * 什么是 transaction message？
 *  它可以被认为是两阶段的 message 提交实现，以确保在分布式系统下的一致性。
 *  事务消息确保执行本地事务和发送消息是原子的。
 *
 * 使用限制：
 *  1. 事务消息不支持定时任务和批量操作
 *  2. 为了避免单个消息被检查太多次并导致一半队列消息累积，默认限制检查单条消息的次数为15，但是
 *  用户可以通过修改 broker 的配置文件的 "transactionCheckMax" 参数来修改这个值。如果一条
 *  消息被检查的次数超过了 "transactionCheckMax" 的值，broker 会默认放弃这条消息并打印出
 *  一条错误日志。用户可以修改这个操作，通过复写 "AbstractTransactionCheckListener" 类。
 *  3. 事务消息值一个确定的时间之后被检测，这个时间值是通过配置 broker 的
 *  "transactionTimeout"参数决定的。当然用户也是可以在发行事务消息的时候修改这个值，通过设置
 *  "CHECK_IMMUNITY_TIME_IN_SECONDS" 参数。这个参数优先于 "transactionMsgTimeout" 参数
 *  4. 事务消息可能被检查和消费多次
 *  5. 对用户的目标主题消息重置可能会失败，当然，它也是支持日志记录的。RocketMQ本身的高可用性机制
 *  确保了高可用性。如果要确保事务性消息不会丢失并且保证事务完整性，建议使用同步双写机制。
 *  6. 事务性消息的生产者ID不能与其他类型的消息的生产者ID共享，与其他类型的消息不同，
 *  事务性消息允许后向查询，MQ Server按其生产者ID查询客户端。
 *
 * 应用：
 * 1、事务状态
 *  * TransactionStatus.CommitTransaction：提交事务，意味着消费者可已消费这条消息
 *  * TransactionStatus.RollbackTransaction：回滚事务，它意味着消息将被删除，不允许消费。
 *  * TransactionStatus.Unknown：中间状态，意味着mq需要重复检查去决定这个状态
 *
 * 2、发送事务消息
 *  下面的例子
 *
 * @author ljming
 */
public class TransactionProducer {
    public static void main(String[] args) throws MQClientException, InterruptedException {
        /**
         * 使用TransactionMQProducer创建 producer客户端，指定唯一的生产者组，然后可以设置一个
         * 消费者线程池去执行检查请求。执行完本地事务之后，你需要根据执行结果唤醒MQ，唤醒的状态在
         * 1（事务状态）中描述了
         */

        // 实现TransactionListener
        TransactionListener transactionListener = new TransactionListenerImpl();
        TransactionMQProducer transactionProducer = new TransactionMQProducer("transactionProducer");

        ExecutorService executorService = new ThreadPoolExecutor(2, 5, 100,
                TimeUnit.SECONDS, new ArrayBlockingQueue<>(2000), r -> {
                    Thread thread = new Thread(r);
                    thread.setName("clients-transaction-msg-check-thread");
                    return thread;
                });
        transactionProducer.setExecutorService(executorService);
        transactionProducer.setTransactionListener(transactionListener);
        transactionProducer.start();

        String[] tags = new String[] {"TagA", "TagB", "TagC", "TagD", "TagE"};
        for (int i = 0; i < 10; i++) {
            try {
                Message msg = new Message("TopicTest1234", tags[i % tags.length], "KEY" + i,
                        ("hello RocketMQ " + i).getBytes(RemotingHelper.DEFAULT_CHARSET));
                SendResult result = transactionProducer.sendMessageInTransaction(msg, null);
                System.out.printf("%s%n", result);

                Thread.sleep(10);
            } catch (MQClientException | UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        for (int i = 0; i < 100000; i++) {
            Thread.sleep(1000);
        }
        transactionProducer.shutdown();
    }
}
