#
# @lc app=leetcode id=30 lang=python
#
# [30] Substring with Concatenation of All Words
#

# @lc code=start
from collections import Counter, defaultdict

class Solution(object):
    def findSubstring(self, s, words):
        if not s or not words:
            return []
        
        word_len = len(words[0])
        word_count_need = Counter(words)
        total_len = word_len * len(words)

        if len(s) < total_len:
            return []
        result = []
        for offset in range(word_len):
            left = offset 
            seen = defaultdict(int)
            used = 0

            for right in range(offset, len(s) - word_len + 1, word_len):
                word = s[right: right + word_len]

                if word in word_count_need:
                    seen[word] +=1
                    used +=1

                    while seen[word] > word_count_need[word]:
                        left_word = s[left:left + word_len]
                        seen[left_word] -=1
                        used -=1
                        left += word_len

                    if used == len(words):
                        result.append(left)
                        left_word = s[left: left + word_len]
                        seen[left_word] -=1
                        used -=1
                        left += word_len
                else:
                    seen.clear()
                    used = 0
                    left = right + word_len


