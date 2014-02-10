package messages;

import datatypes.Building;

public class BuildingIsMsg extends Msg {

    public Building building;

    private BuildingIsMsg(){
        this.header = Header.BuildingIs;
    }

    public BuildingIsMsg(Building building) {
        this();
        this.building = building;
    }

}
