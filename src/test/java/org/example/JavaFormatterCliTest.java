package org.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class JavaFormatterCliTest {
    @TempDir
    private Path tempDir;

    @Test
    void writeFormatsJavaFilesInPlace() throws Exception {
        Path file = tempDir.resolve("Sample.java");
        Files.writeString(file, "class Sample{int one(){return 1;}}\n", StandardCharsets.UTF_8);

        CliRun run = runCli("--write", tempDir.toString());

        assertThat(run.exitCode()).isEqualTo(0);
        assertThat(Files.readString(file, StandardCharsets.UTF_8)).isEqualTo("""
                class Sample {
                    int one() {
                        return 1;
                    }
                }
                """);
        assertThat(run.out()).contains("formatted " + file.toAbsolutePath().normalize());
    }

    @Test
    void checkReportsChangesWithoutWriting() throws Exception {
        Path file = tempDir.resolve("Sample.java");
        String original = "class Sample{int one(){return 1;}}\n";
        Files.writeString(file, original, StandardCharsets.UTF_8);

        CliRun run = runCli("--check", file.toString());

        assertThat(run.exitCode()).isEqualTo(0);
        assertThat(Files.readString(file, StandardCharsets.UTF_8)).isEqualTo(original);
        assertThat(run.out()).contains("would format " + file.toAbsolutePath().normalize());
    }

    @Test
    void writePreservesGenericMethodTypeParameters() throws Exception {
        Path file = tempDir.resolve("GenericMethod.java");
        Files.writeString(file, "class Sample{<T> T run(T value){return value;}}\n", StandardCharsets.UTF_8);

        CliRun run = runCli("--write", file.toString());

        assertThat(run.exitCode()).isEqualTo(0);
        assertThat(Files.readString(file, StandardCharsets.UTF_8)).isEqualTo("""
                class Sample {
                    <T> T run(T value) {
                        return value;
                    }
                }
                """);
        assertThat(run.out()).contains("formatted " + file.toAbsolutePath().normalize());
        assertThat(run.err()).isEmpty();
    }

    @Test
    void writePreservesGenericTypeDeclarationParameters() throws Exception {
        Path classFile = tempDir.resolve("Box.java");
        Path interfaceFile = tempDir.resolve("Repository.java");
        Files.writeString(classFile, "class Box<T>{T value;}\n", StandardCharsets.UTF_8);
        Files.writeString(interfaceFile, "interface Repository<T>{T get();}\n", StandardCharsets.UTF_8);

        CliRun run = runCli("--write", tempDir.toString());

        assertThat(run.exitCode()).isEqualTo(0);
        assertThat(Files.readString(classFile, StandardCharsets.UTF_8)).isEqualTo("""
                class Box<T> {
                    T value;
                }
                """);
        assertThat(Files.readString(interfaceFile, StandardCharsets.UTF_8)).isEqualTo("""
                interface Repository<T> {
                    T get();
                }
                """);
        assertThat(run.out()).contains("formatted " + classFile.toAbsolutePath().normalize());
        assertThat(run.out()).contains("formatted " + interfaceFile.toAbsolutePath().normalize());
        assertThat(run.err()).isEmpty();
    }

    @Test
    void writePreservesAnnotationsAndInterfaces() throws Exception {
        Path file = tempDir.resolve("Repository.java");
        Files.writeString(file, "@Deprecated interface Repository{default int one(){return 1;}}\n", StandardCharsets.UTF_8);

        CliRun run = runCli("--write", file.toString());

        assertThat(run.exitCode()).isEqualTo(0);
        assertThat(Files.readString(file, StandardCharsets.UTF_8)).isEqualTo("""
                @Deprecated
                interface Repository {
                    default int one() {
                        return 1;
                    }
                }
                """);
    }

    @Test
    void writePreservesStaticImports() throws Exception {
        Path file = tempDir.resolve("StaticImport.java");
        Files.writeString(file, "import static org.assertj.core.api.Assertions.assertThat;class StaticImport{void run(){assertThat(1).isOne();}}\n", StandardCharsets.UTF_8);

        CliRun run = runCli("--write", file.toString());

        assertThat(run.exitCode()).isEqualTo(0);
        assertThat(Files.readString(file, StandardCharsets.UTF_8)).startsWith("""
                import static org.assertj.core.api.Assertions.assertThat;

                class StaticImport {
                """);
    }

    @Test
    void writeDoesNotAccumulateIndentInsideRawMembers() throws Exception {
        Path file = tempDir.resolve("RawMembers.java");
        String source = """
                @Deprecated
                class RawMembers {
                    @Id
                    @GeneratedValue(strategy = GenerationType.IDENTITY)
                    private Long id;

                    RawMembers(String message) {
                        super(message);
                    }
                }
                """;
        Files.writeString(file, source, StandardCharsets.UTF_8);

        CliRun run = runCli("--write", file.toString());

        assertThat(run.exitCode()).isEqualTo(0);
        assertThat(Files.readString(file, StandardCharsets.UTF_8)).isEqualTo(source);
    }

    @Test
    void writeDoesNotInsertSpaceBeforeEmptyReturnSemicolon() throws Exception {
        Path file = tempDir.resolve("VoidReturn.java");
        Files.writeString(file, "class VoidReturn{void run(){return;}}\n", StandardCharsets.UTF_8);

        CliRun run = runCli("--write", file.toString());

        assertThat(run.exitCode()).isEqualTo(0);
        assertThat(Files.readString(file, StandardCharsets.UTF_8)).isEqualTo("""
                class VoidReturn {
                    void run() {
                        return;
                    }
                }
                """);
    }

    @Test
    void writePreservesLineCommentsBetweenStatements() throws Exception {
        Path file = tempDir.resolve("StatementComments.java");
        Files.writeString(file, "class StatementComments{void run(){first(); // keep this\nsecond();}}\n", StandardCharsets.UTF_8);

        CliRun run = runCli("--write", file.toString());

        assertThat(run.exitCode()).isEqualTo(0);
        assertThat(Files.readString(file, StandardCharsets.UTF_8)).contains("""
                        first(); // keep this
                        second();
                """);
        assertThat(run.out()).contains("formatted " + file.toAbsolutePath().normalize());
    }

    @Test
    void writePreservesLineCommentsBeforeFirstStatement() throws Exception {
        Path file = tempDir.resolve("LeadingStatementComment.java");
        Files.writeString(file, "class LeadingStatementComment{void run(){// keep first\nfirst();}}\n", StandardCharsets.UTF_8);

        CliRun run = runCli("--write", file.toString());

        assertThat(run.exitCode()).isEqualTo(0);
        assertThat(Files.readString(file, StandardCharsets.UTF_8)).contains("""
                        // keep first
                        first();
                """);
        assertThat(run.out()).contains("formatted " + file.toAbsolutePath().normalize());
    }

    @Test
    void writePreservesBlockCommentBetweenReturnAndExpression() throws Exception {
        Path file = tempDir.resolve("ReturnComment.java");
        Files.writeString(file, "class ReturnComment{Object run(){return /* keep return */ value();}}\n", StandardCharsets.UTF_8);

        CliRun run = runCli("--write", file.toString());

        assertThat(run.exitCode()).isEqualTo(0);
        assertThat(Files.readString(file, StandardCharsets.UTF_8)).contains("return /* keep return */ value();");
        assertThat(run.out()).contains("formatted " + file.toAbsolutePath().normalize());
    }

    @Test
    void writePreservesBlockCommentAfterOpeningParen() throws Exception {
        Path file = tempDir.resolve("ConditionComment.java");
        Files.writeString(file, "class ConditionComment{void run(){if(/* keep condition */ ready()){return;}}}\n", StandardCharsets.UTF_8);

        CliRun run = runCli("--write", file.toString());

        assertThat(run.exitCode()).isEqualTo(0);
        assertThat(Files.readString(file, StandardCharsets.UTF_8)).contains("if (/* keep condition */ ready())");
        assertThat(run.out()).contains("formatted " + file.toAbsolutePath().normalize());
    }

    @Test
    void writePreservesFileLevelComments() throws Exception {
        Path file = tempDir.resolve("FileComments.java");
        Files.writeString(file, "// file header\nclass FileComments{int run(){return 1;}}\n// file tail\n", StandardCharsets.UTF_8);

        CliRun run = runCli("--write", file.toString());

        assertThat(run.exitCode()).isEqualTo(0);
        assertThat(Files.readString(file, StandardCharsets.UTF_8)).isEqualTo("""
                // file header
                class FileComments {
                    int run() {
                        return 1;
                    }
                }
                // file tail
                """);
        assertThat(run.out()).contains("formatted " + file.toAbsolutePath().normalize());
    }

    @Test
    void writeSkipsCommonBuildDirectories() throws Exception {
        Path sourceFile = tempDir.resolve("src/Sample.java");
        Path buildFile = tempDir.resolve("build/Broken.java");
        Files.createDirectories(sourceFile.getParent());
        Files.createDirectories(buildFile.getParent());
        Files.writeString(sourceFile, "class Sample{int one(){return 1;}}\n", StandardCharsets.UTF_8);
        Files.writeString(buildFile, "not java", StandardCharsets.UTF_8);

        CliRun run = runCli("--write", tempDir.toString());

        assertThat(run.exitCode()).isEqualTo(0);
        assertThat(run.err()).isEmpty();
        assertThat(Files.readString(sourceFile, StandardCharsets.UTF_8)).contains("class Sample {");
        assertThat(Files.readString(buildFile, StandardCharsets.UTF_8)).isEqualTo("not java");
    }

    private CliRun runCli(String... args) {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream errBytes = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(outBytes, true, StandardCharsets.UTF_8);
        PrintStream err = new PrintStream(errBytes, true, StandardCharsets.UTF_8);

        int exitCode = new JavaFormatterCli(out, err).run(args);

        return new CliRun(
                exitCode,
                outBytes.toString(StandardCharsets.UTF_8),
                errBytes.toString(StandardCharsets.UTF_8)
        );
    }

    private record CliRun(int exitCode, String out, String err) {
    }
}
