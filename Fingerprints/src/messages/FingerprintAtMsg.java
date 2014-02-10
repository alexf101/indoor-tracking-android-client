package messages;

import datatypes.Fingerprint;
import datatypes.Location;

import java.io.Serializable;

/**
 * This message represents a request that the server store the provided fingerprint as matching the provided location
 */
public class FingerprintAtMsg extends Msg implements Serializable {

    public Fingerprint fingerprint;
    public Location location;

    private static final long serialVersionUID = 1;

    public FingerprintAtMsg(){
        header = Header.fingerprintAtLocation;
    }

    public FingerprintAtMsg(Fingerprint fingerprint, Location location) {
        this();
        this.fingerprint = fingerprint;
        this.location = location;
    }

}
