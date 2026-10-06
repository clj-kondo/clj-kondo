// Build corpus/java/java25.jar with corpus/java/java25/build.sh on JDK 25.
package java25;

public sealed interface Shape permits Circle {
    double area();
}
