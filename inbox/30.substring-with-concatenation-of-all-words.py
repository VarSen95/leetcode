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
        word_count = len(words)
        total_len = word_len * word_count

        if total_len > len(s):
            return []

        need = Counter(words)
        res = []


        # We try each possible starting offset modulo word_len
        for offset in range(word_len):
            left = offset
            seen = defaultdict(int)
            used = 0  # number of valid words currently in window
            print(seen)

            # Move right in steps of word_len (word by word)
            for right in range(offset, len(s) - word_len + 1, word_len):
                w = s[right:right + word_len]

                if w in need:
                    seen[w] += 1
                    used += 1

                    # Too many of word w -> shrink from left until valid
                    while seen[w] > need[w]:
                        left_word = s[left:left + word_len]
                        seen[left_word] -= 1
                        left += word_len
                        used -= 1

                    # If we have exactly word_count words, record start
                    if used == word_count:
                        res.append(left)

                        # Slide window forward by removing leftmost word
                        left_word = s[left:left + word_len]
                        seen[left_word] -= 1
                        left += word_len
                        used -= 1

                else:
                    # Reset window if word not in target list
                    seen.clear()
                    used = 0
                    left = right + word_len

        return res



        
# @lc code=end

