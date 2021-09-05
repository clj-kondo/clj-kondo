package clj_kondo.impl;

import com.github.javaparser.ParseException;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.serialization.JavaParserJsonSerializer;
import com.github.javaparser.metamodel.BaseNodeMetaModel;
import com.github.javaparser.metamodel.JavaParserMetaModel;
import com.github.javaparser.metamodel.PropertyMetaModel;

import javax.json.stream.JsonGenerator;
import javax.json.Json;
import javax.json.stream.JsonGeneratorFactory;

import java.util.Map;
import java.util.HashMap;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

// can we use https://github.com/javaparser/javaparser/blob/master/javaparser-core-serialization/src/main/java/com/github/javaparser/serialization/JavaParserJsonSerializer.java
// as inspiration to write our own EDN serialization?
// or probably we can just use the voidvisitoradapter

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

    static void serializeEdn(Node node) {
        System.out.println(node.getClass());
        BaseNodeMetaModel nodeMetaModel = JavaParserMetaModel.getNodeMetaModel(node.getClass()).orElseThrow(() -> new IllegalStateException("Unknown Node: " + node.getClass()));
        for (PropertyMetaModel propertyMetaModel : nodeMetaModel.getAllPropertyMetaModels()) {
            String name = propertyMetaModel.getName();
            System.out.println("Name: " + name);
            Object value = propertyMetaModel.getValue(node);
            if (value != null) {
                if (propertyMetaModel.isNodeList()) {
                    NodeList<Node> list = (NodeList<Node>) value;
                    for (Node n : list) {
                        serializeEdn(n);
                    }
                } else if (propertyMetaModel.isNode()) {
                    serializeEdn((Node) value);
                }
            }
        }

    }

    public static void listClasses(File javaSource) {
        try {
            com.github.javaparser.JavaParser parser = new com.github.javaparser.JavaParser();
            var unit = parser.parse(javaSource).getResult().get();
            // https://www.javadoc.io/doc/com.github.javaparser/javaparser-core/3.3.1/com/github/javaparser/ast/visitor/VoidVisitorAdapter.html
            new VoidVisitorAdapter<Object>() {
                @Override
                public void visit(ClassOrInterfaceDeclaration n, Object arg) {
                    super.visit(n, arg);
                    System.out.println(" * " + n.getName());
                }
                @Override
                public void visit(MethodDeclaration n, Object arg) {
                    super.visit(n, arg);
                    System.out.println(" * " + n.getName());
                }
            }.visit(unit, null);
            System.out.println(); // empty line
            // System.out.println(serialize(unit, true));
            serializeEdn(unit);
        } catch (IOException e) {
            new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        File javaSource = new File("Foo.java");
        listClasses(javaSource);
    }
}
