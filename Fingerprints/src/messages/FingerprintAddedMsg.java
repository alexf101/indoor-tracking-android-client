package messages;

public class FingerprintAddedMsg extends Msg {

    private static final long serialVersionUID = 1;
    public boolean success;
    public Throwable error;

    public FingerprintAddedMsg() {
        this.header = Header.FingerprintAdded;
    }

    public FingerprintAddedMsg(boolean success, Throwable error) {
        this();
        this.success = success;
        this.error = error;
    }
}
