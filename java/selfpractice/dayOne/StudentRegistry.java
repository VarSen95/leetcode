package selfpractice.dayOne;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

enum Grade {
    A(4.0), B(3.0), C(2.0), D(1.0), F(0.0);

    private double gpa;

    Grade(double gpa) {
        this.gpa = gpa;
    }

    public double getGpa() {
        return this.gpa;
    }
}

public class StudentRegistry {

    private Map<Grade, List<Student>> studentGradeMap = new HashMap<>();

    public StudentRegistry() {
    }

    public void addStudent(String name, String grade) {
        Student student = new Student(name, grade);
        if (isValidGrade(grade)) {
            this.studentGradeMap.computeIfAbsent(Grade.valueOf(grade), k -> new ArrayList<>()).add(student);
        } else {
            throw new IllegalArgumentException("Invalid grade");
        }
    }

    public List<String> getStudentsByGrade(String grade) {
        if (isValidGrade(grade)) {
            return studentGradeMap.getOrDefault(Grade.valueOf(grade), Collections.emptyList())
                    .stream()
                    .map(s -> s.getName())
                    .collect(Collectors.toList());
        } else {
            throw new IllegalArgumentException("Invalid grade");
        }
    }

    public double getAverageGpa() {
        int totalStudents = size();
        if(totalStudents == 0) {
            return 0.0;
        }

        double totalGpa = studentGradeMap.entrySet()
        .stream()
        .mapToDouble(e -> e.getKey().getGpa() * e.getValue().size())
        .sum();

        return totalGpa / totalStudents;

    }

    public boolean removeStudent(String name) {
        for (List<Student> students : studentGradeMap.values()) {
            if (students.removeIf(s -> s.getName().equals(name))) {
                return true;
            }
        }
        return false;
    }

    public int size() {
        return studentGradeMap.values().stream().mapToInt(List::size).sum();
    }

    public Map<String, Integer> getGradeDistribution() {
        return studentGradeMap.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().name(),
                        e -> e.getValue().size()));
    }

    private boolean isValidGrade(String grade) {

        try {
            Grade.valueOf(grade);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
