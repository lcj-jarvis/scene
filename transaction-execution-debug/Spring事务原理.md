# 前置知识

## 一、事务传播行为

在Spring中，事务传播行为（Transaction Propagation Behavior）定义了一个事务方法如何与现有事务进行交互。Spring提供了七种传播行为，通过`@Transactional`注解可以指定这些行为。了解每种传播行为的含义有助于正确处理事务边界和事务管理。以下是七种传播行为的详细说明：

#### 1. `PROPAGATION_REQUIRED`

这是默认的传播行为。如果当前已经存在事务，则加入该事务；如果没有事务，则创建一个新的事务。
```java
@Transactional(propagation = Propagation.REQUIRED)
public void someMethod() {
    // method implementation
}
```
**示例：**
如果`someMethod`在一个已经存在的事务中被调用，那么`someMethod`将会加入到这个现有事务中。如果在调用时没有事务存在，`someMethod`会启动一个新的事务。

#### 2. `PROPAGATION_REQUIRES_NEW`
每次都会创建一个新的事务。如果当前存在事务，则将其挂起。
```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void someMethod() {
    // method implementation
}
```
**示例：**
无论调用`someMethod`时是否存在事务，它都会创建一个新的事务。如果存在事务，则当前事务会被挂起，`someMethod`完成后再恢复。

#### 3. `PROPAGATION_SUPPORTS`
支持当前事务。如果没有事务存在，则以非事务方式执行。
```java
@Transactional(propagation = Propagation.SUPPORTS)
public void someMethod() {
    // method implementation
}
```
**示例：**
如果`someMethod`在一个事务中被调用，则它会在该事务中执行。如果在调用时没有事务存在，它将以非事务方式执行。

#### 4. `PROPAGATION_NOT_SUPPORTED`
不支持当前事务。如果当前存在事务，则将其挂起，并以非事务方式执行。
```java
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public void someMethod() {
    // method implementation
}
```
**示例：**
无论调用`someMethod`时是否存在事务，它都会以非事务方式执行。如果存在事务，则当前事务会被挂起，`someMethod`完成后再恢复。

#### 5. `PROPAGATION_NEVER`
以非事务方式执行，如果当前存在事务，则抛出异常。
```java
@Transactional(propagation = Propagation.NEVER)
public void someMethod() {
    // method implementation
}
```
**示例：**
如果调用`someMethod`时存在事务，则抛出`IllegalTransactionStateException`异常。如果没有事务存在，它将以非事务方式执行。

#### 6. `PROPAGATION_MANDATORY`
必须在事务中执行，如果当前没有事务，则抛出异常。
```java
@Transactional(propagation = Propagation.MANDATORY)
public void someMethod() {
    // method implementation
}
```
**示例：**
如果调用`someMethod`时不存在事务，则抛出`IllegalTransactionStateException`异常。它只能在一个已经存在的事务中执行。

#### 7. `PROPAGATION_NESTED`
如果当前存在事务，则在该事务中执行一个嵌套事务；如果没有事务，则行为类似于`PROPAGATION_REQUIRED`。
```java
@Transactional(propagation = Propagation.NESTED)
public void someMethod() {
    // method implementation
}
```
**示例：**
如果`someMethod`在一个事务中被调用，则它会创建一个嵌套事务（使用保存点）。如果在调用时没有事务存在，它会启动一个新的事务。

#### 总结
选择合适的传播行为取决于具体的业务需求和事务管理的策略。以下是一些常见的使用场景：

- **`PROPAGATION_REQUIRED`**: 适用于大多数情况，这是默认行为，确保方法在事务中执行。
- **`PROPAGATION_REQUIRES_NEW`**: 适用于需要独立事务的操作，例如日志记录或审计。
- **`PROPAGATION_SUPPORTS`**: 适用于希望方法可以在事务或非事务环境中执行的情况。
- **`PROPAGATION_NOT_SUPPORTED`**: 适用于不希望在事务中执行的操作。
- **`PROPAGATION_NEVER`**: 适用于严格要求非事务环境的操作。
- **`PROPAGATION_MANDATORY`**: 适用于必须在现有事务中执行的操作。
- **`PROPAGATION_NESTED`**: 适用于需要嵌套事务支持的复杂事务操作。



## 二、保存点

数据库保存点（Savepoint）是事务处理中的一个中间状态点，用于在事务执行过程中创建一个标记。可以在需要时回滚到这个标记，而不是回滚整个事务。保存点使得事务的控制更加灵活和精细。

### 1、基本操作

以下是保存点的基本操作，包括设置保存点、回滚到保存点和释放保存点。

#### 1.1 设置保存点

在一个事务中，可以使用 `SAVEPOINT` 命令设置一个保存点。

```sql
SAVEPOINT savepoint_name;
```

#### 1.2 回滚到保存点

可以使用 `ROLLBACK TO SAVEPOINT` 命令回滚到某个保存点。

```sql
ROLLBACK TO SAVEPOINT savepoint_name;
```

#### 1.3 释放保存点

可以使用 `RELEASE SAVEPOINT` 命令释放某个保存点。释放后该保存点将不可用。

```sql
RELEASE SAVEPOINT savepoint_name;
```

### 2、MySQL的保存点机制

以下是使用MySQL的完整示例，演示如何使用保存点。

#### 2.1 创建测试数据库和表

首先，创建一个测试数据库和表：

```sql
CREATE DATABASE testdb;
USE testdb;

CREATE TABLE employees (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50),
    position VARCHAR(50)
);
```

#### 2.2 使用保存点的事务操作

```sql
-- 开始事务
START TRANSACTION;

-- 插入第一条记录
INSERT INTO employees (name, position) VALUES ('John Doe', 'Manager');

-- 设置保存点
SAVEPOINT savepoint1;

-- 插入第二条记录
INSERT INTO employees (name, position) VALUES ('Jane Doe', 'Developer');

-- 回滚到保存点
ROLLBACK TO SAVEPOINT savepoint1;

-- 插入另一条记录
INSERT INTO employees (name, position) VALUES ('Alice Smith', 'Analyst');

-- 提交事务
COMMIT;
```

#### 2.3 验证结果

事务操作完成后，查询 `employees` 表以验证结果：

```sql
SELECT * FROM employees;
```

```
+----+-------------+---------+
| id | name        | position|
+----+-------------+---------+
|  1 | John Doe    | Manager |
|  2 | Alice Smith | Analyst |
+----+-------------+---------+
```





### 3、示例代码

#### 3.1 代码实现

```java
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Savepoint;
import java.sql.SQLException;

public class SavepointExample {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/testdb";
    private static final String DB_USER = "your_username";
    private static final String DB_PASSWORD = "your_password";

    public static void main(String[] args) {
        Connection conn = null;
        Savepoint savepoint1 = null;

        try {
            // 1. 获取数据库连接
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            // 2. 关闭自动提交模式
            conn.setAutoCommit(false);

            // 3. 插入第一条记录
            String sql1 = "INSERT INTO employees (name, position) VALUES (?, ?)";
            try (PreparedStatement pstmt1 = conn.prepareStatement(sql1)) {
                pstmt1.setString(1, "John Doe");
                pstmt1.setString(2, "Manager");
                pstmt1.executeUpdate();
            }

            // 4. 设置保存点
            savepoint1 = conn.setSavepoint("savepoint1");

            // 5. 插入第二条记录
            String sql2 = "INSERT INTO employees (name, position) VALUES (?, ?)";
            try (PreparedStatement pstmt2 = conn.prepareStatement(sql2)) {
                pstmt2.setString(1, "Jane Doe");
                pstmt2.setString(2, "Developer");
                pstmt2.executeUpdate();
            }

            // 模拟错误：将其注释掉以实际测试
            // int error = 1 / 0;

            // 6. 回滚到保存点
            conn.rollback(savepoint1);
            
             // 7、回滚保存点后，释放掉
            // 释放保存点以避免占用资源或引起混淆。
            // 可以帮助数据库管理系统更高效地管理资源，并明确表示该保存点已经不再需要使用。
            conn.releaseSavepoint(savepoint1);


            // 8. 插入另一条记录
            String sql3 = "INSERT INTO employees (name, position) VALUES (?, ?)";
            try (PreparedStatement pstmt3 = conn.prepareStatement(sql3)) {
                pstmt3.setString(1, "Alice Smith");
                pstmt3.setString(2, "Analyst");
                pstmt3.executeUpdate();
            }

            // 9. 提交事务
            conn.commit();

        } catch (SQLException ex) {
            ex.printStackTrace();
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }
}
```

【说明】

**创建保存点**：使用 `Connection.setSavepoint(String name)` 方法创建一个带有名称的保存点。

**回滚到保存点**：使用 `Connection.rollback(Savepoint savepoint)` 方法回滚到指定的保存点。

**释放保存点**：使用 `Connection.releaseSavepoint(Savepoint savepoint)` 方法释放不再需要的保存点。



#### 3.2 查询结果

在MySQL中执行以下查询，验证 `employees` 表中的数据：

```sql
SELECT * FROM employees;
```

最终结果应为：

```plaintext
+----+-------------+---------+
| id | name        | position|
+----+-------------+---------+
|  1 | John Doe    | Manager |
|  2 | Alice Smith | Analyst |
+----+-------------+---------+
```



4、注意事项

1. **事务支持**：保存点只能在事务内部使用，且事务必须是长事务或显式事务。
2. **资源管理**：虽然保存点是有用的，但使用过多的保存点可能会增加资源消耗，需谨慎管理。
3. **数据库支持**：并非所有数据库管理系统都支持保存点，在使用前应确认数据库的支持情况。
4. **嵌套保存点**：可以在一个事务中创建多个保存点，且可以嵌套使用，但需要注意管理这些保存点的层级关系。



## 三、编程式事务

1、方式一

```java
@Autowired
private PlatformTransactionManager platformTransactionManager;

// 默认传播行为: PROPAGATION_REQUIRED。实际上是TransactionTemplate
@Autowired
private TransactionDefinition transactionDefinition;

@Override
public void testThirdManual() {
    // 手动开启事务
    TransactionStatus transactionStatus = platformTransactionManager.getTransaction(transactionDefinition);
    try {
        // 执行业务逻辑

        // 手动提交事务
        platformTransactionManager.commit(transactionStatus);
    } catch (Exception e) {
        log.error("error;",e);
        // 手动回滚事务
        platformTransactionManager.rollback(transactionStatus);
    }        
}
```



2、方式二

```java
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
```



## 四、多个事务管理器

在Spring中，你可以定义多个事务管理器，并指定在特定的服务或方法中使用哪个事务管理器。这在处理多个数据源时特别有用。你可以使用`@Transactional`注解并结合不同的限定符来指定应使用哪个事务管理器。

### 1、定义多个事务管理器

你需要在Spring配置中定义多个`PlatformTransactionManager` bean。每个事务管理器都与不同的数据源关联。

```java
@Configuration
public class DataSourceConfig {

    @Bean(name = "orderDataSource")
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource.order")
    public DataSource orderDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "accountDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.account")
    public DataSource accountDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "reactiveAccountDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.reactive-account")
    public DataSource reactiveAccountDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "orderTransactionManager")
    public PlatformTransactionManager orderTransactionManager(
            @Qualifier("orderDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean(name = "accountTransactionManager")
    public PlatformTransactionManager accountTransactionManager(
            @Qualifier("accountDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean(name = "reactiveAccountTransactionManager")
    public PlatformTransactionManager reactiveAccountTransactionManager(
            @Qualifier("reactiveAccountDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
```

### 2、**在服务中指定事务管理器**

使用`@Transactional`注解和`value`属性来指定某个方法或类应使用哪个事务管理器。

```java
@Service
public class TransactionalService {

    @Transactional("orderTransactionManager")
    public void processOrder() {
        // 处理订单的逻辑
    }

    @Transactional("accountTransactionManager")
    public void processAccount() {
        // 处理账户的逻辑
    }

    @Transactional("reactiveAccountTransactionManager")
    public void processReactiveAccount() {
        // 处理反应式账户的逻辑
    }
}
```

### 3、**默认事务管理器**

如果希望定义一个默认的事务管理器，当没有找到特定的事务管理器时使用，可以定义一个不带限定符或使用默认名称`transactionManager`的事务管理器。

```java
@Bean(name = "transactionManager")
public PlatformTransactionManager defaultTransactionManager(
        @Qualifier("orderDataSource") DataSource dataSource) {
    return new DataSourceTransactionManager(dataSource);
}
```

### 4、**启用事务管理**

确保在Spring配置中启用了事务管理。

```java
@EnableTransactionManagement
public class TransactionManagementConfig {
    // 配置bean（如上所述）
}
```



### 5、分析与总结

- **多个事务管理器：** 通过定义多个`PlatformTransactionManager` bean，你可以分别处理不同数据源的事务。
- **指定事务管理器：** `@Transactional`注解可以结合特定的事务管理器限定符，来指定某个方法或类应使用哪个事务管理器。
- **默认事务管理器：** 如果没有指定特定的事务管理器，则会使用默认的`transactionManager` bean。这对于确保始终有一个备用事务管理器可用非常有用。

通过配置多个事务管理器并使用`@Transactional`注解和限定符，你可以有效地管理不同数据源的事务。这种方法允许在多数据库设置中对事务行为进行细粒度的控制。





## 五、回滚规则

获胜的规则是在继承层次中最接近异常的规则。如果没有规则适用，则采用默认规则：即判断抛出的异常是否是RuntimeException类型(包含子类)或者是Error类型(包含子类)，如果是，则回滚，反之不回滚。

```java
/**
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
public void testRollBack() throws Exception {
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
```



## 六、Spring AOP原理

详细见： [Spring的AOP原理.md](F:\code\scene\aop-principle\Spring的AOP原理.md)  要求属性Spring AOP的代理对象的创建和调用过程



# 准备工作

## 一、引入依赖

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.5.6</version>
        <relativePath/> <!-- lookup parent from repository -->
    </parent>

    <groupId>com.mrlu</groupId>
    <artifactId>transaction-execution-debug</artifactId>
    <version>1.0.0</version>

    <properties>
        <java.version>1.8</java.version>
        <mysql.version>5.1.43</mysql.version>
        <commons-pool2.version>2.11.1</commons-pool2.version>
        <mybatis-plus-boot-starter.version>3.5.3.1</mybatis-plus-boot-starter.version>
        <druid-spring-boot-starter.version>1.2.16</druid-spring-boot-starter.version>
        <maven.compiler.source>8</maven.compiler.source>
        <maven.compiler.target>8</maven.compiler.target>
    </properties>

    <dependencies>
        <!--mvc-->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Mybatis-plus-boot-starter -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-boot-starter</artifactId>
            <version>${mybatis-plus-boot-starter.version}</version>
        </dependency>

        <!--数据库-->
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>druid-spring-boot-starter</artifactId>
            <version>${druid-spring-boot-starter.version}</version>
        </dependency>
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
        </dependency>

        <!--redis-->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-pool2</artifactId>
            <version>${commons-pool2.version}</version>
        </dependency>


        <!--工具包-->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-devtools</artifactId>
            <scope>runtime</scope>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-configuration-processor</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!--单元测试-->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>com.mrlu</groupId>
            <artifactId>scene-common</artifactId>
            <version>1.0.0</version>
        </dependency>

        <!--aop依赖-->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-aop</artifactId>
        </dependency>

        <!--<dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-aop</artifactId>
        </dependency>
        <dependency>
            <groupId>org.aspectj</groupId>
            <artifactId>aspectjweaver</artifactId>
            <version>1.9.7</version>
        </dependency>-->
    </dependencies>

</project>
```



## 二、SQL脚本

```sql
create table t_person(
     id int primary key auto_increment,
     name varchar(100),
     sex varchar(100),
     age int
);
create table t_animal(
     id int primary key auto_increment,
     name varchar(100),
     age int
);
```



## 三、调试代码

* entity

```sql
@Data
@Accessors(chain = true)
@TableName(value = "t_animal", autoResultMap = true)
@ToString
public class Animal implements Serializable {
    private static final long serialVersionUID = -75941339471069023L;

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String name;

    private Integer age;
}
```

```java
@Data
@Accessors(chain = true)
@TableName(value = "t_person", autoResultMap = true)
@ToString
public class Person implements Serializable {
    private static final long serialVersionUID = -75941339471069023L;

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String name;

    private Integer age;

}
```

* Mapper

```java
@Mapper
public interface AnimalMapper extends BaseMapper<Animal> {

}
```

```java
@Mapper
public interface PersonMapper extends BaseMapper<Person> {

}

```

* Service

```java
public interface PersonService extends IService<Person> {

    void canGettedBeanUseTc();

    void savePerson();

    Boolean testSaveByDeclaredTcAndManualTc();

    Boolean testFirstManual();

    Boolean testSecondManual();

    Boolean testThirdManual();

    Boolean testFourthManual();

    void testFifthManual();


    void testRequired() throws NotRunTimeException;

    void testRequiredNew() throws NotRunTimeException;

    void testSupported() throws NotRunTimeException;

    void testMandatory() throws NotRunTimeException;

    void testNotSupported() throws NotRunTimeException;

    void testNever() throws NotRunTimeException;

    void testNested() throws NotRunTimeException;

    int addDemoPerson();

    void testRollBackRule() throws NotRunTimeException, Exception;


    void testGlobalRollback();

}
```

```java
public interface AnimalService extends IService<Animal> {

    void saveAnimal();

    void manualSaveAnimal();


    void testRequired() throws NotRunTimeException;

    void testRequiredNew() throws NotRunTimeException;

    void testSupported() throws NotRunTimeException;

    void testMandatory() throws NotRunTimeException;

    void testNotSupported() throws NotRunTimeException;

    void testNever() throws NotRunTimeException;

    void testNested() throws NotRunTimeException;


}
```



```java
/**
 * @author 简单de快乐
 * @since 2023-12-04 17:15:17
 */
@Service
@Slf4j
public class PersonServiceImpl extends ServiceImpl<PersonMapper, Person> implements PersonService {

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
        throw new ServiceException("save error");
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



}
```



```java
@Service
@Slf4j
public class AnimalServiceImpl extends ServiceImpl<AnimalMapper, Animal> implements AnimalService {

    @Autowired
    private AnimalMapper animalMapper;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAnimal() {
        String name = UUID.randomUUID().toString().substring(0, 5);
        Integer age = 3;
        Animal p2 = new Animal().setName(name).setAge(age);
        save(p2);
        log.info("animal={}", p2);
        throw new ServiceException("");
    }


    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    // 默认传播行为: PROPAGATION_REQUIRED
    @Autowired
    private TransactionDefinition transactionDefinition;

    @Override
    public void manualSaveAnimal() {
        // 手动开启事务
        TransactionStatus transactionStatus = platformTransactionManager.getTransaction(transactionDefinition);
        try {
            saveDemoAnimal();
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
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void testRequired() throws NotRunTimeException {
        saveDemoAnimal();
    }


    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testRequiredNew() throws NotRunTimeException {
        saveDemoAnimal();
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS, rollbackFor = Exception.class)
    public void testSupported() throws NotRunTimeException {
        saveDemoAnimal();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void testMandatory() throws NotRunTimeException {
        saveDemoAnimal();
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED, rollbackFor = Exception.class)
    public void testNotSupported() throws NotRunTimeException {
        saveDemoAnimal();
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public void testNever() throws NotRunTimeException {
        saveDemoAnimal();
    }

    @Override
    @Transactional(propagation = Propagation.NESTED, rollbackFor = Exception.class)
    public void testNested() throws NotRunTimeException {
        saveDemoAnimal();
    }

    private void saveDemoAnimal() throws NotRunTimeException {
        String name = UUID.randomUUID().toString().substring(0, 5);
        Integer age = new Random().nextInt();
        Animal p2 = new Animal().setName(name).setAge(age);
        save(p2);
        log.info("animal={}", p2);
        // 编译时异常
        //throw new NotRunTimeException("saveDemoAnimal error");
        // 运行时异常
        //throw new ServiceException("saveDemoAnimal error");
    }




}
```

* 异常类

```java
public class CustomException extends RuntimeException {

    private String message;

    public CustomException(String message) {
        super(message);
    }

    public static class AnotherException extends RuntimeException{
        private String message;
        public AnotherException(String message) {
            super(message);
        }
    }

}
```

```java
public class CustomExceptionV2 extends RuntimeException {

    private String message;

    public CustomExceptionV2(String message) {
        super(message);
    }
}
```

```java
public class NotRunTimeException extends Exception {

    private String message;

    public NotRunTimeException(String message) {
        super(message);
    }

}
```

```java
public class ServiceException extends RuntimeException {

    private long code = ApiErrorCode.FAILED.getCode();

    private String msg;

    public ServiceException(String message) {
        super(message);
        msg = message;
    }

    public ServiceException(long code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public ServiceException(Exception exception) {
        super(exception);
        msg = getMessage();
    }

    public ServiceException(String fmt, Object... args) {
        init(fmt, args);
    }

    // 示例：throw new ServiceException(ApiErrorCode.ARGS_ERROR.getCode(), "args error;arg1=%s,arg2=%s", 1, 2);
    public ServiceException(long code, String fmt, Object... args) {
        this.code = code;
        init(fmt, args);
    }

    private void init(String fmt, Object... args) {
        if (fmt != null) {
            try {
                msg = format(getThrowableStack() + fmt, args);
            } catch (Exception e) {
                msg = "serviceException format err;fmt=" + fmt;
            }
        }
    }

    /**
     * 拿到抛异常的具体堆栈信息，方便定位
     * @return
     */
    private String getThrowableStack(){
        Throwable throwable = new Throwable();
        StackTraceElement[] stacks = throwable.getStackTrace();
        StackTraceElement stack = stacks[3];
        String className = stack.getClassName();
        return getClassName(className) + "::" + stack.getMethodName() + ":" + stack.getLineNumber() + ": ";
    }

    private static String getClassName(String cls) {
        if (cls == null) {
            return null;
        }
        int pos = cls.lastIndexOf('.');
        if (pos > 0) {
            cls = cls.substring(pos + 1);
        }
        return cls;
    }

    /**
     * 格式化
     * @param fmt
     * @param args
     */
    private static String format(String fmt, Object... args) {
        if (args == null || args.length == 0) {
            return fmt;
        }
        return String.format(fmt, args);
    }

    public long getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
```

```java
public interface IErrorCode {

    /**
     * 错误编码 -1、失败 0、成功
     */
    long getCode();

    /**
     * 错误描述
     */
    String getMsg();
}
```

```java
public enum ApiErrorCode implements IErrorCode {
    /**
     * 失败
     */
    FAILED(-1, "操作失败"),
    /**
     * 成功
     */
    SUCCESS(0, "执行成功"),

    ARGS_ERROR(-2, "参数错误"),

    PARSE_ERROR(-3, "解析错误"),

    SIGN_EXPIRED(-4, "签名失效"),

    AUTHENTICATION_ERROR(-5, "鉴权失败"),

    AUTHENTICATION_ARGS_EMPTY(-6, "鉴权参数不存在"),

    REPLAY_ERROR(-7, "重复请求错误");

    private final long code;
    private final String msg;

    ApiErrorCode(final long code, final String msg) {
        this.code = code;
        this.msg = msg;
    }

    public static ApiErrorCode fromCode(long code) {
        ApiErrorCode[] ecs = ApiErrorCode.values();
        for (ApiErrorCode ec : ecs) {
            if (ec.getCode() == code) {
                return ec;
            }
        }
        return SUCCESS;
    }

    @Override
    public long getCode() {
        return code;
    }

    @Override
    public String getMsg() {
        return msg;
    }

    @Override
    public String toString() {
        return String.format(" ErrorCode:{code=%s, msg=%s} ", code, msg);
    }
}
```

## 四、主启动类

```java
@SpringBootApplication
@EnableTransactionManagement
public class TcExecAppServer {
    public static void main(String[] args) {
        ConfigurableApplicationContext application = SpringApplication.run(TcExecAppServer.class, args);
    }
}
```



# 整体分析

* 声明式事务

Spring 事务的本质是通过AOP来实现的。使用@EnableTransactionManagement注解，引入增强器，然后拦截目标方法的调用。在增强器中执行开启、提交、回滚事务等操作。

* 编程式事务

编程式事务底层也是使用事务增强器里的相关方法，来完成开启、提交、回滚事务等操作

* @EnableTransactionManagement

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(TransactionManagementConfigurationSelector.class)
public @interface EnableTransactionManagement {

	/**
	 * Indicate whether subclass-based (CGLIB) proxies are to be created ({@code true}) as
	 * opposed to standard Java interface-based proxies ({@code false}). The default is
	 * {@code false}. <strong>Applicable only if {@link #mode()} is set to
	 * {@link AdviceMode#PROXY}</strong>.
	 * <p>Note that setting this attribute to {@code true} will affect <em>all</em>
	 * Spring-managed beans requiring proxying, not just those marked with
	 * {@code @Transactional}. For example, other beans marked with Spring's
	 * {@code @Async} annotation will be upgraded to subclass proxying at the same
	 * time. This approach has no negative impact in practice unless one is explicitly
	 * expecting one type of proxy vs another, e.g. in tests.
	 */
	boolean proxyTargetClass() default false;

	/**
	 * Indicate how transactional advice should be applied.
	 * <p><b>The default is {@link AdviceMode#PROXY}.</b>
	 * Please note that proxy mode allows for interception of calls through the proxy
	 * only. Local calls within the same class cannot get intercepted that way; an
	 * {@link Transactional} annotation on such a method within a local call will be
	 * ignored since Spring's interceptor does not even kick in for such a runtime
	 * scenario. For a more advanced mode of interception, consider switching this to
	 * {@link AdviceMode#ASPECTJ}.
	 */
	AdviceMode mode() default AdviceMode.PROXY;

	/**
	 * Indicate the ordering of the execution of the transaction advisor
	 * when multiple advices are applied at a specific joinpoint.
	 * <p>The default is {@link Ordered#LOWEST_PRECEDENCE}.
	 */
	int order() default Ordered.LOWEST_PRECEDENCE;

}
```

@EnableTransactionManagement 引入TransactionManagementConfigurationSelector注册类

* TransactionManagementConfigurationSelector

```java
public class TransactionManagementConfigurationSelector extends AdviceModeImportSelector<EnableTransactionManagement> {

	/**
	 * Returns {@link ProxyTransactionManagementConfiguration} or
	 * {@code AspectJ(Jta)TransactionManagementConfiguration} for {@code PROXY}
	 * and {@code ASPECTJ} values of {@link EnableTransactionManagement#mode()},
	 * respectively.
	 */
	@Override
	protected String[] selectImports(AdviceMode adviceMode) {
		switch (adviceMode) {
			case PROXY:
                  // 默认为PROXY代理方式，
				return new String[] {AutoProxyRegistrar.class.getName(),
                          // 注册代理事务管理配置类           
						ProxyTransactionManagementConfiguration.class.getName()};
			case ASPECTJ:
				return new String[] {determineTransactionAspectClass()};
			default:
				return null;
		}
	}

	private String determineTransactionAspectClass() {
		return (ClassUtils.isPresent("javax.transaction.Transactional", getClass().getClassLoader()) ?
				TransactionManagementConfigUtils.JTA_TRANSACTION_ASPECT_CONFIGURATION_CLASS_NAME :
				TransactionManagementConfigUtils.TRANSACTION_ASPECT_CONFIGURATION_CLASS_NAME);
	}

}
```



## 一、ProxyTransactionManagementConfiguration

```java
@Configuration(proxyBeanMethods = false)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class ProxyTransactionManagementConfiguration extends AbstractTransactionManagementConfiguration {
	
    // 增强器
	@Bean(name = TransactionManagementConfigUtils.TRANSACTION_ADVISOR_BEAN_NAME)
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public BeanFactoryTransactionAttributeSourceAdvisor transactionAdvisor(
			TransactionAttributeSource transactionAttributeSource, TransactionInterceptor transactionInterceptor) {

		BeanFactoryTransactionAttributeSourceAdvisor advisor = new BeanFactoryTransactionAttributeSourceAdvisor();
        // 设置注解属性
		advisor.setTransactionAttributeSource(transactionAttributeSource);
         // 设置通知
		advisor.setAdvice(transactionInterceptor);
		if (this.enableTx != null) {
			advisor.setOrder(this.enableTx.<Integer>getNumber("order"));
		}
		return advisor;
	}
    
    // 注解属性
	@Bean
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public TransactionAttributeSource transactionAttributeSource() {
		return new AnnotationTransactionAttributeSource();
	}
    
    // 事务拦截器（通知）   
	@Bean
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public TransactionInterceptor transactionInterceptor(TransactionAttributeSource transactionAttributeSource) {
		TransactionInterceptor interceptor = new TransactionInterceptor();
         // 设置事务注解属性来源（AnnotationTransactionAttributeSource）
		interceptor.setTransactionAttributeSource(transactionAttributeSource);
		// 设置事务管理器（这里没有设置）
		if (this.txManager != null) {
			interceptor.setTransactionManager(this.txManager);
		}
		return interceptor;
	}

}
```



```java
@Configuration
public abstract class AbstractTransactionManagementConfiguration implements ImportAware {

	@Nullable
	protected AnnotationAttributes enableTx;

	/**
	 * Default transaction manager, as configured through a {@link TransactionManagementConfigurer}.
	 */
	@Nullable
	protected TransactionManager txManager;


	@Override
	public void setImportMetadata(AnnotationMetadata importMetadata) {
		this.enableTx = AnnotationAttributes.fromMap(
				importMetadata.getAnnotationAttributes(EnableTransactionManagement.class.getName(), false));
		if (this.enableTx == null) {
			throw new IllegalArgumentException(
					"@EnableTransactionManagement is not present on importing class " + importMetadata.getClassName());
		}
	}
	
    // 在当前示例中Collection<TransactionManagementConfigurer> configurers不存在
	@Autowired(required = false)
	void setConfigurers(Collection<TransactionManagementConfigurer> configurers) {
		if (CollectionUtils.isEmpty(configurers)) {
			return;
		}
		if (configurers.size() > 1) {
			throw new IllegalStateException("Only one TransactionManagementConfigurer may exist");
		}
		TransactionManagementConfigurer configurer = configurers.iterator().next();
		this.txManager = configurer.annotationDrivenTransactionManager();
	}


	@Bean(name = TransactionManagementConfigUtils.TRANSACTIONAL_EVENT_LISTENER_FACTORY_BEAN_NAME)
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public static TransactionalEventListenerFactory transactionalEventListenerFactory() {
		return new TransactionalEventListenerFactory();
	}

}

```

![048bc1024e0b0cb1cbb79af8d79a587](https://lu-note.oss-cn-shenzhen.aliyuncs.com/notes/work/048bc1024e0b0cb1cbb79af8d79a587.png)

通过AOP的源码分析可以知道，主要的增强逻辑位于增强器里的通知中。所以我们重点关注TransactionInterceptor的invoke方法



## 二、TransactionInterceptor

```java
public class TransactionInterceptor extends TransactionAspectSupport implements MethodInterceptor, Serializable {
    // ....

	@Override
	@Nullable
	public Object invoke(MethodInvocation invocation) throws Throwable {
		// Work out the target class: may be {@code null}.
		// The TransactionAttributeSource should be passed the target class
		// as well as the method, which may be from an interface.
         // 获取目标类
		Class<?> targetClass = (invocation.getThis() != null ? AopUtils.getTargetClass(invocation.getThis()) : null);
         // 执行事务调用
		// Adapt to TransactionAspectSupport's invokeWithinTransaction...
		return invokeWithinTransaction(invocation.getMethod(), targetClass, new CoroutinesInvocationCallback() {
            
			@Override
			@Nullable
			public Object proceedWithInvocation() throws Throwable {
                  // 从AOP源码分析可知，如果调用到了AOP拦截器链的最后一个拦截器，proceed方法就是执行目标方法。
                  // 反之，则继续调用下一个拦截器
                  // 为了方便后面源码说明，我们统一称proceedWithInvocation方法为目标(业务逻辑)方法。 
				return invocation.proceed();
			}
			@Override
			public Object getTarget() {
				return invocation.getThis();
			}
			@Override
			public Object[] getArguments() {
				return invocation.getArguments();
			}
		});
	}

	// ....

}
```



* invokeWithinTransaction

```java
/**
 * 用于基于around-advice的子类的通用委托，委托给该类上的其他几个模板方法。能够处理
 * CallbackPreferringPlatformTransactionManager以及响应式返回类型的
 * 常规PlatformTransactionManager实现和ReactiveTransactionManager实现。
 * 
 * General delegate for around-advice-based subclasses, delegating to several other template
 * methods on this class. Able to handle {@link CallbackPreferringPlatformTransactionManager}
 * as well as regular {@link PlatformTransactionManager} implementations and
 * {@link ReactiveTransactionManager} implementations for reactive return types.
 * @param method the Method being invoked 被调用的方法
 * @param targetClass the target class that we're invoking the method on 我们正在调用该方法的目标类
 * @param invocation the callback to use for proceeding with the target invocation 用于处理目标调用的回调
 * @return the return value of the method, if any 方法的返回值(如果有的话)
 * @throws Throwable propagated from the target invocation 
 */
@Nullable
protected Object invokeWithinTransaction(Method method, @Nullable Class<?> targetClass,
        final InvocationCallback invocation) throws Throwable {

    // If the transaction attribute is null, the method is non-transactional.
    TransactionAttributeSource tas = getTransactionAttributeSource();
    final TransactionAttribute txAttr = (tas != null ? tas.getTransactionAttribute(method, targetClass) : null);
    final TransactionManager tm = determineTransactionManager(txAttr);

    if (this.reactiveAdapterRegistry != null && tm instanceof ReactiveTransactionManager) {
        boolean isSuspendingFunction = KotlinDetector.isSuspendingFunction(method);
        boolean hasSuspendingFlowReturnType = isSuspendingFunction &&
                COROUTINES_FLOW_CLASS_NAME.equals(new MethodParameter(method, -1).getParameterType().getName());
        if (isSuspendingFunction && !(invocation instanceof CoroutinesInvocationCallback)) {
            throw new IllegalStateException("Coroutines invocation not supported: " + method);
        }
        CoroutinesInvocationCallback corInv = (isSuspendingFunction ? (CoroutinesInvocationCallback) invocation : null);

        ReactiveTransactionSupport txSupport = this.transactionSupportCache.computeIfAbsent(method, key -> {
            Class<?> reactiveType =
                    (isSuspendingFunction ? (hasSuspendingFlowReturnType ? Flux.class : Mono.class) : method.getReturnType());
            ReactiveAdapter adapter = this.reactiveAdapterRegistry.getAdapter(reactiveType);
            if (adapter == null) {
                throw new IllegalStateException("Cannot apply reactive transaction to non-reactive return type: " +
                        method.getReturnType());
            }
            return new ReactiveTransactionSupport(adapter);
        });

        InvocationCallback callback = invocation;
        if (corInv != null) {
            callback = () -> CoroutinesUtils.invokeSuspendingFunction(method, corInv.getTarget(), corInv.getArguments());
        }
        Object result = txSupport.invokeWithinTransaction(method, targetClass, callback, txAttr, (ReactiveTransactionManager) tm);
        if (corInv != null) {
            Publisher<?> pr = (Publisher<?>) result;
            return (hasSuspendingFlowReturnType ? KotlinDelegate.asFlow(pr) :
                    KotlinDelegate.awaitSingleOrNull(pr, corInv.getContinuation()));
        }
        return result;
    }

    PlatformTransactionManager ptm = asPlatformTransactionManager(tm);
    final String joinpointIdentification = methodIdentification(method, targetClass, txAttr);

    if (txAttr == null || !(ptm instanceof CallbackPreferringPlatformTransactionManager)) {
        // Standard transaction demarcation with getTransaction and commit/rollback calls.
        TransactionInfo txInfo = createTransactionIfNecessary(ptm, txAttr, joinpointIdentification);

        Object retVal;
        try {
            // This is an around advice: Invoke the next interceptor in the chain.
            // This will normally result in a target object being invoked.
            retVal = invocation.proceedWithInvocation();
        }
        catch (Throwable ex) {
            // target invocation exception
            completeTransactionAfterThrowing(txInfo, ex);
            throw ex;
        }
        finally {
            cleanupTransactionInfo(txInfo);
        }

        if (retVal != null && vavrPresent && VavrDelegate.isVavrTry(retVal)) {
            // Set rollback-only in case of Vavr failure matching our rollback rules...
            TransactionStatus status = txInfo.getTransactionStatus();
            if (status != null && txAttr != null) {
                retVal = VavrDelegate.evaluateTryFailure(retVal, txAttr, status);
            }
        }

        commitTransactionAfterReturning(txInfo);
        return retVal;
    }

    else {
        Object result;
        final ThrowableHolder throwableHolder = new ThrowableHolder();

        // It's a CallbackPreferringPlatformTransactionManager: pass a TransactionCallback in.
        try {
            result = ((CallbackPreferringPlatformTransactionManager) ptm).execute(txAttr, status -> {
                TransactionInfo txInfo = prepareTransactionInfo(ptm, txAttr, joinpointIdentification, status);
                try {
                    Object retVal = invocation.proceedWithInvocation();
                    if (retVal != null && vavrPresent && VavrDelegate.isVavrTry(retVal)) {
                        // Set rollback-only in case of Vavr failure matching our rollback rules...
                        retVal = VavrDelegate.evaluateTryFailure(retVal, txAttr, status);
                    }
                    return retVal;
                }
                catch (Throwable ex) {
                    if (txAttr.rollbackOn(ex)) {
                        // A RuntimeException: will lead to a rollback.
                        if (ex instanceof RuntimeException) {
                            throw (RuntimeException) ex;
                        }
                        else {
                            throw new ThrowableHolderException(ex);
                        }
                    }
                    else {
                        // A normal return value: will lead to a commit.
                        throwableHolder.throwable = ex;
                        return null;
                    }
                }
                finally {
                    cleanupTransactionInfo(txInfo);
                }
            });
        }
        catch (ThrowableHolderException ex) {
            throw ex.getCause();
        }
        catch (TransactionSystemException ex2) {
            if (throwableHolder.throwable != null) {
                logger.error("Application exception overridden by commit exception", throwableHolder.throwable);
                ex2.initApplicationException(throwableHolder.throwable);
            }
            throw ex2;
        }
        catch (Throwable ex2) {
            if (throwableHolder.throwable != null) {
                logger.error("Application exception overridden by commit exception", throwableHolder.throwable);
            }
            throw ex2;
        }

        // Check result state: It might indicate a Throwable to rethrow.
        if (throwableHolder.throwable != null) {
            throw throwableHolder.throwable;
        }
        return result;
    }
}
```



本示例注入的是JdbcTransactionManager 事务管理器（PlatformTransactionManager类型），不属于响应式编程事务管理器类型，

也不属于回调首选平台事务管理器CallbackPreferringPlatformTransactionManager类型。所以我们把响应式编程事务管理器和

回调首选平台事务管理器CallbackPreferringPlatformTransactionManager的逻辑去掉，**重点关注以下的逻辑**。

```java
protected Object invokeWithinTransaction(Method method, @Nullable Class<?> targetClass,
        final InvocationCallback invocation) throws Throwable {		
    // If the transaction attribute is null, the method is non-transactional.
     // 获取事务注解属性来源（AnnotationTransactionAttributeSource）
    TransactionAttributeSource tas = getTransactionAttributeSource();
     // 1、获取事务属性
    final TransactionAttribute txAttr = (tas != null ? tas.getTransactionAttribute(method, targetClass) : null);
     // 2、确定事务管理器
    final TransactionManager tm = determineTransactionManager(txAttr);

     // 转换成PlatformTransactionManager类型，然后返回
    PlatformTransactionManager ptm = asPlatformTransactionManager(tm);
     // 获取连接点标识（完整的方法名：类名 + 方法名）。例如com.mrlu.tx.service.impl.PersonServiceImpl.testRequired
    final String joinpointIdentification = methodIdentification(method, targetClass, txAttr);

    if (txAttr == null || !(ptm instanceof CallbackPreferringPlatformTransactionManager)) {
         // 3、创建事务信息（如果有必要的话）
        // 使用getTransaction和提交/回滚调用进行标准事务划分。
        // Standard transaction demarcation with getTransaction and commit/rollback calls.
        TransactionInfo txInfo = createTransactionIfNecessary(ptm, txAttr, joinpointIdentification);

        Object retVal;
        try {
            //这是一个环绕通知:调用链中的下一个拦截器。这通常会导致目标对象被调用。            
            // This is an around advice: Invoke the next interceptor in the chain.
            // This will normally result in a target object being invoked.
            // 4、调用目标方法
            retVal = invocation.proceedWithInvocation();
        }
        catch (Throwable ex) {                  
              // 处理一个可抛出对象，完成事务。我们可以提交或回滚，这取决于配置。
              // 5、抛出异常后处理事务
            // target invocation exception
            completeTransactionAfterThrowing(txInfo, ex);
            // 处理完成后，抛出异常。（让其他调用者处理）
            throw ex;
        }
        finally {
              // 6、清除事务信息（回退成旧的事务信息，如果旧的没有就是清除了）
            cleanupTransactionInfo(txInfo);
        }

         // 这里也不用管
        if (retVal != null && vavrPresent && VavrDelegate.isVavrTry(retVal)) {
            // Set rollback-only in case of Vavr failure matching our rollback rules...
            TransactionStatus status = txInfo.getTransactionStatus();
            if (status != null && txAttr != null) {
                retVal = VavrDelegate.evaluateTryFailure(retVal, txAttr, status);
            }
        }

         // 7、提交事务
        commitTransactionAfterReturning(txInfo);
        return retVal;
    }		
}

// 转换成PlatformTransactionManager类型
@Nullable
private PlatformTransactionManager asPlatformTransactionManager(@Nullable Object transactionManager) {
    if (transactionManager == null || transactionManager instanceof PlatformTransactionManager) {
        return (PlatformTransactionManager) transactionManager;
    }
    else {
        throw new IllegalStateException(
                "Specified transaction manager is not a PlatformTransactionManager: " + transactionManager);
    }
}

// 获取连接点标识（完整的方法名：类名 + 方法名）
private String methodIdentification(Method method, @Nullable Class<?> targetClass,
        @Nullable TransactionAttribute txAttr) {
    String methodIdentification = methodIdentification(method, targetClass);
    if (methodIdentification == null) {
        // RuleBasedTransactionAttribute
        if (txAttr instanceof DefaultTransactionAttribute) {
            // 执行到这里。获取事务注解属性描述符（类名 + 方法名）
            methodIdentification = ((DefaultTransactionAttribute) txAttr).getDescriptor();
        }
        if (methodIdentification == null) {
            methodIdentification = ClassUtils.getQualifiedMethodName(method, targetClass);
        }
    }
    return methodIdentification;
}
protected String methodIdentification(Method method, @Nullable Class<?> targetClass) {
    return null;
}
```

* JdbcTransactionManager

![image-20240724175042312](https://lu-note.oss-cn-shenzhen.aliyuncs.com/notes/work/image-20240724175042312.png)

# 详细分析

##  一、获取事务属性

* 获取事务注解属性来源

```java
/**
 * Return the transaction attribute source.
 */
@Nullable
public TransactionAttributeSource getTransactionAttributeSource() {
    // AnnotationTransactionAttributeSource
    return this.transactionAttributeSource;
}
```

根据事务注解属性来源，获取事务注解属性

```java
/**
 * Determine the transaction attribute for this method invocation.
 * <p>Defaults to the class's transaction attribute if no method attribute is found.
 * @param method the method for the current invocation (never {@code null})
 * @param targetClass the target class for this invocation (may be {@code null})
 * @return a TransactionAttribute for this method, or {@code null} if the method
 * is not transactional
 */
@Override
@Nullable
public TransactionAttribute getTransactionAttribute(Method method, @Nullable Class<?> targetClass) {
    // 如果方法定义的类为Object，直接返回null
    if (method.getDeclaringClass() == Object.class) {
        return null;
    }
	
    // 获取缓存的key
    // First, see if we have a cached value.
    Object cacheKey = getCacheKey(method, targetClass);
    // 从缓存中获取事务的注解属性。
    TransactionAttribute cached = this.attributeCache.get(cacheKey);
    if (cached != null) {
        // 存在缓存。缓存的值不是缓存的空对象，则返回缓存的值。反之，返回null
        // 值要么是规范值，表示没有事务属性;或一个实际的事务属性。
        // Value will either be canonical value indicating there is no transaction attribute,
        // or an actual transaction attribute.
        if (cached == NULL_TRANSACTION_ATTRIBUTE) {
            return null;
        }
        else {
            return cached;
        }
    }
    else {
        // 缓存不存在
        // 计算事务的注解属性
        // We need to work it out.
        TransactionAttribute txAttr = computeTransactionAttribute(method, targetClass);
        // Put it in the cache.
        if (txAttr == null) {
            // 计算的事务注解属性不存在，则缓存空对象
            this.attributeCache.put(cacheKey, NULL_TRANSACTION_ATTRIBUTE);
        }
        else {
            // 计算的事务注解属性存在
            // 获取完整的方法名称（类名+方法名）
            String methodIdentification = ClassUtils.getQualifiedMethodName(method, targetClass);
            if (txAttr instanceof DefaultTransactionAttribute) {
                DefaultTransactionAttribute dta = (DefaultTransactionAttribute) txAttr;
                // 设置成事务注解属性的描述符
                dta.setDescriptor(methodIdentification);
                // 设置属性解析器（解析占位符）
                dta.resolveAttributeStrings(this.embeddedValueResolver);
            }
            if (logger.isTraceEnabled()) {
                logger.trace("Adding transactional method '" + methodIdentification + "' with attribute: " + txAttr);
            }
            // 添加结果到缓存
            this.attributeCache.put(cacheKey, txAttr);
        }
        return txAttr;
    }
}

// 获取缓存的key
protected Object getCacheKey(Method method, @Nullable Class<?> targetClass) {
    return new MethodClassKey(method, targetClass);
}

@Nullable
private transient StringValueResolver embeddedValueResolver;

/**
 * Cache of TransactionAttributes, keyed by method on a specific target class.
 * <p>As this base class is not marked Serializable, the cache will be recreated
 * after serialization - provided that the concrete subclass is Serializable.
 */
private final Map<Object, TransactionAttribute> attributeCache = new ConcurrentHashMap<>(1024);

// 缓存的空对象
private static final TransactionAttribute NULL_TRANSACTION_ATTRIBUTE = new DefaultTransactionAttribute() {
    @Override
    public String toString() {
        return "null";
    }
};
```

【注意】这里实际上会命中缓存。因为在创建代理对象时，判断事务增强器是否为符合条件的增强器时，会走非缓存逻辑，执行完成会缓存结果，所以我们这里会命中缓存。具体分析参考 [Spring的AOP原理.md](F:\code\scene\aop-custom\Spring的AOP原理.md) 。虽然分析AOP的时候我们已经说明过了，但是这里我们还是分析一下事务的注解属性是如何解析的。



### 1、计算事务注解属性

```java
protected TransactionAttribute computeTransactionAttribute(Method method, @Nullable Class<?> targetClass) {
    // Don't allow no-public methods as required.
    // 如果方法不是public权限，返回null。
    if (allowPublicMethodsOnly() && !Modifier.isPublic(method.getModifiers())) {
        return null;
    }
    
    // 优先从目标类的具体方法获取事务属性
    // The method may be on an interface, but we need attributes from the target class.
    // If the target class is null, the method will be unchanged.
    Method specificMethod = AopUtils.getMostSpecificMethod(method, targetClass);
    // First try is the method in the target class.
    TransactionAttribute txAttr = findTransactionAttribute(specificMethod);
    if (txAttr != null) {
        return txAttr;
    }
    
    // 如果具体方法没有找到事务属性，尝试从目标类本身获取事务属性
    // Second try is the transaction attribute on the target class.
    txAttr = findTransactionAttribute(specificMethod.getDeclaringClass());
    if (txAttr != null && ClassUtils.isUserLevelMethod(method)) {
        return txAttr;
    }
    
    // 回退到原始方法
    // 如果最具体的方法和原始方法不同，再次尝试从原始方法和其声明的类上获取事务属性，作为最后的回退方案。
    if (specificMethod != method) {
        // Fallback is to look at the original method.
        txAttr = findTransactionAttribute(method);
        if (txAttr != null) {
            return txAttr;
        }
        // Last fallback is the class of the original method.
        txAttr = findTransactionAttribute(method.getDeclaringClass());
        if (txAttr != null && ClassUtils.isUserLevelMethod(method)) {
            return txAttr;
        }
    }

    return null;
}
```



```java
@Override
@Nullable
protected TransactionAttribute findTransactionAttribute(Class<?> clazz) {
     // 获取类上的事务注解属性
    return determineTransactionAttribute(clazz);
}

@Override
@Nullable
protected TransactionAttribute findTransactionAttribute(Method method) {
    // 获取方法上的事务注解属性
    return determineTransactionAttribute(method);
}

@Nullable
protected TransactionAttribute determineTransactionAttribute(AnnotatedElement element) {
    for (TransactionAnnotationParser parser : this.annotationParsers) {
        // 通过解析器解析注解（SpringTransactionAnnotationParser）
        TransactionAttribute attr = parser.parseTransactionAnnotation(element);
        if (attr != null) {
            return attr;
        }
    }
    return null;
}
```



解析器是什么时候有的呢？？？

在ProxyTransactionManagementConfiguration中创建AnnotationTransactionAttributeSource，AnnotationTransactionAttributeSource就会创建事务注解解析器。

```java
public class AnnotationTransactionAttributeSource extends AbstractFallbackTransactionAttributeSource
		implements Serializable {

	private static final boolean jta12Present;

	private static final boolean ejb3Present;

	static {
         // 没有这两个类存在
		ClassLoader classLoader = AnnotationTransactionAttributeSource.class.getClassLoader();
		jta12Present = ClassUtils.isPresent("javax.transaction.Transactional", classLoader);
		ejb3Present = ClassUtils.isPresent("javax.ejb.TransactionAttribute", classLoader);
	}

	private final boolean publicMethodsOnly;

	private final Set<TransactionAnnotationParser> annotationParsers;


	/**
	 * Create a default AnnotationTransactionAttributeSource, supporting
	 * public methods that carry the {@code Transactional} annotation
	 * or the EJB3 {@link javax.ejb.TransactionAttribute} annotation.
	 */
	public AnnotationTransactionAttributeSource() {
         // 只允许public方法
		this(true);
	}

	/**
	 * Create a custom AnnotationTransactionAttributeSource, supporting
	 * public methods that carry the {@code Transactional} annotation
	 * or the EJB3 {@link javax.ejb.TransactionAttribute} annotation.
	 * @param publicMethodsOnly whether to support public methods that carry
	 * the {@code Transactional} annotation only (typically for use
	 * with proxy-based AOP), or protected/private methods as well
	 * (typically used with AspectJ class weaving)
	 */
	public AnnotationTransactionAttributeSource(boolean publicMethodsOnly) {
         // 只允许public方法
		this.publicMethodsOnly = publicMethodsOnly;        
		if (jta12Present || ejb3Present) {
			this.annotationParsers = new LinkedHashSet<>(4);
			this.annotationParsers.add(new SpringTransactionAnnotationParser());
			if (jta12Present) {
				this.annotationParsers.add(new JtaTransactionAnnotationParser());
			}
			if (ejb3Present) {
				this.annotationParsers.add(new Ejb3TransactionAnnotationParser());
			}
		}
		else {
             // 添加事务注解解析器
			this.annotationParsers = Collections.singleton(new SpringTransactionAnnotationParser());
		}
	}
}
```



#### 1.1 解析器解析注解

* SpringTransactionAnnotationParser

```java
@Override
@Nullable
public TransactionAttribute parseTransactionAnnotation(AnnotatedElement element) {
    // 获取@Transactional注解属性
    AnnotationAttributes attributes = AnnotatedElementUtils.findMergedAnnotationAttributes(
            element, Transactional.class, false, false);
    if (attributes != null) {
        // 解析@Transactional注解属性并返回
        return parseTransactionAnnotation(attributes);
    }
    else {
        return null;
    }
}

// 解析@Transactional注解属性并返回
protected TransactionAttribute parseTransactionAnnotation(AnnotationAttributes attributes) {
    // 创建基于规则的事务属性
    RuleBasedTransactionAttribute rbta = new RuleBasedTransactionAttribute();
    
    // 获取并设置传播行为。默认是PROPAGATION_REQUIRED
    Propagation propagation = attributes.getEnum("propagation");
    rbta.setPropagationBehavior(propagation.value());
    // 获取并设置隔离级别
    Isolation isolation = attributes.getEnum("isolation");
    rbta.setIsolationLevel(isolation.value());
    
    // 获取并设置超时时间
    rbta.setTimeout(attributes.getNumber("timeout").intValue());
    String timeoutString = attributes.getString("timeoutString");
    Assert.isTrue(!StringUtils.hasText(timeoutString) || rbta.getTimeout() < 0,
            "Specify 'timeout' or 'timeoutString', not both");
    rbta.setTimeoutString(timeoutString);
    
    // 设置是否只读
    rbta.setReadOnly(attributes.getBoolean("readOnly"));
    // 设置指定的事务管理器bean名称
    rbta.setQualifier(attributes.getString("value"));
    // 设置labels
    rbta.setLabels(Arrays.asList(attributes.getStringArray("label")));
    
    List<RollbackRuleAttribute> rollbackRules = new ArrayList<>();
    for (Class<?> rbRule : attributes.getClassArray("rollbackFor")) {
        // 设置回滚规则
        rollbackRules.add(new RollbackRuleAttribute(rbRule));
    }
    for (String rbRule : attributes.getStringArray("rollbackForClassName")) {
        // 设置回滚规则
        rollbackRules.add(new RollbackRuleAttribute(rbRule));
    }
    for (Class<?> rbRule : attributes.getClassArray("noRollbackFor")) {
		// 设置非回滚规则
        rollbackRules.add(new NoRollbackRuleAttribute(rbRule));
    }
    for (String rbRule : attributes.getStringArray("noRollbackForClassName")) {
        // 设置非回滚规则
        rollbackRules.add(new NoRollbackRuleAttribute(rbRule));
    }
    // 设置所有的规则
    rbta.setRollbackRules(rollbackRules);
	// 返回基于规则的事务属性
    return rbta;
}
```

* RuleBasedTransactionAttribute

![image-20240724180633289](https://lu-note.oss-cn-shenzhen.aliyuncs.com/notes/work/image-20240724180633289.png)



### 2、总结

先从缓存获取事务注解属性，如果有缓存，则返回（空对象返回null）。反之，通过事务注解解析器解析方法或者类上的事务注解，返回事务注解属性。



##  二、确定事务管理器

```java
/**
 * 确定给定事务使用的具体事务管理器
 * Determine the specific transaction manager to use for the given transaction.
 */
@Nullable
protected TransactionManager determineTransactionManager(@Nullable TransactionAttribute txAttr) {
    // Do not attempt to lookup tx manager if no tx attributes are set
    // 如果事务属性为null或者beanFactory为null。 这里我们获取到的事务属性不为空，不会执行if
    if (txAttr == null || this.beanFactory == null) {
        // 获取事务拦截器TransactionInterceptor里指定的事务管理器。
        // ProxyTransactionManagementConfiguration中创建的TransactionInterceptor没有指定
        return getTransactionManager();
    }
    
    // 获取事务属性里指定的事务管理器bean名称（即@Transactional注解指定value或者transactionManager属性值）
    String qualifier = txAttr.getQualifier();
    if (StringUtils.hasText(qualifier)) {
        // 根据qualifier设置的bean名称，从beanFactory中获取相应名称的事务管理器
        return determineQualifiedTransactionManager(this.beanFactory, qualifier);
    }
    // 事务拦截器TransactionInterceptor指定的事务拦截器的bean名称
    //ProxyTransactionManagementConfiguration中创建的TransactionInterceptor没有指定。这个if也不进入
    else if (StringUtils.hasText(this.transactionManagerBeanName)) {
        // 根据transactionManagerBeanName设置的bean名称，从beanFactory中获取相应名称的事务管理器
        return determineQualifiedTransactionManager(this.beanFactory, this.transactionManagerBeanName);
    }
    else {
         // 获取事务拦截器TransactionInterceptor里指定的事务管理器。
        // ProxyTransactionManagementConfiguration中创建的TransactionInterceptor没有指定
        TransactionManager defaultTransactionManager = getTransactionManager();
        if (defaultTransactionManager == null) {
            // 从缓存获取
            defaultTransactionManager = this.transactionManagerCache.get(DEFAULT_TRANSACTION_MANAGER_KEY);
            if (defaultTransactionManager == null) {
                // 缓存不存在，从beanFactory获取
                defaultTransactionManager = this.beanFactory.getBean(TransactionManager.class);
                // 添加结果到缓存
                this.transactionManagerCache.putIfAbsent(
                        DEFAULT_TRANSACTION_MANAGER_KEY, defaultTransactionManager);
            }
        }
        // 返回结果
        return defaultTransactionManager;
    }
}

// 从beanFactory中获取相应名称的事务管理器
private TransactionManager determineQualifiedTransactionManager(BeanFactory beanFactory, String qualifier) {
    // 从缓存中获取
    TransactionManager txManager = this.transactionManagerCache.get(qualifier);
    if (txManager == null) {
        // 缓存不存在，从beanFactory获取
        txManager = BeanFactoryAnnotationUtils.qualifiedBeanOfType(
                beanFactory, TransactionManager.class, qualifier);
        // 添加结果到缓存
        this.transactionManagerCache.putIfAbsent(qualifier, txManager);
    }
    return txManager;
}

/**
 * Key to use to store the default transaction manager.
 */
private static final Object DEFAULT_TRANSACTION_MANAGER_KEY = new Object();

private final ConcurrentMap<Object, TransactionManager> transactionManagerCache =
			new ConcurrentReferenceHashMap<>(4);

@Nullable
private TransactionManager transactionManager;

@Nullable
private String transactionManagerBeanName;

@Nullable
public TransactionManager getTransactionManager() {
	return this.transactionManager;
}
```

这里我们获取到**JdbcTransactionManager**

![image-20240724175042312](https://lu-note.oss-cn-shenzhen.aliyuncs.com/notes/work/image-20240724175042312.png)

JdbcTransactionManager如何来的呢？？？

![image-20240724175553777](https://lu-note.oss-cn-shenzhen.aliyuncs.com/notes/work/image-20240724175553777.png)

通过SpringBoot的自动配置类DataSourceTransactionManagerAutoConfiguration注入的





## 三、创建事务信息（如果有必要的话）

```java
/**
 * 必要时根据给定的TransactionAttribute创建事务。
 * 允许调用者通过TransactionAttributeSource执行自定义的TransactionAttribute查找。
 *
 * Create a transaction if necessary based on the given TransactionAttribute.
 * <p>Allows callers to perform custom TransactionAttribute lookups through
 * the TransactionAttributeSource.
 * @param txAttr the TransactionAttribute (may be {@code null})
 * @param joinpointIdentification the fully qualified method name
 * (used for monitoring and logging purposes)
 * @return a TransactionInfo object, whether or not a transaction was created.
 * The {@code hasTransaction()} method on TransactionInfo can be used to
 * tell if there was a transaction created.
 * @see #getTransactionAttributeSource()
 */
@SuppressWarnings("serial")
protected TransactionInfo createTransactionIfNecessary(@Nullable PlatformTransactionManager tm,
        @Nullable TransactionAttribute txAttr, final String joinpointIdentification) {
    // 如果未指定名称，则应用方法标识(类名 + 方法名)作为事务名称。
    // If no name specified, apply method identification as transaction name.
    if (txAttr != null && txAttr.getName() == null) {
        // 包装成代理的事务属性DelegatingTransactionAttribute的实现类
        txAttr = new DelegatingTransactionAttribute(txAttr) {
            @Override
            public String getName() {
                // 连接点标识（完整的方法名：类名 + 方法名）
                return joinpointIdentification;
            }
        };
    }

    TransactionStatus status = null;
    if (txAttr != null) {
        if (tm != null) {
            // 1、获取事务状态
            status = tm.getTransaction(txAttr);
        }
        else {
            if (logger.isDebugEnabled()) {
                logger.debug("Skipping transactional joinpoint [" + joinpointIdentification +
                        "] because no transaction manager has been configured");
            }
        }
    }
    
    // 2、准备事务信息
    return prepareTransactionInfo(tm, txAttr, joinpointIdentification, status);
}
```

### 1、 获取事务状态

```java
/**
 * 这个实现处理传播行为。委托给doGetTransaction, isExistingTransaction和doBegin。
 * This implementation handles propagation behavior. Delegates to
 * {@code doGetTransaction}, {@code isExistingTransaction}
 * and {@code doBegin}.
 * @see #doGetTransaction
 * @see #isExistingTransaction
 * @see #doBegin
 */
@Override
public final TransactionStatus getTransaction(@Nullable TransactionDefinition definition)
        throws TransactionException {
	// 这里我们获取到的事务属性不为空。事务属性实现了TransactionDefinition
    // Use defaults if no transaction definition given.
    TransactionDefinition def = (definition != null ? definition : TransactionDefinition.withDefaults());
    
    // 1.1 获取事务对象
    Object transaction = doGetTransaction();
    boolean debugEnabled = logger.isDebugEnabled();
    
    // 1.2 判断是否存在事务
    if (isExistingTransaction(transaction)) {
        // 找到现有事务->检查传播行为找到如何行为(执行)。
        // Existing transaction found -> check propagation behavior to find out how to behave.    
        // 1.4 根据传播行为处理已经存在的事务 
        return handleExistingTransaction(def, transaction, debugEnabled);
    }
    
    // 检查超时时间是否小于-1，如果小于，则抛出异常。-1表示使用默认的超时时间
    // Check definition settings for new transaction.
    if (def.getTimeout() < TransactionDefinition.TIMEOUT_DEFAULT) {
        throw new InvalidTimeoutException("Invalid transaction timeout", def.getTimeout());
    }
    
    // 正常的调用来说，一开始是不存在事务的。所以上面的“判断是否存在事务”的if先不进入
    // 未找到现有事务->检查传播行为找到如何处理。
    // No existing transaction found -> check propagation behavior to find out how to proceed.
    // 1.3 未找到现有事务，根据相应传播行为处理
    if (def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_MANDATORY) {
        // 1.3.1 PROPAGATION_MANDATORY的处理
        // 未找到现有事务，但是事务传播行为是PROPAGATION_MANDATORY，则抛出异常。
        throw new IllegalTransactionStateException(
                "No existing transaction found for transaction marked with propagation 'mandatory'");
    }
    else if (def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRED ||
            def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW ||
            def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NESTED) {
        //  1.3.2 PROPAGATION_REQUIRED、PROPAGATION_REQUIRES_NEW、PROPAGATION_NESTED的处理
        SuspendedResourcesHolder suspendedResources = suspend(null);
        if (debugEnabled) {
            logger.debug("Creating new transaction with name [" + def.getName() + "]: " + def);
        }
        try {
            return startTransaction(def, transaction, debugEnabled, suspendedResources);
        }
        catch (RuntimeException | Error ex) {
            resume(null, suspendedResources);
            throw ex;
        }
    }
    else {
        // 1.3.3 PROPAGATION_SUPPORTS、PROPAGATION_NOT_SUPPORTED、PROPAGATION_NEVER的处理
        // 执行到这里，说明事务传播行为对应PROPAGATION_SUPPORTS、PROPAGATION_NOT_SUPPORTED、
        // PROPAGATION_NEVER三个中的某个
        // 创建"空"事务:没有实际的事务，但可能同步。
        // Create "empty" transaction: no actual transaction, but potentially synchronization.
        if (def.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT && logger.isWarnEnabled()) {
            logger.warn("Custom isolation level specified but no actual transaction initiated; " +
                    "isolation level will effectively be ignored: " + def);
        }
        // 是否为新的同步。默认为true。
        boolean newSynchronization = (getTransactionSynchronization() == SYNCHRONIZATION_ALWAYS); 
        // 准备事务状态
        return prepareTransactionStatus(def, null, true, newSynchronization, debugEnabled, null);
    }
}


int TIMEOUT_DEFAULT = -1;

/**
 * 如果此事务管理器应激活线程绑定的事务同步支持，则返回true。
 * Return if this transaction manager should activate the thread-bound
 * transaction synchronization support.
 */
public final int getTransactionSynchronization() {
    // 默认为SYNCHRONIZATION_ALWAYS。
    // 因为Spring在创建JdbcTransactionManager时没有设置transactionSynchronization。
    return this.transactionSynchronization;
}

private int transactionSynchronization = SYNCHRONIZATION_ALWAYS;
```

#### 1.1 获取事务对象

位于JdbcTransactionManager继承的DataSourceTransactionManager

```java
@Override
protected Object doGetTransaction() {
    // 1、创建数据源事务对象
    DataSourceTransactionObject txObject = new DataSourceTransactionObject();
    // 设置是否允许保存点Savepoint（使用JdbcTransactionManager是允许的）
    txObject.setSavepointAllowed(isNestedTransactionAllowed());
    // 2、根据数据源，获取数据库连接持有对象
    ConnectionHolder conHolder =
            (ConnectionHolder) TransactionSynchronizationManager.getResource(obtainDataSource());
    // 3、设置数据库连接持有对象和不是新的持有对象
    txObject.setConnectionHolder(conHolder, false);
    return txObject;
}

/**
 * 返回是否允许嵌套事务。创建JdbcTransactionManager时候，指定为true。
 * Return whether nested transactions are allowed.
 */
public final boolean isNestedTransactionAllowed() {
    return this.nestedTransactionAllowed;
}

/**
 * 获取(JdbcTransactionManager)的数据源
 * Obtain the DataSource for actual use.
 * @return the DataSource (never {@code null})
 * @throws IllegalStateException in case of no DataSource set
 * @since 5.0
 */
protected DataSource obtainDataSource() {
    DataSource dataSource = getDataSource();
    Assert.state(dataSource != null, "No DataSource set");
    return dataSource;
}
```

* DataSourceTransactionObject

位于DataSourceTransactionManager的内部类

```java
/**
 * DataSource transaction object, representing a ConnectionHolder.
 * Used as transaction object by DataSourceTransactionManager.
 */
private static class DataSourceTransactionObject extends JdbcTransactionObjectSupport {
    
    // 是否是新的数据库连接持有器
    private boolean newConnectionHolder;
    // 是否必须恢复自动提交
    private boolean mustRestoreAutoCommit;

    public void setConnectionHolder(@Nullable ConnectionHolder connectionHolder, boolean newConnectionHolder) {
        // 设置数据库连接。父类JdbcTransactionObjectSupport中保存
        super.setConnectionHolder(connectionHolder);
        this.newConnectionHolder = newConnectionHolder;
    }

    public boolean isNewConnectionHolder() {
        return this.newConnectionHolder;
    }

    public void setMustRestoreAutoCommit(boolean mustRestoreAutoCommit) {
        this.mustRestoreAutoCommit = mustRestoreAutoCommit;
    }

    public boolean isMustRestoreAutoCommit() {
        return this.mustRestoreAutoCommit;
    }

    public void setRollbackOnly() {
        getConnectionHolder().setRollbackOnly();
    }

    @Override
    public boolean isRollbackOnly() {
        return getConnectionHolder().isRollbackOnly();
    }

    @Override
    public void flush() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationUtils.triggerFlush();
        }
    }
}
```

![image-20240728000011608](https://lu-note.oss-cn-shenzhen.aliyuncs.com/notes/work/image-20240728000011608.png)



##### 1.1.1 获取数据库连接持有对象

```java
ConnectionHolder conHolder = (ConnectionHolder) TransactionSynchronizationManager.getResource(obtainDataSource());
```

根据数据源，从事务同步管理器中获取数据库连接持有对象。

```java
/**
 * 检索绑定到当前线程的给定key的资源。
 * Retrieve a resource for the given key that is bound to the current thread.
 * @param key the key to check (usually the resource factory)
 * @return a value bound to the current thread (usually the active
 * resource object), or {@code null} if none
 * @see ResourceTransactionManager#getResourceFactory()
 */
@Nullable
public static Object getResource(Object key) {
    // 获取实际的key
    Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
    // 根据actualKey，从ThreadLocal获取数据库连接对象。
    // 一开始ThreadLocal中是没有值的，这里获取到null。
    // 什么时候ThreadLocal中会有呢？？？开启新的事务之后会有，我们继续往下看就知道了    
    return doGetResource(actualKey);
}

private static final ThreadLocal<Map<Object, Object>> resources =
			new NamedThreadLocal<>("Transactional resources");
/**
 * Actually check the value of the resource that is bound for the given key.
 * 实际检查给定key绑定的资源的值。
 */
@Nullable
private static Object doGetResource(Object actualKey) {
    // 获取ThreadLocal中保存的map。一开始是没有的。直接返回null
    Map<Object, Object> map = resources.get();
    if (map == null) {
        return null;
    }
    Object value = map.get(actualKey);
    // Transparently remove ResourceHolder that was marked as void...
    if (value instanceof ResourceHolder && ((ResourceHolder) value).isVoid()) {
        map.remove(actualKey);
        // Remove entire ThreadLocal if empty...
        if (map.isEmpty()) {
            resources.remove();
        }
        value = null;
    }
    return value;
}


/**
 * Unwrap the given resource handle if necessary; otherwise return
 * the given handle as-is.
 * @since 5.3.4
 * @see InfrastructureProxy#getWrappedObject()
 */
public static Object unwrapResourceIfNecessary(Object resource) {
    // resource = DruidDataSourceWrapper。不属于InfrastructureProxy
    Assert.notNull(resource, "Resource must not be null");
    Object resourceRef = resource;
    // unwrap infrastructure proxy
    if (resourceRef instanceof InfrastructureProxy) {
        resourceRef = ((InfrastructureProxy) resourceRef).getWrappedObject();
    }
    // aopAvailable = true。
    if (aopAvailable) {
        // now unwrap scoped proxy
        resourceRef = ScopedProxyUnwrapper.unwrapIfNecessary(resourceRef);
    }
    // 直接返回DruidDataSourceWrapper对象
    return resourceRef;
}
private static final boolean aopAvailable = ClassUtils.isPresent(
			"org.springframework.aop.scope.ScopedObject", TransactionSynchronizationUtils.class.getClassLoader());
/**
 * Inner class to avoid hard-coded dependency on AOP module.
 */
private static class ScopedProxyUnwrapper {

    public static Object unwrapIfNecessary(Object resource) {
        if (resource instanceof ScopedObject) {
            return ((ScopedObject) resource).getTargetObject();
        }
        else {
            // 直接返回
            return resource;
        }
    }
}
```

TransactionSynchronizationManager 事务同步管理器是一个抽象类，通常用于管理线程内的事务资源。见【关键类】



#### 1.2 判断是否存在事务

```java
protected boolean isExistingTransaction(Object transaction) {
    DataSourceTransactionObject txObject = (DataSourceTransactionObject) transaction;
    // 数据源事务对象的数据库连接持有对象不为null，而且数据库连接持有对象有事务活跃。  
    return (txObject.hasConnectionHolder() && txObject.getConnectionHolder().isTransactionActive());
}

/**
* 检查事务对象是否有数据库连接持有对象
 * Check whether this transaction object has a ConnectionHolder.
 */
public boolean hasConnectionHolder() {
    return (this.connectionHolder != null);
}

/**
 * 返回该数据库连接持有对象是否存在一个JDBC管理的活跃事务
 * Return whether this holder represents an active, JDBC-managed transaction.
 */
protected boolean isTransactionActive() {
    return this.transactionActive;
}
// 默认为false
private boolean transactionActive = false;
```





#### 1.3、未找到现有事务，根据相应传播行为处理

说明：这里我们先跳过【根据传播行为处理已经存在的事务】，我们先分析 【未找到现有事务，根据相应传播行为处理】。因为按照正常的调用，我们都是先执行到未找到现有事务的情况。

```java
// 未找到现有事务->检查传播行为找到如何处理。
// No existing transaction found -> check propagation behavior to find out how to proceed.
// 1.4 未找到现有事务，根据相应传播行为处理
if (def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_MANDATORY) {
    // 1.4.1 PROPAGATION_MANDATORY的处理
    // 未找到现有事务，但是事务传播行为是PROPAGATION_MANDATORY，则抛出异常。
    throw new IllegalTransactionStateException(
            "No existing transaction found for transaction marked with propagation 'mandatory'");
}
else if (def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRED ||
        def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW ||
        def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NESTED) {
    //  1.4.2 PROPAGATION_REQUIRED、PROPAGATION_REQUIRES_NEW、PROPAGATION_NESTED的处理
    // 1.4.2.1、挂起给定的事务对象（以及挂起并清除事务同步相关资源）。这里我们传入null
    SuspendedResourcesHolder suspendedResources = suspend(null);
    if (debugEnabled) {
        logger.debug("Creating new transaction with name [" + def.getName() + "]: " + def);
    }
    try {
        // 1.4.2.2、开启事务并返回事务状态
        return startTransaction(def, transaction, debugEnabled, suspendedResources);
    }
    catch (RuntimeException | Error ex) {
        resume(null, suspendedResources);
        throw ex;
    }
}
else {
    // 1.4.3 PROPAGATION_SUPPORTS、PROPAGATION_NOT_SUPPORTED、PROPAGATION_NEVER的处理
    // 执行到这里，说明事务传播行为对应PROPAGATION_SUPPORTS、PROPAGATION_NOT_SUPPORTED、
    // PROPAGATION_NEVER三个中的某个
    // Create "empty" transaction: no actual transaction, but potentially synchronization.
    if (def.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT && logger.isWarnEnabled()) {
        logger.warn("Custom isolation level specified but no actual transaction initiated; " +
                "isolation level will effectively be ignored: " + def);
    }
    // 是否为新的同步。默认为true。
    boolean newSynchronization = (getTransactionSynchronization() == SYNCHRONIZATION_ALWAYS); 
    // 准备事务状态
    return prepareTransactionStatus(def, null, true, newSynchronization, debugEnabled, null);
}
```

##### 1.3.1  PROPAGATION_MANDATORY的处理

```
未找到现有事务，但是事务传播行为是PROPAGATION_MANDATORY，则抛出异常。
```

##### 1.3.2  PROPAGATION_REQUIRED、PROPAGATION_REQUIRES_NEW、PROPAGATION_NESTED的处理

###### 1.3.2.1  挂起给定的事务对象（以及挂起并清除事务同步相关资源）

【注意】此时的传参transaction=null

```java
/**
 * 挂起给定的事务。首先挂起事务同步，然后委托给doSuspend模板方法。
 * Suspend the given transaction. Suspends transaction synchronization first,
 * then delegates to the {@code doSuspend} template method.
 * 
 * @param transaction the current transaction object
 * (or {@code null} to just suspend active synchronizations, if any)
 * 
 * 返回保存挂起资源的对象(如果事务和同步都不活动，则为空)
 * @return an object that holds suspended resources
 * (or {@code null} if neither transaction nor synchronization active)
 * @see #doSuspend
 * @see #resume
 */
@Nullable
protected final SuspendedResourcesHolder suspend(@Nullable Object transaction) throws TransactionException {   
    // 当前线程是否有事务同步活跃
    if (TransactionSynchronizationManager.isSynchronizationActive()) {	
        // 更加具体的分析可以见【关键类】SuspendedResourcesHolder的【1、 挂起现有的事务对象】
        List<TransactionSynchronization> suspendedSynchronizations = doSuspendSynchronization();
        try {
            Object suspendedResources = null;
            if (transaction != null) {
                suspendedResources = doSuspend(transaction);
            }
            String name = TransactionSynchronizationManager.getCurrentTransactionName();
            TransactionSynchronizationManager.setCurrentTransactionName(null);
            boolean readOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
            TransactionSynchronizationManager.setCurrentTransactionReadOnly(false);
            Integer isolationLevel = TransactionSynchronizationManager.getCurrentTransactionIsolationLevel();
            TransactionSynchronizationManager.setCurrentTransactionIsolationLevel(null);
            boolean wasActive = TransactionSynchronizationManager.isActualTransactionActive();
            TransactionSynchronizationManager.setActualTransactionActive(false);
            return new SuspendedResourcesHolder(
                    suspendedResources, suspendedSynchronizations, name, readOnly, isolationLevel, wasActive);
        }
        catch (RuntimeException | Error ex) {
            // doSuspend failed - original transaction is still active...
            doResumeSynchronization(suspendedSynchronizations);
            throw ex;
        }
    }   
    else if (transaction != null) {
        // Transaction active but no synchronization active.
        Object suspendedResources = doSuspend(transaction);
        return new SuspendedResourcesHolder(suspendedResources);
    }
    else {
        // 既没有事务对象，也没有事务同步器活跃。直接返回空的挂起资源对象null
        // Neither transaction nor synchronization active.
        return null;
    }
}

/**
 * Return if transaction synchronization is active for the current thread.
 * Can be called before register to avoid unnecessary instance creation.
 * @see #registerSynchronization
 */
public static boolean isSynchronizationActive() {
    // 未找到现有事务的时候，是没有事务同步器存在的
    return (synchronizations.get() != null);
}

private static final ThreadLocal<Set<TransactionSynchronization>> synchronizations =
			new NamedThreadLocal<>("Transaction synchronizations");
```



###### 1.3.2.2 开启事务并返回事务状态

```java
/*
    此时传参如下    
    definition：事务注解属性
    transaction： 数据源事务对象
    newSynchronization：true
    debugEnabled：false
    suspendedResources：null
*/
见【关键类】JdbcTransactionManager的【1、开启事务并返回事务状态】
```



##### 1.3.3 PROPAGATION_SUPPORTS、PROPAGATION_NOT_SUPPORTED、PROPAGATION_NEVER的处理

```java
// 执行到这里，说明事务传播行为对应PROPAGATION_SUPPORTS、PROPAGATION_NOT_SUPPORTED、PROPAGATION_NEVER三个中的某个
// Create "empty" transaction: no actual transaction, but potentially synchronization.
if (def.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT && logger.isWarnEnabled()) {
    logger.warn("Custom isolation level specified but no actual transaction initiated; " +
            "isolation level will effectively be ignored: " + def);
}
// 是否为新的同步。默认为true。
boolean newSynchronization = (getTransactionSynchronization() == SYNCHRONIZATION_ALWAYS); 
// 准备事务状态。【注意】这里的事务对象transaction为null，挂起的资源suspendedResources也为null，newTransaction=true
return prepareTransactionStatus(def, null, true, newSynchronization, debugEnabled, null);
```

```java
/**
 * Create a new TransactionStatus for the given arguments,
 * also initializing transaction synchronization as appropriate.
 * @see #newTransactionStatus
 * @see #prepareTransactionStatus
 */
protected final DefaultTransactionStatus prepareTransactionStatus(
        TransactionDefinition definition, @Nullable Object transaction, boolean newTransaction,
        boolean newSynchronization, boolean debug, @Nullable Object suspendedResources) {
    // 见【关键类】DefaultTransactionStatus的1.1、创建DefaultTransactionStatus
    //【注意】这里的事务对象transaction为null，挂起的资源suspendedResources也为null
    DefaultTransactionStatus status = newTransactionStatus(
            definition, transaction, newTransaction, newSynchronization, debug, suspendedResources);
    // 准备同步
    prepareSynchronization(status, definition);
    return status;
}
```

```java
/**
 * 根据需要初始化事务同步。
 * Initialize transaction synchronization as appropriate.
 */
protected void prepareSynchronization(DefaultTransactionStatus status, TransactionDefinition definition) {
    // 如果是新的事务同步。这里获取到的DefaultTransactionStatus里设置了新的同步
    if (status.isNewSynchronization()) {
        // 当前线程是否有实际事务活跃。
        // 对于这三种传播行为，获取到的DefaultTransactionStatus没有事务对象，设置成没有实际事务活跃
        TransactionSynchronizationManager.setActualTransactionActive(status.hasTransaction());
        // 绑定当前事务隔离级别到当前线程
        TransactionSynchronizationManager.setCurrentTransactionIsolationLevel(
                // 不使用默认的隔离级别，则使用指定的具体隔离级别
                definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT ?
                        definition.getIsolationLevel() : null);
        // 绑定当前事务是否只读到当前线程
        TransactionSynchronizationManager.setCurrentTransactionReadOnly(definition.isReadOnly());
        // 绑定事务名称到当前线程
        TransactionSynchronizationManager.setCurrentTransactionName(definition.getName());
        // 初始化同步
        TransactionSynchronizationManager.initSynchronization();
    }
}

/**
 * 返回是否有实际的活动事务。
 * Return whether there is an actual transaction active.
 */
public boolean hasTransaction() {
    // 这里数据源事务对象为空。
    return (this.transaction != null);
}

/**
 * 激活当前线程的事务同步。由事务管理器在事务开始时调用。
 * Activate transaction synchronization for the current thread.
 * Called by a transaction manager on transaction begin.
 * @throws IllegalStateException if synchronization is already active
 */
public static void initSynchronization() throws IllegalStateException {
    // 如果已经有同步器激活，则抛出异常。一开始是没激活的
    if (isSynchronizationActive()) {
        throw new IllegalStateException("Cannot activate transaction synchronization - already active");
    }
    // 设置LinkedHashSet到synchronizations
    synchronizations.set(new LinkedHashSet<>());
}
private static final ThreadLocal<Set<TransactionSynchronization>> synchronizations =
			new NamedThreadLocal<>("Transaction synchronizations");

/**
 * 如果当前线程的事务同步是活动的，则返回。可以在注册前调用，避免不必要的实例创建。
 * Return if transaction synchronization is active for the current thread.
 * Can be called before register to avoid unnecessary instance creation.
 * @see #registerSynchronization
 */
public static boolean isSynchronizationActive() {
    return (synchronizations.get() != null);
}
```



##### 1.3.4 未找到现有事务的处理总结

未找到现有的事务，经过不同传播行为的处理，事务状态和事务同步的属性情况分别如下：

* DefaultTransactionStatus

（1）PROPAGATION_MANDATORY：无（已经抛出异常） 

（2）PROPAGATION_REQUIRED、PROPAGATION_REQUIRES_NEW、PROPAGATION_NESTED（默认情况）

![image-20240726155320509](https://lu-note.oss-cn-shenzhen.aliyuncs.com/notes/work/image-20240726155320509.png)

（3）PROPAGATION_SUPPORTS、PROPAGATION_NOT_SUPPORTED、PROPAGATION_NEVER

![image-20240726154302325](https://lu-note.oss-cn-shenzhen.aliyuncs.com/notes/work/image-20240726154302325.png)



* TransactionSynchronizationManager

（1）PROPAGATION_MANDATORY：无（已经抛出异常） 

（2）PROPAGATION_REQUIRED、PROPAGATION_REQUIRES_NEW、PROPAGATION_NESTED（默认情况）

```java
public abstract class TransactionSynchronizationManager {

	//当前线程的事务资源map 存放 数据源对象 --> 数据库连接持有对象的键值对 
	private static final ThreadLocal<Map<Object, Object>> resources =
			new NamedThreadLocal<>("Transactional resources");
    // 存着LinkedHashSet，表示事务同步器激活，但是集合里没有元素
	private static final ThreadLocal<Set<TransactionSynchronization>> synchronizations =
			new NamedThreadLocal<>("Transaction synchronizations");
  	// 存储当前事务的名称（类名 + 方法名）
	private static final ThreadLocal<String> currentTransactionName =
			new NamedThreadLocal<>("Current transaction name");
     // 存储当前事务不为只读事务。默认为false，来源@Transactional注解
	private static final ThreadLocal<Boolean> currentTransactionReadOnly =
			new NamedThreadLocal<>("Current transaction read-only status");
      // 存储当前事务的隔离级别，使用数据库的默认隔离级别，来源@Transactional注解
	private static final ThreadLocal<Integer> currentTransactionIsolationLevel =
			new NamedThreadLocal<>("Current transaction isolation level");
	// 当前线程有事务活跃
	private static final ThreadLocal<Boolean> actualTransactionActive =
		new NamedThreadLocal<>("Actual transaction active"); 
}
```

（3）PROPAGATION_SUPPORTS、PROPAGATION_NOT_SUPPORTED、PROPAGATION_NEVER

```java
public abstract class TransactionSynchronizationManager {
	// 无资源map
	private static final ThreadLocal<Map<Object, Object>> resources =
			new NamedThreadLocal<>("Transactional resources");
    // 存着LinkedHashSet，表示事务同步器激活，但是集合里没有元素
	private static final ThreadLocal<Set<TransactionSynchronization>> synchronizations =
			new NamedThreadLocal<>("Transaction synchronizations");
  	//  存储当前事务的名称（类名 + 方法名）
	private static final ThreadLocal<String> currentTransactionName =
			new NamedThreadLocal<>("Current transaction name");
     //  存储当前事务不为只读事务。默认为false，来源@Transactional注解
	private static final ThreadLocal<Boolean> currentTransactionReadOnly =
			new NamedThreadLocal<>("Current transaction read-only status");
     // 存储当前事务的隔离级别，使用数据库的默认隔离级别，来源@Transactional注解
	private static final ThreadLocal<Integer> currentTransactionIsolationLevel =
			new NamedThreadLocal<>("Current transaction isolation level");
	// 当前线程没有事务活跃
	private static final ThreadLocal<Boolean> actualTransactionActive =
		new NamedThreadLocal<>("Actual transaction active"); 
}
```

了解这些，后面的【1.4 根据传播行为处理已经存在的事务】才能更好地分析



（3）重点结论

**在当前线程中，一次完整的事务过程的调用只能有一个事务同步。**
**在当前线程中，一次非事务过程的调用只能有一个事务同步。**

下面举例部分调用过程说明：

* 示例一

```java
@Override
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void testRequiredNew() {
    addDemoPerson();
    animalService.testRequiredNew();
}

// animalService
@Override
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void testRequiredNew() {
    saveDemoAnimal();
}
```

调用testRequiredNew方法时候，会新建一个事务，它的调用过程有自己的事务同步，这里称为事务同步一。

当调用到animalService的testRequiredNew方法时候，也会新建一个事务，它的调用过程也有自己的事务同步，这里称为事务同步二。

事务同步一和事务同步二分别管理自己的资源。

* 示例二

```java
@Override
@Transactional(propagation = Propagation.SUPPORTS)
public void testSupported() {
    addDemoPerson();
    animalService.testSupported();
}

// animalService
@Override
@Transactional(propagation = Propagation.SUPPORTS)
public void testSupported() {
    saveDemoAnimal();
}
```

调用testSupported方法时候，即使没有事务存在，它的调用过程有自己的事务同步，这里称为事务同步一。

当调用到animalService的testSupported方法时候，没有事务存在，共用事务同步一。

* 示例三

```java
@Override
@Transactional(propagation = Propagation.SUPPORTS)
public void testSupported() {
    addDemoPerson();
    animalService.testRequired();
}

// animalService
@Override
@Transactional(propagation = Propagation.REQUIRED)
public void testRequired() {
    saveDemoAnimal();
}
```

调用testSupported方法时候，即使没有事务存在，它的调用过程有自己的事务同步，这里称为事务同步一。

当调用到animalService的testRequired方法时候，会新建一个事务，它的调用过程也有自己的事务同步，这里称为事务同步二。

事务同步一和事务同步二分别管理自己的资源。

* 示例四

```java
@Transactional(propagation = Propagation.REQUIRED)
public void testRequired() {
    addDemoPerson();
    animalService.testRequired();
}

// animalService
@Override
@Transactional(propagation = Propagation.REQUIRED)
public void testRequired() {
    saveDemoAnimal();
}
```

调用testRequired方法时候，会新建一个事务，它的调用过程有自己的事务同步，这里称为事务同步一。

当调用到animalService的testRequired方法时候，无需新建事务，使用同一个事务，因此共用事务同步一。

* 示例五

```java
@Transactional(propagation = Propagation.REQUIRED)
public void testRequired() {
    addDemoPerson();
    animalService.testRequiredNew();
}

// animalService
@Override
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void testRequiredNew() {
    saveDemoAnimal();
}
```

调用testRequired方法时候，会新建一个事务，它的调用过程有自己的事务同步，这里称为事务同步一。

当调用到animalService的testRequiredNew方法时候，新建一个事务，它的调用过程也有自己的事务同步，这里称为事务同步二。

事务同步一和事务同步二分别管理自己的资源。

* 示例六

```java
@Override
@Transactional(propagation = Propagation.REQUIRED)
public void testNested() throws NotRunTimeException {
    addDemoPerson();
    animalService.testNested();
}

// animalService
@Override
@Transactional(propagation = Propagation.NESTED, rollbackFor = Exception.class)
public void testNested() throws NotRunTimeException {
    saveDemoAnimal();
}
```

调用testNested方法时候，会新建一个事务，它的调用过程有自己的事务同步，这里称为事务同步一。

当调用到animalService的testNested方法时候，通过创建保存点来完成嵌套事务，无需新建事务，因此共用事务同步一。



#### 1.4  根据传播行为处理已经存在的事务

以下这种情况是会判断为已经存在事务

* PersonServiceImpl

```java
@Transactional
public void testBehavior() {
    addDemoPerson();
    animalService.testRequiredNew();
}
```

* AnimalServiceImpl

```java
@Override
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void testRequiredNew() {
    saveDemoAnimal();
}
```

调用PersonServiceImpl的testRequiredNew方法时，在animalService执行testRequiredNew方法前，会被事务拦截器拦截，此时获取到的事务对象是有数据库连接对象的，就会判断为存在现有的事务，然后为现有的事务创建一个TransactionStatus。



```java
/**
 * 为现有的事务创建一个TransactionStatus。
 * Create a TransactionStatus for an existing transaction.
 */
private TransactionStatus handleExistingTransaction(
        TransactionDefinition definition, Object transaction, boolean debugEnabled)
        throws TransactionException {
    // 1.4.1 事务传播行为是PROPAGATION_NEVER，则抛出异常
    if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NEVER) {
        throw new IllegalTransactionStateException(
                "Existing transaction found for transaction marked with propagation 'never'");
    }
    
    // 1.4.2 事务传播行为是PROPAGATION_NOT_SUPPORTED，则挂起现有的事务，以非事务的方式运行
    if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NOT_SUPPORTED) {
        if (debugEnabled) {
            logger.debug("Suspending current transaction");
        }
        // 1.4.2.1 挂起现有的事务对象（以及挂起并清除事务同步相关资源）
        Object suspendedResources = suspend(transaction);       
        boolean newSynchronization = (getTransactionSynchronization() == SYNCHRONIZATION_ALWAYS);
        // 1.4.2.2 创建相应的事务状态
        return prepareTransactionStatus(
                definition, null, false, newSynchronization, debugEnabled, suspendedResources);
    }

    //  1.4.3 事务传播行为是PROPAGATION_REQUIRES_NEW，则挂起现有的事务，创建新的事务运行
    if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW) {
        if (debugEnabled) {
            logger.debug("Suspending current transaction, creating new transaction with name [" +
                    definition.getName() + "]");
        }
        // 挂起现有的事务对象（以及挂起并清除事务同步相关资源）
        SuspendedResourcesHolder suspendedResources = suspend(transaction);
        try {
            // 开启新的事务
            return startTransaction(definition, transaction, debugEnabled, suspendedResources);
        }
        catch (RuntimeException | Error beginEx) {
            resumeAfterBeginException(transaction, suspendedResources, beginEx);
            throw beginEx;
        }
    }

    // 1.4.4 事务传播行为是PROPAGATION_NESTED，如果不允许嵌套事务，则直接抛出异常（创建的JdbcTransactionManager是支持的）
    if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NESTED) {
        if (!isNestedTransactionAllowed()) {
            throw new NestedTransactionNotSupportedException(
                    "Transaction manager does not allow nested transactions by default - " +
                    "specify 'nestedTransactionAllowed' property with value 'true'");
        }
        if (debugEnabled) {
            logger.debug("Creating nested transaction with name [" + definition.getName() + "]");
        }
        // 是否为嵌套事务使用保存点。创建的JdbcTransactionManager默认使用保存点完成的
        if (useSavepointForNestedTransaction()) {
            // 使用保存点完成嵌套事务            
            // Create savepoint within existing Spring-managed transaction,
            // through the SavepointManager API implemented by TransactionStatus.
            // Usually uses JDBC 3.0 savepoints. Never activates Spring synchronization.
            //在现有spring管理的事务中创建保存点
	       //通过TransactionStatus实现的SavepointManager API。
	       //通常使用JDBC 3.0保存点。从不激活Spring同步。
            DefaultTransactionStatus status =
                    prepareTransactionStatus(definition, transaction, false, false, debugEnabled, null);
            status.createAndHoldSavepoint();
            return status;
        }
        else {
            // 不使用保存点完成嵌套事务             
            // Nested transaction through nested begin and commit/rollback calls.
            // Usually only for JTA: Spring synchronization might get activated here
            // in case of a pre-existing JTA transaction.
            return startTransaction(definition, transaction, debugEnabled, null);
        }
    }

    // 1.4.5 执行到这里，大概是PROPAGATION_SUPPORTS或者PROPAGATION_REQUIRED。也有可能是Propagation.MANDATORY
    // Assumably PROPAGATION_SUPPORTS or PROPAGATION_REQUIRED.
    if (debugEnabled) {
        logger.debug("Participating in existing transaction");
    }
    // 是否校验现有的事务。创建的JdbcTransactionManager是没有设置需要校验的，默认不需要
    if (isValidateExistingTransaction()) {
        // 校验现有事务的隔离级别是否和当前事务定义的隔离级别一样
        if (definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT) {
            Integer currentIsolationLevel = TransactionSynchronizationManager.getCurrentTransactionIsolationLevel();
            if (currentIsolationLevel == null || currentIsolationLevel != definition.getIsolationLevel()) {
                Constants isoConstants = DefaultTransactionDefinition.constants;
                throw new IllegalTransactionStateException("Participating transaction with definition [" +
                        definition + "] specifies isolation level which is incompatible with existing transaction: " +
                        (currentIsolationLevel != null ?
                                isoConstants.toCode(currentIsolationLevel, DefaultTransactionDefinition.PREFIX_ISOLATION) :
                                "(unknown)"));
            }
        }
        // 当前事务定义的隔离级别不是只读，如果现有的事务是只读，则抛出异常
        if (!definition.isReadOnly()) {
            if (TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
                throw new IllegalTransactionStateException("Participating transaction with definition [" +
                        definition + "] is not marked as read-only but existing transaction is");
            }
        }
    }
    boolean newSynchronization = (getTransactionSynchronization() != SYNCHRONIZATION_NEVER);
    return prepareTransactionStatus(definition, transaction, false, newSynchronization, debugEnabled, null);
}



/**
 * Return whether existing transactions should be validated before participating
 * in them.
 * @since 2.5.1
 */
public final boolean isValidateExistingTransaction() {
    return this.validateExistingTransaction;
}
private boolean validateExistingTransaction = false;
```



##### 1.4.1 PROPAGATION_NEVER的处理

```java
// 事务传播行为是PROPAGATION_NEVER，则抛出异常
if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NEVER) {
    throw new IllegalTransactionStateException(
            "Existing transaction found for transaction marked with propagation 'never'");
}
```

##### 1.4.2 PROPAGATION_NOT_SUPPORTED的处理

###### 1.4.1.1 挂起现有的事务对象（以及挂起并清除事务同步相关资源）

```
见【关键类】SuspendedResourcesHolder的【1、挂起现有的事务对象】。
此时TransactionSynchronizationManager里的属性情况可以参考【1.3.4 未找到现有事务的处理总结】
```

###### 1.4.2.2 创建相应的事务状态

```java
// 默认newSynchronization=true，是新的同步。
boolean newSynchronization = (getTransactionSynchronization() == SYNCHRONIZATION_ALWAYS);
// 事务对象为null，newTransaction=false（不是新的事务）
// 挂起的资源suspendedResources不为null，是SuspendedResourcesHolder对象（保存现有事务的数据库连接持有对象、事务同步等）。
prepareTransactionStatus(definition, null, false, newSynchronization, debugEnabled, suspendedResources);

/**
 * Create a new TransactionStatus for the given arguments,
 * also initializing transaction synchronization as appropriate.
 * @see #newTransactionStatus
 * @see #prepareTransactionStatus
 */
protected final DefaultTransactionStatus prepareTransactionStatus(
        TransactionDefinition definition, @Nullable Object transaction, boolean newTransaction,
        boolean newSynchronization, boolean debug, @Nullable Object suspendedResources) {
	// 创建事务状态。【注意】这里的事务状态保存着挂起的资源（SuspendedResourcesHolder）
    DefaultTransactionStatus status = newTransactionStatus(
            definition, transaction, newTransaction, newSynchronization, debug, suspendedResources);
    // 准备事务同步
    prepareSynchronization(status, definition);
    return status;
}

/**
 * Initialize transaction synchronization as appropriate.
 */
protected void prepareSynchronization(DefaultTransactionStatus status, TransactionDefinition definition) {
    // 新的同步，进入if
    if (status.isNewSynchronization()) {
        // 此时无事务存在，也就是没有实际的事务活跃
        TransactionSynchronizationManager.setActualTransactionActive(status.hasTransaction());
        TransactionSynchronizationManager.setCurrentTransactionIsolationLevel(
                definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT ?
                        definition.getIsolationLevel() : null);
        TransactionSynchronizationManager.setCurrentTransactionReadOnly(definition.isReadOnly());
        TransactionSynchronizationManager.setCurrentTransactionName(definition.getName());
        // 初始化事务同步
        TransactionSynchronizationManager.initSynchronization();
    }
}
```



##### 1.4.3 PROPAGATION_REQUIRES_NEW的处理

```java
//  1.4.3 事务传播行为是PROPAGATION_REQUIRES_NEW，则挂起现有的事务，创建新的事务运行
if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW) {
    if (debugEnabled) {
        logger.debug("Suspending current transaction, creating new transaction with name [" +
                definition.getName() + "]");
    }
    // 1.4.3.1 挂起现有的事务对象（以及挂起并清除事务同步相关资源）
    // 见【关键类】SuspendedResourcesHolder的【1、挂起现有的事务对象】。
	// 此时TransactionSynchronizationManager里的属性情况可以参考【1.3.4 未找到现有事务的处理总结】
    SuspendedResourcesHolder suspendedResources = suspend(transaction);
    try {
        // 1.4.3.2 开启新的事务并返回事务状态        
        /*
        见【关键类】JdbcTransactionManager的【1、开启事务并返回事务状态】
            此时传参如下    
            definition：事务注解属性
            transaction： 现有的事务对象           
            debugEnabled：false
            suspendedResources：挂起的资源SuspendedResourcesHolder对象（保存现有事务的数据库连接持有对象、事务同步等）。
        */     
        // 这里会重新获取数据库连接
        return startTransaction(definition, transaction, debugEnabled, suspendedResources);
    }
    catch (RuntimeException | Error beginEx) {
        // 发生异常，则恢复挂起的资源（后面会分析，这里先忽略）
        resumeAfterBeginException(transaction, suspendedResources, beginEx);
        throw beginEx;
    }
}
```



##### 1.4.4 PROPAGATION_NESTED的处理

```java
// 1.4.4 事务传播行为是PROPAGATION_NESTED，如果不允许嵌套事务，则直接抛出异常（创建的JdbcTransactionManager是支持的）
if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NESTED) {
    if (!isNestedTransactionAllowed()) {
        throw new NestedTransactionNotSupportedException(
                "Transaction manager does not allow nested transactions by default - " +
                "specify 'nestedTransactionAllowed' property with value 'true'");
    }
    if (debugEnabled) {
        logger.debug("Creating nested transaction with name [" + definition.getName() + "]");
    }
    // 是否为嵌套事务使用保存点。创建的JdbcTransactionManager默认使用保存点完成的
    if (useSavepointForNestedTransaction()) {
        // 1.4.4.1 使用保存点完成嵌套事务            
        // Create savepoint within existing Spring-managed transaction,
        // through the SavepointManager API implemented by TransactionStatus.
        // Usually uses JDBC 3.0 savepoints. Never activates Spring synchronization.
        //在现有spring管理的事务中创建保存点
	    //通过TransactionStatus实现的SavepointManager API。
	    //通常使用JDBC 3.0保存点。从不激活Spring同步。
        // 1.4.4.1--(1)使用保存点完成嵌套事务  
        DefaultTransactionStatus status =
                prepareTransactionStatus(definition, transaction, false, false, debugEnabled, null);   
        // 1.4.4.1--(2)创建保存点并保存到事务状态
        status.createAndHoldSavepoint();
        return status;
    }
    else {
        // 不使用保存点完成嵌套事务（这里我们用不到，先不分析）             
        // Nested transaction through nested begin and commit/rollback calls.
        // Usually only for JTA: Spring synchronization might get activated here
        // in case of a pre-existing JTA transaction.
        return startTransaction(definition, transaction, debugEnabled, null);
    }
}
```

###### 1.4.4.1 使用保存点完成嵌套事务 

1、创建事务状态

```java
/**
同【关键类】DefaultTransactionStatus的1、创建DefaultTransactionStatus并适当初始化事务同步
此时的传参如下：
definition：事务注解属性
transaction：现有事务对象
是否新的事务newTransaction：false
是否新的事务同步newSynchronization：false
debugEnabled：false
挂起的资源suspendedResources：null
*/
DefaultTransactionStatus status =
                prepareTransactionStatus(definition, transaction, false, false, debugEnabled, null);  
```

2、创建保存点并保存到事务状态

```
【关键类】DefaultTransactionStatus的【2、创建保存点并保存到事务状态】
```

##### 1.4.5 PROPAGATION_SUPPORTS或者PROPAGATION_REQUIRED的处理

```java
/**
* 见【关键类】DefaultTransactionStatus的【1、创建DefaultTransactionStatus并适当初始化事务同步】
*  definition：事务注解属性
*  transaction：现有的事务对象
*  newTransaction：false。不是新的事务
*  suspendedResources：挂起的资源不存在
*  newSynchronization : 默认为true。但是执行到这里，对于PROPAGATION_SUPPORTS和PROPAGATION_REQUIRED，
*                       因为存在了现有的事务，都是已经开启过事务同步，所以不用开启新的事务同步了
*/
boolean newSynchronization = (getTransactionSynchronization() != SYNCHRONIZATION_NEVER);
return prepareTransactionStatus(definition, transaction, false, newSynchronization, debugEnabled, null);
```



### 2、 准备事务信息（入栈）

见【关键类】TransactionInfo

```java
/**
 * 为给定的属性和状态对象准备一个TransactionInfo。
 * Prepare a TransactionInfo for the given attribute and status object.
 * 
 * @param txAttr the TransactionAttribute (may be {@code null}) 事务注解属性
 * @param joinpointIdentification the fully qualified method name 连接点标识符（类名 + 方法名）
 * (used for monitoring and logging purposes) 
 * @param status the TransactionStatus for the current transaction 当前事务的事务状态
 * 
 * @return the prepared TransactionInfo object 返回准备好的事务信息对象
 */
protected TransactionInfo prepareTransactionInfo(@Nullable PlatformTransactionManager tm,
        @Nullable TransactionAttribute txAttr, String joinpointIdentification,
        @Nullable TransactionStatus status) {
	
    // 构建TransactionInfo
    TransactionInfo txInfo = new TransactionInfo(tm, txAttr, joinpointIdentification);
    if (txAttr != null) {
        // We need a transaction for this method...
        if (logger.isTraceEnabled()) {
            logger.trace("Getting transaction for [" + txInfo.getJoinpointIdentification() + "]");
        }
        // The transaction manager will flag an error if an incompatible tx already exists.
        // 设置事务状态
        txInfo.newTransactionStatus(status);
    }
    else {
        // The TransactionInfo.hasTransaction() method will return false. We created it only
        // to preserve the integrity of the ThreadLocal stack maintained in this class.
        if (logger.isTraceEnabled()) {
            logger.trace("No need to create transaction for [" + joinpointIdentification +
                    "]: This method is not transactional.");
        }
    }

    // 我们总是绑定事务信息到线程，即使我们没有在这里创建一个新的事务信息。
    // 这保存事务信息的堆栈会正确地管理，即使这个切面没有事务创建，
    // We always bind the TransactionInfo to the thread, even if we didn't create
    // a new transaction here. This guarantees that the TransactionInfo stack
    // will be managed correctly even if no transaction was created by this aspect.
    // 绑定事务信息到线程（事务信息入栈）
    txInfo.bindToThread();
    return txInfo;
}

// 1、绑定事务信息到线程（事务信息入栈）
private void bindToThread() {
    // 暴露当前的TransactionStatus，保留任何现有的TransactionStatus用于此事务完成后的恢复。
    // Expose current TransactionStatus, preserving any existing TransactionStatus
    // for restoration after this transaction is complete.        
    // 设置旧的事务信息
    this.oldTransactionInfo = transactionInfoHolder.get();
    // 保存当前事务信息到当前线程的transactionInfoHolder
    transactionInfoHolder.set(this);
}
```



##  四、调用目标方法

```java
//这是一个环绕通知:调用链中的下一个拦截器。这通常会导致目标对象被调用。            
// This is an around advice: Invoke the next interceptor in the chain.
// This will normally result in a target object being invoked.
// 4、调用目标方法
retVal = invocation.proceedWithInvocation();
```

这里本质是AOP的链式调用，我们可以看做调用目标方法。

具体参考： [Spring的AOP原理.md](F:\code\scene\aop-principle\Spring的AOP原理.md) 动态代理的调用



## 五、抛出异常后处理事务

```java
/**
 * Handle a throwable, completing the transaction.
 * We may commit or roll back, depending on the configuration.
 * @param txInfo information about the current transaction
 * @param ex throwable encountered
 */
protected void completeTransactionAfterThrowing(@Nullable TransactionInfo txInfo, Throwable ex) {
    if (txInfo != null && txInfo.getTransactionStatus() != null) {
        if (logger.isTraceEnabled()) {
            logger.trace("Completing transaction for [" + txInfo.getJoinpointIdentification() +
                    "] after exception: " + ex);
        }
        // 事务注解属性不为空，通过事务注解属性判断该异常是否要回滚。
        // txInfo.transactionAttribute.rollbackOn(ex) ：参考【前置知识】五、回滚规则
        if (txInfo.transactionAttribute != null && txInfo.transactionAttribute.rollbackOn(ex)) {
            try {
                // 回滚
                txInfo.getTransactionManager().rollback(txInfo.getTransactionStatus());
            }
            catch (TransactionSystemException ex2) {
                logger.error("Application exception overridden by rollback exception", ex);
                ex2.initApplicationException(ex);
                throw ex2;
            }
            catch (RuntimeException | Error ex2) {
                logger.error("Application exception overridden by rollback exception", ex);
                throw ex2;
            }
        }
        else {
            // 这个异常不回滚，执行提交事务逻辑。
            // We don't roll back on this exception.
            // Will still roll back if TransactionStatus.isRollbackOnly() is true.
            //我们不回滚这个异常。如果TransactionStatus.isRollbackOnly()为true，仍然会回滚。
            try {
                // 提交事务见【七、提交事务】
                txInfo.getTransactionManager().commit(txInfo.getTransactionStatus());
            }
            catch (TransactionSystemException ex2) {
                logger.error("Application exception overridden by commit exception", ex);
                ex2.initApplicationException(ex);
                throw ex2;
            }
            catch (RuntimeException | Error ex2) {
                logger.error("Application exception overridden by commit exception", ex);
                throw ex2;
            }
        }
    }
}
```

### 1.1 判断回滚还是提交事务

```java
// txInfo.transactionAttribute.rollbackOn(ex) ：参考【前置知识】五、回滚规则
txInfo.transactionAttribute != null && txInfo.transactionAttribute.rollbackOn(ex)
```

返回true，则回滚事务。反之，提交事务。

### 1.2 回滚事务（判断为回滚）

```java
/**
 * 回滚的实现处理参与现有事务。委托给dorollback和doSetRollbackOnly。
 * This implementation of rollback handles participating in existing
 * transactions. Delegates to {@code doRollback} and
 * {@code doSetRollbackOnly}.
 * @see #doRollback
 * @see #doSetRollbackOnly
 */
@Override
public final void rollback(TransactionStatus status) throws TransactionException {
    // 事务已经完成，则抛出异常
    if (status.isCompleted()) {
        throw new IllegalTransactionStateException(
               "Transaction is already completed - do not call commit or rollback more than once per transaction");
    }
	
    DefaultTransactionStatus defStatus = (DefaultTransactionStatus) status;
    // 处理回滚
    processRollback(defStatus, false);
}
```

* 处理实际的回滚

```java
/**
 * Process an actual rollback.
 * The completed flag has already been checked.
 * @param status object representing the transaction
 * @throws TransactionException in case of rollback failure
 */
private void processRollback(DefaultTransactionStatus status, boolean unexpected) {
    try {
        boolean unexpectedRollback = unexpected;

        try {
            // 1.2.1、回调事务同步器的beforeCompletion方法
            // 如果是新的事务同步，则回调所有事务同步器的beforeCompletion方法
            triggerBeforeCompletion(status);
		   
            // 1.2.2、处理回滚
            // 是否有保存点
            if (status.hasSavepoint()) {
                // 有保存点，说明是嵌套事务。
                if (status.isDebug()) {
                    logger.debug("Rolling back transaction to savepoint");
                }             
                // 回滚到保存的保存点，并释放保存点
                status.rollbackToHeldSavepoint();
            }
            // 是否是新的事务
            else if (status.isNewTransaction()) {               
                if (status.isDebug()) {
                    logger.debug("Initiating transaction rollback");
                }
                 // 如果是新的事务，则执行回滚
                doRollback(status);
            }
            else {
                // 执行到这里，说明是参与到现有的事务中。                
                // Participating in larger transaction
                // 如果有事务(对象)存在。
                if (status.hasTransaction()) {
                    // （1）如果事务状态是本地回滚，默认为false。
                    //  (2) 参与到现有事务的部分执行失败是否进行全局回滚，默认是true。
                    if (status.isLocalRollbackOnly() || isGlobalRollbackOnParticipationFailure()) {
                        if (status.isDebug()) {
                            logger.debug("Participating transaction failed - marking existing transaction as rollback-only");
                        }
                        // 设置事务状态为回滚 
                        doSetRollbackOnly(status);
                    }
                    else {
                        if (status.isDebug()) {
                            logger.debug("Participating transaction failed - letting transaction originator decide on rollback");
                        }
                    }
                }
                else {
                    logger.debug("Should roll back transaction but cannot - no transaction available");
                }
                // 意外回滚只有在我们被要求提前失败时才有意义。
                // Unexpected rollback only matters here if we're asked to fail early
                // 如果事务被全局标记为仅回滚，则返回是否提前失败。
                // isFailEarlyOnGlobalRollbackOnly()默认为false。所以 !isFailEarlyOnGlobalRollbackOnly()=true       
                if (!isFailEarlyOnGlobalRollbackOnly()) {
                    // 这里我们设置为false
                    unexpectedRollback = false;
                }
            }
        }
        catch (RuntimeException | Error ex) {
            // 发生异常。 1.2.3、回调事务同步器的afterCompletion方法	
            triggerAfterCompletion(status, TransactionSynchronization.STATUS_UNKNOWN);
            throw ex;
        }
	   // 1.2.3、回调事务同步器的afterCompletion方法	
        triggerAfterCompletion(status, TransactionSynchronization.STATUS_ROLLED_BACK);

        // 如果我们有一个全局回滚标记，引发unexpected drollbackexception，实现提前失败。  
        /*
        	什么时候会出现unexpectedRollback=true的情况呢？？？
        	例如：
        	@Transactional(propagation = Propagation.REQUIRED)
            public void testRequired() throws NotRunTimeException {
                addDemoPerson();
                animalService.testRequired();                
            }            
            @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
            public void testRequired() throws NotRunTimeException {
                saveDemoAnimal();
                 // 编译时异常
        		throw new NotRunTimeException("saveDemoAnimal error");
            }                      
        */
        // Raise UnexpectedRollbackException if we had a global rollback-only marker
        if (unexpectedRollback) {
            throw new UnexpectedRollbackException(
                    "Transaction rolled back because it has been marked as rollback-only");
        }
    }
    finally {
        // 1.2.4 清除资源
        cleanupAfterCompletion(status);
    }
}
```

#### 1.2.1、回调事务同步器的beforeCompletion方法

```java
/**
 * Trigger {@code beforeCompletion} callbacks.
 * @param status object representing the transaction
 */
protected final void triggerBeforeCompletion(DefaultTransactionStatus status) {
    if (status.isNewSynchronization()) {
        // 如果是新的事务同步，则触发回调
        TransactionSynchronizationUtils.triggerBeforeCompletion();
    }
}

/**
 * 在所有当前注册的同步上触发{@code beforeCompletion}回调。
 * Trigger {@code beforeCompletion} callbacks on all currently registered synchronizations.
 * @see TransactionSynchronization#beforeCompletion()
 */
public static void triggerBeforeCompletion() {
    for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
        try {
            // 执行同步器的beforeCompletion方法
            synchronization.beforeCompletion();
        }
        catch (Throwable ex) {
            logger.debug("TransactionSynchronization.beforeCompletion threw exception", ex);
        }
    }
}
```

#### 1.2.2、处理回滚

```java
// 是否有保存点
if (status.hasSavepoint()) {
    // 有保存点，说明是嵌套事务。
    if (status.isDebug()) {
        logger.debug("Rolling back transaction to savepoint");
    }             
    //1.2.2.1 回滚到保存的保存并释放
    //回滚到为事务保留的保存点，然后立即释放保存点。
    status.rollbackToHeldSavepoint();
}
// 是否是新的事务
else if (status.isNewTransaction()) {
    // 如果是新的事务，则执行回滚
    if (status.isDebug()) {
        logger.debug("Initiating transaction rollback");
    }
    // 1.2.2.2 执行回滚
    doRollback(status);
}
else {   
    // 1.2.2.3 参与现有事务的部分执行失败设置全局回滚
    // 执行到这里，说明是参与到现有的事务中。参与部分执行失败，设置全局回滚
    // Participating in larger transaction
    // 如果有事务(对象)存在。
    if (status.hasTransaction()) {
        // （1）如果事务状态是只允许本地回滚。默认是false，创建DefaultTransactionStatus是不会设置的。
        //  (2) 参与到现有事务的部分执行失败是否需要设置全局回滚，默认是true。
        if (status.isLocalRollbackOnly() || isGlobalRollbackOnParticipationFailure()) {
            if (status.isDebug()) {
                logger.debug("Participating transaction failed - marking existing transaction as rollback-only");
            }
            // 设置为只允许回滚（设置全局回滚）
            doSetRollbackOnly(status);
        }
        else {
            if (status.isDebug()) {
                logger.debug("Participating transaction failed - letting transaction originator decide on rollback");
            }
        }
    }
    else {
        logger.debug("Should roll back transaction but cannot - no transaction available");
    }
    // 意外回滚只有在我们被要求提前失败时才有意义。isFailEarlyOnGlobalRollbackOnly()默认为false   
    // Unexpected rollback only matters here if we're asked to fail early
    // 如果事务被全局标记为仅回滚，则返回是否提前失败。默认是false
    if (!isFailEarlyOnGlobalRollbackOnly()) {
        unexpectedRollback = false;
    }
}
```

* 是否是新的事务

```
见【关键类】--> DefaultTransactionStatus --> 3、判断是否是新的事务
```

##### 1.2.2.1 回滚到保存的保存点并释放

```java
/**
 * Roll back to the savepoint that is held for the transaction
 * and release the savepoint right afterwards.
 */
public void rollbackToHeldSavepoint() throws TransactionException {
    // 获取保存点
    Object savepoint = getSavepoint();
    if (savepoint == null) {
        throw new TransactionUsageException(
                "Cannot roll back to savepoint - no savepoint associated with current transaction");
    }
    // 回滚到保存的保存点
    getSavepointManager().rollbackToSavepoint(savepoint);
    // 释放保存点
    getSavepointManager().releaseSavepoint(savepoint);
    // 设置事务状态的保存点为空
    setSavepoint(null);
}

// 返回数据源事务对象
@Override
protected SavepointManager getSavepointManager() {
    Object transaction = this.transaction;
    if (!(transaction instanceof SavepointManager)) {
        throw new NestedTransactionNotSupportedException(
                "Transaction object [" + this.transaction + "] does not support savepoints");
    }
    return (SavepointManager) transaction;
}
```

* 回滚到保存的保存点

```java
// DataSourceTransactionObject --> JdbcTransactionObjectSupport
/**
 * This implementation rolls back to the given JDBC 3.0 Savepoint.
 * @see java.sql.Connection#rollback(java.sql.Savepoint)
 */
@Override
public void rollbackToSavepoint(Object savepoint) throws TransactionException {
    // 获取数据库连接持有对象
    ConnectionHolder conHolder = getConnectionHolderForSavepoint();
    try {
        // 获取数据库连接，回滚到保存的保存点
        conHolder.getConnection().rollback((Savepoint) savepoint);
        // 设置rollbackOnly=false
        conHolder.resetRollbackOnly();
    }
    catch (Throwable ex) {
        throw new TransactionSystemException("Could not roll back to JDBC savepoint", ex);
    }
}

protected ConnectionHolder getConnectionHolderForSavepoint() throws TransactionException {
    if (!isSavepointAllowed()) {
        throw new NestedTransactionNotSupportedException(
                "Transaction manager does not allow nested transactions");
    }
    if (!hasConnectionHolder()) {
        throw new TransactionUsageException(
                "Cannot create nested transaction when not exposing a JDBC transaction");
    }
    return getConnectionHolder();
}

/**
 * Return the ConnectionHolder for this transaction object.
 */
public ConnectionHolder getConnectionHolder() {
    Assert.state(this.connectionHolder != null, "No ConnectionHolder available");
    return this.connectionHolder;
}

/**
 * Reset the rollback-only status for this resource transaction.
 * <p>Only really intended to be called after custom rollback steps which
 * keep the original resource in action, e.g. in case of a savepoint.
 * @since 5.0
 * @see org.springframework.transaction.SavepointManager#rollbackToSavepoint
 */
public void resetRollbackOnly() {
    this.rollbackOnly = false;
}
```

* 释放保存点

```java
/**
 * This implementation releases the given JDBC 3.0 Savepoint.
 * @see java.sql.Connection#releaseSavepoint
 */
@Override
public void releaseSavepoint(Object savepoint) throws TransactionException {
    // 获取数据库连接持有对象
    ConnectionHolder conHolder = getConnectionHolderForSavepoint();
    try {
        // 获取事务连接，释放保存点
        conHolder.getConnection().releaseSavepoint((Savepoint) savepoint);
    }
    catch (Throwable ex) {
        logger.debug("Could not explicitly release JDBC savepoint", ex);
    }
}
```

##### 1.2.2.2 执行回滚

```java
// JdbcTransactionManager --继承的--> DataSourceTransactionManager
@Override
protected void doRollback(DefaultTransactionStatus status) {
    // 获取数据源事务对象
    DataSourceTransactionObject txObject = (DataSourceTransactionObject) status.getTransaction();
    // 通过数据源事务对象的数据库连接持有对象，获取数据库连接
    Connection con = txObject.getConnectionHolder().getConnection();
    if (status.isDebug()) {
        logger.debug("Rolling back JDBC transaction on Connection [" + con + "]");
    }
    try {
        // 使用数据库连接进行回滚
        con.rollback();
    }
    catch (SQLException ex) {
        throw translateException("JDBC rollback", ex);
    }
}
```

#####  1.2.2.3 参与现有事务的部分执行失败设置全局回滚

参与到现有事务的部分执行失败，通过设置（同一个）数据库连接持有对象为全局回滚，控制现有事务进行回滚。

```java
@Override
protected void doSetRollbackOnly(DefaultTransactionStatus status) {
    // 获取数据源事务对象
    DataSourceTransactionObject txObject = (DataSourceTransactionObject) status.getTransaction();
    if (status.isDebug()) {
        logger.debug("Setting JDBC transaction [" + txObject.getConnectionHolder().getConnection() +
                "] rollback-only");
    }
    // 设置为只允许回滚
    txObject.setRollbackOnly();
}

public void setRollbackOnly() {
    // 获取数据库连接持有对象，并设置rollbackOnly=true。
    //【注意】这里和现有的事务是使用同一个数据库连接持有对象的
    //（能参与到同一个事务中，事务对象的数据库连接持有对象都是同一个，由前面分析可知）
    // 所以只要这里“参与到现有事务的某部分”的“数据库连接持有对象”设置了仅支持回滚。同一个事务的其他地方也会同步设置。         
    getConnectionHolder().setRollbackOnly();
}

/**
 * 将资源事务标记为仅回滚。
 * Mark the resource transaction as rollback-only.
 */
public void setRollbackOnly() {
    this.rollbackOnly = true;
}
```



#### 1.2.3、回调事务同步器的afterCompletion方法	

```java
/**
 * Trigger {@code afterCompletion} callbacks.
 * @param status object representing the transaction
 * @param completionStatus completion status according to TransactionSynchronization constants
 */
private void triggerAfterCompletion(DefaultTransactionStatus status, int completionStatus) {
    // 如果是新的事务同步
    if (status.isNewSynchronization()) {
        // 获取所有的事务同步器
        List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
        // 清除当前事务同步【注意：这里会清空所有的事务同步，就没有事务同步器活跃了】
        TransactionSynchronizationManager.clearSynchronization();
        if (!status.hasTransaction() || status.isNewTransaction()) {
            //当前范围内没有事务或者是新事务，立即调用afterCompletion回调函数
            // No transaction or new transaction for the current scope ->
            // invoke the afterCompletion callbacks immediately
            invokeAfterCompletion(synchronizations, completionStatus);
        }
        // 事务同步器不为空
        else if (!synchronizations.isEmpty()) {
            //我们参与的现有事务，外部控制
		   //这个Spring事务管理器的范围->尝试注册
		   //使用现有(JTA)事务的afterCompletion回调。
            // Existing transaction that we participate in, controlled outside
            // of the scope of this Spring transaction manager -> try to register
            // an afterCompletion callback with the existing (JTA) transaction.
            
            // JdbcTransactionManager这里的实现和上面的invokeAfterCompletion是一样的。只不过完成状态的传参不一样
            registerAfterCompletionWithExistingTransaction(status.getTransaction(), synchronizations);
        }
    }
}

/**
 * Return an unmodifiable snapshot list of all registered synchronizations
 * for the current thread.
 * @return unmodifiable List of TransactionSynchronization instances
 * @throws IllegalStateException if synchronization is not active
 * @see TransactionSynchronization
 */
public static List<TransactionSynchronization> getSynchronizations() throws IllegalStateException {
    Set<TransactionSynchronization> synchs = synchronizations.get();
    if (synchs == null) {
        throw new IllegalStateException("Transaction synchronization is not active");
    }
    // Return unmodifiable snapshot, to avoid ConcurrentModificationExceptions
    // while iterating and invoking synchronization callbacks that in turn
    // might register further synchronizations.
    if (synchs.isEmpty()) {
        return Collections.emptyList();
    }
    else {
        // Sort lazily here, not in registerSynchronization.
        List<TransactionSynchronization> sortedSynchs = new ArrayList<>(synchs);
        OrderComparator.sort(sortedSynchs);
        return Collections.unmodifiableList(sortedSynchs);
    }
}

/**
 * Deactivate transaction synchronization for the current thread.
 * Called by the transaction manager on transaction cleanup.
 * @throws IllegalStateException if synchronization is not active
 */
public static void clearSynchronization() throws IllegalStateException {
    if (!isSynchronizationActive()) {
        throw new IllegalStateException("Cannot deactivate transaction synchronization - not active");
    }
    synchronizations.remove();
}
```

```java
protected void registerAfterCompletionWithExistingTransaction(
        Object transaction, List<TransactionSynchronization> synchronizations) throws TransactionException {

    logger.debug("Cannot register Spring after-completion synchronization with existing transaction - " +
            "processing Spring after-completion callbacks immediately, with outcome status 'unknown'");
    invokeAfterCompletion(synchronizations, TransactionSynchronization.STATUS_UNKNOWN);
}

/**
 * Actually invoke the {@code afterCompletion} methods of the
 * given Spring TransactionSynchronization objects.
 * <p>To be called by this abstract manager itself, or by special implementations
 * of the {@code registerAfterCompletionWithExistingTransaction} callback.
 * @param synchronizations a List of TransactionSynchronization objects
 * @param completionStatus the completion status according to the
 * constants in the TransactionSynchronization interface
 * @see #registerAfterCompletionWithExistingTransaction(Object, java.util.List)
 * @see TransactionSynchronization#STATUS_COMMITTED
 * @see TransactionSynchronization#STATUS_ROLLED_BACK
 * @see TransactionSynchronization#STATUS_UNKNOWN
 */
protected final void invokeAfterCompletion(List<TransactionSynchronization> synchronizations, int completionStatus) {
    TransactionSynchronizationUtils.invokeAfterCompletion(synchronizations, completionStatus);
}

/**
 * Actually invoke the {@code afterCompletion} methods of the
 * given Spring TransactionSynchronization objects.
 * @param synchronizations a List of TransactionSynchronization objects
 * @param completionStatus the completion status according to the
 * constants in the TransactionSynchronization interface
 * @see TransactionSynchronization#afterCompletion(int)
 * @see TransactionSynchronization#STATUS_COMMITTED
 * @see TransactionSynchronization#STATUS_ROLLED_BACK
 * @see TransactionSynchronization#STATUS_UNKNOWN
 */
public static void invokeAfterCompletion(@Nullable List<TransactionSynchronization> synchronizations,
        int completionStatus) {

    if (synchronizations != null) {
        for (TransactionSynchronization synchronization : synchronizations) {
            try {
                // 回调afterCompletion方法
                synchronization.afterCompletion(completionStatus);
            }
            catch (Throwable ex) {
                logger.debug("TransactionSynchronization.afterCompletion threw exception", ex);
            }
        }
    }
}
```



#### 1.2.4 清除资源

```java
/**
 * Clean up after completion, clearing synchronization if necessary,
 * and invoking doCleanupAfterCompletion.
 * @param status object representing the transaction
 * @see #doCleanupAfterCompletion
 */
private void cleanupAfterCompletion(DefaultTransactionStatus status) {
    // 设置事务状态为已完成
    status.setCompleted();
    // 是否是新的事务同步
    if (status.isNewSynchronization()) {
        // 1.2.4.1、清除事务同步管理器里的相关资源
        TransactionSynchronizationManager.clear();
    }
    // 是否是新事务
    if (status.isNewTransaction()) {
        // 1.2.4.2、清除事务对象相关资源
        doCleanupAfterCompletion(status.getTransaction());
    }
    // 是否有挂起的资源
    if (status.getSuspendedResources() != null) {
        if (status.isDebug()) {
            logger.debug("Resuming suspended transaction after completion of inner transaction");
        }
        Object transaction = (status.hasTransaction() ? status.getTransaction() : null);
        // 1.2.4.3、恢复挂起的资源到事务同步管理器（有的话）
        resume(transaction, (SuspendedResourcesHolder) status.getSuspendedResources());
    }
}

/**
 * Mark this transaction as completed, that is, committed or rolled back.
 */
public void setCompleted() {
    this.completed = true;
}
```

##### 1.2.4.1 清除事务同步管理器里的相关资源（如果是新的事务同步）

```java
/**
 * Clear the entire transaction synchronization state for the current thread:
 * registered synchronizations as well as the various transaction characteristics.
 * @see #clearSynchronization()
 * @see #setCurrentTransactionName
 * @see #setCurrentTransactionReadOnly
 * @see #setCurrentTransactionIsolationLevel
 * @see #setActualTransactionActive
 */
public static void clear() {
    // 移除当前线程的事务同步
    synchronizations.remove();
    // 移除当前线程的事务名称
    currentTransactionName.remove();
    // 移除当前线程的事务的只读标识
    currentTransactionReadOnly.remove();
    // 移除当前线程的事务隔离级别
    currentTransactionIsolationLevel.remove();
     // 移除当前线程的事务活跃标识
    actualTransactionActive.remove();
}

private static final ThreadLocal<Set<TransactionSynchronization>> synchronizations =
        new NamedThreadLocal<>("Transaction synchronizations");

private static final ThreadLocal<String> currentTransactionName =
        new NamedThreadLocal<>("Current transaction name");

private static final ThreadLocal<Boolean> currentTransactionReadOnly =
        new NamedThreadLocal<>("Current transaction read-only status");

private static final ThreadLocal<Integer> currentTransactionIsolationLevel =
        new NamedThreadLocal<>("Current transaction isolation level");

private static final ThreadLocal<Boolean> actualTransactionActive =
        new NamedThreadLocal<>("Actual transaction active");
```

##### 1.2.4.2 清除事务对象相关资源（如果是新的事务）

```java
@Override
protected void doCleanupAfterCompletion(Object transaction) {
    // 转换成数据源事务对象
    DataSourceTransactionObject txObject = (DataSourceTransactionObject) transaction;
	
    // 1.2.4.2.1、解绑(移除)当前线程的数据库连接持有对象（需要的话）
    // Remove the connection holder from the thread, if exposed.
    // 如果是新建的数据库连接持有对象，则移除当前线程保存的数据库连接持有对象
    if (txObject.isNewConnectionHolder()) {
        // 对于开启事务的数据源事务对象，都会执行到这里。
        // 因为在开启时候的时候，绑定了数据库连接持有对象到当前线程，所以需要移除
        TransactionSynchronizationManager.unbindResource(obtainDataSource());
    }

    // 1.2.4.2.2、重置数据库连接
    // Reset connection.
    Connection con = txObject.getConnectionHolder().getConnection();
    try {
        // 恢复自动提交。默认都会恢复
        if (txObject.isMustRestoreAutoCommit()) {
            con.setAutoCommit(true);
        }
        // 恢复隔离级别和只读标识（需要的话）
        DataSourceUtils.resetConnectionAfterTransaction(
                con, txObject.getPreviousIsolationLevel(), txObject.isReadOnly());
    }
    catch (Throwable ex) {
        logger.debug("Could not reset JDBC Connection after transaction", ex);
    }
    
    // 1.2.4.2.3、释放数据库连接（需要的话）
    // 如果是新建的数据库连接持有对象，则释放数据库连接
    if (txObject.isNewConnectionHolder()) {
        if (logger.isDebugEnabled()) {
            logger.debug("Releasing JDBC Connection [" + con + "] after transaction");
        }
        DataSourceUtils.releaseConnection(con, this.dataSource);
    }
    
	// 1.2.4.2.4、清除数据库连接持有对象的事务状态
    txObject.getConnectionHolder().clear();
}
```

######  1.2.4.2.1 解绑(移除)当前线程的数据库连接持有对象（需要的话）

```java
/**
 * Unbind a resource for the given key from the current thread.
 * @param key the key to unbind (usually the resource factory)
 * @return the previously bound value (usually the active resource object)
 * @throws IllegalStateException if there is no value bound to the thread
 * @see ResourceTransactionManager#getResourceFactory()
 */
public static Object unbindResource(Object key) throws IllegalStateException {
    // 获取资源的key
    Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
    // 解绑当前线程的数据库连接持有对象
    Object value = doUnbindResource(actualKey);
    if (value == null) {
        throw new IllegalStateException("No value for key [" + actualKey + "] bound to thread");
    }
    return value;
}


public static Object unwrapResourceIfNecessary(Object resource) {
    Assert.notNull(resource, "Resource must not be null");
    Object resourceRef = resource;
    // unwrap infrastructure proxy
    if (resourceRef instanceof InfrastructureProxy) {
        resourceRef = ((InfrastructureProxy) resourceRef).getWrappedObject();
    }
    if (aopAvailable) {
        // now unwrap scoped proxy
        resourceRef = ScopedProxyUnwrapper.unwrapIfNecessary(resourceRef);
    }
    // DataSource直接返回
    return resourceRef;
}


@Nullable
private static Object doUnbindResource(Object actualKey) {
    // 获取资源map
    Map<Object, Object> map = resources.get();
    if (map == null) {
        return null;
    }
    // 移除DataSource对应的数据库连接持有对象
    Object value = map.remove(actualKey);
    // Remove entire ThreadLocal if empty...
    if (map.isEmpty()) {
        // 资源map为空，直接移除
        resources.remove();
    }
    // Transparently suppress a ResourceHolder that was marked as void...
    if (value instanceof ResourceHolder && ((ResourceHolder) value).isVoid()) {
        value = null;
    }
    return value;
}
```

###### 1.2.4.2.2 重置数据库连接

* 恢复自动提交。默认都会恢复

* 恢复隔离级别和只读标识（需要的话）

```java
/**
 * 根据只读标志和隔离级别，在事务之后重置给定的连接。
 * Reset the given Connection after a transaction,
 * regarding read-only flag and isolation level.
 * @param con the Connection to reset
 * @param previousIsolationLevel the isolation level to restore, if any
 * @param resetReadOnly whether to reset the connection's read-only flag
 * @since 5.2.1
 * @see #prepareConnectionForTransaction
 * @see Connection#setTransactionIsolation
 * @see Connection#setReadOnly
 */
public static void resetConnectionAfterTransaction(
        Connection con, @Nullable Integer previousIsolationLevel, boolean resetReadOnly) {

    Assert.notNull(con, "No Connection specified");
    boolean debugEnabled = logger.isDebugEnabled();
    try {
        // 如果为事务更改了隔离级别，则将事务隔离重置为先前的值。
        // Reset transaction isolation to previous value, if changed for the transaction.
        if (previousIsolationLevel != null) {
            if (debugEnabled) {
                logger.debug("Resetting isolation level of JDBC Connection [" +
                        con + "] to " + previousIsolationLevel);
            }
            con.setTransactionIsolation(previousIsolationLevel);
        }

        // 如果在事务开始时设置为true，则重置只读标志。
        // Reset read-only flag if we originally switched it to true on transaction begin.
        if (resetReadOnly) {
            if (debugEnabled) {
                logger.debug("Resetting read-only flag of JDBC Connection [" + con + "]");
            }
            con.setReadOnly(false);
        }
    }
    catch (Throwable ex) {
        logger.debug("Could not reset JDBC Connection after transaction", ex);
    }
}
```

###### 1.2.4.2.3 释放数据库连接（需要的话）

```java
/**
 * Close the given Connection, obtained from the given DataSource,
 * if it is not managed externally (that is, not bound to the thread).
 * @param con the Connection to close if necessary
 * (if this is {@code null}, the call will be ignored)
 * @param dataSource the DataSource that the Connection was obtained from
 * (may be {@code null})
 * @see #getConnection
 */
public static void releaseConnection(@Nullable Connection con, @Nullable DataSource dataSource) {
    try {
        doReleaseConnection(con, dataSource);
    }
    catch (SQLException ex) {
        logger.debug("Could not close JDBC Connection", ex);
    }
    catch (Throwable ex) {
        logger.debug("Unexpected exception on closing JDBC Connection", ex);
    }
}

/**
 * Actually close the given Connection, obtained from the given DataSource.
 * Same as {@link #releaseConnection}, but throwing the original SQLException.
 * <p>Directly accessed by {@link TransactionAwareDataSourceProxy}.
 * @param con the Connection to close if necessary
 * (if this is {@code null}, the call will be ignored)
 * @param dataSource the DataSource that the Connection was obtained from
 * (may be {@code null})
 * @throws SQLException if thrown by JDBC methods
 * @see #doGetConnection
 */
public static void doReleaseConnection(@Nullable Connection con, @Nullable DataSource dataSource) throws SQLException {
    if (con == null) {
        return;
    }
    if (dataSource != null) {
        // 这里获取到null。因为我们前面解绑了
        ConnectionHolder conHolder = (ConnectionHolder) TransactionSynchronizationManager.getResource(dataSource);
        if (conHolder != null && connectionEquals(conHolder, con)) {
            // It's the transactional Connection: Don't close it.
            conHolder.released();
            return;
        }
    }
    // 关闭数据库连接
    doCloseConnection(con, dataSource);
}

/**
 * Close the Connection, unless a {@link SmartDataSource} doesn't want us to.
 * @param con the Connection to close if necessary
 * @param dataSource the DataSource that the Connection was obtained from
 * @throws SQLException if thrown by JDBC methods
 * @see Connection#close()
 * @see SmartDataSource#shouldClose(Connection)
 */
public static void doCloseConnection(Connection con, @Nullable DataSource dataSource) throws SQLException {
    if (!(dataSource instanceof SmartDataSource) || ((SmartDataSource) dataSource).shouldClose(con)) {
        // 关闭数据库连接
        con.close();
    }
}
```

###### 1.2.4.2.4 清除数据库连接持有对象的事务状态

```java
// ConnectionHolder
@Override
public void clear() {
    super.clear();
    // 设置没有事务活跃
    this.transactionActive = false;
    // 是否支持保存点为null
    this.savepointsSupported = null;
    // 保存点个数
    this.savepointCounter = 0;
}

// ConnectionHolder 继承的 ResourceHolderSupport
/**
 * Clear the transactional state of this resource holder.
 */
public void clear() {
    // 设置资源是否与事务同步为false
    this.synchronizedWithTransaction = false;
    // 只回滚设置为false
    this.rollbackOnly = false;
    this.deadline = null;
}
```

##### 1.2.4.3 恢复挂起的资源到事务同步管理器（有的话）

```java
// 获取数据源事务对象
Object transaction = (status.hasTransaction() ? status.getTransaction() : null);
// 恢复挂起的资源
resume(transaction, (SuspendedResourcesHolder) status.getSuspendedResources());
```

```java
/**
 * 恢复给定的事务。委托给{@code doResume}模板方法，然后恢复事务同步。
 * Resume the given transaction. Delegates to the {@code doResume}
 * template method first, then resuming transaction synchronization.
 * @param transaction the current transaction object
 * @param resourcesHolder the object that holds suspended resources,
 * as returned by {@code suspend} (or {@code null} to just
 * resume synchronizations, if any)
 * @see #doResume
 * @see #suspend
 */
protected final void resume(@Nullable Object transaction, @Nullable SuspendedResourcesHolder resourcesHolder)
        throws TransactionException {
	// 判断挂起的资源持有对象是否为null
    if (resourcesHolder != null) {
        // 获取挂起的资源。这里的挂起资源经过上面的分析可知是：数据库连接持有对象
        Object suspendedResources = resourcesHolder.suspendedResources;
        if (suspendedResources != null) {
            // 重新绑定数据库连接持有对象到当前线程
            doResume(transaction, suspendedResources);
        }
        // 获取挂起的事务同步器列表
        List<TransactionSynchronization> suspendedSynchronizations = resourcesHolder.suspendedSynchronizations;
        if (suspendedSynchronizations != null) {
            // 恢复是否有实际的事务活跃
            TransactionSynchronizationManager.setActualTransactionActive(resourcesHolder.wasActive);
            // 恢复当前事务隔离级别
            TransactionSynchronizationManager.setCurrentTransactionIsolationLevel(resourcesHolder.isolationLevel);
            // 恢复只读标识
            TransactionSynchronizationManager.setCurrentTransactionReadOnly(resourcesHolder.readOnly);
            // 恢复当前事务名称
            TransactionSynchronizationManager.setCurrentTransactionName(resourcesHolder.name);
            // 重新激活当前线程的事务同步并恢复所有给定的同步。
            doResumeSynchronization(suspendedSynchronizations);
        }
    }
}

@Override
protected void doResume(@Nullable Object transaction, Object suspendedResources) {
    // 获取数据源，并绑定数据库连接持有对象到当前线程的资源map
    TransactionSynchronizationManager.bindResource(obtainDataSource(), suspendedResources);
}
/**
 * Bind the given resource for the given key to the current thread.
 * @param key the key to bind the value to (usually the resource factory)
 * @param value the value to bind (usually the active resource object)
 * @throws IllegalStateException if there is already a value bound to the thread
 * @see ResourceTransactionManager#getResourceFactory()
 */
public static void bindResource(Object key, Object value) throws IllegalStateException {
    Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
    Assert.notNull(value, "Value must not be null");
    Map<Object, Object> map = resources.get();
    // set ThreadLocal Map if none found
    if (map == null) {
        map = new HashMap<>();
        resources.set(map);
    }
    // 添加到map
    Object oldValue = map.put(actualKey, value);
    // Transparently suppress a ResourceHolder that was marked as void...
    if (oldValue instanceof ResourceHolder && ((ResourceHolder) oldValue).isVoid()) {
        oldValue = null;
    }
    if (oldValue != null) {
        throw new IllegalStateException(
                "Already value [" + oldValue + "] for key [" + actualKey + "] bound to thread");
    }
}
private static final ThreadLocal<Map<Object, Object>> resources =
			new NamedThreadLocal<>("Transactional resources");

/**
 * 重新激活当前线程的事务同步并恢复所有给定的同步。
 * Reactivate transaction synchronization for the current thread
 * and resume all given synchronizations.
 * @param suspendedSynchronizations a List of TransactionSynchronization objects
 */
private void doResumeSynchronization(List<TransactionSynchronization> suspendedSynchronizations) {
    // 初始化事务同步
    TransactionSynchronizationManager.initSynchronization();
    for (TransactionSynchronization synchronization : suspendedSynchronizations) {
        // 调用事务同步器的resume()方法
        synchronization.resume();
        // 注册事务同步器到当前线程
        TransactionSynchronizationManager.registerSynchronization(synchronization);
    }
}

/**
 * Activate transaction synchronization for the current thread.
 * Called by a transaction manager on transaction begin.
 * @throws IllegalStateException if synchronization is already active
 */
public static void initSynchronization() throws IllegalStateException {
    if (isSynchronizationActive()) {
        throw new IllegalStateException("Cannot activate transaction synchronization - already active");
    }
    // 初始化事务同步。设置LinkedHashSet
    synchronizations.set(new LinkedHashSet<>());
}

/**
 * Register a new transaction synchronization for the current thread.
 * Typically called by resource management code.
 * <p>Note that synchronizations can implement the
 * {@link org.springframework.core.Ordered} interface.
 * They will be executed in an order according to their order value (if any).
 * @param synchronization the synchronization object to register
 * @throws IllegalStateException if transaction synchronization is not active
 * @see org.springframework.core.Ordered
 */
public static void registerSynchronization(TransactionSynchronization synchronization)
        throws IllegalStateException {

    Assert.notNull(synchronization, "TransactionSynchronization must not be null");
    Set<TransactionSynchronization> synchs = synchronizations.get();
    if (synchs == null) {
        throw new IllegalStateException("Transaction synchronization is not active");
    }
    synchs.add(synchronization);
}

private static final ThreadLocal<Set<TransactionSynchronization>> synchronizations =
			new NamedThreadLocal<>("Transaction synchronizations");
```

### 1.3 提交事务（判断为提交）

```
提交事务见【七、提交事务】
```



##  六、清除事务信息（出栈）

```java
/**
 * 重置ThreadLocal里的TransactionInfo。在所有情况下都调用这个:异常或正常返回!
 * Reset the TransactionInfo ThreadLocal.
 * <p>Call this in all cases: exception or normal return!
 * @param txInfo information about the current transaction (may be {@code null})
 */
protected void cleanupTransactionInfo(@Nullable TransactionInfo txInfo) {    
    if (txInfo != null) {
        // 见【关键类】TransactionInfo的2、恢复旧的事务信息（事务信息出栈）
        txInfo.restoreThreadLocalStatus();
    }
}
```



## 七、提交事务

```java
/**
 * 在成功完成调用后执行，但不是在处理异常之后执行。如果我们没有创建一个事务，什么也不做。
 * Execute after successful completion of call, but not after an exception was handled.
 * Do nothing if we didn't create a transaction.
 * @param txInfo information about the current transaction
 */
protected void commitTransactionAfterReturning(@Nullable TransactionInfo txInfo) {
    if (txInfo != null && txInfo.getTransactionStatus() != null) {
        // 事务信息和事务状态都不为null，则获取事务管理器(JdbcTransactionManager)执行提交事务
        if (logger.isTraceEnabled()) {
            logger.trace("Completing transaction for [" + txInfo.getJoinpointIdentification() + "]");
        }
        txInfo.getTransactionManager().commit(txInfo.getTransactionStatus());
    }
}
```

```java
/**
 * This implementation of commit handles participating in existing
 * transactions and programmatic rollback requests.
 * Delegates to {@code isRollbackOnly}, {@code doCommit}
 * and {@code rollback}.
 * @see org.springframework.transaction.TransactionStatus#isRollbackOnly()
 * @see #doCommit
 * @see #rollback
 */
@Override
public final void commit(TransactionStatus status) throws TransactionException {
    // 事务状态如果完成了，则抛出异常
    if (status.isCompleted()) {        
        throw new IllegalTransactionStateException(
                "Transaction is already completed - do not call commit or rollback more than once per transaction");
    }

    // 1、检查并应用回滚（检查是否需要进行回滚）
    DefaultTransactionStatus defStatus = (DefaultTransactionStatus) status;    
    // 如果事务状态是只允许回滚的，则直接回滚。默认是false，创建DefaultTransactionStatus是不会设置的。
    if (defStatus.isLocalRollbackOnly()) {     
        if (defStatus.isDebug()) {
            logger.debug("Transactional code has requested rollback");
        }
        // 执行回滚。同【五、抛出异常后处理事务】的1.2 回滚事务
        processRollback(defStatus, false);
        return;
    }
    
    // 默认情况：!shouldCommitOnGlobalRollbackOnly() = true。
    // 如果是全局回滚，则执行回滚。
    // 全局回滚的标识通常在以下位置设置
    // 【五、抛出异常后处理事务】 --> 1.2 回滚事务 --> 1.2.2、处理回滚 --> 1.2.2.3 参与现有事务的部分执行失败设置全局回滚 
    /*    
    示例场景一：
    // personService
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void testRequired() throws NotRunTimeException {
        addDemoPerson();
        animalService.testRequired();
    }
    
    // animalService
    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void testRequired() throws NotRunTimeException {
        saveDemoAnimal();
    }
     private void saveDemoAnimal() throws NotRunTimeException {
        String name = UUID.randomUUID().toString().substring(0, 5);
        Integer age = new Random().nextInt();
        Animal p2 = new Animal().setName(name).setAge(age);
        save(p2);
        log.info("animal={}", p2);
        // 编译时异常
        throw new NotRunTimeException("saveDemoAnimal error");      
    }
    1、animalService的testRequired方法加入personService的事务中。
    saveDemoAnimal方法抛出编译时异常，animalService的testRequired方法执行失败，
    然后执行【1.2 回滚事务】的 【1.2.2.3 参与现有事务的部分执行失败设置全局回滚】。即设置数据库连接持有对象进行全局回滚。
    
    2、最后抛出编译时异常到personService的testRequired方法。
    抛出的异常没有命中回滚规则。见【五、抛出异常后处理事务】的 【1.1 判断回滚还是提交事务】
    然后执行commit方法，就执行到了这里的if判断，
    发现defStatus.isGlobalRollbackOnly()里的“数据库连接持有对象”（同一个事务使用同一个） 设置了全局回滚。
    所以最后进入if，执行回滚方法。 
    
	这样就可以实现同一个事务中的多个参与部分，只要有一个参与部分符合回滚规则设置了全局回滚，
	即使其他参与部分没有命中回滚规则，也会进行全局回滚。        
    
    示例场景二:    
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
    @Override
    public void manualSaveAnimal() {
        // 手动开启事务
        TransactionStatus transactionStatus = platformTransactionManager.getTransaction(transactionDefinition);
        try {
            saveDemoAnimal();
            // 手动提交事务
            platformTransactionManager.commit(transactionStatus);
        } catch (Exception e) {
            log.error("error;",e);
            // 手动回滚事务
            platformTransactionManager.rollback(transactionStatus);
        }
    }
     private void saveDemoAnimal() {
        String name = UUID.randomUUID().toString().substring(0, 5);
        Integer age = new Random().nextInt();
        Animal p2 = new Animal().setName(name).setAge(age);
        save(p2);
        log.info("animal={}", p2);
        throw new ServiceException("saveDemoAnimal error");
    }
    手动事务的底层和我们这里实际是一样的。
    
    1、testGlobalRollback方法和manualSaveAnimal方法属于同一个事务。
       调用testGlobalRollback方法，manualSaveAnimal方法加入到testGlobalRollback方法现有的事务中。
    
    2、manualSaveAnimal方法执行失败，抛出的异常被catch了，
    然后执行【1.2 回滚事务】的 【1.2.2.3 参与现有事务的部分执行失败设置全局回滚】。即设置数据库连接持有对象进行全局回滚
    最后manualSaveAnimal方法执行完成。
    
    3、因为manualSaveAnimal方法没有抛出异常，testGlobalRollback方法没有感知到异常，所以会执行commit方法。
    然后就执行到了这里的if判断，发现defStatus.isGlobalRollbackOnly()里的“数据库连接持有对象”（同一个事务使用同一个） 设置了	  全局回滚。所以最后进入if，执行回滚方法。
    */      
    if (!shouldCommitOnGlobalRollbackOnly() && defStatus.isGlobalRollbackOnly()) {
        // 需要进行回滚
        if (defStatus.isDebug()) {
            logger.debug("Global transaction is marked as rollback-only but transactional code requested commit");
        }       
        // 【注意】这里的unexpected传参为true，因为有全局回滚则会抛出UnexpectedRollbackException异常。
         // 执行回滚。同【五、抛出异常后处理事务】的1.2 回滚事务
        processRollback(defStatus, true);
        return;
    }

	// 2、处理提交
    processCommit(defStatus);
}

// 返回是否以全局方式对已标记为仅回滚的事务调用doCommit。默认为不调用
protected boolean shouldCommitOnGlobalRollbackOnly() {
    return false;
}

/**
 * Determine the rollback-only flag via checking this TransactionStatus.
 * <p>Will only return "true" if the application called {@code setRollbackOnly}
 * on this TransactionStatus object.
 */
public boolean isLocalRollbackOnly() {
    return this.rollbackOnly;
}

// 是否进行全局回滚
@Override
public boolean isGlobalRollbackOnly() {
    // 数据源事务对象属于SmartTransactionObject类型。
    return ((this.transaction instanceof SmartTransactionObject) &&
            ((SmartTransactionObject) this.transaction).isRollbackOnly());
}
// 获取数据库连接持有对象判断是否只能回滚
@Override
public boolean isRollbackOnly() {    
    return getConnectionHolder().isRollbackOnly();
}

/**
 * Return whether the resource transaction is marked as rollback-only.
 */
public boolean isRollbackOnly() {
    return this.rollbackOnly;
}
```

### 1、检查并应用回滚

需要的话，则执行回滚。反之，执行处理提交。

目的：**实现同一个事务中的多个参与部分，只要有一个参与部分符合回滚规则设置了全局回滚，即使其他参与部分没有命中回滚规则，也会进行全局回滚。** 



### 2、 处理实际提交

```java
/**
 * 处理实际提交。仅回滚标志已被检查并应用。
 * Process an actual commit.
 * Rollback-only flags have already been checked and applied.
 * @param status object representing the transaction
 * @throws TransactionException in case of commit failure
 */
private void processCommit(DefaultTransactionStatus status) throws TransactionException {
    try {
        boolean beforeCompletionInvoked = false;

        try {
            boolean unexpectedRollback = false;
            // 2.1 准备提交
            prepareForCommit(status);
            // 2.2 回调事务同步器的beforeCommit方法
            triggerBeforeCommit(status);
            // 2.3 回调事务同步器的beforeCompletion方法
            triggerBeforeCompletion(status);
            // 标记beforeCompletion方法调用过了
            beforeCompletionInvoked = true;
		   
            // 2.4 处理提交
            if (status.hasSavepoint()) {
                if (status.isDebug()) {
                    logger.debug("Releasing transaction savepoint");
                }
                unexpectedRollback = status.isGlobalRollbackOnly();
                status.releaseHeldSavepoint();
            }
            else if (status.isNewTransaction()) {
                if (status.isDebug()) {
                    logger.debug("Initiating transaction commit");
                }
                unexpectedRollback = status.isGlobalRollbackOnly();
                doCommit(status);
            }
            else if (isFailEarlyOnGlobalRollbackOnly()) {
                unexpectedRollback = status.isGlobalRollbackOnly();
            }

            // Throw UnexpectedRollbackException if we have a global rollback-only
            // marker but still didn't get a corresponding exception from commit.
            if (unexpectedRollback) {
                throw new UnexpectedRollbackException(
                        "Transaction silently rolled back because it has been marked as rollback-only");
            }
        }
        catch (UnexpectedRollbackException ex) {
            // 只可能是doCommit方法抛出的异常
            // can only be caused by doCommit
            // 2.6 回调事务同步器的afterCompletion方法
            triggerAfterCompletion(status, TransactionSynchronization.STATUS_ROLLED_BACK);
            // 抛出异常。让调用者感知
            throw ex;
        }
        catch (TransactionException ex) {
            // 只可能是doCommit方法抛出的异常
            // can only be caused by doCommit
            // 提交失败是否执行回滚。JdbcTransactionManager默认是false
            if (isRollbackOnCommitFailure()) {
                // (提交失败)执行回滚
                doRollbackOnCommitException(status, ex);
            }
            else {
                // 2.6 回调事务同步器的afterCompletion方法
                triggerAfterCompletion(status, TransactionSynchronization.STATUS_UNKNOWN);
            }
            //  抛出异常
            throw ex;
        }
        catch (RuntimeException | Error ex) {
   		   // 执行到这里，说明异常不是在提交doCommit的时候发生的。
            // 事务同步器的beforeCompletion如果没有被调用过，则执行回调
            if (!beforeCompletionInvoked) {
                // 2.3 回调事务同步器的beforeCompletion方法
                triggerBeforeCompletion(status);
            }
            // 执行回滚
            doRollbackOnCommitException(status, ex);
            throw ex;
        }
	    // 触发afterCommit回调，即使抛出异常传播到调用者，但事务仍被视为已提交。
        // Trigger afterCommit callbacks, with an exception thrown there
        // propagated to callers but the transaction still considered as committed.
        try {
            // 2.5 回调事务同步器的afterCommit方法
            triggerAfterCommit(status);
        }
        finally {
            // 2.6 回调事务同步器的afterCompletion方法
            triggerAfterCompletion(status, TransactionSynchronization.STATUS_COMMITTED);
        }

    }
    finally {
        // 2.7 清除资源
        cleanupAfterCompletion(status);
    }
}
```

* doRollbackOnCommitException

```java
/**
 * Invoke {@code doRollback}, handling rollback exceptions properly.
 * @param status object representing the transaction
 * @param ex the thrown application exception or error
 * @throws TransactionException in case of rollback failure
 * @see #doRollback
 */
private void doRollbackOnCommitException(DefaultTransactionStatus status, Throwable ex) throws TransactionException {
    try {
        // 如果是新的事务
        if (status.isNewTransaction()) {
            if (status.isDebug()) {
                logger.debug("Initiating transaction rollback after commit exception", ex);
            }
            // 执行回滚
            // 见【五、抛出异常后处理事务】的【1.2.2 处理回滚】的 【1.2.2.2 执行回滚】
            doRollback(status);
        }
        //  如果有事务(对象)存在，而且参与到现有事务的部分执行失败需要设置全局回滚，则设置全局回滚
        //  isGlobalRollbackOnParticipationFailure()默认是true。
        else if (status.hasTransaction() && isGlobalRollbackOnParticipationFailure()) {
            if (status.isDebug()) {
                logger.debug("Marking existing transaction as rollback-only after commit exception", ex);
            }
            // 设置全局回滚
             // 见【五、抛出异常后处理事务】的【同1.2.2 处理回滚】的 【1.2.2.3 参与现有事务的部分执行失败设置全局回滚】
            doSetRollbackOnly(status);
        }
    }
    catch (RuntimeException | Error rbex) {
        logger.error("Commit exception overridden by rollback exception", ex);
        triggerAfterCompletion(status, TransactionSynchronization.STATUS_UNKNOWN);
        throw rbex;
    }
    // 同【五、抛出异常后处理事务】--> 【1.2 回滚事务（判断为回滚）】--> 【1.2.3、回调事务同步器的afterCompletion方法】
    triggerAfterCompletion(status, TransactionSynchronization.STATUS_ROLLED_BACK);
}
```

#### 2.1 准备提交

```java
/**
 * Make preparations for commit, to be performed before the
 * {@code beforeCommit} synchronization callbacks occur.
 * <p>Note that exceptions will get propagated to the commit caller
 * and cause a rollback of the transaction.
 * @param status the status representation of the transaction
 * @throws RuntimeException in case of errors; will be <b>propagated to the caller</b>
 * (note: do not throw TransactionException subclasses here!)
 */
protected void prepareForCommit(DefaultTransactionStatus status) {
}
```

#### 2.2 回调事务同步器的beforeCommit方法

```java
/**
 * Trigger {@code beforeCommit} callbacks.
 * @param status object representing the transaction
 */
protected final void triggerBeforeCommit(DefaultTransactionStatus status) {
    // 如果是新的事务同步
    if (status.isNewSynchronization()) {
        // 触发回调
        TransactionSynchronizationUtils.triggerBeforeCommit(status.isReadOnly());
    }
}
/**
 * Trigger {@code beforeCommit} callbacks on all currently registered synchronizations.
 * @param readOnly whether the transaction is defined as read-only transaction
 * @throws RuntimeException if thrown by a {@code beforeCommit} callback
 * @see TransactionSynchronization#beforeCommit(boolean)
 */
public static void triggerBeforeCommit(boolean readOnly) {
    // 获取所有的事务同步器，遍历执行beforeCommit方法
    for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {   
        synchronization.beforeCommit(readOnly);
    }
}
```

#### 2.3 回调事务同步器的beforeCompletion方法

```
同【五、抛出异常后处理事务】--> 【1.2 回滚事务（判断为回滚）】--> 【1.2.1 回调事务同步器的beforeCompletion方法】
```

#### 2.4 处理提交

```java
// 如果事务状态有保存点
if (status.hasSavepoint()) {
    // 嵌套事务才有
    if (status.isDebug()) {
        logger.debug("Releasing transaction savepoint");
    }
    // 事务状态是否只允许全局回滚。一般执行到这里，这里获取到的都是false，因为前面已经检查过是否需要全局回滚了
    unexpectedRollback = status.isGlobalRollbackOnly();
    // 2.4.1 释放保存的保存点
    status.releaseHeldSavepoint();
}
// 如果是新(开启)的事务
else if (status.isNewTransaction()) {
    if (status.isDebug()) {
        logger.debug("Initiating transaction commit");
    }
    // 事务状态是否只允许全局回滚。一般执行到这里，这里获取到的都是false，因为前面已经检查过是否需要全局回滚了
    unexpectedRollback = status.isGlobalRollbackOnly();
    //  2.4.2 执行提交
    doCommit(status);
}
// 如果事务被全局标记为仅回滚，则返回是否提前失败。默认是false
else if (isFailEarlyOnGlobalRollbackOnly()) {  
    unexpectedRollback = status.isGlobalRollbackOnly();
}

// 如果我们有一个全局回滚，抛出unexpected drollbackexception标记，但仍然没有从commit得到相应的异常。
// 这里获取到的unexpectedRollback都是false，因为前面已经检查过是否需要全局回滚。 见【1、检查并应用回滚】
// Throw UnexpectedRollbackException if we have a global rollback-only
// marker but still didn't get a corresponding exception from commit.
if (unexpectedRollback) {
    throw new UnexpectedRollbackException(
            "Transaction silently rolled back because it has been marked as rollback-only");
}
```

* JdbcTransactionManager

```java
/**
 * 如果事务被全局标记为仅回滚，则返回是否提前失败。默认是false
 * Return whether to fail early in case of the transaction being globally marked
 * as rollback-only.
 * @since 2.0
 */
public final boolean isFailEarlyOnGlobalRollbackOnly() {
    return this.failEarlyOnGlobalRollbackOnly;
}
private boolean failEarlyOnGlobalRollbackOnly = false;
```

##### 2.4.1 释放保存的保存点（嵌套事务）

```java
/**
 * 释放为事务持有的保存点。
 * Release the savepoint that is held for the transaction.
 */
public void releaseHeldSavepoint() throws TransactionException {
    // 获取保存点
    Object savepoint = getSavepoint();
    if (savepoint == null) {
        throw new TransactionUsageException(
                "Cannot release savepoint - no savepoint associated with current transaction");
    }
    // 获取事务对象，然后释放保存点
    getSavepointManager().releaseSavepoint(savepoint);
    // 设置事务状态的保存点为null
    setSavepoint(null);
}
/**
 * Get the savepoint for this transaction, if any.
 */
@Nullable
protected Object getSavepoint() {
    return this.savepoint;
}

@Override
protected SavepointManager getSavepointManager() {
    Object transaction = this.transaction;
    if (!(transaction instanceof SavepointManager)) {
        throw new NestedTransactionNotSupportedException(
                "Transaction object [" + this.transaction + "] does not support savepoints");
    }
    return (SavepointManager) transaction;
}


/**
 * This implementation releases the given JDBC 3.0 Savepoint.
 * @see java.sql.Connection#releaseSavepoint
 */
@Override
public void releaseSavepoint(Object savepoint) throws TransactionException {
    // 获取数据库连接持有对象
    ConnectionHolder conHolder = getConnectionHolderForSavepoint();
    try {
        // 获取数据库连接，是否保存点
        conHolder.getConnection().releaseSavepoint((Savepoint) savepoint);
    }
    catch (Throwable ex) {
        logger.debug("Could not explicitly release JDBC savepoint", ex);
    }
}

protected ConnectionHolder getConnectionHolderForSavepoint() throws TransactionException {
    if (!isSavepointAllowed()) {
        throw new NestedTransactionNotSupportedException(
                "Transaction manager does not allow nested transactions");
    }
    if (!hasConnectionHolder()) {
        throw new TransactionUsageException(
                "Cannot create nested transaction when not exposing a JDBC transaction");
    }
    return getConnectionHolder();
}
```

##### 2.4.2 执行提交

```java
@Override
protected void doCommit(DefaultTransactionStatus status) {
    // 获取数据源事务对象
    DataSourceTransactionObject txObject = (DataSourceTransactionObject) status.getTransaction();
    // 使用数据库连接持有对象获取数据库连接
    Connection con = txObject.getConnectionHolder().getConnection();
    if (status.isDebug()) {
        logger.debug("Committing JDBC transaction on Connection [" + con + "]");
    }
    try {
        // 提交事务
        con.commit();
    }
    catch (SQLException ex) {
        throw translateException("JDBC commit", ex);
    }
}
```

#### 2.5 回调事务同步器的afterCommit方法

```java
/**
 * Trigger {@code afterCommit} callbacks.
 * @param status object representing the transaction
 */
private void triggerAfterCommit(DefaultTransactionStatus status) {
    // 如果是新的事务同步
    if (status.isNewSynchronization()) {
        // 触发回调
        TransactionSynchronizationUtils.triggerAfterCommit();
    }
}	
/**
 * Trigger {@code afterCommit} callbacks on all currently registered synchronizations.
 * @throws RuntimeException if thrown by a {@code afterCommit} callback
 * @see TransactionSynchronizationManager#getSynchronizations()
 * @see TransactionSynchronization#afterCommit()
 */
public static void triggerAfterCommit() {
    invokeAfterCommit(TransactionSynchronizationManager.getSynchronizations());
}

/**
 * Actually invoke the {@code afterCommit} methods of the
 * given Spring TransactionSynchronization objects.
 * @param synchronizations a List of TransactionSynchronization objects
 * @see TransactionSynchronization#afterCommit()
 */
public static void invokeAfterCommit(@Nullable List<TransactionSynchronization> synchronizations) {
    if (synchronizations != null) {
        // 获取所有的事务同步器，遍历执行afterCommit方法
        for (TransactionSynchronization synchronization : synchronizations) {            
            synchronization.afterCommit();
        }
    }
}
```

#### 2.6 回调事务同步器的afterCompletion方法

```
同【五、抛出异常后处理事务】--> 【1.2 回滚事务（判断为回滚）】--> 【1.2.3、回调事务同步器的afterCompletion方法】
```

####  2.7 清除资源

```
同【五、抛出异常后处理事务】--> 【1.2 回滚事务（判断为回滚）】--> 【1.2.4 清除资源】
```

# 关键类

## DefaultTransactionStatus

### 1、创建DefaultTransactionStatus并适当初始化事务同步

```java
/**
 * Create a new TransactionStatus for the given arguments,
 * also initializing transaction synchronization as appropriate.
 *为给定的参数创建一个新的TransactionStatus;*也适当初始化事务同步。
 * @see #newTransactionStatus
 * @see #prepareTransactionStatus
 */
protected final DefaultTransactionStatus prepareTransactionStatus(
        TransactionDefinition definition, @Nullable Object transaction, boolean newTransaction,
        boolean newSynchronization, boolean debug, @Nullable Object suspendedResources) {
	// 1.1 新建DefaultTransactionStatus
    DefaultTransactionStatus status = newTransactionStatus(
            definition, transaction, newTransaction, newSynchronization, debug, suspendedResources);
    // 准备事务同步
    prepareSynchronization(status, definition);
    return status;
}
```

#### 1.1. 创建DefaultTransactionStatus

```java
/**
 * 根据给定的参数，创建事务状态
 * Create a TransactionStatus instance for the given arguments.
 * definition：这里通常是事务的注解属性
 * transaction：事务对象，通常是数据源事务对象
 * newTransaction：是否是新的事务
 * newSynchronization：新的事务同步，即使设置成true，也要求当前线程没有事务同步器。才能为true
 * debug：是否应启用调试日志记录（这个参数不重要，我们可以先忽略）
 * suspendedResources：挂起的资源
 */
protected DefaultTransactionStatus newTransactionStatus(
        TransactionDefinition definition, @Nullable Object transaction, boolean newTransaction,
        boolean newSynchronization, boolean debug, @Nullable Object suspendedResources) {
	// 如果newSynchronization=true而且当前线程没有事务同步器，则为事务打开了新的同步（actualNewSynchronization=true）
    boolean actualNewSynchronization = newSynchronization &&            
            !TransactionSynchronizationManager.isSynchronizationActive();   
    // definition.isReadOnly()是否只读。一般情况下，默认是false。如果没有通过@Transactional的readOnly指定的话。
    return new DefaultTransactionStatus(
            transaction, newTransaction, actualNewSynchronization,
            definition.isReadOnly(), debug, suspendedResources);
}

/**
 * Return if transaction synchronization is active for the current thread.
 * Can be called before register to avoid unnecessary instance creation.
 * 如果当前线程的事务同步是活动的，则返回。可以在注册之前调用，以避免不必要的实例创建。 
 * @see #registerSynchronization
 */
public static boolean isSynchronizationActive() {
    return (synchronizations.get() != null);
}
private static final ThreadLocal<Set<TransactionSynchronization>> synchronizations =
			new NamedThreadLocal<>("Transaction synchronizations");
```

* 构造方法

```java
public class DefaultTransactionStatus extends AbstractTransactionStatus {

	@Nullable
	private final Object transaction;

	private final boolean newTransaction;

	private final boolean newSynchronization;

	private final boolean readOnly;

	private final boolean debug;

	@Nullable
	private final Object suspendedResources;

	/**
	 transaction: 代表底层事务对象，可以保存内部事务实现的状态。
	 newTransaction: 指示这个事务是否是一个新事务；如果是参与到一个现有的事务中，则该值为false。
	 newSynchronization: 表示是否为给定事务打开了新的事务同步。
	 readOnly: 指示事务是否标记为只读。一般情况下，默认是false。如果没有通过@Transactional的readOnly指定的话。
	 debug: 指示是否应启用调试日志记录。这可以通过缓存的方式避免重复调用日志系统来检查是否应该启用调试日志。
	 suspendedResources: 用于持有任何已为此事务挂起的资源。	 
	 
	 * Create a new {@code DefaultTransactionStatus} instance.	
	 * @param transaction underlying transaction object that can hold state
	 * for the internal transaction implementation
	 * @param newTransaction if the transaction is new, otherwise participating
	 * in an existing transaction
	 * @param newSynchronization if a new transaction synchronization has been
	 * opened for the given transaction
	 * @param readOnly whether the transaction is marked as read-only
	 * @param debug should debug logging be enabled for the handling of this transaction?
	 * Caching it in here can prevent repeated calls to ask the logging system whether
	 * debug logging should be enabled.
	 * @param suspendedResources a holder for resources that have been suspended
	 * for this transaction, if any
	 */
	public DefaultTransactionStatus(
			@Nullable Object transaction, boolean newTransaction, boolean newSynchronization,
			boolean readOnly, boolean debug, @Nullable Object suspendedResources) {

		this.transaction = transaction;
		this.newTransaction = newTransaction;
		this.newSynchronization = newSynchronization;
		this.readOnly = readOnly;
		this.debug = debug;
		this.suspendedResources = suspendedResources;
	}
    
    // ....
}
```

`transaction`: 可以是一个事务对象，或 `null`（注释中的 `@Nullable` 表明该参数可以为 `null`）。

`newTransaction`: 布尔值，指示是否是一个新事务。

`newSynchronization`: 布尔值，指示是否为事务打开了新的同步。

`readOnly`: 布尔值，指示事务是否为只读。

`debug`: 布尔值，指示是否启用调试模式。

`suspendedResources`: 可以是挂起的资源对象，或 `null`。挂起的资源对象通常是SuspendedResourcesHolder

* 继承结构

![image-20240725164607476](https://lu-note.oss-cn-shenzhen.aliyuncs.com/notes/work/image-20240725164607476.png)



#### 1.2 适当初始化事务同步

```java
/**
 * 根据需要初始化事务同步。
 * Initialize transaction synchronization as appropriate.
 */
protected void prepareSynchronization(DefaultTransactionStatus status, TransactionDefinition definition) {
    // 如果是新的事务同步。一般新开启的事务这里都是true
    if (status.isNewSynchronization()) {
        // 设置当前线程是否有实际事务活跃。
        TransactionSynchronizationManager.setActualTransactionActive(status.hasTransaction());
        // 绑定当前事务隔离级别到当前线程
        TransactionSynchronizationManager.setCurrentTransactionIsolationLevel(
                // 不使用默认的隔离级别，则使用指定的具体隔离级别
                definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT ?
                        definition.getIsolationLevel() : null);
        // 绑定当前事务是否只读到当前线程
        TransactionSynchronizationManager.setCurrentTransactionReadOnly(definition.isReadOnly());
        // 绑定事务名称到当前线程
        TransactionSynchronizationManager.setCurrentTransactionName(definition.getName());
        // 初始化同步
        TransactionSynchronizationManager.initSynchronization();
    }
}

/**
 * 如果为此事务打开了新的事务同步，则返回true。
 * Return if a new transaction synchronization has been opened
 * for this transaction.
 */
public boolean isNewSynchronization() {
    return this.newSynchronization;
}

/**
 * 返回是否有实际的活动事务。
 * Return whether there is an actual transaction active.
 */
public boolean hasTransaction() {
    // 判断数据源事务对象是否为空。如果不为空，说明有实际的活动事务，反之则无。
    return (this.transaction != null);
}

/**
 * 激活当前线程的事务同步。由事务管理器在事务开始时调用。
 * Activate transaction synchronization for the current thread.
 * Called by a transaction manager on transaction begin.
 * @throws IllegalStateException if synchronization is already active
 */
public static void initSynchronization() throws IllegalStateException {
    // 如果已经有同步器激活，则抛出异常。一开始是没激活的
    if (isSynchronizationActive()) {
        throw new IllegalStateException("Cannot activate transaction synchronization - already active");
    }
    // 设置LinkedHashSet到synchronizations
    synchronizations.set(new LinkedHashSet<>());
}
private static final ThreadLocal<Set<TransactionSynchronization>> synchronizations =
			new NamedThreadLocal<>("Transaction synchronizations");

/**
 * 如果当前线程的事务同步是活动的，则返回。可以在注册前调用，避免不必要的实例创建。
 * Return if transaction synchronization is active for the current thread.
 * Can be called before register to avoid unnecessary instance creation.
 * @see #registerSynchronization
 */
public static boolean isSynchronizationActive() {
    return (synchronizations.get() != null);
}
```





### 2、创建保存点并保存到事务状态

```java
// 位于DefaultTransactionStatus的AbstractTransactionStatus 
/**
 * 创建一个保存点并保存到事务状态
 * Create a savepoint and hold it for the transaction.
 * @throws org.springframework.transaction.NestedTransactionNotSupportedException
 * if the underlying transaction does not support savepoints
 */
public void createAndHoldSavepoint() throws TransactionException {
    // 获取保存点管理器创建保存点并保存
    setSavepoint(getSavepointManager().createSavepoint());
}

/**
 * Set a savepoint for this transaction. Useful for PROPAGATION_NESTED.
 * @see org.springframework.transaction.TransactionDefinition#PROPAGATION_NESTED
 */
protected void setSavepoint(@Nullable Object savepoint) {
    this.savepoint = savepoint;
}
// 保存点
private Object savepoint;
```

#### 2.1 获取保存点管理器

```java
/**
 * 这个实现返回了{@link SavepointManager}接口的底层事务对象(如果有的话)。
 * This implementation exposes the {@link SavepointManager} interface
 * of the underlying transaction object, if any.
 * @throws NestedTransactionNotSupportedException if savepoints are not supported
 * @see #isTransactionSavepointManager()
 */
@Override
protected SavepointManager getSavepointManager() {
    // DefaultTransactionStatus的事务对象（数据源事务对象DataSourceTransactionObject）
    Object transaction = this.transaction;
    if (!(transaction instanceof SavepointManager)) {
        throw new NestedTransactionNotSupportedException(
                "Transaction object [" + this.transaction + "] does not support savepoints");
    }
    return (SavepointManager) transaction;
}
```

![image-20240728000011608](https://lu-note.oss-cn-shenzhen.aliyuncs.com/notes/work/image-20240728000011608.png)

#### 2.2 创建保存点

```java
/**
 * This implementation creates a JDBC 3.0 Savepoint and returns it.
 * @see java.sql.Connection#setSavepoint
 */
@Override
public Object createSavepoint() throws TransactionException {
    // 2.2.1、获取数据库连接持有对象
    ConnectionHolder conHolder = getConnectionHolderForSavepoint();
    try {
        // 数据库连接不支持保存点，直接抛出异常
        if (!conHolder.supportsSavepoints()) {
            throw new NestedTransactionNotSupportedException(
                    "Cannot create a nested transaction because savepoints are not supported by your JDBC driver");
        }
        // 只允许回滚的话，则直接报错
        if (conHolder.isRollbackOnly()) {
            throw new CannotCreateTransactionException(
                    "Cannot create savepoint for transaction which is already marked as rollback-only");
        }
        // 2.2.2、创建保存点
        return conHolder.createSavepoint();
    }
    catch (SQLException ex) {
        throw new CannotCreateTransactionException("Could not create JDBC savepoint", ex);
    }
}

protected ConnectionHolder getConnectionHolderForSavepoint() throws TransactionException {
    // 
    // 【三、创建事务信息（如果有必要的话）】 -->【1.1 获取事务对象】
    // 获取的数据源事务对象DataSourceTransactionObject是支持保存点的
    if (!isSavepointAllowed()) {
        throw new NestedTransactionNotSupportedException(
                "Transaction manager does not allow nested transactions");
    }
    // 没有数据库连接持有对象，直接抛出异常
    if (!hasConnectionHolder()) {
        throw new TransactionUsageException(
                "Cannot create nested transaction when not exposing a JDBC transaction");
    }
    // 返回数据源事务对象DataSourceTransactionObject里的数据库连接持有对象
    return getConnectionHolder();
}
/**
 * Return the ConnectionHolder for this transaction object.
 */
public ConnectionHolder getConnectionHolder() {
    Assert.state(this.connectionHolder != null, "No ConnectionHolder available");
    return this.connectionHolder;
}
```

* ConnectionHolder

```java
/**
 *  返回是否支持JDBC 3.0保存点。
 *  在这个ConnectionHolder的生命周期中缓存标志。 
 * Return whether JDBC 3.0 Savepoints are supported.
 * Caches the flag for the lifetime of this ConnectionHolder.
 * @throws SQLException if thrown by the JDBC driver
 */
public boolean supportsSavepoints() throws SQLException {
    if (this.savepointsSupported == null) {
        this.savepointsSupported = getConnection().getMetaData().supportsSavepoints();
    }
    return this.savepointsSupported;
}
@Nullable
private Boolean savepointsSupported;

/**
 * 返回资源事务是否标记为仅回滚。默认为false
 * Return whether the resource transaction is marked as rollback-only.
 */
public boolean isRollbackOnly() {
    return this.rollbackOnly;
}
private boolean rollbackOnly = false;

/**
 * 为当前连接创建一个新的JDBC 3.0保存点，使用为该连接生成的唯一保存点名称。
 * Create a new JDBC 3.0 Savepoint for the current Connection,
 * using generated savepoint names that are unique for the Connection.
 * @return the new Savepoint
 * @throws SQLException if thrown by the JDBC driver
 */
public Savepoint createSavepoint() throws SQLException {
    // 保存点(数量)计数器加一
    this.savepointCounter++;
    // 获取数据库连接，创建保存点。
    return getConnection().setSavepoint(SAVEPOINT_NAME_PREFIX + this.savepointCounter);
}
// 保存点(数量)计数器
private int savepointCounter = 0;
/**
 * 保存点名称前缀
 * Prefix for savepoint names.
 */
public static final String SAVEPOINT_NAME_PREFIX = "SAVEPOINT_";
```

### 3、判断是否是新的事务

```java
/**
*返回当前事务是否为新事务;否则将参与现有事务，或者可能首先不在实际事务中运行。
* Return whether the present transaction is new; 
* otherwise participating in an existing transaction, 
* or potentially not running in an actual transaction in the first place.
*/
@Override
public boolean isNewTransaction() {
    // 判断是否有事务对象和newTransaction是否为true
   return (hasTransaction() && this.newTransaction);
}

/**
 * 是否有实际的事务对象
 * Return whether there is an actual transaction active.
 */
public boolean hasTransaction() {
    return (this.transaction != null);
}
```



## SuspendedResourcesHolder

AbstractPlatformTransactionManager的内部类SuspendedResourcesHolder

```java
/**
 * Holder for suspended resources.
 * Used internally by {@code suspend} and {@code resume}.
 */
protected static final class SuspendedResourcesHolder {

    @Nullable
    private final Object suspendedResources;

    @Nullable
    private List<TransactionSynchronization> suspendedSynchronizations;

    @Nullable
    private String name;

    private boolean readOnly;

    @Nullable
    private Integer isolationLevel;

    private boolean wasActive;

    private SuspendedResourcesHolder(Object suspendedResources) {
        this.suspendedResources = suspendedResources;
    }
   
    private SuspendedResourcesHolder(
            @Nullable Object suspendedResources, List<TransactionSynchronization> suspendedSynchronizations,
            @Nullable String name, boolean readOnly, @Nullable Integer isolationLevel, boolean wasActive) {
        // 挂起的资源。通常是数据库连接持有对象 
        this.suspendedResources = suspendedResources;
        // 挂起的事务同步器
        this.suspendedSynchronizations = suspendedSynchronizations;
        // 挂起事务的事务名称
        this.name = name;
        // 挂起的事务是否只读
        this.readOnly = readOnly;
         // 挂起事务的隔离级别
        this.isolationLevel = isolationLevel;
         // 挂起的事务是否活跃
        this.wasActive = wasActive;
    }
}
```





### 1、 挂起现有的事务对象

```java
/**
 * 挂起给定的事务。首先挂起事务同步，然后委托给doSuspend模板方法。
 * Suspend the given transaction. Suspends transaction synchronization first,
 * then delegates to the {@code doSuspend} template method.
 * 
 * @param transaction the current transaction object 
 * (or {@code null} to just suspend active synchronizations, if any)
 *
 * 返回保存挂起资源的对象(如果事务和同步都不活动，则为空)
 * @return an object that holds suspended resources
 * (or {@code null} if neither transaction nor synchronization active)
 * @see #doSuspend
 * @see #resume
 */
@Nullable
protected final SuspendedResourcesHolder suspend(@Nullable Object transaction) throws TransactionException { 
    // 如果有事务同步活跃。  
    if (TransactionSynchronizationManager.isSynchronizationActive()) {             
        // 1、挂起并清除事务同步器，并获取所有挂起的事务同步器
        List<TransactionSynchronization> suspendedSynchronizations = doSuspendSynchronization();
        try {
            // 挂起的资源suspendedResources通常是数据源连接持有对象
            Object suspendedResources = null;
            if (transaction != null) {
                // 事务对象不为null
                // 2、挂起事务对象
                suspendedResources = doSuspend(transaction);
            }
            // 获取当前事务的名称
            String name = TransactionSynchronizationManager.getCurrentTransactionName();
            // 设置当前线程的事务名称为null
            TransactionSynchronizationManager.setCurrentTransactionName(null);
            // 获取当前事务是否只读
            boolean readOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
            // 设置当前线程的事务不是只读
            TransactionSynchronizationManager.setCurrentTransactionReadOnly(false);
            // 获取当前事务的隔离级别
            Integer isolationLevel = TransactionSynchronizationManager.getCurrentTransactionIsolationLevel();
            // 设置当前线程的事务隔离级别为null
            TransactionSynchronizationManager.setCurrentTransactionIsolationLevel(null);
            // 获取当前线程是否有实际的事务活跃
            boolean wasActive = TransactionSynchronizationManager.isActualTransactionActive();
            // 设置当前线程没有实际的事务活跃
            TransactionSynchronizationManager.setActualTransactionActive(false);
            // 3、创建挂起资源持有对象
            return new SuspendedResourcesHolder(
                    suspendedResources, suspendedSynchronizations, name, readOnly, isolationLevel, wasActive);
        }
        catch (RuntimeException | Error ex) {            
            // doSuspend failed - original transaction is still active...
            // 通常是doSuspend执行失败。
            // 恢复事务同步
            doResumeSynchronization(suspendedSynchronizations);
            throw ex;
        }
    }
    else if (transaction != null) {
        // Transaction active but no synchronization active.
        // 如果没有事务同步活跃，只有事务对象，挂起事务对象
        // 2、挂起事务对象
        Object suspendedResources = doSuspend(transaction);
        return new SuspendedResourcesHolder(suspendedResources);
    }
    else {
        // Neither transaction nor synchronization active.
        // 没有事务对象和事务同步存在，直接返回null
        return null;
    }
}
```



####  1.1 挂起并清除事务同步器，并获取所有挂起的事务同步器

```java
/**
 * 返回挂起的TransactionSynchronization对象列表
 * Suspend all current synchronizations and deactivate transaction
 * synchronization for the current thread.
 * @return the List of suspended TransactionSynchronization objects
 */
private List<TransactionSynchronization> doSuspendSynchronization() {
    // 返回当前线程所有的事务同步
    List<TransactionSynchronization> suspendedSynchronizations =
            TransactionSynchronizationManager.getSynchronizations();
    // 遍历所有的事务同步器，执行suspend挂起方法
    for (TransactionSynchronization synchronization : suspendedSynchronizations) {
        // 通常会在该方法中释放数据库连接，解绑TransactionSynchronizationManager中相关资源。
        // 常用的事务同步器有DataSourceUtils的ConnectionSynchronization
        synchronization.suspend();
    }
    // 清除当前线程所有的事务同步。【注意】这里会清除
    TransactionSynchronizationManager.clearSynchronization();
    // 返回所有挂起的事务同步
    return suspendedSynchronizations;
}

/**
 * 返回当前线程所有已注册同步的不可修改快照列表。
 * Return an unmodifiable snapshot list of all registered synchronizations
 * for the current thread.
 * @return unmodifiable List of TransactionSynchronization instances
 * @throws IllegalStateException if synchronization is not active
 * @see TransactionSynchronization
 */
public static List<TransactionSynchronization> getSynchronizations() throws IllegalStateException {
    Set<TransactionSynchronization> synchs = synchronizations.get();
    if (synchs == null) {
        throw new IllegalStateException("Transaction synchronization is not active");
    }
    // Return unmodifiable snapshot, to avoid ConcurrentModificationExceptions
    // while iterating and invoking synchronization callbacks that in turn
    // might register further synchronizations.
    if (synchs.isEmpty()) {
        return Collections.emptyList();
    }
    else {
        // Sort lazily here, not in registerSynchronization.
        List<TransactionSynchronization> sortedSynchs = new ArrayList<>(synchs);
        OrderComparator.sort(sortedSynchs);
        return Collections.unmodifiableList(sortedSynchs);
    }
}

/**
 * 停用当前线程的事务同步。由事务管理器在事务清理时调用。
 * Deactivate transaction synchronization for the current thread.
 * Called by the transaction manager on transaction cleanup.
 * @throws IllegalStateException if synchronization is not active
 */
public static void clearSynchronization() throws IllegalStateException {
    if (!isSynchronizationActive()) {
        throw new IllegalStateException("Cannot deactivate transaction synchronization - not active");
    }
    synchronizations.remove();
}
```



#### 1.2 挂起事务对象

```java
@Override
protected Object doSuspend(Object transaction) {   
    // 挂起的事务对象存在，说明TransactionSynchronizationManager的resources有当前线程的事务资源map
    // 并且存放着数据源对象 --> 数据库连接持有对象的键值对。 可参考【1.3.4 未找到现有事务的处理总结】
    DataSourceTransactionObject txObject = (DataSourceTransactionObject) transaction;
    txObject.setConnectionHolder(null);
    // 移除事务资源map现有事务的数据库连接持有对象并返回
    return TransactionSynchronizationManager.unbindResource(obtainDataSource());
}

/**
 * Unbind a resource for the given key from the current thread.
 * @param key the key to unbind (usually the resource factory)
 * @return the previously bound value (usually the active resource object)
 * @throws IllegalStateException if there is no value bound to the thread
 * @see ResourceTransactionManager#getResourceFactory()
 */
public static Object unbindResource(Object key) throws IllegalStateException {
    Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
    Object value = doUnbindResource(actualKey);
    if (value == null) {
        throw new IllegalStateException("No value for key [" + actualKey + "] bound to thread");
    }
    return value;
}

/**
 * Actually remove the value of the resource that is bound for the given key.
 */
@Nullable
private static Object doUnbindResource(Object actualKey) {
    Map<Object, Object> map = resources.get();
    if (map == null) {
        return null;
    }
    Object value = map.remove(actualKey);
    // Remove entire ThreadLocal if empty...
    if (map.isEmpty()) {
        // 移除
        resources.remove();
    }
    // Transparently suppress a ResourceHolder that was marked as void...
    if (value instanceof ResourceHolder && ((ResourceHolder) value).isVoid()) {
        value = null;
    }
    // 返回
    return value;
}
```



#### 1.3 创建挂起资源持有对象

```java
见【关键类】SuspendedResourcesHolder
```



#### 1.4 挂起失败，恢复事务同步

```java
/**
 * Reactivate transaction synchronization for the current thread
 * and resume all given synchronizations.
 * @param suspendedSynchronizations a List of TransactionSynchronization objects
 */
private void doResumeSynchronization(List<TransactionSynchronization> suspendedSynchronizations) {
    // 初始化事务同步
    TransactionSynchronizationManager.initSynchronization();
    for (TransactionSynchronization synchronization : suspendedSynchronizations) {
        // 执行事务同步器的恢复方法
        synchronization.resume();
        // 注册事务同步器到当前线程
        TransactionSynchronizationManager.registerSynchronization(synchronization);
    }
}

/**
 * Activate transaction synchronization for the current thread.
 * Called by a transaction manager on transaction begin.
 * @throws IllegalStateException if synchronization is already active
 */
public static void initSynchronization() throws IllegalStateException {
    if (isSynchronizationActive()) {
        throw new IllegalStateException("Cannot activate transaction synchronization - already active");
    }
    // 初始化一个LinkedHashSet
    synchronizations.set(new LinkedHashSet<>());
}

/**
 * Register a new transaction synchronization for the current thread.
 * Typically called by resource management code.
 * <p>Note that synchronizations can implement the
 * {@link org.springframework.core.Ordered} interface.
 * They will be executed in an order according to their order value (if any).
 * @param synchronization the synchronization object to register
 * @throws IllegalStateException if transaction synchronization is not active
 * @see org.springframework.core.Ordered
 */
public static void registerSynchronization(TransactionSynchronization synchronization)
        throws IllegalStateException {

    Assert.notNull(synchronization, "TransactionSynchronization must not be null");
    // 获取当前线程的事务同步列表
    Set<TransactionSynchronization> synchs = synchronizations.get();
    if (synchs == null) {
        throw new IllegalStateException("Transaction synchronization is not active");
    }
    // 添加事务同步
    synchs.add(synchronization);
}

private static final ThreadLocal<Set<TransactionSynchronization>> synchronizations =
			new NamedThreadLocal<>("Transaction synchronizations");
```





## TransactionSynchronizationManager

介绍常用的方法

```java

/**
（1）管理每个线程的资源和事务同步的中心委托。由资源管理代码使用，而不是由典型的应用程序代码使用。
（2）支持每个键一个资源，不覆盖，即在为同一键设置新资源之前需要删除一个资源。如果同步是活动的，则支持事务同步列表。
（3）资源管理代码应该通过getResource检查线程绑定的资源，例如JDBC连接或Hibernate会话。
	这样的代码通常不应该将资源绑定到线程，因为这是事务管理器的责任。
	另一个选项是，如果事务同步是活动的，则在第一次使用时惰性绑定，用于执行跨越任意数量资源的事务。
（4）事务同步必须由事务管理器通过initSynchronization()和clearSynchronization()激活和取消激活。   AbstractPlatformTransactionManager自动支持此功能，因此所有标准的Spring事务管理器都支持此功能，
例如org.springframework.transaction.jta. jtattransactionmanager和org.springframework.jdbc.datasource.DataSourceTransactionManager。
（5）资源管理代码应该只在这个管理器处于活动状态时注册同步，这可以通过isSynchronizationActive来检查;
它应该立即执行资源清理。如果事务同步不是活动的，则要么没有当前事务，要么事务管理器不支持事务同步。
例如，同步用于在JTA事务中始终返回相同的资源，例如，对于任何给定的数据源或SessionFactory，分别使用JDBC连接或Hibernate会话。
 *
 *
 * Central delegate that manages resources and transaction synchronizations per thread.
 * To be used by resource management code but not by typical application code.
 *
 * <p>Supports one resource per key without overwriting, that is, a resource needs
 * to be removed before a new one can be set for the same key.
 * Supports a list of transaction synchronizations if synchronization is active.
 *
 * <p>Resource management code should check for thread-bound resources, e.g. JDBC
 * Connections or Hibernate Sessions, via {@code getResource}. Such code is
 * normally not supposed to bind resources to threads, as this is the responsibility
 * of transaction managers. A further option is to lazily bind on first use if
 * transaction synchronization is active, for performing transactions that span
 * an arbitrary number of resources.
 *
 * <p>Transaction synchronization must be activated and deactivated by a transaction
 * manager via {@link #initSynchronization()} and {@link #clearSynchronization()}.
 * This is automatically supported by {@link AbstractPlatformTransactionManager},
 * and thus by all standard Spring transaction managers, such as
 * {@link org.springframework.transaction.jta.JtaTransactionManager} and
 * {@link org.springframework.jdbc.datasource.DataSourceTransactionManager}.
 *
 * <p>Resource management code should only register synchronizations when this
 * manager is active, which can be checked via {@link #isSynchronizationActive};
 * it should perform immediate resource cleanup else. If transaction synchronization
 * isn't active, there is either no current transaction, or the transaction manager
 * doesn't support transaction synchronization.
 *
 * <p>Synchronization is for example used to always return the same resources
 * within a JTA transaction, e.g. a JDBC Connection or a Hibernate Session for
 * any given DataSource or SessionFactory, respectively.
 *
 * @author Juergen Hoeller
 * @since 02.06.2003
 * @see #isSynchronizationActive
 * @see #registerSynchronization
 * @see TransactionSynchronization
 * @see AbstractPlatformTransactionManager#setTransactionSynchronization
 * @see org.springframework.transaction.jta.JtaTransactionManager
 * @see org.springframework.jdbc.datasource.DataSourceTransactionManager
 * @see org.springframework.jdbc.datasource.DataSourceUtils#getConnection
 */
public abstract class TransactionSynchronizationManager {

	 // 存放与当前线程相关的事务资源，比如数据库连接
	private static final ThreadLocal<Map<Object, Object>> resources =
			new NamedThreadLocal<>("Transactional resources");
    
    // synchronizations是否有Set存在用于指示事务同步器是否激活，有就是激活，反之不是。
    // Set用于存储 TransactionSynchronization 对象，这些对象用于在事务生命周期的各个阶段注册回调
	private static final ThreadLocal<Set<TransactionSynchronization>> synchronizations =
			new NamedThreadLocal<>("Transaction synchronizations");
  	// 存储当前事务的名称
	private static final ThreadLocal<String> currentTransactionName =
			new NamedThreadLocal<>("Current transaction name");
     // 存储当前事务是否为只读事务
	private static final ThreadLocal<Boolean> currentTransactionReadOnly =
			new NamedThreadLocal<>("Current transaction read-only status");
      // 存储当前事务的隔离级别
	private static final ThreadLocal<Integer> currentTransactionIsolationLevel =
			new NamedThreadLocal<>("Current transaction isolation level");
	// 指示当前线程是否有事务活跃
	private static final ThreadLocal<Boolean> actualTransactionActive =
		new NamedThreadLocal<>("Actual transaction active");


	//-------------------------------------------------------------------------
	// Management of transaction-associated resource handles
	//-------------------------------------------------------------------------

	// ....

	/**
	 * 检索绑定到当前线程的给定键的资源。
	 * Retrieve a resource for the given key that is bound to the current thread.
	 * @param key the key to check (usually the resource factory)
	 * @return a value bound to the current thread (usually the active
	 * resource object), or {@code null} if none
	 * @see ResourceTransactionManager#getResourceFactory()
	 */
	@Nullable
	public static Object getResource(Object key) {
		Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
		return doGetResource(actualKey);
	}

	/**
     * 实际检查为给定键绑定的资源的值。
	 * Actually check the value of the resource that is bound for the given key.
	 */
	@Nullable
	private static Object doGetResource(Object actualKey) {
		Map<Object, Object> map = resources.get();
		if (map == null) {
			return null;
		}
		Object value = map.get(actualKey);
		// Transparently remove ResourceHolder that was marked as void...
		if (value instanceof ResourceHolder && ((ResourceHolder) value).isVoid()) {
			map.remove(actualKey);
			// Remove entire ThreadLocal if empty...
			if (map.isEmpty()) {
				resources.remove();
			}
			value = null;
		}
		return value;
	}

	/**
	 * 将给定键的给定资源绑定到当前线程。
	 * Bind the given resource for the given key to the current thread.
	 * @param key the key to bind the value to (usually the resource factory)
	 * @param value the value to bind (usually the active resource object)
	 * @throws IllegalStateException if there is already a value bound to the thread
	 * @see ResourceTransactionManager#getResourceFactory()
	 */
	public static void bindResource(Object key, Object value) throws IllegalStateException {
		Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
		Assert.notNull(value, "Value must not be null");
		Map<Object, Object> map = resources.get();
		// set ThreadLocal Map if none found
		if (map == null) {
			map = new HashMap<>();
			resources.set(map);
		}
		Object oldValue = map.put(actualKey, value);
		// Transparently suppress a ResourceHolder that was marked as void...
		if (oldValue instanceof ResourceHolder && ((ResourceHolder) oldValue).isVoid()) {
			oldValue = null;
		}
		if (oldValue != null) {
			throw new IllegalStateException(
					"Already value [" + oldValue + "] for key [" + actualKey + "] bound to thread");
		}
	}

	/**
     * 从当前线程解除对给定键的资源的绑定。
	 * Unbind a resource for the given key from the current thread.
	 * @param key the key to unbind (usually the resource factory)
	 * @return the previously bound value (usually the active resource object)
	 * @throws IllegalStateException if there is no value bound to the thread
	 * @see ResourceTransactionManager#getResourceFactory()
	 */
	public static Object unbindResource(Object key) throws IllegalStateException {
		Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
		Object value = doUnbindResource(actualKey);
		if (value == null) {
			throw new IllegalStateException("No value for key [" + actualKey + "] bound to thread");
		}
		return value;
	}

	/**
     * 从当前线程解除对给定键的资源的绑定。
     * 和上面方法的区别在于，这个不存在key对应的value不会抛出异常
	 * Unbind a resource for the given key from the current thread.
	 * @param key the key to unbind (usually the resource factory)
	 * @return the previously bound value, or {@code null} if none bound
	 */
	@Nullable
	public static Object unbindResourceIfPossible(Object key) {
		Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
		return doUnbindResource(actualKey);
	}

	/**
	 * 实际删除为给定键绑定的资源的值。
	 * Actually remove the value of the resource that is bound for the given key.
	 */
	@Nullable
	private static Object doUnbindResource(Object actualKey) {
		Map<Object, Object> map = resources.get();
		if (map == null) {
			return null;
		}
		Object value = map.remove(actualKey);
		// Remove entire ThreadLocal if empty...
		if (map.isEmpty()) {
			resources.remove();
		}
		// Transparently suppress a ResourceHolder that was marked as void...
		if (value instanceof ResourceHolder && ((ResourceHolder) value).isVoid()) {
			value = null;
		}
		return value;
	}


	//-------------------------------------------------------------------------
	// Management of transaction synchronizations
	//-------------------------------------------------------------------------
    // 以下是事务同步的管理
    
	/**
	 * 如果当前线程的事务同步是活动的，则返回。可以在注册之前调用，以避免不必要的实例创建。
	 * Return if transaction synchronization is active for the current thread.
	 * Can be called before register to avoid unnecessary instance creation.
	 * @see #registerSynchronization
	 */
	public static boolean isSynchronizationActive() {
		return (synchronizations.get() != null);
	}

	/**
	 * 激活当前线程的事务同步。由事务管理器在事务开始时调用。
	 * Activate transaction synchronization for the current thread.
	 * Called by a transaction manager on transaction begin.
	 * @throws IllegalStateException if synchronization is already active
	 */
	public static void initSynchronization() throws IllegalStateException {
		if (isSynchronizationActive()) {
			throw new IllegalStateException("Cannot activate transaction synchronization - already active");
		}
		synchronizations.set(new LinkedHashSet<>());
	}

	/**
	 * 为当前线程注册一个新的事务同步。通常由资源管理代码调用。注意，同步可以实现org.springframework.core.Ordered接口。
	 * 它们将根据getOrder方法返回的顺序(如果有的话)执行。
	 *
	 * Register a new transaction synchronization for the current thread.
	 * Typically called by resource management code.
	 * <p>Note that synchronizations can implement the
	 * {@link org.springframework.core.Ordered} interface.
	 * They will be executed in an order according to their order value (if any).
	 * @param synchronization the synchronization object to register
	 * @throws IllegalStateException if transaction synchronization is not active
	 * @see org.springframework.core.Ordered
	 */
	public static void registerSynchronization(TransactionSynchronization synchronization)
			throws IllegalStateException {

		Assert.notNull(synchronization, "TransactionSynchronization must not be null");
		Set<TransactionSynchronization> synchs = synchronizations.get();
		if (synchs == null) {
			throw new IllegalStateException("Transaction synchronization is not active");
		}
		synchs.add(synchronization);
	}

	/**
	 * 返回当前线程所有已注册同步的不可修改快照列表。
	 * Return an unmodifiable snapshot list of all registered synchronizations
	 * for the current thread.
	 * @return unmodifiable List of TransactionSynchronization instances
	 * @throws IllegalStateException if synchronization is not active
	 * @see TransactionSynchronization
	 */
	public static List<TransactionSynchronization> getSynchronizations() throws IllegalStateException {
		Set<TransactionSynchronization> synchs = synchronizations.get();
		if (synchs == null) {
			throw new IllegalStateException("Transaction synchronization is not active");
		}
		// Return unmodifiable snapshot, to avoid ConcurrentModificationExceptions
		// while iterating and invoking synchronization callbacks that in turn
		// might register further synchronizations.
		if (synchs.isEmpty()) {
			return Collections.emptyList();
		}
		else {
			// Sort lazily here, not in registerSynchronization.
			List<TransactionSynchronization> sortedSynchs = new ArrayList<>(synchs);
			OrderComparator.sort(sortedSynchs);
			return Collections.unmodifiableList(sortedSynchs);
		}
	}

	/**
	 * 停用当前线程的事务同步。由事务管理器在事务清理时调用。
	 * Deactivate transaction synchronization for the current thread.
	 * Called by the transaction manager on transaction cleanup.
	 * @throws IllegalStateException if synchronization is not active
	 */
	public static void clearSynchronization() throws IllegalStateException {
		if (!isSynchronizationActive()) {
			throw new IllegalStateException("Cannot deactivate transaction synchronization - not active");
		}
		synchronizations.remove();
	}


	//-------------------------------------------------------------------------
	// Exposure of transaction characteristics
	//-------------------------------------------------------------------------
    // 事务特性暴露

	/**
	 * 公开当前事务的名称(如果有)。
	 * Expose the name of the current transaction, if any.
	 * Called by the transaction manager on transaction begin and on cleanup.
	 * @param name the name of the transaction, or {@code null} to reset it
	 * @see org.springframework.transaction.TransactionDefinition#getName()
	 */
	public static void setCurrentTransactionName(@Nullable String name) {
		currentTransactionName.set(name);
	}

	/**
	 * 返回当前事务的名称，如果未设置则返回null。由资源管理代码调用，
	 * 以对每个用例进行优化，例如优化特定命名事务的获取策略。
	 * 
	 * Return the name of the current transaction, or {@code null} if none set.
	 * To be called by resource management code for optimizations per use case,
	 * for example to optimize fetch strategies for specific named transactions.
	 * @see org.springframework.transaction.TransactionDefinition#getName()
	 */
	@Nullable
	public static String getCurrentTransactionName() {
		return currentTransactionName.get();
	}

	/**
	 * 为当前事务公开一个只读标志。由事务管理器在事务开始和清理时调用。
	 * Expose a read-only flag for the current transaction.
	 * Called by the transaction manager on transaction begin and on cleanup.
	 * @param readOnly {@code true} to mark the current transaction
	 * as read-only; {@code false} to reset such a read-only marker
	 * @see org.springframework.transaction.TransactionDefinition#isReadOnly()
	 */
	public static void setCurrentTransactionReadOnly(boolean readOnly) {
		currentTransactionReadOnly.set(readOnly ? Boolean.TRUE : null);
	}

	/**
	*返回当前事务是否标记为只读。资源管理代码在准备一个新对象时调用创建资源(例如，Hibernate会话)。
	* <p>注意事务同步接收只读标志
	*作为{@code beforeCommit}回调的参数，以便能够
	*在提交时抑制变更检测。目前的方法是有意义的
	*将用于早期的只读检查，例如设置
	*将Hibernate会话刷新为“FlushMode”。手册》的前期。	
	 * Return whether the current transaction is marked as read-only.
	 * To be called by resource management code when preparing a newly
	 * created resource (for example, a Hibernate Session).
	 * <p>Note that transaction synchronizations receive the read-only flag
	 * as argument for the {@code beforeCommit} callback, to be able
	 * to suppress change detection on commit. The present method is meant
	 * to be used for earlier read-only checks, for example to set the
	 * flush mode of a Hibernate Session to "FlushMode.MANUAL" upfront.
	 * @see org.springframework.transaction.TransactionDefinition#isReadOnly()
	 * @see TransactionSynchronization#beforeCommit(boolean)
	 */
	public static boolean isCurrentTransactionReadOnly() {
		return (currentTransactionReadOnly.get() != null);
	}

	/**
	 * 公开当前事务的隔离级别。由事务管理器在事务开始和事务清理时调用。
	 * Expose an isolation level for the current transaction.
	 * Called by the transaction manager on transaction begin and on cleanup.
	 * @param isolationLevel the isolation level to expose, according to the
	 * JDBC Connection constants (equivalent to the corresponding Spring
	 * TransactionDefinition constants), or {@code null} to reset it
	 * @see java.sql.Connection#TRANSACTION_READ_UNCOMMITTED
	 * @see java.sql.Connection#TRANSACTION_READ_COMMITTED
	 * @see java.sql.Connection#TRANSACTION_REPEATABLE_READ
	 * @see java.sql.Connection#TRANSACTION_SERIALIZABLE
	 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_READ_UNCOMMITTED
	 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_READ_COMMITTED
	 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_REPEATABLE_READ
	 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_SERIALIZABLE
	 * @see org.springframework.transaction.TransactionDefinition#getIsolationLevel()
	 */
	public static void setCurrentTransactionIsolationLevel(@Nullable Integer isolationLevel) {
		currentTransactionIsolationLevel.set(isolationLevel);
	}

	/**
	 * 返回当前事务的隔离级别(如果有)。资源管理代码在准备一个新对象时调用
	 * 创建的资源(例如JDBC连接)。
	 * Return the isolation level for the current transaction, if any.
	 * To be called by resource management code when preparing a newly
	 * created resource (for example, a JDBC Connection).
	 * @return the currently exposed isolation level, according to the
	 * JDBC Connection constants (equivalent to the corresponding Spring
	 * TransactionDefinition constants), or {@code null} if none
	 * @see java.sql.Connection#TRANSACTION_READ_UNCOMMITTED
	 * @see java.sql.Connection#TRANSACTION_READ_COMMITTED
	 * @see java.sql.Connection#TRANSACTION_REPEATABLE_READ
	 * @see java.sql.Connection#TRANSACTION_SERIALIZABLE
	 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_READ_UNCOMMITTED
	 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_READ_COMMITTED
	 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_REPEATABLE_READ
	 * @see org.springframework.transaction.TransactionDefinition#ISOLATION_SERIALIZABLE
	 * @see org.springframework.transaction.TransactionDefinition#getIsolationLevel()
	 */
	@Nullable
	public static Integer getCurrentTransactionIsolationLevel() {
		return currentTransactionIsolationLevel.get();
	}

	/**
 	 * 公开当前是否有实际的活动事务。由事务管理器在事务开始和事务清理时调用。
	 * Expose whether there currently is an actual transaction active.
	 * Called by the transaction manager on transaction begin and on cleanup.
	 * @param active {@code true} to mark the current thread as being associated
	 * with an actual transaction; {@code false} to reset that marker
	 */
	public static void setActualTransactionActive(boolean active) {
		actualTransactionActive.set(active ? Boolean.TRUE : null);
	}

	/**
	 * 返回当前是否有实际的活动事务。
	 * 这表明当前线程是否与实际事务相关联，而不仅仅是与活动事务同步相关联。
	 * 由资源管理代码调用，以区分活动事务同步(有或没有后台资源事务;也在PROPAGATION_SUPPORTS上)，
	 *  并且实际事务处于活动状态(具有支持资源事务;PROPAGATION_REQUIRED, PROPAGATION_REQUIRES_NEW等)。
	 * 
	 * Return whether there currently is an actual transaction active.
	 * This indicates whether the current thread is associated with an actual
	 * transaction rather than just with active transaction synchronization.
	 * <p>To be called by resource management code that wants to discriminate
	 * between active transaction synchronization (with or without backing
	 * resource transaction; also on PROPAGATION_SUPPORTS) and an actual
	 * transaction being active (with backing resource transaction;
	 * on PROPAGATION_REQUIRED, PROPAGATION_REQUIRES_NEW, etc).
	 * @see #isSynchronizationActive()
	 */
	public static boolean isActualTransactionActive() {
		return (actualTransactionActive.get() != null);
	}


	/**
	 * 清除当前线程的整个事务同步状态: 注册的同步以及各种事务特征。
	 * Clear the entire transaction synchronization state for the current thread:
	 * registered synchronizations as well as the various transaction characteristics.
	 * @see #clearSynchronization()
	 * @see #setCurrentTransactionName
	 * @see #setCurrentTransactionReadOnly
	 * @see #setCurrentTransactionIsolationLevel
	 * @see #setActualTransactionActive
	 */
	public static void clear() {
		synchronizations.remove();
		currentTransactionName.remove();
		currentTransactionReadOnly.remove();
		currentTransactionIsolationLevel.remove();
		actualTransactionActive.remove();
	}

}

```



## JdbcTransactionManager

### 1、开启新事务并返回事务状态

位于JdbcTransactionManager继承的DataSourceTransactionManager类

```java
/**
 * Start a new transaction.
 */
private TransactionStatus startTransaction(TransactionDefinition definition, Object transaction,
        boolean debugEnabled, @Nullable SuspendedResourcesHolder suspendedResources) {
	// 是否是新的同步。默认为true
    boolean newSynchronization = (getTransactionSynchronization() != SYNCHRONIZATION_NEVER);
    // 创建事务状态【见关键类DefaultTransactionStatus】的1.1. 创建DefaultTransactionStatus
    DefaultTransactionStatus status = newTransactionStatus(
            definition, transaction, true, newSynchronization, debugEnabled, suspendedResources);
    // 开启事务
    doBegin(transaction, definition);
    // 准备事务同步
    prepareSynchronization(status, definition);
    // 返回事务状态
    return status;
}

// 创建的JdbcTransactionManager没有指定transactionSynchronization，默认是SYNCHRONIZATION_ALWAYS
/**
 * Return if this transaction manager should activate the thread-bound
 * transaction synchronization support.
 */
public final int getTransactionSynchronization() {
    return this.transactionSynchronization;
}
private int transactionSynchronization = SYNCHRONIZATION_ALWAYS;
```



```java
/**
 * 根据给定的事务定义开始一个具有语义的新事务。不必关心应用传播行为，因为这个抽象管理器已经处理了这个问题。
 * 当事务管理器决定实际启动新事务时，将调用此方法。要么之前没有任何交易，要么之前的交易被暂停了。
 * 一个特殊的场景是一个没有保存点的嵌套事务:
 *   如果useSavepointForNestedTransaction()返回“false”，这个方法将在必要时被调用来启动一个嵌套事务。
 *   在这样的上下文中，将存在活动事务:此方法的实现必须检测到此并启动适当的嵌套事务。
 * 
 * Begin a new transaction with semantics according to the given transaction
 * definition. Does not have to care about applying the propagation behavior,
 * as this has already been handled by this abstract manager.
 * <p>This method gets called when the transaction manager has decided to actually
 * start a new transaction. Either there wasn't any transaction before, or the
 * previous transaction has been suspended.
 * <p>A special scenario is a nested transaction without savepoint: If
 * {@code useSavepointForNestedTransaction()} returns "false", this method
 * will be called to start a nested transaction when necessary. In such a context,
 * there will be an active transaction: The implementation of this method has
 * to detect this and start an appropriate nested transaction.
 *
 * transaction：doGetTransaction方法返回的事务对象。通常是DataSourceTransactionObject
 * @param transaction the transaction object returned by {@code doGetTransaction}
 * 
 * definition：TransactionDefinition实例，描述传播行为、隔离级别、只读标志、超时和事务名称。
 * 通常是代理事务属性DelegatingTransactionAttribute（包含DefaultTransactionAttribute）的子类。
 * 实际使用的还是DefaultTransactionAttribute。（代理设计模式）
 *
 * @param definition a TransactionDefinition instance, describing propagation
 * behavior, isolation level, read-only flag, timeout, and transaction name
 *
 * @throws TransactionException in case of creation or system errors
 * @throws org.springframework.transaction.NestedTransactionNotSupportedException
 * if the underlying transaction does not support nesting
 */
@Override
protected void doBegin(Object transaction, TransactionDefinition definition) {
    DataSourceTransactionObject txObject = (DataSourceTransactionObject) transaction;
    Connection con = null;

    try {
        // （1）如果当前数据源事务对象没有数据库连接持有对象，则获取数据源，然后获取数据库连接        
        // （2）如果当前数据源事务对象有数据库连接持有对象，而且数据库连接持有对象没有事务同步。
        //      则获取数据源，然后获取数据库连接。
        // 一开始我们是没有数据库连接持有对象
        if (!txObject.hasConnectionHolder() ||
                txObject.getConnectionHolder().isSynchronizedWithTransaction()) {
            // 1.1 获取数据库连接
            // 先获取数据源，然后获取数据库连接。获取到的是DruidPooledConnection  
            Connection newCon = obtainDataSource().getConnection();
            if (logger.isDebugEnabled()) {
                logger.debug("Acquired Connection [" + newCon + "] for JDBC transaction");
            }
            // 创建数据库连接持有对象，保存数据库连接，并标记为新的数据连接
            txObject.setConnectionHolder(new ConnectionHolder(newCon), true);
        }
	    
        // 设置数据库连接持有对象有事务同步.
        txObject.getConnectionHolder().setSynchronizedWithTransaction(true);
        con = txObject.getConnectionHolder().getConnection();

        // 1.2 必要时设置成只读和修改隔离级别，并返回修改前的隔离级别 
        Integer previousIsolationLevel = DataSourceUtils.prepareConnectionForTransaction(con, definition);
        // 保存修改前的隔离级别
        txObject.setPreviousIsolationLevel(previousIsolationLevel);
        // 根据事务注解属性，设置是否只读
        txObject.setReadOnly(definition.isReadOnly());

         //如果需要，切换到手动提交。这在某些JDBC驱动程序中是非常昂贵的，
		//所以我们不想做不必要的事情(例如，如果我们显式地
		//配置连接池设置它)。
        // Switch to manual commit if necessary. This is very expensive in some JDBC drivers,
        // so we don't want to do it unnecessarily (for example if we've explicitly
        // configured the connection pool to set it already).
        // 我们获取到的是DruidPooledConnection，默认是自动提交的。
        // 1.3 设置数据库连接为非自动提交(autoCommit=false)
        if (con.getAutoCommit()) {            
            // 设置必须回复自动提交。【注意】这里在提交完事务后会用到
            txObject.setMustRestoreAutoCommit(true);
            if (logger.isDebugEnabled()) {
                logger.debug("Switching JDBC Connection [" + con + "] to manual commit");
            }
            // 修改自动提交为false，开启事务【注意】 
            con.setAutoCommit(false);
        }

        // 1.4 是否设置成只读事务（默认情况不需要）
        prepareTransactionalConnection(con, definition);
        // 设置数据库连接持有对象有事务活跃
        txObject.getConnectionHolder().setTransactionActive(true);

        // 1.5 设置具体的(非默认的)超时时间
        // 获取数据库连接超时时间。
        int timeout = determineTimeout(definition);
        if (timeout != TransactionDefinition.TIMEOUT_DEFAULT) {   
            // 如果不是默认的超时时间，则使用具体指定的超时时间
            txObject.getConnectionHolder().setTimeoutInSeconds(timeout);
        }

        // 1.6 绑定数据库连接持有对象到当前线程
        // Bind the connection holder to the thread.
        if (txObject.isNewConnectionHolder()) {
            TransactionSynchronizationManager.bindResource(obtainDataSource(), txObject.getConnectionHolder());
        }
    }

    catch (Throwable ex) {
        if (txObject.isNewConnectionHolder()) {
            DataSourceUtils.releaseConnection(con, obtainDataSource());
            txObject.setConnectionHolder(null, false);
        }
        throw new CannotCreateTransactionException("Could not open JDBC Connection for transaction", ex);
    }
}
```

 

```java
private ConnectionHandle connectionHandle;
public ConnectionHolder(Connection connection) {
    this.connectionHandle = new SimpleConnectionHandle(connection);
}

public class SimpleConnectionHandle implements ConnectionHandle {

	private final Connection connection;


	/**
	 * Create a new SimpleConnectionHandle for the given Connection.
	 * @param connection the JDBC Connection
	 */
	public SimpleConnectionHandle(Connection connection) {
		Assert.notNull(connection, "Connection must not be null");
		this.connection = connection;
	}

	/**
	 * Return the specified Connection as-is.
	 */
	@Override
	public Connection getConnection() {
		return this.connection;
	}


	@Override
	public String toString() {
		return "SimpleConnectionHandle: " + this.connection;
	}

}
```





#### 1.1 获取数据库连接

```java
/**
 * Obtain the DataSource for actual use.
 * @return the DataSource (never {@code null})
 * @throws IllegalStateException in case of no DataSource set
 * @since 5.0
 */
protected DataSource obtainDataSource() {
    // DruidDataSourceWrapper
    DataSource dataSource = getDataSource();
    Assert.state(dataSource != null, "No DataSource set");
    return dataSource;
}

// DruidDataSourceWrapper
@Override
public DruidPooledConnection getConnection() throws SQLException {
    return getConnection(maxWait);
}
```



#### 1.2 必要时设置成只读和修改隔离级别

必要时设置成只读和修改隔离级别，并返回修改前的隔离级别 

```java
/**
 * Prepare the given Connection with the given transaction semantics.
 * @param con the Connection to prepare
 * @param definition the transaction definition to apply
 * @return the previous isolation level, if any
 * @throws SQLException if thrown by JDBC methods
 * @see #resetConnectionAfterTransaction
 * @see Connection#setTransactionIsolation
 * @see Connection#setReadOnly
 */
@Nullable
public static Integer prepareConnectionForTransaction(Connection con, @Nullable TransactionDefinition definition)
        throws SQLException {

    Assert.notNull(con, "No Connection specified");

    boolean debugEnabled = logger.isDebugEnabled();
    // 如果指定了ReadOnly，则设置当前数据库连接为只读
    // Set read-only flag.
    if (definition != null && definition.isReadOnly()) {
        try {
            if (debugEnabled) {
                logger.debug("Setting JDBC Connection [" + con + "] read-only");
            }            
            con.setReadOnly(true);
        }
        catch (SQLException | RuntimeException ex) {
            Throwable exToCheck = ex;
            while (exToCheck != null) {
                if (exToCheck.getClass().getSimpleName().contains("Timeout")) {
                    // Assume it's a connection timeout that would otherwise get lost: e.g. from JDBC 4.0
                    throw ex;
                }
                exToCheck = exToCheck.getCause();
            }
            // "read-only not supported" SQLException -> ignore, it's just a hint anyway
            logger.debug("Could not set JDBC Connection read-only", ex);
        }
    }
	
    // 如果有具体的隔离级别，则应用
    // Apply specific isolation level, if any.    
    // 如果事务注解属性没有使用默认的事务隔离级别，而是使用具体的隔离级别，则获取数据库连接的隔离级别。
    // （1）如果数据库连接的隔离级别不等于事务注解属性指定具体的隔离级别，
    //      修改数据库连接的隔离级别为“事务注解属性指定具体的隔离级别”，返回数据库连接先前旧的隔离级别。
    //  (2) 如果相等，直接返回数据库连接的隔离级别
    // 如果没有具体的隔离级别，则返回null
    Integer previousIsolationLevel = null;
    if (definition != null && definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT) {
        if (debugEnabled) {
            logger.debug("Changing isolation level of JDBC Connection [" + con + "] to " +
                    definition.getIsolationLevel());
        }
        int currentIsolation = con.getTransactionIsolation();
        if (currentIsolation != definition.getIsolationLevel()) {
            previousIsolationLevel = currentIsolation;
            con.setTransactionIsolation(definition.getIsolationLevel());
        }
    }

    return previousIsolationLevel;
}
```



#### 1.3 设置数据库连接为非自动提交(autoCommit=false)

如果数据库连接是自动提交，则设置为非自动提交，开启事务。

```java
if (con.getAutoCommit()) {            
    // 设置必须回复自动提交。【注意】这里在提交完事务后会用到
    txObject.setMustRestoreAutoCommit(true);
    if (logger.isDebugEnabled()) {
        logger.debug("Switching JDBC Connection [" + con + "] to manual commit");
    }
    // 修改自动提交为false，开启事务【注意】 
    con.setAutoCommit(false);
}
```



#### 1.4 是否设置成只读事务（默认情况不需要）

```java
protected void prepareTransactionalConnection(Connection con, TransactionDefinition definition)
      throws SQLException {
   // 如果设置了强制只读而且事务注解属性设置了只读，则修改当前事务为只读事务。默认不是只读事务
   if (isEnforceReadOnly() && definition.isReadOnly()) {
      try (Statement stmt = con.createStatement()) {
         stmt.executeUpdate("SET TRANSACTION READ ONLY");
      }
   }
}

/**
 * 默认为false
 * Return whether to enforce the read-only nature of a transaction
 * through an explicit statement on the transactional connection.
 * @since 4.3.7
 * @see #setEnforceReadOnly
 */
public boolean isEnforceReadOnly() {
    return this.enforceReadOnly;
}
private boolean enforceReadOnly = false;
```



#### 1.5 设置具体的(非默认的)超时时间

```java
/**
 * Determine the actual timeout to use for the given definition.
 * Will fall back to this manager's default timeout if the
 * transaction definition doesn't specify a non-default value.
 * @param definition the transaction definition
 * @return the actual timeout to use
 * @see org.springframework.transaction.TransactionDefinition#getTimeout()
 * @see #setDefaultTimeout
 */
protected int determineTimeout(TransactionDefinition definition) {
    if (definition.getTimeout() != TransactionDefinition.TIMEOUT_DEFAULT) {
        // 不是默认的超时时间，则返回设置的超时时间
        return definition.getTimeout();
    }
    // 返回默认的超时时间
    return getDefaultTimeout();
}
/**
 * Return the default timeout that this transaction manager should apply
 * if there is no timeout specified at the transaction level, in seconds.
 * <p>Returns {@code TransactionDefinition.TIMEOUT_DEFAULT} to indicate
 * the underlying transaction infrastructure's default timeout.
 */
public final int getDefaultTimeout() {
    return this.defaultTimeout;
}
private int defaultTimeout = TransactionDefinition.TIMEOUT_DEFAULT;
```

```java
int timeout = determineTimeout(definition);
if (timeout != TransactionDefinition.TIMEOUT_DEFAULT) {   
    // 如果不是默认的超时时间，则使用具体指定的超时时间
    txObject.getConnectionHolder().setTimeoutInSeconds(timeout);
}
```

#### 1.6 绑定数据库连接持有对象到当前线程

```java
/**
 * Bind the given resource for the given key to the current thread.
 * @param key the key to bind the value to (usually the resource factory)
 * @param value the value to bind (usually the active resource object)
 * @throws IllegalStateException if there is already a value bound to the thread
 * @see ResourceTransactionManager#getResourceFactory()
 */
public static void bindResource(Object key, Object value) throws IllegalStateException {
   // 获取实际的key。（实际获取到的还是数据源DruidDataSourceWrapper对象）
   Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
   Assert.notNull(value, "Value must not be null");
   // 获取绑定的资源map。一开始是没有的，获取到null
   Map<Object, Object> map = resources.get();
   // set ThreadLocal Map if none found
   if (map == null) {
      // 新建资源map。保存到当前线程中。 
      map = new HashMap<>();
      resources.set(map);
   }
   //  添加DruidDataSourceWrapper和数据库连接持有对象到资源map中。
   Object oldValue = map.put(actualKey, value);
   // 显然地抑制一个标记为void的ResourceHolder…
   // Transparently suppress a ResourceHolder that was marked as void...
   if (oldValue instanceof ResourceHolder && ((ResourceHolder) oldValue).isVoid()) {
      oldValue = null;
   }
   if (oldValue != null) {
      throw new IllegalStateException(
            "Already value [" + oldValue + "] for key [" + actualKey + "] bound to thread");
   }
}


/**
 * Unwrap the given resource handle if necessary; otherwise return
 * the given handle as-is.
 * @since 5.3.4
 * @see InfrastructureProxy#getWrappedObject()
 */
public static Object unwrapResourceIfNecessary(Object resource) {
    // 这里resource = DruidDataSourceWrapper。不属于InfrastructureProxy。直接返回
    Assert.notNull(resource, "Resource must not be null");
    Object resourceRef = resource;
    // unwrap infrastructure proxy
    if (resourceRef instanceof InfrastructureProxy) {
        resourceRef = ((InfrastructureProxy) resourceRef).getWrappedObject();
    }
    if (aopAvailable) {
        // now unwrap scoped proxy
        resourceRef = ScopedProxyUnwrapper.unwrapIfNecessary(resourceRef);
    }
    return resourceRef;
}
```

【注意】此时ThreadLocal<Map<Object, Object>> resources已经存有资源map，**资源map有数据库源和数据库连接持有对象的键值对**。



#### 1.7 总结

```java
获取数据库连接，设置autoCommit=false，开启事务，保存数据库连接到当前线程（ThreadLocal）。
如果@Transactional注解指定了以下内容：
（1）是否只读
（2）隔离级别
（3）连接超时时间
没有采用默认的内容，则数据库连接相应地设置是否只读，修改隔离级别，设置连接超时时间。
```



## TransactionInfo

事务信息类，是TransactionAspectSupport的内部类

```java
/**
 * 用于保存事务信息的不透明对象。子类必须将其传递回该类的方法，但不能看到其内部结构。
 * Opaque object used to hold transaction information. Subclasses
 * must pass it back to methods on this class, but not see its internals.
 */
protected static final class TransactionInfo {
	
    // 事务管理器
    @Nullable
    private final PlatformTransactionManager transactionManager;

    // 事务注解属性
    @Nullable
    private final TransactionAttribute transactionAttribute;

    // 连接点标识符（通常是类名 + 方法）
    private final String joinpointIdentification;

    // 事务状态
    @Nullable
    private TransactionStatus transactionStatus;

    // 旧的事务信息（或者说是上一个事务信息）
    @Nullable
    private TransactionInfo oldTransactionInfo;

    // 构建TransactionInfo
    public TransactionInfo(@Nullable PlatformTransactionManager transactionManager,
            @Nullable TransactionAttribute transactionAttribute, String joinpointIdentification) {

        this.transactionManager = transactionManager;
        this.transactionAttribute = transactionAttribute;
        this.joinpointIdentification = joinpointIdentification;
    }

    public PlatformTransactionManager getTransactionManager() {
        Assert.state(this.transactionManager != null, "No PlatformTransactionManager set");
        return this.transactionManager;
    }

    @Nullable
    public TransactionAttribute getTransactionAttribute() {
        return this.transactionAttribute;
    }

    /**
     * Return a String representation of this joinpoint (usually a Method call)
     * for use in logging.
     */
    public String getJoinpointIdentification() {
        return this.joinpointIdentification;
    }

    public void newTransactionStatus(@Nullable TransactionStatus status) {
        this.transactionStatus = status;
    }

    @Nullable
    public TransactionStatus getTransactionStatus() {
        return this.transactionStatus;
    }

    /**
     * 返回事务是否由这个切面创建，或者我们是否只是有一个占位符来保持ThreadLocal堆栈的完整性。
     * Return whether a transaction was created by this aspect,
     * or whether we just have a placeholder to keep ThreadLocal stack integrity.
     */
    public boolean hasTransaction() {       
        return (this.transactionStatus != null);
    }

    // 1、绑定事务信息到线程（事务信息入栈）
    private void bindToThread() {
        // 暴露当前的TransactionStatus，保留任何现有的TransactionStatus用于此事务完成后的恢复。
        // Expose current TransactionStatus, preserving any existing TransactionStatus
        // for restoration after this transaction is complete.        
        // 设置旧的事务信息
        this.oldTransactionInfo = transactionInfoHolder.get();
        // 保存当前事务信息到当前线程的transactionInfoHolder
        transactionInfoHolder.set(this);
    }
   
    // 2、恢复旧的事务信息（事务信息出栈）
    private void restoreThreadLocalStatus() {
        // 使用stack来恢复旧的事务信息。如果没有设置，将为空。
        // Use stack to restore old transaction TransactionInfo.
        // Will be null if none was set.
        transactionInfoHolder.set(this.oldTransactionInfo);
    }

    @Override
    public String toString() {
        return (this.transactionAttribute != null ? this.transactionAttribute.toString() : "No transaction");
    }
}


// TransactionAspectSupport里的属性
/**
 * Holder支持currentTransactionStatus()方法，
 * 如果通知涉及多个方法(就像周围通知的情况一样)，支持不同协作通知之间的通信(例如，before通知和after通知)。
 *
 * Holder to support the {@code currentTransactionStatus()} method,
 * and to support communication between different cooperating advices
 * (e.g. before and after advice) if the aspect involves more than a
 * single method (as will be the case for around advice).
 */
private static final ThreadLocal<TransactionInfo> transactionInfoHolder =
        new NamedThreadLocal<>("Current aspect-driven transaction");
```

### 1、 绑定事务信息到线程（事务信息入栈）

见TransactionInfo的bindToThread()方法

### 2、恢复旧的事务信息 （事务信息出栈）

见TransactionInfo的restoreThreadLocalStatus()方法



## DataSourceUtils

### 一、获取数据库连接（必要时注册到事务同步管理器）

```java
/**
 * Helper class that provides static methods for obtaining JDBC Connections from
 * a {@link javax.sql.DataSource}. Includes special support for Spring-managed
 * transactional Connections, e.g. managed by {@link DataSourceTransactionManager}
 * or {@link org.springframework.transaction.jta.JtaTransactionManager}.
 *
 * <p>Used internally by Spring's {@link org.springframework.jdbc.core.JdbcTemplate},
 * Spring's JDBC operation objects and the JDBC {@link DataSourceTransactionManager}.
 * Can also be used directly in application code.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #getConnection
 * @see #releaseConnection
 * @see DataSourceTransactionManager
 * @see org.springframework.transaction.jta.JtaTransactionManager
 * @see org.springframework.transaction.support.TransactionSynchronizationManager
 */
public abstract class DataSourceUtils {

	/**
	 * Order value for TransactionSynchronization objects that clean up JDBC Connections.
	 */
	public static final int CONNECTION_SYNCHRONIZATION_ORDER = 1000;

	private static final Log logger = LogFactory.getLog(DataSourceUtils.class);

	/**
	 * Obtain a Connection from the given DataSource. Translates SQLExceptions into
	 * the Spring hierarchy of unchecked generic data access exceptions, simplifying
	 * calling code and making any exception that is thrown more meaningful.
	 * <p>Is aware of a corresponding Connection bound to the current thread, for example
	 * when using {@link DataSourceTransactionManager}. Will bind a Connection to the
	 * thread if transaction synchronization is active, e.g. when running within a
	 * {@link org.springframework.transaction.jta.JtaTransactionManager JTA} transaction).
	 * @param dataSource the DataSource to obtain Connections from
	 * @return a JDBC Connection from the given DataSource
	 * @throws org.springframework.jdbc.CannotGetJdbcConnectionException
	 * if the attempt to get a Connection failed
	 * @see #releaseConnection(Connection, DataSource)
	 * @see #isConnectionTransactional(Connection, DataSource)
	 */
	public static Connection getConnection(DataSource dataSource) throws CannotGetJdbcConnectionException {
		try {
			return doGetConnection(dataSource);
		}
		catch (SQLException ex) {
			throw new CannotGetJdbcConnectionException("Failed to obtain JDBC Connection", ex);
		}
		catch (IllegalStateException ex) {
			throw new CannotGetJdbcConnectionException("Failed to obtain JDBC Connection: " + ex.getMessage());
		}
	}

	/**
	 * Actually obtain a JDBC Connection from the given DataSource.
	 * Same as {@link #getConnection}, but throwing the original SQLException.
	 * <p>Is aware of a corresponding Connection bound to the current thread, for example
	 * when using {@link DataSourceTransactionManager}. Will bind a Connection to the thread
	 * if transaction synchronization is active (e.g. if in a JTA transaction).
	 * <p>Directly accessed by {@link TransactionAwareDataSourceProxy}.
	 * @param dataSource the DataSource to obtain Connections from
	 * @return a JDBC Connection from the given DataSource
	 * @throws SQLException if thrown by JDBC methods
	 * @see #doReleaseConnection
	 */
	public static Connection doGetConnection(DataSource dataSource) throws SQLException {
		Assert.notNull(dataSource, "No DataSource specified");
		// 根据数据源，从事务同步管理器获取数据库连接持有对象
		ConnectionHolder conHolder = (ConnectionHolder) TransactionSynchronizationManager.getResource(dataSource);
         // 数据库连接持有对象不为空，而且有数据库连接或者与事务同步（开启事务才会设置与事务同步）。
		if (conHolder != null && (conHolder.hasConnection() || conHolder.isSynchronizedWithTransaction())) {
			conHolder.requested();
			if (!conHolder.hasConnection()) {
                  // 没有数据库连接，则拉取数据库连接。一般来说这里不会执行
				logger.debug("Fetching resumed JDBC Connection from DataSource");
				conHolder.setConnection(fetchConnection(dataSource));
			}
             // 返回持有对象的数据库连接
			return conHolder.getConnection();
		}
		// Else we either got no holder or an empty thread-bound holder here.
        // 执行到这里。要么没有数据库连接持有对象，要么当前线程没有绑定数据库连接持有对象

		logger.debug("Fetching JDBC Connection from DataSource");
         // 拉取数据库连接
		Connection con = fetchConnection(dataSource);
		
         // 是否有事务同步活跃。使用@Transactional注解才有
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			try {
                  //在事务中使用相同的Connection进行进一步的JDBC操作。
				//线程绑定对象将在事务完成时被同步删除。调用事务同步器的afterCompletion方法
				// Use same Connection for further JDBC actions within the transaction.
				// Thread-bound object will get removed by synchronization at transaction completion.
				ConnectionHolder holderToUse = conHolder;
				if (holderToUse == null) {
                      // 数据数据库连接持有对象为空
                      // 创建新的数据连接持有对象，并保存数据库连接
					holderToUse = new ConnectionHolder(con);
				}
				else {
                      // 数据数据库连接持有对象不为空，则设置数据库连接
					holderToUse.setConnection(con);
				}
				holderToUse.requested();
                  // 注册ConnectionSynchronization事务同步器。管理数据库连接
				TransactionSynchronizationManager.registerSynchronization(
						new ConnectionSynchronization(holderToUse, dataSource));
                  // 设置与事务同步
				holderToUse.setSynchronizedWithTransaction(true);
				if (holderToUse != conHolder) {
                      // 执行到这里，说明数据库连接持有对象不是新建的
                      // 绑定数据库源和数据库连接持有对象到当前线程
					TransactionSynchronizationManager.bindResource(dataSource, holderToUse);
				}
			}
			catch (RuntimeException ex) {
				// Unexpected exception from external delegation call -> close Connection and rethrow.
				releaseConnection(con, dataSource);
				throw ex;
			}
		}

		return con;
	}

	/**
	 * Actually fetch a {@link Connection} from the given {@link DataSource},
	 * defensively turning an unexpected {@code null} return value from
	 * {@link DataSource#getConnection()} into an {@link IllegalStateException}.
	 * @param dataSource the DataSource to obtain Connections from
	 * @return a JDBC Connection from the given DataSource (never {@code null})
	 * @throws SQLException if thrown by JDBC methods
	 * @throws IllegalStateException if the DataSource returned a null value
	 * @see DataSource#getConnection()
	 */
	private static Connection fetchConnection(DataSource dataSource) throws SQLException {
         // 通过数据源获取数据库连接 
		Connection con = dataSource.getConnection();
		if (con == null) {
			throw new IllegalStateException("DataSource returned null from getConnection(): " + dataSource);
		}
		return con;
	}
    
    // .....
}
```

### 二、释放数据库连接

```java
/**
 * 如果没有外部管理(即没有绑定到线程)，则关闭从给定数据源获得的给定连接。
 * Close the given Connection, obtained from the given DataSource,
 * if it is not managed externally (that is, not bound to the thread).
 * @param con the Connection to close if necessary
 * (if this is {@code null}, the call will be ignored)
 * @param dataSource the DataSource that the Connection was obtained from
 * (may be {@code null})
 * @see #getConnection
 */
public static void releaseConnection(@Nullable Connection con, @Nullable DataSource dataSource) {
    try {
        doReleaseConnection(con, dataSource);
    }
    catch (SQLException ex) {
        logger.debug("Could not close JDBC Connection", ex);
    }
    catch (Throwable ex) {
        logger.debug("Unexpected exception on closing JDBC Connection", ex);
    }
}

/**
 * 实际上关闭从给定数据源获得的给定连接。与releaseConnection相同，但抛出原始的SQLException。
 * Actually close the given Connection, obtained from the given DataSource.
 * Same as {@link #releaseConnection}, but throwing the original SQLException.
 * <p>Directly accessed by {@link TransactionAwareDataSourceProxy}.
 * @param con the Connection to close if necessary
 * (if this is {@code null}, the call will be ignored)
 * @param dataSource the DataSource that the Connection was obtained from
 * (may be {@code null})
 * @throws SQLException if thrown by JDBC methods
 * @see #doGetConnection
 */
public static void doReleaseConnection(@Nullable Connection con, @Nullable DataSource dataSource) 
    throws SQLException {    
    if (con == null) {
        return;
    }
    // 如果数据源不为null
    if (dataSource != null) {
        // 从当前线程获取数据库连接持有对象
        ConnectionHolder conHolder = (ConnectionHolder) TransactionSynchronizationManager.getResource(dataSource);
        // 如果数据库连接持有对象不为null，而且持有的数据库连接和需要关闭的数据库连接一样
        if (conHolder != null && connectionEquals(conHolder, con)) {
            // 说明是事务管理的数据库连接，不关闭
            // It's the transactional Connection: Don't close it.
            // 引用计数减一
            conHolder.released();
            return;
        }
    }
    // 关闭数据库连接
    doCloseConnection(con, dataSource);
}

/**
 * 判断数据库连接持有对象的数据库连接和传递过来的数据库连接是否相当
 * Determine whether the given two Connections are equal, asking the target
 * Connection in case of a proxy. Used to detect equality even if the
 * user passed in a raw target Connection while the held one is a proxy.
 * @param conHolder the ConnectionHolder for the held Connection (potentially a proxy)
 * @param passedInCon the Connection passed-in by the user
 * (potentially a target Connection without proxy)
 * @return whether the given Connections are equal
 * @see #getTargetConnection
 */
private static boolean connectionEquals(ConnectionHolder conHolder, Connection passedInCon) {
    if (!conHolder.hasConnection()) {
        return false;
    }
    Connection heldCon = conHolder.getConnection();
    // Explicitly check for identity too: for Connection handles that do not implement
    // "equals" properly, such as the ones Commons DBCP exposes).
    return (heldCon == passedInCon || heldCon.equals(passedInCon) ||
            getTargetConnection(heldCon).equals(passedInCon));
}

/**
 * Releases the current Connection held by this ConnectionHolder.
 * <p>This is necessary for ConnectionHandles that expect "Connection borrowing",
 * where each returned Connection is only temporarily leased and needs to be
 * returned once the data operation is done, to make the Connection available
 * for other operations within the same transaction.
 */
@Override
public void released() {
    super.released();
    // 如果没有被引用了，而且当前连接不为空
    if (!isOpen() && this.currentConnection != null) {
        // SimpleConnectionHandle
        if (this.connectionHandle != null) {
            // 这里是空实现
            this.connectionHandle.releaseConnection(this.currentConnection);
        }
        // 设置数据库连接持有对象的当前连接为空
        this.currentConnection = null;
    }
}
// super.released();
/**
 * Decrease the reference count by one because the holder has been released
 * (i.e. someone released the resource held by it).
 */
public void released() {
    // 引用计数减一
    this.referenceCount--;
}
/**
 * Return whether there are still open references to this holder.
 */
public boolean isOpen() {    
    // 没有引用
    return (this.referenceCount > 0);
}

/**
 * Close the Connection, unless a {@link SmartDataSource} doesn't want us to.
 * @param con the Connection to close if necessary
 * @param dataSource the DataSource that the Connection was obtained from
 * @throws SQLException if thrown by JDBC methods
 * @see Connection#close()
 * @see SmartDataSource#shouldClose(Connection)
 */
public static void doCloseConnection(Connection con, @Nullable DataSource dataSource) throws SQLException {
    // 不是SmartDataSource类型的，或者是SmartDataSource类型，但是应该关闭的
    if (!(dataSource instanceof SmartDataSource) || ((SmartDataSource) dataSource).shouldClose(con)) {
        // 关闭数据库连接
        con.close();
    }
}
```

* 总结

如果不是事务管理的数据库连接，则直接关闭。如果是，则数据库连接持有对象引用计数减一

### 三、数据库连接是否是事务的

```java
/**
 * 确定给定的JDBC连接是否是事务性的，即:通过Spring的事务工具绑定到当前线程。
 * Determine whether the given JDBC Connection is transactional, that is,
 * bound to the current thread by Spring's transaction facilities.
 * @param con the Connection to check
 * @param dataSource the DataSource that the Connection was obtained from
 * (may be {@code null})
 * @return whether the Connection is transactional
 * @see #getConnection(DataSource)
 */
public static boolean isConnectionTransactional(Connection con, @Nullable DataSource dataSource) {
    // 数据源为空，返回false
    if (dataSource == null) {
        return false;
    } 
    // 从事务同步器中获取ConnectionHolder
    ConnectionHolder conHolder = (ConnectionHolder) TransactionSynchronizationManager.getResource(dataSource);
    // 当前线程存在dataSource对应的ConnectionHolder，而且是同一个数据库连接，说明是事务的。
    return (conHolder != null && connectionEquals(conHolder, con));
}
/**
 * Determine whether the given two Connections are equal, asking the target
 * Connection in case of a proxy. Used to detect equality even if the
 * user passed in a raw target Connection while the held one is a proxy.
 * @param conHolder the ConnectionHolder for the held Connection (potentially a proxy)
 * @param passedInCon the Connection passed-in by the user
 * (potentially a target Connection without proxy)
 * @return whether the given Connections are equal
 * @see #getTargetConnection
 */
private static boolean connectionEquals(ConnectionHolder conHolder, Connection passedInCon) {
    // ConnectionHolder没有数据库连接，返回false
    if (!conHolder.hasConnection()) {
        return false;
    }
    // 获取持有的数据库连接
    Connection heldCon = conHolder.getConnection();
    // Explicitly check for identity too: for Connection handles that do not implement
    // "equals" properly, such as the ones Commons DBCP exposes).
    // 判断数据库连接是否相等
    return (heldCon == passedInCon || heldCon.equals(passedInCon) ||
            getTargetConnection(heldCon).equals(passedInCon));
}
/**
 * Return the innermost target Connection of the given Connection. If the given
 * Connection is a proxy, it will be unwrapped until a non-proxy Connection is
 * found. Otherwise, the passed-in Connection will be returned as-is.
 * @param con the Connection proxy to unwrap
 * @return the innermost target Connection, or the passed-in one if no proxy
 * @see ConnectionProxy#getTargetConnection()
 */
public static Connection getTargetConnection(Connection con) {
    Connection conToUse = con;
    // 如果数据库连接属于ConnectionProxy类型，则获取目标数据库连接（被代理的）
    while (conToUse instanceof ConnectionProxy) {
        conToUse = ((ConnectionProxy) conToUse).getTargetConnection();
    }
    return conToUse;
}
```

# 调用示例分析

* PersonService

```java
public interface PersonService extends IService<Person> {
	void testRequired() throws NotRunTimeException;

    void testRequiredNew() throws NotRunTimeException;

    void testSupported() throws NotRunTimeException;

    void testMandatory() throws NotRunTimeException;

    void testNotSupported() throws NotRunTimeException;

    void testNever() throws NotRunTimeException;

    void testNested() throws NotRunTimeException;

    int addDemoPerson();
}
```

```java
@Service
@Slf4j
public class PersonServiceImpl extends ServiceImpl<PersonMapper, Person> implements PersonService {
	@Autowired
    private AnimalService animalService;
    
     /**===================================以下调试事务传播行为=============================================*/
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void testRequired() throws NotRunTimeException {
        addDemoPerson();
        animalService.testRequired();
        throw new ServiceException("save error");
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
}
```

* AnimalService

```java
public interface AnimalService extends IService<Animal> {
    void testRequired() throws NotRunTimeException;

    void testRequiredNew() throws NotRunTimeException;

    void testSupported() throws NotRunTimeException;

    void testMandatory() throws NotRunTimeException;

    void testNotSupported() throws NotRunTimeException;

    void testNever() throws NotRunTimeException;

    void testNested() throws NotRunTimeException;

}
```

```java
@Service
@Slf4j
public class AnimalServiceImpl extends ServiceImpl<AnimalMapper, Animal> implements AnimalService {
    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void testRequired() throws NotRunTimeException {
        saveDemoAnimal();
    }


    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testRequiredNew() throws NotRunTimeException {
        saveDemoAnimal();
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS, rollbackFor = Exception.class)
    public void testSupported() throws NotRunTimeException {
        saveDemoAnimal();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void testMandatory() throws NotRunTimeException {
        saveDemoAnimal();
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED, rollbackFor = Exception.class)
    public void testNotSupported() throws NotRunTimeException {
        saveDemoAnimal();
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public void testNever() throws NotRunTimeException {
        saveDemoAnimal();
    }

    @Override
    @Transactional(propagation = Propagation.NESTED, rollbackFor = Exception.class)
    public void testNested() throws NotRunTimeException {
        saveDemoAnimal();
    }

    private void saveDemoAnimal() throws NotRunTimeException {
        String name = UUID.randomUUID().toString().substring(0, 5);
        Integer age = new Random().nextInt();
        Animal p2 = new Animal().setName(name).setAge(age);
        save(p2);
        log.info("animal={}", p2);
        // 编译时异常
        //throw new NotRunTimeException("saveDemoAnimal error");
        // 运行时异常
        //throw new ServiceException("saveDemoAnimal error");
    }   
}
```

* 测试分析

```java
@SpringBootTest
public class TransactionTest {

    @Autowired
    private PersonService personService;  

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

}
```



# 扩展

## 一、Mybatis如何和Spring事务共用数据库连接

```
1、Mapper --> MapperFactoryBean---> 生成基于Mapper接口的代理类MybatisMapperProxy （里面含有SqlSessionTemplate）
2、SqlSessionTemplate实现SqlSession接口，含有SqlSession的代理类sqlSessionProxy（具体为SqlSessionInterceptor）
3、调用sql方法
SqlSessionTemplate调用sql方法 --> sqlSessionProxy调用sql方法  --> 执行代理逻辑，创建DefaultSqlSession实例 -->
通过DefaultSqlSession调用具体的sql方法
4、通过DefaultSqlSession调用具体的sql方法 --> Executor创建StatementHandler
  --> Executor使用事务对象SpringManagedTransaction获取数据库连接 
  --> 事务对象使用DataSourceUtils结合事务同步器TransactionSynchronizationManager获取数据库连接
  --> StatementHandler结合数据库连接获取Statement --> Statement执行sql  
```

### 1、SqlSessionInterceptor

```java
 private class SqlSessionInterceptor implements InvocationHandler {
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      // 1、创建DefaultSqlSession
      SqlSession sqlSession = getSqlSession(SqlSessionTemplate.this.sqlSessionFactory,
          SqlSessionTemplate.this.executorType, SqlSessionTemplate.this.exceptionTranslator);
      try {
        // 2、调用sql方法
        Object result = method.invoke(sqlSession, args);
        // 3、SqlSession不是事务的，则强制提交事务（通常使用@Transactional注解的话，都是事务的）
        if (!isSqlSessionTransactional(sqlSession, SqlSessionTemplate.this.sqlSessionFactory)) {
          // force commit even on non-dirty sessions because some databases require
          // a commit/rollback before calling close()
          sqlSession.commit(true);
        }
        // 4、返回结果
        return result;
      } catch (Throwable t) {
        Throwable unwrapped = unwrapThrowable(t);
        if (SqlSessionTemplate.this.exceptionTranslator != null && unwrapped instanceof PersistenceException) {
          // release the connection to avoid a deadlock if the translator is no loaded. See issue #22
          closeSqlSession(sqlSession, SqlSessionTemplate.this.sqlSessionFactory);
          sqlSession = null;
          Throwable translated = SqlSessionTemplate.this.exceptionTranslator
              .translateExceptionIfPossible((PersistenceException) unwrapped);
          if (translated != null) {
            unwrapped = translated;
          }
        }
        throw unwrapped;
      } finally {
        if (sqlSession != null) {
          //4、必要时关闭SqlSession  
          // 检查作为参数传递的SqlSession是否由Spring TransactionSynchronizationManager管理。
          // 如果不是，则关闭它。
          // 否则它只是更新引用计数器，并让Spring在管理事务结束时调用close回调.（通过SqlSessionSynchronization）
          closeSqlSession(sqlSession, SqlSessionTemplate.this.sqlSessionFactory);
        }
      }
    }
  }


  /**
   * Returns if the {@code SqlSession} passed as an argument is being managed by Spring
   *
   * @param session
   *          a MyBatis SqlSession to check
   * @param sessionFactory
   *          the SqlSessionFactory which the SqlSession was built with
   * @return true if session is transactional, otherwise false
   */
  public static boolean isSqlSessionTransactional(SqlSession session, SqlSessionFactory sessionFactory) {
    notNull(session, NO_SQL_SESSION_SPECIFIED);
    notNull(sessionFactory, NO_SQL_SESSION_FACTORY_SPECIFIED);
    // 事务同步管理器中是否有SqlSessionHolder
    SqlSessionHolder holder = (SqlSessionHolder) TransactionSynchronizationManager.getResource(sessionFactory);
    // 如果事务同步管理器中有SqlSessionHolder，而且保存的SqlSession和传递过来的是一样的，说明传递过来的是Spring事务管理的。
    return (holder != null) && (holder.getSqlSession() == session);
  }
```

### 2、创建DefaultSqlSession实例

使用SqlSessionUtils从Spring TransactionSynchronizationManager注册和获取SqlSession

```java
/**
*
* 从Spring Transaction Manager获取SqlSession，或者根据需要创建一个新的。
*  尝试从当前事务中获取SqlSession。如果没有，则创建一个新的。然后，如果Spring TX是活动的，
*  并且<code>SpringManagedTransactionFactory</code>被配置为事务管理器，则它将SqlSession与事务同步。
*
*
* Gets an SqlSession from Spring Transaction Manager or creates a new one if needed. Tries to get a SqlSession out of
* current transaction. If there is not any, it creates a new one. Then, it synchronizes the SqlSession with the
* transaction if Spring TX is active and <code>SpringManagedTransactionFactory</code> is configured as a transaction
* manager.
*
* @param sessionFactory
*          a MyBatis {@code SqlSessionFactory} to create new sessions
* @param executorType
*          The executor type of the SqlSession to create
* @param exceptionTranslator
*          Optional. Translates SqlSession.commit() exceptions to Spring exceptions.
* @return an SqlSession managed by Spring Transaction Manager
* @throws TransientDataAccessResourceException
*           if a transaction is active and the {@code SqlSessionFactory} is not using a
*           {@code SpringManagedTransactionFactory}
* @see SpringManagedTransactionFactory
*/
public static SqlSession getSqlSession(SqlSessionFactory sessionFactory, ExecutorType executorType,
  PersistenceExceptionTranslator exceptionTranslator) {

    notNull(sessionFactory, NO_SQL_SESSION_FACTORY_SPECIFIED);
    notNull(executorType, NO_EXECUTOR_TYPE_SPECIFIED);

    // 从事务同步管理器获取SqlSession持有对象
    SqlSessionHolder holder = (SqlSessionHolder) TransactionSynchronizationManager.getResource(sessionFactory);

    // 从SqlSession持有对象中获取，SqlSession一开始是没有的。
    SqlSession session = sessionHolder(executorType, holder);
    if (session != null) {
    return session;
    }

    LOGGER.debug(() -> "Creating a new SqlSession");
    // 通过SqlSession工厂创建SqlSession
    session = sessionFactory.openSession(executorType);

    // 注册SqlSession持有对象
    registerSessionHolder(sessionFactory, executorType, exceptionTranslator, session);

    return session;
}
```

### 3、通过SqlSession工厂创建SqlSession

MybatisPlusAutoConfiguration引入MybatisSqlSessionFactoryBean，创建了DefaultSqlSessionFactory，用于创建SqlSession。

DefaultSqlSessionFactory中有Environment。Environment的transactionFactory属性默认是SpringManagedTransactionFactory，同时dataSource属性保存着数据源。

```java
@Override
public SqlSession openSession(ExecutorType execType) {
	return openSessionFromDataSource(execType, null, false);
}

private SqlSession openSessionFromDataSource(ExecutorType execType, TransactionIsolationLevel level, boolean autoCommit) {
    Transaction tx = null;
    try {
      // 通过mybatis的配置类获取Environment
      final Environment environment = configuration.getEnvironment();
      // 获取事务工厂SpringManagedTransactionFactory
      final TransactionFactory transactionFactory = getTransactionFactoryFromEnvironment(environment);
      // 创建事务对象。这里创建的是SpringManagedTransaction
      tx = transactionFactory.newTransaction(environment.getDataSource(), level, autoCommit);
      // 创建Executor（保存事务对象SpringManagedTransaction）
      final Executor executor = configuration.newExecutor(tx, execType);
      // 创建DefaultSqlSession     
      return new DefaultSqlSession(configuration, executor, autoCommit);
    } catch (Exception e) {
      closeTransaction(tx); // may have fetched a connection so lets call close()
      throw ExceptionFactory.wrapException("Error opening session.  Cause: " + e, e);
    } finally {
      ErrorContext.instance().reset();
    }
  }
```



* 注册SqlSession持有对象

```java
/**
* 如果同步是活动的(即一个Spring TX是活动的)，注册会话持有者。
* 注意:环境使用的数据源应该通过DataSourceTxMgr或其他tx同步与事务同步。
* 进一步假设，如果抛出异常，无论启动事务的是什么，都将处理关闭/回滚与SqlSession关联的连接
* Register session holder if synchronization is active (i.e. a Spring TX is active).
*
* Note: The DataSource used by the Environment should be synchronized with the transaction either through
* DataSourceTxMgr or another tx synchronization. 
* Further assume that if an exception is thrown, whatever started the
* transaction will handle closing / rolling back the Connection associated with the SqlSession.
*
* @param sessionFactory
*          sqlSessionFactory used for registration.
* @param executorType
*          executorType used for registration.
* @param exceptionTranslator
*          persistenceExceptionTranslator used for registration.
* @param session
*          sqlSession used for registration.
*/
private static void registerSessionHolder(SqlSessionFactory sessionFactory, ExecutorType executorType,
  PersistenceExceptionTranslator exceptionTranslator, SqlSession session) {
SqlSessionHolder holder;
 // 是否有事务同步活跃。使用@Transactional注解才有
if (TransactionSynchronizationManager.isSynchronizationActive()) {
  // 获取 Environment 
  Environment environment = sessionFactory.getConfiguration().getEnvironment();
  // 获取事务工厂。如果是SpringManagedTransactionFactory类型，则注册
  if (environment.getTransactionFactory() instanceof SpringManagedTransactionFactory) {
    LOGGER.debug(() -> "Registering transaction synchronization for SqlSession [" + session + "]");
    // 创建SqlSession持有对象
    holder = new SqlSessionHolder(session, executorType, exceptionTranslator);
    // 绑定到事务同步管理器。即绑定sessionFactory和SqlSession持有对象到当前线程
    TransactionSynchronizationManager.bindResource(sessionFactory, holder);
    // 注册SqlSessionSynchronization。用于管理事务同步管理器中的SqlSession资源。
    TransactionSynchronizationManager
        .registerSynchronization(new SqlSessionSynchronization(holder, sessionFactory));
    // 设置SqlSessionHolder和事务同步  
    holder.setSynchronizedWithTransaction(true);
    // SqlSessionHolder引用计数加一  
    holder.requested();
  } else {
    if (TransactionSynchronizationManager.getResource(environment.getDataSource()) == null) {
      LOGGER.debug(() -> "SqlSession [" + session
          + "] was not registered for synchronization because DataSource is not transactional");
    } else {
      throw new TransientDataAccessResourceException(
          "SqlSessionFactory must be using a SpringManagedTransactionFactory in order to use Spring transaction synchronization");
    }
  }
} else {
  LOGGER.debug(() -> "SqlSession [" + session
      + "] was not registered for synchronization because synchronization is not active");
}

}
```



### 4、通过DefaultSqlSession调用具体的sql方法

DefaultSqlSession使用Executor执行，可以写一个简单的update数据库的方法，最终会调用到SimpleExecutor的以下方法。

```java
@Override
public int doUpdate(MappedStatement ms, Object parameter) throws SQLException {
    Statement stmt = null;
    try {
      Configuration configuration = ms.getConfiguration();
      // 创建StatementHandler
      StatementHandler handler = configuration.newStatementHandler(this, ms, parameter, RowBounds.DEFAULT, null, null);
      // 获取Statement
      stmt = prepareStatement(handler, ms.getStatementLog());
      return handler.update(stmt);
    } finally {
      closeStatement(stmt);
    }
}
```

```java
private Statement prepareStatement(StatementHandler handler, Log statementLog) throws SQLException {
    Statement stmt;
    // 获取数据库连接。重点在于这里的数据库连接如何和事务使用的数据库连接是同一个
    Connection connection = getConnection(statementLog);
    // 根据StatementHandler获取Statement
    stmt = handler.prepare(connection, transaction.getTimeout());
    // 设置Statement的sql参数
    handler.parameterize(stmt);
    return stmt;
  }
```

```java
protected Connection getConnection(Log statementLog) throws SQLException {
    // 通过事务对象（SpringManagedTransaction）获取数据库连接
    Connection connection = transaction.getConnection();
    if (statementLog.isDebugEnabled()) {
      return ConnectionLogger.newInstance(connection, statementLog, queryStack);
    } else {
      return connection;
    }
  }

```

```java
/**
 * SpringManagedTransaction处理JDBC连接的生命周期。它从Spring的事务管理器中检索连接，并在不再需要它时将其返回给它。
 * 如果Spring的事务处理是活动的，那么假定Spring事务管理器将完成所有的提交/回滚/关闭调用，它将不执行任何操作。
 * 如果不是，它的行为将类似于JdbcTransaction。
 *
 * {@code SpringManagedTransaction} handles the lifecycle of a JDBC connection. It retrieves a connection from Spring's
 * transaction manager and returns it back to it when it is no longer needed.
 * <p>
 * If Spring's transaction handling is active it will no-op all commit/rollback/close calls assuming that the  Spring
 * transaction manager will do the job.
 * <p>
 * If it is not it will behave like {@code JdbcTransaction}.
 *
 * @author Hunter Presnall
 * @author Eduardo Macarron
 */
public class SpringManagedTransaction implements Transaction {

  private static final Logger LOGGER = LoggerFactory.getLogger(SpringManagedTransaction.class);

  // 数据源
  private final DataSource dataSource;

  // 数据库连接  
  private Connection connection;

  // 数据库连接是否是事务的  
  private boolean isConnectionTransactional;
 
  // 是否自动提交（默认为false）  
  private boolean autoCommit;

  public SpringManagedTransaction(DataSource dataSource) {
    notNull(dataSource, "No DataSource specified");
    this.dataSource = dataSource;
  }

  /**
   *  获取数据库连接
   * {@inheritDoc}
   */
  @Override
  public Connection getConnection() throws SQLException {
    // 数据库连接为null，则开启数据库连接
    if (this.connection == null) {
      openConnection();
    }
    // 返回数据库连接
    return this.connection;
  }

  /**
   * 从Spring事务管理器获取一个连接，并发现这个事务是应该管理连接还是让它进入Spring。
   * 它还读取自动提交设置，因为当使用Spring Transaction时，MyBatis认为自动提交总是为false，
   * 并且总是调用commit/rollback，所以我们需要no-op调用。
   *
   * Gets a connection from Spring transaction manager and discovers if this {@code Transaction} should manage
   * connection or let it to Spring.
   * <p>
   * It also reads autocommit setting because when using Spring Transaction MyBatis thinks that autocommit is always false and will always call commit/rollback so we need to no-op that calls.
   */
  private void openConnection() throws SQLException {     
    // 使用DataSourceUtils根据数据库源获取数据库连接
    // 见【关键类】--> DataSourceUtils -->【一、获取数据库连接（必要时注册到事务同步管理器）】  
    this.connection = DataSourceUtils.getConnection(this.dataSource);
    // 是否自动提交  
    this.autoCommit = this.connection.getAutoCommit();
    // 数据库连接是否是事务的。见【关键类】--> DataSourceUtils -->【三、数据库连接是否是事务的】  
    this.isConnectionTransactional = DataSourceUtils.isConnectionTransactional(this.connection, this.dataSource);

    LOGGER.debug(() -> "JDBC Connection [" + this.connection + "] will"
        + (this.isConnectionTransactional ? " " : " not ") + "be managed by Spring");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void commit() throws SQLException {
    // 如果数据库连接不为null，而且不是事务的，不是自动提交的    
    if (this.connection != null && !this.isConnectionTransactional && !this.autoCommit) {
      LOGGER.debug(() -> "Committing JDBC Connection [" + this.connection + "]");
      this.connection.commit();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void rollback() throws SQLException {
    // 如果数据库连接不为null，而且不是事务的，不是自动提交的  
    if (this.connection != null && !this.isConnectionTransactional && !this.autoCommit) {
      LOGGER.debug(() -> "Rolling back JDBC Connection [" + this.connection + "]");
      this.connection.rollback();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() throws SQLException {
    // 释放数据库连接  
    DataSourceUtils.releaseConnection(this.connection, this.dataSource);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Integer getTimeout() throws SQLException {
    // 获取数据库连接超时时间  
    ConnectionHolder holder = (ConnectionHolder) TransactionSynchronizationManager.getResource(dataSource);
    if (holder != null && holder.hasTimeout()) {
      return holder.getTimeToLiveInSeconds();
    }
    return null;
  }

}

```

### 5、调试Demo

```java
/**
 * 调试Mybatis如何和Spring事务共用数据库连接
 * @throws NotRunTimeException
 */
@Override
//@Transactional(propagation = Propagation.REQUIRED)
@Transactional(propagation = Propagation.SUPPORTS)
public void testSyncConnection() throws NotRunTimeException {
    addDemoPerson();
    animalService.testRequired();
    //animalService.testSupported();
    //animalService.testRequiredNew();
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

// animalService
@Override
@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
public void testRequired() throws NotRunTimeException {
    saveDemoAnimal();
}
private void saveDemoAnimal() throws NotRunTimeException {
    String name = UUID.randomUUID().toString().substring(0, 5);
    Integer age = new Random().nextInt();
    Animal p2 = new Animal().setName(name).setAge(age);
    save(p2);
    log.info("animal={}", p2);
    // 编译时异常
    // throw new NotRunTimeException("saveDemoAnimal error");
    // 运行时异常
    //throw new ServiceException("saveDemoAnimal error");
}
```



## 二、SqlSessionSynchronization

位于SqlSessionUtils

```java
  /**
  * 用于清理资源的回调。它清理TransactionSynchronizationManager，并提交和关闭SqlSession。
  * 它假设连接生命周期将由DataSourceTransactionManager或JtaTransactionManager管理    
   * Callback for cleaning up resources. It cleans TransactionSynchronizationManager and also commits and closes the
   * {@code SqlSession}. It assumes that {@code Connection} life cycle will be managed by
   * {@code DataSourceTransactionManager} or {@code JtaTransactionManager}
   */
  private static final class SqlSessionSynchronization extends TransactionSynchronizationAdapter {

    private final SqlSessionHolder holder;

    private final SqlSessionFactory sessionFactory;

    private boolean holderActive = true;

    public SqlSessionSynchronization(SqlSessionHolder holder, SqlSessionFactory sessionFactory) {
      notNull(holder, "Parameter 'holder' must be not null");
      notNull(sessionFactory, "Parameter 'sessionFactory' must be not null");

      this.holder = holder;
      this.sessionFactory = sessionFactory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getOrder() {
      // order right before any Connection synchronization
      return DataSourceUtils.CONNECTION_SYNCHRONIZATION_ORDER - 1;
    }

    /**
     * 挂起此同步。假定从TransactionSynchronizationManager中解除绑定资源(如果管理任何资源)。     
     * Suspend this synchronization. 
     * Supposed to unbind resources from TransactionSynchronizationManager if managing any.
     */
    @Override
    public void suspend() {
      if (this.holderActive) {
        LOGGER.debug(() -> "Transaction synchronization suspending SqlSession [" + this.holder.getSqlSession() + "]");
        // 移除当前线程中的sessionFactory对应的SqlSessionHolder
        TransactionSynchronizationManager.unbindResource(this.sessionFactory);
      }
    }

    /**
     * 恢复此同步。如果管理任何资源，应该将资源重新绑定到TransactionSynchronizationManager。
     * Resume this synchronization. 
     * Supposed to rebind resources to TransactionSynchronizationManager if managing any.
     */
    @Override
    public void resume() {
      // 如果有SqlSessionHolder活跃，默认为true 
      if (this.holderActive) {
        LOGGER.debug(() -> "Transaction synchronization resuming SqlSession [" + this.holder.getSqlSession() + "]");
        // 恢复挂起的SqlSessionHolder，重新绑定到TransactionSynchronizationManager
        TransactionSynchronizationManager.bindResource(this.sessionFactory, this.holder);
      }
    }

    /**
     * 在事务提交之前调用(在"beforeCompletion"之前)。例如，可以刷新事务O/R映射会话到数据库。
	 *  这个回调并不意味着事务将被实际提交。在调用此方法之后仍然可以发生回滚决策。
	 *  这个回调的目的是执行只有在提交仍有可能发生时才有意义的工作，比如将SQL语句刷新到数据库中。
	 *  请注意，异常将被传播到提交调用方，并导致事务回滚。
     * Invoked before transaction commit (before "beforeCompletion"). 
     * Can e.g. flush transactional O/R Mapping sessions to the database.
	 * This callback does not mean that the transaction will actually be committed. 
	 * A rollback decision can still occur after this method has been called. 
	 * This callback is rather meant to perform work that's only relevant if a commit still has a chance to happen, 	 * such as flushing SQL statements to the database.
     * Note that exceptions will get propagated to the commit caller and cause a rollback of the transaction.
     */
    @Override
    public void beforeCommit(boolean readOnly) {
      // 数据库连接的提交或者回滚将被ConnectionSynchronization或者DataSourceTransactionManager处理
      // 但是实际上 SqlSession/Executor的清理，包含冲洗(输出)BATCH语句，实际上是在这里执行的。
      // SpringManagedTransaction在jdbc的数据库连接上将不会做出提交操作。
      // TODO这将更新第二级缓存，但tx可能会在稍后回滚!          
      // Connection commit or rollback will be handled by ConnectionSynchronization or
      // DataSourceTransactionManager.
      // But, do cleanup the SqlSession / Executor, including flushing BATCH statements so
      // they are actually executed.
      // SpringManagedTransaction will no-op the commit over the jdbc connection
      // TODO This updates 2nd level caches but the tx may be rolledback later on!
        
      // 如果有事务活跃  
      if (TransactionSynchronizationManager.isActualTransactionActive()) {
        try {
          LOGGER.debug(() -> "Transaction synchronization committing SqlSession [" + this.holder.getSqlSession() + "]");
          // 提交SqlSession。从上面说明可以知道，如果有事务存在，这里不会提交事务，只会冲洗输出sql。见DefaultSqlSession类
          this.holder.getSqlSession().commit();
        } catch (PersistenceException p) {
          if (this.holder.getPersistenceExceptionTranslator() != null) {
            DataAccessException translated = this.holder.getPersistenceExceptionTranslator()
                .translateExceptionIfPossible(p);
            if (translated != null) {
              throw translated;
            }
          }
          throw p;
        }
      }
    }

     // 通常会在这里关闭SqlSession
    /**
     * Invoked before transaction commit/rollback. Can perform resource cleanup before transaction completion.
	 * This method will be invoked after beforeCommit, even when beforeCommit threw an exception.
     * This callback allows for closing resources before transaction completion, for any outcome.
     */
    @Override
    public void beforeCompletion() {
      // Issue #18 Close SqlSession and deregister it now
      // because afterCompletion may be called from a different thread
      // 如果SqlSessionHolder没有开启过
      if (!this.holder.isOpen()) {
        LOGGER
            .debug(() -> "Transaction synchronization deregistering SqlSession [" + this.holder.getSqlSession() + "]");
        // 解绑当前线程中的SqlSessionHolder 
        TransactionSynchronizationManager.unbindResource(sessionFactory);
        // 设置没有SqlSessionHolder活跃  
        this.holderActive = false;
        LOGGER.debug(() -> "Transaction synchronization closing SqlSession [" + this.holder.getSqlSession() + "]");
        // 关闭SqlSession。
        // 通常在这里不会释放数据库连接。具体见DataSourceUtils.releaseConnection(this.connection, this.dataSource);
        this.holder.getSqlSession().close();
      }
    }

    /**
    * 事务提交/回滚后调用。可以在事务完成后执行资源清理。
	* 注意:事务将已经提交或回滚，但事务资源可能仍然是活动的和可访问的。
	* 因此，此时触发的任何数据访问代码仍将“参与”原始事务，允许执行一些清理(不再遵循提交!)，
	* 除非它显式声明它需要在单独的事务中运行。因此:对从这里调用的任何事务操作使用PROPAGATION_REQUIRES_NEW。
    *Invoked after transaction commit/rollback. Can perform resource cleanup after transaction completion.
NOTE: The transaction will have been committed or rolled back already, but the transactional resources might still be active and accessible. As a consequence, any data access code triggered at this point will still "participate" in the original transaction, allowing to perform some cleanup (with no commit following anymore!), unless it explicitly declares that it needs to run in a separate transaction. Hence: Use PROPAGATION_REQUIRES_NEW for any transactional operation that is called from here.
     */
    @Override
    public void afterCompletion(int status) {
      // 如果有SqlSessionHolder活跃
      if (this.holderActive) {
        // afterCompletion may have been called from a different thread
        // so avoid failing if there is nothing in this one
        LOGGER
            .debug(() -> "Transaction synchronization deregistering SqlSession [" + this.holder.getSqlSession() + "]");
        // 解绑SqlSessionHolder  
        TransactionSynchronizationManager.unbindResourceIfPossible(sessionFactory);
        // 设置没有SqlSessionHolder活跃
        this.holderActive = false;
        LOGGER.debug(() -> "Transaction synchronization closing SqlSession [" + this.holder.getSqlSession() + "]");
        // 关闭SqlSession
        this.holder.getSqlSession().close();
      }
      // 重置 SqlSessionHolder 
      this.holder.reset();
    }
  }
```

```java
/**
 * 返回是否仍然有SqlSessionHolder的开启引用
 * Return whether there are still open references to this holder.
 */
public boolean isOpen() {
    // 引用数量是否大于0
    return (this.referenceCount > 0);
}

/**
 * 重置 SqlSessionHolder 
 * Reset this resource holder - transactional state as well as reference count.
 */
@Override
public void reset() {
    clear();
    this.referenceCount = 0;
}
/**
 * Clear the transactional state of this resource holder.
 */
public void clear() {
    this.synchronizedWithTransaction = false;
    this.rollbackOnly = false;
    this.deadline = null;
}
```

* DefaultSqlSession

```java
public class DefaultSqlSession implements SqlSession {
	private final Configuration configuration;
    private final Executor executor;
    private final boolean autoCommit;
    private boolean dirty;
    private List<Cursor<?>> cursorList;
    
 	@Override
  public void commit() {
    commit(false);
  }

  @Override
  public void commit(boolean force) {
    try {
      executor.commit(isCommitOrRollbackRequired(force));
      dirty = false;
    } catch (Exception e) {
      throw ExceptionFactory.wrapException("Error committing transaction.  Cause: " + e, e);
    } finally {
      ErrorContext.instance().reset();
    }
  }
     
  private boolean isCommitOrRollbackRequired(boolean force) {
    // 默认情况：!autoCommit = true ，dirty = false
    return (!autoCommit && dirty) || force;
  }  
    
}
```

* BaseExecutor

```java
@Override
public void commit(boolean required) throws SQLException {
    if (closed) {
      throw new ExecutorException("Cannot commit, transaction is already closed");
    }
    // 清除本地缓存
    clearLocalCache();
    // 冲洗输出sql语句
    flushStatements();
    // 需要的话，提交事务对象
    if (required) {      
      transaction.commit();
    }
}
```

* SpringManagedTransaction

```java
private Connection connection;

private boolean isConnectionTransactional;

private boolean autoCommit;

/**
* Commit inner database connection.
*/	
@Override
public void commit() throws SQLException {
    // 如果数据库连接不为空，而且数据库连接不是事务的，以及不是自动提交的，则提交事务
    if (this.connection != null && !this.isConnectionTransactional && !this.autoCommit) {
      LOGGER.debug(() -> "Committing JDBC Connection [" + this.connection + "]");
      this.connection.commit();
    }
}


/**
* Gets a connection from Spring transaction manager and discovers if this {@code Transaction} should manage
* connection or let it to Spring.
* <p>
* It also reads autocommit setting because when using Spring Transaction MyBatis thinks that autocommit is always
* false and will always call commit/rollback so we need to no-op that calls.
*/
private void openConnection() throws SQLException {
    this.connection = DataSourceUtils.getConnection(this.dataSource);
    this.autoCommit = this.connection.getAutoCommit();
    // 数据库连接是否是事务的。见【关键类】--> DataSourceUtils -->【三、数据库连接是否是事务的】  
    this.isConnectionTransactional = DataSourceUtils.isConnectionTransactional(this.connection, this.dataSource);
    LOGGER.debug(() -> "JDBC Connection [" + this.connection + "] will"
        + (this.isConnectionTransactional ? " " : " not ") + "be managed by Spring");
}
```



## 三、ConnectionSynchronization

```java
	/**
	 * 在非本机JDBC事务结束时进行资源清理的回调(例如，当参与JtaTransactionManager事务时)。
	 * Callback for resource cleanup at the end of a non-native JDBC transaction
	 * (e.g. when participating in a JtaTransactionManager transaction).
	 * @see org.springframework.transaction.jta.JtaTransactionManager
	 */
	private static class ConnectionSynchronization implements TransactionSynchronization {

		private final ConnectionHolder connectionHolder;

		private final DataSource dataSource;

		private int order;

		private boolean holderActive = true;

		public ConnectionSynchronization(ConnectionHolder connectionHolder, DataSource dataSource) {
			this.connectionHolder = connectionHolder;
			this.dataSource = dataSource;
			this.order = getConnectionSynchronizationOrder(dataSource);
		}

		@Override
		public int getOrder() {
			return this.order;
		}

		@Override
		public void suspend() {
             // 如果有ConnectionHolder活跃，默认为true
			if (this.holderActive) {
                 // 解绑当前线程的ConnectionHolder
				TransactionSynchronizationManager.unbindResource(this.dataSource);
                  //如果有数据库连接，而且数据库连接持有对象没有开启（没有被引用了）
				if (this.connectionHolder.hasConnection() && !this.connectionHolder.isOpen()) {
                      //如果应用程序没有保持，则在挂起时释放连接它的句柄。
                      //我们将获取一个新的连接，如果应用程序在恢复后再次访问ConnectionHolder
                      //假设它将参与同一事务。
					// Release Connection on suspend if the application doesn't keep
					// a handle to it anymore. We will fetch a fresh Connection if the
					// application accesses the ConnectionHolder again after resume,
					// assuming that it will participate in the same transaction.
                      // 释放数据库连接
					releaseConnection(this.connectionHolder.getConnection(), this.dataSource);
					this.connectionHolder.setConnection(null);
				}
			}
		}

		@Override
		public void resume() {
             // 如果有ConnectionHolder活跃，默认为true
			if (this.holderActive) {
                  // 重新绑定ConnectionHolder到当前线程
				TransactionSynchronizationManager.bindResource(this.dataSource, this.connectionHolder);
			}
		}
		
         // 对于ConnectionSynchronization，通常会在这里释放数据库连接
		@Override
		public void beforeCompletion() {
			// Release Connection early if the holder is not open anymore
			// (that is, not used by another resource like a Hibernate Session
			// that has its own cleanup via transaction synchronization),
			// to avoid issues with strict JTA implementations that expect
			// the close call before transaction completion.
             // 如果数据库连接持有对象没有开启（没有被引用了）
			if (!this.connectionHolder.isOpen()) {
                 // 解绑当前线程的ConnectionHolder
				TransactionSynchronizationManager.unbindResource(this.dataSource);
                  // 设置没有数据库连接持有对象活跃 
				this.holderActive = false;
                  // 如果有数据库连接
				if (this.connectionHolder.hasConnection()) {
                      // 释放数据库连接
					releaseConnection(this.connectionHolder.getConnection(), this.dataSource);
				}
			}
		}

		@Override
		public void afterCompletion(int status) {
			// If we haven't closed the Connection in beforeCompletion,
			// close it now. The holder might have been used for other
			// cleanup in the meantime, for example by a Hibernate Session.
             // 如果有ConnectionHolder活跃
			if (this.holderActive) {
				// The thread-bound ConnectionHolder might not be available anymore,
				// since afterCompletion might get called from a different thread.
                  // 解绑当前线程的ConnectionHolder
				TransactionSynchronizationManager.unbindResourceIfPossible(this.dataSource);
                  // 设置没有数据库连接持有对象活跃 
				this.holderActive = false;
			    // 如果有数据库连接
				if (this.connectionHolder.hasConnection()) {
                      // 释放数据库连接
					releaseConnection(this.connectionHolder.getConnection(), this.dataSource);
					// Reset the ConnectionHolder: It might remain bound to the thread.
					this.connectionHolder.setConnection(null);
				}
			}
             // 重置ConnectionHolder
			this.connectionHolder.reset();
		}
	}
```

```java
/**
 * Return whether there are still open references to this holder.
 */
public boolean isOpen() {
    // 引用个数是否大于0
    return (this.referenceCount > 0);
}
```



## 四、动态数据源整合

有了这些基础，可以看MybatisPlus动态数据源事务是怎么实现的
