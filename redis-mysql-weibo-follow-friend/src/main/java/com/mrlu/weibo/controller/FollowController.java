package com.mrlu.weibo.controller;

import com.mrlu.response.CommonResults;
import com.mrlu.weibo.service.FollowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


/**
 * 用户和用户关注表(Follow)表控制层
 * @author 简单de快乐
 * @since 2023-05-25 16:46:41
 */
@RestController
@RequestMapping("/friend")
public class FollowController {

    /**
     * 服务对象
     */
    @Autowired
    private FollowService followService;

    /**
     * 关注/取关
     * @param userId 当前用户id（实际上在项目上用户信息，包括用户id，一般都是用登录的token中获取的）
     * @param followUserId 关注/取关的用户id
     * @param isFollowed 1:关注 0:取关
     */
    @GetMapping("/follow")
    public CommonResults follow(@RequestParam Integer userId, @RequestParam Integer followUserId, @RequestParam Integer isFollowed) {
        return followService.follow(userId, followUserId, isFollowed);
    }

    /**
     * 其他用户是否关注了当前用户
     * @param currentUserId 当前用户id（实际上在项目上用户信息，包括用户id，一般都是用登录的token中获取的）
     * @param otherUserId 其他用户id
     * @return true：是  false：否
     */
    @GetMapping("/isFollow")
    public CommonResults isFollow(@RequestParam Integer currentUserId, @RequestParam Integer otherUserId) {
        return followService.isFollow(currentUserId, otherUserId);
    }

    /**
     * 共同关注列表
     * @param currentUserId 当前用户id（实际上在项目上用户信息，包括用户id，一般都是用登录的token中获取的）
     * @param otherUserId 其他用户id
     * @return 返回currentUserId和otherUserId共同关注的好友
     */
    @GetMapping("/commons")
    public CommonResults findCommonsFriends(@RequestParam Integer currentUserId, @RequestParam Integer otherUserId) {
        return followService.findCommonsFriends(currentUserId, otherUserId);
    }

    /**
     * 获取关注的总人数
     * @param userId
     */
    @GetMapping("/total")
    public CommonResults total(Integer userId) {
        return followService.total(userId);
    }

    /**
     * 获取关注的用户列表
     * @param userId
     */
    @GetMapping("/list")
    public CommonResults getFollowingFriends(Integer userId) {
        return followService.getFollowingFriends(userId);
    }

}

