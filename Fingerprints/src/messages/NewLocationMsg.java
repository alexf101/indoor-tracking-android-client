package messages;

import datatypes.Location;
import datatypes.User;

public class NewLocationMsg extends AuthMsg {

    public Location location;

    public NewLocationMsg(Location location, User user) {
        super(user);
        this.header = Header.NewLocation;
        this.location = location;
    }
}
