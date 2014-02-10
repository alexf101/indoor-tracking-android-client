package messages;

import datatypes.Fingerprint;

/**
 * This class represents a request that the server return the current location, given a Fingerprint.
 */
public class LocateMeMsg extends Msg {

    public Fingerprint fingerprint;
    public String building;

    public LocateMeMsg(){
        header = Header.locateMeFromFingerprint;
    }

    public LocateMeMsg(Fingerprint fingerprint, String buildingName) {
        this();
        this.fingerprint = fingerprint;
        this.building = buildingName;
    }
}
