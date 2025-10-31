import org.example.Main;
public class FormatterTest {

    public static void main(String[] args) {
        test(
                "class Main {int a;}",
                "class Main {\n\tint a;\n}\n"
        );

        test(
                "class Main {record A(int x) {};} ",
                "class Main {\n\trecord A(int x) {\n\t};\n}\n"
        );

        test(
                "class Main {void f() {System.out.println(1);}}",
                "class Main {\n\tvoid f() {\n\t\tSystem.out.println(1);\n\t}\n}\n"
        );

        test(
                "class Main {public static void main() {System.out.println(\"Hello\");}}",
                "class Main {\n\tpublic static void main() {\n\t\tSystem.out.println(\"Hello\");\n\t}\n}\n"
        );

        test(
                "class Main {record A(int x) {}; void g() {};}",
                "class Main {\n\trecord A(int x) {\n\t};\n\tvoid g() {\n\t};\n}\n"
        );

        test(
                "class Main {if (true) {System.out.println(1);} System.out.println(2);}",
                "class Main {\n\tif (true) {\n\t\tSystem.out.println(1);\n\t}\n\tSystem.out.println(2);\n}\n"
        );

        test(
                "class Main {void a(){if (true){if (false){}}}}",
                "class Main {\n\tvoid a() {\n\t\tif (true) {\n\t\t\tif (false) {\n\t\t\t}\n\t\t}\n\t}\n}\n"
        );

        test(
                "class Main {record A(int x) {};void f(){System.out.println(1);}}",
                "class Main {\n\trecord A(int x) {\n\t};\n\tvoid f() {\n\t\tSystem.out.println(1);\n\t}\n}\n"
        );

        test(
                "class Main {void f() {System.out.println(1);}void g(){System.out.println(2);}}",
                "class Main {\n\tvoid f() {\n\t\tSystem.out.println(1);\n\t}\n\tvoid g() {\n\t\tSystem.out.println(2);\n\t}\n}\n"
        );

        test(
                "class Main {void f(){System.out.println(new Object(){public String toString(){return \"x\";}});}}",
                "class Main {\n\tvoid f() {\n\t\tSystem.out.println(new Object() {\n\t\t\tpublic String toString() {\n\t\t\t\treturn \"x\";\n\t\t\t}\n\t\t});\n\t}\n}\n"
        );
    }

    private static void test(String input, String expected) {
        String actual = Main.formatter_2(input); // вызываем твою функцию
        if (!actual.equals(expected)) {
            System.out.println("FAIL:");
            System.out.println("input:");
            System.out.println(input);
            System.out.println("expected:");
            System.out.println(showWhitespace(expected));
            System.out.println("actual:");
            System.out.println(showWhitespace(actual));
            System.out.println();
        } else {
            System.out.println("OK");
        }
    }

    private static String showWhitespace(String s) {
        return s
                .replace("\t", "\\t\t")
                .replace(" ", "·")
                .replace("\n", "\\n\n");
    }
}
