package crm.demo.Components;

import crm.demo.DTOs.StatusDTO;
import crm.demo.Repositories.StatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/")
@RequiredArgsConstructor
public class StatusComponent {

    private final StatusRepository statusRepository;

    @GetMapping("status")
    public List<StatusDTO> getAllStatus(){
        return statusRepository.findAll()
                .stream()
                .map(StatusDTO::from)
                .toList();
    }

}
