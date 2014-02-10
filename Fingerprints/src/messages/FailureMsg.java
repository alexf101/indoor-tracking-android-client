package messages;

public class FailureMsg extends Msg {

    public String detail;

    private FailureMsg(){
        this.header = Header.Failure;
        this.detail = "Unknown cause";
    }

    public FailureMsg(String reasonForFailure) {
        this();
        this.detail = reasonForFailure;
    }

}
