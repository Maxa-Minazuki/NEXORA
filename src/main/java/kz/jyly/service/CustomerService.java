package kz.jyly.service;

import kz.jyly.dto.BookingForm;
import kz.jyly.dto.CustomerSummary;
import kz.jyly.entity.Customer;
import kz.jyly.exception.NotFoundException;
import kz.jyly.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    /**
     * Клиентті электрондық пошта бойынша табады немесе жасайды.
     * Бар клиенттің аты мен телефоны ашық формадан қайта жазылмайды: әйтпесе кез келген адам
     * бөтен поштаны енгізіп, басқа клиенттің деректерін өзгерте алар еді.
     */
    @Transactional
    public Customer findOrCreate(BookingForm form) {
        String email = form.getEmail().trim().toLowerCase(Locale.ROOT);
        return customerRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
            Customer customer = new Customer();
            customer.setFirstName(form.getFirstName().trim());
            customer.setLastName(form.getLastName().trim());
            customer.setPhone(form.getPhone().trim());
            customer.setEmail(email);
            customer.setCompany(Text.blankToNull(form.getCompany()));
            return customerRepository.save(customer);
        });
    }

    @Transactional(readOnly = true)
    public List<CustomerSummary> summaries(String query) {
        return customerRepository.summaries(Text.likePattern(query));
    }

    @Transactional(readOnly = true)
    public Customer get(Long id) {
        return customerRepository.findById(id).orElseThrow(() -> new NotFoundException("Клиент табылмады"));
    }
}
