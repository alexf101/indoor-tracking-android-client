package messages;

import java.io.Serializable;

public class WhatLocationsMsg extends Msg implements Serializable {

    public String building;

    private static final long serialVersionUID = 1;

    public WhatLocationsMsg(){
        this.header = Header.WhatLocations;
    }

    public WhatLocationsMsg(String building) {
        this();
        this.building = building;
    }
}
