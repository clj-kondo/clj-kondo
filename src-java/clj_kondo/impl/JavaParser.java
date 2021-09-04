package clj_kondo.impl;

import com.github.javaparser.ParseException;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.File;
import java.io.IOException;

public class JavaParser {

    public static void listClasses(File javaSource) {
        try {
            com.github.javaparser.JavaParser parser = new com.github.javaparser.JavaParser();
            var unit = parser.parse(javaSource).getResult().get();
            new VoidVisitorAdapter<Object>() {
                @Override
                public void visit(ClassOrInterfaceDeclaration n, Object arg) {
                    super.visit(n, arg);
                    System.out.println(" * " + n.getName());
                }
            }.visit(unit, null);
            System.out.println(); // empty line
        } catch (IOException e) {
            new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        File javaSource = new File("Foo.java");
        listClasses(javaSource);
    }
}
