import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

// ----- Main class (public) – contains main method -----
public class Main {
    private static List<Student> students = new ArrayList<>();
    private static Scanner scanner = new Scanner(System.in);
    private static final String LINE = "=".repeat(50);

    public static void main(String[] args) {
        System.out.println("\n🎓  STUDENT GRADE MANAGEMENT SYSTEM");
        System.out.println(LINE);

        while (true) {
            showMenu();
            int choice = getIntInput("👉 Enter your choice: ");

            switch (choice) {
                case 1 -> addStudent();
                case 2 -> addGrade();
                case 3 -> viewIndividual();
                case 4 -> viewSummaryReport();
                case 5 -> viewOverallStats();
                case 6 -> {
                    System.out.println("👋 Exiting... Thank you!");
                    scanner.close();
                    return;
                }
                default -> System.out.println("⚠️  Please choose 1 to 6.");
            }
        }
    }

    // ----- Inner Student class (static) -----
    static class Student {
        private String name;
        private List<Integer> grades;

        public Student(String name) {
            this.name = name;
            this.grades = new ArrayList<>();
        }

        public String getName() { return name; }
        public List<Integer> getGrades() { return grades; }

        public boolean addGrade(int grade) {
            if (grade < 0 || grade > 100) {
                System.out.println("❌ Grade must be between 0 and 100.");
                return false;
            }
            grades.add(grade);
            return true;
        }

        public double getAverage() {
            if (grades.isEmpty()) return 0.0;
            int sum = 0;
            for (int g : grades) sum += g;
            return (double) sum / grades.size();
        }

        public int getHighest() {
            if (grades.isEmpty()) return -1;
            int max = grades.get(0);
            for (int g : grades) if (g > max) max = g;
            return max;
        }

        public int getLowest() {
            if (grades.isEmpty()) return -1;
            int min = grades.get(0);
            for (int g : grades) if (g < min) min = g;
            return min;
        }

        public int getGradeCount() { return grades.size(); }

        public String getReportRow() {
            if (grades.isEmpty()) {
                return String.format("| %-12s | %4d       | %6s   | %6s   | %6s   |",
                        name, 0, "N/A", "N/A", "N/A");
            }
            return String.format("| %-12s | %4d       | %8.2f | %6d   | %6d   |",
                    name, grades.size(), getAverage(), getHighest(), getLowest());
        }
    }

    // ----- Menu Display -----
    private static void showMenu() {
        System.out.println("\n" + LINE);
        System.out.println("📌  MAIN MENU");
        System.out.println(LINE);
        System.out.println("1. ➕ Add Student");
        System.out.println("2. 📝 Add Grade to Student");
        System.out.println("3. 🔍 View Individual Student (Avg/High/Low)");
        System.out.println("4. 📊 View Full Summary Report (All Students)");
        System.out.println("5. 📈 View Overall Class Stats (Avg/High/Low)");
        System.out.println("6. 🚪 Exit");
        System.out.println(LINE);
    }

    // ----- 1. Add Student -----
    private static void addStudent() {
        System.out.print("Enter student name: ");
        String name = scanner.nextLine().trim();
        if (name.isEmpty()) {
            System.out.println("❌ Name cannot be empty.");
            return;
        }
        for (Student s : students) {
            if (s.getName().equalsIgnoreCase(name)) {
                System.out.println("⚠️  Student already exists.");
                return;
            }
        }
        students.add(new Student(name));
        System.out.println("✅ Student '" + name + "' added.");
    }

    // ----- 2. Add Grade -----
    private static void addGrade() {
        if (students.isEmpty()) {
            System.out.println("❌ No students. Add one first.");
            return;
        }
        System.out.print("Enter student name: ");
        Student s = findStudent(scanner.nextLine().trim());
        if (s == null) {
            System.out.println("❌ Student not found.");
            return;
        }
        int grade = getIntInput("Enter grade (0-100): ");
        if (s.addGrade(grade)) {
            System.out.println("✅ Grade " + grade + " added.");
        }
    }

    // ----- 3. View Individual -----
    private static void viewIndividual() {
        if (students.isEmpty()) {
            System.out.println("❌ No students.");
            return;
        }
        System.out.print("Enter student name: ");
        Student s = findStudent(scanner.nextLine().trim());
        if (s == null) {
            System.out.println("❌ Not found.");
            return;
        }
        System.out.println("\n" + LINE);
        System.out.println("🔍  DETAILS FOR: " + s.getName());
        System.out.println(LINE);
        System.out.println("Total Grades  : " + s.getGradeCount());
        if (s.getGradeCount() == 0) {
            System.out.println("Average       : N/A");
            System.out.println("Highest       : N/A");
            System.out.println("Lowest        : N/A");
        } else {
            System.out.printf("Average       : %.2f\n", s.getAverage());
            System.out.println("Highest       : " + s.getHighest());
            System.out.println("Lowest        : " + s.getLowest());
            System.out.println("All Grades    : " + s.getGrades());
        }
        System.out.println(LINE);
    }

    // ----- 4. Full Summary Report -----
    private static void viewSummaryReport() {
        if (students.isEmpty()) {
            System.out.println("📭  No students to report.");
            return;
        }
        System.out.println("\n📊  COMPLETE SUMMARY REPORT");
        System.out.println(LINE);
        System.out.printf("| %-12s | %-10s | %-8s | %-8s | %-8s |\n",
                "Name", "Grades", "Average", "Highest", "Lowest");
        System.out.println("-".repeat(50));
        for (Student s : students) {
            System.out.println(s.getReportRow());
        }
        System.out.println(LINE);
        System.out.println("🏷️  Total Students: " + students.size());
    }

    // ----- 5. Overall Class Stats -----
    private static void viewOverallStats() {
        if (students.isEmpty()) {
            System.out.println("❌ No students.");
            return;
        }

        List<Integer> allGrades = new ArrayList<>();
        int withGrades = 0;
        for (Student s : students) {
            if (s.getGradeCount() > 0) {
                allGrades.addAll(s.getGrades());
                withGrades++;
            }
        }

        if (allGrades.isEmpty()) {
            System.out.println("📭  No grades entered yet.");
            return;
        }

        int sum = 0, high = allGrades.get(0), low = allGrades.get(0);
        for (int g : allGrades) {
            sum += g;
            if (g > high) high = g;
            if (g < low) low = g;
        }
        double avg = (double) sum / allGrades.size();

        System.out.println("\n📈  OVERALL CLASS STATISTICS");
        System.out.println(LINE);
        System.out.println("Total Students        : " + students.size());
        System.out.println("Students with grades  : " + withGrades);
        System.out.println("Total Grades entered  : " + allGrades.size());
        System.out.printf("Overall Class Average : %.2f\n", avg);
        System.out.println("Class Highest Score   : " + high);
        System.out.println("Class Lowest Score    : " + low);
        System.out.println(LINE);
    }

    // ----- Helper Methods -----
    private static Student findStudent(String name) {
        for (Student s : students) {
            if (s.getName().equalsIgnoreCase(name)) return s;
        }
        return null;
    }

    private static int getIntInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("❌ Please enter a valid number.");
            }
        }
    }
}