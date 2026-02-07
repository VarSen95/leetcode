# Day 1: Java Fundamentals Exercises

## Environment (JDK 8)

Use the local scripts to switch to Java 8 and run a file:

```bash
source java/self-practice/env.sh
java -version
java/self-practice/run.sh BankAccount.java
```
## Time: ~1.5 hours | No AI help! Google syntax if stuck, but solve yourself.

---

## Exercise 1: Classes & Constructors (15 min)

Create a `BankAccount` class with:
- Private fields: `String accountId`, `String ownerName`, `double balance`
- A constructor that takes all three fields
- Getters for all fields
- A `deposit(double amount)` method that throws `IllegalArgumentException` if amount <= 0
- A `withdraw(double amount)` method that throws `IllegalArgumentException` if amount <= 0 or amount > balance
- A `toString()` method

Write a `main` method that creates 2 accounts, deposits, withdraws, and prints them.

```java
// Start here:
public class BankAccount {
    // YOUR CODE
}
```

---

## Exercise 2: Interfaces & Implementation (15 min)

Create an interface `Printable` with a method `String format()`.

Create an interface `Identifiable` with a method `String getId()`.

Create a class `Employee` that implements BOTH interfaces:
- Fields: `String id`, `String name`, `String department`
- `getId()` returns the id
- `format()` returns "Employee[id=xxx, name=xxx, dept=xxx]"

Create a class `Contractor` that implements BOTH interfaces:
- Fields: `String id`, `String company`, `double hourlyRate`
- `format()` returns "Contractor[id=xxx, company=xxx, rate=xxx]"

Write a main method that puts both in a `List<Printable>` and loops through calling `format()` on each.

```java
// Start here:
public interface Printable {
    // YOUR CODE
}
```

---

## Exercise 3: Abstract Classes & Inheritance (20 min)

Create an abstract class `Shape`:
- Has a `String color` field
- Constructor that takes color
- Abstract method `double area()`
- Abstract method `double perimeter()`
- Concrete method `String describe()` that returns "A {color} shape with area {area}"

Create `Circle` (has `radius`), `Rectangle` (has `width`, `height`), and `Triangle` (has `sideA`, `sideB`, `sideC` and uses Heron's formula for area).

Write a main method that creates a `List<Shape>`, adds one of each, and prints `describe()` for all.

```java
// Start here:
public abstract class Shape {
    // YOUR CODE
}
```

**Bonus:** Add `Comparable<Shape>` to Shape, comparing by area.

---

## Exercise 4: HashMap Workout (20 min)

Write a class `WordCounter` with the following methods:

```java
public class WordCounter {
    private Map<String, Integer> counts;

    // Constructor
    
    // Adds all words from the sentence (split by space, lowercase)
    public void addSentence(String sentence) { }

    // Returns count for a specific word (0 if not found)
    public int getCount(String word) { }

    // Returns the top N most frequent words as a List
    // Hint: you'll need to sort the entries
    public List<String> topN(int n) { }

    // Returns all words that appear more than 'threshold' times
    public Set<String> wordsAbove(int threshold) { }
}
```

Test with:
```java
WordCounter wc = new WordCounter();
wc.addSentence("the cat sat on the mat the cat");
System.out.println(wc.getCount("the"));     // 3
System.out.println(wc.getCount("cat"));     // 2
System.out.println(wc.topN(2));             // [the, cat]
System.out.println(wc.wordsAbove(2));       // [the]
```

---

## Exercise 5: Collections Drill (20 min)

Write a class `StudentRegistry`:

```java
public class StudentRegistry {
    // Use appropriate collection types

    // Add a student with a name and grade (A, B, C, D, F)
    public void addStudent(String name, String grade) { }

    // Get all students with a specific grade, sorted alphabetically
    public List<String> getStudentsByGrade(String grade) { }

    // Get average grade as GPA (A=4, B=3, C=2, D=1, F=0)
    public double getAverageGpa() { }

    // Remove a student by name, return true if found
    public boolean removeStudent(String name) { }

    // Get total number of students
    public int size() { }

    // Get grade distribution as a Map (e.g., {A=3, B=5, C=2})
    public Map<String, Integer> getGradeDistribution() { }
}
```

Think about: What data structure(s) do you need? A single `Map<String, String>`? A `Map<String, List<String>>`? Both?

---

## Exercise 6: Putting It All Together — Mini Banking System (remaining time)

This is a warm-up for your actual assessment. Build a tiny version:

1. Create an interface `AccountHolder` with method `String getName()`
2. Create class `Person` implementing `AccountHolder` (has name, age)
3. Create class `Company` implementing `AccountHolder` (has name, registrationNumber)
4. Create class `Account` with:
   - `long accountNumber`
   - `AccountHolder holder`
   - `double balance`
   - `deposit()` and `withdraw()` methods
5. Create class `Bank` with:
   - `Map<Long, Account> accounts`
   - `createAccount(AccountHolder holder, double initialDeposit)` — generates account number
   - `getAccount(long accountNumber)` — returns Account or throws exception
   - `transfer(long fromAccount, long toAccount, double amount)`

Test it:
```java
Bank bank = new Bank();
Person alice = new Person("Alice", 30);
Company acme = new Company("Acme Corp", "REG-12345");

long a1 = bank.createAccount(alice, 1000);
long a2 = bank.createAccount(acme, 5000);

bank.transfer(a2, a1, 500);
// alice should have 1500, acme should have 4500
```

---

## Self-Check After Day 1

Rate yourself 1-5 on each. Anything below 3, revisit tomorrow:

- [ ] Writing a class with private fields, constructor, getters
- [ ] Interfaces: declaring and implementing
- [ ] Abstract classes: abstract vs concrete methods
- [ ] HashMap: put, get, getOrDefault, iterating entries
- [ ] ArrayList: creating, adding, sorting
- [ ] for-each loops over collections
- [ ] Throwing and catching exceptions
- [ ] String methods: split, toLowerCase, trim

---

*Remember: The goal isn't perfect code. It's getting your fingers and brain used to Java again. Ugly working code > elegant code you googled.*
