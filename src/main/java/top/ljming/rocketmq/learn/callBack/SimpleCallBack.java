package top.ljming.rocketmq.learn.callBack;

import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;

/**
 * 发送消息回调.
 *
 * @author ljming
 */
public class SimpleCallBack implements SendCallback {
    public void onSuccess(SendResult sendResult) {
        System.out.println("SimpleCallBack: 发送消息成功，回调函数，" + sendResult.getMsgId());
        try {
            Thread.sleep(10000); // 睡眠10s
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void onException(Throwable e) {
        System.out.println("SimpleCallBack: 回调异常, " + e.getMessage());
    }
}
