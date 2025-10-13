import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {
        String studentFile = "C:\\Users\\Admin\\Downloads\\Student details - Student.csv";
        String examFile = "C:\\Users\\Admin\\Downloads\\Student details - Exam.csv";

        ArrayList<Student> students = StudentReader.readStudentsFromCSV(studentFile);
        ArrayList<Exam> exams = ExamReader.readExamsFromCSV(examFile);

        ArrayList<Seating> seatingList = new ArrayList<>();

        for (Student s : students) {
            for (Exam e : exams) {
                if (s.getDepartment().equalsIgnoreCase(e.getDepartment()) &&
                    s.getYear().equalsIgnoreCase(e.getYear())) {

                    seatingList.add(new Seating(s, e));
                }
            }
        }

        // Print out seating plan
        System.out.println("Seating Plan:");
        for (Seating seat : seatingList) {
            System.out.println(seat);
        }
    }
}

