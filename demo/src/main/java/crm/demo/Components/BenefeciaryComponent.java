package crm.demo.Components;

import crm.demo.DTOs.BenefeciaryDTO;
import crm.demo.DTOs.CityDTO;
import crm.demo.DTOs.ProjectDTO;
import crm.demo.Enteties.Beneficiary;
import crm.demo.Enteties.City;
import crm.demo.Repositories.BenefeciaryRepository;
import crm.demo.Repositories.CitiesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/")
@RequiredArgsConstructor
public class BenefeciaryComponent {
    private final BenefeciaryRepository benefeciaryRepository;
    private final CitiesRepository cityRepository;
    @GetMapping("benefeciaries")
    public List<BenefeciaryDTO> getAllBenefeciaries(){
        return benefeciaryRepository.findAll()
                .stream()
                .map(BenefeciaryDTO::from)
                .toList();
    }
    @GetMapping("cities")
    public List<CityDTO> getAllCities(){
        return cityRepository.findAll()
                .stream()
                .map(CityDTO::from)
                .toList();
    }
}
