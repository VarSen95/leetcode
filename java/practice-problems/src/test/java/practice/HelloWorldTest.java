package practice;

public class HelloWorldTest {
    public static void main(String[] args) {
        String expected = "Hello, world!";
        String actual = getHello();
        if (!expected.equals(actual)) {
            throw new AssertionError("Expected: " + expected + ", got: " + actual);
        }
        System.out.println("HelloWorldTest passed");
    }

    private static String getHello() {
        return "Hello, world!";
    }
}
