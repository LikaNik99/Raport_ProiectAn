package crm.demo.Controllers;

import crm.demo.Enteties.AppConfig;
import crm.demo.Repositories.AppConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AppConfigController {

    private final AppConfigRepository repository;

    @GetMapping("/config")
    public AppConfig getConfig() {
        return repository.findById(1L)
                .orElseGet(() -> repository.save(new AppConfig(null, "My App", "en", "light", 20)));
    }
    @PutMapping("/config")
    public AppConfig updateConfig(@RequestBody AppConfig config) {
        config.setId(1L); // always keep one row
        return repository.save(config);
    }
}
