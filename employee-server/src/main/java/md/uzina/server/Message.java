package md.uzina.server;
public class Message {
    public String type;
    public Object payload;
    public Message() {}
    public Message(String type, Object payload) {
        this.type = type;
        this.payload = payload;
    }
}
