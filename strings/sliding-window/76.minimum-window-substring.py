#
# @lc app=leetcode id=76 lang=python
#
# [76] Minimum Window Substring
#

# @lc code=start
from collections import Counter, defaultdict


class Solution(object):
    def minWindow(self, s, t):
        """
        :type s: str
        :type t: str
        :rtype: str
        """
        
        if not s or not t:
            return ""
        if len(t) > len(s):
            return ""
        need = Counter(t)

        left = 0
        minimum = float("inf")
        ans = ""
        seen = defaultdict(int)
        formed = 0
        required = len(need)
        for right in range(len(s)):
            if s[right] in need:
                seen[s[right]] += 1

            if s[right] in need and seen[s[right]] == need[s[right]]:
                formed += 1
            while formed == required:
                window_len = right - left + 1
                if minimum > window_len:
                    minimum = window_len
                    ans = s[left:right + 1]

                if s[left] in need:
                    seen[s[left]] -= 1
                    if seen[s[left]] < need[s[left]]:
                        formed -= 1
                left +=1

            
        return ans


# @lc code=end
