# Formatter

Formatter is a Java formatter where the formatting style is described separately by rules written in a custom EBNF-like DSL. The project parses Java code into an AST using JavaParser, matches AST nodes against DSL rules, and builds formatted Java code from output templates.

The main idea of the project is to implement a formatter with rules that are both expressive and concise. To change the output style, you do not need to rewrite the formatter core; it is enough to change the set of rules.

## What the project can do

- formats individual `.java` files and directories containing Java files;
- supports a check mode that does not modify files;
- can be run through Gradle or as an installed local CLI;
- builds the AST of Java code using JavaParser;
- parses DSL rules using ANTLR;
- matches rules against the JavaParser AST and extracts the required node fields;
- generates formatted code using templates with:
  - string literals such as `"class"`, `"return"`, `";"`, and similar tokens;
  - spaces via `sp`;
  - line breaks via `nl`;
  - indentation level increase via `indent`;
  - indentation level decrease via `dedent`;
  - lists via `join(<Item>, separator)`;
  - conditional fragments via `ifpresent(Name, ...)`.
- preserves the original text of unsupported fragments, so the absence of a separate rule for a specific AST node does not break the code;
- checks the safety of the result before writing: the formatted text is parsed again, and the normalized AST is compared with the original one.

## Why this approach is more convenient than ordinary Java formatters

Many popular Java formatters solve the problem of style unification, but they either give the user almost no ability to describe custom formatting rules at the language-structure level, or make such rules so difficult to describe that it is often easier to hardcode a custom fixed formatter using the `openrewrite` framework.

In general, existing solutions have **3** main **problems**:

- **fixed or almost fixed style**;
- **too many scattered settings**;
- **difficulty of writing custom rules for structural formatting**.

Existing Java code formatters include **google-java-format**, **clang-format**, **Eclipse Java formatter**, **ast-grep**, and **Topiary**:

`google-java-format` works well for projects that need to strictly follow `Google Java Style`, but its formatting algorithm is intentionally non-configurable: this is done to preserve a single consistent style. `palantir-java-format` follows a similar approach: it is a modern 120-character-line formatter based on `google-java-format`. These tools are convenient when a team accepts a ready-made style, but inconvenient when the team needs to describe its own formatting rules.

`spring-javaformat` is aimed at `Spring-style` projects and integrates well with Maven, Gradle, Eclipse, and IntelliJ IDEA. However, its configuration is also limited to specific options: for example, you can switch the indentation style to spaces or specify `java-baseline=8`, but you cannot describe your own output rules for AST constructs.

`clang-format` provides many parameters through `.clang-format`, supports predefined styles, and uses YAML configuration. This is a powerful approach, but it is based on configuring a large set of options (to configure them properly, you need to read more than 70 pages of documentation), rather than writing separate rules of the form `pattern => format expression` for specific Java syntax constructs.

`Eclipse Java formatter` also allows users to create and edit formatting profiles. This is convenient for configuring style inside an IDE, but the user works with profiles and parameters, not with a compact DSL that directly describes the structure of an AST node and the order in which its parts should be printed.

`ast-grep` and `Topiary` are closer to the structural approach because they work around ASTs and rules. However, `ast-grep` has a complex rule language, which makes it difficult to build a working formatter, while `Topiary` is a universal formatter in the `Tree-sitter` ecosystem aimed at formatter authors for different languages; its Java formatter is still at a rather early stage and supports very little.

The main difference of `Formatter` is that formatting is defined declaratively. A rule describes:

- which JavaParser AST node should be matched;
- which fields of this node should be extracted;
- which nested rules should be applied;
- in what order the final Java code should be assembled;
- where spaces, line breaks, and indentation should be placed.

**Formatter is convenient because**:

- formatting rules are separated from the program code;
- the style can be evolved gradually;
- unsupported Java code fragments are preserved through a fallback mechanism;
- rules read like a description of Java code structure;
- nested formatting is expressed naturally through references to other rules;
- lists and optional elements are described directly in the DSL;
- the same engine can be used with different rule sets.

**Conclusion**: Formatter can be used not only as an automatic formatting tool, but also as a platform for quickly describing, checking, and evolving custom Java formatting rules. Instead of changing the formatter's Java code, it is enough to change a DSL rule. This makes the approach flexible, extensible, and easier to understand when experimenting with different formatting styles.

**Links to the reviewed alternatives**:

- [google-java-format](https://github.com/google/google-java-format);
- [palantir-java-format](https://github.com/palantir/palantir-java-format);
- [spring-javaformat](https://github.com/spring-io/spring-javaformat);
- [clang-format](https://clang.llvm.org/docs/ClangFormat.html);
- [Eclipse Java formatter](https://help.eclipse.org/latest/topic/org.eclipse.jdt.doc.user/reference/preferences/java/codestyle/ref-preferences-formatter.htm);
- [ast-grep](https://ast-grep.github.io/);
- [Topiary](https://topiary.tweag.io/).

## Project status

The main implementation is complete: the project has a working CLI, a DSL rule parser, an internal rule model, AST matching, result rendering, fallback for unsupported fragments, and a safety check before writing.

The base rule set is located in `DefaultFormatterFactory` (it can be customized using the guide in [HowToWriteRules.md](HowToWriteRules.md)). It covers the main Java file formatting pipeline: `package`, `import`, `class`, `interface`, methods, blocks, `if/else`, `for`, `return`, and expression statements.

## Requirements

- JDK 25;
- Git;
- the Gradle Wrapper from the repository: `./gradlew` or `gradlew.bat`.

## Quick start

```bash
git clone https://github.com/Shopin-Igor/Formatter.git
cd Formatter
./gradlew test
```

## Running through Gradle

Check which files would be formatted:

```bash
./gradlew run --args="--check path/to/file-or-dir"
```

Format files in place:

```bash
./gradlew run --args="--write path/to/file-or-dir"
```

Show details for files skipped by the safety check:

```bash
./gradlew run --args="--write --explain-skips path/to/file-or-dir"
```

`--explain-skips` is useful for debugging rules. A regular `skipped ...` message reports the reason for skipping, while this flag makes the formatter additionally show the first line where the normalized AST after formatting differs from the original AST. The output shows:

- the number of the first differing line in the normalized AST;
- how this line looked in the original tree;
- how it looked after formatting.

This helps quickly understand which rule changed not only spaces and line breaks, but also the structure of the Java code. In this case, the file remains unchanged.

You can pass several files or directories:

```bash
./gradlew run --args="--check src/main/java src/test/java"
```

When traversing directories, the formatter skips service directories: `.git`, `.gradle`, `build`, `target`, and `out`.

## Installing a local CLI

Build a local executable script:

```bash
./gradlew installDist
```

After that, the following script will be available on Linux/macOS:

```bash
./build/install/core_of_my_project/bin/core_of_my_project
```

Run examples:

```bash
./build/install/core_of_my_project/bin/core_of_my_project --check /home/igor2/IdeaProjects/spring-hw-08-Shopin-Igor
./build/install/core_of_my_project/bin/core_of_my_project --write /home/igor2/IdeaProjects/spring-hw-08-Shopin-Igor
./build/install/core_of_my_project/bin/core_of_my_project --write --explain-skips /home/igor2/IdeaProjects/spring-hw-08-Shopin-Igor
```

On Windows, the equivalent script is located in the same directory and has the `.bat` extension.

## CLI options

| Option            | What it does                                                                                     |
|-------------------|--------------------------------------------------------------------------------------------------|
| `--write`         | formats `.java` files in the specified directory                                                 |
| `--check`         | shows the files that the formatter would change, but does not modify the files in the directory  |
| `--explain-skips` | adds diagnostic output for files skipped by the safety check                                     |
| `-h`, `--help`    | shows help                                                                                       |

`--check` is a preview mode. If the formatter writes `would format ...`, it means that the file differs from the formatting result, but the file itself has not been changed. A non-zero exit code appears only on real errors: for example, when the path is not found, the source file cannot be parsed, or an exception occurs during processing.

## How formatting works

In brief:

1. The CLI collects a list of `.java` files.
2. JavaParser builds an AST for each file.
3. `FormatterEngine` starts formatting from the root rule.
4. `PatternMatcher` searches for a suitable DSL rule for the current AST node.
5. The matched AST parts are saved in `Bindings`.
6. `TemplateRenderer` assembles text according to the output template.
7. For nested nodes, the formatter recursively repeats the same process.
8. If there is no separate rule for a fragment, its original tokens are passed to the result.
9. The final text is parsed again, and then the normalized AST is compared with the original one.

A detailed architecture description is provided in a separate file: [Contributing.md](Contributing.md).

## How to write rules

**A detailed algorithm for writing rules, choosing JavaParser node names, and finding their properties is provided in a separate guide: [HowToWriteRules.md](HowToWriteRules.md).**

***Briefly:***

A rule consists of two parts: a pattern for the JavaParser AST and an output template.

A real example of a rule for `return`:

```ebnf
<Statement> ::= ReturnStmt(expression?=<Expression>)
  => "return" ifpresent(Expression, sp <Expression>) ";";
```

What happens here:

- `ReturnStmt(...)` is matched against the JavaParser `ReturnStmt` node;
- `expression?=<Expression>` extracts the optional expression after `return`;
- `"return"` and `";"` are printed as regular text;
- `ifpresent(Expression, sp <Expression>)` adds a space and the expression only when the expression actually exists.

For example, the rule correctly prints both variants:

```java
return;
return value;
```

Another example is a block of statements:

```ebnf
<Statement> ::= BlockStmt(statements=[<Statement>*])
  => "{" nl indent join(<Statement>, nl) nl dedent "}";
```

Here, `join(<Statement>, nl)` prints a list of nested statements separated by line breaks, while `indent`/`dedent` control the indentation level.

## Project structure

```text
src/main/antlr/ebnf.g4                          # DSL grammar
src/main/java/org/example/Main.java             # CLI entry point
src/main/java/org/example/JavaFormatterCli.java # CLI argument and file handling
src/main/java/org/example/ebnfFormatter/dsl     # building the rule model from the DSL parse tree
src/main/java/org/example/ebnfFormatter/match   # matching rules against the JavaParser AST
src/main/java/org/example/ebnfFormatter/model   # internal model of rules and formatting expressions
src/main/java/org/example/ebnfFormatter/render  # rendering the final Java code
src/main/java/org/example/ebnfFormatter/runtime # building the formatter engine, registry, and default rules
src/test/java                                   # tests
```

## Quality checks

Run tests:

```bash
./gradlew test
```

Full build:

```bash
./gradlew build
```

ANTLR classes are generated automatically by a Gradle task during the build.

## Related user documentation

- The guide for writing DSL rules is located in [HowToWriteRules.md](HowToWriteRules.md).
- The detailed architecture description is provided in [Contributing.md](Contributing.md).