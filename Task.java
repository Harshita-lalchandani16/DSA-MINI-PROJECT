import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Task.java
 * The data model for a To-Do item, including its ID, details, and status.
 */
public class Task {
    private static long nextId = 1;
    private final long id;
    private String title;
    private String description;
    private LocalDate dueDate;
    private String category;
    private String priority; // High, Medium, Low
    private boolean completed;

    // Constructor to create a new task
    public Task(String title, String description, LocalDate dueDate, String category, String priority) {
        this.id = nextId++;
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.category = category;
        this.priority = priority;
        this.completed = false;
    }

    // Constructor for loading tasks with existing IDs (or manual creation/update)
    public Task(long id, String title, String description, LocalDate dueDate, String category, String priority, boolean completed) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.category = category;
        this.priority = priority;
        this.completed = completed;
        // Ensure nextId is always greater than the highest ID loaded
        if (id >= nextId) {
            nextId = id + 1;
        }
    }

    // Getters
    public long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public LocalDate getDueDate() { return dueDate; }
    public String getCategory() { return category; }
    public String getPriority() { return priority; }
    public boolean isCompleted() { return completed; }

    // Setters (ID is final, cannot be set)
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public void setCategory(String category) { this.category = category; }
    public void setPriority(String priority) { this.priority = priority; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    // Utility method for table display
    public Object[] toRowData() {
        return new Object[]{
            id,
            title,
            priority,
            category,
            dueDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            completed ? "Yes" : "No"
        };
    }
}
