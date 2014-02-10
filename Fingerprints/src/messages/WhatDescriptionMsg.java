package messages;

public class WhatDescriptionMsg extends Msg {

    private static final long serialVersionUID = 1;
    public String location;
    public String building;

    private WhatDescriptionMsg(){
        this.header = Header.WhatDescription;
    }

    public WhatDescriptionMsg(String location, String building) {
        this();
        this.location = location;
        this.building = building;
    }
}
