package org.example;

import java.io.*;
import java.util.Scanner;

public class Main {
    public  static String formatter_1(String s) {
        return s;
    }

    public static String formatter_2(String s) {
        StringBuilder result = new StringBuilder();
        int cnt_tabs = 0;
        boolean atLineStart = true;
        boolean noLettersWere = true;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            String cc = Character.toString(c);


            if (atLineStart && !cc.equals("\n") && !cc.equals("\r") && !cc.equals("\t")) {
                if (!cc.equals("}")) {
                    result.append("\t".repeat(Math.max(0, cnt_tabs)));
                } else {
                    result.append("\t".repeat(Math.max(0, cnt_tabs - 1)));
                }
                atLineStart = false;
            }

            if (cc.equals("{")) {
                boolean spaceAlready =
                        result.length() > 0 &&
                                result.charAt(result.length() - 1) == ' ';

                if (!spaceAlready && result.length() > 0 &&
                        result.charAt(result.length() - 1) != '\t' &&
                        result.charAt(result.length() - 1) != '\n') {
                    result.append(' ');
                }

                result.append('{');
                result.append('\n');
                cnt_tabs++;
                atLineStart = true;
                noLettersWere = true;

            } else if (cc.equals("}")) {
                cnt_tabs = Math.max(0, cnt_tabs - 1);
                result.append('}');

                if (i == s.length() - 1 || (!Character.toString(s.charAt(i + 1)).equals(";") && !Character.toString(s.charAt(i + 1)).equals(")"))) {
                    result.append('\n');
                    atLineStart = true;
                    noLettersWere = true;
                }

            } else if (cc.equals(";")) {
                result.append(';');
                result.append('\n');
                atLineStart = true;
                noLettersWere = true;

            } else if (cc.equals("\r") || cc.equals("\t") || cc.equals("\n") || (cc.equals(" ") && (noLettersWere || i == 0 || (s.charAt(i - 1) == ' ' || s.charAt(i - 1) == '\t' || s.charAt(i - 1) == '\r')))) {
            } else {
                noLettersWere = false;
                result.append(c);
            }
        }

        int len = result.length();
        return result.substring(0, len);
    }

    public  static String formatter_3(String s) {
        return s;
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        StringBuilder codeBuilder = new StringBuilder();

        try (BufferedReader br = new BufferedReader(new FileReader("input.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                codeBuilder
                        .append(line)
                        .append("\n");
            }
        } catch (IOException e) {
            codeBuilder.setLength(0);
            codeBuilder
                    .append("/* Ошибка чтения input.txt: ")
                    .append(e.getMessage())
                    .append(" */")
                    .append("\n");
        }

        String code = codeBuilder.toString();
        System.out.println("Введи тип форматтирования (1/2/3):");
        int type = sc.nextInt();
        String formattedResult;

        switch (type) {
            case 1:
                formattedResult = formatter_1(code);
                break;
            case 2:
                formattedResult = formatter_2(code);
                break;
            case 3:
                formattedResult = formatter_3(code);
                break;
            default:
                formattedResult = "Неверный формат";;
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter("output.txt"))) {
            bw.write(formattedResult);
        } catch (IOException e) {
            System.out.println("Ошибка записи в output.txt: " + e.getMessage());
        }
    }
}

