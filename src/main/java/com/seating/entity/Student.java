package com.seating.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "student")
public class Student {

    @Id
    @Column(name = "Rollno", length = 20)
    private String rollNo;

    @Column(name = "Dept", length = 20)
    private String dept;

    @Column(name = "Year", length = 10)
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
