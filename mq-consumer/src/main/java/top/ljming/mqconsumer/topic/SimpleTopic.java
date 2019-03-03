package top.ljming.mqconsumer.topic;

/**
 * 主题.
 *
 * @author ljming
 */
public enum SimpleTopic {
    SIMPLETOPIC("simple_topic", 4, 3000, 3),
    SIMPLEPULLTOPIC("simple_pull_topic", 4, 3000, 3);

    private String name;
    private int queue;
    private long timeout;
    private int reConsumeTimesMax;

    SimpleTopic(String name, int queue, long timeout, int reConsumeTimesMax) {
        this.name = name;
        this.queue = queue;
        this.timeout = timeout;
        this.reConsumeTimesMax = reConsumeTimesMax;
    }

    public String getName() {
        return name;
    }

    public int getQueue() {
        return queue;
    }

    public long getTimeout() {
        return timeout;
    }

    public int getReconsumeTimesMax() {
        return reConsumeTimesMax;
    }
}
