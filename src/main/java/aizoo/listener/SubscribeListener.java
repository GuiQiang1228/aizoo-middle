package aizoo.listener;

import aizoo.service.UserService;
import aizoo.utils.SpringBeanUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;

import java.util.Map;

/**
 * 订阅接收发布者的消息
 */

public class SubscribeListener implements MessageListener{

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Logger logger = LoggerFactory.getLogger(SubscribeListener.class);

//    private UserService userService = BaseHolder.getBean("UserService");

    @Override
    public void onMessage(Message message, byte[] pattern) {

        logger.info(new String(message.getBody()));
        logger.info("...................");

        Map<String, String> messageMap = null;
        logger.info("读取message数据");
        try {
            messageMap = objectMapper.readValue(new String(message.getBody()), new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            e.printStackTrace();
            logger.info(e.getMessage());
        }

        logger.info("获取username");
        String username = messageMap.get("username");
        String email = messageMap.get("email");
        logger.info(username);
        logger.info(email);
        UserService userService = (UserService) SpringBeanUtils.getBean("UserService");
        userService.addUserSync(username, email);
    }
}
