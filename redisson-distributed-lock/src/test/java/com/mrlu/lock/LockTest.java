package com.mrlu.lock;

import com.mrlu.lock.core.IDistributedLock;
import com.mrlu.server.controller.PersonController;
import com.mrlu.server.entity.Animal;
import com.mrlu.server.entity.Person;
import com.mrlu.server.service.AnimalService;
import com.mrlu.server.service.PersonService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author 简单de快乐
 * @create 2024-01-09 22:37
 *
 * https://blog.51cto.com/knifeedge/5257076#:~:text=Redisson,%E4%B9%9F%E9%83%BD%E5%A4%84%E7%90%86%E7%9A%84%E5%BE%88%E5%A5%BD%E3%80%82
 */
@SpringBootTest
@Slf4j
public class LockTest {

    @Autowired
    private IDistributedLock distributedLock;

    // 计数器
    private int count;

    @Test
    public void test() throws InterruptedException {

        CountDownLatch countDownLatch = new CountDownLatch(1000);

        for (int i = 0; i < 1000; i++) {
            new Thread(() -> {

                // 每个线程都创建自己的锁对象
                // 这是基于 Redis 实现的分布式锁
                RLock lock = distributedLock.lock("counterLock", 555, TimeUnit.SECONDS, false);
                try {
                    // 计数器自增 1
                    this.count = this.count + 1;
                } finally {
                    // 释放锁
                    distributedLock.unlock(lock);
                }
                countDownLatch.countDown();
            }).start();
        }

        countDownLatch.await();

        log.info("count = {}", this.count);
    }


    /**
     * e8d063b8-b00c-4848-8224-8881d517ffea:distributed-lockcounterLock
     * @throws InterruptedException
     */
    @Test
    public void t1() throws InterruptedException {

        //CountDownLatch countDownLatch = new CountDownLatch(1);


        //new Thread(() -> {

            // 每个线程都创建自己的锁对象
            // 这是基于 Redis 实现的分布式锁
            log.info("thread={}", Thread.currentThread());
            RLock lock = distributedLock.lock("counterLock", 555, TimeUnit.SECONDS, false);
            try {
                try {
                    TimeUnit.SECONDS.sleep(666);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // 计数器自增 1
                this.count = this.count + 1;
            } finally {
                // 释放锁
                distributedLock.unlock(lock);
            }

            //countDownLatch.countDown();

        //}).start();


        //countDownLatch.await();

    }

    @Autowired
    private PersonController personController;

    @Autowired
    private PersonService personService;

    @Autowired
    private AnimalService animalService;


    @Test
    public void lockAspectTest01() throws InterruptedException {
        Integer id = 6;
        int threadNum = 1;
        Person person = personService.getById(id);
        Animal animal = animalService.getById(id);
        log.info("oldPerson={},oldAnimal={}", person, animal);

        CountDownLatch countDownLatch = new CountDownLatch(threadNum);
        for (int i = 0; i < threadNum; i++) {
            new Thread(() -> {
                try {
                    personController.testAddAge(id);
                } finally {
                    countDownLatch.countDown();
                }
            }).start();
        }
        countDownLatch.await();

        person = personService.getById(id);
        animal = animalService.getById(id);
        log.info("person={},animal={}", person, animal);
    }

    @Test
    public void lockAspectTest02() throws InterruptedException {
        Integer id = 6;
        int threadNum = 50;
        final Person person = personService.getById(id);
        log.info("oldPerson={}", person);
        CountDownLatch countDownLatch = new CountDownLatch(threadNum);
        for (int i = 0; i < threadNum; i++) {
            new Thread(() -> {
                try {
                    personController.testLockByIdAndName(person);
                } finally {
                    countDownLatch.countDown();
                }
            }).start();
        }
        countDownLatch.await();
        log.info("person={}", personService.getById(id));
    }




}
