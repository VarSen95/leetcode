package selfpractice.dayOne;

import java.util.ArrayList;
import java.util.List;
public class Circle extends Shape{
    private double radius;

    public Circle(String color, double radius) {
        super(color);
        this.radius = radius;
    }

    @Override
    public double area() {
        return 3.14 * radius * radius;
    }

    @Override
    public double perimeter() {
        return 2 * 3.14 * radius;
    }

    public static void main(String[] args) {
        List<Shape> shapes = new ArrayList<>();
        Shape shape1 = new Circle("Blue", 5);
        Shape shape2 = new Rectangle("Green", 5, 6);
        Shape shape3 = new Triangle("green", 3, 5, 7);
        shapes.add(shape1);
        shapes.add(shape2);
        shapes.add(shape3);

        for(Shape shape : shapes) {
            System.out.println(shape.describe());
        }

        System.out.println(String.format("Comparing shape1 to shape 3: %d", shape1.compareTo(shape3)));

    }
}
