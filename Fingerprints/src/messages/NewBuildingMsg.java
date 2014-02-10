package messages;

import datatypes.Building;
import datatypes.User;

public class NewBuildingMsg extends AuthMsg {

    public Building building;

    public NewBuildingMsg(){
        this.header = Header.NewBuilding;
    }

    public NewBuildingMsg(User user, Building building) {
        this();
        this.owner = user;
        this.building = building;
    }
}
