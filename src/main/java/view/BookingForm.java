package view;

import model.Customer;
import model.Reservation;
import model.Room;
import model.RoomType;
import service.BookingService;
import service.RoomService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Unified Booking & Reservation Management Screen:
 * 1. Search available rooms by Room Type and Date range.
 * 2. Create reservations with LKR 10,000 refundable advance and 2-day advance validation.
 * 3. Search reservations by Booking ID (Binary Search), Guest Name (Linear Search),
 *    or Check-in Date (Merge Sort / Filter).
 * 4. Modify or cancel eligible reservations (> 24 hours before check-in) and launch Invoice view.
 */
public class BookingForm extends JPanel {

    private final BookingService bookingService;
    private final RoomService roomService;
    private final Consumer<Reservation> onOpenInvoice;
    private final DecimalFormat moneyFmt = new DecimalFormat("#,##0.00");

    // Top Panel: Search Available Rooms & Make Reservation
    private JComboBox<Object> cmbSearchRoomType;
    private JTextField txtCheckIn;
    private JTextField txtCheckOut;
    private JSpinner spnGuests;
    private JComboBox<Customer> cmbCustomer;
    private JTable availableRoomsTable;
    private DefaultTableModel availableRoomsModel;
    private List<Room> currentAvailableRooms = new ArrayList<>();

    // Bottom Panel: Manage & Search Reservations
    private JTextField txtSearchBookingId;
    private JTextField txtSearchGuestName;
    private JTextField txtFilterCheckIn;
    private JTable reservationsTable;
    private DefaultTableModel reservationsModel;
    private List<Reservation> displayedReservations = new ArrayList<>();

    public BookingForm(BookingService bookingService, RoomService roomService, Consumer<Reservation> onOpenInvoice) {
        this.bookingService = bookingService;
        this.roomService = roomService;
        this.onOpenInvoice = onOpenInvoice;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, createBookingPanel(), createManagePanel());
        splitPane.setResizeWeight(0.46);
        add(splitPane, BorderLayout.CENTER);
    }

    private JPanel createBookingPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createTitledBorder("1. Search Available Rooms & Make Reservation (Advance: LKR 10,000)"));

        JPanel controls = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        cmbSearchRoomType = new JComboBox<>();
        cmbSearchRoomType.addItem("All Types");
        for (RoomType type : RoomType.values()) {
            cmbSearchRoomType.addItem(type);
        }

        LocalDate defaultCheckIn = LocalDate.now().plusDays(3);
        txtCheckIn = new JTextField(defaultCheckIn.toString(), 10);
        txtCheckOut = new JTextField(defaultCheckIn.plusDays(2).toString(), 10);
        spnGuests = new JSpinner(new SpinnerNumberModel(2, 1, 10, 1));
        cmbCustomer = new JComboBox<>();

        JButton btnSearchRooms = new JButton("Search Available Rooms");
        JButton btnBook = new JButton("Confirm Reservation (LKR 10,000 Advance)");

        btnSearchRooms.addActionListener(e -> handleSearchAvailableRooms());
        btnBook.addActionListener(e -> handleCreateReservation());

        gbc.gridx = 0; gbc.gridy = 0; controls.add(new JLabel("Room Type:"), gbc);
        gbc.gridx = 1; controls.add(cmbSearchRoomType, gbc);
        gbc.gridx = 2; controls.add(new JLabel("Check-in (YYYY-MM-DD):"), gbc);
        gbc.gridx = 3; controls.add(txtCheckIn, gbc);
        gbc.gridx = 4; controls.add(new JLabel("Check-out (YYYY-MM-DD):"), gbc);
        gbc.gridx = 5; controls.add(txtCheckOut, gbc);
        gbc.gridx = 6; controls.add(btnSearchRooms, gbc);

        gbc.gridx = 0; gbc.gridy = 1; controls.add(new JLabel("Registered Guest:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2; controls.add(cmbCustomer, gbc);
        gbc.gridwidth = 1;
        gbc.gridx = 3; controls.add(new JLabel("No. of Guests:"), gbc);
        gbc.gridx = 4; controls.add(spnGuests, gbc);
        gbc.gridx = 5; gbc.gridwidth = 2; controls.add(btnBook, gbc);

        panel.add(controls, BorderLayout.NORTH);

        String[] cols = {"Room ID", "Room Type", "Bed Type", "Daily Rate (LKR)", "Breakfast", "Base/Max Cap", "Extra Guest Fee", "Tax"};
        availableRoomsModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        availableRoomsTable = new JTable(availableRoomsModel);
        availableRoomsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        panel.add(new JScrollPane(availableRoomsTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createManagePanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createTitledBorder("2. Search, View, Modify & Cancel Reservations"));

        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        txtSearchBookingId = new JTextField(8);
        JButton btnSearchById = new JButton("Search ID (Binary Search)");

        txtSearchGuestName = new JTextField(12);
        JButton btnSearchByName = new JButton("Search Guest (Linear)");

        txtFilterCheckIn = new JTextField(9);
        JButton btnFilterDate = new JButton("Filter Check-in Date");
        JButton btnSortByDate = new JButton("Sort by Check-in (Merge Sort)");
        JButton btnShowAll = new JButton("Show All");

        btnSearchById.addActionListener(e -> handleBinarySearchById());
        btnSearchByName.addActionListener(e -> handleLinearSearchByGuestName());
        btnFilterDate.addActionListener(e -> handleFilterByCheckInDate());
        btnSortByDate.addActionListener(e -> handleSortByCheckInDate());
        btnShowAll.addActionListener(e -> loadAllReservationsIntoTable());

        searchBar.add(new JLabel("Booking ID:"));
        searchBar.add(txtSearchBookingId);
        searchBar.add(btnSearchById);
        searchBar.add(new JLabel("Guest Name:"));
        searchBar.add(txtSearchGuestName);
        searchBar.add(btnSearchByName);
        searchBar.add(new JLabel("Check-in:"));
        searchBar.add(txtFilterCheckIn);
        searchBar.add(btnFilterDate);
        searchBar.add(btnSortByDate);
        searchBar.add(btnShowAll);

        panel.add(searchBar, BorderLayout.NORTH);

        String[] cols = {"Booking ID", "Guest Name", "NIC/Passport", "Room ID", "Room Type", "Check-in", "Check-out", "Nights", "Guests", "Status"};
        reservationsModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        reservationsTable = new JTable(reservationsModel);
        reservationsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        panel.add(new JScrollPane(reservationsTable), BorderLayout.CENTER);

        JPanel actionBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        JButton btnModify = new JButton("Modify Selected Reservation");
        JButton btnCancel = new JButton("Cancel Selected Reservation");
        JButton btnInvoice = new JButton("Generate / View Detailed Invoice");

        btnModify.addActionListener(e -> handleModifySelected());
        btnCancel.addActionListener(e -> handleCancelSelected());
        btnInvoice.addActionListener(e -> handleOpenInvoice());

        actionBar.add(btnModify);
        actionBar.add(btnCancel);
        actionBar.add(btnInvoice);
        panel.add(actionBar, BorderLayout.SOUTH);

        return panel;
    }

    private void handleSearchAvailableRooms() {
        try {
            LocalDate checkIn = LocalDate.parse(txtCheckIn.getText().trim());
            LocalDate checkOut = LocalDate.parse(txtCheckOut.getText().trim());
            Object sel = cmbSearchRoomType.getSelectedItem();
            RoomType roomType = (sel instanceof RoomType) ? (RoomType) sel : null;

            currentAvailableRooms = bookingService.searchAvailableRooms(roomType, checkIn, checkOut);
            availableRoomsModel.setRowCount(0);
            for (Room r : currentAvailableRooms) {
                availableRoomsModel.addRow(new Object[]{
                        r.getRoomId(),
                        r.getRoomType().getDisplayName(),
                        r.getBedType().getDisplayName(),
                        moneyFmt.format(r.getDailyRate()),
                        r.hasComplimentaryBreakfast() ? "Yes" : "No",
                        r.getBaseCapacity() + " / " + r.getMaxCapacity(),
                        moneyFmt.format(r.getExtraGuestChargeRate()),
                        String.format("%.0f%%", r.getTaxRate() * 100)
                });
            }
            if (currentAvailableRooms.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No available rooms match your criteria.");
            }
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "Use YYYY-MM-DD date format (e.g., 2026-12-01).", "Invalid Date", JOptionPane.ERROR_MESSAGE);
        } catch (IllegalArgumentException | SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Search Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleCreateReservation() {
        int selectedRow = availableRoomsTable.getSelectedRow();
        if (selectedRow < 0 || selectedRow >= currentAvailableRooms.size()) {
            JOptionPane.showMessageDialog(this, "Please search and select an available room first.");
            return;
        }
        Customer customer = (Customer) cmbCustomer.getSelectedItem();
        if (customer == null) {
            JOptionPane.showMessageDialog(this, "Please register and select a customer first.");
            return;
        }
        try {
            Room room = currentAvailableRooms.get(selectedRow);
            LocalDate checkIn = LocalDate.parse(txtCheckIn.getText().trim());
            LocalDate checkOut = LocalDate.parse(txtCheckOut.getText().trim());
            int guests = (Integer) spnGuests.getValue();

            Reservation created = bookingService.createReservation(
                    customer, room, guests, LocalDate.now(), checkIn, checkOut
            );
            JOptionPane.showMessageDialog(this,
                    "Reservation Confirmed!\nBooking ID: " + created.getReservationId()
                            + "\nRefundable Advance Charged: LKR 10,000.00");
            handleSearchAvailableRooms();
            loadAllReservationsIntoTable();
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "Invalid date format. Use YYYY-MM-DD.", "Date Error", JOptionPane.ERROR_MESSAGE);
        } catch (IllegalArgumentException | IllegalStateException | SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Booking Rule Violation", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleBinarySearchById() {
        String id = txtSearchBookingId.getText().trim();
        if (id.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter a Booking ID (e.g., BK001).");
            return;
        }
        try {
            List<Reservation> all = bookingService.loadAllReservations();
            Reservation found = bookingService.binarySearchByBookingId(all, id);
            List<Reservation> result = new ArrayList<>();
            if (found != null) {
                result.add(found);
            } else {
                JOptionPane.showMessageDialog(this, "No reservation found for Booking ID: " + id);
            }
            populateReservationsTable(result);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Search Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleLinearSearchByGuestName() {
        String query = txtSearchGuestName.getText().trim();
        if (query.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter a guest name to search.");
            return;
        }
        try {
            List<Reservation> all = bookingService.loadAllReservations();
            List<Reservation> matches = bookingService.linearSearchByGuestName(all, query);
            populateReservationsTable(matches);
            if (matches.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No reservations found for guest name: " + query);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Search Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleFilterByCheckInDate() {
        try {
            LocalDate targetDate = LocalDate.parse(txtFilterCheckIn.getText().trim());
            List<Reservation> all = bookingService.loadAllReservations();
            List<Reservation> matches = bookingService.filterReservationsByCheckInDate(all, targetDate);
            populateReservationsTable(matches);
            if (matches.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No reservations found for check-in date: " + targetDate);
            }
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "Enter check-in date as YYYY-MM-DD.", "Invalid Date", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Filter Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleSortByCheckInDate() {
        try {
            List<Reservation> all = bookingService.loadAllReservations();
            List<Reservation> sorted = bookingService.mergeSortByCheckInDate(all);
            populateReservationsTable(sorted);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Sort Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleModifySelected() {
        int row = reservationsTable.getSelectedRow();
        if (row < 0 || row >= displayedReservations.size()) {
            JOptionPane.showMessageDialog(this, "Select a reservation to modify.");
            return;
        }
        Reservation res = displayedReservations.get(row);
        String newInStr = JOptionPane.showInputDialog(this, "New Check-in Date (YYYY-MM-DD):", res.getCheckInDate().toString());
        if (newInStr == null) return;
        String newOutStr = JOptionPane.showInputDialog(this, "New Check-out Date (YYYY-MM-DD):", res.getCheckOutDate().toString());
        if (newOutStr == null) return;
        String newGuestsStr = JOptionPane.showInputDialog(this, "New Number of Guests:", String.valueOf(res.getNumberOfGuests()));
        if (newGuestsStr == null) return;

        try {
            bookingService.modifyReservation(
                    res,
                    LocalDate.parse(newInStr.trim()),
                    LocalDate.parse(newOutStr.trim()),
                    Integer.parseInt(newGuestsStr.trim()),
                    LocalDateTime.now()
            );
            JOptionPane.showMessageDialog(this, "Reservation " + res.getReservationId() + " modified successfully.");
            loadAllReservationsIntoTable();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Modification Blocked", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleCancelSelected() {
        int row = reservationsTable.getSelectedRow();
        if (row < 0 || row >= displayedReservations.size()) {
            JOptionPane.showMessageDialog(this, "Select a reservation to cancel.");
            return;
        }
        Reservation res = displayedReservations.get(row);
        int confirm = JOptionPane.showConfirmDialog(this,
                "Cancel reservation " + res.getReservationId() + " and refund LKR 10,000 advance?",
                "Confirm Cancellation", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            bookingService.cancelReservation(res, LocalDateTime.now());
            JOptionPane.showMessageDialog(this,
                    "Reservation " + res.getReservationId() + " cancelled.\nRoom "
                            + res.getRoom().getRoomId() + " is now Available and LKR 10,000 advance refunded.");
            loadAllReservationsIntoTable();
        } catch (IllegalStateException | SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Cancellation Blocked", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleOpenInvoice() {
        int row = reservationsTable.getSelectedRow();
        if (row < 0 || row >= displayedReservations.size()) {
            JOptionPane.showMessageDialog(this, "Select a reservation to view its invoice.");
            return;
        }
        if (onOpenInvoice != null) {
            onOpenInvoice.accept(displayedReservations.get(row));
        }
    }

    private void populateReservationsTable(List<Reservation> list) {
        displayedReservations = new ArrayList<>(list);
        reservationsModel.setRowCount(0);
        for (Reservation r : displayedReservations) {
            reservationsModel.addRow(new Object[]{
                    r.getReservationId(),
                    r.getCustomer().getFullName(),
                    r.getCustomer().getNicPassport(),
                    r.getRoom().getRoomId(),
                    r.getRoom().getRoomType().getDisplayName(),
                    r.getCheckInDate(),
                    r.getCheckOutDate(),
                    r.getNumberOfNights(),
                    r.getNumberOfGuests(),
                    r.getStatus().getDisplayName()
            });
        }
    }

    public void loadAllReservationsIntoTable() {
        try {
            List<Reservation> all = bookingService.loadAllReservations();
            populateReservationsTable(bookingService.mergeSortByCheckInDate(all));
        } catch (SQLException ignored) {
            // Handled on startup
        }
    }

    public void refreshData() {
        try {
            List<Customer> customers = bookingService.loadAllCustomers();
            cmbCustomer.removeAllItems();
            for (Customer c : customers) {
                cmbCustomer.addItem(c);
            }
            roomService.loadAllRooms();
            loadAllReservationsIntoTable();
        } catch (SQLException ignored) {
            // Handled on startup
        }
    }
}
