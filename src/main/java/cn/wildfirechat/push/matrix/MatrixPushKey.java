package cn.wildfirechat.push.matrix;

import cn.wildfirechat.push.android.AndroidPushType;

final class MatrixPushKey {
    final int pushType;
    final String token;

    private MatrixPushKey(int pushType, String token) {
        this.pushType = pushType;
        this.token = token;
    }

    static MatrixPushKey parse(String pushKey) {
        if (pushKey == null) {
            return null;
        }
        int separator = pushKey.indexOf(':');
        if (separator <= 0 || separator == pushKey.length() - 1) {
            return null;
        }

        String provider = pushKey.substring(0, separator).trim().toLowerCase();
        String token = pushKey.substring(separator + 1);
        if (token.trim().isEmpty()) {
            return null;
        }

        switch (provider) {
            case "xiaomi":
            case "mipush":
                return new MatrixPushKey(AndroidPushType.ANDROID_PUSH_TYPE_XIAOMI, token);
            case "huawei":
            case "hms":
                return new MatrixPushKey(AndroidPushType.ANDROID_PUSH_TYPE_HUAWEI, token);
            case "vivo":
                return new MatrixPushKey(AndroidPushType.ANDROID_PUSH_TYPE_VIVO, token);
            case "oppo":
            case "heytap":
                return new MatrixPushKey(AndroidPushType.ANDROID_PUSH_TYPE_OPPO, token);
            case "fcm":
                return new MatrixPushKey(AndroidPushType.ANDROID_PUSH_TYPE_FCM, token);
            case "getui":
                return new MatrixPushKey(AndroidPushType.ANDROID_PUSH_TYPE_GETUI, token);
            case "honor":
                return new MatrixPushKey(AndroidPushType.ANDROID_PUSH_TYPE_HONOR, token);
            case "unipush":
                return new MatrixPushKey(AndroidPushType.PUSH_TYPE_UNIPUSH_V2, token);
            default:
                return null;
        }
    }
}
