package messages;

public class NewUserMsg extends Msg {
    public String password;

    public NewUserMsg() {
        this.header = Header.NewUser;
    }

    public NewUserMsg(String password) {
        this();
        this.password = password;
    }
}
