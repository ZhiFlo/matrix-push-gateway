package cn.wildfirechat.push.android.vivo;

import cn.wildfirechat.push.PushMessage;
import com.vivo.push.sdk.notofication.Message;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class VivoMatrixFallbackTest {
    @Test
    public void matrixFallbackNeverIncludesMatrixContentOrMetadata() {
        VivoPush push = new VivoPush();
        push.mConfig = new VivoConfig();

        Message vendorMessage = push.buildMessage(matrixMessage(), "v2-test-registration-id");

        assertEquals("Element", vendorMessage.getTitle());
        assertEquals("你收到一条新消息", vendorMessage.getContent());
        assertTrue(vendorMessage.getClientCustomMap() == null || vendorMessage.getClientCustomMap().isEmpty());
        assertTrue(vendorMessage.getExtra() == null || vendorMessage.getExtra().isEmpty());
        assertFalse(String.valueOf(vendorMessage.getContent()).contains("SHOULD-NOT-LEAK"));
        assertFalse(String.valueOf(vendorMessage.getExtra()).contains("client-secret"));
    }

    private PushMessage matrixMessage() {
        PushMessage message = new PushMessage();
        message.setMatrixDataOnly(true);
        message.setSenderName("Element");
        message.setPushContent("SHOULD-NOT-LEAK");
        message.setPushData("{\"event_id\":\"$event\",\"room_id\":\"!room\",\"cs\":\"client-secret\"}");
        return message;
    }
}
