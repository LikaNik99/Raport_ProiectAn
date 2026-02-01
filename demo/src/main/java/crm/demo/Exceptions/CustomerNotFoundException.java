package crm.demo.Exceptions;

public class CustomerNotFoundException extends RuntimeException {
    public CustomerNotFoundException(Long id){
        super("Could not find any Customer by this ID - " + id);
    }
}