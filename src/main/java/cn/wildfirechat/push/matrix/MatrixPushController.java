package cn.wildfirechat.push.matrix;

import cn.wildfirechat.push.PushMessage;
import cn.wildfirechat.push.PushMessageType;
import cn.wildfirechat.push.android.AndroidPushService;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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
            pushMessage.matrixDataOnly = true;
            pushMessage.senderName = config.getTitle();
            pushMessage.pushContent = config.getBody();
            pushMessage.isHiddenDetail = false;
            pushMessage.convType = 0;
            pushMessage.unReceivedMsg = request.notification.counts == null
                    ? 0
                    : request.notification.counts.unread;
            pushMessage.pushData = createPushData(request.notification, device);

            try {
                androidPushService.push(pushMessage);
            } catch (RuntimeException e) {
                LOG.error("Unable to enqueue Matrix push for provider type {}", key.pushType, e);
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Upstream push provider failed",
                        e
                );
            }
        }

        return response;
    }

    private String createPushData(MatrixPushRequest.Notification notification, MatrixPushRequest.Device device) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("kind", "matrix");
        if (notification.event_id != null) {
            data.put("event_id", notification.event_id);
        }
        if (notification.room_id != null) {
            data.put("room_id", notification.room_id);
        }
        if (notification.counts != null) {
            data.put("unread", String.valueOf(notification.counts.unread));
        }
        String clientSecret = extractClientSecret(device);
        if (clientSecret != null) {
            data.put("cs", clientSecret);
        }
        return new Gson().toJson(data);
    }

    private String extractClientSecret(MatrixPushRequest.Device device) {
        if (device == null || device.data == null) {
            return null;
        }
        Object defaultPayload = device.data.get("default_payload");
        if (defaultPayload instanceof Map) {
            Object clientSecret = ((Map<?, ?>) defaultPayload).get("cs");
            return clientSecret == null ? null : String.valueOf(clientSecret);
        }
        if (defaultPayload instanceof String) {
            try {
                Object parsed = new Gson().fromJson((String) defaultPayload, Object.class);
                if (parsed instanceof Map) {
                    Object clientSecret = ((Map<?, ?>) parsed).get("cs");
                    return clientSecret == null ? null : String.valueOf(clientSecret);
                }
            } catch (RuntimeException e) {
                LOG.warn("Invalid Matrix default_payload JSON");
            }
        }
        return null;
    }
}
