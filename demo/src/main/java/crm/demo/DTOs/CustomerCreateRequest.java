package crm.demo.DTOs;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerCreateRequest {
    private String nickName;
    private Long crmUserId;
}