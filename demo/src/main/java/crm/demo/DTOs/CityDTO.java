package crm.demo.DTOs;

import crm.demo.Enteties.City;
public record CityDTO(
        Long id,
        String city
) {
    public static CityDTO from(City city){
        return new CityDTO(
                city.getId(),
                city.getCity()
        );
    }
}
