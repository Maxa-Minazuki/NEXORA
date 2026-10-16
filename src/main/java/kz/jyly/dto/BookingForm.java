package kz.jyly.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** Жаңа брондау формасы. Сервер барлық өрістерді қайта тексереді. */
public class BookingForm {

    @NotNull(message = "Бөлмені таңдаңыз")
    private Long roomId;

    @NotNull(message = "Күнді таңдаңыз")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;

    @NotNull(message = "Басталу уақытын таңдаңыз")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @NotNull(message = "Аяқталу уақытын таңдаңыз")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime endTime;

    @NotNull(message = "Қонақтар санын көрсетіңіз")
    @Min(value = 1, message = "Қонақтар саны нөлден көп болуы керек")
    @Max(value = 500, message = "Қонақтар саны тым көп")
    private Integer guestCount;

    @NotBlank(message = "Кездесу мақсатын көрсетіңіз")
    @Size(max = 120, message = "Мақсат 120 таңбадан аспауы керек")
    private String purpose;

    @Size(max = 1000, message = "Қосымша тілектер 1000 таңбадан аспауы керек")
    private String notes;

    @NotBlank(message = "Атыңызды енгізіңіз")
    @Size(max = 60, message = "Аты 60 таңбадан аспауы керек")
    private String firstName;

    @NotBlank(message = "Тегіңізді енгізіңіз")
    @Size(max = 60, message = "Тегі 60 таңбадан аспауы керек")
    private String lastName;

    @NotBlank(message = "Телефон нөмірін енгізіңіз")
    @Pattern(regexp = "^\\+?[0-9][0-9 ()\\-]{9,19}$", message = "Телефон нөмірі дұрыс емес (мысалы, +7 701 123 45 67)")
    private String phone;

    @NotBlank(message = "Электрондық поштаны енгізіңіз")
    @Email(message = "Электрондық пошта дұрыс емес")
    @Size(max = 120, message = "Электрондық пошта тым ұзын")
    private String email;

    @Size(max = 120, message = "Компания атауы тым ұзын")
    private String company;

    @Valid
    @Size(max = 50, message = "Қонақтар тізімі тым ұзын")
    private List<GuestForm> guests = new ArrayList<>();

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public Integer getGuestCount() { return guestCount; }
    public void setGuestCount(Integer guestCount) { this.guestCount = guestCount; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
    public List<GuestForm> getGuests() { return guests; }
    public void setGuests(List<GuestForm> guests) { this.guests = guests; }
}
