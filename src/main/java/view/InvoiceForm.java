package view;

import model.Invoice;
import model.Reservation;
import service.BookingService;
import service.InvoiceService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.print.PrinterException;
import java.sql.SQLException;
import java.util.List;

/**
 * Detailed Invoice Generation & Final Payment Settlement Screen.
 * Displays all required room, guest, stay, extra guest, discount, tax,
 * refundable booking advance, and final payable amount details.
 */
public class InvoiceForm extends JPanel {

    private final BookingService bookingService;
    private final InvoiceService invoiceService;

    private JComboBox<Reservation> cmbReservation;
    private JTextArea txtInvoiceDisplay;
    private Invoice currentInvoice;

    public InvoiceForm(BookingService bookingService, InvoiceService invoiceService) {
        this.bookingService = bookingService;
        this.invoiceService = invoiceService;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        topBar.setBorder(BorderFactory.createTitledBorder("Select Reservation for Invoice"));

        cmbReservation = new JComboBox<>();
        JButton btnGenerate = new JButton("Generate & Save Invoice");
        JButton btnSettle = new JButton("Record Final Payment & Check-out");
        JButton btnPrint = new JButton("Print Invoice");

        btnGenerate.addActionListener(e -> handleGenerateSelected());
        btnSettle.addActionListener(e -> handleSettleInvoice());
        btnPrint.addActionListener(e -> handlePrintInvoice());

        topBar.add(new JLabel("Reservation:"));
        topBar.add(cmbReservation);
        topBar.add(btnGenerate);
        topBar.add(btnSettle);
        topBar.add(btnPrint);

        add(topBar, BorderLayout.NORTH);

        txtInvoiceDisplay = new JTextArea();
        txtInvoiceDisplay.setEditable(false);
        txtInvoiceDisplay.setFont(new Font("Monospaced", Font.PLAIN, 14));
        txtInvoiceDisplay.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        add(new JScrollPane(txtInvoiceDisplay), BorderLayout.CENTER);
    }

    public void displayInvoiceForReservation(Reservation reservation) {
        if (reservation == null) {
            return;
        }
        refreshReservations();
        for (int i = 0; i < cmbReservation.getItemCount(); i++) {
            if (cmbReservation.getItemAt(i).getReservationId().equals(reservation.getReservationId())) {
                cmbReservation.setSelectedIndex(i);
                break;
            }
        }
        try {
            currentInvoice = invoiceService.generateAndSaveInvoice(reservation);
            txtInvoiceDisplay.setText(invoiceService.formatInvoiceText(currentInvoice));
        } catch (SQLException ex) {
            currentInvoice = invoiceService.calculateInvoice(reservation);
            txtInvoiceDisplay.setText(invoiceService.formatInvoiceText(currentInvoice));
        }
    }

    private void handleGenerateSelected() {
        Reservation selected = (Reservation) cmbReservation.getSelectedItem();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "No reservation selected.");
            return;
        }
        try {
            currentInvoice = invoiceService.generateAndSaveInvoice(selected);
            txtInvoiceDisplay.setText(invoiceService.formatInvoiceText(currentInvoice));
            JOptionPane.showMessageDialog(this, "Invoice " + currentInvoice.getInvoiceId() + " generated and saved.");
        } catch (IllegalArgumentException | SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Invoice Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleSettleInvoice() {
        if (currentInvoice == null) {
            JOptionPane.showMessageDialog(this, "Generate an invoice first before recording final payment.");
            return;
        }
        try {
            invoiceService.settleInvoice(currentInvoice);
            JOptionPane.showMessageDialog(this,
                    "Final payment recorded! Reservation " + currentInvoice.getReservation().getReservationId()
                            + " marked COMPLETED and room returned to Available.");
            refreshReservations();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Settlement Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handlePrintInvoice() {
        if (txtInvoiceDisplay.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nothing to print. Generate an invoice first.");
            return;
        }
        try {
            txtInvoiceDisplay.print();
        } catch (PrinterException ex) {
            JOptionPane.showMessageDialog(this, "Print failed: " + ex.getMessage(), "Print Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void refreshReservations() {
        try {
            List<Reservation> list = bookingService.loadAllReservations();
            cmbReservation.removeAllItems();
            for (Reservation r : list) {
                cmbReservation.addItem(r);
            }
        } catch (SQLException ignored) {
            // Handled on startup
        }
    }
}
