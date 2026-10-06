package java25;

public record Circle(double radius) implements Shape {
    public double area() {
        return Math.PI * radius * radius;
    }

    public String describe() {
        return "circle " + radius;
    }
}
