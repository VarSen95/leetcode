List<Transaction> transactions = List.of(
    new Transaction("T1", "M1", 500.0, "APPROVED"),
    new Transaction("T2", "M1", 200.0, "DECLINED"),
    new Transaction("T3", "M2", 800.0, "APPROVED"),
    new Transaction("T4", "M2", 150.0, "APPROVED"),
    new Transaction("T5", "M3", 1200.0, "DECLINED"),
    new Transaction("T6", "M3", 300.0, "APPROVED"),
    new Transaction("T7", "M1", 950.0, "APPROVED"),
    new Transaction("T8", "M2", 100.0, "REFUNDED")
);
```

**Exercise 1: filter + collect**
```
Get all APPROVED transactions → List
Get all transactions over 500 → List
Get APPROVED transactions over 200 → List
```

**Exercise 2: map**
```
Get all merchant IDs → List<String>
Get all amounts → List<Double>
Get all transaction IDs where status is DECLINED → List<String>
```

**Exercise 3: mapToDouble + math**
```
Total of all amounts → double
Average of approved amounts → double
Max transaction amount → double
Min approved transaction amount → double
```

**Exercise 4: sorted + limit**
```
Top 3 highest transactions → List
Bottom 2 lowest approved transactions → List
All transactions sorted by merchant then by amount → List
```

---

### Session 2: Collectors (45 min)

**Exercise 5: groupingBy + counting**
```
Count transactions per status → Map<String, Long>
Count transactions per merchant → Map<String, Long>
```

**Exercise 6: groupingBy + summingDouble**
```
Total revenue per merchant → Map<String, Double>
Total revenue per status → Map<String, Double>
```

**Exercise 7: groupingBy + averagingDouble**
```
Average amount per merchant → Map<String, Double>
Average amount per status → Map<String, Double>
```

**Exercise 8: groupingBy + mapping**
```
Transaction IDs per status → Map<String, List<String>>
Transaction IDs per merchant → Map<String, List<String>>
```

**Exercise 9: nested groupingBy**
```
Group by merchant, then by status → Map<String, Map<String, List<Transaction>>>
Group by merchant, then count by status → Map<String, Map<String, Long>>
```

**Exercise 10: toMap**
```
Transaction ID to amount → Map<String, Double>
Transaction ID to transaction → Map<String, Transaction>
Merchant to highest amount (handle duplicates) → Map<String, Double>
```

---

### Session 3: Complex Chains (30 min)

**Exercise 11: Multi-step problems**
```
1. Merchant with highest total revenue
   → groupBy + sum → entrySet stream → max → get key

2. Percentage of approved transactions
   → filter count / total count * 100

3. Top 2 merchants by number of transactions
   → groupBy + counting → entrySet stream → sort → limit 2

4. Average amount of top 3 highest transactions
   → sorted desc → limit 3 → mapToDouble → average

5. All merchants who have at least one declined transaction
   → filter declined → map to merchant → distinct → collect
```

---

### Session 4: Redo Without Looking (30 min)

Close all notes. Open blank file. Solve these from memory:
```
1. Total approved revenue
2. Count per status
3. Revenue per merchant
4. Top 3 highest approved transactions
5. Merchant with highest total revenue
6. Group by merchant, then by status
7. Average amount per merchant
8. All unique merchants with declined transactions
9. Transaction ID to amount map
10. Percentage of approved vs total