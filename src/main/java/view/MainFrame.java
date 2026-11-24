package view;

import database.DatabaseConnection;
import model.Reservation;
import service.BookingService;
import service.InvoiceService;
import service.RoomService;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

/**
 * Main application window for the SmartStay Hotel Booking System.
 * Coordinates navigation between Administrator Room Management, Guest Registration,
 * Reservations & Search, and Detailed Invoicing.
 */
public class MainFrame extends JFrame {

    private final RoomService roomService;
    private final BookingService bookingService;
    private final InvoiceService invoiceService;

    private JTabbedPane tabbedPane;
    private RoomForm roomForm;
    private GuestForm guestForm;
    private BookingForm bookingForm;
    private InvoiceForm invoiceForm;
    private JLabel dbStatusLabel;

    public MainFrame() {
        this.roomService = new RoomService();
        this.bookingService = new BookingService();
        this.invoiceService = new InvoiceService();
        initComponents();
        checkDatabaseConnection();
    }

    private void initComponents() {
        setTitle("SmartStay Hotel Booking System — CI6115");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1150, 760);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));

        // Header Banner
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(28, 53, 88));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));

        JLabel titleLabel = new JLabel("SmartStay Hotel Booking System (Sri Lanka)");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);

        dbStatusLabel = new JLabel("Checking MySQL...");
        dbStatusLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        dbStatusLabel.setForeground(new Color(220, 235, 255));

        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(dbStatusLabel, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // Tabbed Workspace
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("SansSerif", Font.BOLD, 13));

        roomForm = new RoomForm(roomService);
        guestForm = new GuestForm(bookingService);
        invoiceForm = new InvoiceForm(bookingService, invoiceService);
        bookingForm = new BookingForm(bookingService, roomService, this::openInvoiceForReservation);

        tabbedPane.addTab("Room Management (Admin)", roomForm);
        tabbedPane.addTab("Guest Registration", guestForm);
        tabbedPane.addTab("Room Search & Bookings", bookingForm);
        tabbedPane.addTab("Detailed Invoices", invoiceForm);

        tabbedPane.addChangeListener(e -> {
            int idx = tabbedPane.getSelectedIndex();
            if (idx == 0) {
                roomForm.refreshTable();
            } else if (idx == 1) {
                guestForm.refreshTable();
            } else if (idx == 2) {
                bookingForm.refreshData();
            } else if (idx == 3) {
                invoiceForm.refreshReservations();
            }
        });

        add(tabbedPane, BorderLayout.CENTER);
    }

    private void openInvoiceForReservation(Reservation reservation) {
        tabbedPane.setSelectedComponent(invoiceForm);
        invoiceForm.displayInvoiceForReservation(reservation);
    }

    private void checkDatabaseConnection() {
        boolean connected = DatabaseConnection.getInstance().testConnection();
        if (connected) {
            dbStatusLabel.setText("MySQL Connected (smartstay_db)");
            dbStatusLabel.setForeground(new Color(144, 238, 144));
            roomForm.refreshTable();
            guestForm.refreshTable();
            bookingForm.refreshData();
            invoiceForm.refreshReservations();
        } else {
            dbStatusLabel.setText("MySQL Disconnected — Run sql/smartstay_schema.sql");
            dbStatusLabel.setForeground(new Color(255, 160, 122));
            JOptionPane.showMessageDialog(
                    this,
                    "Could not connect to MySQL database 'smartstay_db'.\n"
                            + "Please verify MySQL Server is running and execute sql/smartstay_schema.sql.\n"
                            + "Optional env vars: SMARTSTAY_DB_URL, SMARTSTAY_DB_USER, SMARTSTAY_DB_PASSWORD.",
                    "Database Connection Warning",
                    JOptionPane.WARNING_MESSAGE
            );
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Fall back to default Swing Look & Feel
        }
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}
