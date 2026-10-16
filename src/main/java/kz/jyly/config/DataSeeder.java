package kz.jyly.config;

import kz.jyly.dto.PriceQuote;
import kz.jyly.entity.Booking;
import kz.jyly.entity.Customer;
import kz.jyly.entity.Guest;
import kz.jyly.entity.Payment;
import kz.jyly.entity.Room;
import kz.jyly.enums.BookingStatus;
import kz.jyly.enums.PaymentType;
import kz.jyly.enums.RoomType;
import kz.jyly.enums.TransactionStatus;
import kz.jyly.repository.BookingRepository;
import kz.jyly.repository.CustomerRepository;
import kz.jyly.repository.PaymentRepository;
import kz.jyly.repository.RoomRepository;
import kz.jyly.service.BookingNumberGenerator;
import kz.jyly.service.BookingRules;
import kz.jyly.service.PaymentService;
import kz.jyly.service.PricingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Демо деректер. Бөлмелер кестесі бос болса ғана іске қосылады, сондықтан қосымшаны
 * қайта іске қосқанда деректер қайталанбайды. Барлық сомалар PricingService арқылы есептеледі.
 */
@Component
@Order(2)
@ConditionalOnProperty(name = "jyly.seed-demo-data", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final RoomRepository rooms;
    private final CustomerRepository customers;
    private final BookingRepository bookings;
    private final PaymentRepository payments;
    private final PricingService pricing;
    private final PaymentService paymentService;
    private final BookingRules rules;
    private final BookingNumberGenerator generator;

    public DataSeeder(RoomRepository rooms, CustomerRepository customers, BookingRepository bookings,
                      PaymentRepository payments, PricingService pricing, PaymentService paymentService,
                      BookingRules rules, BookingNumberGenerator generator) {
        this.rooms = rooms;
        this.customers = customers;
        this.bookings = bookings;
        this.payments = payments;
        this.pricing = pricing;
        this.paymentService = paymentService;
        this.rules = rules;
        this.generator = generator;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (rooms.count() > 0) {
            log.info("Demo data already present — skipping seeding");
            return;
        }

        Room focus = rooms.save(room("Focus Room", "focus-room", RoomType.FOCUS, 4, 14, 6_000,
                "Жеке кездесулер мен келіссөздерге арналған тыныш, жайлы бөлме.",
                "Focus Room — екі-төрт адамға арналған тыныш бөлме. Жеке кеңес, сұхбат немесе маңызды келіссөз үшін "
                        + "қажеттінің бәрі бар: жақсы дыбыс оқшаулау, табиғи жарық және жайлы жұмыс үстелі.",
                "/images/rooms/focus.jpg",
                List.of("43 дюймдік Smart TV экраны", "Жоғары жылдамдықты Wi‑Fi", "Дыбыс оқшаулау", "Тегін кофе мен шай")));

        Room connect = rooms.save(room("Connect Room", "connect-room", RoomType.NETWORKING, 12, 36, 12_000,
                "Networking кездесулері мен шағын командаларға арналған ашық кеңістік.",
                "Connect Room — networking кездесулеріне, командалық талқылауларға және бейресми әңгімелерге арналған "
                        + "ашық кеңістік. Жұмсақ лаунж-аймақ пен ыңғайлы үстелдер жаңа таныстықтарға қолайлы атмосфера жасайды.",
                "/images/rooms/connect.jpg",
                List.of("Жұмсақ лаунж-аймақ", "Сымсыз презентация жүйесі", "Магнитті ақ тақта", "Кофе-брейк аймағы")));

        Room studio = rooms.save(room("Studio Room", "studio-room", RoomType.STUDIO, 30, 72, 20_000,
                "Презентациялар, талқылаулар және шағын іс-шараларға арналған кең студия.",
                "Studio Room — презентациялар, воркшоптар және шағын іс-шараларға арналған кең студия. Жиһазды театр, "
                        + "сынып немесе дөңгелек үстел форматында орналастыруға болады.",
                "/images/rooms/studio.jpg",
                List.of("Проектор және 3 метрлік экран", "Дыбыс жүйесі және 2 микрофон", "Сахналық жарық", "Жылжымалы жиһаз")));

        Room executive = rooms.save(room("Executive Room", "executive-room", RoomType.EXECUTIVE, 10, 40, 18_000,
                "Іскерлік кездесулерге арналған премиум келіссөз бөлмесі.",
                "Executive Room — серіктестермен және инвесторлармен кездесуге арналған премиум келіссөз бөлмесі. "
                        + "Панорамалық терезелер, бейнеконференция жүйесі және жеке ресепшн қызметі кездесуді жоғары "
                        + "деңгейде өткізуге көмектеседі.",
                "/images/rooms/executive.jpg",
                List.of("75 дюймдік 4K дисплей", "Бейнеконференция жүйесі", "Панорамалық терезелер", "Жеке ресепшн қызметі")));

        Customer aigerim = customers.save(customer("Айгерім", "Сейтқали", "+7 701 234 56 78", "aigerim@example.kz", "Steppe Labs"));
        Customer daulet = customers.save(customer("Дәулет", "Нұрланов", "+7 702 345 67 89", "daulet@example.kz", "Qazaq Ventures"));
        Customer madina = customers.save(customer("Мәдина", "Есенова", "+7 705 456 78 90", "madina@example.kz", null));
        Customer erlan = customers.save(customer("Ерлан", "Жақсыбеков", "+7 707 567 89 01", "erlan@example.kz", "Altyn Group"));

        LocalDate today = rules.today();
        LocalDateTime now = rules.timestamp();

        // 1. Расталған, депозит төленген (README-дегі демо іздеу үшін тұрақты нөмір).
        Booking b1 = booking("JY-DEMO-2026", executive, aigerim, today.plusDays(2), 10, 13, 6,
                "Инвесторлармен кездесу", BookingStatus.CONFIRMED, now.minusDays(5));
        b1.addGuest(new Guest("Арман", "Төлеуов", "arman@example.kz", "Qazaq Ventures"));
        b1.addGuest(new Guest("Жанна", "Омарова", null, "Steppe Labs"));
        bookings.save(b1);
        pay(b1, PaymentType.DEPOSIT, b1.getDepositAmount(), TransactionStatus.SUCCEEDED, now.minusDays(5));

        // 2. Күтілуде: депозит әрекеті сәтсіз болды — төленген сомаға қосылмайды.
        Booking b2 = booking(null, connect, daulet, today.plusDays(3), 15, 18, 10,
                "Стартаптар networking кеші", BookingStatus.PENDING, now.minusDays(1));
        b2.addGuest(new Guest("Ләззат", "Әбенова", "lazzat@example.kz", null));
        bookings.save(b2);
        pay(b2, PaymentType.DEPOSIT, b2.getDepositAmount(), TransactionStatus.FAILED, now.minusDays(1));

        // 3. Толық төленген.
        Booking b3 = booking(null, studio, madina, today.plusDays(6), 11, 15, 24,
                "Өнім презентациясы", BookingStatus.CONFIRMED, now.minusDays(8));
        bookings.save(b3);
        pay(b3, PaymentType.DEPOSIT, b3.getDepositAmount(), TransactionStatus.SUCCEEDED, now.minusDays(8));
        pay(b3, PaymentType.BALANCE, b3.getTotalAmount().subtract(b3.getDepositAmount()), TransactionStatus.SUCCEEDED, now.minusDays(2));

        // 4. Аяқталған, толық төленген.
        Booking b4 = booking(null, focus, erlan, today.minusDays(3), 9, 11, 3,
                "Серіктестермен келіссөз", BookingStatus.COMPLETED, now.minusDays(10));
        bookings.save(b4);
        pay(b4, PaymentType.DEPOSIT, b4.getDepositAmount(), TransactionStatus.SUCCEEDED, now.minusDays(10));
        pay(b4, PaymentType.BALANCE, b4.getTotalAmount().subtract(b4.getDepositAmount()), TransactionStatus.SUCCEEDED, now.minusDays(3));

        // 5. Тоқтатылған, депозит қайтарылған — бұл уақыт қайтадан бос.
        Booking b5 = booking(null, connect, aigerim, today.plusDays(1), 10, 12, 8,
                "Командалық стратегиялық сессия", BookingStatus.CANCELLED, now.minusDays(6));
        b5.setCancelledAt(now.minusDays(3));
        b5.setCancellationReason("Клиент тегін тоқтату мерзімінде тоқтатты");
        bookings.save(b5);
        pay(b5, PaymentType.DEPOSIT, b5.getDepositAmount(), TransactionStatus.SUCCEEDED, now.minusDays(6));
        pay(b5, PaymentType.REFUND, b5.getDepositAmount(), TransactionStatus.SUCCEEDED, now.minusDays(3));

        // 6. Ішінара төленген: депозит + қалған соманың бір бөлігі.
        Booking b6 = booking(null, executive, erlan, today.plusDays(1), 14, 16, 8,
                "Директорлар кеңесі", BookingStatus.CONFIRMED, now.minusDays(4));
        bookings.save(b6);
        pay(b6, PaymentType.DEPOSIT, b6.getDepositAmount(), TransactionStatus.SUCCEEDED, now.minusDays(4));
        pay(b6, PaymentType.BALANCE, new BigDecimal("10000"), TransactionStatus.SUCCEEDED, now.minusDays(1));

        for (Booking b : List.of(b1, b2, b3, b4, b5, b6)) {
            paymentService.recalculate(b);
        }
        log.info("Demo data seeded: {} rooms, {} customers, {} bookings, {} payments",
                rooms.count(), customers.count(), bookings.count(), payments.count());
    }

    private Room room(String name, String slug, RoomType type, int capacity, int area, int rate,
                      String shortDescription, String description, String image, List<String> equipment) {
        Room r = new Room();
        r.setName(name);
        r.setSlug(slug);
        r.setType(type);
        r.setCapacity(capacity);
        r.setArea(area);
        r.setHourlyRate(BigDecimal.valueOf(rate));
        r.setShortDescription(shortDescription);
        r.setDescription(description);
        r.setImageUrl(image);
        r.getEquipment().addAll(equipment);
        r.setActive(true);
        return r;
    }

    private Customer customer(String first, String last, String phone, String email, String company) {
        Customer c = new Customer();
        c.setFirstName(first);
        c.setLastName(last);
        c.setPhone(phone);
        c.setEmail(email);
        c.setCompany(company);
        return c;
    }

    private Booking booking(String number, Room room, Customer customer, LocalDate date, int startHour, int endHour,
                            int guests, String purpose, BookingStatus status, LocalDateTime createdAt) {
        LocalDateTime start = date.atTime(LocalTime.of(startHour, 0));
        LocalDateTime end = date.atTime(LocalTime.of(endHour, 0));
        PriceQuote quote = pricing.quote(room.getHourlyRate(), start, end);
        Booking b = new Booking();
        b.setBookingNumber(number != null ? number : generator.next());
        b.setRoom(room);
        b.setCustomer(customer);
        b.setStartDateTime(start);
        b.setEndDateTime(end);
        b.setGuestCount(guests);
        b.setPurpose(purpose);
        b.setHourlyRate(quote.hourlyRate());
        b.setBillableHours(quote.billableHours());
        b.setTotalAmount(quote.totalAmount());
        b.setDepositAmount(quote.depositAmount());
        b.setStatus(status);
        b.setCreatedAt(createdAt);
        return b;
    }

    private void pay(Booking booking, PaymentType type, BigDecimal amount, TransactionStatus status, LocalDateTime at) {
        Payment p = new Payment();
        p.setBooking(booking);
        p.setAmount(amount);
        p.setPaymentType(type);
        p.setStatus(status);
        p.setTransactionReference(generator.transactionReference(type == PaymentType.REFUND ? "DEMO-RF" : "DEMO"));
        p.setDemo(true);
        p.setNote(status == TransactionStatus.SUCCEEDED ? "Демо деректер: нақты ақша есептен шығарылған жоқ"
                : "Демо деректер: банк операцияны қабылдамады");
        p.setCreatedAt(at);
        payments.save(p);
    }
}
