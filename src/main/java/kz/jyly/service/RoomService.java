package kz.jyly.service;

import kz.jyly.dto.RoomForm;
import kz.jyly.dto.RoomSearchCriteria;
import kz.jyly.dto.RoomSearchResult;
import kz.jyly.dto.TimeRange;
import kz.jyly.entity.Booking;
import kz.jyly.entity.Room;
import kz.jyly.enums.BookingStatus;
import kz.jyly.exception.BusinessException;
import kz.jyly.exception.NotFoundException;
import kz.jyly.repository.BookingRepository;
import kz.jyly.repository.RoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class RoomService {

    private static final Logger log = LoggerFactory.getLogger(RoomService.class);

    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final BookingRules rules;

    public RoomService(RoomRepository roomRepository, BookingRepository bookingRepository, BookingRules rules) {
        this.roomRepository = roomRepository;
        this.bookingRepository = bookingRepository;
        this.rules = rules;
    }

    @Transactional(readOnly = true)
    public List<Room> listActive() {
        return roomRepository.findByActiveTrue(Sort.by("hourlyRate"));
    }

    @Transactional(readOnly = true)
    public List<Room> listAll() {
        return roomRepository.findAll(Sort.by("id"));
    }

    @Transactional(readOnly = true)
    public Room getActiveBySlug(String slug) {
        return roomRepository.findBySlug(slug)
                .filter(Room::isActive)
                .orElseThrow(() -> new NotFoundException("Бөлме табылмады немесе қазір қолжетімсіз"));
    }

    @Transactional(readOnly = true)
    public Room get(Long id) {
        return roomRepository.findById(id).orElseThrow(() -> new NotFoundException("Бөлме табылмады"));
    }

    /** Каталог іздеуі: барлық сүзгілер дерекқор сұрауы арқылы орындалады. */
    @Transactional(readOnly = true)
    public RoomSearchResult search(RoomSearchCriteria c) {
        String error = null;
        if (c.getGuests() != null && c.getGuests() < 1) {
            error = "Қонақтар саны нөлден көп болуы керек";
            c.setGuests(null);
        }
        if (c.getMaxPrice() != null && c.getMaxPrice().signum() <= 0) {
            error = "Баға нөлден көп болуы керек";
            c.setMaxPrice(null);
        }
        List<Room> rooms = roomRepository.search(c.getGuests(), c.getMaxPrice(), c.getType(), sortFor(c.getSort()));

        if (!c.hasAnyInterval()) {
            return new RoomSearchResult(rooms, 0, false, error);
        }
        if (!c.hasFullInterval()) {
            return new RoomSearchResult(rooms, 0, false,
                    "Бос бөлмелерді іздеу үшін күнді, басталу және аяқталу уақытын толық таңдаңыз");
        }
        LocalDateTime start = c.getDate().atTime(c.getStart());
        LocalDateTime end = c.getDate().atTime(c.getEnd());
        try {
            rules.validateInterval(start, end);
        } catch (BusinessException e) {
            return new RoomSearchResult(rooms, 0, false, e.getMessage());
        }
        Set<Long> busy = new HashSet<>(bookingRepository.findBusyRoomIds(start, end, BookingStatus.BLOCKING));
        List<Room> free = rooms.stream().filter(r -> !busy.contains(r.getId())).toList();
        return new RoomSearchResult(free, rooms.size() - free.size(), true, error);
    }

    /** Дәл қазір бос емес бөлмелердің идентификаторлары. */
    @Transactional(readOnly = true)
    public Set<Long> busyNowIds() {
        LocalDateTime now = rules.now();
        return new HashSet<>(bookingRepository.findBusyRoomIds(now, now.plusMinutes(1), BookingStatus.BLOCKING));
    }

    /** Бөлменің берілген күндегі бос емес уақыттары. Клиенттердің жеке деректері берілмейді. */
    @Transactional(readOnly = true)
    public List<TimeRange> busySlots(Long roomId, LocalDate date) {
        List<Booking> bookings = bookingRepository.findBlockingInRange(roomId, date.atStartOfDay(),
                date.plusDays(1).atStartOfDay(), BookingStatus.BLOCKING);
        return bookings.stream().map(b -> new TimeRange(b.getStartDateTime(), b.getEndDateTime())).toList();
    }

    @Transactional
    public Room create(RoomForm form) {
        Room room = new Room();
        apply(room, form);
        room.setSlug(uniqueSlug(form.getName()));
        Room saved = roomRepository.save(room);
        log.info("Room '{}' created by admin", saved.getName());
        return saved;
    }

    /** Бағаның өзгеруі бұрынғы брондауларға әсер етпейді: олар өз бағасын сақтайды. */
    @Transactional
    public Room update(Long id, RoomForm form) {
        Room room = get(id);
        apply(room, form);
        log.info("Room '{}' updated by admin", room.getName());
        return room;
    }

    @Transactional
    public Room toggleActive(Long id) {
        Room room = get(id);
        room.setActive(!room.isActive());
        log.info("Room '{}' active={}", room.getName(), room.isActive());
        return room;
    }

    @Transactional(readOnly = true)
    public RoomForm toForm(Long id) {
        Room room = get(id);
        RoomForm form = new RoomForm();
        form.setName(room.getName());
        form.setType(room.getType());
        form.setShortDescription(room.getShortDescription());
        form.setDescription(room.getDescription());
        form.setCapacity(room.getCapacity());
        form.setArea(room.getArea());
        form.setHourlyRate(room.getHourlyRate().stripTrailingZeros());
        form.setEquipmentText(String.join("\n", room.getEquipment()));
        form.setImageUrl(room.getImageUrl());
        form.setActive(room.isActive());
        return form;
    }

    public static List<String> parseEquipment(String text) {
        if (text == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.stream(text.split("\\r?\\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.length() > 80 ? s.substring(0, 80) : s)
                .distinct()
                .limit(20)
                .toList());
    }

    private void apply(Room room, RoomForm form) {
        room.setName(form.getName().trim());
        room.setType(form.getType());
        room.setShortDescription(form.getShortDescription().trim());
        room.setDescription(form.getDescription().trim());
        room.setCapacity(form.getCapacity());
        room.setArea(form.getArea());
        room.setHourlyRate(form.getHourlyRate());
        room.getEquipment().clear();
        room.getEquipment().addAll(parseEquipment(form.getEquipmentText()));
        String image = Text.blankToNull(form.getImageUrl());
        room.setImageUrl(image == null ? "/images/room-placeholder.svg" : image);
        room.setActive(form.isActive());
    }

    private String uniqueSlug(String name) {
        String base = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        if (base.isEmpty()) {
            base = "room";
        }
        String slug = base;
        int i = 2;
        while (roomRepository.existsBySlug(slug)) {
            slug = base + "-" + i++;
        }
        return slug;
    }

    private static Sort sortFor(String key) {
        if (key == null) {
            return Sort.by("hourlyRate");
        }
        return switch (key) {
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "hourlyRate");
            case "capacity_asc" -> Sort.by("capacity").and(Sort.by("hourlyRate"));
            case "capacity_desc" -> Sort.by(Sort.Direction.DESC, "capacity").and(Sort.by("hourlyRate"));
            default -> Sort.by("hourlyRate");
        };
    }
}
