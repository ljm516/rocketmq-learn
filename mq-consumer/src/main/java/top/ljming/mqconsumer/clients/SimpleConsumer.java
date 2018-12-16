package top.ljming.mqconsumer.clients;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.remoting.common.RemotingHelper;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.ljming.mqconsumer.topic.SimpleTopic;

import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * 简单例子，消息消费者.
 *
 * @author ljming
 */
@Component
public class SimpleConsumer implements InitializingBean {
    @Value("${mq.address}")
    private String nameSrv;

    private DefaultMQPushConsumer simpleConsumer;

    @Override
    public void afterPropertiesSet() {
        System.out.println("init simpleConsumer");
        startConsumer();
    }

    private void startConsumer() {
        start((msgs, context) -> {
            try {
                if (!processMsg(msgs)) {
                    if (msgs.get(0).getReconsumeTimes() >= simpleConsumer.getMaxReconsumeTimes()) {
                        System.err.println("message consumer over max retry times: msgId=" + msgs.get(0).getMsgId());
                        return ConsumeOrderlyStatus.SUCCESS;
                    } else {
                        System.out.println("consumer later");
                        return ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT;
                    }
                }
            } catch (Exception e) {
                System.err.println("message consumer occur exception: " + e);
                return ConsumeOrderlyStatus.SUCCESS;
            }
            return ConsumeOrderlyStatus.SUCCESS;
        });
    }

    private void start(MessageListenerOrderly messageListenerOrderly) {
        try {
            simpleConsumer = new DefaultMQPushConsumer("simpleConsumer");
            if (StringUtils.isNotEmpty(nameSrv)) {
                simpleConsumer.setNamesrvAddr(nameSrv);
            }
            simpleConsumer.subscribe(SimpleTopic.SIMPLETOPIC.getName(), "*");
            simpleConsumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
            simpleConsumer.setMessageListener(messageListenerOrderly);
            simpleConsumer.setMaxReconsumeTimes(SimpleTopic.SIMPLETOPIC.getReconsumeTimesMax());
            simpleConsumer.start();

            System.out.println("consumer started");
        } catch (Exception e) {
            System.err.println("consumer start error: " + e);
        }

    }

    private boolean processMsg(List<MessageExt> msgs) throws UnsupportedEncodingException {
        MessageExt messageExt = msgs.get(0);
        String msgBody = new String(messageExt.getBody(), RemotingHelper.DEFAULT_CHARSET);
        JSONObject json = JSONObject.parseObject(msgBody);
        return process(json);
    }

    private boolean process(JSONObject json) {
        System.out.println("consumer success msg content: " + json.toJSONString());
        return true;
    }
}
