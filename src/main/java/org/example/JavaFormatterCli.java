package org.example;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import org.example.ebnfFormatter.runtime.DefaultFormatterFactory;
import org.example.ebnfFormatter.runtime.FormatterEngine;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class JavaFormatterCli {
    private static final Set<String> EXCLUDED_DIRECTORY_NAMES = Set.of(
            ".git",
            ".gradle",
            "build",
            "target",
            "out"
    );

    private final FormatterEngine formatterEngine;
    private final JavaParser javaParser;
    private final PrintStream out;
    private final PrintStream err;

    public JavaFormatterCli(PrintStream out, PrintStream err) {
        this(
                DefaultFormatterFactory.createEngine(),
                new JavaParser(new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_25)),
                out,
                err
        );
    }

    JavaFormatterCli(
            FormatterEngine formatterEngine,
            JavaParser javaParser,
            PrintStream out,
            PrintStream err
    ) {
        this.formatterEngine = formatterEngine;
        this.javaParser = javaParser;
        this.out = out;
        this.err = err;
    }

    public int run(String[] args) {
        CliOptions options;
        try {
            options = parseOptions(args);
        } catch (IllegalArgumentException e) {
            err.println(e.getMessage());
            printUsage(err);
            return 1;
        }

        if (options.help()) {
            printUsage(out);
            return 0;
        }

        List<Path> files;
        try {
            files = collectJavaFiles(options.roots());
        } catch (IOException | IllegalArgumentException e) {
            err.println(e.getMessage());
            return 1;
        }

        Summary summary = new Summary();
        for (Path file : files) {
            handleFile(file, options.mode(), options.explainSkips(), summary);
        }

        out.printf(
                "Scanned %d Java file(s): %d changed, %d unchanged, %d skipped, %d failed.%n",
                summary.scanned,
                summary.changed,
                summary.unchanged,
                summary.skipped,
                summary.failed
        );

        if (options.mode() == Mode.CHECK && summary.changed > 0) {
            out.println("Run with --write to update files.");
        }

        if (summary.failed > 0) {
            return 1;
        }
        return 0;
    }

    private CliOptions parseOptions(String[] args) {
        if (args.length == 0) {
            throw new IllegalArgumentException("Expected --write or --check.");
        }

        Mode mode = null;
        boolean explainSkips = false;
        List<Path> roots = new ArrayList<>();

        for (String arg : args) {
            switch (arg) {
                case "-h", "--help" -> {
                    return CliOptions.forHelp();
                }
                case "--write" -> mode = parseMode(mode, Mode.WRITE);
                case "--check" -> mode = parseMode(mode, Mode.CHECK);
                case "--explain-skips" -> explainSkips = true;
                default -> {
                    if (arg.startsWith("-")) {
                        throw new IllegalArgumentException("Unknown option: " + arg);
                    }
                    roots.add(Path.of(arg));
                }
            }
        }

        if (mode == null) {
            throw new IllegalArgumentException("Expected --write or --check.");
        }
        if (roots.isEmpty()) {
            throw new IllegalArgumentException("Expected at least one .java file or directory.");
        }

        return new CliOptions(mode, List.copyOf(roots), false, explainSkips);
    }

    private Mode parseMode(Mode existing, Mode next) {
        if (existing != null && existing != next) {
            throw new IllegalArgumentException("Use only one mode: --write or --check.");
        }
        return next;
    }

    private List<Path> collectJavaFiles(List<Path> roots) throws IOException {
        LinkedHashSet<Path> files = new LinkedHashSet<>();

        for (Path root : roots) {
            Path normalizedRoot = root.toAbsolutePath().normalize();
            if (!Files.exists(normalizedRoot)) {
                throw new IllegalArgumentException("Path does not exist: " + root);
            }

            if (Files.isRegularFile(normalizedRoot)) {
                if (!isJavaFile(normalizedRoot)) {
                    throw new IllegalArgumentException("Not a .java file: " + root);
                }
                files.add(normalizedRoot);
                continue;
            }

            if (!Files.isDirectory(normalizedRoot)) {
                throw new IllegalArgumentException("Not a regular file or directory: " + root);
            }

            Files.walkFileTree(normalizedRoot, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (!dir.equals(normalizedRoot) && isExcludedDirectory(dir)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (attrs.isRegularFile() && isJavaFile(file)) {
                        files.add(file.toAbsolutePath().normalize());
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }

        List<Path> sorted = new ArrayList<>(files);
        sorted.sort(Comparator.comparing(Path::toString));
        return sorted;
    }

    private boolean isExcludedDirectory(Path dir) {
        Path name = dir.getFileName();
        return name != null && EXCLUDED_DIRECTORY_NAMES.contains(name.toString());
    }

    private boolean isJavaFile(Path path) {
        Path name = path.getFileName();
        return name != null && name.toString().endsWith(".java");
    }

    private void handleFile(Path file, Mode mode, boolean explainSkips, Summary summary) {
        summary.scanned++;
        try {
            String original = Files.readString(file, StandardCharsets.UTF_8);
            CompilationUnit originalAst = parseCompilationUnit(original, file, "original source");
            String formatted = formatSafely(originalAst, file);
            formatted = preserveOriginalLineEndings(formatted, original);
            CompilationUnit formattedAst = parseFormattedCompilationUnit(formatted, file);
            assertAstPreserved(originalAst, formattedAst);

            if (formatted.equals(original)) {
                summary.unchanged++;
                return;
            }

            summary.changed++;
            if (mode == Mode.WRITE) {
                Files.writeString(file, formatted, StandardCharsets.UTF_8);
                out.println("formatted " + file);
            } else {
                out.println("would format " + file);
            }
        } catch (UnsafeFormatException e) {
            summary.skipped++;
            out.println("skipped " + file + ": " + e.getMessage());
            if (explainSkips && e.details() != null) {
                out.println(e.details());
            }
        } catch (RuntimeException | IOException e) {
            summary.failed++;
            err.println("failed " + file + ": " + e.getMessage());
        }
    }

    private String formatSafely(CompilationUnit originalAst, Path file) {
        try {
            return formatterEngine.format(originalAst, "CompilationUnit");
        } catch (RuntimeException e) {
            throw new UnsafeFormatException("formatter cannot render this file: " + e.getMessage(), e);
        }
    }

    private CompilationUnit parseCompilationUnit(String source, Path file, String phase) {
        ParseResult<CompilationUnit> result = javaParser.parse(source);
        if (result.getResult().isEmpty() || !result.getProblems().isEmpty()) {
            throw new IllegalArgumentException(
                    "Cannot parse " + phase + " for " + file + ": " + result.getProblems()
            );
        }
        return result.getResult().get();
    }

    private CompilationUnit parseFormattedCompilationUnit(String source, Path file) {
        ParseResult<CompilationUnit> result = javaParser.parse(source);
        if (result.getResult().isEmpty() || !result.getProblems().isEmpty()) {
            throw new UnsafeFormatException("formatted source does not parse: " + result.getProblems());
        }
        return result.getResult().get();
    }

    private void assertAstPreserved(CompilationUnit originalAst, CompilationUnit formattedAst) {
        String original = normalizeLineEndings(originalAst.toString());
        String formatted = normalizeLineEndings(formattedAst.toString());
        if (!original.equals(formatted)) {
            throw new UnsafeFormatException(
                    "formatted AST differs from original AST; file was left unchanged.",
                    firstAstDifference(original, formatted)
            );
        }
    }

    private String normalizeLineEndings(String text) {  // в Windows перевод на новую строку == "\r\n", а я вставляю nl == '\n' (в Linux формате), соответственно нужна нормализация
        return text.replace("\r\n", "\n").replace("\r", "\n");
    }


    private String firstAstDifference(String original, String formatted) {
        String[] originalLines = original.split("\\R", -1);
        String[] formattedLines = formatted.split("\\R", -1);
        int lineCount = Math.min(originalLines.length, formattedLines.length);

        for (int i = 0; i < lineCount; i++) {
            if (!originalLines[i].equals(formattedLines[i])) {
                return "  first different normalized AST line " + (i + 1) + ":" + System.lineSeparator()
                        + "    original : " + abbreviate(originalLines[i]) + System.lineSeparator()
                        + "    formatted: " + abbreviate(formattedLines[i]);
            }
        }

        return "  normalized AST line count differs: original=" + originalLines.length
                + ", formatted=" + formattedLines.length;
    }

    private String abbreviate(String value) {
        if (value.length() <= 180) {
            return value;
        }
        return value.substring(0, 177) + "...";
    }

    private String preserveOriginalLineEndings(String formatted, String original) {
        String lineEnding = original.contains("\r\n") ? "\r\n" : "\n";
        String result = "\n".equals(lineEnding) ? formatted : formatted.replace("\n", lineEnding);

        if (endsWithLineTerminator(original) && !endsWithLineTerminator(result)) {
            return result + lineEnding;
        }
        return result;
    }

    private boolean endsWithLineTerminator(String text) {
        return text.endsWith("\n") || text.endsWith("\r");
    }

    private void printUsage(PrintStream stream) {
        stream.println("""
                Usage:
                  ./gradlew run --args="--write <file-or-dir>"
                  ./gradlew run --args="--check <file-or-dir>"

                You can pass multiple files or directories after --write/--check.

                Options:
                  --write    Format .java files in place.
                  --check          Report files that would change without writing them.
                  --explain-skips  Show the first normalized AST difference for skipped files.
                  -h, --help       Show this help.
                """);
    }

    private static final class UnsafeFormatException extends RuntimeException {
        private final String details;

        private UnsafeFormatException(String message) {
            super(message);
            this.details = null;
        }

        private UnsafeFormatException(String message, Throwable cause) {
            super(message, cause);
            this.details = null;
        }

        private UnsafeFormatException(String message, String details) {
            super(message);
            this.details = details;
        }

        private String details() {
            return details;
        }
    }

    private enum Mode {
        WRITE,
        CHECK
    }

    private record CliOptions(Mode mode, List<Path> roots, boolean help, boolean explainSkips) {
        static CliOptions forHelp() {
            return new CliOptions(null, List.of(), true, false);
        }
    }

    private static final class Summary {
        private int scanned;
        private int changed;
        private int unchanged;
        private int skipped;
        private int failed;
    }
}
