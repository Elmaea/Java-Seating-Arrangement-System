import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class ExamReader {
    public static ArrayList<Exam> readExamsFromCSV(String filePath) {
        ArrayList<Exam> exams = new ArrayList<>();
        
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            br.readLine(); // Skip header line
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                
                if (values.length == 5) {
                    String examDate = values[0].trim();
                    String subject = values[1].trim();
                    String subjectCode = values[2].trim();
                    String department = values[3].trim();
                    String year = values[4].trim();

                    Exam exam = new Exam(examDate, subject, subjectCode, department, year);
                    exams.add(exam);
                } else {
                    System.out.println("Skipping invalid line: " + line);
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
        }

        return exams;
    }
}
