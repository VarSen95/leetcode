package selfpractice.dayOne;

import java.util.ArrayList;
import java.util.List;
public class Employee implements Printable, Identifiable{
    private String id;
    private String name;
    private String department;

    public Employee(String id, String name, String department) {
        this.id = id;
        this.name = name;
        this.department = department;
    }

    @Override
    public String format(){
        return String.format("Employee[id=%s, name=%s, dept=%s]", this.id, this.name, this.department);
    }

    @Override
    public String getId(){
        return this.id;
    }

    public static void main(String[] args){
        List<Printable> entities = new ArrayList<>();
        entities.add(new Employee("1", "Krishna", "Engg"));
        entities.add(new Contractor("2", "Google", 345.0));

        for(Printable entity: entities){
            System.out.println(entity.format());
        }
    }
}
