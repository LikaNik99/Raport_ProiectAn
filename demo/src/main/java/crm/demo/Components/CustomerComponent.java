package crm.demo.Components;

import java.security.Principal;
import java.util.List;

import crm.demo.DTOs.CustomerCreateRequest;
import crm.demo.DTOs.CustomerDTO;
import crm.demo.Enteties.CrmUser;
import crm.demo.Repositories.CrmUserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import crm.demo.Enteties.Customer;
import crm.demo.Repositories.CustomerRepository;
import lombok.RequiredArgsConstructor;

//@CrossOrigin(origins = "http://localhost:3000, http://172.20.130.242:3000")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/")
public class CustomerComponent {

    private final CustomerRepository customerRepository;
    private final CrmUserRepository crmUserRepository;

//    http://localhost:8080/api/customers
    @GetMapping("customers")
    public List<CustomerDTO> customers(){
        return customerRepository.findAll()
                .stream()
                .map(CustomerDTO::from)
                .toList();
    }
    @GetMapping("customer/{id}")
    public Customer getCustomerById(@PathVariable Long id) {
        return customerRepository.findById(id).orElseThrow(()->new RuntimeException("No such id "+id));
    }

//    @PostMapping("/customer/add")
//    public Customer postMethodName(@RequestBody CustomerCreateRequest customerReq) {
//        CrmUser user = crmUserRepository.findById(customerReq.getCrmUserId())
//                .orElseThrow(()-> new RuntimeException("no such ID"));
//
//        Customer newCustomer = new Customer();
//        newCustomer.setNickName(customerReq.getNickName());
//        newCustomer.setCrmUser(user);
//        return customerRepository.save(newCustomer);
//    }

//    @PutMapping("customer/{id}")
//    public Customer editCustomer(@RequestBody Customer newCustomer, @PathVariable Long id, Principal principal) {
//
//        Customer updatedCustomer = customerRepository.findById(id).orElseThrow(()->new RuntimeException("no customer with such ID - "+id));
//        updatedCustomer.setNickName(newCustomer.getNickName());
//        updatedCustomer.setProject(newCustomer.getProject());
////        return customerRepository.save(updatedCustomer);
//    }
    @DeleteMapping("employee/{id}/delete")
    public void deleteById(@PathVariable Long id ){
        customerRepository.deleteById(id);
    }
}
