package com.seating.controller;

import com.seating.entity.Dept;
import com.seating.repository.DeptRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@Controller
public class DeptController {

    @Autowired
    private DeptRepository deptRepository;

    @GetMapping("/dept/register")
    public String showRegister() {
        return "redirect:/dept_register.html";
    }

    @PostMapping("/dept/register")
    public String registerDept(@RequestParam String deptName,
                               @RequestParam String email,
                               @RequestParam String password,
                               RedirectAttributes ra) {
        if (deptRepository.findByEmail(email) != null) {
            ra.addFlashAttribute("error", "Email already registered");
            return "redirect:/dept_register.html";
        }
        Dept dept = new Dept();
        dept.setDeptName(deptName);
        dept.setEmail(email);
        dept.setPassword(password);
        deptRepository.save(dept);
        ra.addFlashAttribute("success", "Registration successful. Please login.");
        return "redirect:/dept_login.html";
    }

    @GetMapping("/dept/login")
    public String showLogin() {
        return "redirect:/dept_login.html";
    }

    @PostMapping("/dept/login")
    public String loginDept(@RequestParam(required = false) String deptName,
                            @RequestParam String email,
                            @RequestParam String password,
                            RedirectAttributes ra,
                            HttpSession session) {
        Dept dept = deptRepository.findByEmail(email);
        if (dept == null || !dept.getPassword().equals(password)) {
            ra.addFlashAttribute("error", "Invalid credentials");
            return "redirect:/dept_login.html";
        }

        // if deptName not provided (e.g. wrong form), infer from DB
        if (deptName == null || deptName.isBlank()) {
            deptName = dept.getDeptName();
        }

        if (!dept.getDeptName().equalsIgnoreCase(deptName)) {
            ra.addFlashAttribute("error", "Department mismatch");
            return "redirect:/dept_login.html";
        }
        // store dept info in session for later uploads
        session.setAttribute("deptEmail", dept.getEmail());
        session.setAttribute("deptName", dept.getDeptName());
        // On successful login redirect to dept upload page
        return "redirect:/dept_upload.html";
    }

    /**
     * REST API endpoint to get the currently logged-in department info
     */
    @GetMapping("/api/dept/info")
    public ResponseEntity<Map<String, String>> getDeptInfo(HttpSession session) {
        Map<String, String> response = new HashMap<>();
        
        String deptName = (String) session.getAttribute("deptName");
        String deptEmail = (String) session.getAttribute("deptEmail");
        
        if (deptName == null || deptEmail == null) {
            response.put("authenticated", "false");
            return ResponseEntity.ok(response);
        }
        
        response.put("authenticated", "true");
        response.put("deptName", deptName);
        response.put("deptEmail", deptEmail);
        return ResponseEntity.ok(response);
    }

    /**
     * REST API endpoint to logout the department
     */
    @PostMapping("/api/dept/logout")
    public ResponseEntity<Map<String, String>> deptLogout(HttpSession session) {
        session.invalidate();
        Map<String, String> response = new HashMap<>();
        response.put("message", "Logged out successfully");
        return ResponseEntity.ok(response);
    }
}
