#
# @lc app=leetcode id=1493 lang=python
#
# [1493] Longest Subarray of 1's After Deleting One Element
#

# @lc code=start
class Solution(object):
    def longestSubarray(self, nums):
        """
        :type nums: List[int]
        :rtype: int
        """

        left = 0 
        max_length = 0
        curr_length = 0
        num_zeros = 0

        saw_zero = False

        for right in range(len(nums)):
            if nums[right] == 1:
                curr_length += 1
            if nums[right] == 0:
                num_zeros += 1
                saw_zero = True
            while num_zeros > 1:
                if nums[left] == 0:
                    num_zeros -= 1
                else:
                    curr_length -= 1
                left += 1

            max_length = max(max_length, curr_length)
        return max_length if saw_zero else max(0, max_length - 1)

            
        
# @lc code=end

