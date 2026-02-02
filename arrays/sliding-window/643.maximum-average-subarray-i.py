#
# @lc app=leetcode id=643 lang=python
#
# [643] Maximum Average Subarray I
#

# @lc code=start
class Solution(object):
    def findMaxAverage(self, nums, k):
        """
        :type nums: List[int]
        :type k: int
        :rtype: float
        """

        left = 0
        max_sum = float("-inf")
        curr_sum = 0

        for right in range(len(nums)):
            curr_sum += nums[right]
            # When the condition is met for fixed window, reduce the window
            if right - left + 1 == k:
                max_sum = max(max_sum, curr_sum)
                curr_sum -= nums[left]
                left +=1 
                

        return max_sum/float(k)
            

        
# @lc code=end


