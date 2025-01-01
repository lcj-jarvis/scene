package com.mrlu.tx.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mrlu.exception.ServiceException;
import com.mrlu.tx.entity.Person;
import com.mrlu.tx.exception.CustomException;
import com.mrlu.tx.exception.CustomExceptionV2;
import com.mrlu.tx.exception.NotRunTimeException;
import com.mrlu.tx.mapper.PersonMapper;
import com.mrlu.tx.service.AnimalService;
import com.mrlu.tx.service.PersonService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Random;
import java.util.UUID;

/**
 * @author 简单de快乐
 * @since 2023-12-04 17:15:17
 */
@Service
@Slf4j
public class PersonServiceImpl extends ServiceImpl<PersonMapper, Person> implements PersonService {

    @Autowired
    private PersonService personService;

    @Autowired
    private AnimalService animalService;

    @Autowired
    private ApplicationContext applicationContext;


    /**
     * 获取的bean是否可以使用事务拦截器
     */
    @Override
    public void canGettedBeanUseTc() {
        PersonService bean = applicationContext.getBean(PersonService.class);
        // 发现获取到的是代理对象
        log.info("bean is proxy={};bean={}", AopUtils.isAopProxy(bean), bean);
        // 不会走事务拦截器
        bean.savePerson();
    }

    /**
     * 声明式事务和编程式事务一起使用
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean testSaveByDeclaredTcAndManualTc() {
        String name = UUID.randomUUID().toString().substring(0, 5);
        Integer age = 1;
        Person p1 = new Person().setName(name).setAge(age);
        save(p1);
        log.info("P1={}", p1);

        // 直接通过this调用的话，走不到事务增强器的
        System.out.println(this);
        PersonService bean = applicationContext.getBean(PersonService.class);
        System.out.println("PersonService：" + bean);
        // 通过aop工具类判断是否为代理。发现获取到的是代理对象
        log.info("bean is proxy={};", AopUtils.isAopProxy(bean));
        bean.savePerson();

        // this 目标对象 直接调用，并不是代理对象进行调用，secondSave是不会触发走事务的aop的。
        // 对于controller来说，通过PersonService的实现类来调用testSave方法，即通过代理对象调用，是会走到事务的aop的
        savePerson();

        // animalService的saveAnimal方法为spring事务动态代理的方法，会走到事务的aop的
        animalService.saveAnimal();
        return Boolean.TRUE;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void savePerson() {
        String name = UUID.randomUUID().toString().substring(0, 5);
        Integer age = new Random().nextInt();
        Person p2 = new Person().setName(name).setAge(age);
        save(p2);
        log.info("person={}", p2);
        // throw new ServiceException("=============ex=============");
    }

    /**================================以下是编程式事务第一种方式========================================**/
    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    // 默认传播行为: PROPAGATION_REQUIRED。实际上是TransactionTemplate
    @Autowired
    private TransactionDefinition transactionDefinition;


    /**
     * 该方法PersonService使用编程式事务，AnimalService使用声明式事务，
     * 使用发现同一个数据库连接，看源码发现属于一个事务。
     * 默认传播行为: PROPAGATION_REQUIRED，animalService的事务会加入PersonService的事务
     * @return
     */
    @Override
    public Boolean testFirstManual() {
        // 手动开启事务
        TransactionStatus transactionStatus = platformTransactionManager.getTransaction(transactionDefinition);
        log.info("person transactionDefinition={}", transactionDefinition);
        log.info("person transactionStatus={}", transactionStatus);
        try {
            String name = UUID.randomUUID().toString().substring(0, 5);
            Integer age = 6;
            Person p2 = new Person().setName(name).setAge(age);
            save(p2);

            // 参考事务AOP的TransactionInfo txInfo = createTransactionIfNecessary(ptm, txAttr, joinpointIdentification);的status = tm.getTransaction(txAttr);
            // 和PersonService使用同一个事务，因为是同一个链接。看源码也知道，当animalService发saveAnimal调用完时，没有提交事务
            animalService.saveAnimal();

            // int i = 1 / 0;

            // 手动提交事务
            platformTransactionManager.commit(transactionStatus);
        } catch (Exception e) {
            log.error("error;",e);
            // 手动回滚事务
            platformTransactionManager.rollback(transactionStatus);
        }
        return true;
    }

    /**
     * 该方法PersonService使用声明事务，AnimalService使用编程式事务
     * 使用发现同一个数据库连接，看源码发现属于一个事务。
     * 默认传播行为: PROPAGATION_REQUIRED，animalService的事务会加入PersonService的事务
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean testSecondManual() {
        String name = UUID.randomUUID().toString().substring(0, 5);
        Integer age = 8;
        Person p2 = new Person().setName(name).setAge(age);
        save(p2);

        animalService.manualSaveAnimal();

        // int i = 1 / 0;

        return true;
    }

    /**
     * 该方法PersonService使用编程式事务，AnimalService使用编程式事务
     * 使用发现同一个数据库连接，看源码发现属于一个事务。
     * 默认传播行为: PROPAGATION_REQUIRED，animalService的事务会加入PersonService的事务
     * @return
     */
    @Override
    public Boolean testThirdManual() {
        // 手动开启事务
        TransactionStatus transactionStatus = platformTransactionManager.getTransaction(transactionDefinition);
        try {
            String name = UUID.randomUUID().toString().substring(0, 5);
            Integer age = 9;
            Person p2 = new Person().setName(name).setAge(age);
            save(p2);

            animalService.manualSaveAnimal();

             int i = 1 / 0;

            // 手动提交事务
            platformTransactionManager.commit(transactionStatus);
        } catch (Exception e) {
            log.error("error;",e);
            // 手动回滚事务
            platformTransactionManager.rollback(transactionStatus);
        }
        return true;
    }


    /**
     * 以下是编程式事务第二种方式。参考官网
     * https://docs.spring.io/spring-framework/docs/5.3.22/reference/html/data-access.html#transaction-programmatic
     */
    // 默认传播行为: PROPAGATION_REQUIRED。
    @Autowired
    private TransactionTemplate transactionTemplate;

    /**
     * 点进去execute方法，发现底层实际上和我们上面的编程式事务第一种写法是类似的
     */
    @Override
    public Boolean testFourthManual() {
        Boolean execute = transactionTemplate.execute(new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction(TransactionStatus status) {
                addDemoPerson();
                int i = 1 / 0;
                return Boolean.TRUE;
            }
        });
        return execute;
    }

    /**
     * 点进去execute方法，发现底层实际上和我们上面的编程式事务第一种写法是类似的
     */
    @Override
    public void testFifthManual() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                //try {
                //    addDemoPerson();
                //    throw new ServiceException("添加失败");
                //} catch (ServiceException ex) {
                //    status.setRollbackOnly();
                //}
                addDemoPerson();
                int i = 1 / 0;
            }
        });
    }

    @Override
    public int addDemoPerson() {
        String name = UUID.randomUUID().toString().substring(0, 5);
        Integer age = 8;
        Person person = new Person().setName(name).setAge(age);
        save(person);
        log.info("finish addDemoPerson;person={}", person);
        return person.getId();
    }

    /**
     *
     * 解析事务注解并封装为基于规则的事务属性类RuleBasedTransactionAttribute
     * rollbackFor/rollbackForClassName封装为RollbackRuleAttribute（回滚规则属性）
     * noRollbackFor/noRollbackForClassName封装为NoRollbackRuleAttribute（非回滚规则属性），NoRollbackRuleAttribute继承RollbackRuleAttribute
     * @see org.springframework.transaction.annotation.SpringTransactionAnnotationParser#parseTransactionAnnotation(Transactional)
     *
     * 判断是否回滚，参考以下方法。
     * @see org.springframework.transaction.interceptor.RuleBasedTransactionAttribute#rollbackOn(Throwable)
     *
     * // Winning rule is the shallowest rule (that is, the closest in the inheritance hierarchy to the exception).
     * If no rule applies (-1), return false.
     * @Override
     * public boolean rollbackOn(Throwable ex) {
     *     RollbackRuleAttribute winner = null;
     *     int deepest = Integer.MAX_VALUE;
     *
     *     // 如果 rollbackRules 不为空
     *     if (this.rollbackRules != null) {
     *         // 遍历所有的回滚规则
     *         for (RollbackRuleAttribute rule : this.rollbackRules) {
     *             // 获取当前规则匹配异常的深度
     *             int depth = rule.getDepth(ex);
     *             // 如果匹配且深度小于当前最深的深度
     *             if (depth >= 0 && depth < deepest) {
     *                 deepest = depth;
     *                 winner = rule;
     *             }
     *         }
     *     }
     *
     *     // 如果没有匹配到任何规则，使用父类的行为（对未检查异常进行回滚）
     *     if (winner == null) {
     *         return super.rollbackOn(ex);
     *     }
     *
     *     // 如果匹配到规则并且该规则不是 NoRollbackRuleAttribute 类型，则回滚
     *     return !(winner instanceof NoRollbackRuleAttribute);
     * }
     *
     * super.rollbackOn(ex)如下
     * public boolean rollbackOn(Throwable ex) {
     * 		return (ex instanceof RuntimeException || ex instanceof Error);
     * }
     *
     * // 返回超类匹配的深度。0表示ex完全匹配。如果没有匹配，返回-1。否则，返回深度，最低深度获胜。
     * public int getDepth(Throwable ex) {
     * 		return getDepth(ex.getClass(), 0);
     * }
     * private int getDepth(Class<?> exceptionClass, int depth) {
     * 		if (exceptionClass.getName().contains(this.exceptionName)) {
     * 			// Found it!
     * 			return depth;
     *        }
     * 		// If we've gone as far as we can go and haven't found it...
     * 		if (exceptionClass == Throwable.class) {
     * 			return -1;
     *        }
     * 		return getDepth(exceptionClass.getSuperclass(), depth + 1);
     * }
     *
     * 【重点结论】对方法的注释和源码分析进行总结：
     * 获胜的规则是在继承层次中最接近异常的规则。如果没有规则适用，则
     * 采用默认规则：即判断抛出的异常是否是RuntimeException类型(包含子类)或者是Error类型(包含子类)，如果是，则回滚，反之不回滚。
     *
     * 这是什么意思呢？？？我们举例说明：
     *  1、@Transactional(rollbackFor = RuntimeException.class, noRollbackFor = Exception.class)
     *     当被代理的方法抛出ServiceException异常时。判断采用的规则
     *     ServiceException异常的全类名不包含RuntimeException的全类名，也不包含Exception的全类名
     *     获取ServiceException的父类RuntimeException，此时发现包含rollbackFor属性指定的RuntimeException的全类名（RollbackRuleAttribute）
     *     但是ServiceException的父类Exception，也包含noRollbackFor属性指定的Exception的全类名。（NoRollbackRuleAttribute）
     *     两个异常都满足，取继承层次最小的
     *     ServiceException --> RuntimeException 的继承层级为1层
     *     ServiceException --> Exception 的继承层级为2层
     *     最终获取到的规则是回滚。
     *
     * 2、@Transactional(rollbackFor = Exception.class, noRollbackFor = ServiceException.class)
     *     当执行被代理的方法抛出ServiceException异常时。判断采用的规则
     *      ServiceException异常的全类名不包含RuntimeException的全类名，也不包含Exception的全类名
     *      获取ServiceException的父类RuntimeException，此时发现包含noRollbackFor属性指定的RuntimeException的全类名（NoRollbackRuleAttribute）
     *      但是ServiceException的父类Exception，也包含rollbackFor属性指定的Exception的全类名。（RollbackRuleAttribute）
     *      两个异常都满足，取继承层次最小的
     *      ServiceException --> RuntimeException 的继承层级为1层
     *      ServiceException --> Exception 的继承层级为2层
     *      最终获取到的规则是不回滚。
     *
     * 3、@Transactional(rollbackFor = Exception.class, noRollbackFor = Exception.class)
     *    当执行被代理的方法抛出ServiceException异常时。
     *    ServiceException的异常名称不包含Exception的全类名
     *    获取到ServiceException的父类Exception，同时包含rollbackFor属性和noRollbackFor属性指定的Exception的全类名。
     *    此时RollbackRuleAttribute和NoRollbackRuleAttribute都满足。而且继承层级是一样的。
     *    接下来要怎么判断呢？？？
     *    我们在以下源码发现在解析事务注解属性的时候，先添加的是RollbackRuleAttribute
     *    org.springframework.transaction.annotation.SpringTransactionAnnotationParser#parseTransactionAnnotation(Transactional)
     *    对于继承层级相同的规则，优先获取RollbackRuleAttribute（第一个遍历到的）。所以最终获取到的规则是回滚。
     *
     * 4、@Transactional(rollbackFor = CustomException.class)
     *   （1）抛出CustomExceptionV2异常或者CustomException.AnotherException，他们的异常全类名分别为
     *    com.mrlu.tx.exception.CustomExceptionV2、com.mrlu.tx.exception.CustomException$AnotherException，
     *    都包含CustomException的全类名，所以会回滚。
     *
     *   （2）抛出ServiceException异常时，ServiceException异常以及所有父类的全类名都不包含CustomException的全类名。
     *       没有找到规则。
     *       采用默认规则：即判断抛出的异常是否是RuntimeException类型(包含子类)或者是Error类型(包含子类)，如果是，则回滚，反之不回滚。
     *
     * 5、@Transactional(rollbackFor = CustomException.class, noRollbackFor = ServiceException.class)
     *    (1) 抛出CustomException异常或者它的子类与内部类时，抛出的异常全类名包含CustomException的全类名（RollbackRuleAttribute），
     *        不包含ServiceException的全类名，找到的规则是回滚。
     *   （2）抛出ServiceException异常或者它的子类与内部类时，抛出的异常全类名包含ServiceException的全类名（NoRollbackRuleAttribute），
     *       不包含CustomException的全类名，找到的规则是不回滚。
     *   （3）如果抛出的异常不属于
     *        CustomException异常或者它的子类与内部类
     *        ServiceException异常或者它的子类与内部类
     *        则采用默认规则：即判断抛出的异常是否是RuntimeException类型(包含子类)或者是Error类型(包含子类)，如果是，则回滚，反之不回滚。
     *
     * 6、@Transactional
     *    没有指定回滚和非回滚规则，
     *    采用默认规则：即判断抛出的异常是否是RuntimeException类型(包含子类)或者是Error类型(包含子类)，如果是，则回滚，反之不回滚。
     *
     * 7、@Transactional(rollbackFor = Exception.class)
     *    程序出现OutOfMemoryError等Error的类型，
     *    没有找到RollbackRuleAttribute规则，
     *    采用默认规则：即判断抛出的异常是否是RuntimeException类型(包含子类)或者是Error类型(包含子类)，如果是，则回滚，反之不回滚。
     *
     * @Transactional的rollbackForClassName和noRollbackForClassName本质也是使用上述规则判断
     */
    @Override
    // 抛出ServiceException回滚
    //@Transactional(rollbackFor = RuntimeException.class, noRollbackFor = Exception.class)
    // 抛出ServiceException不回滚。
    //@Transactional(rollbackFor = Exception.class, noRollbackFor = RuntimeException.class)
    // 抛出任何异常和Error都会回滚。
    //@Transactional(rollbackFor = Exception.class, noRollbackFor = Exception.class)
    //@Transactional(rollbackFor = CustomException.class)
    //@Transactional(rollbackFor = CustomException.class, noRollbackFor = ServiceException.class)
    // 抛出Error也会回滚
    @Transactional(rollbackFor = Exception.class)
    //@Transactional(rollbackForClassName = "com.mrlu.tx.exception.CustomException")
    public void testRollBackRule() throws Exception {
        // 该方法用于测试回滚规则

        savePerson();
        // com.mrlu.tx.exception.CustomException
        log.info("CustomException name={}", CustomException.class.getName());
        // com.mrlu.tx.exception.CustomExceptionV2
        log.info("CustomExceptionV2 name={}", CustomExceptionV2.class.getName());
        // com.mrlu.tx.exception.CustomException$AnotherException
        log.info("CustomException.AnotherException name={}", CustomException.AnotherException.class.getName());
        //throw new CustomExceptionV2("save error");
        //throw new CustomException.AnotherException("save error");
        //throw new ServiceException("save error");
        //throw new NotRunTimeException("save error");
        //throw new OutOfMemoryError();
    }

    /**
     * 编程式事务设置全局回滚
     */
    @Override
    public void testGlobalRollback() {
        // 手动开启事务
        TransactionStatus transactionStatus = platformTransactionManager.getTransaction(transactionDefinition);
        try {
            addDemoPerson();
            animalService.manualSaveAnimal();
            // 手动提交事务
            platformTransactionManager.commit(transactionStatus);
        } catch (Exception e) {
            log.error("error;",e);
            // 手动回滚事务
            platformTransactionManager.rollback(transactionStatus);
        }
    }



    /**===================================以下调试事务传播行为=============================================*/
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void testRequired() throws NotRunTimeException {
        addDemoPerson();
        animalService.testRequired();
        //throw new ServiceException("save error");
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void testRequiredNew() throws NotRunTimeException {
        addDemoPerson();
        animalService.testRequiredNew();
        throw new ServiceException("save error");
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    //@Transactional(propagation = Propagation.REQUIRED)
    public void testSupported() throws NotRunTimeException {
        addDemoPerson();
        animalService.testSupported();
        //throw new ServiceException("save error");
    }

    @Override
    //@Transactional(propagation = Propagation.MANDATORY)
    @Transactional(propagation = Propagation.REQUIRED)
    public void testMandatory() throws NotRunTimeException {
        addDemoPerson();
        animalService.testMandatory();
    }

    @Override
    //@Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Transactional(propagation = Propagation.REQUIRED)
    public void testNotSupported() throws NotRunTimeException {
        addDemoPerson();
        animalService.testNotSupported();
        //throw new ServiceException("save error");
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void testNever() throws NotRunTimeException {
        addDemoPerson();
        animalService.testNever();
    }

    @Override
    @Transactional(propagation = Propagation.NESTED)
    public void testNested() throws NotRunTimeException {
        addDemoPerson();
        animalService.testNested();
        throw new ServiceException("save error");
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void testTc() {
        try {
            animalService.testRequired();
        } catch (Exception e) {
            e.printStackTrace();
        }
        addDemoPerson();
    }

    /**
     * 调试Mybatis如何和Spring事务共用数据库连接
     * @throws NotRunTimeException
     */
    @Override
    //@Transactional(propagation = Propagation.REQUIRED)
    @Transactional(propagation = Propagation.SUPPORTS)
    public void testSyncConnection() throws NotRunTimeException {
        addDemoPerson();
        //animalService.testRequired();
        //animalService.testSupported();
        animalService.testRequiredNew();
    }

}
