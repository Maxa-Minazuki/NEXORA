package kz.jyly.service;

import kz.jyly.dto.BookingDetails;
import kz.jyly.dto.BookingEditForm;
import kz.jyly.dto.BookingForm;
import kz.jyly.dto.GuestForm;
import kz.jyly.dto.PriceQuote;
import kz.jyly.entity.Booking;
import kz.jyly.entity.Customer;
import kz.jyly.entity.Guest;
import kz.jyly.entity.Payment;
import kz.jyly.entity.Room;
import kz.jyly.enums.BookingStatus;
import kz.jyly.event.BookingEvent;
import kz.jyly.event.BookingEventType;
import kz.jyly.exception.BookingConflictException;
import kz.jyly.exception.BusinessException;
import kz.jyly.exception.NotFoundException;
import kz.jyly.repository.BookingRepository;
import kz.jyly.repository.GuestRepository;
import kz.jyly.repository.RoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final GuestRepository guestRepository;
    private final CustomerService customerService;
    private final PricingService pricingService;
    private final PaymentService paymentService;
    private final BookingRules rules;
    private final BookingNumberGenerator numberGenerator;
    private final BookingEventPublisher eventPublisher;

    public BookingService(BookingRepository bookingRepository,
                          RoomRepository roomRepository,
                          GuestRepository guestRepository,
                          CustomerService customerService,
                          PricingService pricingService,
                          PaymentService paymentService,
                          BookingRules rules,
                          BookingNumberGenerator numberGenerator,
                          BookingEventPublisher eventPublisher) {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
        this.guestRepository = guestRepository;
        this.customerService = customerService;
        this.pricingService = pricingService;
        this.paymentService = paymentService;
        this.rules = rules;
        this.numberGenerator = numberGenerator;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Жаңа брондау жасайды. Бөлме жолы PESSIMISTIC_WRITE арқылы құлыпталады, сондықтан
     * бір бөлмеге параллель келген сұраулар кезекпен тексеріледі — қос брондау болмайды.
     */
    @Transactional
    public Booking create(BookingForm form) {
        Room room = roomRepository.findByIdForUpdate(form.getRoomId())
                .orElseThrow(() -> new BusinessException("Таңдалған бөлме табылмады", "roomId"));
        if (!room.isActive()) {
            throw new BusinessException("Бұл бөлме қазір брондауға қолжетімсіз", "roomId");
        }

        LocalDateTime start = LocalDateTime.of(form.getDate(), form.getStartTime());
        LocalDateTime end = LocalDateTime.of(form.getDate(), form.getEndTime());
        rules.validateInterval(start, end);
        rules.validateGuests(form.getGuestCount(), room);
        List<Guest> guests = toGuests(form.getGuests(), form.getGuestCount());

        if (bookingRepository.countOverlapping(room.getId(), start, end, BookingStatus.BLOCKING) > 0) {
            throw new BookingConflictException();
        }

        Customer customer = customerService.findOrCreate(form);
        PriceQuote quote = pricingService.quote(room.getHourlyRate(), start, end);

        Booking booking = new Booking();
        booking.setBookingNumber(uniqueNumber());
        booking.setRoom(room);
        booking.setCustomer(customer);
        booking.setStartDateTime(start);
        booking.setEndDateTime(end);
        booking.setGuestCount(form.getGuestCount());
        booking.setPurpose(form.getPurpose().trim());
        booking.setNotes(Text.blankToNull(form.getNotes()));
        booking.setHourlyRate(quote.hourlyRate());
        booking.setBillableHours(quote.billableHours());
        booking.setTotalAmount(quote.totalAmount());
        booking.setDepositAmount(quote.depositAmount());
        booking.setAmountPaid(BigDecimal.ZERO);
        booking.setStatus(BookingStatus.PENDING);
        booking.setCreatedAt(rules.timestamp());
        guests.forEach(booking::addGuest);

        Booking saved = bookingRepository.save(booking);
        log.info("Booking {} created: room={}, {} - {}, total={}", saved.getBookingNumber(),
                room.getName(), start, end, quote.totalAmount());
        eventPublisher.publish(BookingEvent.of(BookingEventType.BOOKING_CREATED, saved, rules.timestamp()));
        return saved;
    }

    /** Уақыт қақтығысы болғанда ұсынылатын бос уақыттар (сол күні, сол ұзақтықпен). */
    @Transactional(readOnly = true)
    public List<LocalTime> suggestAlternatives(Long roomId, LocalDate date, long durationMinutes, int limit) {
        List<LocalTime> result = new ArrayList<>();
        if (roomId == null || date == null || durationMinutes <= 0) {
            return result;
        }
        LocalDateTime dayStart = date.atTime(rules.getProperties().getOpeningTime());
        LocalDateTime dayEnd = date.atTime(rules.getProperties().getClosingTime());
        List<Booking> busy = bookingRepository.findBlockingInRange(roomId, dayStart, dayEnd, BookingStatus.BLOCKING);
        for (LocalTime slot : rules.timeSlots()) {
            LocalDateTime start = date.atTime(slot);
            LocalDateTime end = start.plusMinutes(durationMinutes);
            if (end.isAfter(dayEnd) || !start.isAfter(rules.now())) {
                continue;
            }
            boolean free = busy.stream().noneMatch(b -> b.getStartDateTime().isBefore(end) && b.getEndDateTime().isAfter(start));
            if (free) {
                result.add(slot);
                if (result.size() >= limit) {
                    break;
                }
            }
        }
        return result;
    }

    @Transactional(readOnly = true)
    public boolean isAvailable(Long roomId, LocalDateTime start, LocalDateTime end) {
        return bookingRepository.countOverlapping(roomId, start, end, BookingStatus.BLOCKING) == 0;
    }

    @Transactional(readOnly = true)
    public Booking getByNumber(String number) {
        return bookingRepository.findByBookingNumber(normalizeNumber(number))
                .orElseThrow(() -> new NotFoundException("Брондау табылмады"));
    }

    @Transactional(readOnly = true)
    public Booking getById(Long id) {
        return bookingRepository.findById(id).orElseThrow(() -> new NotFoundException("Брондау табылмады"));
    }

    /**
     * Нөмір мен электрондық пошта сәйкес келсе ғана брондауды қайтарады.
     * Қай өріс қате екенін әдейі айтпаймыз (бөтен брондауларды іздеп табуды қиындату үшін).
     */
    @Transactional(readOnly = true)
    public Optional<Booking> lookup(String number, String email) {
        if (number == null || email == null) {
            return Optional.empty();
        }
        return bookingRepository.findByBookingNumber(normalizeNumber(number))
                .filter(b -> b.getCustomer().getEmail().equalsIgnoreCase(email.trim()));
    }

    @Transactional(readOnly = true)
    public List<Booking> findByNumbers(Collection<String> numbers) {
        if (numbers.isEmpty()) {
            return List.of();
        }
        return bookingRepository.findByBookingNumberIn(numbers, Sort.by(Sort.Direction.DESC, "startDateTime"));
    }

    @Transactional(readOnly = true)
    public BookingDetails details(String number) {
        return toDetails(getByNumber(number));
    }

    @Transactional(readOnly = true)
    public BookingDetails detailsById(Long id) {
        return toDetails(getById(id));
    }

    @Transactional(readOnly = true)
    public List<Booking> adminSearch(String query, BookingStatus status, LocalDate from, LocalDate to) {
        return bookingRepository.adminSearch(Text.likePattern(query), status,
                from == null ? null : from.atStartOfDay(),
                to == null ? null : to.plusDays(1).atStartOfDay());
    }

    @Transactional(readOnly = true)
    public List<Booking> forCustomer(Long customerId) {
        return bookingRepository.findByCustomerIdOrderByStartDateTimeDesc(customerId);
    }

    /** Клиент өз брондауын тоқтатады. Қайтару сомасы тегін тоқтату ережесіне байланысты. */
    @Transactional
    public Booking cancelByCustomer(String number) {
        Booking booking = lockByNumber(number);
        if (!booking.getStartDateTime().isAfter(rules.now())) {
            throw new BusinessException("Басталған немесе өткен брондауды тоқтату мүмкін емес");
        }
        boolean free = rules.isFreeCancellation(booking);
        cancel(booking, free, free ? "Клиент тегін тоқтату мерзімінде тоқтатты" : "Клиент кеш тоқтатты");
        return booking;
    }

    /** Әкімші статусты тек рұқсат етілген бағытта өзгерте алады. */
    @Transactional
    public Booking changeStatusByAdmin(Long id, BookingStatus target) {
        Booking booking = bookingRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Брондау табылмады"));
        if (target == null || !booking.getStatus().canTransitionTo(target)) {
            throw new BusinessException("Бұл статусқа өзгерту мүмкін емес");
        }
        switch (target) {
            case CONFIRMED -> {
                booking.setStatus(BookingStatus.CONFIRMED);
                eventPublisher.publish(BookingEvent.of(BookingEventType.BOOKING_CONFIRMED, booking, rules.timestamp()));
            }
            case COMPLETED -> {
                if (booking.getEndDateTime().isAfter(rules.now())) {
                    throw new BusinessException("Брондау әлі аяқталған жоқ");
                }
                booking.setStatus(BookingStatus.COMPLETED);
            }
            // Клуб тарапынан тоқтатылса, клиентке бүкіл төленген сома қайтарылады.
            case CANCELLED -> cancel(booking, true, "Әкімші тоқтатты");
            default -> throw new BusinessException("Бұл статусқа өзгерту мүмкін емес");
        }
        log.info("Booking {} status changed by admin to {}", booking.getBookingNumber(), target);
        return booking;
    }

    @Transactional(readOnly = true)
    public BookingEditForm editForm(String number) {
        Booking booking = getByNumber(number);
        BookingEditForm form = new BookingEditForm();
        form.setGuestCount(booking.getGuestCount());
        form.setPurpose(booking.getPurpose());
        form.setNotes(booking.getNotes());
        for (Guest g : guestRepository.findByBookingIdOrderByIdAsc(booking.getId())) {
            form.getGuests().add(new GuestForm(g.getFirstName(), g.getLastName(), g.getEmail(), g.getCompany()));
        }
        return form;
    }

    /** Уақыт пен бөлме өзгермейді: олар бағаға және төлемдерге әсер етеді. */
    @Transactional
    public Booking update(String number, BookingEditForm form) {
        Booking booking = lockByNumber(number);
        if (!canEdit(booking)) {
            throw new BusinessException("Бұл брондауды енді өзгерту мүмкін емес");
        }
        rules.validateGuests(form.getGuestCount(), booking.getRoom());
        List<Guest> guests = toGuests(form.getGuests(), form.getGuestCount());
        booking.setGuestCount(form.getGuestCount());
        booking.setPurpose(form.getPurpose().trim());
        booking.setNotes(Text.blankToNull(form.getNotes()));
        booking.getGuests().clear();
        guests.forEach(booking::addGuest);
        return booking;
    }

    public boolean canEdit(Booking booking) {
        return booking.getStatus().isActive() && booking.getStartDateTime().isAfter(rules.now());
    }

    private void cancel(Booking booking, boolean fullRefund, String reason) {
        if (!booking.getStatus().canTransitionTo(BookingStatus.CANCELLED)) {
            throw new BusinessException("Бұл брондауды тоқтату мүмкін емес");
        }
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(rules.timestamp());
        booking.setCancellationReason(reason);
        paymentService.refundOnCancellation(booking, fullRefund);
        eventPublisher.publish(BookingEvent.of(BookingEventType.BOOKING_CANCELLED, booking, rules.timestamp()));
        log.info("Booking {} cancelled ({}), fullRefund={}", booking.getBookingNumber(), reason, fullRefund);
    }

    private BookingDetails toDetails(Booking booking) {
        List<Guest> guests = guestRepository.findByBookingIdOrderByIdAsc(booking.getId());
        List<Payment> payments = paymentService.history(booking.getId());
        boolean active = booking.getStatus().isActive();
        boolean upcoming = booking.getStartDateTime().isAfter(rules.now());
        boolean free = rules.isFreeCancellation(booking);
        return new BookingDetails(
                booking,
                guests,
                payments,
                active && !booking.isDepositCovered(),
                active && booking.isDepositCovered() && booking.getRemainingAmount().signum() > 0,
                active && upcoming,
                canEdit(booking),
                paymentService.refundAmount(booking, free),
                free);
    }

    private List<Guest> toGuests(List<GuestForm> forms, Integer guestCount) {
        List<Guest> guests = new ArrayList<>();
        if (forms == null) {
            return guests;
        }
        for (int i = 0; i < forms.size(); i++) {
            GuestForm f = forms.get(i);
            if (f == null || f.isBlank()) {
                continue;
            }
            String first = Text.blankToNull(f.getFirstName());
            String last = Text.blankToNull(f.getLastName());
            if (first == null || last == null) {
                throw new BusinessException("Қонақтың аты мен тегін толық енгізіңіз", "guests[" + i + "].firstName");
            }
            String email = Text.blankToNull(f.getEmail());
            guests.add(new Guest(first, last, email == null ? null : email.toLowerCase(Locale.ROOT),
                    Text.blankToNull(f.getCompany())));
        }
        if (guestCount != null && guests.size() > guestCount) {
            throw new BusinessException("Тізімдегі қонақтар саны жалпы қонақтар санынан (" + guestCount
                    + ") аспауы керек", "guestCount");
        }
        return guests;
    }

    private Booking lockByNumber(String number) {
        Booking booking = getByNumber(number);
        return bookingRepository.findByIdForUpdate(booking.getId())
                .orElseThrow(() -> new NotFoundException("Брондау табылмады"));
    }

    private String uniqueNumber() {
        String number;
        do {
            number = numberGenerator.next();
        } while (bookingRepository.existsByBookingNumber(number));
        return number;
    }

    public static String normalizeNumber(String number) {
        return number == null ? null : number.trim().toUpperCase(Locale.ROOT);
    }
}
