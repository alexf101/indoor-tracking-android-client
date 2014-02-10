package messages;

import datatypes.User;

public class DeleteMsg extends AuthMsg {

    public DeleteMsg(User user) {
        super(user);
        this.header = Header.Delete;
    }
}
