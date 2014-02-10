package messages;


/**
 * All messages should extend this class. The header basically is supposed to tell you which subclass it is (i.e. what kind of message).
 *
 * This system provides confirmation that the message is built correctly, and interpreted correctly, as well as allowing
 * flexibility on the system of communication used. Therefore, even though the message type should be determined
 * by the url requested, it is still useful to have this header.
 */
public class Msg {

    public Header header;
    public Msg(){

    }
}
