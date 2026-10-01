package cn.wildfirechat.push.matrix;

import cn.wildfirechat.push.PushMessage;
import cn.wildfirechat.push.PushMessageType;
import cn.wildfirechat.push.android.AndroidPushService;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class MatrixPushController {
    private static final Logger LOG = LoggerFactory.getLogger(MatrixPushController.class);

    @Autowired
    private AndroidPushService androidPushService;

    @Autowired
    private MatrixPushConfig config;

    @PostMapping(value = "/_matrix/push/v1/notify", produces = "application/json;charset=UTF-8")
    public MatrixPushResponse notify(@RequestBody MatrixPushRequest request) {
        MatrixPushResponse response = new MatrixPushResponse();
        if (request == null || request.notification == null || request.notification.devices == null) {
            return response;
        }

        for (MatrixPushRequest.Device device : request.notification.devices) {
            if (device == null) {
                continue;
            }

            MatrixPushKey key = MatrixPushKey.parse(device.pushkey);
            if (key == null) {
                if (device.pushkey != null) {
                    response.rejected.add(device.pushkey);
                }
                continue;
            }

            PushMessage pushMessage = new PushMessage();
            pushMessage.pushType = key.pushType;
            pushMessage.deviceToken = key.token;
            pushMessage.packageName = config.getPackageName();
            pushMessage.pushMessageType = PushMessageType.PUSH_MESSAGE_TYPE_NORMAL;
            pushMessage.senderName = config.getTitle();
            pushMessage.pushContent = config.getBody();
            pushMessage.isHiddenDetail = false;
            pushMessage.convType = 0;
            pushMessage.unReceivedMsg = request.notification.counts == null
                    ? 0
                    : request.notification.counts.unread;
            pushMessage.pushData = createPushData(request.notification);

            try {
                androidPushService.push(pushMessage);
            } catch (RuntimeException e) {
                LOG.error("Unable to enqueue Matrix push for provider key {}", device.pushkey, e);
            }
        }

        return response;
    }

    private String createPushData(MatrixPushRequest.Notification notification) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("kind", "matrix");
        if (notification.event_id != null) {
            data.put("event_id", notification.event_id);
        }
        if (notification.room_id != null) {
            data.put("room_id", notification.room_id);
        }
        return new Gson().toJson(data);
    }
}
