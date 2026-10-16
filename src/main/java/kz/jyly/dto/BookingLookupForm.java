package kz.jyly.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Өз брондауын нөмірі және электрондық поштасы арқылы табу. */
public class BookingLookupForm {

    @NotBlank(message = "Брондау нөмірін енгізіңіз")
    @Size(max = 20, message = "Брондау нөмірі тым ұзын")
    private String bookingNumber;

    @NotBlank(message = "Электрондық поштаны енгізіңіз")
    @Email(message = "Электрондық пошта дұрыс емес")
    private String email;

    public String getBookingNumber() { return bookingNumber; }
    public void setBookingNumber(String bookingNumber) { this.bookingNumber = bookingNumber; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
