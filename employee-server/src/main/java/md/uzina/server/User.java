package md.uzina.server;
public class User {
    public int id;
    public String name;
    public String role; // WORKER, TEAMLEADER, HR
    public User() {}
    public User(int id, String name, String role) {
        this.id = id; this.name = name; this.role = role;
    }
}
