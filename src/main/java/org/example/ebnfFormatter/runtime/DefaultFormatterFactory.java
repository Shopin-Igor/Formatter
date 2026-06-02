package org.example.ebnfFormatter.runtime;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.example.ebnfFormatter.dsl.RuleAstBuilder;
import org.example.ebnfFormatter.match.PatternMatcher;
import org.example.ebnfFormatter.model.RuleDef;
import org.example.ebnfFormatter.render.TemplateRenderer;
import org.example.ebnfLexer;
import org.example.ebnfParser;

import java.util.List;

public final class DefaultFormatterFactory {
    private static final String DEFAULT_RULES = """
            <CompilationUnit> ::= CompilationUnit(packageDeclaration?=<PackageDeclaration>, imports=[<ImportDeclaration>*], types=[<TypeDeclaration>*])
              => ifpresent(PackageDeclaration, <PackageDeclaration> nl nl)
                 ifpresent(ImportDeclaration, join(<ImportDeclaration>, nl) nl nl)
                 join(<TypeDeclaration>, nl nl);

            <PackageDeclaration> ::= PackageDeclaration(name=<Name>)
              => "package" sp <Name> ";";

            <ImportDeclaration> ::= ImportDeclaration(static=true, asterisk=true, name=<Name>)
              => "import" sp "static" sp <Name> ".*" ";";

            <ImportDeclaration> ::= ImportDeclaration(static=true, asterisk=false, name=<Name>)
              => "import" sp "static" sp <Name> ";";

            <ImportDeclaration> ::= ImportDeclaration(static=false, asterisk=true, name=<Name>)
              => "import" sp <Name> ".*" ";";

            <ImportDeclaration> ::= ImportDeclaration(static=false, asterisk=false, name=<Name>)
              => "import" sp <Name> ";";

            <ExtendedType> ::= <ClassOrInterfaceType>
              => <ClassOrInterfaceType>;

            <ImplementedType> ::= <ClassOrInterfaceType>
              => <ClassOrInterfaceType>;

            <TypeDeclaration> ::= <ClassOrInterfaceDeclaration>
              => <ClassOrInterfaceDeclaration>;

            <ClassOrInterfaceDeclaration> ::= ClassOrInterfaceDeclaration(annotations=[<AnnotationExpr>*], modifiers=[<Modifier>*], interface=true, name=<SimpleName>, typeParameters=[<TypeParameter>*], extendedTypes=[<ExtendedType>*], members=[<BodyDeclaration>*])
              => ifpresent(AnnotationExpr, join(<AnnotationExpr>, nl) nl)
                 ifpresent(Modifier, join(<Modifier>, "")) "interface" sp <SimpleName>
                 ifpresent(TypeParameter, "<" join(<TypeParameter>, ", ") ">")
                 ifpresent(ExtendedType, sp "extends" sp join(<ExtendedType>, ", "))
                 sp "{"
                 ifpresent(BodyDeclaration, nl indent join(<BodyDeclaration>, nl nl) nl dedent)
                 "}";

            <ClassOrInterfaceDeclaration> ::= ClassOrInterfaceDeclaration(annotations=[<AnnotationExpr>*], modifiers=[<Modifier>*], interface=false, name=<SimpleName>, typeParameters=[<TypeParameter>*], extendedTypes=[<ExtendedType>*], implementedTypes=[<ImplementedType>*], members=[<BodyDeclaration>*])
              => ifpresent(AnnotationExpr, join(<AnnotationExpr>, nl) nl)
                 ifpresent(Modifier, join(<Modifier>, "")) "class" sp <SimpleName>
                 ifpresent(TypeParameter, "<" join(<TypeParameter>, ", ") ">")
                 ifpresent(ExtendedType, sp "extends" sp join(<ExtendedType>, ", "))
                 ifpresent(ImplementedType, sp "implements" sp join(<ImplementedType>, ", "))
                 sp "{"
                 ifpresent(BodyDeclaration, nl indent join(<BodyDeclaration>, nl nl) nl dedent)
                 "}";

            <BodyDeclaration> ::= <MethodDeclaration>
              => <MethodDeclaration>;

            <MethodDeclaration> ::= MethodDeclaration(annotations=[<AnnotationExpr>*], modifiers=[<Modifier>*], typeParameters=[<TypeParameter>*], type=<Type>, name=<SimpleName>, parameters=[<Parameter>*], thrownExceptions=[<ReferenceType>*], body=<Statement>)
              => ifpresent(AnnotationExpr, join(<AnnotationExpr>, nl) nl)
                 ifpresent(Modifier, join(<Modifier>, ""))
                 ifpresent(TypeParameter, "<" join(<TypeParameter>, ", ") ">" sp)
                 <Type> sp <SimpleName> "("
                 ifpresent(Parameter, join(<Parameter>, ", "))
                 ")" ifpresent(ReferenceType, sp "throws" sp join(<ReferenceType>, ", ")) sp <Statement>;

            <MethodDeclaration> ::= MethodDeclaration(annotations=[<AnnotationExpr>*], modifiers=[<Modifier>*], typeParameters=[<TypeParameter>*], type=<Type>, name=<SimpleName>, parameters=[<Parameter>*], thrownExceptions=[<ReferenceType>*])
              => ifpresent(AnnotationExpr, join(<AnnotationExpr>, nl) nl)
                 ifpresent(Modifier, join(<Modifier>, ""))
                 ifpresent(TypeParameter, "<" join(<TypeParameter>, ", ") ">" sp)
                 <Type> sp <SimpleName> "("
                 ifpresent(Parameter, join(<Parameter>, ", "))
                 ")" ifpresent(ReferenceType, sp "throws" sp join(<ReferenceType>, ", ")) ";";

            <Statement> ::= BlockStmt(statements=[<Statement>*])
              => "{" nl indent join(<Statement>, nl) nl dedent "}";

            <Statement> ::= IfStmt(condition=<CondExpr>, thenStmt=<ThenStmt>, elseStmt?=<ElseStmt>)
              => "if" sp "(" <CondExpr> ")" <ThenStmt>
                 ifpresent(ElseStmt, nl "else" <ElseStmt>);

            <ThenStmt> ::= BlockStmt(statements=[<Statement>*])
              => sp "{" nl indent join(<Statement>, nl) nl dedent "}";

            <ThenStmt> ::= ReturnStmt(expression?=<ThenExpr>)
              => nl indent "return" ifpresent(ThenExpr, sp <ThenExpr>) ";" dedent;

            <ThenStmt> ::= ExpressionStmt(expression=<ThenExpr>)
              => nl indent <ThenExpr> ";" dedent;

            <ThenStmt> ::= ForStmt(initialization=[<InitExpr>*], compare?=<CompareExpr>, update=[<UpdateExpr>*], body=<ForBody>)
              => nl indent "for" sp "("
                 ifpresent(InitExpr, join(<InitExpr>, ", "))
                 ";" ifpresent(CompareExpr, sp <CompareExpr>)
                 ";" ifpresent(UpdateExpr, sp join(<UpdateExpr>, ", "))
                 ")" <ForBody> dedent;

            <ElseStmt> ::= <NestedIf>
              => sp <NestedIf>;

            <NestedIf> ::= IfStmt(condition=<CondExpr>, thenStmt=<ThenStmt>, elseStmt?=<ElseStmt>)
              => "if" sp "(" <CondExpr> ")" <ThenStmt>
                 ifpresent(ElseStmt, nl "else" <ElseStmt>);

            <ElseStmt> ::= BlockStmt(statements=[<Statement>*])
              => sp "{" nl indent join(<Statement>, nl) nl dedent "}";

            <ElseStmt> ::= ReturnStmt(expression?=<ElseExpr>)
              => nl indent "return" ifpresent(ElseExpr, sp <ElseExpr>) ";" dedent;

            <ElseStmt> ::= ExpressionStmt(expression=<ElseExpr>)
              => nl indent <ElseExpr> ";" dedent;

            <ElseStmt> ::= ForStmt(initialization=[<InitExpr>*], compare?=<CompareExpr>, update=[<UpdateExpr>*], body=<ForBody>)
              => nl indent "for" sp "("
                 ifpresent(InitExpr, join(<InitExpr>, ", "))
                 ";" ifpresent(CompareExpr, sp <CompareExpr>)
                 ";" ifpresent(UpdateExpr, sp join(<UpdateExpr>, ", "))
                 ")" <ForBody> dedent;

            <Statement> ::= ForStmt(initialization=[<InitExpr>*], compare?=<CompareExpr>, update=[<UpdateExpr>*], body=<ForBody>)
              => "for" sp "("
                 ifpresent(InitExpr, join(<InitExpr>, ", "))
                 ";" ifpresent(CompareExpr, sp <CompareExpr>)
                 ";" ifpresent(UpdateExpr, sp join(<UpdateExpr>, ", "))
                 ")" <ForBody>;

            <ForBody> ::= BlockStmt(statements=[<Statement>*])
              => sp "{" nl indent join(<Statement>, nl) nl dedent "}";

            <ForBody> ::= ExpressionStmt(expression=<ForExpr>)
              => nl indent <ForExpr> ";" dedent;

            <ForBody> ::= ReturnStmt(expression?=<ForExpr>)
              => nl indent "return" ifpresent(ForExpr, sp <ForExpr>) ";" dedent;

            <Statement> ::= ReturnStmt(expression?=<Expression>)
              => "return" ifpresent(Expression, sp <Expression>) ";";

            <Statement> ::= ExpressionStmt(expression=<Expression>)
              => <Expression> ";";
            """;

    private DefaultFormatterFactory() {
    }

    public static FormatterEngine createEngine() {
        RuleRegistry ruleRegistry = new RuleRegistry();
        ruleRegistry.registerAll(parseRules(DEFAULT_RULES));

        TypeRegistryUniversal typeRegistry = new TypeRegistryUniversal();
        PatternMatcher patternMatcher = new PatternMatcher(typeRegistry, ruleRegistry);
        TemplateRenderer templateRenderer = new TemplateRenderer();

        return new FormatterEngine(ruleRegistry, patternMatcher, templateRenderer);
    }

    public static List<RuleDef> parseRules(String rules) {
        ebnfLexer lexer = new ebnfLexer(CharStreams.fromString(rules));
        ebnfParser parser = new ebnfParser(new CommonTokenStream(lexer));
        ThrowingErrorListener errorListener = new ThrowingErrorListener();

        lexer.removeErrorListeners();
        parser.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        parser.addErrorListener(errorListener);

        ebnfParser.RulelistContext ctx = parser.rulelist();
        return new RuleAstBuilder().buildRules(ctx);
    }

    private static final class ThrowingErrorListener extends BaseErrorListener {
        @Override
        public void syntaxError(
                Recognizer<?, ?> recognizer,
                Object offendingSymbol,
                int line,
                int charPositionInLine,
                String msg,
                RecognitionException e
        ) {
            throw new IllegalArgumentException(
                    "Rules syntax error at " + line + ":" + charPositionInLine + ": " + msg,
                    e
            );
        }
    }
}
