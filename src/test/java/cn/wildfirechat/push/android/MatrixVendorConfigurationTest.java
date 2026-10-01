package cn.wildfirechat.push.android;

import cn.wildfirechat.push.PushMessage;
import cn.wildfirechat.push.android.hms.HMSConfig;
import cn.wildfirechat.push.android.hms.HMSPush;
import cn.wildfirechat.push.android.honor.HonorConfig;
import cn.wildfirechat.push.android.honor.HonorPush;
import cn.wildfirechat.push.android.oppo.OppoConfig;
import cn.wildfirechat.push.android.oppo.OppoPush;
import cn.wildfirechat.push.android.vivo.VivoConfig;
import cn.wildfirechat.push.android.vivo.VivoPush;
import cn.wildfirechat.push.android.xiaomi.XiaomiConfig;
import cn.wildfirechat.push.android.xiaomi.XiaomiPush;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.fail;

public class MatrixVendorConfigurationTest {
    @Test
    public void matrixXiaomiFailsWhenProviderIsNotConfigured() throws Exception {
        XiaomiPush push = new XiaomiPush();
        ReflectionTestUtils.setField(push, "mConfig", new XiaomiConfig());
        expectConfigurationFailure(() -> push.push(matrixMessage()));
    }

    @Test
    public void matrixHuaweiFailsWhenProviderIsNotConfigured() throws Exception {
        HMSPush push = new HMSPush();
        ReflectionTestUtils.setField(push, "mConfig", new HMSConfig());
        expectConfigurationFailure(() -> push.push(matrixMessage()));
    }

    @Test
    public void matrixHonorFailsWhenProviderIsNotConfigured() throws Exception {
        HonorPush push = new HonorPush();
        ReflectionTestUtils.setField(push, "mConfig", new HonorConfig());
        expectConfigurationFailure(() -> push.push(matrixMessage()));
    }

    @Test
    public void matrixOppoFailsWhenProviderIsNotConfigured() throws Exception {
        OppoPush push = new OppoPush();
        ReflectionTestUtils.setField(push, "mConfig", new OppoConfig());
        expectConfigurationFailure(() -> push.push(matrixMessage()));
    }

    @Test
    public void matrixVivoFailsWhenProviderIsNotConfigured() throws Exception {
        VivoPush push = new VivoPush();
        ReflectionTestUtils.setField(push, "mConfig", new VivoConfig());
        expectConfigurationFailure(() -> push.push(matrixMessage()));
    }

    private PushMessage matrixMessage() {
        PushMessage message = new PushMessage();
        message.setMatrixDataOnly(true);
        message.setDeviceToken("dummy-token");
        return message;
    }

    private void expectConfigurationFailure(ThrowingRunnable runnable) throws Exception {
        try {
            runnable.run();
            fail("Expected Matrix provider configuration failure");
        } catch (IllegalStateException expected) {
            // Expected.
        }
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
