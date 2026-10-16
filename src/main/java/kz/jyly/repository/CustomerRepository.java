package kz.jyly.repository;

import kz.jyly.dto.CustomerSummary;
import kz.jyly.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByEmailIgnoreCase(String email);

    @Query("""
            select new kz.jyly.dto.CustomerSummary(
                c.id, c.firstName, c.lastName, c.email, c.phone, c.company, c.createdAt,
                count(b.id), coalesce(sum(b.amountPaid), 0))
            from Customer c left join Booking b on b.customer = c
            where (:q is null
                   or lower(c.firstName) like :q
                   or lower(c.lastName) like :q
                   or lower(c.email) like :q
                   or lower(c.phone) like :q
                   or lower(coalesce(c.company, '')) like :q)
            group by c.id, c.firstName, c.lastName, c.email, c.phone, c.company, c.createdAt
            order by c.lastName, c.firstName
            """)
    List<CustomerSummary> summaries(@Param("q") String q);
}
