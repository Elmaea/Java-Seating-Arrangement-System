package com.seating.entity;

public class Student {

    private String rollNo;

    private String dept;

    private String year;

    public Student() {}

    public Student(String rollNo, String dept, String year) {
        this.rollNo = rollNo;
        this.dept = dept;
        this.year = year;
    }

    // Getters and Setters
    public String getRollNo() {
        return rollNo;
    }

    public void setRollNo(String rollNo) {
        this.rollNo = rollNo;
    }

    public String getDept() {
        return dept;
    }

    public void setDept(String dept) {
        this.dept = dept;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }
}
