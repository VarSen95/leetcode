package selfpractice.dayOne;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
public class WordCounter {
    private Map<String, Integer> counts;

    public WordCounter() {
        this.counts = new HashMap<>();
    }

    public void addSentence(String sentence){
        for(String word : sentence.split("\\s+")) {
            counts.put(word, counts.getOrDefault(word, 0) + 1);
        }
    }

    public int getCount(String word) {
        return counts.getOrDefault(word, 0);
    }

    public List<String> topN(int n){
        return this.counts.entrySet().stream()
        .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
        .limit(n)
        .map(entry -> entry.getKey())
        .collect(Collectors.toList());
    }

    public Set<String> wordsAbove(int threshold){
        return this.counts.entrySet().stream()
        .filter(entry -> entry.getValue() > threshold)
        .map(entry -> entry.getKey())
        .collect(Collectors.toSet());
    }

    public static void main(String[] args) {
        WordCounter wc = new WordCounter();
        wc.addSentence("the cat sat on the mat the cat");
        System.out.println(wc.getCount("the"));     // 3
        System.out.println(wc.getCount("cat"));     // 2
        System.out.println(wc.topN(2));             // [the, cat]
        System.out.println(wc.wordsAbove(2));  
    }
}
