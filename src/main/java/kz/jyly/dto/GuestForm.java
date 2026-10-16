package kz.jyly.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public class GuestForm {

    @Size(max = 60, message = "Аты 60 таңбадан аспауы керек")
    private String firstName;

    @Size(max = 60, message = "Тегі 60 таңбадан аспауы керек")
    private String lastName;

    @Email(message = "Қонақтың электрондық поштасы дұрыс емес")
    @Size(max = 120, message = "Электрондық пошта тым ұзын")
    private String email;

    @Size(max = 120, message = "Компания атауы тым ұзын")
    private String company;

    public GuestForm() {
    }

    public GuestForm(String firstName, String lastName, String email, String company) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.company = company;
    }

    /** Толығымен бос жол (JavaScript қосып, бірақ толтырылмаған) есепке алынбайды. */
    public boolean isBlank() {
        return isEmpty(firstName) && isEmpty(lastName) && isEmpty(email) && isEmpty(company);
    }

    private static boolean isEmpty(String value) {
        return value == null || value.isBlank();
    }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
}
