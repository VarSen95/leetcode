package selfpractice.dayOne;

public class Company implements AccountHolder{
    private String name;
    private String registrationNumber;

    public Company(String name, String registrationNumber) {
        this.name = name;
        this.registrationNumber = registrationNumber;
    }

    @Override
    public String getName() {
       return this.name;
    }

    @Override
    public String getAccountNumber() {
        return this.registrationNumber;
    }

    
}
