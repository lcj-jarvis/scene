package com.mrlu.tx;

import com.mrlu.tx.service.PersonService;
import lombok.Data;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author 简单de快乐
 * @create 2024-07-17 22:30
 */
@SpringBootTest
public class TransactionTest {

    @Autowired
    private PersonService personService;

    @Test
    public void testCanInvokeProxyMethod() {
        personService.addDemoPerson();
    }

    @Test
    public void testCanGettedBeanUseTc() {
        personService.canGettedBeanUseTc();
    }

    @Test
    public void testRollBackRule() throws Exception {
        personService.testRollBackRule();
    }

    @Test
    public void saveByDeclaredTc() throws Exception {
        personService.testSaveByDeclaredTcAndManualTc();
    }

    @Test
    public void testGlobalRollback() throws Exception {
        personService.testGlobalRollback();
    }

    /**===============================传播行为============================================*/
    /*
     *  对于初始调用者来说，对应Propagation.NESTED，Propagation.REQUIRED，Propagation.REQUIRES_NEW传播行为都是新建事务。
     *  这里personService作为初始调用者。
     *
     * （1）调用animalService抛出运行时异常ServiceException
     *      animalService的方法调用完后：命中回滚规则，发现是加入到现有的事务，设置全局回滚。
     *      personService的方法调用完后：接收到异常，命中回滚规则，回滚事务。
     *      但是会抛出以下异常：
     *      org.springframework.transaction.UnexpectedRollbackException: Transaction rolled back because it has been marked as rollback-only
     *
     *      结果：没有新增了一条animal记录，没有新增一条person记录
     *
     * （2）调用animalService抛出编译时异常NotRunTimeException
     *      animalService的方法调用完后：命中回滚规则，发现是加入到现有的事务，设置全局回滚。
     *      personService的方法调用完后：没有命中回滚规则，发现设置了全局回滚，执行回滚。
     *
     *
     *      结果：没有新增了一条animal记录，没有新增一条person记录
     *
     * （3）animalService的方法正常调用后，personService的方法抛出运行时异常ServiceException
     *      animalService的方法调用完后：正常执行，不是新的事务，不提交事务。
     *      personService的方法调用完后：命中回滚规则，回滚事务。
     *      结果：没有新增了一条animal记录，没有新增一条person记录
     *
     * @throws Exception
     */
    @Test
    public void testRequired() throws Exception {
        personService.testRequired();
    }


    /*
     *  对于初始调用者来说，对应Propagation.NESTED，Propagation.REQUIRED，Propagation.REQUIRES_NEW传播行为都是新建事务。
     *  这里personService作为初始调用者。调用animalService方法，不管是否存在事务，都是新建一个事务。
     *  我们称初始调用者personService开启的事务为事务A，被调用者animalService开启的事务称为事务B。
     *
     * （1）调用animalService抛出运行时异常ServiceException
     *      animalService的方法调用完后：命中回滚规则，发现事务B是新的事务，执行回滚。
     *      personService的方法调用完后：接收到异常，命中回滚规则，发现事务A是新的事务，回滚事务。
     *      结果：没有新增了一条animal记录，没有新增一条person记录
     *
     * （2）调用animalService抛出编译时异常NotRunTimeException
     *      animalService的方法调用完后：命中回滚规则，发现事务B是新的事务，执行回滚。
     *      personService的方法调用完后：接收到异常，没有命中回滚规则，发现事务A是新的事务，提交事务。
     *      结果：没有新增了一条animal记录，新增一条person记录
     *
     * （3）animalService的方法正常调用后，personService的方法抛出运行时异常ServiceException
     *      animalService的方法调用完后：正常执行，发现事务B是新的事务，执行提交。
     *      personService的方法调用完后：命中回滚规则，发现事务A是新的事务，回滚事务。
     *      结果：新增了一条animal记录，没有新增一条person记录
     *
     * @throws Exception
     */
    @Test
    public void testRequiredNew() throws Exception {
        personService.testRequiredNew();
    }

    /*
    * 对于初始调用者来说，对于Propagation.SUPPORTS传播行为，以非事务方式运行。
    * 作为被调用者，相当于Propagation.REQUIRED，存在现有的事务，则加入。不存在，则新建事务
    */
    @Test
    public void testSupported() throws Exception {
        personService.testSupported();
    }

    /*
     *  对于初始调用者来说，对应Propagation.NESTED，Propagation.REQUIRED，Propagation.REQUIRES_NEW传播行为都是新建事务。
     *  这里personService作为初始调用者。不管是否存在事务，animalService方法都以非事务的方式运行。
     *
     * （1）调用animalService抛出运行时异常ServiceException
     *      animalService的方法调用完后：抛出异常前的所有数据库操作都自动提交了。
     *      personService的方法调用完后：命中回滚规则，回滚事务。
     *      结果：新增了一条animal记录，没有新增person记录
     *
     * （2）调用animalService抛出编译时异常NotRunTimeException
     *      animalService的方法调用完后：抛出异常前的所有数据库操作都自动提交了。
     *      personService的方法调用完后：没有命中回滚规则，提交事务。
     *      结果：新增了一条animal记录，新增一条person记录
     *
     * （3）animalService的方法正常调用后，personService的方法抛出运行时异常ServiceException
     *      animalService的方法调用完后：正常执行，所有数据库操作都自动提交。
     *      personService的方法调用完后：命中回滚规则，回滚事务。
     *      结果：新增了一条animal记录，没有新增person记录
     */
    @Test
    public void testNotSupported() throws Exception {
        personService.testNotSupported();
    }

    /*
     * 1、对于传播行为propagation = Propagation.MANDATORY，personService作为初始调用者，没有开启事务，则抛出异常。
     * 2、personService作为初始调用者，使用Propagation.REQUIRED传播行为，开启事务。
     *    animalService的方法使用Propagation.MANDATORY传播行为，会加入到现有的事务中。
     */
    @Test
    public void testMandatory() throws Exception {
        personService.testMandatory();
    }

    /*
     *  对于初始调用者来说，对应Propagation.NESTED，Propagation.REQUIRED，Propagation.REQUIRES_NEW传播行为都是新建事务。
     *  这里personService作为初始调用者，开启事务，调用到animalService的方法，发现存在现有的事务，则抛出异常。
     */
    @Test
    public void testNever() throws Exception {
        personService.testNever();
    }

    /*
     *  对于初始调用者来说，对应Propagation.NESTED，Propagation.REQUIRED，Propagation.REQUIRES_NEW传播行为都是新建事务。
     *  这里personService作为初始调用者。
     *
     * （1）调用animalService抛出运行时异常ServiceException
     *      animalService的方法调用完后：命中回滚规则，回滚保存点。
     *      personService的方法调用完后：命中回滚规则，回滚事务。
     *      结果：没有新增了一条animal记录，没有新增一条person记录
     *
     * （2）调用animalService抛出编译时异常NotRunTimeException
     *      animalService的方法调用完后：命中回滚规则，回滚保存点。
     *      personService的方法调用完后：没有命中回滚规则，提交事务。
     *      结果：没有新增了一条animal记录，新增一条person记录
     *
     * （3）animalService的方法正常调用后，personService的方法抛出运行时异常ServiceException
     *      animalService的方法调用完后：正常执行，释放保存点。
     *      personService的方法调用完后：命中回滚规则，回滚事务。
     *      结果：没有新增了一条animal记录，没有新增一条person记录
     *
     * @throws Exception
     */
    @Test
    public void testNested() throws Exception {
        personService.testNested();
    }
    /**===============================传播行为============================================*/
    @Test
    public void testTc() throws Exception {
        personService.testTc();
    }

    @Test
    public void testSyncConnection() throws Exception {
        personService.testSyncConnection();
    }

    @Test
    public void testConnect() {
        // 线程交替输出a、b、c
        ReentrantLock lock = new ReentrantLock();
        Condition condition = lock.newCondition();
        Counter counter = new Counter();
        Printer p1 = new Printer(lock, condition, 0, "a", 3, counter);
        Printer p2 = new Printer(lock, condition, 1, "b", 3, counter);
        Printer p3 = new Printer(lock, condition, 2, "c", 3, counter);
        Thread t1 = new Thread(p1::println);
        Thread t2 = new Thread(p2::println);
        Thread t3 = new Thread(p3::println);
        t1.start();
        t2.start();
        t3.start();
    }
}

class Printer {
    ReentrantLock lock;

    Condition condition;

    private int flag;

    private String str;

    private Integer threadNum;

    private Counter counter;

    public Printer(ReentrantLock lock, Condition condition, int flag, String str, Integer threadNum, Counter counter) {
        this.lock = lock;
        this.condition = condition;
        this.flag = flag;
        this.str = str;
        this.threadNum = threadNum;
        this.counter = counter;
    }

    public void println(){
        while (true) {
            lock.lock();
            try {
                while (counter.getCount() % threadNum != flag) {
                    // 当前线程等待
                    try {
                        condition.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                System.out.println(str);
                counter.increment();

                // 唤醒所有等待的线程
                condition.signalAll();
            } finally {
                lock.unlock();
            }
        }
    }
}

@Data
class Counter {
    private int count;
    public void increment() {
        count++;
    }
}







