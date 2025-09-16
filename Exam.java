public class Exam {
    private String examDate;
    private String subject;
    private String subjectCode;
    private String department;
    private String year;

    public Exam(String examDate, String subject, String subjectCode, String department, String year) {
        this.examDate = examDate;
        this.subject = subject;
        this.subjectCode = subjectCode;
        this.department = department;
        this.year = year;
    }

    // Getters
    public String getExamDate() {
        return examDate;
    }

    public String getSubject() {
        return subject;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public String getDepartment() {
        return department;
    }

    public String getYear() {
        return year;
    }

    public String printExam() {
        return examDate + " - " + subject + " (" + subjectCode + "), " + department + ", Year " + year;
    }
}
