package datatypes;


/**
 * Immutable data structure. Hash/equals on frequency, bssid and ssid, but compares on level.
 */
public class FingerprintScan implements Comparable {
    private BaseStation base_station;
    private final int level;

    public FingerprintScan(String bssid, String ssid, int frequency, int level) {
        this.base_station = new BaseStation(bssid, ssid, frequency);
        this.level = level;
    }

    public FingerprintScan(FingerprintScan bs, int level) {
        this(bs.getBssid(), bs.getSsid(), bs.getFrequency(), level);
    }

    /**
     * Returns a unique identifier for this base station (bssid + frequency)
     *
     * @return
     */
    public String uid() {
        return getBssid() + ":" + getFrequency();
    }

    @Override
    public String toString() {
        return "(BSSID: " + getBssid() + ", SSID: " + getSsid() + ", Frequency: " + getFrequency() + ", Level: " + getLevel() + ")";
    }


    /**
     * Compares for uniqueness on bssid and frequency
     *
     * @return
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FingerprintScan that = (FingerprintScan) o;

        if (getFrequency() != that.getFrequency()) return false;
        if (getBssid() != null ? !getBssid().equals(that.getBssid()) : that.getBssid() != null) return false;

        return true;
    }

    /**
     * Compares for uniqueness on bssid and frequency
     *
     * @return
     */
    @Override
    public int hashCode() {
        int result = getFrequency();
        result = 31 * result + (getBssid() != null ? getBssid().hashCode() : 0);
        return result;
    }

    /**
     * Greater than/less than comparison on level
     * @param another
     * @return
     */
    @Override
    public int compareTo(Object another) {
        FingerprintScan otherBS = (FingerprintScan) another;
        return otherBS.getLevel() - this.getLevel();
    }

    public int getLevel() {
        return level;
    }

    public String getBssid() {
        return base_station.bssid;
    }

    public String getSsid() {
        return base_station.ssid;
    }

    public int getFrequency() {
        return base_station.frequency;
    }
}
