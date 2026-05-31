package org.example;

public class Main {
    public static void main(String[] args) {
        int exitCode = new JavaFormatterCli(System.out, System.err).run(args);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }
}
