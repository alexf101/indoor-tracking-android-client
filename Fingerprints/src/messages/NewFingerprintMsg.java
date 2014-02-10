package messages;

import datatypes.Fingerprint;
import datatypes.User;

public class NewFingerprintMsg extends AuthMsg {

    public Fingerprint fingerprint;

    public NewFingerprintMsg(Fingerprint fingerprint, User user) {
        super(user);
        this.header = Header.NewFingerprint;
        this.fingerprint = fingerprint;
    }
}
