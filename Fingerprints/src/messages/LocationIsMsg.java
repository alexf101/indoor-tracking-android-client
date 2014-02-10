package messages;

import datatypes.Location;

import java.io.Serializable;

public class LocationIsMsg extends Msg implements Serializable {

    private static final long serialVersionUID = 1;

    public Location location;

    public LocationIsMsg(){
        header = Header.locationIs;
    }

    public LocationIsMsg(Location location) {
        this();
        this.location = location;
    }
}
