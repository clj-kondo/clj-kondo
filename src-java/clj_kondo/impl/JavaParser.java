package clj_kondo.impl;

import com.github.javaparser.ParseException;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.serialization.JavaParserJsonSerializer;

import javax.json.stream.JsonGenerator;
import javax.json.Json;
import javax.json.stream.JsonGeneratorFactory;

import java.util.Map;
import java.util.HashMap;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

public class JavaParser {

    static String serialize(Node node, boolean prettyPrint) {
        Map<String, ?> config = new HashMap<>();
        if (prettyPrint) {
            config.put(JsonGenerator.PRETTY_PRINTING, null);
        }
        JsonGeneratorFactory generatorFactory = Json.createGeneratorFactory(config);
        JavaParserJsonSerializer serializer = new JavaParserJsonSerializer();
        StringWriter jsonWriter = new StringWriter();
        try (JsonGenerator generator = generatorFactory.createGenerator(jsonWriter)) {
            serializer.serialize(node, generator);
        }
        return jsonWriter.toString();
    }

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
            System.out.println(serialize(unit, true));
        } catch (IOException e) {
            new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        File javaSource = new File("Foo.java");
        listClasses(javaSource);
    }
}
