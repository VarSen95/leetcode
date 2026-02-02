#
# @lc app=leetcode id=438 lang=python
#
# [438] Find All Anagrams in a String
#

# @lc code=start
from collections import Counter


class Solution(object):
    def findAnagrams(self, s, p):
        """
        :type s: str
        :type p: str
        :rtype: List[int]
        """

        if len(p) > len(s):
            return []
        
        left = 0
        k = len(p)
        need = Counter(p)
        # count of characters upto length of p
        window = Counter(s[:k])
        result = []
        if need == window:
            result.append(0)
        
        for right in range(k, len(s)):
            left = right - k 

            window[s[left]] -=1
            if window[s[left]] == 0:
                del window[s[left]]
            window[s[right]] += 1

            if window == need:
                result.append(left+1)

        return result
        
# @lc code=end

