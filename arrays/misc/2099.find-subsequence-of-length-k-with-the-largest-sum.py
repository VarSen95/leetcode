#
# @lc app=leetcode id=2099 lang=python
#
# [2099] Find Subsequence of Length K With the Largest Sum
#

# @lc code=start
class Solution(object):
    def maxSubsequence(self, nums, k):
        """
        :type nums: List[int]
        :type k: int
        :rtype: List[int]
        """

        pairs = [(nums[i], i) for i in range(len(nums))]

        # sort by the value
        pairs.sort(key=lambda x: x[0], reverse=True)
        topk = pairs[:k]

        topk.sort(key=lambda x: x[1])
        return [x[0] for x in topk]
        # @lc code=end

