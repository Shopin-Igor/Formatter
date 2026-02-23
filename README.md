## Ниже представлена документация о том, как выглядит форматирование Java code правилами на EBNF подобном языке

---
### Новые термины форматтера
- **sp** - один пробел
- **nl** - перенос на новую строку
- **indent** - увеличить уровень отступа на 1 таб
- **dedent** - уменьшить уровень отступа на 1 таб

---
### Кратко о написанном ниже:
- фигурируют 3 конструкции: **IfStmt, MethodDeclaration, ForStmt**
- описаны крайние случаи, есть по **3** примера с разным форматированием на каждое правило

---
### Definition of done (для всех правил):
- описанное на EBNF правило (причём только то, которое я написал, чтобы было нагляднее) для форматирования применяется на Java исходник с нечётным номером N и получается Java исходник с номером N + 1  

---
## Конструкция IfStmt

### Правило
Правило для форматирования для элемента (IfStmt) AST дерева:
```ebnf
<IfStmt> ::=
    "if" sp '(' <Expr> ')' sp <IfBody>
    [ sp "else" sp <ElseBody> ];

<IfBody> ::= 
    <Block> | ( nl indent <Stmt> dedent );

<Block> ::= 
    '{' nl indent (<Stmt>)* nl dedent '}';

<ElseBody> ::= 
    <IfStmt> | <IfBody>;
```

### Пример 1
Java исходник 1
```java
public class AST {
    public int sum(int a, int b) {
        if(a == b){return 2 * a;}
        return a + b;
    }
}
```
Java исходник 2
```java
public class AST {
    public int sum(int a, int b) {
        if (a == b) {
            return 2 * a;
        }
        return a + b;
    }
}
```

### Пример 2
Java исходник 3
```java
public class AST {
    public int sum(int a, int b) {
        if(a == b)return 2 * a;
        else return a + b;
    }
}
```
Java исходник 4
```java
public class AST {
    public int sum(int a, int b) {
        if (a == b) 
            return 2 * a;
        else 
            return a + b;
    }
}
```

### Пример 3
Java исходник 5
```java
public class AST {
    public int sum(int a, int b) {
        if(a == b)return 2 * a;
        else if((a&2)==2) return a + b;
        else return b;
    }
}
```
Java исходник 6
```java
public class AST {
    public int sum(int a, int b) {
        if (a == b)
            return 2 * a;
        else if ((a&2)==2) 
            return a + b;
        else 
            return b;
    }
}
```

---
## Конструкция MethodDeclaration

### Правило
Правило для форматирования для элемента (MethodDeclaration) AST дерева:
```ebnf
<MethodDeclaration> ::=
    <Modifiers>? <Type> sp <Name> '(' <ParamList>? ')' sp <MethodBodyOrSemi>;
    
<Modifiers> ::=
    <Annotation>* <AccessModifier>? <MethodModifier>*;
    
<AccessModifier> ::= 
    "public" | "protected" | "private" ;

<MethodModifier> ::=
    "abstract" | "static" | "final" | "synchronized" | "native";

<MethodBodyOrSemi> ::= 
    <Block> | ';';

<ParamList> ::=
    <Param> ( ',' sp <Param> )*;

<Block> ::= 
    '{' nl indent (<Stmt>)* nl dedent '}';
```

### Пример 1
Java исходник 1
```java
public class AST {
    public int sum(int a,int b){if(a==b){return 2*a;}return a+b;}
}
```
Java исходник 2
```java
public class AST {
    public int sum(int a, int b) {
        if(a==b){return 2*a;}return a+b;
    }
}
```

### Пример 2
Java исходник 3

```java
public class AST {
    public int sum(Parameter a,Parameter b,Parameter c,Parameter d) {}
}
```
Java исходник 4
```java
public class AST {
    public int sum(Parameter a, Parameter b, Parameter c, Parameter d) {
        
    }
}
```

### Пример 3
Java исходник 5
```java
abstract class AST {
    public abstract int sum(Input 
                                        input);
}
```
Java исходник 6
```java
abstract class AST {
    public abstract int sum(Input input);
}
```

---
## Конструкция ForStmt

### Правило
Правило для форматирования для элемента (ForStmt) AST дерева:
```ebnf
<ForStmt> ::=
    "for" sp "(" <ForInit>? ";" sp <Condition>? ";" sp <ForUpdate>? ")" sp <ForBody>;

<ForInit>   ::= 
    <ExprList>;
    
<Condition> ::= 
    <Expr>;                         // bool Expr

<ForUpdate> ::= 
    <ExprList>;

<ExprList>  
    ::= <Expr> ( "," sp <Expr> )*;

<ForBody> ::= 
    <Block> | ( nl indent <Stmt> dedent );
```

### Пример 1
Java исходник 1
```java
public class AST {
    public int sum(int a, int b) {
        int sm = 0;
        for (int i=0;i<5;++i){sm += i;}
    }
}
```
Java исходник 2
```java
public class AST {
    public int sum(int a, int b) {
        int sm = 0;
        for (int i=0; i<5; ++i) {
            sm += i;
        }
    }
}
```

### Пример 2
Java исходник 3
```java
public class AST {
    public int sum(int a, int b) {
        int sm = 0;
        for (int i=0;i<5;++i)sm += i;
    }
}
```
Java исходник 4
```java
public class AST {
    public int sum(int a, int b) {
        int sm = 0;
        for (int i=0; i<5; ++i)
            sm += i;
    }
}
```

### Пример 3
Java исходник 5
```java
public class AST {
    public int sum(int a, int b) {
        for (;;)make();
    }
}
```
Java исходник 6
```java
public class AST {
    public int sum(int a, int b) {
        for (;;)
            make();
    }
}
```
