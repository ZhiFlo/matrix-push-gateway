package cn.wildfirechat.push.matrix;

import cn.wildfirechat.push.PushMessage;
import cn.wildfirechat.push.android.AndroidPushService;
import cn.wildfirechat.push.android.AndroidPushType;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class MatrixPushControllerTest {
    private CapturingAndroidPushService pushService;
    private MatrixPushController controller;
    private MockMvc mockMvc;

    @Before
    public void setUp() {
        pushService = new CapturingAndroidPushService();
        controller = new MatrixPushController();
        MatrixPushConfig config = new MatrixPushConfig();
        config.setPackageName("io.element.android.x");
        config.setTitle("Element");
        config.setBody("You have a new message");
        ReflectionTestUtils.setField(controller, "androidPushService", pushService);
        ReflectionTestUtils.setField(controller, "config", config);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void httpEndpointBindsMatrixJsonAndDispatches() throws Exception {
        String body = "{"
                + "\"notification\":{"
                + "\"event_id\":\"$event:example.org\","
                + "\"room_id\":\"!room:example.org\","
                + "\"counts\":{\"unread\":3},"
                + "\"devices\":[{"
                + "\"app_id\":\"io.element.android.x\","
                + "\"pushkey\":\"hms:http-token\","
                + "\"data\":{\"default_payload\":{\"cs\":\"http-secret\"}}"
                + "}]"
                + "}"
                + "}";

        mockMvc.perform(
                post("/_matrix/push/v1/notify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rejected").isArray())
                .andExpect(jsonPath("$.rejected").isEmpty());

        assertEquals(1, pushService.messages.size());
        PushMessage message = pushService.messages.get(0);
        assertEquals(AndroidPushType.ANDROID_PUSH_TYPE_HUAWEI, message.pushType);
        assertEquals("http-token", message.deviceToken);
        JsonObject payload = new JsonParser().parse(message.pushData).getAsJsonObject();
        assertEquals("http-secret", payload.get("cs").getAsString());
        assertEquals("3", payload.get("unread").getAsString());
    }

    @Test
    public void notifyRoutesMatrixPayloadWithObjectDefaultPayload() {
        MatrixPushRequest.Device device = new MatrixPushRequest.Device();
        device.app_id = "io.element.android.x";
        device.pushkey = "xiaomi:vendor-token";
        Map<String, Object> defaultPayload = new HashMap<>();
        defaultPayload.put("cs", "client-secret");
        device.data = new HashMap<>();
        device.data.put("default_payload", defaultPayload);

        MatrixPushResponse response = controller.notify(createRequest(device));

        assertTrue(response.rejected.isEmpty());
        assertEquals(1, pushService.messages.size());
        PushMessage message = pushService.messages.get(0);
        assertEquals(AndroidPushType.ANDROID_PUSH_TYPE_XIAOMI, message.pushType);
        assertEquals("vendor-token", message.deviceToken);
        assertEquals("io.element.android.x", message.packageName);
        assertEquals(4, message.unReceivedMsg);
        assertTrue(message.matrixDataOnly);

        JsonObject payload = new JsonParser().parse(message.pushData).getAsJsonObject();
        assertEquals("matrix", payload.get("kind").getAsString());
        assertEquals("$event:example.org", payload.get("event_id").getAsString());
        assertEquals("!room:example.org", payload.get("room_id").getAsString());
        assertEquals("4", payload.get("unread").getAsString());
        assertEquals("client-secret", payload.get("cs").getAsString());
    }

    @Test
    public void notifyRoutesMatrixPayloadWithStringDefaultPayload() {
        MatrixPushRequest.Device device = new MatrixPushRequest.Device();
        device.pushkey = "honor:honor-token";
        device.data = new HashMap<>();
        device.data.put("default_payload", "{\"cs\":\"string-secret\"}");

        MatrixPushResponse response = controller.notify(createRequest(device));

        assertTrue(response.rejected.isEmpty());
        assertEquals(1, pushService.messages.size());
        PushMessage message = pushService.messages.get(0);
        assertEquals(AndroidPushType.ANDROID_PUSH_TYPE_HONOR, message.pushType);
        assertEquals("honor-token", message.deviceToken);
        JsonObject payload = new JsonParser().parse(message.pushData).getAsJsonObject();
        assertEquals("string-secret", payload.get("cs").getAsString());
    }

    @Test
    public void notifyRejectsUnsupportedProviderWithoutDispatching() {
        MatrixPushRequest.Device device = new MatrixPushRequest.Device();
        device.pushkey = "unknown:token";

        MatrixPushResponse response = controller.notify(createRequest(device));

        assertEquals(Collections.singletonList("unknown:token"), response.rejected);
        assertTrue(pushService.messages.isEmpty());
    }

    @Test
    public void matrixPushKeyParsesAllElementChinaProviders() {
        assertPushKey("hms:huawei-token", AndroidPushType.ANDROID_PUSH_TYPE_HUAWEI, "huawei-token");
        assertPushKey("honor:honor-token", AndroidPushType.ANDROID_PUSH_TYPE_HONOR, "honor-token");
        assertPushKey("xiaomi:xiaomi-token", AndroidPushType.ANDROID_PUSH_TYPE_XIAOMI, "xiaomi-token");
        assertPushKey("oppo:oppo-token", AndroidPushType.ANDROID_PUSH_TYPE_OPPO, "oppo-token");
        assertPushKey("vivo:vivo-token", AndroidPushType.ANDROID_PUSH_TYPE_VIVO, "vivo-token");
    }

    @Test
    public void matrixPushKeyRejectsMalformedValues() {
        assertNull(MatrixPushKey.parse(null));
        assertNull(MatrixPushKey.parse(""));
        assertNull(MatrixPushKey.parse("xiaomi"));
        assertNull(MatrixPushKey.parse("xiaomi:"));
        assertNull(MatrixPushKey.parse("unknown:token"));
    }

    private MatrixPushRequest createRequest(MatrixPushRequest.Device device) {
        MatrixPushRequest.Counts counts = new MatrixPushRequest.Counts();
        counts.unread = 4;
        MatrixPushRequest.Notification notification = new MatrixPushRequest.Notification();
        notification.event_id = "$event:example.org";
        notification.room_id = "!room:example.org";
        notification.counts = counts;
        notification.devices = Collections.singletonList(device);
        MatrixPushRequest request = new MatrixPushRequest();
        request.notification = notification;
        return request;
    }

    private void assertPushKey(String raw, int expectedType, String expectedToken) {
        MatrixPushKey key = MatrixPushKey.parse(raw);
        assertNotNull(key);
        assertEquals(expectedType, key.pushType);
        assertEquals(expectedToken, key.token);
    }

    private static final class CapturingAndroidPushService implements AndroidPushService {
        private final List<PushMessage> messages = new ArrayList<>();

        @Override
        public Object push(PushMessage pushMessage) {
            messages.add(pushMessage);
            return null;
        }

        @Override
        public void testPush(PushMessage pushMessage) {
            messages.add(pushMessage);
        }
    }
}
