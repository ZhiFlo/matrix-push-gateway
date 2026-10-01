package cn.wildfirechat.push.matrix;

import java.util.List;
import java.util.Map;

public class MatrixPushRequest {
    public Notification notification;

    public static class Notification {
        public String event_id;
        public String room_id;
        public String type;
        public String sender;
        public String sender_display_name;
        public String room_name;
        public String room_alias;
        public String prio;
        public Counts counts;
        public List<Device> devices;
    }

    public static class Counts {
        public int unread;
        public int missed_calls;
    }

    public static class Device {
        public String app_id;
        public String pushkey;
        public Long pushkey_ts;
        public Map<String, Object> data;
        public Map<String, Object> tweaks;
    }
}
