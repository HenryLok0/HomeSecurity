package ict.mgame.homesecurity;

public class AlertItem {
    public final String message;
    public final String time;
    public final String uri;

    public AlertItem(String message, String time, String uri) {
        this.message = message;
        this.time = time;
        this.uri = uri;
    }
}
