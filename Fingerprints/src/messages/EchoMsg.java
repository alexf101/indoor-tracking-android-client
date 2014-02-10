package messages;

public class EchoMsg extends Msg {

    public String echo;

    public EchoMsg(){
        this.header = Header.Echo;
    }

    public EchoMsg(String echo) {
        this();
        this.echo = echo;
    }
}
