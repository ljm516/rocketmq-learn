package top.ljming.rocketmq.learn;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import org.apache.rocketmq.remoting.common.RemotingHelper;

import java.io.UnsupportedEncodingException;

/**
 * rocketmq learning.
 *
 * @author ljming
 */
public class RocketMQLearnApplication {

    public static void main(String[] args) throws UnsupportedEncodingException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("codeDesc", "InvalidParameter");
        jsonObject.put("code", 4000);
        jsonObject.put("riskType", "null");
        jsonObject.put("level", 0);
        jsonObject.put("openId", "pbkdf2:sha1:1000$jaY2CpCg$69ff71b1bd13a13f121b64b2af611ff4747104a9");
        jsonObject.put("ip", "220.115.234.99");
        jsonObject.put("activityName", "pintuan");
        jsonObject.put("subfix", "201812");
        jsonObject.put("time", 1543673478022L);
        jsonObject.put("message", "参数错误:uid的值不是合法的String[nonce:1375929935][0010-0000-0021]");
        jsonObject.put("userId", 1144992);

        System.out.println(jsonObject.getString("openId").getBytes(RemotingHelper.DEFAULT_CHARSET).length);

//        check(jsonObject);
    }

    public static void check(JSONObject jsonObject) {
        TianyuCheckLogPO checkLogPO = new TianyuCheckLogPO();
        jsonObject.forEach((key, value) -> {
            checkLogPO.setUserId(jsonObject.getInteger("userId"));
            checkLogPO.setTableNo(jsonObject.getInteger("subfix"));
            checkLogPO.setOpenId(jsonObject.getString("openId"));
            checkLogPO.setActivityName(jsonObject.getString("activityName"));
            checkLogPO.setCode(jsonObject.getInteger("code").intValue());
            checkLogPO.setCodeDesc(jsonObject.getString("codeDesc"));
            checkLogPO.setCheckIp(jsonObject.getString("ip"));
            checkLogPO.setLevel(jsonObject.getInteger("level"));
            checkLogPO.setTime(jsonObject.getLong("time"));
            checkLogPO.setNonce(jsonObject.getString("nonce"));
            checkLogPO.setRiskType(jsonObject.getString("riskType"));
            checkLogPO.setMessage(jsonObject.getString("message"));
        });
        System.out.println(checkLogPO.toString());
    }

    @Data
    private static class TianyuCheckLogPO {
        private Integer id;
        private Integer tableNo;
        private Integer userId;
        private String openId;
        private String checkIp;
        private long time;
        private String activityName;
        private int code;
        private String codeDesc;
        private int level;
        private String riskType;
        private String nonce;
        private String message;
    }
}
