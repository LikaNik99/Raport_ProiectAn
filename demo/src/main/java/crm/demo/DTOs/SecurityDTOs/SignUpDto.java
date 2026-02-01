package crm.demo.DTOs.SecurityDTOs;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Schema(description = "Registration Request")
public class SignUpDto {
    @Schema(description = "User First Name", example = "John")
    @Size(min = 3, max = 50, message = "User First name should contain from 3 to 50 characters")
    @NotBlank(message = "User First name should not be blank")
    private String firstName;
    @Schema(description = "User Last Name", example = "Snow")
    @Size(min = 3, max = 50, message = "User Last name should contain from 3 to 50 characters")
    @NotBlank(message = "User Last name should not be blank")
    private String lastName;
    @Schema(description = "User Login", example = "johnsnow")
    @Size(min = 3, max = 25, message = "User Login should contain from 3 to 25 characters")
    @NotBlank(message = "User Login should not be blank")
    private String login;
    @Schema(description = "User Email", example = "johnsnow@email.com")
    @Size(max = 255, message = "User Login should contain not more than 255 characters")
    @NotBlank(message = "User Login should not be blank")
    @Email(message = "Email should be like 'user@example.com'")
    private String email;
    @Schema(description = "Password", example = "my_secret_password")
    @Size(max = 255, message = "Password should contain not more than 255 characters")
    private String password;
    private Boolean isActive=true;
    private Boolean isBlocked=false;
    private Boolean isAccountExpired=false;
}