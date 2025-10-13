package com.seating.model;

public class Seating {
    private Student student;
    private Exam exam;

    public Seating(Student student, Exam exam) {
        this.student = student;
        this.exam = exam;
    }

    public Student getStudent() { return student; }
    public Exam getExam() { return exam; }

    @Override
    public String toString() {
        return student.getRollNo() + " - " + student.getDepartment() + " (Year " + student.getYear() + ") | "
             + exam.getExamDate() + " - " + exam.getSubject() + " (" + exam.getSubjectCode() + ")";
    }
}
