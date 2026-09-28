public class StudentProfile {

    private String studentId;
    private String studentName;
    private double quizScore;

    private static int studentCount = 0;

    public StudentProfile(String studentId, String studentName, double quizScore) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.quizScore = quizScore;
        studentCount++;
    }

    public String getStudentId() {
        return studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public double getQuizScore() {
        return quizScore;
    }

    public void setQuizScore(double quizScore) {
        if (quizScore >= 0 && quizScore <= 100) {
            this.quizScore = quizScore;
            System.out.println("Quiz score updated successfully.");
        } else {
            System.out.println("Invalid quiz score. Score must be between 0 and 100.");
        }
    }

    public void displayProfile() {
        System.out.println("Student ID: " + studentId);
        System.out.println("Student Name: " + studentName);
        System.out.println("Quiz Score: " + quizScore);
        System.out.println();
    }

    public static int getStudentCount() {
        return studentCount;
    }
}