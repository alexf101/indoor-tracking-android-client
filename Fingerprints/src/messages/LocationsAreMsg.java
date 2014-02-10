package messages;

import datatypes.Fingerprint;
import datatypes.Location;

public class LocationsAreMsg extends Msg {

    public Location[] locations;
    public Fingerprint fingerprint;

    private LocationsAreMsg(){
        this.header = Header.LocationsAre;
    }

    public LocationsAreMsg(Location[] locations) {
        this();
        this.locations = locations;
    }
}
