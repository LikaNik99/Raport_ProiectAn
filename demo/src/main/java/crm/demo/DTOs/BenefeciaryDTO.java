package crm.demo.DTOs;

import crm.demo.Enteties.Beneficiary;

public record BenefeciaryDTO(
        Long id,
        String benificiatyFirstName,
        String companyName
) {
    public static BenefeciaryDTO from(Beneficiary beneficiary) {
        return new BenefeciaryDTO(
                beneficiary.getId(),
                beneficiary.getBenificiatyFirstName(),
                beneficiary.getCompanyName()
        );
    }
}
