package view;

import model.AvailabilityStatus;
import model.BedType;
import model.Room;
import model.RoomType;
import service.RoomService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
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
import java.text.DecimalFormat;
import java.util.List;

/**
 * Administrator screen for adding, updating, removing, viewing rooms,
 * and managing room availability status.
 */
public class RoomForm extends JPanel {

    private final RoomService roomService;
    private final DecimalFormat moneyFmt = new DecimalFormat("#,##0.00");

    private JTextField txtRoomId;
    private JComboBox<RoomType> cmbRoomType;
    private JComboBox<BedType> cmbBedType;
    private JComboBox<AvailabilityStatus> cmbStatus;
    private JTextField txtDailyRate;
    private JTextField txtPackageInfo;

    private JTable roomTable;
    private DefaultTableModel tableModel;

    public RoomForm(RoomService roomService) {
        this.roomService = roomService;
        initComponents();
        updatePackagePreview();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // Form Panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Room Details (Administrator)"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 8, 5, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        txtRoomId = new JTextField(12);
        cmbRoomType = new JComboBox<>(RoomType.values());
        cmbBedType = new JComboBox<>(BedType.values());
        cmbStatus = new JComboBox<>(AvailabilityStatus.values());

        txtDailyRate = new JTextField(12);
        txtDailyRate.setEditable(false);

        txtPackageInfo = new JTextField(32);
        txtPackageInfo.setEditable(false);

        cmbRoomType.addActionListener(e -> updatePackagePreview());

        gbc.gridx = 0; gbc.gridy = 0; formPanel.add(new JLabel("Room ID:"), gbc);
        gbc.gridx = 1; formPanel.add(txtRoomId, gbc);
        gbc.gridx = 2; formPanel.add(new JLabel("Room Type:"), gbc);
        gbc.gridx = 3; formPanel.add(cmbRoomType, gbc);
        gbc.gridx = 4; formPanel.add(new JLabel("Bed Type:"), gbc);
        gbc.gridx = 5; formPanel.add(cmbBedType, gbc);

        gbc.gridx = 0; gbc.gridy = 1; formPanel.add(new JLabel("Status:"), gbc);
        gbc.gridx = 1; formPanel.add(cmbStatus, gbc);
        gbc.gridx = 2; formPanel.add(new JLabel("Daily Rate (LKR):"), gbc);
        gbc.gridx = 3; formPanel.add(txtDailyRate, gbc);
        gbc.gridx = 4; formPanel.add(new JLabel("Package Info:"), gbc);
        gbc.gridx = 5; formPanel.add(txtPackageInfo, gbc);

        // Action Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton btnAdd = new JButton("Add Room");
        JButton btnUpdate = new JButton("Update Room");
        JButton btnStatus = new JButton("Change Status Only");
        JButton btnDelete = new JButton("Remove Room");
        JButton btnClear = new JButton("Clear");
        JButton btnRefresh = new JButton("Refresh");

        btnAdd.addActionListener(e -> handleAddRoom());
        btnUpdate.addActionListener(e -> handleUpdateRoom());
        btnStatus.addActionListener(e -> handleUpdateStatus());
        btnDelete.addActionListener(e -> handleDeleteRoom());
        btnClear.addActionListener(e -> clearFields());
        btnRefresh.addActionListener(e -> refreshTable());

        btnPanel.add(btnAdd);
        btnPanel.add(btnUpdate);
        btnPanel.add(btnStatus);
        btnPanel.add(btnDelete);
        btnPanel.add(btnClear);
        btnPanel.add(btnRefresh);

        JPanel northContainer = new JPanel(new BorderLayout(4, 4));
        northContainer.add(formPanel, BorderLayout.CENTER);
        northContainer.add(btnPanel, BorderLayout.SOUTH);
        add(northContainer, BorderLayout.NORTH);

        // Rooms Table
        String[] columns = {
                "Room ID", "Room Type", "Bed Type", "Daily Rate (LKR)",
                "Breakfast", "Extra Guest (LKR)", "Tax (%)", "Base/Max Cap", "Status"
        };
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        roomTable = new JTable(tableModel);
        roomTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        roomTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                populateFormFromSelectedRow();
            }
        });

        add(new JScrollPane(roomTable), BorderLayout.CENTER);
    }

    private void updatePackagePreview() {
        RoomType selected = (RoomType) cmbRoomType.getSelectedItem();
        if (selected != null) {
            txtDailyRate.setText(moneyFmt.format(selected.getDailyRate()));
            txtPackageInfo.setText(String.format(
                    "Breakfast: %s | Extra: LKR %.0f | Tax: %.0f%% | Cap: %d (Max %d)",
                    selected.hasComplimentaryBreakfast() ? "Yes" : "No",
                    selected.getExtraGuestCharge(),
                    selected.getTaxRate() * 100,
                    selected.getBaseCapacity(),
                    selected.getMaxCapacity()
            ));
        }
    }

    private void populateFormFromSelectedRow() {
        int row = roomTable.getSelectedRow();
        if (row < 0) {
            return;
        }
        String roomId = tableModel.getValueAt(row, 0).toString();
        Room room = roomService.findRoomByIdFromCache(roomId);
        if (room != null) {
            txtRoomId.setText(room.getRoomId());
            cmbRoomType.setSelectedItem(room.getRoomType());
            cmbBedType.setSelectedItem(room.getBedType());
            cmbStatus.setSelectedItem(room.getAvailabilityStatus());
            updatePackagePreview();
        }
    }

    private void handleAddRoom() {
        try {
            roomService.addRoom(
                    txtRoomId.getText(),
                    (RoomType) cmbRoomType.getSelectedItem(),
                    (BedType) cmbBedType.getSelectedItem(),
                    (AvailabilityStatus) cmbStatus.getSelectedItem()
            );
            JOptionPane.showMessageDialog(this, "Room added successfully.");
            clearFields();
            refreshTable();
        } catch (IllegalArgumentException | SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Add Room Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleUpdateRoom() {
        try {
            roomService.updateRoom(
                    txtRoomId.getText(),
                    (RoomType) cmbRoomType.getSelectedItem(),
                    (BedType) cmbBedType.getSelectedItem(),
                    (AvailabilityStatus) cmbStatus.getSelectedItem()
            );
            JOptionPane.showMessageDialog(this, "Room updated successfully.");
            refreshTable();
        } catch (IllegalArgumentException | SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Update Room Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleUpdateStatus() {
        try {
            roomService.updateRoomStatus(
                    txtRoomId.getText(),
                    (AvailabilityStatus) cmbStatus.getSelectedItem()
            );
            JOptionPane.showMessageDialog(this, "Room availability status updated.");
            refreshTable();
        } catch (IllegalArgumentException | SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Status Update Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleDeleteRoom() {
        String roomId = txtRoomId.getText().trim();
        if (roomId.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Select or enter a Room ID to remove.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to remove room " + roomId + "?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            roomService.deleteRoom(roomId);
            JOptionPane.showMessageDialog(this, "Room removed successfully.");
            clearFields();
            refreshTable();
        } catch (IllegalArgumentException | SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not remove room (it may be linked to existing reservations):\n" + ex.getMessage(),
                    "Remove Room Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearFields() {
        txtRoomId.setText("");
        cmbRoomType.setSelectedIndex(0);
        cmbBedType.setSelectedIndex(0);
        cmbStatus.setSelectedIndex(0);
        roomTable.clearSelection();
        updatePackagePreview();
    }

    public void refreshTable() {
        try {
            List<Room> rooms = roomService.loadAllRooms();
            tableModel.setRowCount(0);
            for (Room r : rooms) {
                tableModel.addRow(new Object[]{
                        r.getRoomId(),
                        r.getRoomType().getDisplayName(),
                        r.getBedType().getDisplayName(),
                        moneyFmt.format(r.getDailyRate()),
                        r.hasComplimentaryBreakfast() ? "Yes" : "No",
                        moneyFmt.format(r.getExtraGuestChargeRate()),
                        String.format("%.0f%%", r.getTaxRate() * 100),
                        r.getBaseCapacity() + " / " + r.getMaxCapacity(),
                        r.getAvailabilityStatus().getDisplayName()
                });
            }
        } catch (SQLException ignored) {
            // Connection warning already shown on startup
        }
    }
}
