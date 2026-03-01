package ui;

public class TestImage {
    public static void main(String[] args) {
        // use a dummy instance to call getClass()
        TestImage t = new TestImage();
        System.out.println(t.getClass().getResource("clipboard_person.png"));
    }
}
