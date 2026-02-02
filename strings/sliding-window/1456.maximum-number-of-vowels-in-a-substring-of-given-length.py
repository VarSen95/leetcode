#
# @lc app=leetcode id=1456 lang=python
#
# [1456] Maximum Number of Vowels in a Substring of Given Length
#

# @lc code=start
class Solution(object):
    def maxVowels(self, s, k):
        """
        :type s: str
        :type k: int
        :rtype: int
        """
        left = 0
        no_of_vowels = 0
        max_vowels = float("-inf")
        vowels = ["a", "e", "i", "o", "u"]

        for right in range(len(s)):
            if s[right] in vowels:
                no_of_vowels += 1

            if right - left + 1 == k:
                max_vowels = max(max_vowels, no_of_vowels)
                if s[left] in vowels:
                    no_of_vowels -= 1
                left +=1

        return max_vowels

# @lc code=end

