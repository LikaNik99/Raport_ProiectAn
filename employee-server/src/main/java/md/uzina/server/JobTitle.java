package md.uzina.server;

import java.time.LocalDateTime;

public class JobTitle {
    public int id;
    public String title;
    public String description;
    public LocalDateTime createdAt;
    
    public JobTitle(int id, String title, String description, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.createdAt = createdAt;
    }
}
