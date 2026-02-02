#
# @lc app=leetcode id=567 lang=python
#
# [567] Permutation in String
#

# @lc code=start
from collections import Counter


class Solution(object):
    def checkInclusion(self, s1, s2):
        """
        :type s1: str
        :type s2: str
        :rtype: bool
        """
        k = len(s1)
        if len(s1) > len(s2):
            return False
        window = Counter(s2[:k])
        need = Counter(s1)

        if window == need:
            return True
        
        for right in range(k, len(s2)):
            left = right - k 

            window[s2[left]] -= 1
            if window[s2[left]] == 0:
                del window[s2[left]]
            window[s2[right]] +=1

            if window == need:
                return True
        return False


# @lc code=end

