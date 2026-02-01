package crm.demo.Components;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
public class AppConfig {
    private String siteName = "My App";
    private String defaultLanguage = "en";
    private String theme = "light";
    private int itemsPerPage = 20;
}
