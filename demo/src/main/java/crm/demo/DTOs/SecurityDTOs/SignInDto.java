package crm.demo.DTOs.SecurityDTOs;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "SignIn Request")
public class SignInDto {
    @Schema(description = "User Login", example = "johnsnow")
    @Size(min = 3, max = 25, message = "User Login should contain from 3 to 25 characters")
    @NotBlank(message = "User Login should not be blank")
    private String login;
    @Schema(description = "Password", example = "my_secret_password")
    @Size(max = 255, message = "Password should contain not more than 255 characters")
    @NotBlank(message = "Password should not be blank")
    private String password;
}
