package messages;

import datatypes.Building;

import java.util.Arrays;
import java.util.List;

public class BuildingsAreMsg extends Msg {

    public Building[] buildings;

    public BuildingsAreMsg(){
        this.header = Header.BuildingsAre;
    }

    public BuildingsAreMsg(Building[] buildings) {
        this();
        this.buildings = buildings;
    }

    public BuildingsAreMsg(List<Building> buildings) {
        this();
        this.buildings = buildings.toArray(new Building[buildings.size()]);
    }

    @Override
    public String toString(){
        return Arrays.toString(buildings);
    }
}
