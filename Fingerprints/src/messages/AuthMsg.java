package messages;

import datatypes.User;

public abstract class AuthMsg extends Msg {

    public User owner;

    public AuthMsg() {
        super();
    }

    public AuthMsg(User user) {
        this();
        this.owner = user;
    }
}
