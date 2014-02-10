package messages;

import datatypes.Location;
import datatypes.User;

public class UpdateLocationMsg extends AuthMsg {

    public Location location;

    public UpdateLocationMsg(Location location, User user) {
        super(user);
        this.header = Header.UpdateLocation;
        this.location = location;
    }

}
