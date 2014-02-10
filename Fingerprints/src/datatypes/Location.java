package datatypes;


import util.Dbg;

import java.io.Serializable;

public class Location implements Serializable {
    public static final Location NOT_FOUND = new Location("NOT_FOUND", new Building("NOT_FOUND"));
    private String building; // url
    public Building building_obj;
    private String name;
    public String description;
    public String url;
    public String room;
    public long id;

    public Location(String name, Building building) {
        this(name, building, "", "");
    }

    public Location(String name, Building building, String room, String description, long location_id) {
        this.id = location_id;
        this.setName(name);
        this.building_obj = building;
        if (building != null){
            this.building = building_obj.getUrl();
            if (building.getUrl() != null) Dbg.logd(this.getClass().getName(), "Creating location with building url " + building.getUrl());
        } else {
            this.building = null;
        }
        if (description == null) {
            this.description = "";
        } else {
            this.description = description;
        }
        if (room == null) {
            this.room = "";
        } else {
            this.room = room;
        }
    }

    public Location(String name, Building building, String room, String description) {
        this(name, building, room, description, -1);
    }

    @Override
    public String toString() {
        return "Location{" +
                "url='" + building + '\'' +
                ", building_obj=" + building_obj +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", location='" + url + '\'' +
                ", id=" + id +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Location)) return false;

        Location otherLocation = (Location) o;

        if (building_obj != null ? !building_obj.equals(otherLocation.building_obj) : otherLocation.building_obj != null) return false;
        if (getName() != null ? !getName().equals(otherLocation.getName()) : otherLocation.getName() != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = building_obj.hashCode();
        result = 31 * result + (getName() != null ? getName().hashCode() : 0);
        return result;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name.replaceAll("'", "\\'").replaceAll("\"", "\\\"");
    }

    public String getUrl() {
        return url;
    }
}
