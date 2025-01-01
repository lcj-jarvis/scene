package com.mrlu.server.thread;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mrlu.exception.ServiceException;
import com.mrlu.server.entity.Animal;
import com.mrlu.server.entity.Person;
import com.mrlu.server.mapper.AnimalMapper;
import com.mrlu.server.mapper.PersonMapper;
import com.mrlu.server.service.AnimalService;
import com.mrlu.server.service.PersonService;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.processing.Completions;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author 简单de快乐
 * @create 2024-04-07 21:26
 *
 * 多线程事务
 */
@Service
@Slf4j
public class MultiThreadTcService {
    
    @Autowired
    private PersonService personService;
    
    @Autowired
    private AnimalService animalService;


    /**
     * 多线程回滚失败情况一
     * 原因：每个子线程都是一个独立的事务，其中一个子线程异常回滚，
     *      不会影响其他子线程和主线程。
     */
    public void threadRollBackFailed01() {
        try {
            CountDownLatch downLatch = new CountDownLatch(5);
            Boolean main = personService.threadRollBackFailed01(1, "main");
            for (int i = 0; i < 5; i++) {
                int finalI = i;
                new Thread(() -> {
                    try {
                        personService.threadRollBackFailed01(finalI, null);
                    }  catch (Exception e) {
                        // 子线程抛出异常，父线程接收不到的
                        log.info("子线程{}异常", Thread.currentThread().getName());
                        throw new ServiceException(e);
                    } finally {
                        downLatch.countDown();
                    }
                }).start();
            }
            downLatch.await();
            log.info("=============end===============");
        } catch (Exception e) {
            // 子线程抛出异常，父线程接收不到的
            log.info("=============main error===============");
            e.printStackTrace();
        }
    }

    /**
     * 多线程事务回滚情况二
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean threadRollBackFailed02() throws InterruptedException {
        AtomicBoolean errorFlag = new AtomicBoolean(false);
        Person Person = new Person().setName("main-" + UUID.randomUUID());
        personService.save(Person);
        CountDownLatch downLatch = new CountDownLatch(5);
        for (int i = 0; i < 5; i++) {
            int finalI = i;
            new Thread(() -> {
                try {
                    boolean error = errorFlag.get();
                    if (error) {
                        return;
                    }
                    /**
                     * 这里用声明式事务.
                     * 1、如果有一个线程出错，其他线程还没有执行完上面的代码，就不会往下执行了。
                     * 2、如果有一个线程出错，有些线程执行完上面的代码，发现没有异常，就会往下执行，这时候也不会回滚。
                     *    即只有出错的线程会回滚，其他正常执行的线程（除主线程外），是会提交事务的。
                     * 3、所有的子线程执行成功，如果主线程执行失败，这些子线程也不会回滚
                     */
                    // 业务逻辑
                    animalService.threadRollBackFailed02(finalI);
                }  catch (Exception e) {
                    errorFlag.compareAndSet(false, true);
                    log.info("子线程{}异常", Thread.currentThread().getName());
                } finally {
                    downLatch.countDown();
                }
            }).start();
        }

        // 主线程阻塞
        downLatch.await();
        boolean error = errorFlag.get();
        if (error) {
            log.info("存在子线程执行异常;主线程{}回滚", Thread.currentThread().getName());
            // 抛出异常让事务回滚
            throw new ServiceException("batch tc error");
        }
        return Boolean.TRUE;
    }

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @Autowired
    private TransactionDefinition transactionDefinition;


    /**
     * 多线程事务回滚情况三
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean threadRollBackFailed03() {
        // 使用线程安全的list，保存每个线程的事务属性
        List<TransactionStatus> threadTransactionStatus = new CopyOnWriteArrayList<>();
        try {
            Person person = new Person().setName("main-" + UUID.randomUUID());
            personService.save(person);
            CountDownLatch downLatch = new CountDownLatch(5);

            // 异常引用
            AtomicReference<Exception> otherThreadExceptionRef = new AtomicReference<>();
            for (int i = 0; i < 5; i++) {
                int finalI = i;
                new Thread(() -> {
                    try {
                        Exception exception = otherThreadExceptionRef.get();
                        if (exception != null) {
                            // 前面已经有线程报错了，不用执行业务逻辑了
                            return;
                        }

                        // 不在子线程提交事务。因为主线程出错了，子线程感知不到
                        // 手动开启事务
                        TransactionStatus transactionStatus = platformTransactionManager.getTransaction(transactionDefinition);
                        threadTransactionStatus.add(transactionStatus);
                        // 业务逻辑
                        animalService.save(new Animal().setName(UUID.randomUUID().toString()));
                        // 模拟异常
                        if (finalI == 3) {
                            int a = 1/0;
                        }
                    }  catch (Exception e) {
                        // 暴露异常引用
                        otherThreadExceptionRef.set(e);
                        log.info("子线程{}异常", Thread.currentThread().getName());
                    } finally {
                        downLatch.countDown();
                    }
                }).start();
            }

            // 主线程阻塞
            downLatch.await();

            Exception exception = otherThreadExceptionRef.get();
            if (exception != null) {
                // 抛出子线程异常
                throw exception;
            }
            // 提交其他线程
            commitOtherThreadTc(threadTransactionStatus);
        } catch (Exception e) {
            // 回滚其他线程
            rollBackOtherThreadTc(threadTransactionStatus);
            // 抛出异常让主事务回滚
            throw new ServiceException("batch tc error");
        }
        return Boolean.TRUE;
    }

    private void rollBackOtherThreadTc(List<TransactionStatus> threadTransactionStatus) {
        for (TransactionStatus transactionStatus : threadTransactionStatus) {
            // 手动回滚事务
            platformTransactionManager.rollback(transactionStatus);
        }
    }


    private void commitOtherThreadTc(List<TransactionStatus> threadTransactionStatus) {
        for (TransactionStatus transactionStatus : threadTransactionStatus) {
            // 手动回滚事务
            platformTransactionManager.commit(transactionStatus);
        }
    }


    @Autowired
    private MultiplyThreadTransactionManager multiplyThreadTransactionManager;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    @Bean
    public ThreadPoolExecutor threadPoolExecutor() {
        int corePoolSize = Runtime.getRuntime().availableProcessors();
        int maximumPoolSize =  corePoolSize * 2;
        long keepAliveTime = 60;
        return new ThreadPoolExecutor
                (
                        corePoolSize,
                        maximumPoolSize,
                        keepAliveTime,
                        TimeUnit.SECONDS,
                        new LinkedBlockingQueue<>(100),
                        Executors.defaultThreadFactory(),
                        new ThreadPoolExecutor.AbortPolicy()
                );
    }


    /**
     * 多线程事务回滚成功情况一
     * 主线程无事务操作，所有的事务操作都在子线程中
     */
    public Boolean batchTcRollBackSuccess01() {
        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            int finalI = i;
            Thread task = new Thread(() -> {
                // 业务逻辑
                animalService.save(new Animal().setName(UUID.randomUUID().toString()));
                // 模拟异常
                if (finalI == 3) {
                    int a = 1 / 0;
                }
            });
            tasks.add(task);
        }
        return multiplyThreadTransactionManager.execute(tasks, threadPoolExecutor);
    }

    /**
     * 多线程事务回滚成功情况二
     * 主线程有事务操作(编程式事务)，所有的事务操作都在子线程中
     */
    public Boolean batchTcRollBackSuccess02() {
        // 主线程开启编程式事务
        TransactionStatus transactionStatus = platformTransactionManager.getTransaction(transactionDefinition);
        Person school = new Person().setName("main02-" + UUID.randomUUID());
        personService.save(school);

        // 开启多线程执行其他业务逻辑
        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            int finalI = i;
            Thread task = new Thread(() -> {
                // 业务逻辑
                animalService.save(new Animal().setName(UUID.randomUUID().toString()));
                // 模拟异常
                if (finalI == 3) {
                    int a = 1 / 0;
                }
            });
            tasks.add(task);
        }

        TcResult tcResult = multiplyThreadTransactionManager.executeInExistingTc(tasks, threadPoolExecutor);
        boolean commit = tcResult.isSuccess();
        if (commit) {
            multiplyThreadTransactionManager.commit(transactionStatus, tcResult.getThreadTcInfos());
        } else {
            multiplyThreadTransactionManager.rollback(transactionStatus, tcResult.getThreadTcInfos());
        }
        return Boolean.TRUE;
    }





    @Autowired
    private SqlSessionFactory sqlSessionFactory;

    public SqlSession getSqlSession(){
        //DefaultSqlSession
        SqlSession sqlSession = sqlSessionFactory.openSession();
        return sqlSession;
    }

    // Spring自动注入SqlSessionTemplate
    @Autowired
    private SqlSession sqlSessionBySpring;

    //Spring自动注入的mapper
    @Autowired
    private AnimalMapper animalMapper;

    @Autowired
    private PersonMapper personMapper;


    public void batchTcRollBackSuccess03() throws SQLException {
        AtomicBoolean execResult = new AtomicBoolean(true);
        SqlSession sqlSession = getSqlSession();
        Connection connection = sqlSession.getConnection();

        try {
            connection.setAutoCommit(false);
            CountDownLatch downLatch = new CountDownLatch(5);
            /*
             * 1、不使用自动注入的mapper。因为在mapper执行sql的过程中，底层使用的SqlSessionTemplate
             * 2、不使用自动注入的sqlSessionBySpring来生成mapper。因为自动注入的sqlSession是SqlSessionTemplate，
             *    在mapper执行sql的过程中，SqlSessionTemplate会给每个线程创建一个新的DefaultSqlSession（如果当前线程不存在的话），
             *    然后用DefaultSqlSession获取数据库连接，即自动注入的SqlSessionTemplate会给每个线程创建一个数据库连接。
             *
             * 要怎么做才能使用同一个数据库连接呢？？？
             * 答：我们发现创建数据库连接使用的是DefaultSqlSession，因此我们通过SqlSessionFactory来创建DefaultSqlSession。
             * 然后根据DefaultSqlSession获取数据库连接，从而保证数据库连接已经存到DefaultSqlSession中。
             * 最后根据DefaultSqlSession创建的Mapper，就实现创建的Mapper使用同一个DefaultSqlSession，
             * 从而达到使用DefaultSqlSession里的(同一个)数据库连接。
             *
             *
             * 需要注意的是: 我们要先在main线程中获取数据库连接，再获取Mapper，这个顺序是不能变的。
             * 为什么呢？？
             * 答：假设有5个子线程，我们先获取Mapper，然后在子线程中执行逻辑和在主线程中获取数据库连接。
             * 这样子线程有可能先获取到数据库连接a，设置DefaultSqlSession中的数据库连接为a，主线程执行慢，得到数据库连接b，
             * 就会重新设置DefaultSqlSession中的数据库连接为b。
             * 子线程使用覆盖前的数据库连接a继续执行逻辑，主线程使用数据库连接b执行，这两个线程使用的不是同一个连接。
             *
             */
            AnimalMapper animalMapperBySession = sqlSession.getMapper(AnimalMapper.class);
            PersonMapper personMapperBySession = sqlSession.getMapper(PersonMapper.class);

            Person school = new Person().setName("main03-" + UUID.randomUUID());
            personMapperBySession.insert(school);
            List<String> names = new CopyOnWriteArrayList<>();

            for (int i = 0; i < 5; i++) {
                int finalI = i;
                new Thread(() -> {
                    boolean flag = execResult.get();
                    if (!flag) {
                        return;
                    }
                    try {
                        String name = "main03-sub-" + UUID.randomUUID().toString();
                        names.add(name);
                        Animal animal = new Animal().setName(name);
                        int insert = animalMapperBySession.insert(animal);


                        if (finalI == 3) {
                            int a = 1/0;
                        }

                        // MybatisMapperProxy
                        // 这里底层用的是spring自动注入的mapper，不会回滚
                        // animalService.save4Tc(finalI);
                    } catch (Exception e) {
                        execResult.compareAndSet(true, false);
                        throw new ServiceException(e);
                    } finally {
                        downLatch.countDown();
                    }
                }).start();
            }

            downLatch.await();
            List<Animal> animals = animalMapperBySession.selectList(new LambdaQueryWrapper<Animal>().in(Animal::getName, names));
            log.info("animals={}", animals);

            // 获取所有线程的最终执行结果来确定事务是否需要回滚
            boolean finalResult = execResult.get();
            if (finalResult) {
                connection.commit();
            } else {
                connection.rollback();
            }
            log.info("=============finish===============");
        } catch (Exception e) {
            e.printStackTrace();
            connection.rollback();
        }
    }


    public void batchTcRollBackSuccess04() throws SQLException {
        List<Connection> connections = new CopyOnWriteArrayList<>();
        AtomicBoolean execResult = new AtomicBoolean(true);
        SqlSession mainSqlSession = getSqlSession();
        Connection mainConnection = mainSqlSession.getConnection();
        try {
            /*
             * 1、main线程获取sqlSession，使用sqlSession获取main线程的数据库连接，即mainConnection。
             * 通过mainConnection获取Mapper，获取到的Mapper在底层会使用mainConnection操作数据库。
             *
             * 2、每个子线程获取各自的sqlSession，使用sqlSession获取各自的数据库连接，
             * 再通过数据库连接获取Mapper，最终每个线程里的Mapper都会使用各自的数据库连接操作数据库。
             *
             * 3、统一收集子线程的执行结果，如果子线程和main都执行成功，就提交所有数据库连接对应的事务。反正，则回滚。
             */
            mainConnection.setAutoCommit(false);
            connections.add(mainConnection);
            CountDownLatch downLatch = new CountDownLatch(5);
            AnimalMapper animalMapperBySession = mainSqlSession.getMapper(AnimalMapper.class);
            PersonMapper personMapperBySession = mainSqlSession.getMapper(PersonMapper.class);
            Person school = new Person().setName("main04-" + UUID.randomUUID());
            personMapperBySession.insert(school);

            List<String> names = new CopyOnWriteArrayList<>();
            for (int i = 0; i < 5; i++) {
                int finalI = i;
                new Thread(() -> {
                    boolean flag = execResult.get();
                    if (!flag) {
                        return;
                    }
                    try {
                        SqlSession subThreadSqlSession = getSqlSession();
                        Connection subThreadConnection = subThreadSqlSession.getConnection();
                        subThreadConnection.setAutoCommit(false);
                        AnimalMapper animalMapperBySubThread = subThreadSqlSession.getMapper(AnimalMapper.class);
                        connections.add(subThreadConnection);

                        String name = "main04-sub-" + UUID.randomUUID().toString();
                        names.add(name);
                        Animal animal = new Animal().setName(name);
                        animalMapperBySubThread.insert(animal);

                        if (finalI == 3) {
                            int a = 1/0;
                        }
                    } catch (Exception e) {
                        execResult.compareAndSet(true, false);
                        log.info("子线程发生异常,", e);
                    } finally {
                        downLatch.countDown();
                    }
                }).start();
            }

            downLatch.await();
            // 这里子事务还没有提交，主线程的数据库连接是查不到的
            List<Animal> animals = animalMapperBySession.selectList(new LambdaQueryWrapper<Animal>().in(Animal::getName, names));
            log.info("animals={}", animals);

            // 获取所有线程的最终执行结果来确定事务是否需要回滚
            commitOrRollback(execResult.get(), connections);
        } catch (Exception e) {
            commitOrRollback(false, connections);
            e.printStackTrace();
        } finally {
            close(connections);
        }
    }

    private void commitOrRollback(boolean commit, List<Connection> connections) throws SQLException {
        for (Connection connection : connections) {
            if (commit) {
                connection.commit();
            } else {
                connection.rollback();
            }
        }
        log.info("finish commitOrRollback;commit={}", commit);
    }

    private void close(List<Connection> connections) throws SQLException {
        for (Connection connection : connections) {
            connection.close();
        }
    }


    /**
     * 没有使用同一个数据库连接，回滚失败
     */
    public void threadRollBackFailed04() throws SQLException {
        AtomicBoolean execResult = new AtomicBoolean(true);
        SqlSession sqlSession = getSqlSession();
        Connection connection = sqlSession.getConnection();
        try {
            connection.setAutoCommit(false);
            CountDownLatch downLatch = new CountDownLatch(5);

            Person school = new Person().setName("main03-" + UUID.randomUUID());
            personMapper.insert(school);

            List<String> names = new CopyOnWriteArrayList<>();
            for (int i = 0; i < 5; i++) {
                int finalI = i;
                new Thread(() -> {
                    boolean flag = execResult.get();
                    if (!flag) {
                        return;
                    }
                    try {
                        String name = "main03-sub-" + UUID.randomUUID().toString();
                        names.add(name);
                        Animal animal = new Animal().setName(name);

                        // MybatisMapperProxy
                        int insert = animalMapper.insert(animal);
                        if (finalI == 3) {
                            int a = 1/0;
                        }
                    } catch (Exception e) {
                        execResult.compareAndSet(true, false);
                        throw new ServiceException(e);
                    } finally {
                        downLatch.countDown();
                    }
                }).start();
            }
            downLatch.await();
            List<Animal> animals = animalMapper.selectList(new LambdaQueryWrapper<Animal>().in(Animal::getName, names));
            log.info("animals={}", animals);

            // 获取所有线程的最终执行结果来确定事务是否需要回滚
            boolean finalResult = execResult.get();
            if (finalResult) {
                connection.commit();
            } else {
                connection.rollback();
            }
            log.info("=============finish===============");
        } catch (Exception e) {
            e.printStackTrace();
            connection.rollback();
        }
    }


}
