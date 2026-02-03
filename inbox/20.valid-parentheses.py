#
# @lc app=leetcode id=20 lang=python
#
# [20] Valid Parentheses
#

# @lc code=start
class Solution(object):
    def isValid(self, s):
        """
        :type s: str
        :rtype: bool
        """
        dict = {
            ')': '(',
            ']': '[',
            '}': '{'
        }
        stack = []
        for c in s:
            if c in dict:
                if not stack or stack[-1] != dict[c]:
                    return False
                stack.pop()
            else:
                stack.append(c)
        return not stack


# @lc code=end

