package top.ljming.rocketmq.learn.clients;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 批量消息生产者.
 * 批量发送消息可提高传递小消息的性能
 *
 * @author ljming
 */
public class BatchProducer {
    public static void main(String[] args) throws Exception {
        DefaultMQProducer batchProducer = new DefaultMQProducer("batchProducer");
        batchProducer.setNamesrvAddr("localhost:9876");

        batchProducer.start();
        String topic = "simple_topic";
        List<Message> messageList = new ArrayList<>();
        messageList.add(new Message(topic, "*", "hello world 0".getBytes()));
        messageList.add(new Message(topic, "*", "hello world 1".getBytes()));
        messageList.add(new Message(topic, "*", "hello world 2".getBytes()));

        batchProducer.send(messageList);

        batchProducer.shutdown();
    }

    public class ListSplitter implements Iterator<List<Message>> {
        private final int SIZE_LIMIT = 1000 * 1000;
        private final List<Message> messages;
        private int currIndex;

        public ListSplitter(List<Message> messages) {
            this.messages = messages;
        }

        @Override
        public boolean hasNext() {
            return currIndex < messages.size();
        }

        @Override
        public List<Message> next() {
            int nextIndex = currIndex;
            int totalSize = 0;
            for (; nextIndex < messages.size(); nextIndex++) {
                Message message = messages.get(nextIndex);
                int temSize = message.getTopic().length() + message.getBody().length;
                Map<String, String> properties = message.getProperties();
                for (Map.Entry<String, String> entry : properties.entrySet()) {
                    temSize += entry.getKey().length() + entry.getValue().length();
                }
                temSize = temSize + 20;
                if (temSize > SIZE_LIMIT) {
                    // 单条消息超过限定的大小是不期望的
                    // 这里让其通过，否则会阻塞splitting 进程
                    if (nextIndex - currIndex == 0) {
                        // 如果下一个子集合没有元素，添加完这个之后就会break
                        nextIndex ++;
                    }
                    break;
                }
                if (temSize + totalSize > SIZE_LIMIT) {
                    break;
                } else {
                    totalSize += temSize;
                }
            }
            List<Message> subList = messages.subList(currIndex, nextIndex);
            currIndex = nextIndex;
            return subList;
        }

    }
}
