package messages;

public class DescriptionIsMsg extends Msg {

    public String description;
    private static final long serialVersionUID = 1;

    private DescriptionIsMsg(){
        this.header = Header.DescriptionIs;
    }

    public DescriptionIsMsg(String description) {
        this();
        this.description = description;
    }
}
