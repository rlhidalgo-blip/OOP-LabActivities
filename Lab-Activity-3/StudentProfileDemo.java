public class StudentProfileDemo {

    public static void main(String[] args) {

        StudentProfile student1 =
                new StudentProfile("20260001", "Juan Dela Cruz", 85);

        StudentProfile student2 =
                new StudentProfile("20260002", "Maria Santos", 90);

        System.out.println("=== INITIAL STUDENT PROFILES ===");
        student1.displayProfile();
        student2.displayProfile();

        System.out.println("=== VALID SCORE UPDATE ===");
        student1.setQuizScore(95);
        student1.displayProfile();

        System.out.println("=== INVALID SCORE UPDATE ===");
        System.out.println("Previous Score: " + student2.getQuizScore());

        student2.setQuizScore(120);

        System.out.println("Score After Invalid Update: "
                + student2.getQuizScore());

        System.out.println();
        System.out.println("Total Student Profiles: "
                + StudentProfile.getStudentCount());
    }
}