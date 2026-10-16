package kz.jyly.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Бар брондауды өңдеу: уақыт пен бөлме өзгермейді (олар бағаға және төлемдерге әсер етеді),
 * тек мақсатты, ескертпелерді, қонақтар санын және тізімін өзгертуге болады.
 */
public class BookingEditForm {

    @NotNull(message = "Қонақтар санын көрсетіңіз")
    @Min(value = 1, message = "Қонақтар саны нөлден көп болуы керек")
    @Max(value = 500, message = "Қонақтар саны тым көп")
    private Integer guestCount;

    @NotBlank(message = "Кездесу мақсатын көрсетіңіз")
    @Size(max = 120, message = "Мақсат 120 таңбадан аспауы керек")
    private String purpose;

    @Size(max = 1000, message = "Қосымша тілектер 1000 таңбадан аспауы керек")
    private String notes;

    @Valid
    @Size(max = 50, message = "Қонақтар тізімі тым ұзын")
    private List<GuestForm> guests = new ArrayList<>();

    public Integer getGuestCount() { return guestCount; }
    public void setGuestCount(Integer guestCount) { this.guestCount = guestCount; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public List<GuestForm> getGuests() { return guests; }
    public void setGuests(List<GuestForm> guests) { this.guests = guests; }
}
