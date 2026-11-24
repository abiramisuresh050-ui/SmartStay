package view;

import model.Customer;
import service.BookingService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.util.List;

/**
 * Guest Registration screen capturing mandatory customer attributes:
 * NIC/Passport, Full Name, Contact Number, and Email Address.
 */
public class GuestForm extends JPanel {

    private final BookingService bookingService;

    private int selectedCustomerId = -1;
    private JTextField txtNicPassport;
    private JTextField txtFullName;
    private JTextField txtContact;
    private JTextField txtEmail;

    private JTable customerTable;
    private DefaultTableModel tableModel;

    public GuestForm(BookingService bookingService) {
        this.bookingService = bookingService;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Guest Registration Form"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        txtNicPassport = new JTextField(18);
        txtFullName = new JTextField(22);
        txtContact = new JTextField(18);
        txtEmail = new JTextField(22);

        gbc.gridx = 0; gbc.gridy = 0; formPanel.add(new JLabel("NIC / Passport:*"), gbc);
        gbc.gridx = 1; formPanel.add(txtNicPassport, gbc);
        gbc.gridx = 2; formPanel.add(new JLabel("Full Name:*"), gbc);
        gbc.gridx = 3; formPanel.add(txtFullName, gbc);

        gbc.gridx = 0; gbc.gridy = 1; formPanel.add(new JLabel("Contact Number:*"), gbc);
        gbc.gridx = 1; formPanel.add(txtContact, gbc);
        gbc.gridx = 2; formPanel.add(new JLabel("Email Address:*"), gbc);
        gbc.gridx = 3; formPanel.add(txtEmail, gbc);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton btnRegister = new JButton("Register Guest");
        JButton btnUpdate = new JButton("Update Guest");
        JButton btnClear = new JButton("Clear");
        JButton btnRefresh = new JButton("Refresh");

        btnRegister.addActionListener(e -> handleRegister());
        btnUpdate.addActionListener(e -> handleUpdate());
        btnClear.addActionListener(e -> clearFields());
        btnRefresh.addActionListener(e -> refreshTable());

        btnPanel.add(btnRegister);
        btnPanel.add(btnUpdate);
        btnPanel.add(btnClear);
        btnPanel.add(btnRefresh);

        JPanel northContainer = new JPanel(new BorderLayout(4, 4));
        northContainer.add(formPanel, BorderLayout.CENTER);
        northContainer.add(btnPanel, BorderLayout.SOUTH);
        add(northContainer, BorderLayout.NORTH);

        String[] cols = {"Customer ID", "NIC / Passport", "Full Name", "Contact Number", "Email Address"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        customerTable = new JTable(tableModel);
        customerTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        customerTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                populateFromTable();
            }
        });

        add(new JScrollPane(customerTable), BorderLayout.CENTER);
    }

    private void populateFromTable() {
        int row = customerTable.getSelectedRow();
        if (row < 0) {
            return;
        }
        selectedCustomerId = Integer.parseInt(tableModel.getValueAt(row, 0).toString());
        txtNicPassport.setText(tableModel.getValueAt(row, 1).toString());
        txtFullName.setText(tableModel.getValueAt(row, 2).toString());
        txtContact.setText(tableModel.getValueAt(row, 3).toString());
        txtEmail.setText(tableModel.getValueAt(row, 4).toString());
    }

    private void handleRegister() {
        try {
            Customer saved = bookingService.registerCustomer(
                    txtNicPassport.getText(),
                    txtFullName.getText(),
                    txtContact.getText(),
                    txtEmail.getText()
            );
            JOptionPane.showMessageDialog(this,
                    "Guest registered successfully! Customer ID: " + saved.getCustomerId());
            clearFields();
            refreshTable();
        } catch (IllegalArgumentException | SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Registration Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleUpdate() {
        if (selectedCustomerId <= 0) {
            JOptionPane.showMessageDialog(this, "Select a guest from the table first.");
            return;
        }
        try {
            Customer updated = new Customer(
                    selectedCustomerId,
                    txtNicPassport.getText(),
                    txtFullName.getText(),
                    txtContact.getText(),
                    txtEmail.getText()
            );
            bookingService.updateCustomer(updated);
            JOptionPane.showMessageDialog(this, "Guest details updated successfully.");
            clearFields();
            refreshTable();
        } catch (IllegalArgumentException | SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Update Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearFields() {
        selectedCustomerId = -1;
        txtNicPassport.setText("");
        txtFullName.setText("");
        txtContact.setText("");
        txtEmail.setText("");
        customerTable.clearSelection();
    }

    public void refreshTable() {
        try {
            List<Customer> customers = bookingService.loadAllCustomers();
            tableModel.setRowCount(0);
            for (Customer c : customers) {
                tableModel.addRow(new Object[]{
                        c.getCustomerId(),
                        c.getNicPassport(),
                        c.getFullName(),
                        c.getContactNumber(),
                        c.getEmail()
                });
            }
        } catch (SQLException ignored) {
            // Connection warning already shown on startup
        }
    }
}
