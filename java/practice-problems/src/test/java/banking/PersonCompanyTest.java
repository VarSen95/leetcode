package banking;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PersonCompanyTest {

    @Test
    public void testPersonFields() {
        Person person = new Person("John", "Doe", 111);
        assertEquals("John", person.getFirstName());
        assertEquals("Doe", person.getLastName());
        assertEquals(111, person.getIdNumber());
    }

    @Test
    public void testCompanyFields() {
        Company company = new Company("Initech", 222);
        assertEquals("Initech", company.getCompanyName());
        assertEquals(222, company.getIdNumber());
    }
}
