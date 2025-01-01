package com.mrlu.weibo.service;

import com.mrlu.response.CommonResults;
import com.mrlu.weibo.entity.Follow;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 用户和用户关注表(Follow)表服务接口
 *
 * @author 简单de快乐
 * @since 2023-05-25 16:46:41
 */
public interface FollowService extends IService<Follow> {

    CommonResults follow(Integer userId, Integer followUserId, Integer isFollowed);

    CommonResults findCommonsFriends(Integer currentUserId, Integer otherUserId);

    CommonResults total(Integer userId);

    CommonResults getFollowingFriends(Integer userId);

    CommonResults isFollow(Integer currentUserId, Integer otherUserId);
}
