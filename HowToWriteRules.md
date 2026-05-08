# Как писать правила, если не знаешь имя JavaParser AST-ноды

Эта инструкция помогает перейти от Java-кода к DSL-правилу форматтера, когда непонятно, как JavaParser называет нужную конструкцию и какие поля у нее есть.

## Короткий алгоритм

1. Берём минимальный Java-код с нужной конструкцией.
2. Распарсиваем его JavaParser-ом.
3. Печатаем дерево AST: имена классов нод и их `.toString()`.
4. Находим ноду, которая соответствует нужной конструкции.
5. Печатаем properties этой ноды.
6. Используем имя класса ноды в DSL pattern.
7. Используем имена properties как имена полей в DSL.

## Минимальный код для поиска имени ноды

Вставляем такой код:

```java
JavaParser parser = new JavaParser(
        new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_25)
);

String code = """
        class Sample {
            void run() {
                while (i < limit) {
                    i++;
                }
            }
        }
        """;

CompilationUnit compilationUnit = parser.parse(code)
        .getResult()
        .orElseThrow();

compilationUnit.findAll(Node.class).forEach(node -> {
    String source = node.toString().replace('\n', ' ');
    System.out.println(node.getClass().getSimpleName() + " -> " + source);
});
```

В выводе будет:

```text
CompilationUnit -> class Sample {      void run() {         while (i < limit) {             i++;         }     } } 
ClassOrInterfaceDeclaration -> class Sample {      void run() {         while (i < limit) {             i++;         }     } }
SimpleName -> Sample
MethodDeclaration -> void run() {     while (i < limit) {         i++;     } }
SimpleName -> run
VoidType -> void
BlockStmt -> {     while (i < limit) {         i++;     } }
WhileStmt -> while (i < limit) {     i++; }
BinaryExpr -> i < limit
NameExpr -> i
SimpleName -> i
NameExpr -> limit
SimpleName -> limit
BlockStmt -> {     i++; }
ExpressionStmt -> i++;
UnaryExpr -> i++
NameExpr -> i
SimpleName -> i
```

Значит для `while` имя JavaParser-ноды:

```text
WhileStmt
```

## Как посмотреть поля ноды

Когда имя ноды найдено, нужно понять, какие поля можно использовать в DSL. Дописываем к предыдущему коду (пример для WhileStmt):

```java
Node node = compilationUnit.findFirst(WhileStmt.class).orElseThrow();

for (PropertyMetaModel property : node.getMetaModel().getAllPropertyMetaModels()) {
    String name = property.getName();

    if (List.of(
            "metaModel",            // описание модели самой ноды
            "range",                // позиция в исходном файле
            "tokenRange",           // диапазон токенов
            "parsed",               //  получена ли нода парсером или создана програмно
            "comment",              // комментарий, прикрепленный к ноде
            "orphanComments",       // комментарий, который был рядом с нодой
            "allContainedComments", // все комментарии в поддереве ноды
            "childNodes",           // все дети ноды без имен полей
            "parentNode"            // родительская нода
    ).contains(name)) {
        continue;
    }

    Object value = property.getValue(node);
    System.out.println(name + " -> " + value);
}
```

Вывод (поля для `WhileStmt`):

```text
body -> {
    i++;
}
condition -> i < limit
```


## Как, зная имя ноды, написать DSL-правило

Если JavaParser class называется:

```text
WhileStmt
```

то в DSL pattern пишем:

```ebnf
WhileStmt(...)
```

Если property называется:

```text
condition
body
```

то в DSL пишем:

```ebnf
condition=<Expression>, body=<Statement>
```

Полный пример:

```ebnf
<Statement> ::= WhileStmt(condition=<Expression>, body=<Statement>)
  => "while" sp "(" <Expression> ")" sp <Statement>;
```

## Списки и optional-поля

Если property содержит список, в DSL используется:

```ebnf
statements=[<Statement>*]
```

Пример:

```ebnf
<Statement> ::= BlockStmt(statements=[<Statement>*])
  => "{" nl indent join(<Statement>, nl) nl dedent "}";
```

Если property может быть, а может отсутствовать, используется `?` у имени поля:

```ebnf
elseStmt?=<ElseStmt>
```

Пример:

```ebnf
<Statement> ::= IfStmt(condition=<Expression>, thenStmt=<ThenStmt>, elseStmt?=<ElseStmt>)
  => "if" sp "(" <Expression> ")" <ThenStmt>
     ifpresent(ElseStmt, nl "else" <ElseStmt>);
```

## Важно: если два поля могут содержать разные значения, не нужно placeholder называть одинаково.

Плохо:

```ebnf
<BinaryExpr> ::= BinaryExpr(left=<Expression>, right=<Expression>)
  => <Expression> sp "+" sp <Expression>;
```

Такой rule может конфликтовать в bindings, потому что левое и правое выражения разные.

Лучше:

```ebnf
<BinaryExpr> ::= BinaryExpr(left=<LeftExpr>, right=<RightExpr>)
  => <LeftExpr> sp "+" sp <RightExpr>;
```

## Если часть конструкции не Node

Не все properties JavaParser являются нодами. Например:
```text
Modifier.keyword    -> FINAL
PrimitiveType.type  -> INT
BinaryExpr.operator -> PLUS
AssignExpr.operator -> PLUS
UnaryExpr.operator  -> PREFIX_INCREMENT
```

Чтобы понять, что именно лежит в таком поле, нужно печатать не только значение, но и Java-класс этого значения (т. к. одно и то же текстовое значение может означать разные вещи в разных классах JavaParser).
Например:
```text
BinaryExpr.operator -> PLUS
AssignExpr.operator -> PLUS
```
Одинаковое название PLUS в разных классах
```text
com.github.javaparser.ast.expr.BinaryExpr.Operator
com.github.javaparser.ast.expr.AssignExpr.Operator
```
Имеют разный смысл
```text
a + b   // BinaryExpr.Operator.PLUS
a += b  // AssignExpr.Operator.PLUS
```

Для определения названий "не Node конструкций" подойдёт такой набор функций
```java
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.metamodel.PropertyMetaModel;

import java.util.List;
import java.util.Optional;

private static void printNonNodeProperties(Node node) {
    for (PropertyMetaModel property : node.getMetaModel().getAllPropertyMetaModels()) {
        String name = property.getName();

        if (List.of(
                "metaModel",            // описание модели самой ноды
                "range",                // позиция в исходном файле
                "tokenRange",           // диапазон токенов
                "parsed",               //  получена ли нода парсером или создана програмно
                "comment",              // комментарий, прикрепленный к ноде
                "orphanComments",       // комментарий, который был рядом с нодой
                "allContainedComments", // все комментарии в поддереве ноды
                "childNodes",           // все дети ноды без имен полей
                "parentNode"            // родительская нода
        ).contains(name)) {
            continue;
        }

        printNonNodeValue(name, property.getValue(node));
    }
}

private static void printNonNodeValue(String name, Object value) {
    switch (value) {
        case null -> {
            return;
        }
        case Optional<?> optionalValue -> {
            optionalValue.ifPresent(innerValue -> printNonNodeValue(name, innerValue));
            return;
        }
        case NodeList<?> values -> {
            for (int i = 1; i <= values.size(); i++) {
                printNonNodeValue(name + "[" + i + "]", values.get(i - 1));
            }
            return;
        }
        case Node node -> {
            return;
        }
        default -> {
        }
    }

    Class<?> valueClass = value instanceof Enum<?> enumValue
            ? enumValue.getDeclaringClass()
            : value.getClass();
    
    String className = valueClass.getCanonicalName();
    // className может не быть у анонимных, лямбда, local классов
    if (className == null) {
        className = valueClass.getName().replace('$', '.');
    }

    String enumConstant = value instanceof Enum<?> enumValue
            ? "." + enumValue.name()
            : "";

    System.out.println(name + " -> " + className + enumConstant + " = " + value);
}
```

Пример использования:
```java
String code = "public class Sample{public int one(){if (a == b) return 1;}}";

CompilationUnit compilationUnit = StaticJavaParser.parse(code);
BinaryExpr binaryExpr = compilationUnit.findFirst(BinaryExpr.class).orElseThrow();
printNonNodeProperties(binaryExpr);
```

Вывод будет таким:

```text
operator -> com.github.javaparser.ast.expr.BinaryExpr.Operator.EQUALS = EQUALS
```

### Пример написания правила для таких конструкций
Определяем название:
```java
import com.github.javaparser.ast.Modifier;

String code = "final class Empty{}";

CompilationUnit compilationUnit = StaticJavaParser.parse(code);
Modifier modifier = compilationUnit.findFirst(Modifier.class).orElseThrow();
printNonNodeProperties(modifier);                                               // keyword -> com.github.javaparser.ast.Modifier.Keyword.FINAL = FINAL
```

В DSL это все равно можно вынести в placeholder:
```ebnf
<Modifier> ::= Modifier(keyword=<Keyword>)                 // будет писать keyword на отдельной строке
  => nl indent <Keyword> dedent nl;                        //            с 1 табом отступа
```
# Итог
## Быстрый шаблон для нового правила

1. Найти в AST:
```text
SomeNode
```

2. Посмотреть properties этой ноды:
```text
left     -> a
operator -> PLUS
right    -> b
```

3. Для каждого property понимаем, что там лежит:
```java
public class Example {
    void example(Node node) {
        for (PropertyMetaModel property : node.getMetaModel().getAllPropertyMetaModels()) {
            String name = property.getName();

            if (List.of(
                    "metaModel", "range", "tokenRange", "parsed",
                    "comment", "orphanComments", "allContainedComments",
                    "childNodes", "parentNode"
            ).contains(name)) {
                continue;
            }
            
            Object value = property.getValue(node);

            if (value == null) {
                System.out.println(name + " -> null");
            } else if (value instanceof Node childNode) {
                System.out.println(name
                        + " -> Node "
                        + childNode.getClass().getSimpleName()
                        + " = "
                        + childNode);
            } else {
                Class<?> valueClass = value instanceof Enum<?> enumValue
                        ? enumValue.getDeclaringClass()
                        : value.getClass();

                String className = valueClass.getCanonicalName();
                if (className == null) {
                    className = valueClass.getName().replace('$', '.');
                }

                String enumConstant = value instanceof Enum<?> enumValue
                        ? "." + enumValue.name()
                        : "";

                System.out.println(name
                        + " -> "
                        + className
                        + enumConstant
                        + " = "
                        + value);
            }
        }
    }
}
```
4. Например для BinaryExpr(a + b) будет:
```text
left -> Node NameExpr = a
operator -> com.github.javaparser.ast.expr.BinaryExpr.Operator.PLUS = PLUS
right -> Node NameExpr = b
```
5. Если property содержит Node, выносим его в placeholder. Если property содержит не Node, например enum для operator/keyword/type, его тоже можно вынести в placeholder.
6. Пример правила:
```text
<SomeRule> ::= SomeNode(left=<LeftExpr>, operator=<Operator>, right=<RightExpr>)
  => <LeftExpr> sp <Operator> sp <RightExpr>;
```