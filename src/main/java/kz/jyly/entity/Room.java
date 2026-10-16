package kz.jyly.entity;

import jakarta.persistence.*;
import kz.jyly.enums.RoomType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rooms",
        uniqueConstraints = @UniqueConstraint(name = "uk_room_slug", columnNames = "slug"),
        indexes = @Index(name = "idx_room_active", columnList = "active"))
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(nullable = false, length = 80)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoomType type;

    @Column(nullable = false, length = 160)
    private String shortDescription;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(nullable = false)
    private int capacity;

    /** Аудан, м². */
    @Column(nullable = false)
    private int area;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal hourlyRate;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "room_equipment", joinColumns = @JoinColumn(name = "room_id"))
    @OrderColumn(name = "position")
    @Column(name = "item", nullable = false, length = 80)
    private List<String> equipment = new ArrayList<>();

    @Column(length = 300)
    private String imageUrl;

    /** Өшірілген бөлме каталогта көрінбейді, бірақ оның брондау тарихы сақталады. */
    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public RoomType getType() { return type; }
    public void setType(RoomType type) { this.type = type; }
    public String getShortDescription() { return shortDescription; }
    public void setShortDescription(String shortDescription) { this.shortDescription = shortDescription; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public int getArea() { return area; }
    public void setArea(int area) { this.area = area; }
    public BigDecimal getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(BigDecimal hourlyRate) { this.hourlyRate = hourlyRate; }
    public List<String> getEquipment() { return equipment; }
    public void setEquipment(List<String> equipment) { this.equipment = equipment; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
