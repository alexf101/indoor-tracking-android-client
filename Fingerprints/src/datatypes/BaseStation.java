package datatypes;

public class BaseStation {
    public final String bssid;
    public final String ssid;
    public final int frequency;

    public BaseStation(String bssid, String ssid, int frequency) {
        this.frequency = frequency;
        this.bssid = bssid;
        this.ssid = ssid;
    }
}
