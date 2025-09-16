import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {
       /* String studentFile = "C:\\Users\\Admin\\Downloads\\Student details - Exam.csv";
//  C:\Users\Admin\Downloads\Student details - Exam.csv
//  C:\\Users\\Admin\\Downloads\\Student details - Student.csv
        ArrayList<Student> students = StudentReader.readStudentsFromCSV(studentFile);

        System.out.println("Students from CSV:");
        for (Student s : students) {
            System.out.println(s);
        }*/
        String filePath = "C:\\Users\\Admin\\Downloads\\Student details - Exam.csv";
        ArrayList<Exam> exams = ExamReader.readExamsFromCSV(filePath);

        for (Exam exam : exams) {
            System.out.println(exam.printExam());
        }
    }
}
