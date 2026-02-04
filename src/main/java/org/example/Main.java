package org.example;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Main {
    public static void main(String[] args) {
        String input = """
                public class AST {
                    public int sum(int a, int b) {
                        System.out.println(a);
                        System.out.println(b);
                        if (a == b)
                            return 2 * a;
                        if (a == 2) {
                            return 2;
                        }
                        if (b == 5)
                            return 5 * b;
                        return a + b;
                    }
                }
                """;

        JavaParser parser = new JavaParser();
        ParseResult<CompilationUnit> parseResult = parser.parse(input);

        CompilationUnit cu = parseResult.getResult().orElseThrow();

        String rule = "SELECT IfStmt Where IfStmt.thenStmt != BlockStmt";
        String[] ruleSplitToWords = rule.split(" +");

        switch (ruleSplitToWords[0] + " " + ruleSplitToWords[2]) {
            case "SELECT Where":
                ///

                String[] thirdWordInRequest = ruleSplitToWords[3].split("\\.");
                if (Objects.equals(ruleSplitToWords[1], thirdWordInRequest[0]) && thirdWordInRequest.length > 1) {
                    switch (ruleSplitToWords[1]) {
                        case "IfStmt":

                            switch (thirdWordInRequest[1]) {
                                case "thenStmt":

                                    if (!Objects.equals(ruleSplitToWords[4], "!=") ||
                                            !Objects.equals(ruleSplitToWords[5], "BlockStmt")) {
                                        IO.println("only IfStmt.thenStmt != BlockStmt work now");
                                        break;
                                    }

                                    List<Statement> listThenNotBlock = new ArrayList<>();

                                    Class<IfStmt> a = IfStmt.class;
                                    Class<BlockStmt> b = BlockStmt.class;

                                    cu.walk(a, ifStmt -> {
                                        Statement thenBlock = ifStmt.getThenStmt();
                                        if (!(b.isInstance(thenBlock))) {
                                            listThenNotBlock.add(thenBlock);
                                        }
                                    });


                                    IO.println(listThenNotBlock.size() == 2);


                                    break;
                                default:
                                    IO.println("1st or 3rd word in request is not correct");
                            }


                            break;
                        default:
                            IO.println("1st or 3rd word in request is not correct");
                    }
                } else {
                    IO.println("2nd word in request is not correct");
                }

                ///
                break;
            default:
                IO.println("1st or 3rd word in request is not correct");
        }
    }
}
