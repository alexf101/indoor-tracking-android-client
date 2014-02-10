package messages;

public class ConfirmMsg extends Msg {
    public Boolean changed;

    public ConfirmMsg(){
        this.header = Header.Confirm;
    }

    public ConfirmMsg(Boolean changed) {
        this();
        this.changed = changed;
    }

    @Override
    public String toString(){
        return String.valueOf(changed);
    }
}
