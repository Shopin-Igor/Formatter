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

## Важно: если два поля могут содержать разные значения, не называй placeholder одинаково.

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

# Итог
## Быстрый шаблон для нового правила

1. Найти в AST:

```text
SomeNode
```

2. Посмотреть properties:

```text
left
operator
right
```

3. Написать правило:

```ebnf
<SomeRule> ::= SomeNode(left=<Left>, operator=<Operator>, right=<Right>)
  => <Left> sp <Operator> sp <Right>;
```