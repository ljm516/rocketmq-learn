package top.ljming.mqconsumer.clients;

import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.consumer.DefaultMQPullConsumer;
import org.apache.rocketmq.client.consumer.PullResult;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import top.ljming.mqconsumer.topic.SimpleTopic;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 描述类的功能.
 *
 * @author ljming
 */
public class    SimplePullConsumer implements InitializingBean {
    @Value("${mq.address}")
    private String nameSrv;

    private final Map<MessageQueue, Long> OFFSET_TABLE = new HashMap<>(); // 自己维护offset

    private DefaultMQPullConsumer simplePullConsumer = null;

    @Override
    public void afterPropertiesSet() {
        System.out.println("init simpleConsumer");
        startConsumer();
    }

    private void startConsumer() {
        try {
            simplePullConsumer = new DefaultMQPullConsumer("simpleConsumer");
            if (StringUtils.isNotEmpty(nameSrv)) {
                simplePullConsumer.setNamesrvAddr(nameSrv);
            }
            Set<MessageQueue> messageQueueSet = simplePullConsumer.fetchSubscribeMessageQueues(SimpleTopic.SIMPLEPULLTOPIC.getName());
            messageQueueSet.forEach(mq -> {
                // 自己封装pullRequest
                try {
                    long offset = simplePullConsumer.fetchConsumeOffset(mq, true);
                    System.out.println("Consumer from the queue : " + mq);
                    while (true) {
                        PullResult pr = simplePullConsumer.pullBlockIfNotFound(mq, null, getMessageQueueOffset(mq), 32);
                        System.out.println("pr: " + pr);
                        putMessageQueueOffset(mq, pr.getNextBeginOffset());
                        switch (pr.getPullStatus()) {
                            case FOUND:
                                break;
                            case NO_NEW_MSG:
                                break;
                            case NO_MATCHED_MSG:
                                break;
                            case OFFSET_ILLEGAL:
                                break;
                            default:
                                break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            simplePullConsumer.start();
            System.out.println("consumer started");
        } catch (Exception e) {
            System.err.println("consumer start error: " + e);
        }
    }

    private long getMessageQueueOffset(MessageQueue mq) {
        Long offset = OFFSET_TABLE.get(mq);
        if (offset != null) {
            return offset;
        }
        return 0;
    }
    private void putMessageQueueOffset(MessageQueue mq, long offset) {
        OFFSET_TABLE.put(mq, offset);
    }
}
