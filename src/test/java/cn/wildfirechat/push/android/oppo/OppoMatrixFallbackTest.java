package cn.wildfirechat.push.android.oppo;

import cn.wildfirechat.push.PushMessage;
import com.oppo.push.server.Notification;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

public class OppoMatrixFallbackTest {
    @Test
    public void matrixFallbackNeverIncludesMatrixContentOrMetadata() {
        OppoPush push = new OppoPush();
        push.mConfig = new OppoConfig();

        PushMessage message = matrixMessage();
        Notification notification = push.getNotification(message);

        assertEquals("Element", notification.getTitle());
        assertEquals("你收到一条新消息", notification.getContent());
        assertNull(notification.getActionParameters());
        assertFalse(notification.toString().contains("SHOULD-NOT-LEAK"));
        assertFalse(notification.toString().contains("client-secret"));
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
