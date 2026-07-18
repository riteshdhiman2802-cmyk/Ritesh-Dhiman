import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

// -------------------- Enums --------------------
enum RoomCategory {
    STANDARD, DELUXE, SUITE
}

enum ReservationStatus {
    ACTIVE, CANCELLED
}

// -------------------- Model Classes --------------------
class Room implements Serializable {
    private static final long serialVersionUID = 1L;
    private final int roomNumber;
    private final RoomCategory category;
    private final double pricePerNight;

    public Room(int roomNumber, RoomCategory category, double pricePerNight) {
        this.roomNumber = roomNumber;
        this.category = category;
        this.pricePerNight = pricePerNight;
    }

    public int getRoomNumber() { return roomNumber; }
    public RoomCategory getCategory() { return category; }
    public double getPricePerNight() { return pricePerNight; }

    @Override
    public String toString() {
        return String.format("Room #%d (%s) - ₹%.2f/night", roomNumber, category, pricePerNight);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Room room = (Room) o;
        return roomNumber == room.roomNumber;
    }

    @Override
    public int hashCode() {
        return Objects.hash(roomNumber);
    }
}

class Reservation implements Serializable {
    private static final long serialVersionUID = 1L;
    private static int idCounter = 1;

    private final int reservationId;
    private final String guestName;
    private final Room room;
    private final LocalDate checkIn;
    private final LocalDate checkOut;
    private final double totalPrice;
    private ReservationStatus status;

    // For new reservations (auto‑increment id)
    public Reservation(String guestName, Room room, LocalDate checkIn, LocalDate checkOut) {
        this.reservationId = idCounter++;
        this.guestName = guestName;
        this.room = room;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.totalPrice = room.getPricePerNight() * (checkOut.toEpochDay() - checkIn.toEpochDay());
        this.status = ReservationStatus.ACTIVE;
    }

    // For loading from file (with given id)
    public Reservation(int reservationId, String guestName, Room room, LocalDate checkIn,
                       LocalDate checkOut, double totalPrice, ReservationStatus status) {
        this.reservationId = reservationId;
        this.guestName = guestName;
        this.room = room;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.totalPrice = totalPrice;
        this.status = status;
        if (reservationId >= idCounter) idCounter = reservationId + 1;
    }

    public int getReservationId() { return reservationId; }
    public String getGuestName() { return guestName; }
    public Room getRoom() { return room; }
    public LocalDate getCheckIn() { return checkIn; }
    public LocalDate getCheckOut() { return checkOut; }
    public double getTotalPrice() { return totalPrice; }
    public ReservationStatus getStatus() { return status; }

    public void cancel() {
        this.status = ReservationStatus.CANCELLED;
    }

    public boolean isActive() {
        return status == ReservationStatus.ACTIVE;
    }

    // Check if this reservation overlaps with given date range
    public boolean overlaps(LocalDate start, LocalDate end) {
        return !(end.isBefore(checkIn) || start.isAfter(checkOut) || start.equals(checkOut) || end.equals(checkIn));
    }

    @Override
    public String toString() {
        return String.format("Reservation #%d | Guest: %s | Room: %d | %s to %s | Total: ₹%.2f | Status: %s",
                reservationId, guestName, room.getRoomNumber(),
                checkIn.format(DateTimeFormatter.ISO_LOCAL_DATE),
                checkOut.format(DateTimeFormatter.ISO_LOCAL_DATE),
                totalPrice, status);
    }
}

// -------------------- Service / Manager --------------------
class HotelService {
    private static final String ROOMS_FILE = "rooms.ser";
    private static final String RESERVATIONS_FILE = "reservations.ser";

    private List<Room> rooms;
    private List<Reservation> reservations;

    public HotelService() {
        loadData();
        // If no rooms exist, create some default ones
        if (rooms.isEmpty()) {
            createDefaultRooms();
            saveRooms();
        }
    }

    // -------- Data Persistence --------
    @SuppressWarnings("unchecked")
    private void loadData() {
        rooms = loadFromFile(ROOMS_FILE, ArrayList.class);
        reservations = loadFromFile(RESERVATIONS_FILE, ArrayList.class);
        if (rooms == null) rooms = new ArrayList<>();
        if (reservations == null) reservations = new ArrayList<>();
    }

    private <T> T loadFromFile(String fileName, Class<T> type) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fileName))) {
            return type.cast(ois.readObject());
        } catch (FileNotFoundException e) {
            // File not found, return null – will be created later
            return null;
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error loading " + fileName + ": " + e.getMessage());
            return null;
        }
    }

    private void saveRooms() {
        saveToFile(ROOMS_FILE, rooms);
    }

    private void saveReservations() {
        saveToFile(RESERVATIONS_FILE, reservations);
    }

    private void saveToFile(String fileName, Object data) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fileName))) {
            oos.writeObject(data);
        } catch (IOException e) {
            System.err.println("Error saving " + fileName + ": " + e.getMessage());
        }
    }

    // -------- Default Rooms --------
    private void createDefaultRooms() {
        rooms.add(new Room(101, RoomCategory.STANDARD, 2000));
        rooms.add(new Room(102, RoomCategory.STANDARD, 2000));
        rooms.add(new Room(103, RoomCategory.STANDARD, 2000));
        rooms.add(new Room(201, RoomCategory.DELUXE, 3500));
        rooms.add(new Room(202, RoomCategory.DELUXE, 3500));
        rooms.add(new Room(301, RoomCategory.SUITE, 5000));
        rooms.add(new Room(302, RoomCategory.SUITE, 5000));
    }

    // -------- Public Methods --------
    public List<Room> searchAvailableRooms(RoomCategory category, LocalDate checkIn, LocalDate checkOut) {
        if (checkIn.isAfter(checkOut) || checkIn.equals(checkOut)) {
            throw new IllegalArgumentException("Check‑in must be before check‑out");
        }
        return rooms.stream()
                .filter(room -> room.getCategory() == category)
                .filter(room -> isRoomAvailable(room, checkIn, checkOut))
                .collect(Collectors.toList());
    }

    public boolean isRoomAvailable(Room room, LocalDate checkIn, LocalDate checkOut) {
        for (Reservation res : reservations) {
            if (res.isActive() && res.getRoom().equals(room)) {
                if (res.overlaps(checkIn, checkOut)) {
                    return false;
                }
            }
        }
        return true;
    }

    public Reservation bookRoom(String guestName, Room room, LocalDate checkIn, LocalDate checkOut) {
        if (!isRoomAvailable(room, checkIn, checkOut)) {
            throw new IllegalStateException("Room is not available for the selected dates.");
        }
        Reservation reservation = new Reservation(guestName, room, checkIn, checkOut);
        reservations.add(reservation);
        saveReservations();
        return reservation;
    }

    public boolean cancelReservation(int reservationId) {
        for (Reservation res : reservations) {
            if (res.getReservationId() == reservationId && res.isActive()) {
                res.cancel();
                saveReservations();
                return true;
            }
        }
        return false;
    }

    public Reservation getReservationDetails(int reservationId) {
        for (Reservation res : reservations) {
            if (res.getReservationId() == reservationId) {
                return res;
            }
        }
        return null;
    }

    public List<Reservation> getAllReservations() {
        return new ArrayList<>(reservations);
    }

    public List<Room> getAllRooms() {
        return new ArrayList<>(rooms);
    }

    // -------- Payment Simulation --------
    public boolean processPayment(double amount) {
        // Simulate payment: ask user to confirm
        Scanner scanner = new Scanner(System.in);
        System.out.printf("Payment of ₹%.2f is required. Confirm payment? (y/n): ", amount);
        String input = scanner.nextLine().trim().toLowerCase();
        return input.equals("y") || input.equals("yes");
    }
}

// -------------------- Payment Processor (stand‑alone) --------------------
class PaymentProcessor {
    private final HotelService hotelService;

    public PaymentProcessor(HotelService hotelService) {
        this.hotelService = hotelService;
    }

    public boolean process(Reservation reservation) {
        return hotelService.processPayment(reservation.getTotalPrice());
    }
}

// -------------------- Main Application --------------------
public class HotelReservationSystem {
    private static final Scanner scanner = new Scanner(System.in);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final HotelService hotelService = new HotelService();
    private static final PaymentProcessor paymentProcessor = new PaymentProcessor(hotelService);

    public static void main(String[] args) {
        System.out.println("=== Welcome to Hotel Reservation System ===");
        boolean exit = false;
        while (!exit) {
            printMenu();
            int choice = readInt("Enter your choice: ", 1, 7);
            switch (choice) {
                case 1 -> searchRooms();
                case 2 -> bookRoom();
                case 3 -> cancelReservation();
                case 4 -> viewReservationDetails();
                case 5 -> viewAllReservations();
                case 6 -> viewAllRooms();
                case 7 -> {
                    System.out.println("Thank you for using the system. Goodbye!");
                    exit = true;
                }
                default -> System.out.println("Invalid option.");
            }
        }
        scanner.close();
    }

    private static void printMenu() {
        System.out.println("\n--- Main Menu ---");
        System.out.println("1. Search available rooms");
        System.out.println("2. Book a room");
        System.out.println("3. Cancel a reservation");
        System.out.println("4. View reservation details");
        System.out.println("5. View all reservations");
        System.out.println("6. View all rooms (with categories)");
        System.out.println("7. Exit");
    }

    // -------- Menu Actions --------
    private static void searchRooms() {
        System.out.println("\n--- Search Available Rooms ---");
        RoomCategory category = chooseCategory();
        LocalDate checkIn = readDate("Enter check‑in date (yyyy‑MM‑dd): ");
        LocalDate checkOut = readDate("Enter check‑out date (yyyy‑MM‑dd): ");
        if (checkIn.isAfter(checkOut) || checkIn.equals(checkOut)) {
            System.out.println("Check‑in must be before check‑out.");
            return;
        }
        try {
            List<Room> available = hotelService.searchAvailableRooms(category, checkIn, checkOut);
            if (available.isEmpty()) {
                System.out.println("No available rooms for the selected category and dates.");
            } else {
                System.out.println("Available rooms:");
                available.forEach(System.out::println);
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void bookRoom() {
        System.out.println("\n--- Book a Room ---");
        RoomCategory category = chooseCategory();
        LocalDate checkIn = readDate("Enter check‑in date (yyyy‑MM‑dd): ");
        LocalDate checkOut = readDate("Enter check‑out date (yyyy‑MM‑dd): ");
        if (checkIn.isAfter(checkOut) || checkIn.equals(checkOut)) {
            System.out.println("Check‑in must be before check‑out.");
            return;
        }

        List<Room> available;
        try {
            available = hotelService.searchAvailableRooms(category, checkIn, checkOut);
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
            return;
        }

        if (available.isEmpty()) {
            System.out.println("No rooms available for these dates.");
            return;
        }

        System.out.println("Available rooms:");
        for (int i = 0; i < available.size(); i++) {
            System.out.println((i + 1) + ". " + available.get(i));
        }
        int roomChoice = readInt("Select room number (1-" + available.size() + "): ", 1, available.size());
        Room selectedRoom = available.get(roomChoice - 1);

        System.out.print("Enter guest name: ");
        String guestName = scanner.nextLine().trim();
        if (guestName.isEmpty()) {
            System.out.println("Guest name cannot be empty.");
            return;
        }

        // Simulate payment before booking
        double total = selectedRoom.getPricePerNight() * (checkOut.toEpochDay() - checkIn.toEpochDay());
        if (!paymentProcessor.process(new Reservation(guestName, selectedRoom, checkIn, checkOut))) {
            System.out.println("Payment failed or cancelled. Booking not confirmed.");
            return;
        }

        try {
            Reservation reservation = hotelService.bookRoom(guestName, selectedRoom, checkIn, checkOut);
            System.out.println("Booking successful!");
            System.out.println("Reservation ID: " + reservation.getReservationId());
            System.out.println("Total amount paid: ₹" + reservation.getTotalPrice());
        } catch (IllegalStateException e) {
            System.out.println("Booking failed: " + e.getMessage());
        }
    }

    private static void cancelReservation() {
        System.out.println("\n--- Cancel Reservation ---");
        int id = readInt("Enter reservation ID to cancel: ", 1, Integer.MAX_VALUE);
        boolean cancelled = hotelService.cancelReservation(id);
        if (cancelled) {
            System.out.println("Reservation #" + id + " cancelled successfully.");
        } else {
            System.out.println("Reservation not found or already cancelled.");
        }
    }

    private static void viewReservationDetails() {
        System.out.println("\n--- View Reservation Details ---");
        int id = readInt("Enter reservation ID: ", 1, Integer.MAX_VALUE);
        Reservation res = hotelService.getReservationDetails(id);
        if (res == null) {
            System.out.println("Reservation not found.");
        } else {
            System.out.println(res);
        }
    }

    private static void viewAllReservations() {
        System.out.println("\n--- All Reservations ---");
        List<Reservation> all = hotelService.getAllReservations();
        if (all.isEmpty()) {
            System.out.println("No reservations yet.");
        } else {
            all.forEach(System.out::println);
        }
    }

    private static void viewAllRooms() {
        System.out.println("\n--- All Rooms ---");
        List<Room> allRooms = hotelService.getAllRooms();
        allRooms.forEach(System.out::println);
    }

    // -------- Helper Methods --------
    private static RoomCategory chooseCategory() {
        System.out.println("Select category:");
        System.out.println("1. STANDARD");
        System.out.println("2. DELUXE");
        System.out.println("3. SUITE");
        int choice = readInt("Enter choice (1-3): ", 1, 3);
        return switch (choice) {
            case 1 -> RoomCategory.STANDARD;
            case 2 -> RoomCategory.DELUXE;
            case 3 -> RoomCategory.SUITE;
            default -> RoomCategory.STANDARD; // never reached
        };
    }

    private static LocalDate readDate(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                return LocalDate.parse(input, DATE_FORMAT);
            } catch (DateTimeParseException e) {
                System.out.println("Invalid date format. Please use yyyy-MM-dd.");
            }
        }
    }

    private static int readInt(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            try {
                int value = Integer.parseInt(scanner.nextLine().trim());
                if (value >= min && value <= max) {
                    return value;
                }
                System.out.println("Please enter a number between " + min + " and " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println("Invalid number. Please try again.");
            }
        }
    }
}
