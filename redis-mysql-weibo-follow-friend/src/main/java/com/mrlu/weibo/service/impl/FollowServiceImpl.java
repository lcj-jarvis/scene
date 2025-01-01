package com.mrlu.weibo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mrlu.response.CommonResults;
import com.mrlu.weibo.entity.Follow;
import com.mrlu.weibo.mapper.FollowMapper;
import com.mrlu.weibo.service.FollowService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Set;

/**
 * 用户和用户关注表(Follow)表服务实现类
 *
 * @author 简单de快乐
 * @since 2023-05-25 16:46:41
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements FollowService {

    @Resource
    private RedisTemplate redisTemplate;


    /**
     * 关注/取关
     * @param userId 当前用户id（实际上在项目上用户信息，包括用户id，一般都是用登录的token中获取的）
     * @param followUserId 关注/取关的用户id (有谁关注了userId)
     * @param isFollowed 1:关注 0:取关
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommonResults follow(Integer userId, Integer followUserId, Integer isFollowed) {
        Follow followInfo = getFollowInfo(userId, followUserId);
        if (isFollowed == 1 && followInfo == null) {
            // 首次关注，添加关注信息
            Date now = new Date();
            Follow firstFollow = new Follow().setFollowUserId(followUserId)
                    .setIsValid(1)
                    .setUserId(userId)
                    .setCreateDate(now)
                    .setUpdateDate(now);
            boolean isFollow = save(firstFollow);
            if (isFollow) {
                // 添加关注信息到redis
                addToRedisSet(userId, followUserId);
            }
            return CommonResults.ok("关注成功");
        }

        if (isFollowed == 1 && followInfo.getIsValid() == 0) {
            // 之前取关过了，再次关注
            boolean success = setIsValid(1, followInfo.getId());
            if (success) {
                // 添加关注信息到redis
                addToRedisSet(userId, followUserId);
            }
            return CommonResults.ok("关注成功");
        }

        if (isFollowed == 0 && followInfo != null && followInfo.getIsValid() == 1) {
            // 取消关注
            boolean success = setIsValid(0, followInfo.getId());
            if (success) {
                // 移除redis的关注信息
                removeFromRedisSet(userId, followUserId);
            }
            return CommonResults.ok("成功取关");
        }

        return CommonResults.ok("操作成功");
    }

    @Override
    public CommonResults findCommonsFriends(Integer currentUserId, Integer otherUserId) {
        String currentUserKey = USER_FOLLOWING + currentUserId;
        String otherUserKey = USER_FOLLOWING + otherUserId;
        // 计算交集
        Set<Integer> userIds = redisTemplate.opsForSet().intersect(currentUserKey, otherUserKey);
        // todo 这里就不根据用户id去组装用户信息了
        return CommonResults.ok(userIds);
    }

    @Override
    public CommonResults total(Integer userId) {
        String userKey = USER_FOLLOWING + userId;
        Long size = redisTemplate.opsForSet().size(userKey);
        return CommonResults.ok(size);
    }

    @Override
    public CommonResults getFollowingFriends(Integer userId) {
        String userKey = USER_FOLLOWING + userId;
        Set<Integer> members = redisTemplate.opsForSet().members(userKey);
        return CommonResults.ok(members);
    }

    @Override
    public CommonResults isFollow(Integer currentUserId, Integer otherUserId) {
        String userKey = USER_FOLLOWERS + otherUserId;
        Boolean isMember = redisTemplate.opsForSet().isMember(userKey, currentUserId);
        return CommonResults.ok(isMember);
    }

    private boolean setIsValid(int valid, int id) {
        Follow againFollow = new Follow().setIsValid(valid)
                .setUpdateDate(new Date()).setId(id);
        boolean success = updateById(againFollow);
        return success;
    }


    private Follow getFollowInfo(Integer userId, Integer followUserId) {
        LambdaQueryWrapper<Follow> condition = new LambdaQueryWrapper<Follow>()
                .eq(Follow::getUserId, userId)
                .eq(Follow::getFollowUserId, followUserId);
        return getOne(condition);
    }

    private static final String USER_FOLLOWING = "user_following_";
    private static final String USER_FOLLOWERS = "user_followers_";

    /**
     * 添加关注列表到 Redis
     * @param userId
     * @param followUserId
     */
    private void addToRedisSet(Integer userId, Integer followUserId) {
        // 当前用户userId下有谁关注了
        redisTemplate.opsForSet().add(USER_FOLLOWING + userId, followUserId);
        // 用户followUserId 关注了谁
        redisTemplate.opsForSet().add(USER_FOLLOWERS + followUserId, userId);
    }

    /**
     * 移除 Redis 关注列表
     *
     * @param userId
     * @param followUserId
     */
    private void removeFromRedisSet(Integer userId, Integer followUserId) {
        redisTemplate.opsForSet().remove(USER_FOLLOWING + userId, followUserId);
        redisTemplate.opsForSet().remove(USER_FOLLOWERS + followUserId, userId);
    }



}
