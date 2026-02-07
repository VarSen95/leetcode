package selfpractice.dayOne;

public class Triangle extends Shape{
    private double sideA;
    private double sideB;
    private double sideC;

    public Triangle(String color, double sideA, double sideB, double sideC) {
        super(color);
        this.sideA = sideA;
        this.sideB = sideB;
        this.sideC = sideC;
    }

    @Override
    public double area() {
        return sideA * sideB * sideC;
    }

    @Override
    public double perimeter() {
        return sideA + sideB + sideC;
    }

    
    
}
