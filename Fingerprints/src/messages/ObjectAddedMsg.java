package messages;

/**
 * Object was added successfully, and assigned the id referenced.
 */
public class ObjectAddedMsg extends Msg {

    public long id;

    public ObjectAddedMsg(){
        this.header = Header.ObjectAdded;
    }

    public ObjectAddedMsg(long id) {
        this();
        this.id = id;
    }

    @Override
    public String toString(){
        return "ObjectAdded: "+id;
    }
}
