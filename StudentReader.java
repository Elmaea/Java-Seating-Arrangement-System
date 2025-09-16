import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class StudentReader {

    public static ArrayList<Student> readStudentsFromCSV(String filePath) {
        ArrayList<Student> students = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;

            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");

                if (values.length == 3) {
                    String id = values[0].trim();
                    String name = values[1].trim();
                    String course = values[2].trim();

                    Student student = new Student(id, name, course);
                    students.add(student);
                } else {
                    System.out.println("Skipping invalid line: " + line);
                }
            }

        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
        }

        return students;
    }
}
