import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import javax.swing.text.DateFormatter;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ModestToDoListApp: A feature-rich Java Swing application
 * focused on a modern Dark Mode UI and complete CRUD operations
 * including filtering, searching, and sorting.
 */
public class ModestToDoListApp extends JFrame {

    // --- Design & Configuration Constants (Deep Dark Palette) ---
    private static final String FILE_NAME = "modest_tasks.ser";
    // Delimiter used to separate Task Name and Detailed Description
    private static final String NAME_DETAIL_DELIMITER = "\n###\n";

    // Deep Dark Colors
    private final Color PRIMARY_BG = Color.decode("#1E2C3D"); // Deep Navy Background
    private final Color SECONDARY_BG = Color.decode("#14202D"); // Darker Blue Panel/Separator
    private final Color TEXT_LIGHT = Color.decode("#E0E5E9"); // Off-White Text (Default Task Name Color)
    private final Color ACCENT_TEAL = Color.decode("#00C4A7"); // Vibrant Teal (Primary Action)
    private final Color ACCENT_GOLD = Color.decode("#FFD700"); // Soft Gold/Yellow (Edit / Medium Priority)
    private final Color ACCENT_SUCCESS = Color.decode("#28A745"); // Success Green (Completed Status)
    private final Color ACCENT_DELETE = Color.decode("#E74C3C"); // Danger Red (Delete/High Priority)
    private final Color BORDER_COLOR = Color.decode("#34495E"); // Dark Border Gray

    private final Font HEADER_FONT = new Font("SansSerif", Font.BOLD, 24);
    private final Font LABEL_FONT = new Font("SansSerif", Font.BOLD, 14);
    private final Font INPUT_FONT = new Font("SansSerif", Font.PLAIN, 14);
    private final Font DATE_FONT = new Font("SansSerif", Font.BOLD, 14);

    // Date Format for Main List View (MMM dd, yyyy)
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    // Date format for the input field in the dialog (yyyy-MM-dd)
    private static final DateTimeFormatter DIALOG_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String DATE_MASK = "####-##-##"; // Mask for yyyy-MM-dd

    // --- Data and Components ---
    private List<Task> allTasks = new ArrayList<>();
    private final DefaultListModel<Task> listModel = new DefaultListModel<>();
    private final JList<Task> taskJList = new JList<>(listModel);

    // Input/State Fields
    private JTextField taskNameField;
    private JTextArea taskDescriptionArea;
    private JComboBox<String> priorityComboBox;

    // NEW: Date Input Field
    private JFormattedTextField dueDateField;

    // Control Components
    private JTextField searchField;
    private JComboBox<String> filterStatusComboBox;
    private JComboBox<String> sortComboBox;

    // State for Editing (Kept for consistency)
    private Task taskToEdit = null;

    // Standard Dimensions
    private final Dimension SMALL_INPUT_SIZE = new Dimension(140, 36);

    // =========================================================================
    // I. TASK DATA MODEL
    // =========================================================================

    static class Task implements Serializable {
        private static final long serialVersionUID = 1L;

        String description;
        int priority; // 1=Low, 2=Medium, 3=High
        LocalDate dueDate;
        boolean isCompleted;
        long creationTimestamp;

        public Task(String description, int priority, LocalDate dueDate) {
            this.description = description;
            this.priority = priority;
            this.dueDate = dueDate;
            this.isCompleted = false;
            this.creationTimestamp = System.currentTimeMillis();
        }

        public void toggleCompletion() { this.isCompleted = !this.isCompleted; }

        // Accessors required for Sorting
        public String getDescription() { return description; }
        public int getPriority() { return priority; }
        public LocalDate getDueDate() { return dueDate; }
        public boolean isCompleted() { return isCompleted; }
        public long getCreationTimestamp() { return creationTimestamp; }

        public void setDescription(String description) { this.description = description; }
        public void setPriority(int priority) { this.priority = priority; }
        public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

        public String getDisplayName() {
            if (description.contains(NAME_DETAIL_DELIMITER)) {
                return description.split(NAME_DETAIL_DELIMITER, 2)[0];
            }
            return description;
        }

        public String getDetailDescription() {
            if (description.contains(NAME_DETAIL_DELIMITER)) {
                String[] parts = description.split(NAME_DETAIL_DELIMITER, 2);
                return parts.length > 1 ? parts[1] : "";
            }
            return "";
        }
    }

    // Helper class for instant search filtering
    private class DocumentChangeListener implements DocumentListener {
        private final ActionListener actionListener;
        public DocumentChangeListener(ActionListener actionListener) {
            this.actionListener = actionListener;
        }
        @Override
        public void insertUpdate(DocumentEvent e) { actionListener.actionPerformed(null); }
        @Override
        public void removeUpdate(DocumentEvent e) { actionListener.actionPerformed(null); }
        @Override
        public void changedUpdate(DocumentEvent e) { actionListener.actionPerformed(null); }
    }


    // =========================================================================
    // II. CUSTOM CELL RENDERER & STYLING
    // =========================================================================

    class TaskCellRenderer extends DefaultListCellRenderer {
        private final Color OVERDUE_BG = Color.decode("#4A1C1C");

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Task task = (Task) value;

            JPanel panel = new JPanel(new BorderLayout(10, 0));
            panel.setOpaque(true);
            panel.setBorder(new EmptyBorder(12, 18, 12, 18));

            Color bg = PRIMARY_BG;
            // Highlight Overdue tasks with a dark red background
            if (task.getDueDate().isBefore(LocalDate.now()) && !task.isCompleted()) {
                 bg = OVERDUE_BG;
            }

            // --- 1. Left Side: Task Name and Priority ---
            JLabel nameLabel = new JLabel();
            nameLabel.setFont(INPUT_FONT);
            nameLabel.setOpaque(false);

            Color fg = TEXT_LIGHT; // Default color for Low Priority
            String displayName = task.getDisplayName();

            if (task.isCompleted()) {
                // Completed tasks
                fg = ACCENT_SUCCESS;
                nameLabel.setText("<html><del>" + displayName + "</del></html>");
            } else {
                // Incomplete tasks: apply priority color
                if (task.getPriority() == 3) {
                    fg = ACCENT_DELETE.brighter(); // High Priority (Red)
                } else if (task.getPriority() == 2) {
                    fg = ACCENT_GOLD.brighter(); // Medium Priority (Gold/Yellow)
                }
                nameLabel.setText(displayName);
            }

            nameLabel.setForeground(fg);

            // --- 2. Right Side: Due Date ---
            JLabel dateLabel = new JLabel("Due: " + task.getDueDate().format(DATE_FORMATTER));
            dateLabel.setFont(DATE_FONT);
            dateLabel.setHorizontalAlignment(SwingConstants.RIGHT);
            dateLabel.setOpaque(false);
            // Due date text color is green if completed, teal otherwise
            dateLabel.setForeground(task.isCompleted() ? ACCENT_SUCCESS : ACCENT_TEAL.brighter());

            panel.add(nameLabel, BorderLayout.CENTER);
            panel.add(dateLabel, BorderLayout.EAST);

            // --- 3. Selection and Final Colors ---
            if (isSelected) {
                panel.setBackground(ACCENT_TEAL.darker().darker());
            } else {
                panel.setBackground(bg);
            }

            nameLabel.setBackground(panel.getBackground());
            dateLabel.setBackground(panel.getBackground());

            return panel;
        }
    }

    /**
     * Styles the JComboBox and ensures popup visibility against the dark theme.
     */
    private void styleComboBoxForVisibility(JComboBox<String> comboBox, Dimension size) {
        comboBox.setPreferredSize(size);
        comboBox.setFont(INPUT_FONT);
        comboBox.setEditable(false);

        // Set the visible component (the box itself) to Secondary Dark BG
        comboBox.setBackground(SECONDARY_BG);
        // Set foreground to Black for content in the field (Fixes visibility in dialog)
        comboBox.setForeground(Color.BLACK);
        comboBox.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));

        // Use a renderer to force the popup list's colors to be readable
        comboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                // Keep the popup list text black for contrast
                comp.setForeground(Color.BLACK);

                if (isSelected) {
                    comp.setBackground(ACCENT_TEAL.brighter());
                    comp.setForeground(Color.WHITE);
                } else {
                    // This sets the styling for unselected items in the popup list
                    comp.setBackground(Color.WHITE);
                }
                return comp;
            }
        });
    }


    // =========================================================================
    // III. APPLICATION CORE & UI SETUP
    // =========================================================================

    public ModestToDoListApp() {
        // Set System L&F for better integration (best done first)
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Fails gracefully if L&F is unavailable
        }

        setTitle("Modern Task Manager");
        // Ensure data is saved on close
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                saveTasks();
                dispose();
                System.exit(0);
            }
        });

        getContentPane().setBackground(SECONDARY_BG);
        setLayout(new BorderLayout(0, 0));

        loadTasks();

        taskJList.setCellRenderer(new TaskCellRenderer());
        taskJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        taskJList.setBackground(PRIMARY_BG);
        taskJList.setBorder(new EmptyBorder(0, 0, 0, 0));

        // Double-click to edit
        taskJList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    setupEditTask();
                }
            }
        });

        add(createControlPanel(), BorderLayout.NORTH);
        add(createMainContentPanel(), BorderLayout.CENTER);
        add(createActionPanel(), BorderLayout.SOUTH);

        applyFiltersAndSorting(); // Initial display

        pack();
        setSize(1000, 750);
        setMinimumSize(new Dimension(850, 650));
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JPanel createMainContentPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout(0, 0));
        mainPanel.setBorder(new EmptyBorder(0, 20, 0, 20));
        mainPanel.setBackground(SECONDARY_BG);

        JScrollPane scrollPane = new JScrollPane(taskJList);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        scrollPane.getViewport().setBackground(PRIMARY_BG);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        return mainPanel;
    }

    private JPanel createControlPanel() {
        JPanel topWrapper = new JPanel(new BorderLayout());
        topWrapper.setBackground(PRIMARY_BG);
        topWrapper.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR),
            new EmptyBorder(15, 20, 10, 20)
        ));

        JLabel listHeader = new JLabel("Task List and Controls");
        listHeader.setFont(HEADER_FONT);
        listHeader.setForeground(TEXT_LIGHT);
        topWrapper.add(listHeader, BorderLayout.NORTH);

        // --- 1. Primary Controls Row (Add, Filter, Sort) ---
        JPanel primaryControlPanel = new JPanel(new GridBagLayout());
        primaryControlPanel.setBackground(PRIMARY_BG);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Add New Button
        JButton addNewButton = createStyledButton("Add New Task", ACCENT_TEAL, ACCENT_TEAL.brighter());
        addNewButton.setPreferredSize(new Dimension(150, 42));
        addNewButton.addActionListener(e -> showTaskDialog(null, true));
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.25; primaryControlPanel.add(addNewButton, gbc);

        // Status Filter
        JLabel filterLabel = new JLabel("Status Filter:");
        filterLabel.setFont(LABEL_FONT); filterLabel.setForeground(TEXT_LIGHT);
        String[] statuses = {"All Statuses", "Incomplete", "Completed"};
        filterStatusComboBox = new JComboBox<>(statuses);
        styleComboBoxForVisibility(filterStatusComboBox, new Dimension(140, 36));
        filterStatusComboBox.addActionListener(e -> applyFiltersAndSorting());
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 0; primaryControlPanel.add(filterLabel, gbc);
        gbc.gridx = 2; gbc.weightx = 0.25; primaryControlPanel.add(filterStatusComboBox, gbc);

        // Sort By
        JLabel sortLabel = new JLabel("Sort By:");
        sortLabel.setFont(LABEL_FONT); sortLabel.setForeground(TEXT_LIGHT);
        String[] sorts = {
            "Creation Date (Newest)",
            "Due Date (Upcoming)",
            "Due Date (Farthest)",
            "Priority (High to Low)",
            "Priority (Low to High)"
        };
        sortComboBox = new JComboBox<>(sorts);
        styleComboBoxForVisibility(sortComboBox, new Dimension(160, 36));
        sortComboBox.addActionListener(e -> applyFiltersAndSorting());

        gbc.gridx = 3; gbc.gridy = 0; gbc.weightx = 0; primaryControlPanel.add(sortLabel, gbc);
        gbc.gridx = 4; gbc.weightx = 0.25; primaryControlPanel.add(sortComboBox, gbc);

        // --- 2. Search Bar Row ---
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        searchPanel.setBackground(PRIMARY_BG);

        JLabel searchLabel = new JLabel("Search Task Name/Details:");
        searchLabel.setFont(LABEL_FONT); searchLabel.setForeground(TEXT_LIGHT);

        searchField = new JTextField(40);
        searchField.setPreferredSize(new Dimension(350, 36));
        searchField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        searchField.setFont(INPUT_FONT);
        searchField.setBackground(SECONDARY_BG);
        searchField.setForeground(TEXT_LIGHT);
        searchField.setCaretColor(TEXT_LIGHT);
        searchField.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        searchField.getDocument().addDocumentListener(new DocumentChangeListener(e -> applyFiltersAndSorting()));

        searchPanel.add(searchLabel);
        searchPanel.add(searchField);

        JPanel combinedControls = new JPanel();
        combinedControls.setLayout(new BoxLayout(combinedControls, BoxLayout.Y_AXIS));
        combinedControls.setBackground(PRIMARY_BG);

        primaryControlPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        searchPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        combinedControls.add(primaryControlPanel);
        combinedControls.add(searchPanel);

        topWrapper.add(combinedControls, BorderLayout.CENTER);

        return topWrapper;
    }

    private JButton createStyledButton(String text, Color bgColor, Color hoverColor) {
        JButton button = new JButton(text);
        button.setBackground(bgColor);
        button.setForeground(Color.BLACK); // Explicitly setting text color to black
        button.setFont(new Font("SansSerif", Font.BOLD, 14));
        button.setFocusPainted(false);

        Border compoundBorder = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(bgColor.darker(), 1),
            new EmptyBorder(10, 20, 10, 20)
        );
        button.setBorder(compoundBorder);

        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) { button.setBackground(hoverColor); }
            public void mouseExited(java.awt.event.MouseEvent evt) { button.setBackground(bgColor); }
        });

        return button;
    }

    /**
     * Uses GridBagLayout to force action buttons to take up all available width.
     */
    private JPanel createActionPanel() {
        JPanel actionPanel = new JPanel(new GridBagLayout());
        actionPanel.setBackground(SECONDARY_BG);
        actionPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COLOR.darker()),
            new EmptyBorder(10, 10, 10, 10)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL; // Make components fill the cell
        gbc.weightx = 1.0; // Distribute space evenly among buttons
        gbc.insets = new Insets(0, 10, 0, 10); // Spacing between buttons

        // Buttons
        JButton editButton = createStyledButton("Edit Task", ACCENT_GOLD.darker(), ACCENT_GOLD);
        editButton.setPreferredSize(new Dimension(100, 42)); // Set height
        editButton.addActionListener(e -> setupEditTask());
        gbc.gridx = 0; actionPanel.add(editButton, gbc);

        JButton toggleButton = createStyledButton("Toggle Completion", ACCENT_SUCCESS.darker(), ACCENT_SUCCESS);
        toggleButton.setPreferredSize(new Dimension(100, 42)); // Set height
        toggleButton.addActionListener(e -> toggleTaskCompletion());
        gbc.gridx = 1; actionPanel.add(toggleButton, gbc);

        JButton deleteButton = createStyledButton("Delete Task", ACCENT_DELETE, ACCENT_DELETE.brighter());
        deleteButton.setPreferredSize(new Dimension(100, 42)); // Set height
        deleteButton.addActionListener(e -> deleteSelectedTask());
        gbc.gridx = 2; actionPanel.add(deleteButton, gbc);

        return actionPanel;
    }

    // =========================================================================
    // IV. TASK DIALOG (Modal for Add/Edit)
    // =========================================================================

    /**
     * Uses JFormattedTextField with a date mask for date input.
     */
    private void showTaskDialog(Task task, boolean isNew) {
        JDialog dialog = new JDialog(this, isNew ? "Add New Task" : "Edit Task", true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.getContentPane().setBackground(PRIMARY_BG);
        dialog.setSize(600, 500);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);

        LocalDate initialDate = task != null ? task.getDueDate() : LocalDate.now().plusWeeks(1);
        int initialPriorityIndex = task != null ? 3 - task.getPriority() : 0;

        String initialName = task != null ? task.getDisplayName() : "";
        String initialDetails = task != null ? task.getDetailDescription() : "";

        // --- Center Panel (Name and Description Inputs) ---
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));
        inputPanel.setBackground(PRIMARY_BG);
        inputPanel.setBorder(new EmptyBorder(15, 15, 0, 15));

        JLabel nameLabel = new JLabel("Task Name (Short):");
        nameLabel.setFont(LABEL_FONT); nameLabel.setForeground(TEXT_LIGHT);
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        taskNameField = new JTextField(initialName);
        taskNameField.setPreferredSize(new Dimension(550, 36));
        taskNameField.setMaximumSize(new Dimension(550, 36));
        taskNameField.setFont(INPUT_FONT);
        taskNameField.setBackground(Color.WHITE); taskNameField.setForeground(Color.BLACK);
        taskNameField.setCaretColor(Color.BLACK);
        taskNameField.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));

        inputPanel.add(nameLabel);
        inputPanel.add(taskNameField);
        inputPanel.add(Box.createVerticalStrut(10));

        JLabel descLabel = new JLabel("Detailed Description:");
        descLabel.setFont(LABEL_FONT); descLabel.setForeground(TEXT_LIGHT);
        descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        taskDescriptionArea = new JTextArea(initialDetails);
        taskDescriptionArea.setFont(INPUT_FONT);
        taskDescriptionArea.setLineWrap(true); taskDescriptionArea.setWrapStyleWord(true);
        taskDescriptionArea.setBackground(Color.WHITE); taskDescriptionArea.setForeground(Color.BLACK);
        taskDescriptionArea.setCaretColor(Color.BLACK);
        taskDescriptionArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JScrollPane scrollPane = new JScrollPane(taskDescriptionArea);
        scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        scrollPane.setPreferredSize(new Dimension(550, 180));
        scrollPane.setMaximumSize(new Dimension(550, 180));
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));

        inputPanel.add(descLabel);
        inputPanel.add(scrollPane);

        dialog.add(inputPanel, BorderLayout.CENTER);

        // --- Bottom Panel (Priority and Date Input) ---
        JPanel controlPanel = new JPanel(new GridBagLayout());
        controlPanel.setBackground(PRIMARY_BG);
        controlPanel.setBorder(new EmptyBorder(10, 15, 15, 15));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Priority setup
        JLabel priorityLabel = new JLabel("Priority:");
        priorityLabel.setFont(LABEL_FONT); priorityLabel.setForeground(TEXT_LIGHT);
        String[] priorities = {"High", "Medium", "Low"};
        priorityComboBox = new JComboBox<>(priorities);
        styleComboBoxForVisibility(priorityComboBox, SMALL_INPUT_SIZE);
        priorityComboBox.setSelectedIndex(initialPriorityIndex);

        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST; controlPanel.add(priorityLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST; controlPanel.add(priorityComboBox, gbc);

        // Date Input Field setup (FIXED)
        JLabel dateLabel = new JLabel("Due Date (YYYY-MM-DD):");
        dateLabel.setFont(LABEL_FONT); dateLabel.setForeground(TEXT_LIGHT);

        MaskFormatter mask = null;
        try {
            mask = new MaskFormatter(DATE_MASK);
            mask.setPlaceholderCharacter('_');
        } catch (ParseException e) {
            e.printStackTrace();
        }

        // FIX: Use SimpleDateFormat for compatibility with DateFormatter
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        dateFormat.setLenient(false); // Disallow invalid dates (e.g., Feb 30th)
        DateFormatter dateFormatter = new DateFormatter(dateFormat);

        dueDateField = new JFormattedTextField(dateFormatter);
        if (mask != null) {
            mask.install(dueDateField);
        }
        
        // **CRITICAL FIX:** Use setText() instead of setValue() to avoid IllegalArgumentException
        dueDateField.setText(initialDate.format(DIALOG_DATE_FORMATTER)); 

        dueDateField.setPreferredSize(new Dimension(120, 36));
        dueDateField.setFont(INPUT_FONT);
        dueDateField.setHorizontalAlignment(JTextField.CENTER);
        dueDateField.setBackground(Color.WHITE);
        dueDateField.setForeground(Color.BLACK);
        dueDateField.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));

        gbc.gridx = 2; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST; controlPanel.add(dateLabel, gbc);
        gbc.gridx = 3; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST; controlPanel.add(dueDateField, gbc);

        // Action Buttons (Save/Cancel)
        JButton saveButton = createStyledButton(isNew ? "Add Task" : "Update Task", ACCENT_TEAL, ACCENT_TEAL.brighter());
        JButton cancelButton = createStyledButton("Cancel", BORDER_COLOR.darker(), BORDER_COLOR);

        saveButton.setPreferredSize(new Dimension(120, 42));
        cancelButton.setPreferredSize(new Dimension(100, 42));

        saveButton.addActionListener(e -> {
            if (handleSaveTask(task, isNew)) {
                dialog.dispose();
            }
        });
        cancelButton.addActionListener(e -> dialog.dispose());

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonRow.setBackground(PRIMARY_BG);
        buttonRow.add(cancelButton);
        buttonRow.add(saveButton);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 4; gbc.anchor = GridBagConstraints.EAST;
        controlPanel.add(buttonRow, gbc);

        dialog.add(controlPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    // =========================================================================
    // V. CRUD OPERATIONS
    // =========================================================================

    /**
     * Reads task details and the date from the JFormattedTextField.
     */
    private boolean handleSaveTask(Task task, boolean isNew) {
        String taskName = taskNameField.getText().trim();
        String taskDetails = taskDescriptionArea.getText().trim();
        int priority = 3 - priorityComboBox.getSelectedIndex();
        LocalDate dueDate;

        if (taskName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Task Name cannot be empty.", "Input Error", JOptionPane.WARNING_MESSAGE);
            return false;
        }

        // Read and validate date from the JFormattedTextField
        try {
            // Replace placeholder character (if user didn't type fully)
            String dateString = dueDateField.getText().trim().replace('_', '0');
            // Check if the date is fully entered and parse it
            if (dateString.contains("0")) {
                 JOptionPane.showMessageDialog(this, "Please enter a valid, complete date in YYYY-MM-DD format.", "Date Error", JOptionPane.WARNING_MESSAGE);
                 return false;
            }
            dueDate = LocalDate.parse(dateString, DIALOG_DATE_FORMATTER);
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "Invalid date format. Please use YYYY-MM-DD.", "Date Error", JOptionPane.WARNING_MESSAGE);
            return false;
        }

        String combinedDescription = taskName + NAME_DETAIL_DELIMITER + taskDetails;

        if (isNew) {
            Task newTask = new Task(combinedDescription, priority, dueDate);
            allTasks.add(newTask);
        } else {
            task.setDescription(combinedDescription);
            task.setPriority(priority);
            task.setDueDate(dueDate);
            taskToEdit = null;
        }

        applyFiltersAndSorting();
        saveTasks();
        return true;
    }

    private void setupEditTask() {
        int selectedIndex = taskJList.getSelectedIndex();
        if (selectedIndex != -1) {
            Task task = listModel.getElementAt(selectedIndex);
            showTaskDialog(task, false);
        } else {
            JOptionPane.showMessageDialog(this, "Please select a task to edit.", "Selection Required", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void toggleTaskCompletion() {
        int selectedIndex = taskJList.getSelectedIndex();
        if (selectedIndex != -1) {
            Task task = listModel.getElementAt(selectedIndex);
            task.toggleCompletion();

            // Reapply filters and sorting to update the list view immediately
            applyFiltersAndSorting();
            saveTasks();
        } else {
            JOptionPane.showMessageDialog(this, "Please select a task to toggle its completion status.", "Selection Required", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void deleteSelectedTask() {
        int selectedIndex = taskJList.getSelectedIndex();
        if (selectedIndex != -1) {
            Task task = listModel.getElementAt(selectedIndex);

            int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete the task: '" + task.getDisplayName() + "'?",
                "Confirm Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

            if (confirm == JOptionPane.YES_OPTION) {
                allTasks.remove(task);
                applyFiltersAndSorting();
                saveTasks();
            }
        } else {
            JOptionPane.showMessageDialog(this, "Please select a task to delete.", "Selection Required", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Main method for applying search, status filters, and sorting the task list.
     */
    private void applyFiltersAndSorting() {
        // 1. Get Filters and Sort Criteria
        String searchText = searchField.getText().toLowerCase().trim();
        String filterStatus = (String) filterStatusComboBox.getSelectedItem();
        String sortCriteria = (String) sortComboBox.getSelectedItem(); // This holds the sort choice

        // 2. Filtering
        List<Task> filteredTasks = allTasks.stream()
            .filter(task -> {
                // Status Filter
                boolean statusMatch = true;
                if ("Incomplete".equals(filterStatus)) {
                    statusMatch = !task.isCompleted();
                } else if ("Completed".equals(filterStatus)) {
                    statusMatch = task.isCompleted();
                }

                // Search Filter
                boolean searchMatch = true;
                if (!searchText.isEmpty()) {
                    String taskContent = (task.getDisplayName() + task.getDetailDescription()).toLowerCase();
                    searchMatch = taskContent.contains(searchText);
                }

                return statusMatch && searchMatch;
            })
            .collect(Collectors.toList());

        // 3. Sorting Logic (Uses Comparators based on sortCriteria)
        Comparator<Task> comparator;

        switch (sortCriteria) {
            case "Due Date (Upcoming)":
                // Sort by Due Date (Ascending/Nearest), then Priority (Descending)
                comparator = Comparator
                    .comparing(Task::getDueDate)
                    .thenComparing(Task::getPriority, Comparator.reverseOrder())
                    .thenComparing(Task::getCreationTimestamp);
                break;

            case "Due Date (Farthest)":
                // Sort by Due Date (Descending/Farthest), then Priority (Descending)
                comparator = Comparator
                    .comparing(Task::getDueDate, Comparator.reverseOrder())
                    .thenComparing(Task::getPriority, Comparator.reverseOrder())
                    .thenComparing(Task::getCreationTimestamp);
                break;

            case "Priority (High to Low)":
                // Sort by Priority (Descending/High), then Due Date (Ascending)
                comparator = Comparator
                    .comparing(Task::getPriority, Comparator.reverseOrder())
                    .thenComparing(Task::getDueDate)
                    .thenComparing(Task::getCreationTimestamp);
                break;

            case "Priority (Low to High)":
                // Sort by Priority (Ascending/Low), then Due Date (Ascending)
                comparator = Comparator
                    .comparing(Task::getPriority)
                    .thenComparing(Task::getDueDate)
                    .thenComparing(Task::getCreationTimestamp);
                break;

            case "Creation Date (Newest)":
            default:
                // Default sort by Creation Timestamp (Newest first)
                comparator = Comparator.comparing(Task::getCreationTimestamp, Comparator.reverseOrder());
                break;
        }

        // Apply the sort
        filteredTasks.sort(comparator);

        // 4. Update the JList Model
        listModel.clear();
        filteredTasks.forEach(listModel::addElement);

        // Ensure an item is always selected if the list is not empty
        if (!listModel.isEmpty() && taskJList.getSelectedIndex() == -1) {
            taskJList.setSelectedIndex(0);
        }
    }

    // =========================================================================
    // VI. PERSISTENCE
    // =========================================================================

    @SuppressWarnings("unchecked")
    private void loadTasks() {
        File file = new File(FILE_NAME);
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                allTasks = (List<Task>) ois.readObject();
            } catch (FileNotFoundException e) {
                allTasks = new ArrayList<>();
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Error loading tasks: " + e.getMessage());
                JOptionPane.showMessageDialog(this, "Error loading existing tasks.", "Load Error", JOptionPane.ERROR_MESSAGE);
                allTasks = new ArrayList<>();
            }
        } else {
             allTasks = new ArrayList<>();
        }
    }

    private void saveTasks() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_NAME))) {
            oos.writeObject(allTasks);
        } catch (IOException e) {
            System.err.println("Error saving tasks: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error saving tasks.", "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // =========================================================================
    // VII. MAIN METHOD
    // =========================================================================

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ModestToDoListApp::new);
    }
}