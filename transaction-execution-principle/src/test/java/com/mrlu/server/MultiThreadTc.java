package com.mrlu.server;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mrlu.server.config.Gender;
import com.mrlu.server.entity.Person;
import com.mrlu.server.service.PersonService;
import com.mrlu.server.thread.MultiThreadTcService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;


import java.sql.SQLException;

/**
 * @author 简单de快乐
 * @create 2024-04-07 21:34
 */
@SpringBootTest
public class MultiThreadTc {

    @Autowired
    private MultiThreadTcService multiThreadTcService;

    @Test
    public void threadRollBackFailedTest01() {
        multiThreadTcService.threadRollBackFailed01();
    }

    @Test
    public void threadRollBackFailedTest02() throws InterruptedException {
        multiThreadTcService.threadRollBackFailed02();
    }

    @Test
    public void threadRollBackFailedTest03() {
        multiThreadTcService.threadRollBackFailed03();
    }

    @Test
    public void threadRollBackFailed04() throws SQLException {
        multiThreadTcService.threadRollBackFailed04();
    }


    @Test
    public void batchTcRollBackSuccess01() {
        multiThreadTcService.batchTcRollBackSuccess01();
    }

    @Test
    public void batchTcRollBackSuccess02() {
        multiThreadTcService.batchTcRollBackSuccess02();
    }

    @Test
    public void batchTcRollBackSuccess03() throws SQLException {
        multiThreadTcService.batchTcRollBackSuccess03();
    }

    @Test
    public void batchTcRollBackSuccess04() throws SQLException {
        multiThreadTcService.batchTcRollBackSuccess04();
    }


    @Autowired
    private PersonService personService;

    /**
     *
     * private static final ThreadLocal<Map<Object, Object>> resources =
     * 			new NamedThreadLocal<>("Transactional resources");
     *
     * 	private static final ThreadLocal<Set<TransactionSynchronization>> synchronizations =
     * 			new NamedThreadLocal<>("Transaction synchronizations");
     *
     * 	private static final ThreadLocal<String> currentTransactionName =
     * 			new NamedThreadLocal<>("Current transaction name");
     *
     * 	private static final ThreadLocal<Boolean> currentTransactionReadOnly =
     * 			new NamedThreadLocal<>("Current transaction read-only status");
     *
     * 	private static final ThreadLocal<Integer> currentTransactionIsolationLevel =
     * 			new NamedThreadLocal<>("Current transaction isolation level");
     *
     * 	private static final ThreadLocal<Boolean> actualTransactionActive =
     * 			new NamedThreadLocal<>("Actual transaction active");
     *
     * 	同一个线程中，获取到的ThreadLocalMap是一样的。
     * 	因为这6个ThreadLocal的hashCode不一样，所以算出来的ThreadLocalMap的位置不一样。
     * 	同时ThreadLocal是ThreadLocalMap中Entry的key
     *
     * @throws InterruptedException
     */
    @Test
    public void single() throws InterruptedException {
        personService.single(1, null);
        System.out.println(personService.list(new LambdaQueryWrapper<Person>().eq(Person::getSex, Gender.MALE)));
    }

    @Test
    public void testThirdManual() {
        personService.testThirdManual();
    }

}
