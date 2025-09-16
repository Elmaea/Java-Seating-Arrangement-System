public class Student {
    private String rollNo;
    private String department;
    private String year;

    public Student(String rollNo, String department, String year) {
        this.rollNo = rollNo;
        this.department = department;
        this.year = year;
    }

    // Getters
    public String getRollNo() {
        return rollNo;
    }
    public String getDepartment() {
        return department;
    }
    public String getYear() {
        return year;
    }

    @Override
    public String toString() {
        return rollNo + " - " + department + " (Year " + year + ")";
    }
}

