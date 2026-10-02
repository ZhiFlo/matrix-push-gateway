package cn.wildfirechat.push.matrix;

import com.google.common.util.concurrent.Striped;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;

@Component
public class MatrixPushDeduplicator {
    private static final long DEFAULT_RETENTION_SECONDS = 24 * 60 * 60;

    private final ConcurrentMap<String, Long> delivered = new ConcurrentHashMap<>();
    private final Striped<Lock> locks = Striped.lock(256);

    @Value("${matrix.push.dedup-retention-seconds:86400}")
    private long retentionSeconds = DEFAULT_RETENTION_SECONDS;

    /**
     * Runs a provider delivery at most once per event/provider/device within the retention window.
     * The cache key is SHA-256 hashed so vendor tokens and Matrix event ids are never retained in
     * plaintext by the deduplication layer.
     */
    public boolean deliverIfNew(String eventId, int pushType, String deviceToken, Runnable delivery) {
        if (eventId == null || eventId.isEmpty()) {
            delivery.run();
            return true;
        }

        String key = digest(eventId, pushType, deviceToken);
        Lock lock = locks.get(key);
        lock.lock();
        try {
            long now = System.currentTimeMillis();
            Long expiresAt = delivered.get(key);
            if (expiresAt != null && expiresAt > now) {
                return false;
            }
            if (expiresAt != null) {
                delivered.remove(key, expiresAt);
            }

            delivery.run();
            delivered.put(key, now + retentionMillis());
            opportunisticCleanup(now);
            return true;
        } finally {
            lock.unlock();
        }
    }

    private long retentionMillis() {
        long seconds = retentionSeconds > 0 ? retentionSeconds : DEFAULT_RETENTION_SECONDS;
        return seconds * 1000L;
    }

    private void opportunisticCleanup(long now) {
        if (delivered.size() < 1024) {
            return;
        }
        delivered.entrySet().removeIf(entry -> entry.getValue() <= now);
    }

    private String digest(String eventId, int pushType, String deviceToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            update(digest, eventId);
            digest.update((byte) 0);
            update(digest, String.valueOf(pushType));
            digest.update((byte) 0);
            update(digest, deviceToken == null ? "" : deviceToken);
            byte[] bytes = digest.digest();
            StringBuilder result = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                result.append(String.format("%02x", value & 0xff));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private void update(MessageDigest digest, String value) {
        digest.update(value.getBytes(StandardCharsets.UTF_8));
    }
}
