package selfpractice.dayOne;

public abstract class Shape implements Comparable<Shape>{
    private String color;

    public Shape(String color) {
        this.color = color;
    }

    public abstract double area();
    public abstract double perimeter();

    public String describe(){
        return String.format("A %s shape with area %f", this.color, area());
    }

    @Override
    public int compareTo(Shape other) {
        return Double.compare(this.area(), other.area());
    }

}
