# Student Department Upload Feature - Implementation Summary

## Overview
Implemented a complete department-wise student upload feature that allows authenticated departments to upload their student CSV files, which get stored in the MySQL database. When a department re-uploads, their old records are automatically replaced with new ones.

## Implementation Details

### 1. **Student Database Entity** ([entity/Student.java](entity/Student.java))
- Created a JPA entity class mapped to the `student` table in MySQL
- Fields: `Rollno` (Primary Key), `Dept`, `Year`
- Automatically managed by Hibernate with DDL-auto update

### 2. **StudentRepository** ([repository/StudentRepository.java](repository/StudentRepository.java))
- Spring Data JPA repository for database operations
- Custom methods:
  - `findByDept(String dept)` - Retrieve students by department
  - `deleteByDept(String dept)` - Delete all students of a department (used for re-upload handling)
  - `deleteAll()` - Clear all student records
- All write operations are `@Transactional` for data consistency

### 3. **StudentService** ([service/StudentService.java](service/StudentService.java))
- Core business logic layer for student management
- Key methods:
  - `uploadAndSaveStudents(MultipartFile studentFile, String dept)` - Main upload handler
    - Reads CSV file
    - Validates department match
    - **Deletes existing records for that department**
    - **Saves new records to database**
    - Returns count of saved records
  - `getStudentsByDept(String dept)` - Query students by department
  - `getAllStudents()` - Query all students
  - `deleteStudentsByDept(String dept)` - Manual deletion endpoint

### 4. **Updated UploadController** ([controller/UploadController.java](controller/UploadController.java))
- Updated `/api/dept/upload` endpoint with new workflow:
  1. Validates CSV format/structure using `CsvValidationService`
  2. Retrieves department name from `HttpSession` (set during dept login)
  3. Calls `StudentService.uploadAndSaveStudents()` to process and save
  4. Optionally saves backup CSV file
  5. Returns success message with record count
  
- Error handling for:
  - Unauthorized access (missing/invalid session)
  - Validation failures
  - IO exceptions
  - Database errors

### 5. **Department Login Integration**
- [DeptController.java](controller/DeptController.java) already sets `deptName` in session
- Session attributes used: `deptName` (department identifier)

## Data Flow

```
1. Department Login (DeptController → Session stores deptName)
   ↓
2. Navigate to Upload Page (/dept_upload.html)
   ↓
3. Select Student CSV (Rollno,Dept,Year columns)
   ↓
4. Submit Form (POST /api/dept/upload)
   ↓
5. UploadController validates CSV format
   ↓
6. StudentService processes file:
   - Reads CSV into Student entities
   - Validates department match (CSV dept == logged-in dept)
   - Deletes old records: DELETE FROM student WHERE Dept = '{deptName}'
   - Inserts new records: INSERT INTO student VALUES (...)
   ↓
7. Response: "Successfully uploaded X student records for {DeptName}"
   ↓
8. On Re-upload: Same department's old records are automatically replaced
```

## Re-upload Handling

When a department uploads again (e.g., due to mistakes):
- All existing student records for that department are deleted first
- Then new records are inserted
- This ensures no duplicate or outdated student entries

**Example:**
1. **Initial Upload (CSE):** 50 CSE students → Database has 50 CSE records + other dept records
2. **Re-upload (CSE):** 55 CSE students → Database deletes 50 CSE records, inserts 55 new CSE records
3. **Other dept data:** Remains untouched

## CSV File Format

**Expected Headers:** `Rollno,Dept,Year`

**Example File (Student.csv):**
```
Rollno,Dept,Year
CSE001,CSE,2024
CSE002,CSE,2024
CSE003,CSE,2025
ECE001,ECE,2024
```

## Multiple Department Support

Each department can independently:
- Upload their own student list
- Re-upload with corrections (auto-replaces old data)
- Merge into a single `student` table

**Example Database State:**
```
Rollno  | Dept | Year
--------|------|------
CSE001  | CSE  | 2024
CSE002  | CSE  | 2024
ECE001  | ECE  | 2024
ECE002  | ECE  | 2024
CIVIL001| CIVIL| 2024
```

## Technical Features

✅ **Transaction Management** - All database operations are atomic (@Transactional)
✅ **Input Validation** - CSV format validation before processing
✅ **Error Handling** - Comprehensive error messages for users
✅ **Session Security** - Department identity verified from session
✅ **Backup Storage** - CSV files saved for audit trail
✅ **Logging** - Console logging for debugging

## Database Changes

No additional migrations needed. Hibernate automatically creates/updates the `student` table on startup based on the Student entity definition.

## Testing Checklist

- [ ] Test department login stores deptName in session
- [ ] Test CSV upload with valid format
- [ ] Test database records are inserted correctly
- [ ] Test re-upload replaces old department data
- [ ] Test validation rejects invalid CSV format
- [ ] Test unauthorized access returns 403 error
- [ ] Test multiple departments can coexist in student table
