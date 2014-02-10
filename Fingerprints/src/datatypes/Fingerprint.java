package datatypes;

import android.text.format.Time;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * This data structure represents a location signature.
 * <p/>
 * For the moment, this means scan results, magnetic field components, and location.
 */
public class Fingerprint {
    public String timestamp;

    public List<FingerprintScan> getScans() {
        Collections.sort(scans);
        return scans;
    }

    /**
     * May be null
     */
    public String location;
    /**
     * May be null
     */
    public String url;
    private List<FingerprintScan> scans;
    public float zaxis;
    public float magnitude;
    public float direction;
    /**
     * can be overridden - the device model that recorded this fingerprint *
     */
    public String device = android.os.Build.MODEL;
    private long id = -1;

    public Fingerprint(List<FingerprintScan> scans) {
        this(scans, 0, 0, 0);
    }

    /**
     * A Fingerprint represents all of the measurements that can be performed by a smartphone
     * at a location, that could conceivably be unique in some way to that location.
     *
     * @param scans         RSSID signal strengths
     * @param zMagComponent Z-component of magnetic field (aka. vertical)
     * @param magTotal      Sum of magnetic field strength along all axes (not magnitude per se)
     */
    public Fingerprint(List<FingerprintScan> scans, float zMagComponent, float magTotal, float direction) {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        timestamp = fmt.format(date);
        this.scans = scans;
        this.direction = direction;
        setScans(scans);
        this.zaxis = zMagComponent;
        this.magnitude = magTotal;
    }

    @Override
    public String toString() {
        return "Fingerprint{" +
                "timestamp=" + timestamp +
                ", url='" + url + '\'' +
                ", location='" + location +'\'' +
                ", scans=" + scans +
                ", zaxis=" + zaxis +
                ", magnitude=" + magnitude +
                ", direction=" + direction +
                ", id=" + id +
                '}';
    }

    public void setScans(List<FingerprintScan> scans) {
        Collections.sort(scans);
        this.scans = scans;
    }

    public static int countUniqueChannels(List<Fingerprint> fingerprints) {
        Set<FingerprintScan> bs = new HashSet<FingerprintScan>();
        for (Fingerprint fingerprint : fingerprints) {
            bs.addAll(fingerprint.getScans());
        }
        return bs.size();
    }

    public String id() {
        if (id == -1) {
            if (url == null) {
                return null;
            } else {
                String urlWithoutTrailingSlash = stripTrailingSlash(url);
                int startIndex = urlWithoutTrailingSlash.lastIndexOf("/") + 1;
                id = Long.valueOf(urlWithoutTrailingSlash.substring(startIndex));
            }
        }
        return String.valueOf(id);
    }

    public Long idLong(){
        if (id != -1) return id;
        if (url != null) {
            String urlWithoutTrailingSlash = stripTrailingSlash(url);
            int startIndex = urlWithoutTrailingSlash.lastIndexOf("/") + 1;
            id = Long.valueOf(urlWithoutTrailingSlash.substring(startIndex));
            return id;
        } else {
            return null;
        }
    }

    private String stripTrailingSlash(String url) {
        return url.charAt(url.length()) == '/' ? url.substring(0, url.length() - 1) : url;
    }

    /**
     * Returns the unique BSSIDs in the scans of this fingerprint
     * @return Set of strings, one for each unique BSSID
     */
    public Set uniqueBSSIDs() {
        Set<String> uniqueBSSIDs = new HashSet<String>();
        for (FingerprintScan fingerprintScan : getScans()) {
            uniqueBSSIDs.add(fingerprintScan.getBssid());
        }
        return uniqueBSSIDs;
    }
}
