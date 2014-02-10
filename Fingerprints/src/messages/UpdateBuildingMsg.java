package messages;

import datatypes.Building;
import datatypes.User;

public class UpdateBuildingMsg extends AuthMsg {
    public Building building;

    public UpdateBuildingMsg(){
        super();
        header = Header.UpdateBuilding;
    }

    public UpdateBuildingMsg(Building building, User user) {
        this();
        this.building = building;
        this.owner = user;
    }
}
