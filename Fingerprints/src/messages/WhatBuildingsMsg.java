package messages;

import java.io.Serializable;

public class WhatBuildingsMsg extends Msg implements Serializable {

    private static final long serialVersionUID = 1;

    public WhatBuildingsMsg(){
        this.header = Header.WhatBuildings;
    }

}
