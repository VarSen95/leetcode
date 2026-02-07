package selfpractice.dayOne;

public class Contractor implements Identifiable, Printable{

    private String id;
    private String company;
    private double hourlyRate;

    public Contractor(String id, String company, double hourlyRate) {
        this.id = id;
        this.company = company;
        this.hourlyRate = hourlyRate;
    }
    @Override
    public String format() {
        return String.format("Contractor[id=%s, company=%s, rate=%s]", this.id, this.company, this.hourlyRate);
    }

    @Override
    public String getId() {
        return this.id;
    }    
}
