package com.mrlu.weibo.entity;

import java.util.Date;
import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.experimental.Accessors;

/**
 * 用户和用户关注表(Follow)实体类
 *
 * @author 简单de快乐
 * @since 2023-05-25 16:46:41
 */
@Data
@Accessors(chain = true)
@TableName("t_follow")
public class Follow implements Serializable {
    private static final long serialVersionUID = 267563877201767199L;

    @TableId(type = IdType.AUTO)
    private Integer id;
    /**
     * 当前登录用户的id
     */
    private Integer userId;
    /**
     * 当前登录用户关注的用户的id
     */
    private Integer followUserId;
    /**
     * 关注状态，0-没有关注，1-关注了
     */
    private Integer isValid;

    private Date createDate;

    private Date updateDate;

}

