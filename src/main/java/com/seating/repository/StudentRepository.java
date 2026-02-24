package com.seating.repository;

import com.seating.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Repository
public interface StudentRepository extends JpaRepository<Student, String> {
    
    /**
     * Find all students by department
     */
    List<Student> findByDept(String dept);
    
    /**
     * Delete all students by department
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM Student s WHERE s.dept = :dept")
    void deleteByDept(@Param("dept") String dept);
    
    /**
     * Delete all students
     */
    @Modifying
    @Transactional
    void deleteAll();
}
