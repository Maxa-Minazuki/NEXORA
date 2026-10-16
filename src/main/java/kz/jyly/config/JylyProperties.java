package kz.jyly.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * Клуб ережелері application.properties ішінде (jyly.* префиксі) сақталады,
 * сондықтан депозит пайызы, жұмыс уақыты т.б. кодта "сиқырлы сан" болып шашырамайды.
 */
@ConfigurationProperties(prefix = "jyly")
public class JylyProperties {

    /** Депозит мөлшері, жалпы құннан пайызбен. */
    private BigDecimal depositPercent = new BigDecimal("30");

    /** Клубтың уақыт белдеуі. */
    private String zone = "Asia/Almaty";

    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime openingTime = LocalTime.of(8, 0);

    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime closingTime = LocalTime.of(22, 0);

    /** Уақытты таңдау қадамы, минут. */
    private int slotMinutes = 30;

    private int minDurationMinutes = 60;

    private int maxDurationHours = 12;

    /** Қанша күн алдын ала брондауға болады. */
    private int maxDaysAhead = 90;

    /** Басталуға дейін осы сағаттан ерте тоқтатылса, депозит толық қайтарылады. */
    private int freeCancellationHours = 24;

    private final Admin admin = new Admin();

    public static class Admin {
        private String username = "admin";
        private String password;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public BigDecimal getDepositPercent() { return depositPercent; }
    public void setDepositPercent(BigDecimal depositPercent) { this.depositPercent = depositPercent; }
    public String getZone() { return zone; }
    public void setZone(String zone) { this.zone = zone; }
    public LocalTime getOpeningTime() { return openingTime; }
    public void setOpeningTime(LocalTime openingTime) { this.openingTime = openingTime; }
    public LocalTime getClosingTime() { return closingTime; }
    public void setClosingTime(LocalTime closingTime) { this.closingTime = closingTime; }
    public int getSlotMinutes() { return slotMinutes; }
    public void setSlotMinutes(int slotMinutes) { this.slotMinutes = slotMinutes; }
    public int getMinDurationMinutes() { return minDurationMinutes; }
    public void setMinDurationMinutes(int minDurationMinutes) { this.minDurationMinutes = minDurationMinutes; }
    public int getMaxDurationHours() { return maxDurationHours; }
    public void setMaxDurationHours(int maxDurationHours) { this.maxDurationHours = maxDurationHours; }
    public int getMaxDaysAhead() { return maxDaysAhead; }
    public void setMaxDaysAhead(int maxDaysAhead) { this.maxDaysAhead = maxDaysAhead; }
    public int getFreeCancellationHours() { return freeCancellationHours; }
    public void setFreeCancellationHours(int freeCancellationHours) { this.freeCancellationHours = freeCancellationHours; }
    public Admin getAdmin() { return admin; }
}
