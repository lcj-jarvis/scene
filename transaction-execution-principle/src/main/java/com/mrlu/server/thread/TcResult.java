package com.mrlu.server.thread;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author 简单de快乐
 * @create 2024-02-04 17:37
 */
@Data
@Accessors(chain = true)
public class TcResult {

    //是否所有的Runable执行成功
    private boolean success;

    // 线程的事务信息
    private  List<TcInfo> threadTcInfos;

}
