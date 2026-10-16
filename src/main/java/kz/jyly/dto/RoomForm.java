package kz.jyly.dto;

import jakarta.validation.constraints.*;
import kz.jyly.enums.RoomType;

import java.math.BigDecimal;

/** Әкімші бөлмені қосу/өңдеу формасы. */
public class RoomForm {

    @NotBlank(message = "Бөлме атауын енгізіңіз")
    @Size(max = 60, message = "Атауы 60 таңбадан аспауы керек")
    private String name;

    @NotNull(message = "Бөлме түрін таңдаңыз")
    private RoomType type;

    @NotBlank(message = "Қысқа сипаттама енгізіңіз")
    @Size(max = 160, message = "Қысқа сипаттама 160 таңбадан аспауы керек")
    private String shortDescription;

    @NotBlank(message = "Толық сипаттама енгізіңіз")
    @Size(max = 2000, message = "Сипаттама 2000 таңбадан аспауы керек")
    private String description;

    @NotNull(message = "Сыйымдылықты көрсетіңіз")
    @Min(value = 1, message = "Сыйымдылық кемінде 1 адам")
    @Max(value = 200, message = "Сыйымдылық 200 адамнан аспауы керек")
    private Integer capacity;

    @NotNull(message = "Ауданды көрсетіңіз")
    @Min(value = 4, message = "Аудан кемінде 4 м²")
    @Max(value = 2000, message = "Аудан тым үлкен")
    private Integer area;

    @NotNull(message = "Сағаттық бағаны көрсетіңіз")
    @DecimalMin(value = "500", message = "Сағаттық баға кемінде 500 ₸")
    @DecimalMax(value = "1000000", message = "Сағаттық баға тым жоғары")
    @Digits(integer = 7, fraction = 0, message = "Бағаны бүтін теңгемен көрсетіңіз")
    private BigDecimal hourlyRate;

    /** Әр жолда бір жабдық. */
    @Size(max = 1500, message = "Жабдықтар тізімі тым ұзын")
    private String equipmentText;

    @Size(max = 300, message = "Сурет сілтемесі тым ұзын")
    @Pattern(regexp = "^$|^/images/[A-Za-z0-9_./-]+$|^https://[^\\s\"'<>]+$",
            message = "Сурет сілтемесі /images/... немесе https://... түрінде болуы керек")
    private String imageUrl;

    private boolean active = true;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public RoomType getType() { return type; }
    public void setType(RoomType type) { this.type = type; }
    public String getShortDescription() { return shortDescription; }
    public void setShortDescription(String shortDescription) { this.shortDescription = shortDescription; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
    public Integer getArea() { return area; }
    public void setArea(Integer area) { this.area = area; }
    public BigDecimal getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(BigDecimal hourlyRate) { this.hourlyRate = hourlyRate; }
    public String getEquipmentText() { return equipmentText; }
    public void setEquipmentText(String equipmentText) { this.equipmentText = equipmentText; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
