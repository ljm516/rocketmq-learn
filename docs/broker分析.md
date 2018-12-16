# rocketmq 之 broker 分析

## broker 启动

查看mqbroker启动broker脚本可以知道，broker启动是执行了`org.apache.rocketmq.broker.BrokerStartup` 类的main方法。

### main 方法

```
public static void main(String[] args) {
    start(createBrokerController(args));
}

public static BrokerController createBrokerController(String[] args) {
    System.setProperty(RemotingCommand.REMOTING_VERSION_KEY, Integer.toString(MQVersion.CURRENT_VERSION));

    if (null == System.getProperty(NettySystemConfig.COM_ROCKETMQ_REMOTING_SOCKET_SNDBUF_SIZE)) {
        NettySystemConfig.socketSndbufSize = 131072;
    }

    if (null == System.getProperty(NettySystemConfig.COM_ROCKETMQ_REMOTING_SOCKET_RCVBUF_SIZE)) {
        NettySystemConfig.socketRcvbufSize = 131072;
    }

    try{
        /**
        代码省略
        这段操作主要是main方法的参数中解析出传过来的option参数，加载配置参数等。
        **/

        final BrokerController controller = new BrokerController(//
            brokerConfig, //
            nettyServerConfig, //
            nettyClientConfig, //
            messageStoreConfig);
        // remember all configs to prevent discard
        controller.getConfiguration().registerConfig(properties);

        boolean initResult = controller.initialize(); // brokerController 初始化
        if (!initResult) {
            controller.shutdown();
            System.exit(-3);
        }
        // 添加brokershutdown的钩子
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            private volatile boolean hasShutdown = false;
            private AtomicInteger shutdownTimes = new AtomicInteger(0);

            @Override
            public void run() {
                synchronized (this) {
                    log.info("Shutdown hook was invoked, {}", this.shutdownTimes.incrementAndGet());
                    if (!this.hasShutdown) {
                        this.hasShutdown = true;
                        long begineTime = System.currentTimeMillis();
                        controller.shutdown(); // borker shutdown
                        long consumingTimeTotal = System.currentTimeMillis() - begineTime;
                        log.info("Shutdown hook over, consuming total time(ms): {}", consumingTimeTotal);
                    }
                }
            }
        }, "ShutdownHook"));

        return controller;
    } catch (Throwable e) {
        e.printStackTrace();
        System.exit(-1);
    }

    return null;
}
```

### BrokerController的initialize方法

首先从存储文件路径加载topic信息、consumerOfferset信息，subscriptionGroup信息。分别通过对应的manager加载，所有的manager都继承ConfigManager类。加载信息的load方法在父类实现

```
result = result && this.topicConfigManager.load();
result = result && this.consumerOffsetManager.load();
result = result && this.subscriptionGroupManager.load();

// ConfigManager
// 首先从 ~store/*/*.json文件加载，如果加载失败，从~store/*/*.json.bak文件中加载
public boolean load() {
    String fileName = null;
    try {
        fileName = this.configFilePath();
        String jsonString = MixAll.file2String(fileName);

        if (null == jsonString || jsonString.length() == 0) {
            return this.loadBak();
        } else {
            this.decode(jsonString);
            PLOG.info("load {} OK", fileName);
            return true;
        }
    } catch (Exception e) {
        PLOG.error("load " + fileName + " Failed, and try to load backup file", e);
        return this.loadBak();
    }
}    
```

然后初始化messageStore。如果MessageStorePluginContext具有plugin，则messageStore实例为AbstractPluginMessageStore类的实例，否则为DefaultMessageStore实例。然后通过messageStore加载commitLog和consumerQueue。

```
if (result) {
    try {
        this.messageStore =
            new DefaultMessageStore(this.messageStoreConfig, this.brokerStatsManager, this.messageArrivingListener,
                this.brokerConfig);
        this.brokerStats = new BrokerStats((DefaultMessageStore) this.messageStore);
        //load plugin
        MessageStorePluginContext context = new MessageStorePluginContext(messageStoreConfig, brokerStatsManager, messageArrivingListener, brokerConfig);
        this.messageStore = MessageStoreFactory.build(context, this.messageStore);
    } catch (IOException e) {
        result = false;
        e.printStackTrace();
    }
}
result = result && this.messageStore.load();
```

**messageStore.load()实现**

1. commitLog.load()从commitLog存储路径，加载commitLog文件，每个文件都会new 一个 MappedFile 对象。大小默认为1G。然后加入到CopyOnWriteArrayList中。
2. loadConsumerQueue() 从消息存储根目录加载所有的consumerQueue文件，然后加入到缓存--MessageStore类的consumeQueueTable属性，它是一个双重map结构：`ConcurrentHashMap<String/* topic */, ConcurrentHashMap<Integer/* queueId */, ConsumeQueue>>`;第一层的key为topic,第二层map的为queueId

```
public boolean load() {
    boolean result = true;

    try {
        boolean lastExitOK = !this.isTempFileExist();
        log.info("last shutdown {}", lastExitOK ? "normally" : "abnormally");

        if (null != scheduleMessageService) {
            result = result && this.scheduleMessageService.load();
        }

        // load Commit Log
        result = result && this.commitLog.load();

        // load Consume Queue
        result = result && this.loadConsumeQueue();

        if (result) {
            this.storeCheckpoint =
                new StoreCheckpoint(StorePathConfigHelper.getStoreCheckpoint(this.messageStoreConfig.getStorePathRootDir()));

            this.indexService.load(lastExitOK);

            this.recover(lastExitOK);

            log.info("load over, and the max phy offset = {}", this.getMaxPhyOffset());
        }
    } catch (Exception e) {
        log.error("load exception", e);
        result = false;
    }

    if (!result) {
        this.allocateMappedFileService.shutdown();
    }

    return result;
}
```

上述步骤完成，broker将启动一系列的线程池。

```
// 发送消息的线程池
this.sendMessageExecutor = new BrokerFixedThreadPoolExecutor(
    this.brokerConfig.getSendMessageThreadPoolNums(),
    this.brokerConfig.getSendMessageThreadPoolNums(),
    1000 * 60,
    TimeUnit.MILLISECONDS,
    this.sendThreadPoolQueue,
    new ThreadFactoryImpl("SendMessageThread_"));

// 拉取消息的线程池
this.pullMessageExecutor = new BrokerFixedThreadPoolExecutor(
    this.brokerConfig.getPullMessageThreadPoolNums(),
    this.brokerConfig.getPullMessageThreadPoolNums(),
    1000 * 60,
    TimeUnit.MILLISECONDS,
    this.pullThreadPoolQueue,
    new ThreadFactoryImpl("PullMessageThread_"));

// 管理broker的线程池
this.adminBrokerExecutor =
    Executors.newFixedThreadPool(this.brokerConfig.getAdminBrokerThreadPoolNums(), new ThreadFactoryImpl(
        "AdminBrokerThread_"));

// 客户端管理线程池
this.clientManageExecutor = new ThreadPoolExecutor(
    this.brokerConfig.getClientManageThreadPoolNums(),
    this.brokerConfig.getClientManageThreadPoolNums(),
    1000 * 60,
    TimeUnit.MILLISECONDS,
    this.clientManagerThreadPoolQueue,
    new ThreadFactoryImpl("ClientManageThread_"));

// 消费者管理线程池
this.consumerManageExecutor =
    Executors.newFixedThreadPool(this.brokerConfig.getConsumerManageThreadPoolNums(), new ThreadFactoryImpl(
        "ConsumerManageThread_"));
```

接下来向remotingServer注册一系列的processor, this.regitserProcessor()

```
public void registerProcessor() {
    /**
     * 发送消息处理器
     */
    SendMessageProcessor sendProcessor = new SendMessageProcessor(this);
    sendProcessor.registerSendMessageHook(sendMessageHookList);
    sendProcessor.registerConsumeMessageHook(consumeMessageHookList);

    this.remotingServer.registerProcessor(RequestCode.SEND_MESSAGE, sendProcessor, this.sendMessageExecutor);
    this.remotingServer.registerProcessor(RequestCode.SEND_MESSAGE_V2, sendProcessor, this.sendMessageExecutor);
    this.remotingServer.registerProcessor(RequestCode.CONSUMER_SEND_MSG_BACK, sendProcessor, this.sendMessageExecutor);
    this.fastRemotingServer.registerProcessor(RequestCode.SEND_MESSAGE, sendProcessor, this.sendMessageExecutor);
    this.fastRemotingServer.registerProcessor(RequestCode.SEND_MESSAGE_V2, sendProcessor, this.sendMessageExecutor);
    this.fastRemotingServer.registerProcessor(RequestCode.CONSUMER_SEND_MSG_BACK, sendProcessor, this.sendMessageExecutor);
    /**
     * 拉取消息处理器
     */
    this.remotingServer.registerProcessor(RequestCode.PULL_MESSAGE, this.pullMessageProcessor, this.pullMessageExecutor);
    this.pullMessageProcessor.registerConsumeMessageHook(consumeMessageHookList);

    /**
     * 获取消息处理器
     */
    NettyRequestProcessor queryProcessor = new QueryMessageProcessor(this);
    this.remotingServer.registerProcessor(RequestCode.QUERY_MESSAGE, queryProcessor, this.pullMessageExecutor);
    this.remotingServer.registerProcessor(RequestCode.VIEW_MESSAGE_BY_ID, queryProcessor, this.pullMessageExecutor);

    this.fastRemotingServer.registerProcessor(RequestCode.QUERY_MESSAGE, queryProcessor, this.pullMessageExecutor);
    this.fastRemotingServer.registerProcessor(RequestCode.VIEW_MESSAGE_BY_ID, queryProcessor, this.pullMessageExecutor);

    /**
     * 客户端管理处理器
     */
    ClientManageProcessor clientProcessor = new ClientManageProcessor(this);
    this.remotingServer.registerProcessor(RequestCode.HEART_BEAT, clientProcessor, this.clientManageExecutor);
    this.remotingServer.registerProcessor(RequestCode.UNREGISTER_CLIENT, clientProcessor, this.clientManageExecutor);

    this.fastRemotingServer.registerProcessor(RequestCode.HEART_BEAT, clientProcessor, this.clientManageExecutor);
    this.fastRemotingServer.registerProcessor(RequestCode.UNREGISTER_CLIENT, clientProcessor, this.clientManageExecutor);

    /**
     * 消费者管理处理器
     */
    ConsumerManageProcessor consumerManageProcessor = new ConsumerManageProcessor(this);
    this.remotingServer.registerProcessor(RequestCode.GET_CONSUMER_LIST_BY_GROUP, consumerManageProcessor, this.consumerManageExecutor);
    this.remotingServer.registerProcessor(RequestCode.UPDATE_CONSUMER_OFFSET, consumerManageProcessor, this.consumerManageExecutor);
    this.remotingServer.registerProcessor(RequestCode.QUERY_CONSUMER_OFFSET, consumerManageProcessor, this.consumerManageExecutor);

    this.fastRemotingServer.registerProcessor(RequestCode.GET_CONSUMER_LIST_BY_GROUP, consumerManageProcessor, this.consumerManageExecutor);
    this.fastRemotingServer.registerProcessor(RequestCode.UPDATE_CONSUMER_OFFSET, consumerManageProcessor, this.consumerManageExecutor);
    this.fastRemotingServer.registerProcessor(RequestCode.QUERY_CONSUMER_OFFSET, consumerManageProcessor, this.consumerManageExecutor);

    /**
     * 事务
     */
    this.remotingServer.registerProcessor(RequestCode.END_TRANSACTION, new EndTransactionProcessor(this), this.sendMessageExecutor);
    this.fastRemotingServer.registerProcessor(RequestCode.END_TRANSACTION, new EndTransactionProcessor(this), this.sendMessageExecutor);

    /**
     * 默认的admin
     */
    AdminBrokerProcessor adminProcessor = new AdminBrokerProcessor(this);
    this.remotingServer.registerDefaultProcessor(adminProcessor, this.adminBrokerExecutor);
    this.fastRemotingServer.registerDefaultProcessor(adminProcessor, this.adminBrokerExecutor);
}
```

### controller.start()

上面讲到broker的initialize过程，在broker初始化完成之后，进行broker的启动过程--broker.start()。

```
public void start() throws Exception {
    if (this.messageStore != null) {
        this.messageStore.start();
    }

    if (this.remotingServer != null) {
        this.remotingServer.start();
    }

    if (this.fastRemotingServer != null) {
        this.fastRemotingServer.start();
    }

    if (this.brokerOuterAPI != null) {
        this.brokerOuterAPI.start();
    }

    if (this.pullRequestHoldService != null) {
        this.pullRequestHoldService.start();
    }

    if (this.clientHousekeepingService != null) {
        this.clientHousekeepingService.start();
    }

    if (this.filterServerManager != null) {
        this.filterServerManager.start();
    }

    this.registerBrokerAll(true, false);

    this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {

        @Override
        public void run() {
            try {
                BrokerController.this.registerBrokerAll(true, false);
            } catch (Throwable e) {
                log.error("registerBrokerAll Exception", e);
            }
        }
    }, 1000 * 10, 1000 * 30, TimeUnit.MILLISECONDS);

    if (this.brokerStatsManager != null) {
        this.brokerStatsManager.start();
    }

    if (this.brokerFastFailure != null) {
        this.brokerFastFailure.start();
    }
}
```

这个过程首先是将broker的几个服务启动起来。然后将broker信息注册到其它的broker上

## broker接收存储消息

在broker启动中讲到，control.start() 中启动了remotingServer--远程服务。remotingServer.start() 是基于netty通信服务的。当producer发送消息到broker中，remotingServer接收到消息，对消息进行处理。

```
public void processMessageReceived(ChannelHandlerContext ctx, RemotingCommand msg) throws Exception {
    final RemotingCommand cmd = msg;
    if (cmd != null) {
        switch (cmd.getType()) {
            case REQUEST_COMMAND:
                processRequestCommand(ctx, cmd);
                break;
            case RESPONSE_COMMAND:
                processResponseCommand(ctx, cmd);
                break;
            default:
                break;
        }
    }
}

```

在processRequestConmmand(ChannelHandlerContext ctx, RemotingCommand cmd) 方法中，真正处理消息的代码：`final RemotingCommand response = pair.getObject1().processRequest(ctx, cmd);`; 在broker初始化的时候，注册了多个processor，存储在`processorTable`, key为具体的业务，这里是处理消息，所以key是`SEND_MESSAGE = 10 `;所以从缓存中获取的processor为SendMessageProcessor，即真正处理消息的逻辑，在SendMessageProcessor类。

```
public RemotingCommand processRequest(ChannelHandlerContext ctx, RemotingCommand request) throws RemotingCommandException {
    SendMessageContext mqtraceContext;
    switch (request.getCode()) {
        case RequestCode.CONSUMER_SEND_MSG_BACK:
            return this.consumerSendMsgBack(ctx, request);
        default:
            SendMessageRequestHeader requestHeader = parseRequestHeader(request);
            if (requestHeader == null) {
                return null;
            }

            mqtraceContext = buildMsgContext(ctx, requestHeader);
            this.executeSendMessageHookBefore(ctx, request, mqtraceContext);
            final RemotingCommand response = this.sendMessage(ctx, request, mqtraceContext, requestHeader);

            this.executeSendMessageHookAfter(response, mqtraceContext);
            return response;
    }
}
// sendMessage
private RemotingCommand sendMessage(final ChannelHandlerContext ctx, //
    final RemotingCommand request, //
    final SendMessageContext sendMessageContext, //
    final SendMessageRequestHeader requestHeader) throws RemotingCommandException {
    // 构建response对象
    final RemotingCommand response = RemotingCommand.createResponseCommand(SendMessageResponseHeader.class);
    final SendMessageResponseHeader responseHeader = (SendMessageResponseHeader) response.readCustomHeader();

    response.setOpaque(request.getOpaque()); 

    response.addExtField(MessageConst.PROPERTY_MSG_REGION, this.brokerController.getBrokerConfig().getRegionId());
    response.addExtField(MessageConst.PROPERTY_TRACE_SWITCH, String.valueOf(this.brokerController.getBrokerConfig().isTraceOn()));

    if (log.isDebugEnabled()) {
        log.debug("receive SendMessage request command, {}", request);
    }

    final long startTimstamp = this.brokerController.getBrokerConfig().getStartAcceptSendRequestTimeStamp();
    if (this.brokerController.getMessageStore().now() < startTimstamp) {
        response.setCode(ResponseCode.SYSTEM_ERROR);
        response.setRemark(String.format("broker unable to service, until %s", UtilAll.timeMillisToHumanString2(startTimstamp)));
        return response;
    }

    response.setCode(-1);
    super.msgCheck(ctx, requestHeader, response);
    if (response.getCode() != -1) {
        return response;
    }
    // 获取消息体
    final byte[] body = request.getBody();
    // queueId
    int queueIdInt = requestHeader.getQueueId();
    // 获取topic配置
    TopicConfig topicConfig = this.brokerController.getTopicConfigManager().selectTopicConfig(requestHeader.getTopic());

    if (queueIdInt < 0) {
        queueIdInt = Math.abs(this.random.nextInt() % 99999999) % topicConfig.getWriteQueueNums();
    }

    int sysFlag = requestHeader.getSysFlag();

    if (TopicFilterType.MULTI_TAG == topicConfig.getTopicFilterType()) {
        sysFlag |= MessageSysFlag.MULTI_TAGS_FLAG;
    }

    String newTopic = requestHeader.getTopic();
    if (null != newTopic && newTopic.startsWith(MixAll.RETRY_GROUP_TOPIC_PREFIX)) {
        String groupName = newTopic.substring(MixAll.RETRY_GROUP_TOPIC_PREFIX.length());
        SubscriptionGroupConfig subscriptionGroupConfig =
            this.brokerController.getSubscriptionGroupManager().findSubscriptionGroupConfig(groupName);
        if (null == subscriptionGroupConfig) {
            response.setCode(ResponseCode.SUBSCRIPTION_GROUP_NOT_EXIST);
            response.setRemark(
                "subscription group not exist, " + groupName + " " + FAQUrl.suggestTodo(FAQUrl.SUBSCRIPTION_GROUP_NOT_EXIST));
            return response;
        }

        int maxReconsumeTimes = subscriptionGroupConfig.getRetryMaxTimes();
        if (request.getVersion() >= MQVersion.Version.V3_4_9.ordinal()) {
            maxReconsumeTimes = requestHeader.getMaxReconsumeTimes();
        }
        int reconsumeTimes = requestHeader.getReconsumeTimes() == null ? 0 : requestHeader.getReconsumeTimes();
        if (reconsumeTimes >= maxReconsumeTimes) {
            newTopic = MixAll.getDLQTopic(groupName);
            queueIdInt = Math.abs(this.random.nextInt() % 99999999) % DLQ_NUMS_PER_GROUP;
            topicConfig = this.brokerController.getTopicConfigManager().createTopicInSendMessageBackMethod(newTopic, //
                DLQ_NUMS_PER_GROUP, //
                PermName.PERM_WRITE, 0
            );
            if (null == topicConfig) {
                response.setCode(ResponseCode.SYSTEM_ERROR);
                response.setRemark("topic[" + newTopic + "] not exist");
                return response;
            }
        }
    }
    MessageExtBrokerInner msgInner = new MessageExtBrokerInner();
    msgInner.setTopic(newTopic);
    msgInner.setBody(body);
    msgInner.setFlag(requestHeader.getFlag());
    MessageAccessor.setProperties(msgInner, MessageDecoder.string2messageProperties(requestHeader.getProperties()));
    msgInner.setPropertiesString(requestHeader.getProperties());
    msgInner.setTagsCode(MessageExtBrokerInner.tagsString2tagsCode(topicConfig.getTopicFilterType(), msgInner.getTags()));

    msgInner.setQueueId(queueIdInt);
    msgInner.setSysFlag(sysFlag);
    msgInner.setBornTimestamp(requestHeader.getBornTimestamp());
    msgInner.setBornHost(ctx.channel().remoteAddress());
    msgInner.setStoreHost(this.getStoreHost());
    msgInner.setReconsumeTimes(requestHeader.getReconsumeTimes() == null ? 0 : requestHeader.getReconsumeTimes());

    if (this.brokerController.getBrokerConfig().isRejectTransactionMessage()) {
        String traFlag = msgInner.getProperty(MessageConst.PROPERTY_TRANSACTION_PREPARED);
        if (traFlag != null) {
            response.setCode(ResponseCode.NO_PERMISSION);
            response.setRemark(
                "the broker[" + this.brokerController.getBrokerConfig().getBrokerIP1() + "] sending transaction message is forbidden");
            return response;
        }
    }
    // 消息落盘
    PutMessageResult putMessageResult = this.brokerController.getMessageStore().putMessage(msgInner);
    if (putMessageResult != null) {
        boolean sendOK = false;

        switch (putMessageResult.getPutMessageStatus()) {
            // Success
            case PUT_OK:
                sendOK = true;
                response.setCode(ResponseCode.SUCCESS);
                break;
            case FLUSH_DISK_TIMEOUT:
                response.setCode(ResponseCode.FLUSH_DISK_TIMEOUT);
                sendOK = true;
                break;
            case FLUSH_SLAVE_TIMEOUT:
                response.setCode(ResponseCode.FLUSH_SLAVE_TIMEOUT);
                sendOK = true;
                break;
            case SLAVE_NOT_AVAILABLE:
                response.setCode(ResponseCode.SLAVE_NOT_AVAILABLE);
                sendOK = true;
                break;

            // Failed
            case CREATE_MAPEDFILE_FAILED:
                response.setCode(ResponseCode.SYSTEM_ERROR);
                response.setRemark("create mapped file failed, server is busy or broken.");
                break;
            case MESSAGE_ILLEGAL:
            case PROPERTIES_SIZE_EXCEEDED:
                response.setCode(ResponseCode.MESSAGE_ILLEGAL);
                response.setRemark(
                    "the message is illegal, maybe msg body or properties length not matched. msg body length limit 128k, msg properties length limit 32k.");
                break;
            case SERVICE_NOT_AVAILABLE:
                response.setCode(ResponseCode.SERVICE_NOT_AVAILABLE);
                response.setRemark(
                    "service not available now, maybe disk full, " + diskUtil() + ", maybe your broker machine memory too small.");
                break;
            case OS_PAGECACHE_BUSY:
                response.setCode(ResponseCode.SYSTEM_ERROR);
                response.setRemark("[PC_SYNCHRONIZED]broker busy, start flow control for a while");
                break;
            case UNKNOWN_ERROR:
                response.setCode(ResponseCode.SYSTEM_ERROR);
                response.setRemark("UNKNOWN_ERROR");
                break;
            default:
                response.setCode(ResponseCode.SYSTEM_ERROR);
                response.setRemark("UNKNOWN_ERROR DEFAULT");
                break;
        }

        String owner = request.getExtFields().get(BrokerStatsManager.COMMERCIAL_OWNER);
        if (sendOK) {

            this.brokerController.getBrokerStatsManager().incTopicPutNums(msgInner.getTopic());
            this.brokerController.getBrokerStatsManager().incTopicPutSize(msgInner.getTopic(),
                putMessageResult.getAppendMessageResult().getWroteBytes());
            this.brokerController.getBrokerStatsManager().incBrokerPutNums();

            response.setRemark(null);

            responseHeader.setMsgId(putMessageResult.getAppendMessageResult().getMsgId());
            responseHeader.setQueueId(queueIdInt);
            responseHeader.setQueueOffset(putMessageResult.getAppendMessageResult().getLogicsOffset());

            doResponse(ctx, request, response);

            if (hasSendMessageHook()) {
                sendMessageContext.setMsgId(responseHeader.getMsgId());
                sendMessageContext.setQueueId(responseHeader.getQueueId());
                sendMessageContext.setQueueOffset(responseHeader.getQueueOffset());

                int commercialBaseCount = brokerController.getBrokerConfig().getCommercialBaseCount();
                int wroteSize = putMessageResult.getAppendMessageResult().getWroteBytes();
                int incValue = (int) Math.ceil(wroteSize / BrokerStatsManager.SIZE_PER_COUNT) * commercialBaseCount;

                sendMessageContext.setCommercialSendStats(BrokerStatsManager.StatsType.SEND_SUCCESS);
                sendMessageContext.setCommercialSendTimes(incValue);
                sendMessageContext.setCommercialSendSize(wroteSize);
                sendMessageContext.setCommercialOwner(owner);
            }
            return null;
        } else {
            if (hasSendMessageHook()) {
                int wroteSize = request.getBody().length;
                int incValue = (int) Math.ceil(wroteSize / BrokerStatsManager.SIZE_PER_COUNT);

                sendMessageContext.setCommercialSendStats(BrokerStatsManager.StatsType.SEND_FAILURE);
                sendMessageContext.setCommercialSendTimes(incValue);
                sendMessageContext.setCommercialSendSize(wroteSize);
                sendMessageContext.setCommercialOwner(owner);
            }
        }
    } else {
        response.setCode(ResponseCode.SYSTEM_ERROR);
        response.setRemark("store putMessage return null");
    }

    return response;
}
```

### 消息落盘--messageStore.putMessage

```
public PutMessageResult putMessage(MessageExtBrokerInner msg) {
    if (this.shutdown) {
        log.warn("message store has shutdown, so putMessage is forbidden");
        return new PutMessageResult(PutMessageStatus.SERVICE_NOT_AVAILABLE, null);
    }
    // 检查broker是否为master
    if (BrokerRole.SLAVE == this.messageStoreConfig.getBrokerRole()) {
        long value = this.printTimes.getAndIncrement();
        if ((value % 50000) == 0) {
            log.warn("message store is slave mode, so putMessage is forbidden ");
        }

        return new PutMessageResult(PutMessageStatus.SERVICE_NOT_AVAILABLE, null);
    }
    // 是否为可写状态
    if (!this.runningFlags.isWriteable()) {
        long value = this.printTimes.getAndIncrement();
        if ((value % 50000) == 0) {
            log.warn("message store is not writeable, so putMessage is forbidden " + this.runningFlags.getFlagBits());
        }

        return new PutMessageResult(PutMessageStatus.SERVICE_NOT_AVAILABLE, null);
    } else {
        this.printTimes.set(0);
    }
    // topic 合法性检测
    if (msg.getTopic().length() > Byte.MAX_VALUE) {
        log.warn("putMessage message topic length too long " + msg.getTopic().length());
        return new PutMessageResult(PutMessageStatus.MESSAGE_ILLEGAL, null);
    }
    // properties 检测
    if (msg.getPropertiesString() != null && msg.getPropertiesString().length() > Short.MAX_VALUE) {
        log.warn("putMessage message properties length too long " + msg.getPropertiesString().length());
        return new PutMessageResult(PutMessageStatus.PROPERTIES_SIZE_EXCEEDED, null);
    }

    if (this.isOSPageCacheBusy()) {
        return new PutMessageResult(PutMessageStatus.OS_PAGECACHE_BUSY, null);
    }
    // commitLog 写入
    long beginTime = this.getSystemClock().now();
    PutMessageResult result = this.commitLog.putMessage(msg);

    long eclipseTime = this.getSystemClock().now() - beginTime;
    if (eclipseTime > 500) {
        log.warn("putMessage not in lock eclipse time(ms)={}, bodyLength={}", eclipseTime, msg.getBody().length);
    }
    this.storeStatsService.setPutMessageEntireTimeMax(eclipseTime);

    if (null == result || !result.isOk()) {
        this.storeStatsService.getPutMessageFailedTimes().incrementAndGet();
    }

    return result;
}
```








