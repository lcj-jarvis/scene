package com.mrlu.server.thread;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author 简单de快乐
 * @create 2024-02-04 15:54
 *
 * @RequiredArgsConstructor会帮我们自动注入依赖
 * 多线程事务管理器
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MultiplyThreadTransactionManager {


    public boolean execute(List<Runnable> tasks, Executor executor) {
        if(executor==null){
            throw new IllegalArgumentException("线程池不能为空");
        }
        //是否发生了异常
        AtomicBoolean ex = new AtomicBoolean();

        List<CompletableFuture> taskFutureList=new CopyOnWriteArrayList<>();
        List<TcInfo> tcInfos = new CopyOnWriteArrayList<>();
        tasks.forEach(task->{
            taskFutureList.add(CompletableFuture.runAsync(
                    () -> {
                        try{
                            //1.开启新事务
                            TransactionStatus transactionStatus = transactionManager.getTransaction(transactionDefinition);
                            //2.copy事务资源
                            TransactionResource transactionResource = TransactionResource.copyTransactionResource();

                            //3.设置事务属性等
                            TcInfo tcInfo = new TcInfo();
                            tcInfo.setTransactionStatus(transactionStatus);
                            tcInfo.setTransactionResource(transactionResource);
                            tcInfos.add(tcInfo);

                            //4.异步任务执行
                            task.run();
                        }catch (Throwable throwable){
                            //打印异常
                            throwable.printStackTrace();
                            //其中某个异步任务执行出现了异常,进行标记
                            ex.set(Boolean.TRUE);
                            //其他任务还没执行的不需要执行了
                            taskFutureList.forEach(completableFuture -> completableFuture.cancel(true));
                        }
                    }, executor)
            );
        });

        try {
            //阻塞直到所有任务全部执行结束---如果有任务被取消,这里会抛出异常滴,需要捕获
            CompletableFuture.allOf(taskFutureList.toArray(new CompletableFuture[]{})).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        if (!ex.get()) {
            commit(tcInfos);
        } else {
            rollback(tcInfos);
        }
        return Boolean.TRUE;
    }

    /**
     * 在已经存在事务的线程中执行
     * @param tasks
     * @param executor
     * @return
     */
    public TcResult executeInExistingTc(List<Runnable> tasks, Executor executor) {
        if(executor==null){
            throw new IllegalArgumentException("线程池不能为空");
        }
        //是否发生了异常
        AtomicBoolean ex = new AtomicBoolean();

        List<CompletableFuture> taskFutureList = new CopyOnWriteArrayList<>();
        List<TcInfo> tcInfos = new CopyOnWriteArrayList<>();
        tasks.forEach(task->{
            taskFutureList.add(CompletableFuture.runAsync(
                    () -> {
                        try{
                            //1.开启新事务
                            TransactionStatus transactionStatus = transactionManager.getTransaction(transactionDefinition);
                            //2.copy事务资源
                            TransactionResource transactionResource = TransactionResource.copyTransactionResource();

                            //3.设置事务属性等
                            TcInfo tcInfo = new TcInfo();
                            tcInfo.setTransactionStatus(transactionStatus);
                            tcInfo.setTransactionResource(transactionResource);
                            tcInfos.add(tcInfo);

                            //4.异步任务执行
                            task.run();
                        } catch (Throwable throwable){
                            //打印异常
                            throwable.printStackTrace();
                            //其中某个异步任务执行出现了异常,进行标记
                            ex.set(Boolean.TRUE);
                            //其他任务还没执行的不需要执行了。后面的CompletableFuture.allOf会抛出ExecutionException
                            taskFutureList.forEach(completableFuture -> completableFuture.cancel(true));
                        }
                    }, executor)
            );
        });

        try {
            //阻塞直到所有任务全部执行结束---如果有任务被取消,这里会抛出异常滴,需要捕获
            CompletableFuture.allOf(taskFutureList.toArray(new CompletableFuture[]{})).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("thread exec error;", e);
            return new TcResult().setSuccess(false).setThreadTcInfos(tcInfos);
        }
        // 返回事务结果
        return new TcResult().setSuccess(!ex.get()).setThreadTcInfos(tcInfos);
    }

    public void commit(TransactionStatus existingTransactionStatus, List<TcInfo> threadTcInfos) {
        // 先提交调用者线程(即主线程)
        transactionManager.commit(existingTransactionStatus);
        // 再提交子线程
        commit(threadTcInfos);
    }

    public void rollback(TransactionStatus existingTransactionStatus, List<TcInfo> threadTcInfos) {
        // 先回滚调用者线程(即主线程)，释放主线程的事务属性。先回滚子线程的会报错的
        transactionManager.rollback(existingTransactionStatus);
        // 再回滚子线程
        rollback(threadTcInfos);
    }

    public void rollback(List<TcInfo> threadTcInfos) {
        process(threadTcInfos, false);
    }

    public void commit(List<TcInfo> threadTcInfos) {
        process(threadTcInfos, true);
    }

    private void  process(List<TcInfo> tcInfos, boolean commit) {
        for (TcInfo tcInfo : tcInfos) {
            TransactionResource transactionResource = tcInfo.getTransactionResource();
            TransactionStatus transactionStatus = tcInfo.getTransactionStatus();
            transactionResource.autoWiredTransactionResource();
            if (commit) {
                transactionManager.commit(transactionStatus);
            } else {
                transactionManager.rollback(transactionStatus);
            }
            transactionResource.removeTransactionResource();
        }
    }


    /**
     * 如果是多数据源的情况下,需要指定具体是哪一个数据源
     */
    private final DataSource dataSource;

    private final PlatformTransactionManager transactionManager;

    private final TransactionDefinition transactionDefinition;

    /**
     * 保存当前事务资源,用于线程间的事务资源COPY操作
     */
    @Builder
    public static class TransactionResource{
        //事务结束后默认会移除集合中的DataSource作为key关联的资源记录
        private  Map<Object, Object> resources;

        //下面五个属性会在事务结束后被自动清理,无需我们手动清理
        private  Set<TransactionSynchronization> synchronizations;

        private  String currentTransactionName;

        private Boolean currentTransactionReadOnly;

        private Integer currentTransactionIsolationLevel;

        private Boolean actualTransactionActive;

        public static TransactionResource copyTransactionResource(){
            return TransactionResource.builder()
                    //返回的是不可变集合
                    .resources(TransactionSynchronizationManager.getResourceMap())
                    //如果需要注册事务监听者,这里记得修改--我们这里不需要,就采用默认负责--spring事务内部默认也是这个值
                    .synchronizations(new LinkedHashSet<>())
                    .currentTransactionName(TransactionSynchronizationManager.getCurrentTransactionName())
                    .currentTransactionReadOnly(TransactionSynchronizationManager.isCurrentTransactionReadOnly())
                    .currentTransactionIsolationLevel(TransactionSynchronizationManager.getCurrentTransactionIsolationLevel())
                    .actualTransactionActive(TransactionSynchronizationManager.isActualTransactionActive())
                    .build();
        }

        public void autoWiredTransactionResource(){
            resources.forEach(TransactionSynchronizationManager::bindResource);
            //如果需要注册事务监听者,这里记得修改--我们这里不需要,就采用默认负责--spring事务内部默认也是这个值
            TransactionSynchronizationManager.initSynchronization();
            TransactionSynchronizationManager.setActualTransactionActive(actualTransactionActive);
            TransactionSynchronizationManager.setCurrentTransactionName(currentTransactionName);
            TransactionSynchronizationManager.setCurrentTransactionIsolationLevel(currentTransactionIsolationLevel);
            TransactionSynchronizationManager.setCurrentTransactionReadOnly(currentTransactionReadOnly);
        }


        public void removeTransactionResource() {
            //事务结束后默认会移除集合中的DataSource作为key关联的资源记录
            //DataSource如果重复移除,unbindResource时会因为不存在此key关联的事务资源而报错
            resources.keySet().forEach(key->{
                if(!(key instanceof DataSource)){
                    TransactionSynchronizationManager.unbindResource(key);
                }
            });
        }
    }


    /**================================以下实现也可以=================================================*/
    /**
     * 执行的是无返回值的任务
     * @param tasks 异步执行的任务列表
     * @param executor 异步执行任务需要用到的线程池,考虑到线程池需要隔离,这里强制要求传
     */
    public void runAsyncButWaitUntilAllDown(List<Runnable> tasks, Executor executor) {
        if(executor==null){
            throw new IllegalArgumentException("线程池不能为空");
        }
        DataSourceTransactionManager transactionManager = getTransactionManager();
        //是否发生了异常
        AtomicBoolean ex=new AtomicBoolean();

        List<CompletableFuture> taskFutureList=new CopyOnWriteArrayList<>();
        List<TransactionStatus> transactionStatusList=new CopyOnWriteArrayList<>();
        List<TransactionResource> transactionResources=new CopyOnWriteArrayList<>();

        tasks.forEach(task->{
            taskFutureList.add(CompletableFuture.runAsync(
                    () -> {
                        try{
                            //1.开启新事务
                            transactionStatusList.add(openNewTransaction(transactionManager));
                            //2.copy事务资源
                            transactionResources.add(TransactionResource.copyTransactionResource());
                            //3.异步任务执行
                            task.run();
                        }catch (Throwable throwable){
                            //打印异常
                            throwable.printStackTrace();
                            //其中某个异步任务执行出现了异常,进行标记
                            ex.set(Boolean.TRUE);
                            //其他任务还没执行的不需要执行了
                            taskFutureList.forEach(completableFuture -> completableFuture.cancel(true));
                        }
                    }
                    , executor)
            );
        });

        try {
            //阻塞直到所有任务全部执行结束---如果有任务被取消,这里会抛出异常滴,需要捕获
            CompletableFuture.allOf(taskFutureList.toArray(new CompletableFuture[]{})).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        //发生了异常则进行回滚操作,否则提交
        if(ex.get()){
            log.info("发生异常,全部事务回滚");
            for (int i = 0; i < tasks.size(); i++) {
                transactionResources.get(i).autoWiredTransactionResource();
                transactionManager.rollback(transactionStatusList.get(i));
                transactionResources.get(i).removeTransactionResource();
            }
        }else {
            log.info("全部事务正常提交");
            for (int i = 0; i < tasks.size(); i++) {
                transactionResources.get(i).autoWiredTransactionResource();
                transactionManager.commit(transactionStatusList.get(i));
                transactionResources.get(i).removeTransactionResource();
            }
        }
    }

    private TransactionStatus openNewTransaction(DataSourceTransactionManager transactionManager) {
        //JdbcTransactionManager根据TransactionDefinition信息来进行一些连接属性的设置
        //包括隔离级别和传播行为等
        DefaultTransactionDefinition transactionDef = new DefaultTransactionDefinition();
        //开启一个新事务---此时autocommit已经被设置为了false,并且当前没有事务,这里创建的是一个新事务
        return transactionManager.getTransaction(transactionDef);
    }

    private DataSourceTransactionManager getTransactionManager() {
        return new DataSourceTransactionManager(dataSource);
    }
    /**================================以下实现也可以=================================================*/
}
