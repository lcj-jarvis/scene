# 一、基本概念

## （1）切面(Aspect)

切面是一个横切关注点的模块化，一个切面能够包含同一个类型的不同增强方法，比如说事务处理和日志处理可以理解为两个切面。切面由切入点和通知组成，它既包含了横切逻辑的定义，也包括了切入点的定义。 Spring AOP就是负责实施切面的框架，它将切面所定义的横切逻辑织入到切面所指定的连接点中。

```java
@Component
@Aspect
public class LogAspect {
}
```

## （2）目标对象(Target)

目标对象指将要被增强的对象(被代理对象)，即包含主业务逻辑的类对象。或者说是被一个或者多个切面所通知的对象。

## （3）连接点(JoinPoint)

程序执行过程中明确的点，如方法的调用或特定的异常被抛出。连接点由两个信息确定：

- 方法(表示程序执行点，即在哪个目标方法)
- 相对点(表示方位，即目标方法的什么位置，比如调用前，后等)

**简单来说，连接点就是被拦截到的程序执行点，因为Spring只支持方法类型的连接点，所以在Spring中连接点就是被拦截到的方法**。

```java
@Before("pointcut()")
public void log(JoinPoint joinPoint) { //这个JoinPoint参数就是连接点
}
```

## （4）切入点(PointCut)

切入点是对连接点进行拦截的条件定义。切入点表达式如何和连接点匹配是AOP的核心，Spring默认使用AspectJ切入点语法。 一般认为，所有的方法都可以认为是连接点，但是我们并不希望在所有的方法上都添加通知，而切入点的作用就是提供一组规则(使用 AspectJ pointcut expression language 来描述) 来匹配连接点，给满足规则的连接点添加通知。

```java
@Pointcut("execution(* com.remcarpediem.test.aop.service..*(..))")
public void pointcut() {
}
```

## （5）通知(Advice)

通知是指拦截到连接点之后要执行的代码，包括了“around”、“before”和“after”等不同类型的通知。Spring AOP框架以拦截器来实现通知模型，并维护一个以连接点为中心的拦截器链。

```java
// @Before说明这是一个前置通知，log函数中是要前置执行的代码，JoinPoint是连接点，
@Before("pointcut()")
public void log(JoinPoint joinPoint) { 
}
```

## （6）织入(Weaving)

织入是将切面和业务逻辑对象连接起来, 并创建通知代理的过程。织入可以在编译时，类加载时和运行时完成。在编译时进行织入就是静态代理，而在运行时进行织入则是动态代理。

## （7）增强器(Advisor)

Advisor是切面的另外一种实现，能够将通知以更为复杂的方式织入到目标对象中，是将通知包装为更复杂切面的装配器。**Advisor由切入点PointCut和通知Advice组成**。 Advisor这个概念来自于Spring对AOP的支撑，在AspectJ中是没有等价的概念的。**Advisor就像是一个小的自包含的切面，这个切面只有一个通知**。切面自身通过一个Bean表示，并且必须实现一个默认接口。

```java
// AbstractPointcutAdvisor是默认接口
public class LogAdvisor extends AbstractPointcutAdvisor {
 private Advice advice; // Advice
 private Pointcut pointcut; // 切入点

 @PostConstruct
 public void init() {
 // AnnotationMatchingPointcut是依据修饰类和方法的注解进行拦截的切入点。
 this.pointcut = new AnnotationMatchingPointcut((Class) null, Log.class);
 // 通知
 this.advice = new LogMethodInterceptor();
 }
}
```

**说明：如果我们需要自定义增强器，切入点可以使用AnnotationMatchingPointcut**



## （8）总结

简单来讲，整个 aspect 可以描述为: **满足 pointcut 规则的 joinpoint 会被添加相应的 advice 操作**



# 二、调试代码

## 1、创建工程

为了看缓存切面的创建、事务切面的创建，创建springboot工程，加入redis的注解操作缓存的依赖。（其他依赖先忽略）

```xml
<dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
```

 在springboot 的主启动类加入以下注解

```java
@EnableCaching
@EnableTransactionManagement
```

## 2、自定义切面

* 注解

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface WebLogAnno {
}
```

* 切面

```java
@Aspect
@Component
@Order(1)
public class WebLogAspect {

    private final static Logger LOGGER = LoggerFactory.getLogger(WebLogAspect.class);

    /** 以 controller 包下定义的所有请求为切入点 */
    // @Pointcut("execution(public * com..*..*.controller..*.*(..))")
    // 以WebLogAnno为切入点
    @Pointcut("@annotation(com.mrlu.aop.service.WebLogAnno)")
    public void webLog() {}

    /**
     * 在切点之前织入
     * @param joinPoint
     * @throws Throwable
     */
    @Before("webLog()")
    public void doBefore(JoinPoint joinPoint) throws Throwable {
        //  开始打印请求日志
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();

        // 初始化traceId
        // initTraceId(request);
        // 打印请求相关参数
        //LOGGER.info("========================================== Start ==========================================");
        //// 打印请求 url
        //LOGGER.info("URL            : {}", request.getRequestURL().toString());
        //// 打印 Http method
        //LOGGER.info("HTTP Method    : {}", request.getMethod());
        //// 打印调用 controller 的全路径以及执行方法
        //LOGGER.info("Class Method   : {}.{}", joinPoint.getSignature().getDeclaringTypeName(), joinPoint.getSignature().getName());
        //// 打印请求的 IP
        //LOGGER.info("IP             : {}", IPAddressUtil.getIpAdrress(request));
        //// 打印请求入参
        //LOGGER.info("Request Args   : {}", joinPoint.getArgs());


        LOGGER.info("-------------doBefore-------------");
    }

    @AfterReturning("webLog()")
    public void afterReturning() {
        LOGGER.info("-------------afterReturning-------------");
    }

    @AfterThrowing("webLog()")
    public void afterThrowing() {
        LOGGER.info("-------------afterThrowing-------------");
    }

    /**
     * 在切点之后织入
     * @throws Throwable
     */
    @After("webLog()")
    public void doAfter() throws Throwable {
        LOGGER.info("-------------doAfter-------------");
    }

    /**
     * 环绕
     * @param proceedingJoinPoint
     * @return
     * @throws Throwable
     */
    @Around("webLog()")
    public Object doAround(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        LOGGER.info("-------------doAround before proceed-------------");
        Object result = proceedingJoinPoint.proceed();
        // 打印出参
        LOGGER.info("Response Args  : {}", result);
        // 执行耗时
        LOGGER.info("Time-Consuming : {} ms", System.currentTimeMillis() - startTime);
        LOGGER.info("-------------doAround after proceed-------------");
        return result;
    }

}
```

```java
public interface WhInterface {
    @Transactional
    void tt(String type);
}
```

```java
public abstract class WhParentService implements WhInterface {
    @Override
    @WebLogAnno
    @Cacheable(cacheNames = "wh-tt", key = "#type")
    public void tt(String type) {
        System.out.println("ttttttttttttttt");
    }
}
```

```java
@Service
public class WhService extends WhParentService implements InitializingBean, WhInterface {

    // 使用cglib动态代理的时候，注入具体的类不会报错，因为cglib动态代理类是继承AopInjectServiceImpl来创建的
    // 使用jdk动态代理的时候，AopInjectService为合理的被代理接口，被代理对象基于实现接口来的，它本身已经继承Proxy类了，所以会报错。
    // 所以我们一般建议注入接口，不要注入具体的实现类
    /*
    The bean 'aopInjectServiceImpl' could not be injected because it is a JDK dynamic proxy
    The bean is of type 'com.sun.proxy.$Proxy71' and implements:
    com.mrlu.aop.service.AopInjectService
    org.springframework.beans.factory.InitializingBean
    org.springframework.aop.SpringProxy
    org.springframework.aop.framework.Advised
    org.springframework.core.DecoratingProxy
    */
    //@Autowired
    //private AopInjectServiceImpl aopInjectServiceImpl;

    @Autowired
    private AopInjectService aopInjectService;

    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("WhService");
    }

    @Transactional
    @WebLogAnno
    @Cacheable(cacheNames = "whCache", key = "#type")
    public String getWhList(String type) {
        System.out.println("getWhList");
        return "getWhList";
    }

}

```



```java
public interface AopInjectService {
    void proxyedMethod();
}
```

```java
@Service
public class AopInjectServiceImpl implements AopInjectService, InitializingBean {

    @WebLogAnno
    @Transactional
    @Override
    public void proxyedMethod() {
        System.out.println("---被代理方法----");
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("--AopInjectServiceImpl afterPropertiesSet--");
    }
}
```





## 3、调试说明

引入缓存切面，事务切面，自定义的切面调试，先在WhService的afterPropertiesSet方法里打断点，然后再调试到切面的AnnotationAwareAspectJAutoProxyCreator的findCandidateAdvisors()方法。为什么会调试到findCandidateAdvisors方法呢，下面有说明

# 三、AOP源码解析

## 1、AOP的初始化

### 1.1 AOP后置处理器引入与升级

* @EnableCaching

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(CachingConfigurationSelector.class)
public @interface EnableCaching {
	boolean proxyTargetClass() default false;
	AdviceMode mode() default AdviceMode.PROXY;
	int order() default Ordered.LOWEST_PRECEDENCE;
}
```

引入CachingConfigurationSelector 注册类，加载AutoProxyRegistrar和ProxyCachingConfiguration到spring。

![image-20240515171027948](https://lu-note.oss-cn-shenzhen.aliyuncs.com/notes/work/image-20240515171027948.png)

（1）ProxyCachingConfiguration

```java
@Configuration(proxyBeanMethods = false)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class ProxyCachingConfiguration extends AbstractCachingConfiguration {

    
     // 属于Advisor类型（增强器）
	@Bean(name = CacheManagementConfigUtils.CACHE_ADVISOR_BEAN_NAME)
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public BeanFactoryCacheOperationSourceAdvisor cacheAdvisor(
			CacheOperationSource cacheOperationSource, CacheInterceptor cacheInterceptor) {

		BeanFactoryCacheOperationSourceAdvisor advisor = new BeanFactoryCacheOperationSourceAdvisor();
         // 设置缓存操作来源AnnotationCacheOperationSource到切入点CacheOperationSourcePointcut
		advisor.setCacheOperationSource(cacheOperationSource);
         // 设置通知
		advisor.setAdvice(cacheInterceptor);
		if (this.enableCaching != null) {
			advisor.setOrder(this.enableCaching.<Integer>getNumber("order"));
		}
		return advisor;
	}

	@Bean
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public CacheOperationSource cacheOperationSource() {
		return new AnnotationCacheOperationSource();
	}

     // 属于MethodInterceptor类型。（增强器里的拦截器，也可以理解为通知）
	@Bean
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public CacheInterceptor cacheInterceptor(CacheOperationSource cacheOperationSource) {
		CacheInterceptor interceptor = new CacheInterceptor();
		interceptor.configure(this.errorHandler, this.keyGenerator, this.cacheResolver, this.cacheManager);
		interceptor.setCacheOperationSource(cacheOperationSource);
		return interceptor;
	}

}
```

即注入缓存增强器BeanFactoryCacheOperationSourceAdvisor到spring中。



* @EnableTransactionManagement

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(TransactionManagementConfigurationSelector.class)
public @interface EnableTransactionManagement {
	boolean proxyTargetClass() default false;
	AdviceMode mode() default AdviceMode.PROXY;
	int order() default Ordered.LOWEST_PRECEDENCE;
}
```

引入TransactionManagementConfigurationSelector注册类，加载AutoProxyRegistrar和ProxyTransactionManagementConfiguration到spring。

![image-20240515170909996](https://lu-note.oss-cn-shenzhen.aliyuncs.com/notes/work/image-20240515170909996.png)

（1）ProxyTransactionManagementConfiguration

```java
@Configuration(proxyBeanMethods = false)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class ProxyTransactionManagementConfiguration extends AbstractTransactionManagementConfiguration {
    
    // 属于Advisor类型（增强器）
	@Bean(name = TransactionManagementConfigUtils.TRANSACTION_ADVISOR_BEAN_NAME)
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public BeanFactoryTransactionAttributeSourceAdvisor transactionAdvisor(
			TransactionAttributeSource transactionAttributeSource, TransactionInterceptor transactionInterceptor) {   
		BeanFactoryTransactionAttributeSourceAdvisor advisor = new BeanFactoryTransactionAttributeSourceAdvisor();
         // 设置事务属性来源AnnotationTransactionAttributeSource到切入点TransactionAttributeSourcePointcut
		advisor.setTransactionAttributeSource(transactionAttributeSource);
         // 设置通知
		advisor.setAdvice(transactionInterceptor);
		if (this.enableTx != null) {
			advisor.setOrder(this.enableTx.<Integer>getNumber("order"));
		}
		return advisor;
	}

	@Bean
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public TransactionAttributeSource transactionAttributeSource() {
		return new AnnotationTransactionAttributeSource();
	}

      // 属于MethodInterceptor类型。（增强器里的拦截器，也可以理解为通知）
	@Bean
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public TransactionInterceptor transactionInterceptor(TransactionAttributeSource transactionAttributeSource) {
		TransactionInterceptor interceptor = new TransactionInterceptor();
		interceptor.setTransactionAttributeSource(transactionAttributeSource);
		if (this.txManager != null) {
			interceptor.setTransactionManager(this.txManager);
		}
		return interceptor;
	}

}
```

即注入事务增强器BeanFactoryTransactionAttributeSourceAdvisor到spring中。



* 默认情况springboot自动配置引入的@EnableAspectJAutoProxy(proxyTargetClass = true)

![image-20240515163429723](https://lu-note.oss-cn-shenzhen.aliyuncs.com/notes/work/image-20240515163429723.png)

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(AspectJAutoProxyRegistrar.class)
public @interface EnableAspectJAutoProxy {

	/**
	 * Indicate whether subclass-based (CGLIB) proxies are to be created as opposed
	 * to standard Java interface-based proxies. The default is {@code false}.
	 */
	boolean proxyTargetClass() default false;

	/**
	 * Indicate that the proxy should be exposed by the AOP framework as a {@code ThreadLocal}
	 * for retrieval via the {@link org.springframework.aop.framework.AopContext} class.
	 * Off by default, i.e. no guarantees that {@code AopContext} access will work.
	 * @since 4.3.1
	 */
	boolean exposeProxy() default false;
}
```



```java
class AspectJAutoProxyRegistrar implements ImportBeanDefinitionRegistrar {

	/**
	 * Register, escalate, and configure the AspectJ auto proxy creator based on the value
	 * of the @{@link EnableAspectJAutoProxy#proxyTargetClass()} attribute on the importing
	 * {@code @Configuration} class.
	 */
	@Override
	public void registerBeanDefinitions(
			AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {

		AopConfigUtils.registerAspectJAnnotationAutoProxyCreatorIfNecessary(registry);

		AnnotationAttributes enableAspectJAutoProxy =
				AnnotationConfigUtils.attributesFor(importingClassMetadata, EnableAspectJAutoProxy.class);
		if (enableAspectJAutoProxy != null) {
              // 从CglibAutoProxyConfiguration的@EnableAspectJAutoProxy(proxyTargetClass = true)获取到true。
			if (enableAspectJAutoProxy.getBoolean("proxyTargetClass")) {
                  // 设置proxyTargetClass=true
				AopConfigUtils.forceAutoProxyCreatorToUseClassProxying(registry);
			}
             // 默认为false
			if (enableAspectJAutoProxy.getBoolean("exposeProxy")) {
                  // 暴露代理对象当前到AopContext的ThreadLocal类型变量currentProxy。
				AopConfigUtils.forceAutoProxyCreatorToExposeProxy(registry);
			}
		}
	}

}
```

结合我们知道bean定义的加载过程，上面的这些配置类或者配置类注册类。先加载主启动类上注解引入的，再加载springboot自动配置的。至于@EnableCaching和@EnableTransactionManagement注解哪个先加载，其实无所谓，AspectJAutoProxyRegistrar肯定是最后加载。

默认情况下，springboot自动配置加载CglibAutoProxyConfiguration配置类，使用cglib动态代理。如果想使用jdk动态代理，则要在yaml文件或者properties文件加入相应配置。

* yaml配置

```yaml
spring:
  aop:
    proxy-target-class: false
```

* properties配置

```properties
spring.aop.proxy-target-class=false
```



（1）假设@EnableTransactionManagement引入的AutoProxyRegistrar先加载

```java
public class AutoProxyRegistrar implements ImportBeanDefinitionRegistrar {

	private final Log logger = LogFactory.getLog(getClass());

	/**
	 * 针对给定的注册中心注册、升级和配置标准自动代理创建器(APC)。
	 *  通过查找在导入的同时具有mode和proxyTargetClass属性的@Configuration类上声明的最近的注释来工作。
	 *  如果模式设置为PROXY，则APC注册;如果proxyTargetClass设置为true，那么APC将强制使用子类(CGLIB)代理。
	 *  几个@Enable*注释暴露了mode和proxyTargetClass属性。重要的是要注意，这些功能中的大多数最终共享一个APC。
	 *  由于这个原因，这个实现并不“关心”它找到的是哪个注释
	 *  只要它公开了正确的mode和proxyTargetClass属性，APC就可以被注册和配置。
	 *  
	 *  意思就是，不管经过@Enablexxx注解的怎么升级，最终都是使用同一个动态代理创建器
	 * 
	 * Register, escalate, and configure the standard auto proxy creator (APC) against the
	 * given registry. Works by finding the nearest annotation declared on the importing
	 * {@code @Configuration} class that has both {@code mode} and {@code proxyTargetClass}
	 * attributes. If {@code mode} is set to {@code PROXY}, the APC is registered; if
	 * {@code proxyTargetClass} is set to {@code true}, then the APC is forced to use
	 * subclass (CGLIB) proxying.
	 * <p>Several {@code @Enable*} annotations expose both {@code mode} and
	 * {@code proxyTargetClass} attributes. It is important to note that most of these
	 * capabilities end up sharing a {@linkplain AopConfigUtils#AUTO_PROXY_CREATOR_BEAN_NAME
	 * single APC}. For this reason, this implementation doesn't "care" exactly which
	 * annotation it finds -- as long as it exposes the right {@code mode} and
	 * {@code proxyTargetClass} attributes, the APC can be registered and configured all
	 * the same.
	 */
	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		boolean candidateFound = false;
         // 1、获取类上的所有注解类型
		Set<String> annTypes = importingClassMetadata.getAnnotationTypes();
		for (String annType : annTypes) {
             // 2、获取类上的相应类型的注解属性
			AnnotationAttributes candidate = AnnotationConfigUtils.attributesFor(importingClassMetadata, annType);
			if (candidate == null) {
				continue;
			}            
			Object mode = candidate.get("mode");
			Object proxyTargetClass = candidate.get("proxyTargetClass");
			if (mode != null && proxyTargetClass != null && AdviceMode.class == mode.getClass() &&
					Boolean.class == proxyTargetClass.getClass()) {
             // 要求同时满足
             // (1) 注解的mode属性值和proxyTargetClass属性值不为空
             // (2) mode属性值是AdviceMode类型，proxyTargetClass属性值的布尔类型
				candidateFound = true;
				if (mode == AdviceMode.PROXY) {
                     // JDK代理，注册AutoProxyCreator
					AopConfigUtils.registerAutoProxyCreatorIfNecessary(registry);
                      // @EnableTransactionManagement注解默认proxyTargetClass=false
					if ((Boolean) proxyTargetClass) {                         
                          // 如果设置了proxyTargetClass=true，升级代理创建器。
						AopConfigUtils.forceAutoProxyCreatorToUseClassProxying(registry);
						return;
					}
				}
			}
		}
		if (!candidateFound && logger.isInfoEnabled()) {
			String name = getClass().getSimpleName();
			logger.info(String.format("%s was imported but no annotations were found " +
					"having both 'mode' and 'proxyTargetClass' attributes of type " +
					"AdviceMode and boolean respectively. This means that auto proxy " +
					"creator registration and configuration may not have occurred as " +
					"intended, and components may not be proxied as expected. Check to " +
					"ensure that %s has been @Import'ed on the same class where these " +
					"annotations are declared; otherwise remove the import of %s " +
					"altogether.", name, name, name));
		}
	}

}
```



```java
@Nullable
public static BeanDefinition registerAutoProxyCreatorIfNecessary(BeanDefinitionRegistry registry) {
    return registerAutoProxyCreatorIfNecessary(registry, null);
}

@Nullable
public static BeanDefinition registerAutoProxyCreatorIfNecessary(
        BeanDefinitionRegistry registry, @Nullable Object source) {
    return registerOrEscalateApcAsRequired(InfrastructureAdvisorAutoProxyCreator.class, registry, source);
}	
```



```java
// 动态代理创建器的bean名称
public static final String AUTO_PROXY_CREATOR_BEAN_NAME =
			"org.springframework.aop.config.internalAutoProxyCreator";
@Nullable
private static BeanDefinition registerOrEscalateApcAsRequired(
        Class<?> cls, BeanDefinitionRegistry registry, @Nullable Object source) {

    Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
	
    // 这里我们的cls=InfrastructureAdvisorAutoProxyCreator.class
    // 是否包含名为org.springframework.aop.config.internalAutoProxyCreator的bean定义
    // 一开始不包含
    if (registry.containsBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME)) {      
        BeanDefinition apcDefinition = registry.getBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME);
        if (!cls.getName().equals(apcDefinition.getBeanClassName())) {
            int currentPriority = findPriorityForClass(apcDefinition.getBeanClassName());
            int requiredPriority = findPriorityForClass(cls);
            if (currentPriority < requiredPriority) {
                apcDefinition.setBeanClassName(cls.getName());
            }
        }
        return null;
    }
    
    // 创建cls=InfrastructureAdvisorAutoProxyCreator.class的bean定义，指定bean名称为AUTO_PROXY_CREATOR_BEAN_NAME，
    RootBeanDefinition beanDefinition = new RootBeanDefinition(cls);
    beanDefinition.setSource(source);
    beanDefinition.getPropertyValues().add("order", Ordered.HIGHEST_PRECEDENCE);
    beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
    registry.registerBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME, beanDefinition);
    return beanDefinition;
}
```

总结：加载InfrastructureAdvisorAutoProxyCreator到spring



（2）@EnableCaching引入的AutoProxyRegistrar 加载

执行到AopConfigUtils的registerOrEscalateApcAsRequired方法，此时cls还是等于InfrastructureAdvisorAutoProxyCreator.class

```java
// 动态代理创建器的bean名称
public static final String AUTO_PROXY_CREATOR_BEAN_NAME =
			"org.springframework.aop.config.internalAutoProxyCreator";
@Nullable
private static BeanDefinition registerOrEscalateApcAsRequired(
        Class<?> cls, BeanDefinitionRegistry registry, @Nullable Object source) {
    Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
    // cls=InfrastructureAdvisorAutoProxyCreator.class
    // @EnableTransactionManagement的AutoProxyRegistrar中已经加载名为AUTO_PROXY_CREATOR_BEAN_NAME的bean定义       
    if (registry.containsBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME)) {
        // 获取名为AUTO_PROXY_CREATOR_BEAN_NAME的bean定义。
        // 发现类名一样，直接而结束。无需升级AutoProxyCreator，直接结束
        BeanDefinition apcDefinition = registry.getBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME);
        if (!cls.getName().equals(apcDefinition.getBeanClassName())) {
            int currentPriority = findPriorityForClass(apcDefinition.getBeanClassName());
            int requiredPriority = findPriorityForClass(cls);
            if (currentPriority < requiredPriority) {
                apcDefinition.setBeanClassName(cls.getName());
            }
        }
        return null;
    }

    RootBeanDefinition beanDefinition = new RootBeanDefinition(cls);
    beanDefinition.setSource(source);
    beanDefinition.getPropertyValues().add("order", Ordered.HIGHEST_PRECEDENCE);
    beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
    registry.registerBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME, beanDefinition);
    return beanDefinition;
}
```



（3）最后加载springboot自动配置引入的CglibAutoProxyConfiguration

![image-20240515163429723](https://lu-note.oss-cn-shenzhen.aliyuncs.com/notes/work/image-20240515163429723.png)

* @EnableAspectJAutoProxy(proxyTargetClass = true)

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(AspectJAutoProxyRegistrar.class)
public @interface EnableAspectJAutoProxy {

	/**
	 * Indicate whether subclass-based (CGLIB) proxies are to be created as opposed
	 * to standard Java interface-based proxies. The default is {@code false}.
	 */
	boolean proxyTargetClass() default false;

	/**
	 * Indicate that the proxy should be exposed by the AOP framework as a {@code ThreadLocal}
	 * for retrieval via the {@link org.springframework.aop.framework.AopContext} class.
	 * Off by default, i.e. no guarantees that {@code AopContext} access will work.
	 * @since 4.3.1
	 */
	boolean exposeProxy() default false;

}
```

```java
class AspectJAutoProxyRegistrar implements ImportBeanDefinitionRegistrar {

	/**
	 * Register, escalate, and configure the AspectJ auto proxy creator based on the value
	 * of the @{@link EnableAspectJAutoProxy#proxyTargetClass()} attribute on the importing
	 * {@code @Configuration} class.
	 */
	@Override
	public void registerBeanDefinitions(
			AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		// 注册AspectJAnnotationAutoProxyCreator
		AopConfigUtils.registerAspectJAnnotationAutoProxyCreatorIfNecessary(registry);
	    // 获取@EnableAspectJAutoProxy注解
		AnnotationAttributes enableAspectJAutoProxy =
				AnnotationConfigUtils.attributesFor(importingClassMetadata, EnableAspectJAutoProxy.class);
		if (enableAspectJAutoProxy != null) {
			if (enableAspectJAutoProxy.getBoolean("proxyTargetClass")) {
                  // 设置AutoProxyCreator的proxyTargetClass属性为true
				AopConfigUtils.forceAutoProxyCreatorToUseClassProxying(registry);
			}
             // 默认为false
			if (enableAspectJAutoProxy.getBoolean("exposeProxy")) {
                 // 暴露代理对象当前到AopContext的ThreadLocal类型变量currentProxy。
				AopConfigUtils.forceAutoProxyCreatorToExposeProxy(registry);
			}
		}
	}
}
```

【注意】**proxyTargetClass = true会使用cglib的动态代理**



* 注册AspectJAnnotationAutoProxyCreator

```java
@Nullable
public static BeanDefinition registerAspectJAnnotationAutoProxyCreatorIfNecessary(BeanDefinitionRegistry registry) {
    return registerAspectJAnnotationAutoProxyCreatorIfNecessary(registry, null);
}
@Nullable
public static BeanDefinition registerAspectJAnnotationAutoProxyCreatorIfNecessary(
        BeanDefinitionRegistry registry, @Nullable Object source) {
    return registerOrEscalateApcAsRequired(AnnotationAwareAspectJAutoProxyCreator.class, registry, source);
}
```

```java
@Nullable
private static BeanDefinition registerOrEscalateApcAsRequired(
        Class<?> cls, BeanDefinitionRegistry registry, @Nullable Object source) {

    Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
    // 这里的cls=AnnotationAwareAspectJAutoProxyCreator.class
    // 前面@EnableCaching和@EnableTransactionManagement已经引入了名为AUTO_PROXY_CREATOR_BEAN_NAME
    // 类型为InfrastructureAdvisorAutoProxyCreator的bean定义
    if (registry.containsBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME)) {
        // 获取名为AUTO_PROXY_CREATOR_BEAN_NAME的bean定义。
        BeanDefinition apcDefinition = registry.getBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME);
        if (!cls.getName().equals(apcDefinition.getBeanClassName())) {
            // 类名不一样，获取优先级进行bean类型的更新，从而实现升级
            // 这里我们升级为AnnotationAwareAspectJAutoProxyCreator。因为它的优先级最高
            int currentPriority = findPriorityForClass(apcDefinition.getBeanClassName());
            int requiredPriority = findPriorityForClass(cls);
            if (currentPriority < requiredPriority) {
                apcDefinition.setBeanClassName(cls.getName());
            }
        }
        return null;
    }

    RootBeanDefinition beanDefinition = new RootBeanDefinition(cls);
    beanDefinition.setSource(source);
    beanDefinition.getPropertyValues().add("order", Ordered.HIGHEST_PRECEDENCE);
    beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
    registry.registerBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME, beanDefinition);
    return beanDefinition;
}


private static final List<Class<?>> APC_PRIORITY_LIST = new ArrayList<>(3);
static {
    // Set up the escalation list...  越往后优先级越高
    APC_PRIORITY_LIST.add(InfrastructureAdvisorAutoProxyCreator.class);
    APC_PRIORITY_LIST.add(AspectJAwareAdvisorAutoProxyCreator.class);
    APC_PRIORITY_LIST.add(AnnotationAwareAspectJAutoProxyCreator.class);
}
private static int findPriorityForClass(@Nullable String className) {
    for (int i = 0; i < APC_PRIORITY_LIST.size(); i++) {
        Class<?> clazz = APC_PRIORITY_LIST.get(i);
        if (clazz.getName().equals(className)) {
            return i;
        }
    }
    throw new IllegalArgumentException(
            "Class name [" + className + "] is not a known auto-proxy creator class");
}
```



### 1.2 代理类的整体创建流程

AnnotationAwareAspectJAutoProxyCreator属于BeanPostProcessor，继承AbstractAutoProxyCreator，AbstractAutoProxyCreator重写了bean后处理器的postProcessAfterInitialization方法，返回代理对象。(postProcessBeforeInstantiation方法不用关注，创建逻辑不在这里)

![image-20240515180938088](https://lu-note.oss-cn-shenzhen.aliyuncs.com/notes/work/image-20240515180938088.png)

![image-20240515181143931](https://lu-note.oss-cn-shenzhen.aliyuncs.com/notes/work/image-20240515181143931.png)



```java
private final Set<String> targetSourcedBeans = Collections.newSetFromMap(new ConcurrentHashMap<>(16));
private final Map<Object, Boolean> advisedBeans = new ConcurrentHashMap<>(256);

/**
 * Wrap the given bean if necessary, i.e. if it is eligible for being proxied.
 * @param bean the raw bean instance
 * @param beanName the name of the bean
 * @param cacheKey the cache key for metadata access
 * @return a proxy wrapping the bean, or the raw bean instance as-is
 */
protected Object wrapIfNecessary(Object bean, String beanName, Object cacheKey) {
    // 1、判断是否需要代理
    // bean名称为空，targetSourcedBeans中存在，则不用代理
    // 存在于targetSourcedBeans中，说明在postProcessBeforeInstantiation方法已经被代理，默认情况下是不包含的
    if (StringUtils.hasLength(beanName) && this.targetSourcedBeans.contains(beanName)) {
        return bean;
    }
    // 判断是否缓存为不用代理，如果是，直接返回当前bean
    if (Boolean.FALSE.equals(this.advisedBeans.get(cacheKey))) {
        return bean;
    }
    // 如果是基础设施的类，或者是类名以.ORIGINAL结尾的类，则不用代理
    /**
    * 什么是基础设施的类呢？
    * （1）Advice接口及其实现类
    * （2）Pointcut接口及其实现类
    * （3）Advisor接口及其实现类
    * （3）AopInfrastructureBean接口及其实现类
    */
    if (isInfrastructureClass(bean.getClass()) || shouldSkip(bean.getClass(), beanName)) {
        this.advisedBeans.put(cacheKey, Boolean.FALSE);
        return bean;
    }

    // Create proxy if we have advice.
    // 2、获取目标增强器（获取该bean所有的通知）
    Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(bean.getClass(), beanName, null);
    if (specificInterceptors != DO_NOT_PROXY) {
        // 添加缓存，表示已经代理过
        this.advisedBeans.put(cacheKey, Boolean.TRUE);
        // 3、创建动态代理
        Object proxy = createProxy(
                bean.getClass(), beanName, specificInterceptors, new SingletonTargetSource(bean));
        this.proxyTypes.put(cacheKey, proxy.getClass());
        return proxy;
    }
    
    // 添加缓存，表示不用代理
    this.advisedBeans.put(cacheKey, Boolean.FALSE);
    return bean;
}


/**
 * Return whether the given bean class represents an infrastructure class
 * that should never be proxied.
 * <p>The default implementation considers Advices, Advisors and
 * AopInfrastructureBeans as infrastructure classes.
 * @param beanClass the class of the bean
 * @return whether the bean represents an infrastructure class
 * @see org.aopalliance.aop.Advice
 * @see org.springframework.aop.Advisor
 * @see org.springframework.aop.framework.AopInfrastructureBean
 * @see #shouldSkip
 */
protected boolean isInfrastructureClass(Class<?> beanClass) {
    boolean retVal = Advice.class.isAssignableFrom(beanClass) ||
            Pointcut.class.isAssignableFrom(beanClass) ||
            Advisor.class.isAssignableFrom(beanClass) ||
            AopInfrastructureBean.class.isAssignableFrom(beanClass);
    if (retVal && logger.isTraceEnabled()) {
        logger.trace("Did not attempt to auto-proxy infrastructure class [" + beanClass.getName() + "]");
    }
    return retVal;
}
```



* AbstractAdvisorAutoProxyCreator

```java
@Override
@Nullable
protected Object[] getAdvicesAndAdvisorsForBean(
        Class<?> beanClass, String beanName, @Nullable TargetSource targetSource) {
    // 获取bean的所有符合条件增强器（即获取目标增强器）
    List<Advisor> advisors = findEligibleAdvisors(beanClass, beanName);
    if (advisors.isEmpty()) {
        // 所有符合条件增强器不存在，返回null
        return DO_NOT_PROXY;
    }
    // 返回符合条件增强器
    return advisors.toArray();
}

/**
 * Find all eligible Advisors for auto-proxying this class.
 * @param beanClass the clazz to find advisors for
 * @param beanName the name of the currently proxied bean
 * @return the empty List, not {@code null},
 * if there are no pointcuts or interceptors
 * @see #findCandidateAdvisors
 * @see #sortAdvisors
 * @see #extendAdvisors
 */
protected List<Advisor> findEligibleAdvisors(Class<?> beanClass, String beanName) {
    // 1、获取所有候选的增强器
    List<Advisor> candidateAdvisors = findCandidateAdvisors();
    // 2、筛选出符合条件的增强器
    // 说白了就是判断该bean是否符合对应增强器的切入点表达式，如果符合，增强器就是符合条件的
    List<Advisor> eligibleAdvisors = findAdvisorsThatCanApply(candidateAdvisors, beanClass, beanName);
    // 3、扩展增强器
    extendAdvisors(eligibleAdvisors);
    if (!eligibleAdvisors.isEmpty()) {
        // 4、排序增强器
        eligibleAdvisors = sortAdvisors(eligibleAdvisors);
    }
    // 5、 返回符合条件的增强器
    return eligibleAdvisors;
}
```

所以我们**只需要重点关注（1）获取目标增强器findEligibleAdvisors方法和（2）创建动态代理createProxy(bean.getClass(), beanName, specificInterceptors, new SingletonTargetSource(bean));方法即可**。



### 1.3 总结

```
@EnableCaching、@EnableTransactionManagement先引入InfrastructureAdvisorAutoProxyCreator，springboot自动配置类CglibAutoProxyConfiguration最终升级为AnnotationAwareAspectJAutoProxyCreator。在bean的创建过程中，AnnotationAwareAspectJAutoProxyCreator拦截bean的创建，获取通知，创建动态代理对象。
```





## 2、获取目标增强器

### 2.1获取所有候选的增强器

AbstractAdvisorAutoProxyCreator的findCandidateAdvisors方法

```java
@Override
protected List<Advisor> findCandidateAdvisors() {
    // 1、获取spring中所有Advisor类型的Bean
    // Add all the Spring advisors found according to superclass rules.
    List<Advisor> advisors = super.findCandidateAdvisors();
    // 2、 获取标注@AspectJ注解的bean，为每个AspectJ通知方法创建一个Spring Advisor
    // Build Advisors for all AspectJ aspects in the bean factory.
    if (this.aspectJAdvisorsBuilder != null) {
        advisors.addAll(this.aspectJAdvisorsBuilder.buildAspectJAdvisors());
    }
    return advisors;
}
```



#### 2.1.1 获取spring中所有Advisor类型的Bean

```java
/**
 * Find all candidate Advisors to use in auto-proxying.
 * @return the List of candidate Advisors
 */
protected List<Advisor> findCandidateAdvisors() {
    Assert.state(this.advisorRetrievalHelper != null, "No BeanFactoryAdvisorRetrievalHelper available");
    // BeanFactoryAdvisorRetrievalHelperAdapter
    return this.advisorRetrievalHelper.findAdvisorBeans();
}
```



```java
/**
Find all eligible Advisor beans in the current bean factory, ignoring FactoryBeans and excluding beans that are currently in creation.
查找当前bean工厂中所有符合条件的Advisor bean，忽略FactoryBeans并排除当前正在创建的bean。
*/
public List<Advisor> findAdvisorBeans() {
		// Determine list of advisor bean names, if not cached already.
		String[] advisorNames = this.cachedAdvisorBeanNames;
		// 1、查找Advisor类型的bean名称    	
		if (advisorNames == null) {
             // 一开始没有缓存，执行到这里
             // 查找Advisor类型的bean名称，不包含FactoryBean，以及不在这里初始化bean，同时指定为缓存       
            /**
            这样才是包含查询FactoryBean的写法
            // 查找FactoryBean<Advisor>类型的bean名称
            String[] factoryBeanNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
                this.beanFactory, ResolvableType.forClassWithGenerics(FactoryBean.class, Advisor.class).resolve(), true, false);
            for (String name : factoryBeanNames) {
                System.out.println("FactoryBean<Advisor> bean name: " + name);
            }
            */             
			// Do not initialize FactoryBeans here: We need to leave all regular beans
			// uninitialized to let the auto-proxy creator apply to them!
			advisorNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
					this.beanFactory, Advisor.class, true, false);
			this.cachedAdvisorBeanNames = advisorNames;
		}
         // 没有Advisor类型的bean名称，返回空集合   
		if (advisorNames.length == 0) {
			return new ArrayList<>();
		}

         // 2、遍历Advisor类型的bean名称.如果bean名称是符合以下两个条件
         // (1) bean名称是符合条件的.见isEligibleBean(name)方法
         // (2) 当前bean名称对应的bean没有在创建中
         // 则获取bean名称对应的Advisor类型的bean，保存然后返回.
		List<Advisor> advisors = new ArrayList<>();
		for (String name : advisorNames) {
			if (isEligibleBean(name)) {
				if (this.beanFactory.isCurrentlyInCreation(name)) {
					if (logger.isTraceEnabled()) {
						logger.trace("Skipping currently created advisor '" + name + "'");
					}
				}
				else {
					try {
						// 保存bean名称对应的Advisor类型的bean
						advisors.add(this.beanFactory.getBean(name, Advisor.class));
					}
					catch (BeanCreationException ex) {
						Throwable rootCause = ex.getMostSpecificCause();
						if (rootCause instanceof BeanCurrentlyInCreationException) {
							BeanCreationException bce = (BeanCreationException) rootCause;
							String bceBeanName = bce.getBeanName();
							if (bceBeanName != null && this.beanFactory.isCurrentlyInCreation(bceBeanName)) {
								if (logger.isTraceEnabled()) {
									logger.trace("Skipping advisor '" + name +
											"' with dependency on currently created bean: " + ex.getMessage());
								}
								// Ignore: indicates a reference back to the bean we're trying to advise.
								// We want to find advisors other than the currently created bean itself.
								continue;
							}
						}
						throw ex;
					}
				}
			}
		}
         // 返回所有获取到的Advisor
		return advisors;
	}
```



* isEligibleBean(name)方法

```java
protected boolean isEligibleBean(String beanName) {
    return AbstractAdvisorAutoProxyCreator.this.isEligibleAdvisorBean(beanName);
}
/**
 * Return whether the Advisor bean with the given name is eligible
 * for proxying in the first place.
 * @param beanName the name of the Advisor bean
 * @return whether the bean is eligible
 */
protected boolean isEligibleAdvisorBean(String beanName) {
    return true;
}
```

默认返回true，表示符合条件。



【注意】本示例中事务增强器、缓存增强器分别如下

事务的增强器：BeanFactoryTransactionAttributeSourceAdvisor

缓存的增强器：BeanFactoryCacheOperationSourceAdvisor



#### 2.1.2 获取标注@AspectJ注解的bean，为每个AspectJ通知方法创建一个Spring Advisor

* BeanFactoryAspectJAdvisorsBuilderAdapter

```java
 /**
 * 在当前bean工厂中查找带有@AspectJ注解的切面bean，并返回到表示它们的Spring AOP advisor列表。
 * 为每个AspectJ通知方法创建一个Spring Advisor。 
 * Look for AspectJ-annotated aspect beans in the current bean factory,
 * and return to a list of Spring AOP Advisors representing them.
 * <p>Creates a Spring Advisor for each AspectJ advice method.
 * @return the list of {@link org.springframework.aop.Advisor} beans
 * @see #isEligibleBean
 */
public List<Advisor> buildAspectJAdvisors() {
    // 1、获取带有@AspectJ注解的切面bean名称
    List<String> aspectNames = this.aspectBeanNames;
    // 如果带有@AspectJ注解的切面bean名称不等于null，说明有缓存。如果为空，表示第一次加载
    if (aspectNames == null) {
        synchronized (this) {
            aspectNames = this.aspectBeanNames;
            if (aspectNames == null) {
                List<Advisor> advisors = new ArrayList<>();
                aspectNames = new ArrayList<>();              
                /**
                  2、获取bean工厂中所有的bean名称，如果bean名称满足以下条件
                  （1）bean名称是符合条件的。判断是否符合条件见isEligibleBean(beanName)
                  （2）bean名称对应的Class上有@Aspect注解。
                   则为每个AspectJ通知方法创建一个Spring Advisor，并添加到缓存中
                */
                String[] beanNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
                        this.beanFactory, Object.class, true, false);
                for (String beanName : beanNames) {
                    // （1）判断bean名称是否符合条件
                    if (!isEligibleBean(beanName)) {
                        continue;
                    }
                    // We must be careful not to instantiate beans eagerly as in this case they
                    // would be cached by the Spring container but would not have been weaved.
                    Class<?> beanType = this.beanFactory.getType(beanName, false);
                    if (beanType == null) {
                        continue;
                    }
                    // (2)bean名称对应的Class上是否有@Aspect注解
                    if (this.advisorFactory.isAspect(beanType)) {
                        // 有@Aspect注解，才执行到这里
                        aspectNames.add(beanName);                       
                        // 3、创建@AspectJ注解元数据AspectMetadata
                        // Create a new AspectMetadata instance for the given aspect class.
                        // 创建的AspectMetadata默认情况获取到的是PerClauseKind.SINGLETON。进入if
                        AspectMetadata amd = new AspectMetadata(beanType, beanName);                   
                        if (amd.getAjType().getPerClause().getKind() == PerClauseKind.SINGLETON) {
                            // 4、为每个AspectJ通知方法创建一个Spring Advisor，并添加到缓存中
                            // BeanFactoryAspectInstanceFactory中指定了aspectName=beanName                           
                            // 创建Aspect实例化工厂
                            MetadataAwareAspectInstanceFactory factory =
                                    new BeanFactoryAspectInstanceFactory(this.beanFactory, beanName);
                            // 通过增强器工厂(ReflectiveAspectJAdvisorFactory)创建增强器
                            List<Advisor> classAdvisors = this.advisorFactory.getAdvisors(factory);
                            //如果beanName对应的bean是单例的，则缓存创建好的所有增强器。否则缓存创建的工厂
                            if (this.beanFactory.isSingleton(beanName)) {                               
                                this.advisorsCache.put(beanName, classAdvisors);
                            }
                            else {                               
                                this.aspectFactoryCache.put(beanName, factory);
                            }
                            // 保存创建好的所有增强器
                            advisors.addAll(classAdvisors);
                        }
                        else {                         
                            // Per target or per this.
                            if (this.beanFactory.isSingleton(beanName)) {
                                throw new IllegalArgumentException("Bean with name '" + beanName +
                                        "' is a singleton, but aspect instantiation model is not singleton");
                            }
                            // 不是创建单实例的增强器，则缓存增强器创建工厂
                            MetadataAwareAspectInstanceFactory factory =
                                    new PrototypeAspectInstanceFactory(this.beanFactory, beanName);
                            this.aspectFactoryCache.put(beanName, factory);
                            // 通过增强器工厂创建增强器
                            advisors.addAll(this.advisorFactory.getAdvisors(factory));
                        }
                    }
                }
                this.aspectBeanNames = aspectNames;
                return advisors;
            }
        }
    }
    // 带有@AspectJ注解的切面bean名称为空，则返回空集合
    if (aspectNames.isEmpty()) {
        return Collections.emptyList();
    }
    // 从缓存获取。如果缓存的Advisor为null，重新使用切面工厂创建。
    List<Advisor> advisors = new ArrayList<>();
    for (String aspectName : aspectNames) {
        List<Advisor> cachedAdvisors = this.advisorsCache.get(aspectName);
        if (cachedAdvisors != null) {  		
            advisors.addAll(cachedAdvisors);
        }
        else {
            MetadataAwareAspectInstanceFactory factory = this.aspectFactoryCache.get(aspectName);
            advisors.addAll(this.advisorFactory.getAdvisors(factory));
        }
    }
    // 返回最终构建好的所有Advisor
    return advisors;
}  
```



##### （1）判断bean名称是否符合条件

```java
protected boolean isEligibleBean(String beanName) {
    return AnnotationAwareAspectJAutoProxyCreator.this.isEligibleAspectBean(beanName);
}

/**
 * Check whether the given aspect bean is eligible for auto-proxying.
 * <p>If no &lt;aop:include&gt; elements were used then "includePatterns" will be
 * {@code null} and all beans are included. If "includePatterns" is non-null,
 * then one of the patterns must match.
 */
protected boolean isEligibleAspectBean(String beanName) {
    if (this.includePatterns == null) {
        return true;
    }
    else {
        for (Pattern pattern : this.includePatterns) {
            if (pattern.matcher(beanName).matches()) {
                return true;
            }
        }
        return false;
    }
}

@Nullable
private List<Pattern> includePatterns;

/**
 * Set a list of regex patterns, matching eligible @AspectJ bean names.
 * <p>Default is to consider all @AspectJ beans as eligible.
 */
public void setIncludePatterns(List<String> patterns) {
    this.includePatterns = new ArrayList<>(patterns.size());
    for (String patternText : patterns) {
        this.includePatterns.add(Pattern.compile(patternText));
    }
}
```

检查给定的bean名称是否符合正则表达式，如果有一个正则表达式匹配，则符合条件。

默认情况下AnnotationAwareAspectJAutoProxyCreator是没有指定正则表达式来判断类名的是否符合条件。



##### （2）bean名称对应的Class上是否有@Aspect注解

```java
/**
 * We consider something to be an AspectJ aspect suitable for use by the Spring AOP system
 * if it has the @Aspect annotation, and was not compiled by ajc. The reason for this latter test
 * is that aspects written in the code-style (AspectJ language) also have the annotation present
 * when compiled by ajc with the -1.5 flag, yet they cannot be consumed by Spring AOP.
 * 我们认为一个AspectJ方面适合Spring AOP系统使用，如果它有@Aspect注释，并且没有被ajc编译。
 * 后面这个测试的原因是，用代码风格(AspectJ语言)编写的方面在ajc用-1.5标志编译时也有注释，但是它们不能被Spring AOP使用。
 */
@Override
public boolean isAspect(Class<?> clazz) {
    // （1）类上是否有@Aspect注解
    // （2）是否被ajc编译。默认情况下是没有的，则!compiledByAjc(clazz)=true。
    // 通常情况下只需要看第一点即可
    return (hasAspectAnnotation(clazz) && !compiledByAjc(clazz));
}

private boolean hasAspectAnnotation(Class<?> clazz) {
    return (AnnotationUtils.findAnnotation(clazz, Aspect.class) != null);
}

/**
 * We need to detect this as "code-style" AspectJ aspects should not be
 * interpreted by Spring AOP.
 */
private boolean compiledByAjc(Class<?> clazz) {
    // The AJTypeSystem goes to great lengths to provide a uniform appearance between code-style and
    // annotation-style aspects. Therefore there is no 'clean' way to tell them apart. Here we rely on
    // an implementation detail of the AspectJ compiler.
    for (Field field : clazz.getDeclaredFields()) {
        if (field.getName().startsWith(AJC_MAGIC)) {
            return true;
        }
    }
    return false;
}
```



##### （3）创建@AspectJ注解元数据AspectMetadata

```java
/**
 * Create a new AspectMetadata instance for the given aspect class.
 * @param aspectClass the aspect class
 * @param aspectName the name of the aspect
 */
public AspectMetadata(Class<?> aspectClass, String aspectName) {
    this.aspectName = aspectName;

    Class<?> currClass = aspectClass;
    AjType<?> ajType = null;
    while (currClass != Object.class) {
        // 获取AjTypeImpl
        AjType<?> ajTypeToCheck = AjTypeSystem.getAjType(currClass);
        // 判断是否是AspectJ(判断aspectClass是否有@AspectJ注解)，跳出循环
        if (ajTypeToCheck.isAspect()) {
            ajType = ajTypeToCheck;
            break;
        }
        currClass = currClass.getSuperclass();
    }
    if (ajType == null) {
        throw new IllegalArgumentException("Class '" + aspectClass.getName() + "' is not an @AspectJ aspect");
    }
    if (ajType.getDeclarePrecedence().length > 0) {
        throw new IllegalArgumentException("DeclarePrecedence not presently supported in Spring AOP");
    }
    this.aspectClass = ajType.getJavaClass();
    this.ajType = ajType;
    switch (this.ajType.getPerClause().getKind()) {
        case SINGLETON:
            // 执行到这里
            this.perClausePointcut = Pointcut.TRUE;
            return;
        case PERTARGET:
        case PERTHIS:
            AspectJExpressionPointcut ajexp = new AspectJExpressionPointcut();
            ajexp.setLocation(aspectClass.getName());
            ajexp.setExpression(findPerClause(aspectClass));
            ajexp.setPointcutDeclarationScope(aspectClass);
            this.perClausePointcut = ajexp;
            return;
        case PERTYPEWITHIN:
            // Works with a type pattern
            this.perClausePointcut = new ComposablePointcut(new TypePatternClassFilter(findPerClause(aspectClass)));
            return;
        default:
            throw new AopConfigException(
                    "PerClause " + ajType.getPerClause().getKind() + " not supported by Spring AOP for " + aspectClass);
    }
}

// 判断是否是AspectJ(判断aspectClass是否有@AspectJ注解)
public boolean isAspect() {
    return clazz.getAnnotation(Aspect.class) != null;
}

public PerClause getPerClause() {
         // 判断是否是AspectJ(判断aspectClass是否有@AspectJ注解)
		if (isAspect()) {
             // 获取@AspectJ注解
			Aspect aspectAnn = clazz.getAnnotation(Aspect.class);
             // 获取@AspectJ注解的value属性
			String perClause = aspectAnn.value();
			if (perClause.equals("")) {
                 // 如果value属性为空串， 判断父类是否是AspectJ(判断父类是否有@AspectJ注解)
				if (getSupertype().isAspect()) {
                      // 如果父类有@AspectJ注解，通过父类的@AspectJ注解获取PerClause
					return getSupertype().getPerClause();
				}
                  // 如果父类没有@AspectJ注解，则创建PerClauseImpl，设置kind=PerClauseKind.SINGLETON，即单实例代理
				return new PerClauseImpl(PerClauseKind.SINGLETON);
			} else if (perClause.startsWith("perthis(")) {
				return new PointcutBasedPerClauseImpl(PerClauseKind.PERTHIS,perClause.substring("perthis(".length(),perClause.length() - 1));
			} else if (perClause.startsWith("pertarget(")) {
				return new PointcutBasedPerClauseImpl(PerClauseKind.PERTARGET,perClause.substring("pertarget(".length(),perClause.length() - 1));
			} else if (perClause.startsWith("percflow(")) {
				return new PointcutBasedPerClauseImpl(PerClauseKind.PERCFLOW,perClause.substring("percflow(".length(),perClause.length() - 1));
			} else if (perClause.startsWith("percflowbelow(")) {
				return new PointcutBasedPerClauseImpl(PerClauseKind.PERCFLOWBELOW,perClause.substring("percflowbelow(".length(),perClause.length() - 1));
			} else if (perClause.startsWith("pertypewithin")) {
				return new TypePatternBasedPerClauseImpl(PerClauseKind.PERTYPEWITHIN,perClause.substring("pertypewithin(".length(),perClause.length() - 1));
			} else {
				throw new IllegalStateException("Per-clause not recognized: " + perClause);
			}
		} else {
			return null;
		}
	}

/* (non-Javadoc)
 * @see org.aspectj.lang.reflect.AjType#getSupertype()
 */
public AjType<? super T> getSupertype() {
    Class<? super T> superclass = clazz.getSuperclass();
    return superclass==null ? null : (AjType<? super T>) new AjTypeImpl(superclass);
}
```



```java
public class AjTypeSystem {

		private static Map<Class, WeakReference<AjType>> ajTypes =
			Collections.synchronizedMap(new WeakHashMap<>());

		/**
		 * Return the AspectJ runtime type representation of the given Java type.
		 * Unlike java.lang.Class, AjType understands pointcuts, advice, declare statements,
		 * and other AspectJ type members. AjType is the recommended reflection API for
		 * AspectJ programs as it offers everything that java.lang.reflect does, with
		 * AspectJ-awareness on top.
		 * @param <T> the expected type associated with the returned AjType
		 * @param fromClass the class for which to discover the AjType
		 * @return the AjType corresponding to the input class
		 */
		public static <T> AjType<T> getAjType(Class<T> fromClass) {
			WeakReference<AjType> weakRefToAjType =  ajTypes.get(fromClass);
			if (weakRefToAjType!=null) {
				AjType<T> theAjType = weakRefToAjType.get();
				if (theAjType != null) {
					return theAjType;
				} else {
					theAjType = new AjTypeImpl<>(fromClass);
					ajTypes.put(fromClass, new WeakReference<>(theAjType));
					return theAjType;
				}
			}
             // 一开始没有缓存，返回AjTypeImpl
			// neither key nor value was found
			AjType<T> theAjType = new AjTypeImpl<>(fromClass);
			ajTypes.put(fromClass, new WeakReference<>(theAjType));
			return theAjType;
		}
}
```





##### （4）为每个AspectJ通知方法创建一个Spring Advisor，并添加到缓存中

```java
// 创建Aspect实例化工厂
MetadataAwareAspectInstanceFactory factory = new BeanFactoryAspectInstanceFactory(this.beanFactory, beanName);
// 通过增强器工厂创建增强器
List<Advisor> classAdvisors = this.advisorFactory.getAdvisors(factory);
```

```java
// ReflectiveAspectJAdvisorFactory
@Override
public List<Advisor> getAdvisors(MetadataAwareAspectInstanceFactory aspectInstanceFactory) {
    // 获取@Aspect注解所在的类
    Class<?> aspectClass = aspectInstanceFactory.getAspectMetadata().getAspectClass();
    // 获取@Aspect注解所在类的类名
    String aspectName = aspectInstanceFactory.getAspectMetadata().getAspectName();
    // 1、校验@Aspect注解所在的类
    validate(aspectClass);

    // We need to wrap the MetadataAwareAspectInstanceFactory with a decorator
    // so that it will only instantiate once.
    // 包装Aspect实例化工厂
    MetadataAwareAspectInstanceFactory lazySingletonAspectInstanceFactory =
            new LazySingletonAspectInstanceFactoryDecorator(aspectInstanceFactory);

    List<Advisor> advisors = new ArrayList<>();
    // 2、获取所有的增强器方法。【注意】这里获取的增强器方法是排序好的了
    for (Method method : getAdvisorMethods(aspectClass)) {
        // Prior to Spring Framework 5.2.7, advisors.size() was supplied as the declarationOrderInAspect
        // to getAdvisor(...) to represent the "current position" in the declared methods list.
        // However, since Java 7 the "current position" is not valid since the JDK no longer
        // returns declared methods in the order in which they are declared in the source code.
        // Thus, we now hard code the declarationOrderInAspect to 0 for all advice methods
        // discovered via reflection in order to support reliable advice ordering across JVM launches.
        // Specifically, a value of 0 aligns with the default value used in
        // AspectJPrecedenceComparator.getAspectDeclarationOrder(Advisor).
        // 3、创建增强器
        Advisor advisor = getAdvisor(method, lazySingletonAspectInstanceFactory, 0, aspectName);
        if (advisor != null) {
            advisors.add(advisor);
        }
    }

    // If it's a per target aspect, emit the dummy instantiating aspect.
    if (!advisors.isEmpty() && lazySingletonAspectInstanceFactory.getAspectMetadata().isLazilyInstantiated()) {
        Advisor instantiationAdvisor = new SyntheticInstantiationAdvisor(lazySingletonAspectInstanceFactory);
        advisors.add(0, instantiationAdvisor);
    }
    
    // 根据属性上的@DeclareParents创建增强器DeclareParentsAdvisor。
    // DeclareParentsAdvisor实现IntroductionAdvisor接口
    // Find introduction fields.
    for (Field field : aspectClass.getDeclaredFields()) {
        Advisor advisor = getDeclareParentsAdvisor(field);
        if (advisor != null) {
            advisors.add(advisor);
        }
    }

    return advisors;
}
```



* 校验@Aspect注解所在的类

```java
@Override
public void validate(Class<?> aspectClass) throws AopConfigException {
   // 如果父类有@Aspect注解，而且不是抽象类，就报错 
   // If the parent has the annotation and isn't abstract it's an error
   Class<?> superclass = aspectClass.getSuperclass();
   if (superclass.getAnnotation(Aspect.class) != null &&
         !Modifier.isAbstract(superclass.getModifiers())) {
      throw new AopConfigException("[" + aspectClass.getName() + "] cannot extend concrete aspect [" +
            superclass.getName() + "]");
   }

   AjType<?> ajType = AjTypeSystem.getAjType(aspectClass);
   // 如果不是Aspect类型，就报错
   if (!ajType.isAspect()) {
      throw new NotAnAtAspectException(aspectClass);
   }
   if (ajType.getPerClause().getKind() == PerClauseKind.PERCFLOW) {
      throw new AopConfigException(aspectClass.getName() + " uses percflow instantiation model: " +
            "This is not supported in Spring AOP.");
   }
   if (ajType.getPerClause().getKind() == PerClauseKind.PERCFLOWBELOW) {
      throw new AopConfigException(aspectClass.getName() + " uses percflowbelow instantiation model: " +
            "This is not supported in Spring AOP.");
   }
}
```



* 获取所有的增强器方法并排序

```java
private List<Method> getAdvisorMethods(Class<?> aspectClass) {
    List<Method> methods = new ArrayList<>();
    // 1、除了方法上有@Pointcut注解的，获取类的所有方法作为通知方法（包括父类里的）
    ReflectionUtils.doWithMethods(aspectClass, methods::add, adviceMethodFilter);
    if (methods.size() > 1) {
       // 2、使用adviceMethodComparator对通知方法排序
       // 先按照@Around -> @Before -> @After -> @AfterReturning -> @AfterThrowing的顺序排序，顺序一样再按照方法名称升序
        methods.sort(adviceMethodComparator);
    }
    return methods;
}


// Exclude @Pointcut methods
private static final MethodFilter adviceMethodFilter = ReflectionUtils.USER_DECLARED_METHODS
        .and(method -> (AnnotationUtils.getAnnotation(method, Pointcut.class) == null));

private static final Comparator<Method> adviceMethodComparator;

static {
    // 【重点】这个是执行顺序和排序顺序的区别了
    //注意:虽然@After排在@AfterReturning和@AfterThrowing之前，
    //但是一个@After通知方法实际上会在@AfterReturning 和 @AfterThrowing方法 之后调用
	// 因为AspectJAfterAdvice.invoke(MethodInvocation)
	//在' try '块中调用proceed()，并且只调用@After通知方法在相应的' finally '块中。
    /**
      说白了就是AspectJAfterAdvice的invoke就是大概如下，然后排序顺序和执行顺序不一样了  
      try{
         // @AfterReturning 或 @AfterThrowing通知方法
     	 proceed();
      } finally { 
        // @After通知方法
        ...
      }    
    **/
    // Note: although @After is ordered before @AfterReturning and @AfterThrowing,
    // an @After advice method will actually be invoked after @AfterReturning and
    // @AfterThrowing methods due to the fact that AspectJAfterAdvice.invoke(MethodInvocation)
    // invokes proceed() in a `try` block and only invokes the @After advice method
    // in a corresponding `finally` block.
    Comparator<Method> adviceKindComparator = new ConvertingComparator<>(
            new InstanceComparator<>(
                    Around.class, Before.class, After.class, AfterReturning.class, AfterThrowing.class),
            (Converter<Method, Annotation>) method -> {
                AspectJAnnotation<?> ann = AbstractAspectJAdvisorFactory.findAspectJAnnotationOnMethod(method);
                return (ann != null ? ann.getAnnotation() : null);
            });
    Comparator<Method> methodNameComparator = new ConvertingComparator<>(Method::getName);
    // 先按照@Around -> @Before -> @After -> @AfterReturning -> @AfterThrowing的顺序排序，顺序一样再按照方法名称升序
    adviceMethodComparator = adviceKindComparator.thenComparing(methodNameComparator);
}


/**
 * Perform the given callback operation on all matching methods of the given
 * class and superclasses (or given interface and super-interfaces).
 * <p>The same named method occurring on subclass and superclass will appear
 * twice, unless excluded by the specified {@link MethodFilter}.
 * @param clazz the class to introspect
 * @param mc the callback to invoke for each method
 * @param mf the filter that determines the methods to apply the callback to
 * @throws IllegalStateException if introspection fails
 */
public static void doWithMethods(Class<?> clazz, MethodCallback mc, @Nullable MethodFilter mf) {
    // Keep backing up the inheritance hierarchy.
    Method[] methods = getDeclaredMethods(clazz, false);
    for (Method method : methods) {
        if (mf != null && !mf.matches(method)) {
            continue;
        }
        try {
            mc.doWith(method);
        }
        catch (IllegalAccessException ex) {
            throw new IllegalStateException("Not allowed to access method '" + method.getName() + "': " + ex);
        }
    }
    if (clazz.getSuperclass() != null && (mf != USER_DECLARED_METHODS || clazz.getSuperclass() != Object.class)) {
        doWithMethods(clazz.getSuperclass(), mc, mf);
    }
    else if (clazz.isInterface()) {
        for (Class<?> superIfc : clazz.getInterfaces()) {
            doWithMethods(superIfc, mc, mf);
        }
    }
}
```



* 创建增强器

```java
Advisor advisor = getAdvisor(method, lazySingletonAspectInstanceFactory, 0, aspectName);

ReflectiveAspectJAdvisorFactory
AspectJExpressionPointcut
InstantiationModelAwarePointcutAdvisor
```



```java
@Override
@Nullable
public Advisor getAdvisor(Method candidateAdviceMethod, MetadataAwareAspectInstanceFactory aspectInstanceFactory,
        int declarationOrderInAspect, String aspectName) {
    //（1）校验@Aspect注解所在的类。同上
    validate(aspectInstanceFactory.getAspectMetadata().getAspectClass());

    // （2）创建切入点表达式
    AspectJExpressionPointcut expressionPointcut = getPointcut(
            candidateAdviceMethod, aspectInstanceFactory.getAspectMetadata().getAspectClass());
    if (expressionPointcut == null) {
        // 切入点表达式不存在，直接返回null，不用创建增强器
        return null;
    }
    
    // (3) 创建AspectJ切入点增强器
    // 每个目标方法将有一个此advisor的实例。
    return new InstantiationModelAwarePointcutAdvisorImpl(expressionPointcut, candidateAdviceMethod,
            this, aspectInstanceFactory, declarationOrderInAspect, aspectName);
}
```



###### （4-1）创建切入点表达式

```java
@Nullable
private AspectJExpressionPointcut getPointcut(Method candidateAdviceMethod, Class<?> candidateAspectClass) {
    // 获取方法上相关的AspectJ注解。
    AspectJAnnotation<?> aspectJAnnotation =
            AbstractAspectJAdvisorFactory.findAspectJAnnotationOnMethod(candidateAdviceMethod);
    if (aspectJAnnotation == null) {
        return null;
    }
	
    // 创建AspectJExpressionPointcut，设置切入点表达式
    AspectJExpressionPointcut ajexp =
            new AspectJExpressionPointcut(candidateAspectClass, new String[0], new Class<?>[0]);
    ajexp.setExpression(aspectJAnnotation.getPointcutExpression());
    if (this.beanFactory != null) {
        ajexp.setBeanFactory(this.beanFactory);
    }
    return ajexp;
}

/**
 * Find and return the first AspectJ annotation on the given method
 * (there <i>should</i> only be one anyway...).
 */
@SuppressWarnings("unchecked")
@Nullable
protected static AspectJAnnotation<?> findAspectJAnnotationOnMethod(Method method) {    
    // @Pointcut、@Around、@Before、@After、@AfterReturning、@AfterThrowing
    for (Class<?> clazz : ASPECTJ_ANNOTATION_CLASSES) {
        // 获取方法上相应的注解
        AspectJAnnotation<?> foundAnnotation = findAnnotation(method, (Class<Annotation>) clazz);
        if (foundAnnotation != null) {
            return foundAnnotation;
        }
    }
    return null;
}
private static final Class<?>[] ASPECTJ_ANNOTATION_CLASSES = new Class<?>[] {
			Pointcut.class, Around.class, Before.class, After.class, AfterReturning.class, AfterThrowing.class};


@Nullable
private static <A extends Annotation> AspectJAnnotation<A> findAnnotation(Method method, Class<A> toLookFor) {  
    A result = AnnotationUtils.findAnnotation(method, toLookFor);
    if (result != null) {
        // 解析result，创建AspectJAnnotation
        return new AspectJAnnotation<>(result);
    }
    else {
        return null;
    }
}
```



* 创建AspectJAnnotation

```java
protected static class AspectJAnnotation<A extends Annotation> {

		private static final String[] EXPRESSION_ATTRIBUTES = new String[] {"pointcut", "value"};

		private static Map<Class<?>, AspectJAnnotationType> annotationTypeMap = new HashMap<>(8);

		static {
			annotationTypeMap.put(Pointcut.class, AspectJAnnotationType.AtPointcut);
			annotationTypeMap.put(Around.class, AspectJAnnotationType.AtAround);
			annotationTypeMap.put(Before.class, AspectJAnnotationType.AtBefore);
			annotationTypeMap.put(After.class, AspectJAnnotationType.AtAfter);
			annotationTypeMap.put(AfterReturning.class, AspectJAnnotationType.AtAfterReturning);
			annotationTypeMap.put(AfterThrowing.class, AspectJAnnotationType.AtAfterThrowing);
		}

		private final A annotation;

		private final AspectJAnnotationType annotationType;

		private final String pointcutExpression;

		private final String argumentNames;

		public AspectJAnnotation(A annotation) {
             // 设置注解
			this.annotation = annotation;
             // 设置注解的类型。从annotationTypeMap中获取
			this.annotationType = determineAnnotationType(annotation);
			try {
                  // 获取注解的pointcut或者value属性作为切入点表达式
				this.pointcutExpression = resolveExpression(annotation);
                  // 获取注解的argNames属性，设置为argumentNames
				Object argNames = AnnotationUtils.getValue(annotation, "argNames");
				this.argumentNames = (argNames instanceof String ? (String) argNames : "");
			}
			catch (Exception ex) {
				throw new IllegalArgumentException(annotation + " is not a valid AspectJ annotation", ex);
			}
		}

		private AspectJAnnotationType determineAnnotationType(A annotation) {
			AspectJAnnotationType type = annotationTypeMap.get(annotation.annotationType());
			if (type != null) {
				return type;
			}
			throw new IllegalStateException("Unknown annotation type: " + annotation);
		}

		private String resolveExpression(A annotation) {
			for (String attributeName : EXPRESSION_ATTRIBUTES) {
				Object val = AnnotationUtils.getValue(annotation, attributeName);
				if (val instanceof String) {
					String str = (String) val;
					if (!str.isEmpty()) {
						return str;
					}
				}
			}
			throw new IllegalStateException("Failed to resolve expression: " + annotation);
		}
	}
```



###### （4-2）创建AspectJ切入点增强器

```java
public InstantiationModelAwarePointcutAdvisorImpl(AspectJExpressionPointcut declaredPointcut,
			Method aspectJAdviceMethod, AspectJAdvisorFactory aspectJAdvisorFactory,
			MetadataAwareAspectInstanceFactory aspectInstanceFactory, int declarationOrder, String aspectName) {
		// 设置定义的切入点
		this.declaredPointcut = declaredPointcut;
         // 设置通知方法的声明类
		this.declaringClass = aspectJAdviceMethod.getDeclaringClass();
         // 设置通知方法名称
		this.methodName = aspectJAdviceMethod.getName();
         // 设置通知方法的所有参数类型
		this.parameterTypes = aspectJAdviceMethod.getParameterTypes();
         // 设置通知方法
		this.aspectJAdviceMethod = aspectJAdviceMethod;
         // 设置切面增强器工厂
		this.aspectJAdvisorFactory = aspectJAdvisorFactory;
         // 设置切面实例化工厂
		this.aspectInstanceFactory = aspectInstanceFactory;
         // 设置定义的顺序。由前面可知，同一个切面类里的所有通知方法，这里都是设置为0
		this.declarationOrder = declarationOrder;
         // 设置切面的名称。这里为@Aspect注解所在的bean名称
		this.aspectName = aspectName;
		// 默认情况下不是懒加载，走到else分支
		if (aspectInstanceFactory.getAspectMetadata().isLazilyInstantiated()) {
			// Static part of the pointcut is a lazy type.
			Pointcut preInstantiationPointcut = Pointcuts.union(
					aspectInstanceFactory.getAspectMetadata().getPerClausePointcut(), this.declaredPointcut);

			// Make it dynamic: must mutate from pre-instantiation to post-instantiation state.
			// If it's not a dynamic pointcut, it may be optimized out
			// by the Spring AOP infrastructure after the first evaluation.
			this.pointcut = new PerTargetInstantiationModelPointcut(
					this.declaredPointcut, preInstantiationPointcut, aspectInstanceFactory);
			this.lazy = true;
		}
		else {             
			// A singleton aspect.
             // 【注意】这里设置增强器的两大关键内容：（1）切入点 （2）通知
             // 设置切入点
			this.pointcut = this.declaredPointcut;
             // 设置懒加载为false
			this.lazy = false;
             // 实例化通知并设置
			this.instantiatedAdvice = instantiateAdvice(this.declaredPointcut);
		}
	}
```



* 实例化通知

```java
private Advice instantiateAdvice(AspectJExpressionPointcut pointcut) {
    Advice advice = this.aspectJAdvisorFactory.getAdvice(this.aspectJAdviceMethod, pointcut,
            this.aspectInstanceFactory, this.declarationOrder, this.aspectName);
    return (advice != null ? advice : EMPTY_ADVICE);
}

/**
为给定的AspectJ通知方法构建一个Spring AOP通知。
Build a Spring AOP Advice for the given AspectJ advice method.
Params:
candidateAdviceMethod – the candidate advice method
expressionPointcut – the AspectJ expression pointcut
aspectInstanceFactory – the aspect instance factory
declarationOrder – the declaration order within the aspect
aspectName – the name of the aspect
Returns:
null if the method is not an AspectJ advice method or if it is a pointcut that will be used by other advice but will not create a Spring advice in its own right
See Also:
org.springframework.aop.aspectj.AspectJAroundAdvice,
org.springframework.aop.aspectj.AspectJMethodBeforeAdvice,
org.springframework.aop.aspectj.AspectJAfterAdvice, 
org.springframework.aop.aspectj.AspectJAfterReturningAdvice, org.springframework.aop.aspectj.AspectJAfterThrowingAdvice
*/
@Override
@Nullable
public Advice getAdvice(Method candidateAdviceMethod, AspectJExpressionPointcut expressionPointcut,
        MetadataAwareAspectInstanceFactory aspectInstanceFactory, int declarationOrder, String aspectName) {
    // 获取切面类的Class
    Class<?> candidateAspectClass = aspectInstanceFactory.getAspectMetadata().getAspectClass();
    // 校验candidateAspectClass(同上面的: 校验@Aspect注解所在的类)
    validate(candidateAspectClass);
	
    // 获取通知方法上注解
    AspectJAnnotation<?> aspectJAnnotation =
            AbstractAspectJAdvisorFactory.findAspectJAnnotationOnMethod(candidateAdviceMethod);
    if (aspectJAnnotation == null) {
        return null;
    }
    
    // 校验切面类上是否有@AspectJ注解，如果没有直接报错
    // If we get here, we know we have an AspectJ method.
    // Check that it's an AspectJ-annotated class
    if (!isAspect(candidateAspectClass)) {
        throw new AopConfigException("Advice must be declared inside an aspect type: " +
                "Offending method '" + candidateAdviceMethod + "' in class [" +
                candidateAspectClass.getName() + "]");
    }

    if (logger.isDebugEnabled()) {
        logger.debug("Found AspectJ method: " + candidateAdviceMethod);
    }

    AbstractAspectJAdvice springAdvice;
    
    /*
     根据注解的类型，创建对应的通知。
     @Around   -->  AspectJAroundAdvice
     @Before   -->  AspectJMethodBeforeAdvice
     @After    -->  AspectJAfterAdvice
     @AfterReturning  -->  AspectJAfterReturningAdvice
     @AfterThrowing   -->  AspectJAfterThrowingAdvice
    */
    switch (aspectJAnnotation.getAnnotationType()) {
        case AtPointcut:
            if (logger.isDebugEnabled()) {
                logger.debug("Processing pointcut '" + candidateAdviceMethod.getName() + "'");
            }
            return null;
        case AtAround:
            springAdvice = new AspectJAroundAdvice(
                    candidateAdviceMethod, expressionPointcut, aspectInstanceFactory);
            break;
        case AtBefore:
            springAdvice = new AspectJMethodBeforeAdvice(
                    candidateAdviceMethod, expressionPointcut, aspectInstanceFactory);
            break;
        case AtAfter:
            springAdvice = new AspectJAfterAdvice(
                    candidateAdviceMethod, expressionPointcut, aspectInstanceFactory);
            break;
        case AtAfterReturning:
            springAdvice = new AspectJAfterReturningAdvice(
                    candidateAdviceMethod, expressionPointcut, aspectInstanceFactory);
            AfterReturning afterReturningAnnotation = (AfterReturning) aspectJAnnotation.getAnnotation();
            if (StringUtils.hasText(afterReturningAnnotation.returning())) {
                springAdvice.setReturningName(afterReturningAnnotation.returning());
            }
            break;
        case AtAfterThrowing:
            springAdvice = new AspectJAfterThrowingAdvice(
                    candidateAdviceMethod, expressionPointcut, aspectInstanceFactory);
            AfterThrowing afterThrowingAnnotation = (AfterThrowing) aspectJAnnotation.getAnnotation();
            if (StringUtils.hasText(afterThrowingAnnotation.throwing())) {
                springAdvice.setThrowingName(afterThrowingAnnotation.throwing());
            }
            break;
        default:
            throw new UnsupportedOperationException(
                    "Unsupported advice type on method: " + candidateAdviceMethod);
    }

    // Now to configure the advice...
    // 设置通知的切面名称。这里是@AspectJ注解所在的bean名称
    springAdvice.setAspectName(aspectName);
    // 设置定义的顺序。由前面可知，同一个切面类里的所有通知方法，这里都是设置为0
    springAdvice.setDeclarationOrder(declarationOrder);
    // 设置方法参数名称
    String[] argNames = this.parameterNameDiscoverer.getParameterNames(candidateAdviceMethod);
    if (argNames != null) {
        springAdvice.setArgumentNamesFromStringArray(argNames);
    }
    // 计算并绑定方法参数的名称、类型和位置
    springAdvice.calculateArgumentBindings();

    return springAdvice;
}
```





### 2.2 筛选出符合条件的增强器

* 通过切入点PointCut的ClassFilter（类过滤器）和 MethodMatcher（方法匹配器）实现筛选

```
AopUtils
```

```java
/**
 * Search the given candidate Advisors to find all Advisors that
 * can apply to the specified bean.
 * @param candidateAdvisors the candidate Advisors
 * @param beanClass the target's bean class
 * @param beanName the target's bean name
 * @return the List of applicable Advisors
 * @see ProxyCreationContext#getCurrentProxiedBeanName()
 */
protected List<Advisor> findAdvisorsThatCanApply(
        List<Advisor> candidateAdvisors, Class<?> beanClass, String beanName) {

    ProxyCreationContext.setCurrentProxiedBeanName(beanName);
    try {
        // 执行到这里
        return AopUtils.findAdvisorsThatCanApply(candidateAdvisors, beanClass);
    }
    finally {
        ProxyCreationContext.setCurrentProxiedBeanName(null);
    }
}


/**
 * Determine the sublist of the {@code candidateAdvisors} list
 * that is applicable to the given class.
 * @param candidateAdvisors the Advisors to evaluate
 * @param clazz the target class
 * @return sublist of Advisors that can apply to an object of the given class
 * (may be the incoming List as-is)
 */
public static List<Advisor> findAdvisorsThatCanApply(List<Advisor> candidateAdvisors, Class<?> clazz) {
    if (candidateAdvisors.isEmpty()) {
        return candidateAdvisors;
    }
    List<Advisor> eligibleAdvisors = new ArrayList<>();
    for (Advisor candidate : candidateAdvisors) {
        // 这里我们示例使用的增强器都不是IntroductionAdvisor，所以这里的canApply不用看。
        // 这里对应切面属性上的@DeclareParents注解创建增强器DeclareParentsAdvisor
        if (candidate instanceof IntroductionAdvisor && canApply(candidate, clazz)) {
            eligibleAdvisors.add(candidate);
        }
    }
    //这里我们示例使用的增强器中没有IntroductionAdvisor，所以为false
    boolean hasIntroductions = !eligibleAdvisors.isEmpty();    
    // 遍历所有候选的增强器，判断是否符合条件，保留符合条件
    for (Advisor candidate : candidateAdvisors) {
        if (candidate instanceof IntroductionAdvisor) {
            // already processed
            continue;
        }
        // 判断是否符合条件（即增强器是否可以应用到给定的clazz）
        if (canApply(candidate, clazz, hasIntroductions)) {
            // 保存符合条件的增强器
            eligibleAdvisors.add(candidate);
        }
    }
    // 返回符合条件的增强器
    return eligibleAdvisors;
}

/**
 * 增强器是否可以应用到给定的clazz
 * Can the given advisor apply at all on the given class?
 * <p>This is an important test as it can be used to optimize out a advisor for a class.
 * This version also takes into account introductions (for IntroductionAwareMethodMatchers).
 * @param advisor the advisor to check
 * @param targetClass class we're testing
 * @param hasIntroductions whether or not the advisor chain for this bean includes
 * any introductions
 * @return whether the pointcut can apply on any method
 */
public static boolean canApply(Advisor advisor, Class<?> targetClass, boolean hasIntroductions) {
    if (advisor instanceof IntroductionAdvisor) {
        return ((IntroductionAdvisor) advisor).getClassFilter().matches(targetClass);
    }
    else if (advisor instanceof PointcutAdvisor) {
        // 执行到这里。
        PointcutAdvisor pca = (PointcutAdvisor) advisor;        
        return canApply(pca.getPointcut(), targetClass, hasIntroductions);
    }
    else {
        // 没有切入点，所以我们假设它可以应用
        // It doesn't have a pointcut so we assume it applies.
        return true;
    }
}
```



```java
public static boolean canApply(Pointcut pc, Class<?> targetClass, boolean hasIntroductions) {
    Assert.notNull(pc, "Pointcut must not be null");
    // 2.2.1 类过滤器是否过滤targetClass。不同的切入点过滤逻辑不一样
    if (!pc.getClassFilter().matches(targetClass)) {
        return false;
    }
    
    // 获取方法匹配器。如果方法匹配器为MethodMatcher.TRUE，直接匹配
    MethodMatcher methodMatcher = pc.getMethodMatcher();
    if (methodMatcher == MethodMatcher.TRUE) {
        // No need to iterate the methods if we're matching any method anyway...
        return true;
    }

    IntroductionAwareMethodMatcher introductionAwareMethodMatcher = null;
    if (methodMatcher instanceof IntroductionAwareMethodMatcher) {
        introductionAwareMethodMatcher = (IntroductionAwareMethodMatcher) methodMatcher;
    }
    
    // 获取目标类实现的所有接口类
    Set<Class<?>> classes = new LinkedHashSet<>();
    if (!Proxy.isProxyClass(targetClass)) {
        // 如果类不属于Proxy类或者它的子类，获取目标类被代理之前的类 
        classes.add(ClassUtils.getUserClass(targetClass));
    }
    classes.addAll(ClassUtils.getAllInterfacesForClassAsSet(targetClass));

    // 遍历目标类以及实现的所有接口类
    for (Class<?> clazz : classes) {
        // 获取类中定义的方法，判断是否符合切入点的方法匹配器。
        // 如果有一个方法匹配，返回true，说明该增强器符合给定的类 
        Method[] methods = ReflectionUtils.getAllDeclaredMethods(clazz);
        for (Method method : methods) {
            // 2.2.2 是否匹配方法匹配器。不同的切入点匹配逻辑不一样
            if (introductionAwareMethodMatcher != null ?
                    introductionAwareMethodMatcher.matches(method, targetClass, hasIntroductions) :
                    methodMatcher.matches(method, targetClass)) {
                return true;
            }
        }
    }

    return false;
}
```



#### 2.2.1 类过滤器是否过滤类

不同增强器的类过滤器，过滤逻辑不一样。

##### 2.2.1.1 CacheOperationSourceClassFilter 

 缓存增强器的类过滤器

```java
private class CacheOperationSourceClassFilter implements ClassFilter {
    @Override
    public boolean matches(Class<?> clazz) {
        // 如果clazz属于CacheManager类型及其子类，说明不符合条件
        if (CacheManager.class.isAssignableFrom(clazz)) {
            return false;
        }
        CacheOperationSource cas = getCacheOperationSource();
        // AnnotationCacheOperationSource
        return (cas == null || cas.isCandidateClass(clazz));
    }
}

 // AnnotationCacheOperationSource
@Override
public boolean isCandidateClass(Class<?> targetClass) {
    // 只有SpringCacheAnnotationParser
    for (CacheAnnotationParser parser : this.annotationParsers) {
        if (parser.isCandidateClass(targetClass)) {
            return true;
        }
    }
    return false;
}

// SpringCacheAnnotationParser
private static final Set<Class<? extends Annotation>> CACHE_OPERATION_ANNOTATIONS = new LinkedHashSet<>(8);
static {
    CACHE_OPERATION_ANNOTATIONS.add(Cacheable.class);
    CACHE_OPERATION_ANNOTATIONS.add(CacheEvict.class);
    CACHE_OPERATION_ANNOTATIONS.add(CachePut.class);
    CACHE_OPERATION_ANNOTATIONS.add(Caching.class);
}

@Override
public boolean isCandidateClass(Class<?> targetClass) {    
   return AnnotationUtils.isCandidateClass(targetClass, CACHE_OPERATION_ANNOTATIONS);
}

// AnnotationUtils
public static boolean isCandidateClass(Class<?> clazz, Collection<Class<? extends Annotation>> annotationTypes) {
    for (Class<? extends Annotation> annotationType : annotationTypes) {
        if (isCandidateClass(clazz, annotationType)) {
            return true;
        }
    }
    return false;
}
/**
 * Determine whether the given class is a candidate for carrying the specified annotation
 * (at type, method or field level).
 * @param clazz the class to introspect
 * @param annotationType the searchable annotation type
 * @return {@code false} if the class is known to have no such annotations at any level;
 * {@code true} otherwise. Callers will usually perform full method/field introspection
 * if {@code true} is being returned here.
 * @since 5.2
 * @see #isCandidateClass(Class, String)
 */
public static boolean isCandidateClass(Class<?> clazz, Class<? extends Annotation> annotationType) {
    return isCandidateClass(clazz, annotationType.getName());
}

// 最后调到这里，这个方法返回true
public static boolean isCandidateClass(Class<?> clazz, String annotationName) {
    if (annotationName.startsWith("java.")) {
        return true;
    }
    if (AnnotationsScanner.hasPlainJavaAnnotationsOnly(clazz)) {
        return false;
    }
    return true;
}

static boolean hasPlainJavaAnnotationsOnly(Class<?> type) {
	return (type.getName().startsWith("java.") || type == Ordered.class);
}
```

从CacheOperationSourceClassFilter中可以知道，即使在类过滤器中发现类上没有相关的缓存注解，还是会返回true。

为什么呢？因为即使类上没有相关的缓存注解，方法上也可能有，返回true，让后面的方法匹配器再来匹配相应的方法，从而最终决定增强器是否符合条件。

正如上面的英文注释，仔细体会，精彩至今！！！

```java
 * {@code true} otherwise. Callers will usually perform full method/field introspection
 * if {@code true} is being returned here.
```



##### 2.1.1.2 TransactionAttributeSourceClassFilter

 事务增强器的类过滤器

```java
private class TransactionAttributeSourceClassFilter implements ClassFilter {

    @Override
    public boolean matches(Class<?> clazz) {
       // 如果clazz属于TransactionalProxy类型(子类也算)，或者属于TransactionManager类型（子类也算）
        // 如果clazz属于PersistenceExceptionTranslator类型(子类也算)，说明不符合条件，返回false
        if (TransactionalProxy.class.isAssignableFrom(clazz) ||
                TransactionManager.class.isAssignableFrom(clazz) ||
                PersistenceExceptionTranslator.class.isAssignableFrom(clazz)) {
            return false;
        }
        // AnnotationTransactionAttributeSource
        TransactionAttributeSource tas = getTransactionAttributeSource();
        return (tas == null || tas.isCandidateClass(clazz));
    }
}

// AnnotationTransactionAttributeSource
@Override
public boolean isCandidateClass(Class<?> targetClass) {
    // 只有SpringTransactionAnnotationParser
    for (TransactionAnnotationParser parser : this.annotationParsers) {
        if (parser.isCandidateClass(targetClass)) {
            return true;
        }
    }
    return false;
}

// SpringTransactionAnnotationParser
@Override
public boolean isCandidateClass(Class<?> targetClass) {
    return AnnotationUtils.isCandidateClass(targetClass, Transactional.class);
}

public static boolean isCandidateClass(Class<?> clazz, Class<? extends Annotation> annotationType) {
    return isCandidateClass(clazz, annotationType.getName());
}

// 最后调到这里，这个方法返回true
public static boolean isCandidateClass(Class<?> clazz, String annotationName) {
    if (annotationName.startsWith("java.")) {
        return true;
    }
    if (AnnotationsScanner.hasPlainJavaAnnotationsOnly(clazz)) {
        return false;
    }
    return true;
}
static boolean hasPlainJavaAnnotationsOnly(Class<?> type) {
	return (type.getName().startsWith("java.") || type == Ordered.class);
}	
```

即使在类过滤器发现类上没有@Transactional注解，还是会返回true。因为方法上可能有，返回true，让后面的方法匹配器再来匹配相应的方法，从而最终决定增强器是否符合条件。



##### 2.2.1.3 AspectJExpressionPointcut

@AspectJ的通知方法对应的增强器也属于类过滤器，匹配逻辑较为复杂。不展开

```java
@Override
public boolean matches(Class<?> targetClass) {
    PointcutExpression pointcutExpression = obtainPointcutExpression();
    try {
        try {
            return pointcutExpression.couldMatchJoinPointsInType(targetClass);
        }
        catch (ReflectionWorldException ex) {
            logger.debug("PointcutExpression matching rejected target class - trying fallback expression", ex);
            // Actually this is still a "maybe" - treat the pointcut as dynamic if we don't know enough yet
            PointcutExpression fallbackExpression = getFallbackPointcutExpression(targetClass);
            if (fallbackExpression != null) {
                return fallbackExpression.couldMatchJoinPointsInType(targetClass);
            }
        }
    }
    catch (Throwable ex) {
        logger.debug("PointcutExpression matching rejected target class", ex);
    }
    return false;
}
```

结合上面的分析：

缓存增强器、事务增强器的类过滤器在默认情况下都是符合条件的（除了个别类）。@AspectJ的通知方法对应的增强器要看是否满足切入点表达式，具体分析。



#### 2.2.2 是否匹配方法匹配器

以事务增强器、缓存增强器的方法匹配器来说明

##### 2.2.2.1  TransactionAttributeSourcePointcut

```java
@Override
public boolean matches(Method method, Class<?> targetClass) {
    // AnnotationTransactionAttributeSource
    // AnnotationTransactionAttributeSource继承AbstractFallbackTransactionAttributeSource
    TransactionAttributeSource tas = getTransactionAttributeSource();
    // 事务属性不为空，说明匹配方法匹配器，反之，不符合
    return (tas == null || tas.getTransactionAttribute(method, targetClass) != null);
}
```



```java
private final Map<Object, TransactionAttribute> attributeCache = new ConcurrentHashMap<>(1024);
protected Object getCacheKey(Method method, @Nullable Class<?> targetClass) {
    return new MethodClassKey(method, targetClass);
}
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
    if (method.getDeclaringClass() == Object.class) {
        return null;
    }
    
    // 首先看看是否存在缓存。有的话，返回缓存的结果
    // First, see if we have a cached value.
    Object cacheKey = getCacheKey(method, targetClass);
    TransactionAttribute cached = this.attributeCache.get(cacheKey);
    if (cached != null) {
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
        // 获取事务属性，并添加缓存
        // We need to work it out.
        TransactionAttribute txAttr = computeTransactionAttribute(method, targetClass);
        // Put it in the cache.
        if (txAttr == null) {
            this.attributeCache.put(cacheKey, NULL_TRANSACTION_ATTRIBUTE);
        }
        else {
            String methodIdentification = ClassUtils.getQualifiedMethodName(method, targetClass);
            if (txAttr instanceof DefaultTransactionAttribute) {
                DefaultTransactionAttribute dta = (DefaultTransactionAttribute) txAttr;
                dta.setDescriptor(methodIdentification);
                dta.resolveAttributeStrings(this.embeddedValueResolver);
            }
            if (logger.isTraceEnabled()) {
                logger.trace("Adding transactional method '" + methodIdentification + "' with attribute: " + txAttr);
            }
            this.attributeCache.put(cacheKey, txAttr);
        }
        return txAttr;
    }
}
```



* 获取事务属性

```java
@Nullable
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
        // 通过解析器解析注解
        TransactionAttribute attr = parser.parseTransactionAnnotation(element);
        if (attr != null) {
            return attr;
        }
    }
    return null;
}

// SpringTransactionAnnotationParser
@Override
@Nullable
public TransactionAttribute parseTransactionAnnotation(AnnotatedElement element) {
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
    RuleBasedTransactionAttribute rbta = new RuleBasedTransactionAttribute();

    Propagation propagation = attributes.getEnum("propagation");
    rbta.setPropagationBehavior(propagation.value());
    Isolation isolation = attributes.getEnum("isolation");
    rbta.setIsolationLevel(isolation.value());

    rbta.setTimeout(attributes.getNumber("timeout").intValue());
    String timeoutString = attributes.getString("timeoutString");
    Assert.isTrue(!StringUtils.hasText(timeoutString) || rbta.getTimeout() < 0,
            "Specify 'timeout' or 'timeoutString', not both");
    rbta.setTimeoutString(timeoutString);

    rbta.setReadOnly(attributes.getBoolean("readOnly"));
    rbta.setQualifier(attributes.getString("value"));
    rbta.setLabels(Arrays.asList(attributes.getStringArray("label")));

    List<RollbackRuleAttribute> rollbackRules = new ArrayList<>();
    for (Class<?> rbRule : attributes.getClassArray("rollbackFor")) {
        rollbackRules.add(new RollbackRuleAttribute(rbRule));
    }
    for (String rbRule : attributes.getStringArray("rollbackForClassName")) {
        rollbackRules.add(new RollbackRuleAttribute(rbRule));
    }
    for (Class<?> rbRule : attributes.getClassArray("noRollbackFor")) {
        rollbackRules.add(new NoRollbackRuleAttribute(rbRule));
    }
    for (String rbRule : attributes.getStringArray("noRollbackForClassName")) {
        rollbackRules.add(new NoRollbackRuleAttribute(rbRule));
    }
    rbta.setRollbackRules(rollbackRules);

    return rbta;
}
```



##### 2.2.2.2 CacheOperationSourcePointcut

```java
@Override
public boolean matches(Method method, Class<?> targetClass) {
    // AnnotationCacheOperationSource 
    // AnnotationCacheOperationSource继承AbstractFallbackCacheOperationSource
    CacheOperationSource cas = getCacheOperationSource();
    // 获取缓存的操作列表，如果不为空，说明匹配方法匹配器，反之，不符合。
    return (cas != null && !CollectionUtils.isEmpty(cas.getCacheOperations(method, targetClass)));
}
```



```java
private final Map<Object, Collection<CacheOperation>> attributeCache = new ConcurrentHashMap<>(1024);
protected Object getCacheKey(Method method, @Nullable Class<?> targetClass) {
    return new MethodClassKey(method, targetClass);
}
/**
 * Determine the caching attribute for this method invocation.
 * <p>Defaults to the class's caching attribute if no method attribute is found.
 * @param method the method for the current invocation (never {@code null})
 * @param targetClass the target class for this invocation (may be {@code null})
 * @return {@link CacheOperation} for this method, or {@code null} if the method
 * is not cacheable
 */
@Override
@Nullable
public Collection<CacheOperation> getCacheOperations(Method method, @Nullable Class<?> targetClass) {
    // 方法定义的类为Object，直接返回null
    if (method.getDeclaringClass() == Object.class) {
        return null;
    }
    
    // 首先从缓存中获取，如果存在缓存，则返回缓存结果
    Object cacheKey = getCacheKey(method, targetClass);
    Collection<CacheOperation> cached = this.attributeCache.get(cacheKey);

    if (cached != null) {
        return (cached != NULL_CACHING_ATTRIBUTE ? cached : null);
    }
    else {
        // 获取CacheOperation列表，并添加缓存
        Collection<CacheOperation> cacheOps = computeCacheOperations(method, targetClass);
        if (cacheOps != null) {
            if (logger.isTraceEnabled()) {
                logger.trace("Adding cacheable method '" + method.getName() + "' with attribute: " + cacheOps);
            }
            this.attributeCache.put(cacheKey, cacheOps);
        }
        else {
            this.attributeCache.put(cacheKey, NULL_CACHING_ATTRIBUTE);
        }
        return cacheOps;
    }
}
```



* 获取缓存操作列表

```java
@Nullable
private Collection<CacheOperation> computeCacheOperations(Method method, @Nullable Class<?> targetClass) {
    // 如果配置为只允许公共方法，并且当前方法不是公共方法，则直接返回 null，表示该方法没有缓存操作。
    // Don't allow no-public methods as required.
    if (allowPublicMethodsOnly() && !Modifier.isPublic(method.getModifiers())) {
        return null;
    }


    // The method may be on an interface, but we need attributes from the target class.
    // If the target class is null, the method will be unchanged.
    Method specificMethod = AopUtils.getMostSpecificMethod(method, targetClass);

    // First try is the method in the target class.
    // 首先尝试获取目标类中具体方法的缓存操作。如果存在则直接返回。
    Collection<CacheOperation> opDef = findCacheOperations(specificMethod);
    if (opDef != null) {
        return opDef;
    }
    
    // 如果在方法上没有找到缓存操作，则尝试从方法声明的类上获取缓存操作。如果存在且方法是用户级别的方法，则返回。
    // Second try is the caching operation on the target class.
    opDef = findCacheOperations(specificMethod.getDeclaringClass());
    if (opDef != null && ClassUtils.isUserLevelMethod(method)) {
        return opDef;
    }

    // 回退到原始方法
    // 如果最具体的方法和原始方法不同，再次尝试从原始方法和其声明的类上获取缓存操作，作为最后的回退方案。
    if (specificMethod != method) {
        // Fallback is to look at the original method.
        opDef = findCacheOperations(method);
        if (opDef != null) {
            return opDef;
        }
        // Last fallback is the class of the original method.
        opDef = findCacheOperations(method.getDeclaringClass());
        if (opDef != null && ClassUtils.isUserLevelMethod(method)) {
            return opDef;
        }
    }

    return null;
}
```



```java
@Override
@Nullable
protected Collection<CacheOperation> findCacheOperations(Class<?> clazz) {
    return determineCacheOperations(parser -> parser.parseCacheAnnotations(clazz));
}

@Override
@Nullable
protected Collection<CacheOperation> findCacheOperations(Method method) {
    return determineCacheOperations(parser -> parser.parseCacheAnnotations(method));
}

/**
 * Determine the cache operation(s) for the given {@link CacheOperationProvider}.
 * <p>This implementation delegates to configured
 * {@link CacheAnnotationParser CacheAnnotationParsers}
 * for parsing known annotations into Spring's metadata attribute class.
 * <p>Can be overridden to support custom annotations that carry caching metadata.
 * @param provider the cache operation provider to use
 * @return the configured caching operations, or {@code null} if none found
 */
@Nullable
protected Collection<CacheOperation> determineCacheOperations(CacheOperationProvider provider) {
    Collection<CacheOperation> ops = null;
    for (CacheAnnotationParser parser : this.annotationParsers) {
        Collection<CacheOperation> annOps = provider.getCacheOperations(parser);
        if (annOps != null) {
            if (ops == null) {
                ops = annOps;
            }
            else {
                Collection<CacheOperation> combined = new ArrayList<>(ops.size() + annOps.size());
                combined.addAll(ops);
                combined.addAll(annOps);
                ops = combined;
            }
        }
    }
    return ops;
}
```



```java
// SpringCacheAnnotationParser
@Override
@Nullable
public Collection<CacheOperation> parseCacheAnnotations(Class<?> type) {
    DefaultCacheConfig defaultConfig = new DefaultCacheConfig(type);
    return parseCacheAnnotations(defaultConfig, type);
}

@Override
@Nullable
public Collection<CacheOperation> parseCacheAnnotations(Method method) {
    DefaultCacheConfig defaultConfig = new DefaultCacheConfig(method.getDeclaringClass());
    return parseCacheAnnotations(defaultConfig, method);
}

@Nullable
private Collection<CacheOperation> parseCacheAnnotations(DefaultCacheConfig cachingConfig, AnnotatedElement ae) {
    Collection<CacheOperation> ops = parseCacheAnnotations(cachingConfig, ae, false);
    if (ops != null && ops.size() > 1) {
        // More than one operation found -> local declarations override interface-declared ones...
        Collection<CacheOperation> localOps = parseCacheAnnotations(cachingConfig, ae, true);
        if (localOps != null) {
            return localOps;
        }
    }
    return ops;
}

@Nullable
private Collection<CacheOperation> parseCacheAnnotations(
        DefaultCacheConfig cachingConfig, AnnotatedElement ae, boolean localOnly) {
    // 获取相关的缓存注解列表  
    Collection<? extends Annotation> anns = (localOnly ?
            AnnotatedElementUtils.getAllMergedAnnotations(ae, CACHE_OPERATION_ANNOTATIONS) :
            AnnotatedElementUtils.findAllMergedAnnotations(ae, CACHE_OPERATION_ANNOTATIONS));
    if (anns.isEmpty()) {
        return null;
    }
    //解析@Cacheable、@CacheEvict、@CachePut、@Caching注解属性，封装成缓存操作列表
    final Collection<CacheOperation> ops = new ArrayList<>(1);
    anns.stream().filter(ann -> ann instanceof Cacheable).forEach(
            ann -> ops.add(parseCacheableAnnotation(ae, cachingConfig, (Cacheable) ann)));
    anns.stream().filter(ann -> ann instanceof CacheEvict).forEach(
            ann -> ops.add(parseEvictAnnotation(ae, cachingConfig, (CacheEvict) ann)));
    anns.stream().filter(ann -> ann instanceof CachePut).forEach(
            ann -> ops.add(parsePutAnnotation(ae, cachingConfig, (CachePut) ann)));
    anns.stream().filter(ann -> ann instanceof Caching).forEach(
            ann -> parseCachingAnnotation(ae, cachingConfig, (Caching) ann, ops));
    return ops;
}
```



##### 2.2.2.3 AspectJExpressionPointcut

```java
@Override
public boolean matches(Method method, Class<?> targetClass, boolean hasIntroductions) {
    obtainPointcutExpression();
    ShadowMatch shadowMatch = getTargetShadowMatch(method, targetClass);

    // Special handling for this, target, @this, @target, @annotation
    // in Spring - we can optimize since we know we have exactly this class,
    // and there will never be matching subclass at runtime.
    if (shadowMatch.alwaysMatches()) {
        return true;
    }
    else if (shadowMatch.neverMatches()) {
        return false;
    }
    else {
        // the maybe case
        if (hasIntroductions) {
            return true;
        }
        // A match test returned maybe - if there are any subtype sensitive variables
        // involved in the test (this, target, at_this, at_target, at_annotation) then
        // we say this is not a match as in Spring there will never be a different
        // runtime subtype.
        RuntimeTestWalker walker = getRuntimeTestWalker(shadowMatch);
        return (!walker.testsSubtypeSensitiveVars() || walker.testTargetInstanceOfResidue(targetClass));
    }
}
```

AspectJExpressionPointcut实现IntroductionAwareMethodMatcher接口，IntroductionAwareMethodMatcher继承MethodMatcher



### 2.3 扩展增强器

见 AspectJAwareAdvisorAutoProxyCreator 的 extendAdvisors

```java
/**
 * 添加ExposeInvocationInterceptor到通知链的开头
 * Add an {@link ExposeInvocationInterceptor} to the beginning of the advice chain.
 * <p>This additional advice is needed when using AspectJ pointcut expressions
 * and when using AspectJ-style advice.
 */
@Override
protected void extendAdvisors(List<Advisor> candidateAdvisors) {
    AspectJProxyUtils.makeAdvisorChainAspectJCapableIfNecessary(candidateAdvisors);
}

/**
 * Add special advisors if necessary to work with a proxy chain that contains AspectJ advisors:
 * concretely, {@link ExposeInvocationInterceptor} at the beginning of the list.
 * <p>This will expose the current Spring AOP invocation (necessary for some AspectJ pointcut
 * matching) and make available the current AspectJ JoinPoint. The call will have no effect
 * if there are no AspectJ advisors in the advisor chain.
 * @param advisors the advisors available
 * @return {@code true} if an {@link ExposeInvocationInterceptor} was added to the list,
 * otherwise {@code false}
 */
public static boolean makeAdvisorChainAspectJCapableIfNecessary(List<Advisor> advisors) {
    // Don't add advisors to an empty list; may indicate that proxying is just not required
    if (!advisors.isEmpty()) {
        boolean foundAspectJAdvice = false;
        for (Advisor advisor : advisors) {
            // 确定给定的增强器是否包含AspectJ的通知
            // Be careful not to get the Advice without a guard, as this might eagerly
            // instantiate a non-singleton AspectJ aspect...
            if (isAspectJAdvice(advisor)) {
                foundAspectJAdvice = true;
                break;
            }
        }
        // 如果包含，添加一个DefaultPointcutAdvisor到所有的增强器前面。
        // DefaultPointcutAdvisor包含ExposeInvocationInterceptor通知，切入点为Pointcut pointcut = Pointcut.TRUE;
  		// ExposeInvocationInterceptor的作用是用于记录当前正在调用的MethodInvocation
 	    // ExposeInvocationInterceptor实现了PriorityOrdered接口，PriorityOrdered接口继承Ordered接口，返回的order值为int的最小值 + 1
        if (foundAspectJAdvice && !advisors.contains(ExposeInvocationInterceptor.ADVISOR)) {
            advisors.add(0, ExposeInvocationInterceptor.ADVISOR);
            return true;
        }
    }
    return false;
}

public static final ExposeInvocationInterceptor INSTANCE = new ExposeInvocationInterceptor();
public static final Advisor ADVISOR = new DefaultPointcutAdvisor(INSTANCE) {
		@Override
		public String toString() {
			return ExposeInvocationInterceptor.class.getName() +".ADVISOR";
		}
	};

/**
 * 确定给定的增强器是否包含AspectJ的通知
 * Determine whether the given Advisor contains an AspectJ advice.
 * @param advisor the Advisor to check
 */
private static boolean isAspectJAdvice(Advisor advisor) {
    return (advisor instanceof InstantiationModelAwarePointcutAdvisor ||
            advisor.getAdvice() instanceof AbstractAspectJAdvice ||
            (advisor instanceof PointcutAdvisor &&
                    ((PointcutAdvisor) advisor).getPointcut() instanceof AspectJExpressionPointcut));
}
```

【注意】

ExposeInvocationInterceptor实现了PriorityOrdered接口，PriorityOrdered接口继承Ordered接口。

返回的order值为int的最小值 + 1

【总结】如果有AspectJ相关的增强器，则添加ExposeInvocationInterceptor到增强器列表的首位，用于暴露当前的拦截对象。



### 2.4  排序增强器

```
AspectJAwareAdvisorAutoProxyCreator
```

```java
// 优先级比较器
private static final Comparator<Advisor> DEFAULT_PRECEDENCE_COMPARATOR = new AspectJPrecedenceComparator();

/**
 * Sort the supplied {@link Advisor} instances according to AspectJ precedence.
 * <p>If two pieces of advice come from the same aspect, they will have the same
 * order. Advice from the same aspect is then further ordered according to the
 * following rules:
 * <ul>
 * <li>If either of the pair is <em>after</em> advice, then the advice declared
 * last gets highest precedence (i.e., runs last).</li>
 * <li>Otherwise the advice declared first gets highest precedence (i.e., runs
 * first).</li>
 * </ul>
 *
 * 重要提示:建议按优先级排序，从最高优先级到最低优先级。
 * 在进入连接点的过程中，应该首先运行优先级最高的建议器。在连接点的“出口”中，最高优先级的建议器应该最后运行。
 * <p><b>Important:</b> Advisors are sorted in precedence order, from highest
 * precedence to lowest. "On the way in" to a join point, the highest precedence
 * advisor should run first. "On the way out" of a join point, the highest
 * precedence advisor should run last.
 */
@Override
protected List<Advisor> sortAdvisors(List<Advisor> advisors) {
    List<PartiallyComparableAdvisorHolder> partiallyComparableAdvisors = new ArrayList<>(advisors.size());
    for (Advisor advisor : advisors) {
        // 创建增强器holder，保存增强器和AspectJPrecedenceComparator比较器
        partiallyComparableAdvisors.add(
                new PartiallyComparableAdvisorHolder(advisor, DEFAULT_PRECEDENCE_COMPARATOR));
    }
    // 排序
    List<PartiallyComparableAdvisorHolder> sorted = PartialOrder.sort(partiallyComparableAdvisors);
    if (sorted != null) {
        List<Advisor> result = new ArrayList<>(advisors.size());
        // 保存排序好的结果
        for (PartiallyComparableAdvisorHolder pcAdvisor : sorted) {
            result.add(pcAdvisor.getAdvisor());
        }
        return result;
    }
    else {
        return super.sortAdvisors(advisors);
    }
}
```



#### 2.4.1 PartialOrder有向图排序

```java
/* *******************************************************************
 * Copyright (c) 1999-2001 Xerox Corporation,
 *               2002 Palo Alto Research Center, Incorporated (PARC).
 * All rights reserved.
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Public License v 2.0
 * which accompanies this distribution and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.txt
 *
 * Contributors:
 *     Xerox/PARC     initial implementation
 * ******************************************************************/

package org.aspectj.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * This class implements a partial order
 *
 * It includes routines for doing a topo-sort
 */

public class PartialOrder {

	/**
	 * All classes that want to be part of a partial order must implement PartialOrder.PartialComparable.
	 */
	public interface PartialComparable {
		/**
		 * 【注意】 这里的compareTo方法返回0和 java.util.Comparable.compareTo()方法返回0的意义不一样
		 *         这里的0表示不用比较
		 * @return <ul>
		 *         <li>+1 if this is greater than other</li>
		 *         <li>-1 if this is less than other</li>
		 *         <li>0 if this is not comparable to other</li>
		 *         </ul>
		 *
		 *         <b> Note: returning 0 from this method doesn't mean the same thing as returning 0 from
		 *         java.util.Comparable.compareTo()</b>
		 */
		int compareTo(Object other);

		/**
		 * This method can provide a deterministic ordering for elements that are strictly not comparable. If you have no need for
		 * this, this method can just return 0 whenever called.
		 */
		int fallbackCompareTo(Object other);
	}
	
    
    /**
    * 增强器排序的对象，实现了PartialComparable接口
    */
	private static class SortObject<T extends PartialComparable> {
         // 当前增强器PartiallyComparableAdvisorHolder
		T object;
         // 比当前增强器顺序小的（优先级高的）
		List<SortObject<T>> smallerObjects = new LinkedList<>();
         // 比当前增强器顺序大的（优先级低的）
		List<SortObject<T>> biggerObjects = new LinkedList<>();
		public SortObject(T o) {
			object = o;
		}
         
         // 判断smallerObjects是否为空，即判断是否有比当前增强器顺序更小的增强器存在（优先级高的），true说明有。
		boolean hasNoSmallerObjects() {
			return smallerObjects.size() == 0;
		}
         
        // 从smallerObjects中移除o
		boolean removeSmallerObject(SortObject<T> o) {
			smallerObjects.remove(o);
			return hasNoSmallerObjects();
		}
 
		void addDirectedLinks(SortObject<T> other) {
             // 比较两个增强器的顺序。
             // PartiallyComparableAdvisorHolder的compareTo方法
			int cmp = object.compareTo(other.object);
             // 如果顺序相同，直接结束
			if (cmp == 0) {
				return;
			}
             // 这里的目的其实是维护每个节点的有向图
			if (cmp > 0) {
                 // 如果other的顺序小，则添加other的增强器到当前增强器的smallerObjects（other的优先级高）
                 // 添加当前增强器到other.biggerObjects
				this.smallerObjects.add(other);
				other.biggerObjects.add(this);
			} else {
                 // 如果other的顺序大，则添加other的增强器到当前增强器的biggerObjects（other的优先级低）
                 // 添加当前增强器到other.smallerObjects
				this.biggerObjects.add(other);
				other.smallerObjects.add(this);
			}
		}

		public String toString() {
			return object.toString(); // +smallerObjects+biggerObjects;
		}
	}

	private static <T extends PartialComparable> void addNewPartialComparable(List<SortObject<T>> graph, T o) {
		SortObject<T> so = new SortObject<>(o);
		for (SortObject<T> other : graph) {
			so.addDirectedLinks(other);
		}
		graph.add(so);
	}

	private static <T extends PartialComparable> void removeFromGraph(List<SortObject<T>> graph, SortObject<T> o) {
		for (Iterator<SortObject<T>> i = graph.iterator(); i.hasNext();) {
			SortObject<T> other = i.next();

			if (o == other) {
				i.remove();
			}
			// ??? could use this to build up a new queue of objects with no
			// ??? smaller ones
			other.removeSmallerObject(o);
		}
	}

	/**
	 * 【重点】使用有向图排序算法，排序所有的增强器
	 * @param objects must all implement PartialComparable
	 * @return the same members as objects, but sorted according to their partial order. returns null if the objects are cyclical
	 *
	 */
	public static <T extends PartialComparable> List<T> sort(List<T> objects) {
         // 增强器的顺序小于两个，直接结束，不用排序。
		// lists of size 0 or 1 don't need any sorting
		if (objects.size() < 2) {
			return objects;
		}

		// ??? we might want to optimize a few other cases of small size
         
		// ??? I don't like creating this data structure, but it does give good
		// ??? separation of concerns.
         // 维护好有向图。
         // 遍历，找到比当前增强器顺序大（优先级低的）的所有增强器(biggerObjects)
         // 找到比当前增强器顺序小（优先级高的）的所有增强器(smallerObjects)。
		List<SortObject<T>> sortList = new LinkedList<>();
		for (T object : objects) {          
			addNewPartialComparable(sortList, object);
		}

		// System.out.println(sortList);

		// now we have built our directed graph
		// use a simple sort algorithm from here
		// can increase efficiency later
		// List ret = new ArrayList(objects.size());
		final int N = objects.size();
         // 遍历维护好有向图的增强器节点，获取排序结果         
		for (int index = 0; index < N; index++) {
			// System.out.println(sortList);
			// System.out.println("-->" + ret);             
             
			SortObject<T> leastWithNoSmallers = null;
			for (SortObject<T> so: sortList) {
                  // 找到最小顺序的(优先级最高的)有向图节点
				if (so.hasNoSmallerObjects()) {
					if (leastWithNoSmallers == null || so.object.fallbackCompareTo(leastWithNoSmallers.object) < 0) {
						leastWithNoSmallers = so;
					}
				}
			}

			if (leastWithNoSmallers == null) {
				return null;
			}
			
             // 将最小顺序的有向图节点leastWithNoSmallers从sortList移除。
             // 同时移除  其他有向图节点的smallerObjects中保存的 leastWithNoSmallers。
             // 执行这一步的目的是处理完leastWithNoSmallers节点后，让其他节点中的某个升级为优先级最高的节点
			removeFromGraph(sortList, leastWithNoSmallers);
             // 保存leastWithNoSmallers里的增强器
			objects.set(index, leastWithNoSmallers.object);
		}

		return objects;
	}

	/***********************************************************************************
	 * /* a minimal testing harness
	 ***********************************************************************************/
	static class Token implements PartialComparable {
		private String s;

		Token(String s) {
			this.s = s;
		}

		public int compareTo(Object other) {
			Token t = (Token) other;

			int cmp = s.charAt(0) - t.s.charAt(0);
			if (cmp == 1) {
				return 1;
			}
			if (cmp == -1) {
				return -1;
			}
			return 0;
		}

		public int fallbackCompareTo(Object other) {
			return -s.compareTo(((Token) other).s);
		}

		public String toString() {
			return s;
		}
	}

	public static void main(String[] args) {
		List<Token> l = new ArrayList<>();
		l.add(new Token("a1"));
		l.add(new Token("c2"));
		l.add(new Token("b3"));
		l.add(new Token("f4"));
		l.add(new Token("e5"));
		l.add(new Token("d6"));
		l.add(new Token("c7"));
		l.add(new Token("b8"));

		l.add(new Token("z"));
		l.add(new Token("x"));

		l.add(new Token("f9"));
		l.add(new Token("e10"));
		l.add(new Token("a11"));
		l.add(new Token("d12"));
		l.add(new Token("b13"));
		l.add(new Token("c14"));

		System.out.println(l);

		sort(l);

		System.out.println(l);
	}
}
```









下面我们以具体的增强器来说明这个排序算法，从上面的分析可知：

```
事务增强器BeanFactoryTransactionAttributeSourceAdvisor的顺序值为Integer.MAX_VALUE
缓存增强器BeanFactoryCacheOperationSourceAdvisor的顺序值为Integer.MAX_VALUE  
扩展加入的增强器DefaultPointcutAdvisor（包含ExposeInvocationInterceptor通知）的顺序值为Integer.MIN_VALUE + 1
@AspectJ的五个通知方法对应的五个增强器InstantiationModelAwarePointcutAdvisorImpl的的顺序值为1（因为切面类上使用了@Order(1)注解）
```

经过addNewPartialComparable(sortList, object)方法使用AspectJPrecedenceComparator比较顺序，维护好的有向图如下：

![image-20240626161859094](img\image-20240626161859094.png)

![image-20240626162026691](https://lu-note.oss-cn-shenzhen.aliyuncs.com/notes/work/image-20240626162026691.png)

我们简化下，分别以 expose，transaction，cache，before，around，after，afterReturning，afterThrowing来表示以下的增强器里的通知

```
ExposeInvocationInterceptor: order = -2147483647，TransactionInterceptor: order = 2147483647，
CacheInterceptor: order = 2147483647
AspectJAroundAdvice: order = 1, aspect name = webLogAspect, declaration order = 0
AspectJMethodBeforeAdvice: order = 1, aspect name = webLogAspect, declaration order = 0
AspectJAfterAdvice: order = 1, aspect name = webLogAspect, declaration order = 0
AspectJAfterReturningAdvice: order = 1, aspect name = webLogAspect, declaration order = 0
AspectJAfterThrowingAdvice: order = 1, aspect name = webLogAspect, declaration order = 0
```

![有向图](https://lu-note.oss-cn-shenzhen.aliyuncs.com/notes/work/%E6%9C%89%E5%90%91%E5%9B%BE.png)



遍历维护好有向图的增强器节点，获取排序结果

* 第一次循环后

![image-20240626170552489](https://lu-note.oss-cn-shenzhen.aliyuncs.com/notes/work/image-20240626170552489.png)

* 第二次循环后

![image-20240626170426294](https://lu-note.oss-cn-shenzhen.aliyuncs.com/notes/work/image-20240626170426294.png)



* 第三次循环后

![image-20240626170723814](https://lu-note.oss-cn-shenzhen.aliyuncs.com/notes/work/image-20240626170723814.png)



* 第四次循环后

![image-20240626170833602](https://lu-note.oss-cn-shenzhen.aliyuncs.com/notes/work/image-20240626170833602.png)



* 第五次循环后

![image-20240626171100010](https://lu-note.oss-cn-shenzhen.aliyuncs.com/notes/work/image-20240626171100010.png)



* 第六次循环后

![image-20240626171333437](D:\work\img\image-20240626171333437.png)



* 第七次循环后

![image-20240626171440323](https://lu-note.oss-cn-shenzhen.aliyuncs.com/notes/work/image-20240626171440323.png)



* 第八次循环后

![image-20240626171557666](https://lu-note.oss-cn-shenzhen.aliyuncs.com/notes/work/image-20240626171557666.png)

最终经过8次循环，排序好了所有的增强



#### 2.4.2  比较两个增强器的顺序

* PartiallyComparableAdvisorHolder

```java
private static class PartiallyComparableAdvisorHolder implements PartialComparable {
    
    // 当前持有的增强器
    private final Advisor advisor;
    
    // 当前增强器持有的比较器
    private final Comparator<Advisor> comparator;

    public PartiallyComparableAdvisorHolder(Advisor advisor, Comparator<Advisor> comparator) {
        this.advisor = advisor;
        this.comparator = comparator;
    }

    @Override
    public int compareTo(Object obj) {
        Advisor otherAdvisor = ((PartiallyComparableAdvisorHolder) obj).advisor;
        // this.comparator=AspectJPrecedenceComparator
        return this.comparator.compare(this.advisor, otherAdvisor);
    }

    @Override
    public int fallbackCompareTo(Object obj) {
        return 0;
    }

    public Advisor getAdvisor() {
        return this.advisor;
    }
}
```



* AspectJPrecedenceComparator

```java
/**
* Orders AspectJ advice/advisors by precedence (<i>not</i> invocation order).
*
* <p>Given two pieces of advice, {@code A} and {@code B}:
* <ul>
* <li>If {@code A} and {@code B} are defined in different aspects, then the advice
* in the aspect with the lowest order value has the highest precedence.</li>
* <li>If {@code A} and {@code B} are defined in the same aspect, if one of
* {@code A} or {@code B} is a form of <em>after</em> advice, then the advice declared
* last in the aspect has the highest precedence. If neither {@code A} nor {@code B}
* is a form of <em>after</em> advice, then the advice declared first in the aspect
* has the highest precedence.</li>
* </ul>
*
* <p>Important: This comparator is used with AspectJ's
* {@link org.aspectj.util.PartialOrder PartialOrder} sorting utility. Thus, unlike
* a normal {@link Comparator}, a return value of {@code 0} from this comparator
* means we don't care about the ordering, not that the two elements must be sorted
* identically.
*
* @author Adrian Colyer
* @author Juergen Hoeller
* @since 2.0
*/
class AspectJPrecedenceComparator implements Comparator<Advisor> {

private static final int HIGHER_PRECEDENCE = -1;

private static final int SAME_PRECEDENCE = 0;

private static final int LOWER_PRECEDENCE = 1;

private final Comparator<? super Advisor> advisorComparator;

/**
 * Create a default {@code AspectJPrecedenceComparator}.
 */
public AspectJPrecedenceComparator() {
    // AnnotationAwareOrderComparator INSTANCE = new AnnotationAwareOrderComparator();
    this.advisorComparator = AnnotationAwareOrderComparator.INSTANCE;
}

@Override
public int compare(Advisor o1, Advisor o2) {
    // AnnotationAwareOrderComparator INSTANCE = new AnnotationAwareOrderComparator();
    // 比较优先级，如果优先级一样，判断是否定义在同一个切面类里的增强器。
    // 例如同一个切面类里的@Before、@Around 对应的增强器等。
    int advisorPrecedence = this.advisorComparator.compare(o1, o2);
    if (advisorPrecedence == SAME_PRECEDENCE && declaredInSameAspect(o1, o2)) {
        advisorPrecedence = comparePrecedenceWithinAspect(o1, o2);
    }
    return advisorPrecedence;
}

private int comparePrecedenceWithinAspect(Advisor advisor1, Advisor advisor2) {
    // 判断是否是@AfterReturning、@AfterThrowing、 @After对应增强器
    boolean oneOrOtherIsAfterAdvice =
            (AspectJAopUtils.isAfterAdvice(advisor1) || AspectJAopUtils.isAfterAdvice(advisor2));
    // 由前面的分析可知，对于同一个切面类里的@Before、@Around、 @AfterReturning、@AfterThrowing、 @After对应增强器的declarationOrder都设置成为0
    // 所以adviceDeclarationOrderDelta等于0。       
    int adviceDeclarationOrderDelta = getAspectDeclarationOrder(advisor1) - getAspectDeclarationOrder(advisor2);

    if (oneOrOtherIsAfterAdvice) {
        // the advice declared last has higher precedence
        if (adviceDeclarationOrderDelta < 0) {
            // advice1 was declared before advice2
            // so advice1 has lower precedence
            // 返回1
            return LOWER_PRECEDENCE;
        }
        else if (adviceDeclarationOrderDelta == 0) {
            // 等于0，返回0
            return SAME_PRECEDENCE;
        }
        else {
            // 返回-1
            return HIGHER_PRECEDENCE;
        }
    }
    else {
        // the advice declared first has higher precedence
        if (adviceDeclarationOrderDelta < 0) {
            // advice1 was declared before advice2
            // so advice1 has higher precedence
            return HIGHER_PRECEDENCE;
        }
        else if (adviceDeclarationOrderDelta == 0) {
             // 等于0，返回0
            return SAME_PRECEDENCE;
        }
        else {
            return LOWER_PRECEDENCE;
        }
    }
}

// 是否定义在同一个切面类，比较切面名称是否一样。
// 由上面的分析可知。这里用@Aspect的bean名称.
// 对于同一个切面类里的@Before、@Around、 @AfterReturning、@AfterThrowing、 @After对应增强器的切面名称是一样的。
private boolean declaredInSameAspect(Advisor advisor1, Advisor advisor2) {
    return (hasAspectName(advisor1) && hasAspectName(advisor2) &&
            getAspectName(advisor1).equals(getAspectName(advisor2)));
}

private boolean hasAspectName(Advisor advisor) {
    return (advisor instanceof AspectJPrecedenceInformation ||
            advisor.getAdvice() instanceof AspectJPrecedenceInformation);
}

// pre-condition is that hasAspectName returned true
private String getAspectName(Advisor advisor) {
    AspectJPrecedenceInformation precedenceInfo = AspectJAopUtils.getAspectJPrecedenceInformationFor(advisor);
    Assert.state(precedenceInfo != null, () -> "Unresolvable AspectJPrecedenceInformation for " + advisor);
    return precedenceInfo.getAspectName();
}

// 由前面的分析可知，对于同一个切面类里的@Before、@Around、 @AfterReturning、@AfterThrowing、 @After对应增强器的declarationOrder都设置成为0
private int getAspectDeclarationOrder(Advisor advisor) {
    AspectJPrecedenceInformation precedenceInfo = AspectJAopUtils.getAspectJPrecedenceInformationFor(advisor);
    return (precedenceInfo != null ? precedenceInfo.getDeclarationOrder() : 0);
}

}
```



* AnnotationAwareOrderComparator

```java
AnnotationAwareOrderComparator继承OrderComparator, OrderComparator实现Comparator

@Override
public int compare(@Nullable Object o1, @Nullable Object o2) {
    return doCompare(o1, o2, null);
}

private int doCompare(@Nullable Object o1, @Nullable Object o2, @Nullable OrderSourceProvider sourceProvider) { 
    // 如果 o1 实现了 PriorityOrdered 接口，而 o2 没有实现，doCompare 返回 -1，表示 o1 优先级更高(o1的顺序小)。
	// 如果 o2 实现了 PriorityOrdered 接口，而 o1 没有实现，doCompare 返回 1，表示 o2 优先级更高（o2的顺序小）。
    boolean p1 = (o1 instanceof PriorityOrdered);
    boolean p2 = (o2 instanceof PriorityOrdered);
    if (p1 && !p2) {
        return -1;
    }
    else if (p2 && !p1) {
        return 1;
    }
    // 如果实现了Ordered接口，通过getOrder()方法获取。
    // 否则获取类上@Order注解的，还没有再获取@Priority注解，用于比较。
    int i1 = getOrder(o1, sourceProvider);
    int i2 = getOrder(o2, sourceProvider);
    return Integer.compare(i1, i2);
}

/**
 * This implementation checks for {@link Order @Order} or
 * {@link javax.annotation.Priority @Priority} on various kinds of
 * elements, in addition to the {@link org.springframework.core.Ordered}
 * check in the superclass.
 */
@Override
@Nullable
protected Integer findOrder(Object obj) {
    Integer order = super.findOrder(obj);
    if (order != null) {
        return order;
    }
    return findOrderFromAnnotation(obj);
}
```



## 3、创建动态代理

### 3.1 创建代理工厂 

* AbstractAutoProxyCreator

```java
protected Object createProxy(Class<?> beanClass, @Nullable String beanName,
			@Nullable Object[] specificInterceptors, TargetSource targetSource) {

		if (this.beanFactory instanceof ConfigurableListableBeanFactory) {
             // 给beanName对应的bean定义设置originalTargetClass属性为beanClass
			AutoProxyUtils.exposeTargetClass((ConfigurableListableBeanFactory) this.beanFactory, beanName, beanClass);
		}
	    
         // 1、创建代理工厂   
         // 继承结构： ProxyFactory --> ProxyCreatorSupport --> AdvisedSupport --> ProxyConfig 
         // 构造方法中设置了一个关键的属性 aopProxyFactory = DefaultAopProxyFactory
		ProxyFactory proxyFactory = new ProxyFactory();
         // 【注意】设置关键属性proxyTargetClass，来决定使用的代理方式
		proxyFactory.copyFrom(this);
         
         // 2、确定代理方式。
		if (proxyFactory.isProxyTargetClass()) {
             // 如果proxyTargetClass=true，而且beanClass属性Proxy类型，则获取beanClass的所有接口
             // 然后添加到proxyFactory的interfaces属性中   
             // Explicit handling of JDK proxy targets (for introduction advice scenarios)
			if (Proxy.isProxyClass(beanClass)) {
				// Must allow for introductions; can't just set interfaces to the proxy's interfaces only.
				for (Class<?> ifc : beanClass.getInterfaces()) {
					proxyFactory.addInterface(ifc);
				}
			}
		}
		else {
             // 走到这里说明使用proxyTargetClass=false，使用JDK动态代理            
             // 如果该bean定义的中有preserveTargetClass=true，则设置proxyTargetClass=ture，强制使用cglib动态代理
			// No proxyTargetClass flag enforced, let's apply our default checks...
			if (shouldProxyTargetClass(beanClass, beanName)) {
				proxyFactory.setProxyTargetClass(true);
			}
			else {                
                  // 说明preserveTargetClass=false
                  // 3、求取合理的被代理接口
                  // 因为有些接口是空接口，或者类没有实现接口，使用jdk代理就没意义，所以要改用cglib代理                 
                  // 如果没有被代理类没有合理的被代理接口，则设置proxyTargetClass=ture，使用cglib代理
				evaluateProxyInterfaces(beanClass, proxyFactory);
			}
		}
		
    	// 3、构建增强器
		Advisor[] advisors = buildAdvisors(beanName, specificInterceptors);
		proxyFactory.addAdvisors(advisors);
         // 设置targetSource，targetSource包含原始的对象。这里为SingletonTargetSource
		proxyFactory.setTargetSource(targetSource);
    	// 4、定制化代理工厂
		customizeProxyFactory(proxyFactory);
    
    	// 默认为false
		proxyFactory.setFrozen(this.freezeProxy);
         // 设置增强器前置过滤，默认情况是ture
		if (advisorsPreFiltered()) {
			proxyFactory.setPreFiltered(true);
		}

		// Use original ClassLoader if bean class not locally loaded in overriding class loader
		ClassLoader classLoader = getProxyClassLoader();
		if (classLoader instanceof SmartClassLoader && classLoader != beanClass.getClassLoader()) {
			classLoader = ((SmartClassLoader) classLoader).getOriginalClassLoader();
		}
    	// 5、获取代理类
		return proxyFactory.getProxy(classLoader);
	}



public void copyFrom(ProxyConfig other) {
    Assert.notNull(other, "Other ProxyConfig object must not be null");
    // 这里是属性值为true，见下面截图
    this.proxyTargetClass = other.proxyTargetClass;
    // 在当前示例中，这里我们设置为false。
    this.optimize = other.optimize;
     // 在当前示例中，这里我们设置为false。
    this.exposeProxy = other.exposeProxy;
     // 在当前示例中，这里我们设置为false。
    this.frozen = other.frozen;
     // 在当前示例中，这里我们设置为false。
    this.opaque = other.opaque;
}


/**
 * Determine whether the given bean should be proxied with its target class rather than its interfaces.
 * <p>Checks the {@link AutoProxyUtils#PRESERVE_TARGET_CLASS_ATTRIBUTE "preserveTargetClass" attribute}
 * of the corresponding bean definition.
 * @param beanClass the class of the bean
 * @param beanName the name of the bean
 * @return whether the given bean should be proxied with its target class
 * @see AutoProxyUtils#shouldProxyTargetClass
 */
protected boolean shouldProxyTargetClass(Class<?> beanClass, @Nullable String beanName) {
    return (this.beanFactory instanceof ConfigurableListableBeanFactory &&
            AutoProxyUtils.shouldProxyTargetClass((ConfigurableListableBeanFactory) this.beanFactory, beanName));
}
public static boolean shouldProxyTargetClass(
        ConfigurableListableBeanFactory beanFactory, @Nullable String beanName) {

    if (beanName != null && beanFactory.containsBeanDefinition(beanName)) {
        BeanDefinition bd = beanFactory.getBeanDefinition(beanName);
        return Boolean.TRUE.equals(bd.getAttribute(PRESERVE_TARGET_CLASS_ATTRIBUTE));
    }
    return false;
}
public static final String PRESERVE_TARGET_CLASS_ATTRIBUTE =
			Conventions.getQualifiedAttributeName(AutoProxyUtils.class, "preserveTargetClass");
```

![image-20240515163429723](https://lu-note.oss-cn-shenzhen.aliyuncs.com/notes/work/image-20240515163429723.png)

![image-20240627160336938](https://lu-note.oss-cn-shenzhen.aliyuncs.com/notes/work/image-20240627160336938.png)

![image-20240627160434720](https://lu-note.oss-cn-shenzhen.aliyuncs.com/notes/work/image-20240627160434720.png)

由截图可以知道，spring自动配置会设置proxyTargetClass为true，使用cglib动态代理。如果我们想更改为jdk动态代理，可以在spring的yaml文件或者properties文件加入以下配置，使用JdkDynamicAutoProxyConfiguration配置类，设置proxyTargetClass为false即可。

* yaml

```yaml
spring:
  aop:
    proxy-target-class: false
```

* properties

```java
spring.aop.proxy-target-class=false
```



#### 3.1.1 确定代理方式

```java
// 2、确定代理方式。
if (proxyFactory.isProxyTargetClass()) {
     // 如果proxyTargetClass=true，而且beanClass属性Proxy类型，则获取beanClass的所有接口，然后添加到proxyFactory    		 // Explicit handling of JDK proxy targets (for introduction advice scenarios)
    if (Proxy.isProxyClass(beanClass)) {
        // Must allow for introductions; can't just set interfaces to the proxy's interfaces only.
        for (Class<?> ifc : beanClass.getInterfaces()) {
            proxyFactory.addInterface(ifc);
        }
    }
}
else {
    // 走到这里说明使用proxyTargetClass=false，使用JDK动态代理            
     // 如果该bean定义的中有preserveTargetClass=true，则设置proxyTargetClass=ture，强制使用cglib动态代理
    // No proxyTargetClass flag enforced, let's apply our default checks...
    if (shouldProxyTargetClass(beanClass, beanName)) {
        proxyFactory.setProxyTargetClass(true);
    }
    else {                
        // 说明preserveTargetClass=false
        // 3、求取合理的被代理接口
        // 因为有些接口是空接口，或者类没有实现接口，使用jdk代理就没意义，所以要改用cglib代理                 
        // 如果没有被代理类没有合理的被代理接口，则设置proxyTargetClass=ture，使用cglib代理
        evaluateProxyInterfaces(beanClass, proxyFactory);
    }
}


public static boolean isProxyClass(Class<?> cl) {
    return Proxy.class.isAssignableFrom(cl) && proxyClassCache.containsValue(cl);
}
```



#### 3.1.2 求取合理的被代理接口

```java
/**
 * Check the interfaces on the given bean class and apply them to the {@link ProxyFactory},
 * if appropriate.
 * <p>Calls {@link #isConfigurationCallbackInterface} and {@link #isInternalLanguageInterface}
 * to filter for reasonable proxy interfaces, falling back to a target-class proxy otherwise.
 * @param beanClass the class of the bean
 * @param proxyFactory the ProxyFactory for the bean
 */
protected void evaluateProxyInterfaces(Class<?> beanClass, ProxyFactory proxyFactory) {
    // 获取beanClass实现的所有接口
    Class<?>[] targetInterfaces = ClassUtils.getAllInterfacesForClass(beanClass, getProxyClassLoader());
    boolean hasReasonableProxyInterface = false;
    for (Class<?> ifc : targetInterfaces) {
        /*
        如果满足以下条件，说明是合理的接口
        (1) 接口不属于InitializingBean、DisposableBean、Closeable、AutoCloseable、Aware
       （2）接口不属于GroovyObject、接口名称不以.cglib.proxy.Factory结束，接口名称不以.bytebuddy.MockAccess
       （3）接口不是空接口，接口要有方法
        */       
        if (!isConfigurationCallbackInterface(ifc) && !isInternalLanguageInterface(ifc) &&
                ifc.getMethods().length > 0) {
            hasReasonableProxyInterface = true;
            break;
        }
    }
   
    if (hasReasonableProxyInterface) {
        // 找到合理的接口
        // 添加所有接口到proxyFactory中，作为被代理的接口
        // Must allow for introductions; can't just set interfaces to the target's interfaces only.
        for (Class<?> ifc : targetInterfaces) {
            proxyFactory.addInterface(ifc);
        }
    }
    else {
        // 没有找到合理的接口，设置proxyTargetClass=true，使用cglib代理
        proxyFactory.setProxyTargetClass(true);
    }
}


/**
 * Determine whether the given interface is just a container callback and
 * therefore not to be considered as a reasonable proxy interface.
 * <p>If no reasonable proxy interface is found for a given bean, it will get
 * proxied with its full target class, assuming that as the user's intention.
 * @param ifc the interface to check
 * @return whether the given interface is just a container callback
 */
protected boolean isConfigurationCallbackInterface(Class<?> ifc) {
    return (InitializingBean.class == ifc || DisposableBean.class == ifc || Closeable.class == ifc ||
            AutoCloseable.class == ifc || ObjectUtils.containsElement(ifc.getInterfaces(), Aware.class));
}

/**
 * Determine whether the given interface is a well-known internal language interface
 * and therefore not to be considered as a reasonable proxy interface.
 * <p>If no reasonable proxy interface is found for a given bean, it will get
 * proxied with its full target class, assuming that as the user's intention.
 * @param ifc the interface to check
 * @return whether the given interface is an internal language interface
 */
protected boolean isInternalLanguageInterface(Class<?> ifc) {
    return (ifc.getName().equals("groovy.lang.GroovyObject") ||
            ifc.getName().endsWith(".cglib.proxy.Factory") ||
            ifc.getName().endsWith(".bytebuddy.MockAccess"));
}
```



#### 3.1.3 构建增强器

```java
/**
 * Determine the advisors for the given bean, including the specific interceptors
 * as well as the common interceptor, all adapted to the Advisor interface.
 * @param beanName the name of the bean
 * @param specificInterceptors the set of interceptors that is
 * specific to this bean (may be empty, but not null)
 * @return the list of Advisors for the given bean
 */
protected Advisor[] buildAdvisors(@Nullable String beanName, @Nullable Object[] specificInterceptors) {
    // Handle prototypes correctly...
    // 1、解析常用的增强器(默认情况下是没有的)
    Advisor[] commonInterceptors = resolveInterceptorNames();

    // 2、保存所有的增强器
    List<Object> allInterceptors = new ArrayList<>();
    if (specificInterceptors != null) {
        if (specificInterceptors.length > 0) {
            // 添加具体的增强器
            // specificInterceptors may equal PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS
            allInterceptors.addAll(Arrays.asList(specificInterceptors));
        }
        // 如果有常用的增强器
        if (commonInterceptors.length > 0) {
            // applyCommonInterceptorsFirst=true，默认添加常用的增强器在开头的位置
            if (this.applyCommonInterceptorsFirst) {
                allInterceptors.addAll(0, Arrays.asList(commonInterceptors));
            }
            else {
                allInterceptors.addAll(Arrays.asList(commonInterceptors));
            }
        }
    }
    if (logger.isTraceEnabled()) {
        int nrOfCommonInterceptors = commonInterceptors.length;
        int nrOfSpecificInterceptors = (specificInterceptors != null ? specificInterceptors.length : 0);
        logger.trace("Creating implicit proxy for bean '" + beanName + "' with " + nrOfCommonInterceptors +
                " common interceptors and " + nrOfSpecificInterceptors + " specific interceptors");
    }

    // 3、包装增强器，如果有必要的话
    Advisor[] advisors = new Advisor[allInterceptors.size()];
    for (int i = 0; i < allInterceptors.size(); i++) {
        // DefaultAdvisorAdapterRegistry
        advisors[i] = this.advisorAdapterRegistry.wrap(allInterceptors.get(i));
    }
    return advisors;
}

/** Default is no common interceptors. */
private String[] interceptorNames = new String[0];
/**
 * Resolves the specified interceptor names to Advisor objects.
 * @see #setInterceptorNames
 */
private Advisor[] resolveInterceptorNames() {
    BeanFactory bf = this.beanFactory;
    ConfigurableBeanFactory cbf = (bf instanceof ConfigurableBeanFactory ? (ConfigurableBeanFactory) bf : null);
    List<Advisor> advisors = new ArrayList<>();
    for (String beanName : this.interceptorNames) {
        if (cbf == null || !cbf.isCurrentlyInCreation(beanName)) {
            Assert.state(bf != null, "BeanFactory required for resolving interceptor names");
            Object next = bf.getBean(beanName);
            advisors.add(this.advisorAdapterRegistry.wrap(next));
        }
    }
    return advisors.toArray(new Advisor[0]);
}


// DefaultAdvisorAdapterRegistry
@Override
public Advisor wrap(Object adviceObject) throws UnknownAdviceTypeException {
    // 我们这次示例中的增强器都属于Advisor类型，直接返回即可
    if (adviceObject instanceof Advisor) {
        return (Advisor) adviceObject;
    }
    if (!(adviceObject instanceof Advice)) {
        throw new UnknownAdviceTypeException(adviceObject);
    }
    Advice advice = (Advice) adviceObject;
    if (advice instanceof MethodInterceptor) {
        // So well-known it doesn't even need an adapter.
        return new DefaultPointcutAdvisor(advice);
    }
    for (AdvisorAdapter adapter : this.adapters) {
        // Check that it is supported.
        if (adapter.supportsAdvice(advice)) {
            return new DefaultPointcutAdvisor(advice);
        }
    }
    throw new UnknownAdviceTypeException(advice);
}
```



#### 3.1.4 定制化代理工厂

```java
/**
 * 子类可以选择实现这个方法。
 * 例如，可以通过实现这个方法来改变暴露的接口。从而改变被代理的接口，当然也可以加日志什么的。
 * 默认实现是空的
 * Subclasses may choose to implement this: for example,
 * to change the interfaces exposed.
 * <p>The default implementation is empty.
 * @param proxyFactory a ProxyFactory that is already configured with
 * TargetSource and interfaces and will be used to create the proxy
 * immediately after this method returns
 */
protected void customizeProxyFactory(ProxyFactory proxyFactory) {
}
```



### 3.2 获取代理类

* ProxyFactory

```java
/**
 * Create a new proxy according to the settings in this factory.
 * <p>Can be called repeatedly. Effect will vary if we've added
 * or removed interfaces. Can add and remove interceptors.
 * <p>Uses the given class loader (if necessary for proxy creation).
 * @param classLoader the class loader to create the proxy with
 * (or {@code null} for the low-level proxy facility's default)
 * @return the proxy object
 */
public Object getProxy(@Nullable ClassLoader classLoader) {
    return createAopProxy().getProxy(classLoader);
}
```

```java
/**
 * Subclasses should call this to get a new AOP proxy. They should <b>not</b>
 * create an AOP proxy with {@code this} as an argument.
 */
protected final synchronized AopProxy createAopProxy() {
    if (!this.active) {
        // 调用所有的AdvisedSupportListener的activated方法，通知代理准备创建
        activate();
    }
    // getAopProxyFactory()返回DefaultAopProxyFactory
    return getAopProxyFactory().createAopProxy(this);
}

/**
 * Return the AopProxyFactory that this ProxyConfig uses.
 */
public AopProxyFactory getAopProxyFactory() {
    // 创建ProxyFactory的时候，可以知道这里是DefaultAopProxyFactory
    return this.aopProxyFactory;
}
```



#### 3.2.1 确定使用的代理类

```java
@Override
public AopProxy createAopProxy(AdvisedSupport config) throws AopConfigException {
    /*
    （1）获取系统属性org.graalvm.nativeimage.imagecode，默认是没有的
    （2）optimize为false，在创建proxyFactory的时候设置的
    （3）获取proxyFactory的proxyTargetClass属性
    （4）是否有合理的被代理接口
    */       
    if (!NativeDetector.inNativeImage() &&
            (config.isOptimize() || config.isProxyTargetClass() || hasNoUserSuppliedProxyInterfaces(config))) {             Class<?> targetClass = config.getTargetClass();
        if (targetClass == null) {
            throw new AopConfigException("TargetSource cannot determine target class: " +
                    "Either an interface or a target is required for proxy creation.");
        }
        // 被代理的类为接口，或者属于Proxy类型，则创建JdkDynamicAopProxy，表示使用jdk动态代理          
        if (targetClass.isInterface() || Proxy.isProxyClass(targetClass)) {
            return new JdkDynamicAopProxy(config);
        }
        // ObjenesisCglibAopProxy继承CglibAopProxy ，表示使用cglib动态代理
        return new ObjenesisCglibAopProxy(config);
    }
    else {
        // 以上条件不满足，则创建JdkDynamicAopProxy，表示jdk动态代理
        return new JdkDynamicAopProxy(config);
    }
}

public abstract class NativeDetector {
	// See https://github.com/oracle/graal/blob/master/sdk/src/org.graalvm.nativeimage/src/org/graalvm/nativeimage/ImageInfo.java
	private static final boolean imageCode = (System.getProperty("org.graalvm.nativeimage.imagecode") != null);

	/**
	 * Returns {@code true} if invoked in the context of image building or during image runtime, else {@code false}.
	 */
	public static boolean inNativeImage() {
		return imageCode;
	}
}


/**
 * Determine whether the supplied {@link AdvisedSupport} has only the
 * {@link org.springframework.aop.SpringProxy} interface specified
 * (or no proxy interfaces specified at all).
 */
private boolean hasNoUserSuppliedProxyInterfaces(AdvisedSupport config) {
    // 获取所有被代理的接口，从上面的3.1.2时候就计算好了。
    Class<?>[] ifcs = config.getProxiedInterfaces();
    // 如果没有被代理的接口，或者只有一个被代理的接口而且为SpringProxy类型，返回true
    return (ifcs.length == 0 || (ifcs.length == 1 && SpringProxy.class.isAssignableFrom(ifcs[0])));
}
```

*  JdkDynamicAopProxy

![image-20240627223603226](img\image-20240627223603226.png)

这里我们发现JdkDynamicAopProxy实现了jdk动态代理的InvocationHandler接口

```java
/**
 * Construct a new JdkDynamicAopProxy for the given AOP configuration.
 * @param config the AOP configuration as AdvisedSupport object
 * @throws AopConfigException if the config is invalid. We try to throw an informative
 * exception in this case, rather than let a mysterious failure happen later.
 */
public JdkDynamicAopProxy(AdvisedSupport config) throws AopConfigException {
    Assert.notNull(config, "AdvisedSupport must not be null");
    if (config.getAdvisorCount() == 0 && config.getTargetSource() == AdvisedSupport.EMPTY_TARGET_SOURCE) {
        throw new AopConfigException("No advisors and no TargetSource specified");
    }
    this.advised = config;
    // 获取完整的代理接口
    this.proxiedInterfaces = AopProxyUtils.completeProxiedInterfaces(this.advised, true);
    // 如果被代理的接口存在equals方法，设置equalsDefined属性为true
    // 如果被代理的接口存在HashCode方法，设置hashCodeDefined属性为true
    // 表示有相关的方法存在
    findDefinedEqualsAndHashCodeMethods(this.proxiedInterfaces);
}

/**
 * Determine the complete set of interfaces to proxy for the given AOP configuration.
 * <p>This will always add the {@link Advised} interface unless the AdvisedSupport's
 * {@link AdvisedSupport#setOpaque "opaque"} flag is on. Always adds the
 * {@link org.springframework.aop.SpringProxy} marker interface.
 * @param advised the proxy config
 * @param decoratingProxy whether to expose the {@link DecoratingProxy} interface
 * @return the complete set of interfaces to proxy
 * @since 4.3
 * @see SpringProxy
 * @see Advised
 * @see DecoratingProxy
 */
static Class<?>[] completeProxiedInterfaces(AdvisedSupport advised, boolean decoratingProxy) {
    // 获取3.1.2处合理的被代理接口
    Class<?>[] specifiedInterfaces = advised.getProxiedInterfaces();
    if (specifiedInterfaces.length == 0) {
        // No user-specified interfaces: check whether target class is an interface.
        Class<?> targetClass = advised.getTargetClass();
        if (targetClass != null) {
            // 目标类是接口
            if (targetClass.isInterface()) {
                // 设置目标类为被代理接口
                advised.setInterfaces(targetClass);
            }
            else if (Proxy.isProxyClass(targetClass)) {
                advised.setInterfaces(targetClass.getInterfaces());
            }
            specifiedInterfaces = advised.getProxiedInterfaces();
        }
    }
    List<Class<?>> proxiedInterfaces = new ArrayList<>(specifiedInterfaces.length + 3);
    for (Class<?> ifc : specifiedInterfaces) {
        // Only non-sealed interfaces are actually eligible for JDK proxying (on JDK 17)
        if (isSealedMethod == null || Boolean.FALSE.equals(ReflectionUtils.invokeMethod(isSealedMethod, ifc))) {
            proxiedInterfaces.add(ifc);
        }
    }
    // 这里advised=ProxyFactory，不属于SpringProxy类型，额外添加SpringProxy作为为被代理的接口
    if (!advised.isInterfaceProxied(SpringProxy.class)) {
        proxiedInterfaces.add(SpringProxy.class);
    }
    // 这里advised=ProxyFactory，默认情况下!advised.isOpaque()，不属于Advised类型，额外添加Advised作为为被代理的接口
    if (!advised.isOpaque() && !advised.isInterfaceProxied(Advised.class)) {
        proxiedInterfaces.add(Advised.class);
    }
// decoratingProxy为true，这里advised=ProxyFactory，不属于DecoratingProxy类型，额外添加DecoratingProxy作为为被代理的接口
    if (decoratingProxy && !advised.isInterfaceProxied(DecoratingProxy.class)) {
        proxiedInterfaces.add(DecoratingProxy.class);
    }
    // 返回完整的被代理接口
    return ClassUtils.toClassArray(proxiedInterfaces);
}


/**
 * Finds any {@link #equals} or {@link #hashCode} method that may be defined
 * on the supplied set of interfaces.
 * @param proxiedInterfaces the interfaces to introspect
 */
private void findDefinedEqualsAndHashCodeMethods(Class<?>[] proxiedInterfaces) {
    for (Class<?> proxiedInterface : proxiedInterfaces) {
        /**
        Returns an array containing Method objects reflecting 
        all the declared methods of the class or interface represented by this Class object,
        including public, protected, default (package) access, and private methods, 
        but excluding inherited methods.
        它包括类中所有声明的方法（不包含继承但是没有重写的方法）
        */
        Method[] methods = proxiedInterface.getDeclaredMethods();
        for (Method method : methods) {
            if (AopUtils.isEqualsMethod(method)) {
                this.equalsDefined = true;
            }
            if (AopUtils.isHashCodeMethod(method)) {
                this.hashCodeDefined = true;
            }
            if (this.equalsDefined && this.hashCodeDefined) {
                return;
            }
        }
    }
}
```





*  ObjenesisCglibAopProxy

![image-20240627223657354](https://lu-note.oss-cn-shenzhen.aliyuncs.com/notes/work/image-20240627223657354.png)

```java
/**
 * Create a new ObjenesisCglibAopProxy for the given AOP configuration.
 * @param config the AOP configuration as AdvisedSupport object
 */
public ObjenesisCglibAopProxy(AdvisedSupport config) {
    super(config);
}

class CglibAopProxy implements AopProxy, Serializable {
    // CGLIB回调数组索引的常量
    // Constants for CGLIB callback array indices
	private static final int AOP_PROXY = 0;
	private static final int INVOKE_TARGET = 1;
	private static final int NO_OVERRIDE = 2;
	private static final int DISPATCH_TARGET = 3;
	private static final int DISPATCH_ADVISED = 4;
	private static final int INVOKE_EQUALS = 5;
	private static final int INVOKE_HASHCODE = 6;
    /** The configuration used to configure this proxy. */
	protected final AdvisedSupport advised;
    private final transient AdvisedDispatcher advisedDispatcher;
     /**
	 * Create a new CglibAopProxy for the given AOP configuration.
	 * @param config the AOP configuration as AdvisedSupport object
	 * @throws AopConfigException if the config is invalid. We try to throw an informative
	 * exception in this case, rather than let a mysterious failure happen later.
	 */
	public CglibAopProxy(AdvisedSupport config) throws AopConfigException {
		Assert.notNull(config, "AdvisedSupport must not be null");
		if (config.getAdvisorCount() == 0 && config.getTargetSource() == AdvisedSupport.EMPTY_TARGET_SOURCE) {
			throw new AopConfigException("No advisors and no TargetSource specified");
		}
		this.advised = config;
		this.advisedDispatcher = new AdvisedDispatcher(this.advised);
	}
}
```



【注意】

1、默认情况下，由3.1.1处可知，使用ObjenesisCglibAopProxy

2、在spring的yaml文件或者properties文件以下配置，会加载JdkDynamicAutoProxyConfiguration配置类，设置proxyTargetClass为false，然后使用JdkDynamicAopProxy

* yaml

```yaml
spring:
  aop:
    proxy-target-class: false
```

* properties

```java
spring.aop.proxy-target-class=false
```



#### 3.2.2 总结

```
1、如果proxyTargetClass = true，被代理的不属于接口，被代理的不属于Proxy类及其子类，则使用cglib动态代理，否则使用jdk动态代理。
2、如果proxyTargetClass = false，被代理的对象的bean定义没有preserveTargetClass=true，则使用jdk动态代理
3、如果proxyTargetClass = false，被代理的类没有合理的被代理接口，则使用cglib动态代理
合理的被代理接口：
 (1)  接口不属于InitializingBean、DisposableBean、Closeable、AutoCloseable、Aware
（2）接口不属于GroovyObject、接口名称不以.cglib.proxy.Factory结束，接口名称不以.bytebuddy.MockAccess
（3）接口不是空接口，接口要有方法
（4）接口不属于SpringProxy及其子类
```



### 3.3 创建具体的代理类实例

#### 3.3.1 JdkDynamicAopProxy创建实例

```java
@Override
public Object getProxy(@Nullable ClassLoader classLoader) {
    if (logger.isTraceEnabled()) {
        logger.trace("Creating JDK dynamic proxy: " + this.advised.getTargetSource());
    }
    // 这里就是jdk原生的创建动态代理对象的方法
    // this.proxiedInterfaces 是由上面的3.1.2获取到的被代理接口
    // this表示当前JdkDynamicAopProxy对象，因为JdkDynamicAopProxy实现了InvocationHandler接口
    return Proxy.newProxyInstance(classLoader, this.proxiedInterfaces, this);
}
```

#### 3.3.2 ObjenesisCglibAopProxy创建实例

```java
/**
CGLIB类分隔符
The CGLIB class separator: {@code "$$"}. 
*/
public static final String CGLIB_CLASS_SEPARATOR = "$$";


@Override
public Object getProxy(@Nullable ClassLoader classLoader) {
    if (logger.isTraceEnabled()) {
        logger.trace("Creating CGLIB proxy: " + this.advised.getTargetSource());
    }

    try {
        // 这里advised=ProxyFactory，获取被代理类
        Class<?> rootClass = this.advised.getTargetClass();
        Assert.state(rootClass != null, "Target class must be available for creating a CGLIB proxy");

        Class<?> proxySuperClass = rootClass;
        // 如果被代理类已经是cglib的代理类，则获取它的父类作为被代理类，然后获取被代理的接口，用于代理类实现。
        // 一般情况这里都会是false
        if (rootClass.getName().contains(ClassUtils.CGLIB_CLASS_SEPARATOR)) {
            proxySuperClass = rootClass.getSuperclass();
            Class<?>[] additionalInterfaces = rootClass.getInterfaces();
            for (Class<?> additionalInterface : additionalInterfaces) {
                this.advised.addInterface(additionalInterface);
            }
        }
		
        // 1、校验被代理类。输出日志记录一些不能被代理的方法
        // Validate the class, writing log messages as necessary.
        validateClassIfNecessary(proxySuperClass, classLoader);

        // Configure CGLIB Enhancer...
        Enhancer enhancer = createEnhancer();
        if (classLoader != null) {
            enhancer.setClassLoader(classLoader);
            if (classLoader instanceof SmartClassLoader &&
                    ((SmartClassLoader) classLoader).isClassReloadable(proxySuperClass)) {
                enhancer.setUseCache(false);
            }
        }
        // 设置被代理的类
        enhancer.setSuperclass(proxySuperClass);
        // 设置被代理的接口。
        enhancer.setInterfaces(AopProxyUtils.completeProxiedInterfaces(this.advised));
        // SpringNamingPolicy
        enhancer.setNamingPolicy(SpringNamingPolicy.INSTANCE);
        enhancer.setStrategy(new ClassLoaderAwareGeneratorStrategy(classLoader));

        // 2、获取代理回调
        Callback[] callbacks = getCallbacks(rootClass);
        Class<?>[] types = new Class<?>[callbacks.length];
        for (int x = 0; x < types.length; x++) {
            types[x] = callbacks[x].getClass();
        }
        
        // 3、设置代理回调过滤器
        //  fixedInterceptorMap只在getCallbacks调用之后被填充
        // fixedInterceptorMap only populated at this point, after getCallbacks call above        
        enhancer.setCallbackFilter(new ProxyCallbackFilter(
                this.advised.getConfigurationOnlyCopy(), this.fixedInterceptorMap, this.fixedInterceptorOffset));
        // 设置回调的类型
        enhancer.setCallbackTypes(types);
        
        // 4、实例化代理对象
        // Generate the proxy class and create a proxy instance.
        return createProxyClassAndInstance(enhancer, callbacks);
    }
    catch (CodeGenerationException | IllegalArgumentException ex) {
        throw new AopConfigException("Could not generate CGLIB subclass of " + this.advised.getTargetClass() +
                ": Common causes of this problem include using a final class or a non-visible class",
                ex);
    }
    catch (Throwable ex) {
        // TargetSource.getTarget() failed
        throw new AopConfigException("Unexpected AOP exception", ex);
    }
}



/**
 * Determine the complete set of interfaces to proxy for the given AOP configuration.
 * <p>This will always add the {@link Advised} interface unless the AdvisedSupport's
 * {@link AdvisedSupport#setOpaque "opaque"} flag is on. Always adds the
 * {@link org.springframework.aop.SpringProxy} marker interface.
 * @param advised the proxy config
 * @return the complete set of interfaces to proxy
 * @see SpringProxy
 * @see Advised
 */
public static Class<?>[] completeProxiedInterfaces(AdvisedSupport advised) {
    return completeProxiedInterfaces(advised, false);
}

/**
 * Determine the complete set of interfaces to proxy for the given AOP configuration.
 * <p>This will always add the {@link Advised} interface unless the AdvisedSupport's
 * {@link AdvisedSupport#setOpaque "opaque"} flag is on. Always adds the
 * {@link org.springframework.aop.SpringProxy} marker interface.
 * @param advised the proxy config
 * @param decoratingProxy whether to expose the {@link DecoratingProxy} interface
 * @return the complete set of interfaces to proxy
 * @since 4.3
 * @see SpringProxy
 * @see Advised
 * @see DecoratingProxy
 */
static Class<?>[] completeProxiedInterfaces(AdvisedSupport advised, boolean decoratingProxy) {
    // cglib代理这里没有经过求取合理的被代理接口，获取的接口为0
    Class<?>[] specifiedInterfaces = advised.getProxiedInterfaces();
    if (specifiedInterfaces.length == 0) {
        // No user-specified interfaces: check whether target class is an interface.
        Class<?> targetClass = advised.getTargetClass();
        if (targetClass != null) {
            // 如果目标类是接口，则添加目标类
            if (targetClass.isInterface()) {
                advised.setInterfaces(targetClass);
            }
            // 如果目前类属于Proxy类型，则获取目标接口
            else if (Proxy.isProxyClass(targetClass)) {
                advised.setInterfaces(targetClass.getInterfaces());
            }
            // 使用cglib代理，一般情况下，上面两个if都不执行，这里获取的接口还是0
            specifiedInterfaces = advised.getProxiedInterfaces();
        }
    }
    List<Class<?>> proxiedInterfaces = new ArrayList<>(specifiedInterfaces.length + 3);
    for (Class<?> ifc : specifiedInterfaces) {
        // Only non-sealed interfaces are actually eligible for JDK proxying (on JDK 17)
        if (isSealedMethod == null || Boolean.FALSE.equals(ReflectionUtils.invokeMethod(isSealedMethod, ifc))) {
            proxiedInterfaces.add(ifc);
        }
    }
     // 这里advised=ProxyFactory，不属于SpringProxy类型，添加SpringProxy作为为被代理的接口
    if (!advised.isInterfaceProxied(SpringProxy.class)) {
        proxiedInterfaces.add(SpringProxy.class);
    }
     //!advised.isOpaque()为true，这里advised=ProxyFactory，不属于Advised类型，添加Advised作为为被代理的接口
    if (!advised.isOpaque() && !advised.isInterfaceProxied(Advised.class)) {
        proxiedInterfaces.add(Advised.class);
    }
    // 不添加
    if (decoratingProxy && !advised.isInterfaceProxied(DecoratingProxy.class)) {
        proxiedInterfaces.add(DecoratingProxy.class);
    }
    return ClassUtils.toClassArray(proxiedInterfaces);
}
```

##### 3.3.2.1 校验

```java
/**
 * Checks to see whether the supplied {@code Class} has already been validated and
 * validates it if not.
 */
private void validateClassIfNecessary(Class<?> proxySuperClass, @Nullable ClassLoader proxyClassLoader) {
    if (!this.advised.isOptimize() && logger.isInfoEnabled()) {
        synchronized (validatedClasses) {
            if (!validatedClasses.containsKey(proxySuperClass)) {
                doValidateClass(proxySuperClass, proxyClassLoader,
                        ClassUtils.getAllInterfacesForClassAsSet(proxySuperClass));
                validatedClasses.put(proxySuperClass, Boolean.TRUE);
            }
        }
    }
}

/**
 * Checks for final methods on the given {@code Class}, as well as package-visible
 * methods across ClassLoaders, and writes warnings to the log for each one found.
 */
private void doValidateClass(Class<?> proxySuperClass, @Nullable ClassLoader proxyClassLoader, Set<Class<?>> ifcs) {
    if (proxySuperClass != Object.class) {
        Method[] methods = proxySuperClass.getDeclaredMethods();
        for (Method method : methods) {
            int mod = method.getModifiers();
            // 方法不是静态和私有的
            if (!Modifier.isStatic(mod) && !Modifier.isPrivate(mod)) {
                if (Modifier.isFinal(mod)) {
                    // final方法不能被cglib代理，记录相关日志
                    if (logger.isInfoEnabled() && implementsInterface(method, ifcs)) {
                        logger.info("Unable to proxy interface-implementing method [" + method + "] because " +
                                "it is marked as final: Consider using interface-based JDK proxies instead!");
                    }
                    if (logger.isDebugEnabled()) {
                        logger.debug("Final method [" + method + "] cannot get proxied via CGLIB: " +
                                "Calls to this method will NOT be routed to the target instance and " +
                                "might lead to NPEs against uninitialized fields in the proxy instance.");
                    }
                }
                // 访问权限不是public和protect的，即缺省方法，不能被cglib代理，记录相关日志
                else if (logger.isDebugEnabled() && !Modifier.isPublic(mod) && !Modifier.isProtected(mod) &&
                        proxyClassLoader != null && proxySuperClass.getClassLoader() != proxyClassLoader) {
                    logger.debug("Method [" + method + "] is package-visible across different ClassLoaders " +
                            "and cannot get proxied via CGLIB: Declare this method as public or protected " +
                            "if you need to support invocations through the proxy.");
                }
            }
        }
        doValidateClass(proxySuperClass.getSuperclass(), proxyClassLoader, ifcs);
    }
}
```

##### 3.3.2.2 创建Enhancer

###### （1） 设置被代理类

```java
// 设置被代理的类
enhancer.setSuperclass(proxySuperClass);
```

###### （2） 获取代理回调

```java
// CGLIB回调数组索引的常量
// Constants for CGLIB callback array indices
private static final int AOP_PROXY = 0;
private static final int INVOKE_TARGET = 1;
private static final int NO_OVERRIDE = 2;
private static final int DISPATCH_TARGET = 3;
private static final int DISPATCH_ADVISED = 4;
private static final int INVOKE_EQUALS = 5;
private static final int INVOKE_HASHCODE = 6;

private Callback[] getCallbacks(Class<?> rootClass) throws Exception {
    // Parameters used for optimization choices...
    // 默认为false
    boolean exposeProxy = this.advised.isExposeProxy();
    // 默认为false
    boolean isFrozen = this.advised.isFrozen();
     // SingletonTargetSource返回true
    boolean isStatic = this.advised.getTargetSource().isStatic();

    // 【重点】选择一个“aop”拦截器(用于aop调用)。这个用于aop的调用，所以我们重点关注这个即可
    // Choose an "aop" interceptor (used for AOP calls).
    Callback aopInterceptor = new DynamicAdvisedInterceptor(this.advised);

    // 获取到StaticUnadvisedInterceptor
    // Choose a "straight to target" interceptor. (used for calls that are
    // unadvised but can return this). May be required to expose the proxy.
    Callback targetInterceptor;
    if (exposeProxy) {
        targetInterceptor = (isStatic ?
                new StaticUnadvisedExposedInterceptor(this.advised.getTargetSource().getTarget()) :
                new DynamicUnadvisedExposedInterceptor(this.advised.getTargetSource()));
    }
    else {      
        targetInterceptor = (isStatic ?
                new StaticUnadvisedInterceptor(this.advised.getTargetSource().getTarget()) :
                new DynamicUnadvisedInterceptor(this.advised.getTargetSource()));
    }

    //选择一个“直接到目标”调度程序(用于不建议调用不能返回this的静态目标)。
    // Choose a "direct to target" dispatcher (used for
    // unadvised calls to static targets that cannot return this).
    Callback targetDispatcher = (isStatic ?
            new StaticDispatcher(this.advised.getTargetSource().getTarget()) : new SerializableNoOp());
	
    // 【注意】这里的位置对应 CGLIB回调数组索引的常量
    Callback[] mainCallbacks = new Callback[] {
            aopInterceptor,  // for normal advice 正常的通知的拦截器
            targetInterceptor,  // invoke target without considering advice, if optimized
            new SerializableNoOp(),  // no override for methods mapped to this。 对应没有重写的方法
            targetDispatcher, this.advisedDispatcher,  // advisedDispatcher=AdvisedDispatcher
            new EqualsInterceptor(this.advised), // equals方法的拦截器
            new HashCodeInterceptor(this.advised) // hashcode方法的拦截器
    };

    Callback[] callbacks;

    // If the target is a static one and the advice chain is frozen,
    // then we can make some optimizations by sending the AOP calls
    // direct to the target using the fixed chain for that method.
    if (isStatic && isFrozen) {
        Method[] methods = rootClass.getMethods();
        Callback[] fixedCallbacks = new Callback[methods.length];
        this.fixedInterceptorMap = CollectionUtils.newHashMap(methods.length);

        // TODO: small memory optimization here (can skip creation for methods with no advice)
        for (int x = 0; x < methods.length; x++) {
            Method method = methods[x];
            List<Object> chain = this.advised.getInterceptorsAndDynamicInterceptionAdvice(method, rootClass);
            fixedCallbacks[x] = new FixedChainStaticTargetInterceptor(
                    chain, this.advised.getTargetSource().getTarget(), this.advised.getTargetClass());
            this.fixedInterceptorMap.put(method, x);
        }

        // Now copy both the callbacks from mainCallbacks
        // and fixedCallbacks into the callbacks array.
        callbacks = new Callback[mainCallbacks.length + fixedCallbacks.length];
        System.arraycopy(mainCallbacks, 0, callbacks, 0, mainCallbacks.length);
        System.arraycopy(fixedCallbacks, 0, callbacks, mainCallbacks.length, fixedCallbacks.length);
        this.fixedInterceptorOffset = mainCallbacks.length;
    }
    else {
        // 执行到这里
        callbacks = mainCallbacks;
    }
    // 返回回调数组
    return callbacks;
}
```

###### （3） 设置代理回调过滤器

* ProxyCallbackFilter

```
回调函数的确定分别按下面所说
（1）对于公开的代理
公开代理需要在方法/链调用之前和之后执行代码。这意味着我们必须使用DynamicAdvisedInterceptor，因为所有其他拦截器都可以避免使用try/catch块
（2）对于Object.finalize ():
不使用此方法的重写。
（3）equals():
EqualsInterceptor用于将equals()调用重定向到该代理的特殊处理程序。
（4）对于Advised类上的方法:
AdvisedDispatcher用于将调用直接分派到目标
（5）建议的方法:
如果目标是静态的，并且通知链被冻结，则使用特定于该方法的FixedChainStaticTargetInterceptor来调用通知链。否则将使用DynamicAdvisedInterceptor。
（6）对于非建议方法:
如果可以确定该方法不会返回this，或者当ProxyFactory.getExposeProxy()返回false时，则使用Dispatcher。对于静态目标，使用StaticDispatcher;对于动态目标，使用DynamicUnadvisedInterceptor。如果方法可能返回此值，则静态目标使用StaticUnadvisedInterceptor - DynamicUnadvisedInterceptor已经考虑到这一点。
```

```java
// CGLIB回调数组索引的常量
// Constants for CGLIB callback array indices
private static final int AOP_PROXY = 0;
private static final int INVOKE_TARGET = 1;
private static final int NO_OVERRIDE = 2;
private static final int DISPATCH_TARGET = 3;
private static final int DISPATCH_ADVISED = 4;
private static final int INVOKE_EQUALS = 5;
private static final int INVOKE_HASHCODE = 6;

/**
 * 实现CallbackFilter.accept()以返回我们需要的回调的索引。
 * Implementation of CallbackFilter.accept() to return the index of the
 * callback we need.
 * <p>The callbacks for each proxy are built up of a set of fixed callbacks
 * for general use and then a set of callbacks that are specific to a method
 * for use on static targets with a fixed advice chain. 
 *
 *  回调函数的确定分别按下面所说：我们看翻译都大概可以知道什么情况对应什么类型的回调
 * 
 * <p>The callback used is determined thus:
 * <dl>
 * <dt>For exposed proxies</dt>
 * <dd>Exposing the proxy requires code to execute before and after the
 * method/chain invocation. This means we must use
 * DynamicAdvisedInterceptor, since all other interceptors can avoid the
 * need for a try/catch block</dd>
 * <dt>For Object.finalize():</dt>
 * <dd>No override for this method is used.</dd>
 * <dt>For equals():</dt>
 * <dd>The EqualsInterceptor is used to redirect equals() calls to a
 * special handler to this proxy.</dd>
 * <dt>For methods on the Advised class:</dt>
 * <dd>the AdvisedDispatcher is used to dispatch the call directly to
 * the target</dd>
 * <dt>For advised methods:</dt>
 * <dd>If the target is static and the advice chain is frozen then a
 * FixedChainStaticTargetInterceptor specific to the method is used to
 * invoke the advice chain. Otherwise a DynamicAdvisedInterceptor is
 * used.</dd>
 * <dt>For non-advised methods:</dt>
 * <dd>Where it can be determined that the method will not return {@code this}
 * or when {@code ProxyFactory.getExposeProxy()} returns {@code false},
 * then a Dispatcher is used. For static targets, the StaticDispatcher is used;
 * and for dynamic targets, a DynamicUnadvisedInterceptor is used.
 * If it possible for the method to return {@code this} then a
 * StaticUnadvisedInterceptor is used for static targets - the
 * DynamicUnadvisedInterceptor already considers this.</dd>
 * </dl>
 */
@Override
public int accept(Method method) {
    if (AopUtils.isFinalizeMethod(method)) {
        logger.trace("Found finalize() method - using NO_OVERRIDE");
        // 返回finalize方法的回调索引
        return NO_OVERRIDE;
    }
    if (!this.advised.isOpaque() && method.getDeclaringClass().isInterface() &&
            method.getDeclaringClass().isAssignableFrom(Advised.class)) {
        if (logger.isTraceEnabled()) {
            logger.trace("Method is declared on Advised interface: " + method);
        }
        //如果是Advised接口里的方法， 返回DISPATCH_ADVISED
        return DISPATCH_ADVISED;
    }
    // We must always proxy equals, to direct calls to this.
    if (AopUtils.isEqualsMethod(method)) {
        if (logger.isTraceEnabled()) {
            logger.trace("Found 'equals' method: " + method);
        }
        // 返回equals方法的回调索引
        return INVOKE_EQUALS;
    }
    // We must always calculate hashCode based on the proxy.
    if (AopUtils.isHashCodeMethod(method)) {
        if (logger.isTraceEnabled()) {
            logger.trace("Found 'hashCode' method: " + method);
        }
        // 返回hashCode方法的回调索引
        return INVOKE_HASHCODE;
    }
    Class<?> targetClass = this.advised.getTargetClass();
    // (3-1)获取拦截器链
    // Proxy is not yet available, but that shouldn't matter.
    List<?> chain = this.advised.getInterceptorsAndDynamicInterceptionAdvice(method, targetClass);
    boolean haveAdvice = !chain.isEmpty();
    boolean exposeProxy = this.advised.isExposeProxy();
    boolean isStatic = this.advised.getTargetSource().isStatic();
    boolean isFrozen = this.advised.isFrozen();
    if (haveAdvice || !isFrozen) {
        // If exposing the proxy, then AOP_PROXY must be used.
        if (exposeProxy) {
            if (logger.isTraceEnabled()) {
                logger.trace("Must expose proxy on advised method: " + method);
            }           
            return AOP_PROXY;
        }
        // Check to see if we have fixed interceptor to serve this method.
        // Else use the AOP_PROXY.
        if (isStatic && isFrozen && this.fixedInterceptorMap.containsKey(method)) {
            if (logger.isTraceEnabled()) {
                logger.trace("Method has advice and optimizations are enabled: " + method);
            }
            // We know that we are optimizing so we can use the FixedStaticChainInterceptors.
            int index = this.fixedInterceptorMap.get(method);
            return (index + this.fixedInterceptorOffset);
        }
        else {
            if (logger.isTraceEnabled()) {
                logger.trace("Unable to apply any optimizations to advised method: " + method);
            }
            // 返回DynamicAdvisedInterceptor回调的索引
            return AOP_PROXY;
        }
    }
    else {
        // See if the return type of the method is outside the class hierarchy of the target type.
        // If so we know it never needs to have return type massage and can use a dispatcher.
        // If the proxy is being exposed, then must use the interceptor the correct one is already
        // configured. If the target is not static, then we cannot use a dispatcher because the
        // target needs to be explicitly released after the invocation.
        if (exposeProxy || !isStatic) {
            return INVOKE_TARGET;
        }
        Class<?> returnType = method.getReturnType();
        if (targetClass != null && returnType.isAssignableFrom(targetClass)) {
            if (logger.isTraceEnabled()) {
                logger.trace("Method return type is assignable from target type and " +
                        "may therefore return 'this' - using INVOKE_TARGET: " + method);
            }
            return INVOKE_TARGET;
        }
        else {
            if (logger.isTraceEnabled()) {
                logger.trace("Method return type ensures 'this' cannot be returned - " +
                        "using DISPATCH_TARGET: " + method);
            }
            return DISPATCH_TARGET;
        }
    }
}
```



###### （3-1）获取拦截器链

```java
@Override
public List<Object> getInterceptorsAndDynamicInterceptionAdvice(
        Advised config, Method method, @Nullable Class<?> targetClass) {

    // This is somewhat tricky... We have to process introductions first,
    // but we need to preserve order in the ultimate list.
    AdvisorAdapterRegistry registry = GlobalAdvisorAdapterRegistry.getInstance();
    // 获取所有的增强器
    Advisor[] advisors = config.getAdvisors();
    List<Object> interceptorList = new ArrayList<>(advisors.length);
    Class<?> actualClass = (targetClass != null ? targetClass : method.getDeclaringClass());
    Boolean hasIntroductions = null;
    // 遍历所有的增强器
    // 如果是PointcutAdvisor类型，获取切入点表达式的方法匹配器，校验是否匹配。如果匹配，获取增强器里的所有通知，然后保存
    // 如果是IntroductionAdvisor类型（如@DeclareParents创建增强器DeclareParentsAdvisor），直接保存
    for (Advisor advisor : advisors) {
        if (advisor instanceof PointcutAdvisor) {
            // 在我们这里的实例中，所有的增强都是PointcutAdvisor类型
            // Add it conditionally.
            PointcutAdvisor pointcutAdvisor = (PointcutAdvisor) advisor;
            // config.isPreFiltered()前面已经设置为true 
            if (config.isPreFiltered() || pointcutAdvisor.getPointcut().getClassFilter().matches(actualClass)) {
                // 方法匹配器是否匹配
                MethodMatcher mm = pointcutAdvisor.getPointcut().getMethodMatcher();
                boolean match;
                if (mm instanceof IntroductionAwareMethodMatcher) {
                    if (hasIntroductions == null) {
                        hasIntroductions = hasMatchingIntroductions(advisors, actualClass);
                    }
                    match = ((IntroductionAwareMethodMatcher) mm).matches(method, actualClass, hasIntroductions);
                }
                else {
                    match = mm.matches(method, actualClass);
                }
                if (match) {
                    MethodInterceptor[] interceptors = registry.getInterceptors(advisor);
                    if (mm.isRuntime()) {
                        // @ApsectJ的通知对应的拦截器会保存到InterceptorAndDynamicMethodMatcher。
                        // 只是包装了一层，实际最终调用拦截器链的时候还是用到interceptor
                        // Creating a new object instance in the getInterceptors() method
                        // isn't a problem as we normally cache created chains.
                        for (MethodInterceptor interceptor : interceptors) {
                            interceptorList.add(new InterceptorAndDynamicMethodMatcher(interceptor, mm));
                        }
                    }
                    else {
                        // 缓存和事务的增强器执行到这里
                        interceptorList.addAll(Arrays.asList(interceptors));
                    }
                }
            }
        }
        else if (advisor instanceof IntroductionAdvisor) {
            IntroductionAdvisor ia = (IntroductionAdvisor) advisor;
             // config.isPreFiltered()前面已经设置为true 
            if (config.isPreFiltered() || ia.getClassFilter().matches(actualClass)) {
                // 获取相应的拦截器
                Interceptor[] interceptors = registry.getInterceptors(advisor);
                interceptorList.addAll(Arrays.asList(interceptors));
            }
        }
        else {
            // 获取相应的拦截器
            Interceptor[] interceptors = registry.getInterceptors(advisor);
            interceptorList.addAll(Arrays.asList(interceptors));
        }
    }

    return interceptorList;
}
```

* 使用DefaultAdvisorAdapterRegistry获取拦截器

```java
/**
 * Default implementation of the {@link AdvisorAdapterRegistry} interface.
 * Supports {@link org.aopalliance.intercept.MethodInterceptor},
 * {@link org.springframework.aop.MethodBeforeAdvice},
 * {@link org.springframework.aop.AfterReturningAdvice},
 * {@link org.springframework.aop.ThrowsAdvice}.
 *
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public class DefaultAdvisorAdapterRegistry implements AdvisorAdapterRegistry, Serializable {

	private final List<AdvisorAdapter> adapters = new ArrayList<>(3);


	/**
	 * Create a new DefaultAdvisorAdapterRegistry, registering well-known adapters.
	 */
	public DefaultAdvisorAdapterRegistry() {
		registerAdvisorAdapter(new MethodBeforeAdviceAdapter());
		registerAdvisorAdapter(new AfterReturningAdviceAdapter());
		registerAdvisorAdapter(new ThrowsAdviceAdapter());
	}

	@Override
	public Advisor wrap(Object adviceObject) throws UnknownAdviceTypeException {
		if (adviceObject instanceof Advisor) {
			return (Advisor) adviceObject;
		}
		if (!(adviceObject instanceof Advice)) {
			throw new UnknownAdviceTypeException(adviceObject);
		}
		Advice advice = (Advice) adviceObject;
		if (advice instanceof MethodInterceptor) {
			// So well-known it doesn't even need an adapter.
			return new DefaultPointcutAdvisor(advice);
		}
		for (AdvisorAdapter adapter : this.adapters) {
			// Check that it is supported.
			if (adapter.supportsAdvice(advice)) {
				return new DefaultPointcutAdvisor(advice);
			}
		}
		throw new UnknownAdviceTypeException(advice);
	}

	@Override
	public MethodInterceptor[] getInterceptors(Advisor advisor) throws UnknownAdviceTypeException {
		List<MethodInterceptor> interceptors = new ArrayList<>(3);
		Advice advice = advisor.getAdvice();
         // 如果通知是MethodInterceptor类型的，直接保存
		if (advice instanceof MethodInterceptor) {
			interceptors.add((MethodInterceptor) advice);
		}
         // 如果通知属于MethodBeforeAdvice类型，则保存MethodBeforeAdviceInterceptor
         // 如果通知属于AfterReturningAdviceAdapter类型，则保存AfterReturningAdviceInterceptor
         // 如果通知属于ThrowsAdviceAdapter类型，则保存ThrowsAdviceInterceptor
		for (AdvisorAdapter adapter : this.adapters) {
			if (adapter.supportsAdvice(advice)) {
				interceptors.add(adapter.getInterceptor(advisor));
			}
		}
		if (interceptors.isEmpty()) {
			throw new UnknownAdviceTypeException(advisor.getAdvice());
		}
         // 返回结果
		return interceptors.toArray(new MethodInterceptor[0]);
	}

	@Override
	public void registerAdvisorAdapter(AdvisorAdapter adapter) {
		this.adapters.add(adapter);
	}

}
```



###### （4） 实例化代理对象

```java
@Override
protected Object createProxyClassAndInstance(Enhancer enhancer, Callback[] callbacks) {
    // 生成代理类的Class对象
    Class<?> proxyClass = enhancer.createClass();
    Object proxyInstance = null;

    if (objenesis.isWorthTrying()) {
        // true，执行到这里。创建代理对象
        try {
            proxyInstance = objenesis.newInstance(proxyClass, enhancer.getUseCache());
        }
        catch (Throwable ex) {
            logger.debug("Unable to instantiate proxy using Objenesis, " +
                    "falling back to regular proxy construction", ex);
        }
    }

    if (proxyInstance == null) {
        // Regular instantiation via default constructor...
        try {
            Constructor<?> ctor = (this.constructorArgs != null ?
                    proxyClass.getDeclaredConstructor(this.constructorArgTypes) :
                    proxyClass.getDeclaredConstructor());
            ReflectionUtils.makeAccessible(ctor);
            proxyInstance = (this.constructorArgs != null ?
                    ctor.newInstance(this.constructorArgs) : ctor.newInstance());
        }
        catch (Throwable ex) {
            throw new AopConfigException("Unable to instantiate proxy using Objenesis, " +
                    "and regular proxy instantiation via default constructor fails as well", ex);
        }
    }
    // 设置代理回调
    ((Factory) proxyInstance).setCallbacks(callbacks);
    return proxyInstance;
}

/**
 * Return whether this Objenesis instance is worth trying for instance creation,
 * i.e. whether it hasn't been used yet or is known to work.
 * <p>If the configured Objenesis instantiator strategy has been identified to not
 * work on the current JVM at all or if the "spring.objenesis.ignore" property has
 * been set to "true", this method returns {@code false}.
 */
public boolean isWorthTrying() {
    // worthTrying为null 
    return (this.worthTrying != Boolean.FALSE);
}
private volatile Boolean worthTrying;
```





## 4、动态代理调用

### 4.1 JdkDynamicAopProxy调用

JdkDynamicAopProxy实现InvocationHandler接口，只需要关注重写的invoke方法

```java
/**
 * JDK-based {@link AopProxy} implementation for the Spring AOP framework,
 * based on JDK {@link java.lang.reflect.Proxy dynamic proxies}.
 *
 * <p>Creates a dynamic proxy, implementing the interfaces exposed by
 * the AopProxy. Dynamic proxies <i>cannot</i> be used to proxy methods
 * defined in classes, rather than interfaces.
 *
 * <p>Objects of this type should be obtained through proxy factories,
 * configured by an {@link AdvisedSupport} class. This class is internal
 * to Spring's AOP framework and need not be used directly by client code.
 *
 * <p>Proxies created using this class will be thread-safe if the
 * underlying (target) class is thread-safe.
 *
 * <p>Proxies are serializable so long as all Advisors (including Advices
 * and Pointcuts) and the TargetSource are serializable.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Dave Syer
 * @author Sergey Tsypanov
 * @see java.lang.reflect.Proxy
 * @see AdvisedSupport
 * @see ProxyFactory
 */
final class JdkDynamicAopProxy implements AopProxy, InvocationHandler, Serializable {

	/** Config used to configure this proxy. */
	private final AdvisedSupport advised;

	private final Class<?>[] proxiedInterfaces;

	/**
	 * Is the {@link #equals} method defined on the proxied interfaces?
	 */
	private boolean equalsDefined;

	/**
	 * Is the {@link #hashCode} method defined on the proxied interfaces?
	 */
	private boolean hashCodeDefined;

	/**
	 * Implementation of {@code InvocationHandler.invoke}.
	 * <p>Callers will see exactly the exception thrown by the target,
	 * unless a hook method throws an exception.
	 */
	@Override
	@Nullable
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		Object oldProxy = null;
		boolean setProxyContext = false;	
		TargetSource targetSource = this.advised.targetSource;
		Object target = null;

		try {
			if (!this.equalsDefined && AopUtils.isEqualsMethod(method)) {
				// 目标本身没有定义equals(Object)方法。调用JdkDynamicAopProxy里的equals方法
				return equals(args[0]);
			}
			else if (!this.hashCodeDefined && AopUtils.isHashCodeMethod(method)) {
				// The target does not implement the hashCode() method itself.
                 // 目标本身没有定义hashCode方法。调用JdkDynamicAopProxy里的hashCode方法
				return hashCode();
			}
			else if (method.getDeclaringClass() == DecoratingProxy.class) {
				// There is only getDecoratedClass() declared -> dispatch to proxy config.
                 // 只有getDecoratedClass()声明->分派到代理配置。
                 // 直接调用getDecoratedClass方法
				return AopProxyUtils.ultimateTargetClass(this.advised);
			}
             // 默认情况下!this.advised.opaque=true
			else if (!this.advised.opaque && method.getDeclaringClass().isInterface() &&
					method.getDeclaringClass().isAssignableFrom(Advised.class)) {
				// Service invocations on ProxyConfig with the proxy config...
                  // Advised接口里的方法。
                  // this.advised=ProxyFactory，实现Advised接口，直接通过反射调用ProxyFactory相应的方法
				return AopUtils.invokeJoinpointUsingReflection(this.advised, method, args);
			}

			Object retVal;
             // 默认情况为false
			if (this.advised.exposeProxy) {
                 // 暴露(设置)当前代理类到AopContext中的ThreadLocal里
				// Make invocation available if necessary.
				oldProxy = AopContext.setCurrentProxy(proxy);
				setProxyContext = true;
			}

			// Get as late as possible to minimize the time we "own" the target,
			// in case it comes from a pool.
             // 获取被代理对象
			target = targetSource.getTarget();
             // 获取被代理类
			Class<?> targetClass = (target != null ? target.getClass() : null);

			// Get the interception chain for this method.
             // 1、获取拦截器链
			List<Object> chain = this.advised.getInterceptorsAndDynamicInterceptionAdvice(method, targetClass);

			// Check whether we have any advice. If we don't, we can fallback on direct
			// reflective invocation of the target, and avoid creating a MethodInvocation.
			if (chain.isEmpty()) {
                 // 如果拦截器链为空，直接通过反射调用被代理对象的方法
				// We can skip creating a MethodInvocation: just invoke the target directly
				// Note that the final invoker must be an InvokerInterceptor so we know it does
				// nothing but a reflective operation on the target, and no hot swapping or fancy proxying.
				Object[] argsToUse = AopProxyUtils.adaptArgumentsIfNecessary(method, args);
				retVal = AopUtils.invokeJoinpointUsingReflection(target, method, argsToUse);
			}
			else {
                 // 创建MethodInvocation
				// We need to create a method invocation...
				MethodInvocation invocation =
						new ReflectiveMethodInvocation(proxy, target, method, args, targetClass, chain);
			    // Proceed to the joinpoint through the interceptor chain.
                 // 链式调用
			    retVal = invocation.proceed();
			}
		
			// Massage return value if necessary.           
			Class<?> returnType = method.getReturnType();
			if (retVal != null && retVal == target &&
					returnType != Object.class && returnType.isInstance(proxy) &&
					!RawTargetAccess.class.isAssignableFrom(method.getDeclaringClass())) {
                 // 返回this的情况
				// Special case: it returned "this" and the return type of the method
				// is type-compatible. Note that we can't help if the target sets
				// a reference to itself in another returned object.
				retVal = proxy;
			}
			else if (retVal == null && returnType != Void.TYPE && returnType.isPrimitive()) {
                 // 返回值为空，方法返回值为基本数据类型，则抛出异常
				throw new AopInvocationException(
						"Null return value from advice does not match primitive return type for: " + method);
			}
			return retVal;
		}
		finally {        
			if (target != null && !targetSource.isStatic()) {
                 // SingletonTargetSource不执行到这里
				// Must have come from TargetSource.
				targetSource.releaseTarget(target);
			}
			if (setProxyContext) {
                 // 回退暴露的代理对象
				// Restore old proxy.
				AopContext.setCurrentProxy(oldProxy);
			}
		}
	}

	/**
	 * Equality means interfaces, advisors and TargetSource are equal.
	 * <p>The compared object may be a JdkDynamicAopProxy instance itself
	 * or a dynamic proxy wrapping a JdkDynamicAopProxy instance.
	 */
	@Override
	public boolean equals(@Nullable Object other) {
		if (other == this) {
			return true;
		}
		if (other == null) {
			return false;
		}

		JdkDynamicAopProxy otherProxy;
		if (other instanceof JdkDynamicAopProxy) {
			otherProxy = (JdkDynamicAopProxy) other;
		}
		else if (Proxy.isProxyClass(other.getClass())) {
			InvocationHandler ih = Proxy.getInvocationHandler(other);
			if (!(ih instanceof JdkDynamicAopProxy)) {
				return false;
			}
			otherProxy = (JdkDynamicAopProxy) ih;
		}
		else {
			// Not a valid comparison...
			return false;
		}

		// If we get here, otherProxy is the other AopProxy.
		return AopProxyUtils.equalsInProxy(this.advised, otherProxy.advised);
	}

	/**
	 * Proxy uses the hash code of the TargetSource.
	 */
	@Override
	public int hashCode() {
		return JdkDynamicAopProxy.class.hashCode() * 13 + this.advised.getTargetSource().hashCode();
	}

}
```

* AopUtils

```java
/**
 * Invoke the given target via reflection, as part of an AOP method invocation.
 * @param target the target object
 * @param method the method to invoke
 * @param args the arguments for the method
 * @return the invocation result, if any
 * @throws Throwable if thrown by the target method
 * @throws org.springframework.aop.AopInvocationException in case of a reflection error
 */
@Nullable
public static Object invokeJoinpointUsingReflection(@Nullable Object target, Method method, Object[] args)
        throws Throwable {
	// 使用反射调用方法
    // Use reflection to invoke the method.
    try {
        ReflectionUtils.makeAccessible(method);
        return method.invoke(target, args);
    }
    catch (InvocationTargetException ex) {
        // Invoked method threw a checked exception.
        // We must rethrow it. The client won't see the interceptor.
        throw ex.getTargetException();
    }
    catch (IllegalArgumentException ex) {
        throw new AopInvocationException("AOP configuration seems to be invalid: tried calling method [" +
                method + "] on target [" + target + "]", ex);
    }
    catch (IllegalAccessException ex) {
        throw new AopInvocationException("Could not access method [" + method + "]", ex);
    }
}
```

#### 4.1.1 获取拦截器链

```java
/** Cache with Method as key and advisor chain List as value. */
private transient Map<MethodCacheKey, List<Object>> methodCache;
/**
 * Determine a list of {@link org.aopalliance.intercept.MethodInterceptor} objects
 * for the given method, based on this configuration.
 * @param method the proxied method
 * @param targetClass the target class
 * @return a List of MethodInterceptors (may also include InterceptorAndDynamicMethodMatchers)
 */
public List<Object> getInterceptorsAndDynamicInterceptionAdvice(Method method, @Nullable Class<?> targetClass) {
    MethodCacheKey cacheKey = new MethodCacheKey(method);
    List<Object> cached = this.methodCache.get(cacheKey);
    if (cached == null) {
        // 同3.3.2.2 创建Enhancer里的(3-1)获取拦截器链
        cached = this.advisorChainFactory.getInterceptorsAndDynamicInterceptionAdvice(
                this, method, targetClass);
        // 添加缓存
        this.methodCache.put(cacheKey, cached);
    }
    return cached;
}
```

#### 4.1.2  链式调用

##### 4.1.2.1 创建ReflectiveMethodInvocation

```java
/**
 * Construct a new ReflectiveMethodInvocation with the given arguments.
 * @param proxy the proxy object that the invocation was made on
 * @param target the target object to invoke
 * @param method the method to invoke
 * @param arguments the arguments to invoke the method with
 * @param targetClass the target class, for MethodMatcher invocations
 * @param interceptorsAndDynamicMethodMatchers interceptors that should be applied,
 * along with any InterceptorAndDynamicMethodMatchers that need evaluation at runtime.
 * MethodMatchers included in this struct must already have been found to have matched
 * as far as was possibly statically. Passing an array might be about 10% faster,
 * but would complicate the code. And it would work only for static pointcuts.
 */
protected ReflectiveMethodInvocation(
        Object proxy, @Nullable Object target, Method method, @Nullable Object[] arguments,
        @Nullable Class<?> targetClass, List<Object> interceptorsAndDynamicMethodMatchers) {
    // 设置代理实例
    this.proxy = proxy;
    // 设置被代理实例
    this.target = target;
    // 设置被代理类
    this.targetClass = targetClass;
    // 目标方法
    this.method = BridgeMethodResolver.findBridgedMethod(method);
    // 目标方法参数
    this.arguments = AopProxyUtils.adaptArgumentsIfNecessary(method, arguments);
    // 拦截器链
    this.interceptorsAndDynamicMethodMatchers = interceptorsAndDynamicMethodMatchers;
}
```



```java
/**
 * List of MethodInterceptor and InterceptorAndDynamicMethodMatcher
 * that need dynamic checks.
 */
protected final List<?> interceptorsAndDynamicMethodMatchers;

/**
 * Index from 0 of the current interceptor we're invoking.
 * -1 until we invoke: then the current interceptor.
 */
private int currentInterceptorIndex = -1;

@Override
@Nullable
public Object proceed() throws Throwable {    
    // We start with an index of -1 and increment early.
    if (this.currentInterceptorIndex == this.interceptorsAndDynamicMethodMatchers.size() - 1) {
        // 执行完所有的拦截器链，通过反射，直接调用目标方法
        return invokeJoinpoint();
    }
	
    // currentInterceptorIndex加一，获取当前拦截器
    Object interceptorOrInterceptionAdvice =
            this.interceptorsAndDynamicMethodMatchers.get(++this.currentInterceptorIndex);
    if (interceptorOrInterceptionAdvice instanceof InterceptorAndDynamicMethodMatcher) {
        // @AspectJ的通知对应的拦截器执行到这里
        // Evaluate dynamic method matcher here: static part will already have
        // been evaluated and found to match.
        InterceptorAndDynamicMethodMatcher dm =
                (InterceptorAndDynamicMethodMatcher) interceptorOrInterceptionAdvice;
        Class<?> targetClass = (this.targetClass != null ? this.targetClass : this.method.getDeclaringClass());
        // 校验该拦截器的方法匹配器是否匹配
        if (dm.methodMatcher.matches(this.method, targetClass, this.arguments)) {
            // 如果匹配，获取（包装前的）拦截器interceptor，执行拦截器的invoke方法，传入this对象进行链式调用
            return dm.interceptor.invoke(this);
        }
        else {
            // 动态匹配失败，递归调用，跳过这个拦截器，调用下一个拦截器
            // Dynamic matching failed.
            // Skip this interceptor and invoke the next in the chain.
            return proceed();
        }
    }
    else {
        // 在我们的示例中，我们都会执行到这里
        // 执行拦截器的invoke方法，传入this，用于链式调用。
        // It's an interceptor, so we just invoke it: The pointcut will have
        // been evaluated statically before this object was constructed.
        return ((MethodInterceptor) interceptorOrInterceptionAdvice).invoke(this);
    }
}

/**
 * Invoke the joinpoint using reflection.
 * Subclasses can override this to use custom invocation.
 * @return the return value of the joinpoint
 * @throws Throwable if invoking the joinpoint resulted in an exception
 */
@Nullable
protected Object invokeJoinpoint() throws Throwable {
    return AopUtils.invokeJoinpointUsingReflection(this.target, this.method, this.arguments);
}

/**
 * Invoke the given target via reflection, as part of an AOP method invocation.
 * @param target the target object
 * @param method the method to invoke
 * @param args the arguments for the method
 * @return the invocation result, if any
 * @throws Throwable if thrown by the target method
 * @throws org.springframework.aop.AopInvocationException in case of a reflection error
 */
@Nullable
public static Object invokeJoinpointUsingReflection(@Nullable Object target, Method method, Object[] args)
        throws Throwable {

    // Use reflection to invoke the method.
    try {
        ReflectionUtils.makeAccessible(method);
        return method.invoke(target, args);
    }
    catch (InvocationTargetException ex) {
        // Invoked method threw a checked exception.
        // We must rethrow it. The client won't see the interceptor.
        throw ex.getTargetException();
    }
    catch (IllegalArgumentException ex) {
        throw new AopInvocationException("AOP configuration seems to be invalid: tried calling method [" +
                method + "] on target [" + target + "]", ex);
    }
    catch (IllegalAccessException ex) {
        throw new AopInvocationException("Could not access method [" + method + "]", ex);
    }
}
```







##### 4.1.2.2 演示调用

我们用下面测试案例来debug分析拦截器的调用，在ReflectiveMethodInvocation的proceed方法打上断点。

```java
@SpringBootTest
public class WhServiceTest {
    //@Autowired
    //private WhService whService;
    //
    //@Test
    //public void test() {
    //    whService.getWhList("aaaa");
    //}

    // 修改spring.aop.proxy-target-class=false，使用jdk动态代理
    // 测试jdk使用下面的。因为jdk动态代理的类已经继承了Proxy，上面注入具体的类为WhService，类型不匹配，会报错。
    @Autowired
    private WhInterface whInterface;

    @Test
    public void tt() {
        whInterface.tt("ttt");
    }
}
```

![image-20240628231658172](https://lu-note.oss-cn-shenzhen.aliyuncs.com/notes/work/image-20240628231658172.png)

说明

（1）第一个为spring自动添加的（有AspectJ通知才添加），用于暴露当前调用的拦截器

（2）第二个到第六个为@AspectJ注解里的通知方法对应的拦截器

（3）倒数第二个为事务的拦截器

（4）最后一个为缓存的拦截器



* 第一次调用

ExposeInvocationInterceptor

```java
private static final ThreadLocal<MethodInvocation> invocation =
			new NamedThreadLocal<>("Current AOP method invocation");    

@Override
@Nullable
public Object invoke(MethodInvocation mi) throws Throwable {   
    MethodInvocation oldInvocation = invocation.get();
    // 记录当前调用的拦截器
    invocation.set(mi);
    try {
       //  mi为ReflectiveMethodInvocation，又回到ReflectiveMethodInvocation的proceed方法，相当于递归调用
        return mi.proceed();
    }
    finally {
        // 释放
        invocation.set(oldInvocation);
    }
}
```



* 第二次调用

AspectJAroundAdvice

```java
@Override
@Nullable
public Object invoke(MethodInvocation mi) throws Throwable {
    if (!(mi instanceof ProxyMethodInvocation)) {
        throw new IllegalStateException("MethodInvocation is not a Spring ProxyMethodInvocation: " + mi);
    }

    ProxyMethodInvocation pmi = (ProxyMethodInvocation) mi;
    // 将pmi包装到MethodInvocationProceedingJoinPoint
    ProceedingJoinPoint pjp = lazyGetProceedingJoinPoint(pmi);
    // JoinPointMatchImpl
    JoinPointMatch jpm = getJoinPointMatch(pmi);
    // 通过反射调用增强方法（这里就是@Around注解标注的方法）
    return invokeAdviceMethod(pjp, jpm, null, null);
}
```



```java
// As above, but in this case we are given the join point.
protected Object invokeAdviceMethod(JoinPoint jp, @Nullable JoinPointMatch jpMatch,
        @Nullable Object returnValue, @Nullable Throwable t) throws Throwable {
    // argBinding(jp, jpMatch, returnValue, t) 绑定方法参数，这里不是我们关注的重点
    return invokeAdviceMethodWithGivenArgs(argBinding(jp, jpMatch, returnValue, t));
}

protected Object invokeAdviceMethodWithGivenArgs(Object[] args) throws Throwable {
    Object[] actualArgs = args;
    if (this.aspectJAdviceMethod.getParameterCount() == 0) {
        actualArgs = null;
    }
    try {
        // 通过反射调用增强方法.
        // aspectJAdviceMethod就是具体的通知方法
        // @Around注解、@Before注解、@After、@AfterReturning、@AfterThrowing注解标注的方法都是通知方法
        ReflectionUtils.makeAccessible(this.aspectJAdviceMethod);
        return this.aspectJAdviceMethod.invoke(this.aspectInstanceFactory.getAspectInstance(), actualArgs);
    }
    catch (IllegalArgumentException ex) {
        throw new AopInvocationException("Mismatch on arguments to advice method [" +
                this.aspectJAdviceMethod + "]; pointcut expression [" +
                this.pointcut.getPointcutExpression() + "]", ex);
    }
    catch (InvocationTargetException ex) {
        throw ex.getTargetException();
    }
}
```

```java
@Around("webLog()")
public Object doAround(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
    long startTime = System.currentTimeMillis();
    LOGGER.info("-------------doAround before proceed-------------");
    // MethodInvocationProceedingJoinPoint的proceed方法。注意这里又调到了proceed方法
    Object result = proceedingJoinPoint.proceed();
    // 打印出参
    LOGGER.info("Response Args  : {}", result);
    // 执行耗时
    LOGGER.info("Time-Consuming : {} ms", System.currentTimeMillis() - startTime);
    LOGGER.info("-------------doAround after proceed-------------");
    return result;
}
```

MethodInvocationProceedingJoinPoint

```java
@Override
@Nullable
public Object proceed() throws Throwable {
    // methodInvocation=ReflectiveMethodInvocation，调用它的克隆方法，然后调用proceed方法。这里又相当于递归调用
    return this.methodInvocation.invocableClone().proceed();
}
```



* 第三次调用

MethodBeforeAdviceInterceptor

```java
public class MethodBeforeAdviceInterceptor implements MethodInterceptor, BeforeAdvice, Serializable {

	private final MethodBeforeAdvice advice;

	/**
	 * Create a new MethodBeforeAdviceInterceptor for the given advice.
	 * @param advice the MethodBeforeAdvice to wrap
	 */
	public MethodBeforeAdviceInterceptor(MethodBeforeAdvice advice) {
		Assert.notNull(advice, "Advice must not be null");
		this.advice = advice;
	}

	@Override
	@Nullable
	public Object invoke(MethodInvocation mi) throws Throwable {
         // this.advice=AspectJMethodBeforeAdvice.
         // mi.getMethod() 获取到被代理的方法
         // mi.getThis()获取到被代理实例
         // mi.getArguments()获取被代理的方法参数
		this.advice.before(mi.getMethod(), mi.getArguments(), mi.getThis());
         // @Before对应的通知方法调用完成后，调用ReflectiveMethodInvocation的proceed方法，再次递归调用
		return mi.proceed();
	}

}
```

通过反射调用通知方法，实际和上面是一样的

```java
@Override
public void before(Method method, Object[] args, @Nullable Object target) throws Throwable {
    invokeAdviceMethod(getJoinPointMatch(), null, null);
}
protected Object invokeAdviceMethod(
        @Nullable JoinPointMatch jpMatch, @Nullable Object returnValue, @Nullable Throwable ex)
        throws Throwable {

    return invokeAdviceMethodWithGivenArgs(argBinding(getJoinPoint(), jpMatch, returnValue, ex));
}
protected Object invokeAdviceMethodWithGivenArgs(Object[] args) throws Throwable {
    Object[] actualArgs = args;
    if (this.aspectJAdviceMethod.getParameterCount() == 0) {
        actualArgs = null;
    }
    try {
         // 通过反射调用增强方法.
        // aspectJAdviceMethod就是具体的通知方法
        // @Around注解、@Before注解、@After、@AfterReturning、@AfterThrowing注解标注的方法都是通知方法
        ReflectionUtils.makeAccessible(this.aspectJAdviceMethod);
        return this.aspectJAdviceMethod.invoke(this.aspectInstanceFactory.getAspectInstance(), actualArgs);
    }
    catch (IllegalArgumentException ex) {
        throw new AopInvocationException("Mismatch on arguments to advice method [" +
                this.aspectJAdviceMethod + "]; pointcut expression [" +
                this.pointcut.getPointcutExpression() + "]", ex);
    }
    catch (InvocationTargetException ex) {
        throw ex.getTargetException();
    }
}
```



* 第四次调用

AspectJAfterAdvice

```java
/**
 * Spring AOP advice wrapping an AspectJ after advice method.
 *
 * @author Rod Johnson
 * @since 2.0
 */
@SuppressWarnings("serial")
public class AspectJAfterAdvice extends AbstractAspectJAdvice
		implements MethodInterceptor, AfterAdvice, Serializable {

	public AspectJAfterAdvice(
			Method aspectJBeforeAdviceMethod, AspectJExpressionPointcut pointcut, AspectInstanceFactory aif) {

		super(aspectJBeforeAdviceMethod, pointcut, aif);
	}


	@Override
	@Nullable
	public Object invoke(MethodInvocation mi) throws Throwable {
		try {
             // 调用ReflectiveMethodInvocation的proceed方法，再次递归调用
			return mi.proceed();
		}
		finally {
             // 调用@After对应的通知方法.这里debug进去和上面是一样的，我们就不展开了
             // 可以发现@After的实际底层是通过拦截器的try-finally实现的
			invokeAdviceMethod(getJoinPointMatch(), null, null);
		}
	}

	@Override
	public boolean isBeforeAdvice() {
		return false;
	}

	@Override
	public boolean isAfterAdvice() {
		return true;
	}

}
```



* 第五次调用

```java
public class AfterReturningAdviceInterceptor implements MethodInterceptor, AfterAdvice, Serializable {

	private final AfterReturningAdvice advice;

	/**
	 * Create a new AfterReturningAdviceInterceptor for the given advice.
	 * @param advice the AfterReturningAdvice to wrap
	 */
	public AfterReturningAdviceInterceptor(AfterReturningAdvice advice) {
		Assert.notNull(advice, "Advice must not be null");
		this.advice = advice;
	}

	@Override
	@Nullable
	public Object invoke(MethodInvocation mi) throws Throwable {
         // 调用ReflectiveMethodInvocation的proceed方法，再次递归调用
		Object retVal = mi.proceed();
         // this.advice=AspectJAfterReturningAdvice，调用@AfterReturning对应的通知方法
		this.advice.afterReturning(retVal, mi.getMethod(), mi.getArguments(), mi.getThis());
		return retVal;
	}

}
```

```java
/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.aop.aspectj;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.springframework.aop.AfterAdvice;
import org.springframework.aop.AfterReturningAdvice;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.TypeUtils;

/**
 * Spring AOP advice wrapping an AspectJ after-returning advice method.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Ramnivas Laddad
 * @since 2.0
 */
@SuppressWarnings("serial")
public class AspectJAfterReturningAdvice extends AbstractAspectJAdvice
		implements AfterReturningAdvice, AfterAdvice, Serializable {

	public AspectJAfterReturningAdvice(
			Method aspectJBeforeAdviceMethod, AspectJExpressionPointcut pointcut, AspectInstanceFactory aif) {

		super(aspectJBeforeAdviceMethod, pointcut, aif);
	}


	@Override
	public boolean isBeforeAdvice() {
		return false;
	}

	@Override
	public boolean isAfterAdvice() {
		return true;
	}

	@Override
	public void setReturningName(String name) {
		setReturningNameNoCheck(name);
	}

	@Override
	public void afterReturning(@Nullable Object returnValue, Method method, Object[] args, @Nullable Object target) throws Throwable {
         // 检查返回值类型是否和方法定义的匹配
		if (shouldInvokeOnReturnValueOf(method, returnValue)) {
             // 调用@AspectJAfterReturning对应的通知方法.这里debug进去和上面是一样的，我们就不展开了
			invokeAdviceMethod(getJoinPointMatch(), returnValue, null);
		}
	}


	/**
	 * Following AspectJ semantics, if a returning clause was specified, then the
	 * advice is only invoked if the returned value is an instance of the given
	 * returning type and generic type parameters, if any, match the assignment
	 * rules. If the returning type is Object, the advice is *always* invoked.
	 * @param returnValue the return value of the target method
	 * @return whether to invoke the advice method for the given return value
	 */
	private boolean shouldInvokeOnReturnValueOf(Method method, @Nullable Object returnValue) {
		Class<?> type = getDiscoveredReturningType();
		Type genericType = getDiscoveredReturningGenericType();
		// If we aren't dealing with a raw type, check if generic parameters are assignable.
		return (matchesReturnValue(type, method, returnValue) &&
				(genericType == null || genericType == type ||
						TypeUtils.isAssignable(genericType, method.getGenericReturnType())));
	}

	/**
	 * Following AspectJ semantics, if a return value is null (or return type is void),
	 * then the return type of target method should be used to determine whether advice
	 * is invoked or not. Also, even if the return type is void, if the type of argument
	 * declared in the advice method is Object, then the advice must still get invoked.
	 * @param type the type of argument declared in advice method
	 * @param method the advice method
	 * @param returnValue the return value of the target method
	 * @return whether to invoke the advice method for the given return value and type
	 */
	private boolean matchesReturnValue(Class<?> type, Method method, @Nullable Object returnValue) {
		if (returnValue != null) {
			return ClassUtils.isAssignableValue(type, returnValue);
		}
		else if (Object.class == type && void.class == method.getReturnType()) {
			return true;
		}
		else {
			return ClassUtils.isAssignable(type, method.getReturnType());
		}
	}

}
```



* 第六次调用

```java
/**
 * Spring AOP advice wrapping an AspectJ after-throwing advice method.
 *
 * @author Rod Johnson
 * @since 2.0
 */
@SuppressWarnings("serial")
public class AspectJAfterThrowingAdvice extends AbstractAspectJAdvice
		implements MethodInterceptor, AfterAdvice, Serializable {

	public AspectJAfterThrowingAdvice(
			Method aspectJBeforeAdviceMethod, AspectJExpressionPointcut pointcut, AspectInstanceFactory aif) {

		super(aspectJBeforeAdviceMethod, pointcut, aif);
	}


	@Override
	public boolean isBeforeAdvice() {
		return false;
	}

	@Override
	public boolean isAfterAdvice() {
		return true;
	}

	@Override
	public void setThrowingName(String name) {
		setThrowingNameNoCheck(name);
	}

	@Override
	@Nullable
	public Object invoke(MethodInvocation mi) throws Throwable {
		try {
             // 调用ReflectiveMethodInvocation的proceed方法，再次递归调用
			return mi.proceed();
		}
		catch (Throwable ex) {
             // 如果抛出了异常。默认情况下会进去
			if (shouldInvokeOnThrowing(ex)) {
                  // 调用@AfterThrowing对应的通知方法.这里debug进去和上面是一样的，我们就不展开了
				invokeAdviceMethod(getJoinPointMatch(), null, ex);
			}
			throw ex;
		}
	}

	/**
	 * In AspectJ semantics, after throwing advice that specifies a throwing clause
	 * is only invoked if the thrown exception is a subtype of the given throwing type.
	 */
	private boolean shouldInvokeOnThrowing(Throwable ex) {
        // getDiscoveredThrowingType() 默认为Object.class
		return getDiscoveredThrowingType().isAssignableFrom(ex.getClass());
	}

}
```



* 第七次递归调用

事务拦截器TransactionInterceptor的invoke里调用的invokeWithinTransaction方法内容比较多就不展开，最终会调用CoroutinesInvocationCallback的 proceedWithInvocation方法。

```java
@Override
@Nullable
public Object invoke(MethodInvocation invocation) throws Throwable {
    // Work out the target class: may be {@code null}.
    // The TransactionAttributeSource should be passed the target class
    // as well as the method, which may be from an interface.
    Class<?> targetClass = (invocation.getThis() != null ? AopUtils.getTargetClass(invocation.getThis()) : null);

    // Adapt to TransactionAspectSupport's invokeWithinTransaction...
    return invokeWithinTransaction(invocation.getMethod(), targetClass, new CoroutinesInvocationCallback() {
        @Override
        @Nullable
        public Object proceedWithInvocation() throws Throwable {
            // 调用ReflectiveMethodInvocation的proceed方法，再次递归调用
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
```



* 第八次调用

```java
public class CacheInterceptor extends CacheAspectSupport implements MethodInterceptor, Serializable {

	@Override
	@Nullable
	public Object invoke(final MethodInvocation invocation) throws Throwable {
		Method method = invocation.getMethod();
		
         // 创建缓存操作调用器
		CacheOperationInvoker aopAllianceInvoker = () -> {
			try {
                 // 调用ReflectiveMethodInvocation的proceed方法，再次递归调用
				return invocation.proceed();
			}
			catch (Throwable ex) {
				throw new CacheOperationInvoker.ThrowableWrapper(ex);
			}
		};
		// 获取被代理对象
		Object target = invocation.getThis();
		Assert.state(target != null, "Target must not be null");
		try {
             // 缓存的execute方法比较复杂，不是我们关注的重点
             // 只需要知道最后会调用aopAllianceInvoker的invoke方法,实现ReflectiveMethodInvocation的proceed方法递归调用
			return execute(aopAllianceInvoker, target, method, invocation.getArguments());
		}
		catch (CacheOperationInvoker.ThrowableWrapper th) {
			throw th.getOriginal();
		}
	}

}
```



* 最后

```java
@Override
@Nullable
public Object proceed() throws Throwable {
    // We start with an index of -1 and increment early.
    if (this.currentInterceptorIndex == this.interceptorsAndDynamicMethodMatchers.size() - 1) {
        // 最后执行到这里，见面知义调用连接点，即通过反射直接调用目标方法
        return invokeJoinpoint();
    }

    Object interceptorOrInterceptionAdvice =
            this.interceptorsAndDynamicMethodMatchers.get(++this.currentInterceptorIndex);
    if (interceptorOrInterceptionAdvice instanceof InterceptorAndDynamicMethodMatcher) {
        // Evaluate dynamic method matcher here: static part will already have
        // been evaluated and found to match.
        InterceptorAndDynamicMethodMatcher dm =
                (InterceptorAndDynamicMethodMatcher) interceptorOrInterceptionAdvice;
        Class<?> targetClass = (this.targetClass != null ? this.targetClass : this.method.getDeclaringClass());
        if (dm.methodMatcher.matches(this.method, targetClass, this.arguments)) {
            return dm.interceptor.invoke(this);
        }
        else {
            // Dynamic matching failed.
            // Skip this interceptor and invoke the next in the chain.
            return proceed();
        }
    }
    else {
        // It's an interceptor, so we just invoke it: The pointcut will have
        // been evaluated statically before this object was constructed.
        return ((MethodInterceptor) interceptorOrInterceptionAdvice).invoke(this);
    }
}

/**
 * Invoke the joinpoint using reflection.
 * Subclasses can override this to use custom invocation.
 * @return the return value of the joinpoint
 * @throws Throwable if invoking the joinpoint resulted in an exception
 */
@Nullable
protected Object invokeJoinpoint() throws Throwable {
    return AopUtils.invokeJoinpointUsingReflection(this.target, this.method, this.arguments);
}

// AopUtils
/**
 * Invoke the given target via reflection, as part of an AOP method invocation.
 * @param target the target object
 * @param method the method to invoke
 * @param args the arguments for the method
 * @return the invocation result, if any
 * @throws Throwable if thrown by the target method
 * @throws org.springframework.aop.AopInvocationException in case of a reflection error
 */
@Nullable
public static Object invokeJoinpointUsingReflection(@Nullable Object target, Method method, Object[] args)
        throws Throwable {

    // Use reflection to invoke the method.
    try {
        ReflectionUtils.makeAccessible(method);
        return method.invoke(target, args);
    }
    catch (InvocationTargetException ex) {
        // Invoked method threw a checked exception.
        // We must rethrow it. The client won't see the interceptor.
        throw ex.getTargetException();
    }
    catch (IllegalArgumentException ex) {
        throw new AopInvocationException("AOP configuration seems to be invalid: tried calling method [" +
                method + "] on target [" + target + "]", ex);
    }
    catch (IllegalAccessException ex) {
        throw new AopInvocationException("Could not access method [" + method + "]", ex);
    }
}
```

目标方法执行完后，依次进行递归回退。

### 4.2 DynamicAdvisedInterceptor调用

ObjenesisCglibAopProxy创建代理实例的时候，设置AOP的回调为DynamicAdvisedInterceptor

```java
/**
 * 通用的AOP回调。当目标是动态的或代理未被冻结。
 * General purpose AOP callback. Used when the target is dynamic or when the
 * proxy is not frozen.
 */
private static class DynamicAdvisedInterceptor implements MethodInterceptor, Serializable {

    private final AdvisedSupport advised;

    public DynamicAdvisedInterceptor(AdvisedSupport advised) {
        this.advised = advised;
    }
    
    /**
    * proxy:代理对象
    * method:正在被调用的方法对象。这个参数表示被调用的实际方法，可以通过它获取方法的名称、参数类型、返回类型等信息。
    *        可以用它来反射地调用方法（使用 method.invoke），但这样会比较慢，因为反射调用的性能较低。
    * args: 被调用方法的参数。这个参数是一个包含方法调用时传入的参数值的数组，你可以在拦截方法中修改这些参数。
    * methodProxy: 用于调用原始未被拦截的方法的代理。提供了更高效的方法调用机制，相对于 Method 对象通过反射调用，
    *              MethodProxy 提供了直接调用方法的能力，性能更好。主要用于在拦截器中调用原始方法。
    */
    @Override
    @Nullable
    public Object intercept(Object proxy, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
        Object oldProxy = null;
        boolean setProxyContext = false;
        Object target = null;
        TargetSource targetSource = this.advised.getTargetSource();
        try {
            // 默认为false
            if (this.advised.exposeProxy) {
                // 暴露(设置)当前代理类到AopContext中的ThreadLocal里
                // Make invocation available if necessary.
                oldProxy = AopContext.setCurrentProxy(proxy);
                setProxyContext = true;
            }
            // Get as late as possible to minimize the time we "own" the target, in case it comes from a pool...
            // 获取被代理对象
            target = targetSource.getTarget();
             // 获取被代理类
            Class<?> targetClass = (target != null ? target.getClass() : null);
            
            // 获取拦截器链(同上面的4.1.1获取拦截器链)
            List<Object> chain = this.advised.getInterceptorsAndDynamicInterceptionAdvice(method, targetClass);
            Object retVal;
            // Check whether we only have one InvokerInterceptor: that is,
            // no real advice, but just reflective invocation of the target.            
            if (chain.isEmpty() && CglibMethodInvocation.isMethodProxyCompatible(method)) {
                // 拦截器链为空，被代理方法不是Object类里的方法，而且是public权限的，直接通过反射调用    
                //我们可以跳过创建MethodInvocation:直接调用目标。
                //注意最后的调用者必须是一个InvokerInterceptor，这样我们就知道了
                //它只对目标对象做一个反射操作，不做热操作
                //交换或花哨的代理。
                // We can skip creating a MethodInvocation: just invoke the target directly.
                // Note that the final invoker must be an InvokerInterceptor, so we know
                // it does nothing but a reflective operation on the target, and no hot
                // swapping or fancy proxying.
                // 获取方法参数
                Object[] argsToUse = AopProxyUtils.adaptArgumentsIfNecessary(method, args);
                try {
                    // 高效率的调用原始方法
                    retVal = methodProxy.invoke(target, argsToUse);
                }
                catch (CodeGenerationException ex) {
                    CglibMethodInvocation.logFastClassGenerationFailure(method);
                    retVal = AopUtils.invokeJoinpointUsingReflection(target, method, argsToUse);
                }
            }
            else {
                // 创建CglibMethodInvocation，然后调用
                // We need to create a method invocation...
                retVal = new CglibMethodInvocation(proxy, target, method, args, targetClass, chain, methodProxy).proceed();
            }
            // 处理返回值           
            retVal = processReturnType(proxy, target, method, retVal);
            return retVal;
        }
        finally {
            if (target != null && !targetSource.isStatic()) {
                targetSource.releaseTarget(target);
            }
            if (setProxyContext) {
                // Restore old proxy.
                AopContext.setCurrentProxy(oldProxy);
            }
        }
    }

    @Override
    public boolean equals(@Nullable Object other) {
        return (this == other ||
                (other instanceof DynamicAdvisedInterceptor &&
                        this.advised.equals(((DynamicAdvisedInterceptor) other).advised)));
    }

    /**
     * CGLIB uses this to drive proxy creation.
     */
    @Override
    public int hashCode() {
        return this.advised.hashCode();
    }
}
```



```java
static boolean isMethodProxyCompatible(Method method) {
    // 仅对不是从java.lang.Object派生的公共方法使用方法代理
   return (Modifier.isPublic(method.getModifiers()) &&
         method.getDeclaringClass() != Object.class && !AopUtils.isEqualsMethod(method) &&
         !AopUtils.isHashCodeMethod(method) && !AopUtils.isToStringMethod(method));
}
```



* 处理返回值

```java
/**
 * Process a return value. Wraps a return of {@code this} if necessary to be the
 * {@code proxy} and also verifies that {@code null} is not returned as a primitive.
 */
@Nullable
private static Object processReturnType(
      Object proxy, @Nullable Object target, Method method, @Nullable Object returnValue) {

   // Massage return value if necessary
   if (returnValue != null && returnValue == target &&
         !RawTargetAccess.class.isAssignableFrom(method.getDeclaringClass())) {
       // 返回this的情况
      // Special case: it returned "this". Note that we can't help
      // if the target sets a reference to itself in another returned object.
      returnValue = proxy;
   }
    
   Class<?> returnType = method.getReturnType();
   if (returnValue == null && returnType != Void.TYPE && returnType.isPrimitive()) {
       // 返回值为空，方法返回值为基本数据类型，则抛出异常
      throw new AopInvocationException(
            "Null return value from advice does not match primitive return type for: " + method);
   }
   return returnValue;
}
```



#### 4.2.1 CglibMethodInvocation调用

##### 4.2.1.1 创建CglibMethodInvocation

```java
private static class CglibMethodInvocation extends ReflectiveMethodInvocation {

		@Nullable
		private final MethodProxy methodProxy;
		
		public CglibMethodInvocation(Object proxy, @Nullable Object target, Method method,
				Object[] arguments, @Nullable Class<?> targetClass,
				List<Object> interceptorsAndDynamicMethodMatchers, MethodProxy methodProxy) {
			// 同4.1.2.1 创建ReflectiveMethodInvocation
			super(proxy, target, method, arguments, targetClass, interceptorsAndDynamicMethodMatchers);
			// 设置methodProxy。仅对非从java.lang.Object派生的公共方法使用方法代理
			// Only use method proxy for public methods not derived from java.lang.Object
			this.methodProxy = (isMethodProxyCompatible(method) ? methodProxy : null);
		}

		@Override
		@Nullable
		public Object proceed() throws Throwable {
			try {
				return super.proceed();
			}
			catch (RuntimeException ex) {
				throw ex;
			}
			catch (Exception ex) {
				if (ReflectionUtils.declaresException(getMethod(), ex.getClass()) ||
						KotlinDetector.isKotlinType(getMethod().getDeclaringClass())) {
					// Propagate original exception if declared on the target method
					// (with callers expecting it). Always propagate it for Kotlin code
					// since checked exceptions do not have to be explicitly declared there.
					throw ex;
				}
				else {
					// Checked exception thrown in the interceptor but not declared on the
					// target method signature -> apply an UndeclaredThrowableException,
					// aligned with standard JDK dynamic proxy behavior.
					throw new UndeclaredThrowableException(ex);
				}
			}
		}

		/**
		 * Gives a marginal performance improvement versus using reflection to
		 * invoke the target when invoking public methods.
		 */
		@Override
		protected Object invokeJoinpoint() throws Throwable {
			if (this.methodProxy != null) {
				try {
                      // 高效率的调用原始方法
					return this.methodProxy.invoke(this.target, this.arguments);
				}
				catch (CodeGenerationException ex) {
					logFastClassGenerationFailure(this.method);
				}
			}
			return super.invokeJoinpoint();
		}

		static boolean isMethodProxyCompatible(Method method) {
			return (Modifier.isPublic(method.getModifiers()) &&
					method.getDeclaringClass() != Object.class && !AopUtils.isEqualsMethod(method) &&
					!AopUtils.isHashCodeMethod(method) && !AopUtils.isToStringMethod(method));
		}

		static void logFastClassGenerationFailure(Method method) {
			if (logger.isDebugEnabled()) {
				logger.debug("Failed to generate CGLIB fast class for method: " + method);
			}
		}
	}
```

##### 4.2.1.2 调用

```java
@Override
@Nullable
public Object proceed() throws Throwable {
    try {
        // 调用了ReflectiveMethodInvocation的proceed()方法。
        // 调用过程就跟 4.1.2 链式调用 是一样的。就不重复展开说明
        return super.proceed();
    }
    catch (RuntimeException ex) {
        throw ex;
    }
    catch (Exception ex) {
        if (ReflectionUtils.declaresException(getMethod(), ex.getClass()) ||
                KotlinDetector.isKotlinType(getMethod().getDeclaringClass())) {
            // Propagate original exception if declared on the target method
            // (with callers expecting it). Always propagate it for Kotlin code
            // since checked exceptions do not have to be explicitly declared there.
            throw ex;
        }
        else {
            // Checked exception thrown in the interceptor but not declared on the
            // target method signature -> apply an UndeclaredThrowableException,
            // aligned with standard JDK dynamic proxy behavior.
            throw new UndeclaredThrowableException(ex);
        }
    }
}
```



# 四、总结

1、先获取所有的增强器。包括为切面类的所有通知方法创建增强器
2、通过切入点（类过滤器和方法匹配器）筛选，获取合适的增强器，并进行排序
3、根据代理模式（一般通过proxyTargetClass属性即可确定），创建代理类创建类
   （1）jdk动态代理：JdkDynamicAopProxy
   （2）cglib动态代理：ObjenesisCglibAopProxy
4、通过代理类创建类来创建代理对象，代理对象将所有合适的增强器处理成拦截器链，然后进行递归调用

调试工程： [aop-principle](F:\code\scene\aop-principle) 

# 五、自定义增强器实现AOP

## 1、需求

在方法调用前后打印日志

## 2、思路

```java
1、自定义日志注解，在类上或者方法上加上日志注解，则可以打印日志
2、通过全局开关注解，完成以下功能
  （1）控制日志的开启和关闭，即使有日志注解存在（即没加全局开关注解，日志就不输出，反之输出）
  （2）从全局开关注解中获取代理模式，根据代理模式结合配置类，加载增强器或者切面到spring
3、加载增强器或者切面
3.1 创建增强器
   3.1.1 创建切入点。包含类过滤器和方法匹配器
    	（1）类过滤器。可以直接返回true(或者过滤一部分配置类)
    	（2）方法匹配器：如果方法上有日志注解(包括继承过来的)，返回true。
    	     方法上没有则判断类上是否有日志注解(包括继承过来的)，如果有，返回true。反之返回false   
   3.1.2 创建通知。实现MethodInterceptor接口，重写invoke方法，在方法调用前后打印日志
3.2、创建切面
	创建类，标注@AspectJ注解。
	创建方法，标注@Before注解，在方法里打印调用前的日志。
	创建方法，标注@AfterReturning注解，在方法里打印调用后的日志。
```



## 3、实现

* 自定义日志注解

```java
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Log {
}
```



* 全局日志开关

```java
import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;

import java.lang.annotation.*;

/**
 * @author 简单de快乐
 * @create 2024-07-03 15:06
 *
 * 创建开启日志注解，引入aop注入类
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Import(LogConfigurationSelector.class)
public @interface EnableLogManagement {

    /**
     * Indicate whether subclass-based (CGLIB) proxies are to be created as opposed
     * to standard Java interface-based proxies. The default is {@code false}. <strong>
     * Applicable only if {@link #mode()} is set to {@link AdviceMode#PROXY}</strong>.
     * <p>Note that setting this attribute to {@code true} will affect <em>all</em>
     * Spring-managed beans requiring proxying, not just those marked with {@code @Cacheable}.
     * For example, other beans marked with Spring's {@code @Transactional} annotation will
     * be upgraded to subclass proxying at the same time. This approach has no negative
     * impact in practice unless one is explicitly expecting one type of proxy vs another,
     * e.g. in tests.
     */
    boolean proxyTargetClass() default false;

    /**
     * Indicate how caching advice should be applied.
     * <p><b>The default is {@link AdviceMode#PROXY}.</b>
     * Please note that proxy mode allows for interception of calls through the proxy
     * only. Local calls within the same class cannot get intercepted that way;
     * a caching annotation on such a method within a local call will be ignored
     * since Spring's interceptor does not even kick in for such a runtime scenario.
     * For a more advanced mode of interception, consider switching this to
     * {@link AdviceMode#ASPECTJ}.
     */
    AdviceMode mode() default AdviceMode.PROXY;

    /**
     * Indicate the ordering of the execution of the caching advisor
     * when multiple advices are applied at a specific joinpoint.
     * <p>The default is {@link Ordered#LOWEST_PRECEDENCE}.
     */
    int order() default Ordered.LOWEST_PRECEDENCE;



}
```

```java
public class LogConfigurationSelector extends AdviceModeImportSelector<EnableLogManagement>{
    @Override
    protected String[] selectImports(AdviceMode adviceMode) {
        // 根据代理模式，加载配置类
        switch (adviceMode) {
            case PROXY:
                return new String[] {AutoProxyRegistrar.class.getName(),
                        ProxyLogManagementConfiguration.class.getName()};
            case ASPECTJ:
                return new String[] {AspectJLogManagementConfiguration.class.getName()};
            default:
                return null;
        }
    }
}
```



* 配置类

```java
@Configuration
public abstract class AbstractLogManagementConfiguration implements ImportAware {

    @Nullable
    protected AnnotationAttributes enableLog;

    @Override
    public void setImportMetadata(AnnotationMetadata importMetadata) {
        enableLog = AnnotationAttributes.fromMap(importMetadata.getAnnotationAttributes(EnableLogManagement.class.getName(), false));
        if (enableLog == null) {
            throw new IllegalArgumentException("@EnableLogManagement is not present on importing class " + importMetadata.getClassName());
        }
    }
}
```

```java
@Configuration(proxyBeanMethods = false)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class ProxyLogManagementConfiguration extends AbstractLogManagementConfiguration{

    // 注入增强器
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public BeanFactoryLogSourceAdvisor logAdvisor(LogSourcePointCut logSourcePointCut, LogInterceptor logInterceptor) {
        BeanFactoryLogSourceAdvisor advisor = new BeanFactoryLogSourceAdvisor();
        // 增强器包含两大项：（1）切入点 （2）通知
        // 设置切入点
        advisor.setLogPointcut(logSourcePointCut);
        // 设置通知
        advisor.setAdvice(logInterceptor);
        // 设置增强器的顺序
        if (this.enableLog != null) {
            advisor.setOrder(this.enableLog.<Integer>getNumber("order"));
        }
        return advisor;
    }

    // 注入切入点
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public LogSourcePointCut logSourcePointCut() {
        return new LogSourcePointCut(Log.class, true);
    }

    //注入通知
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public LogInterceptor logInterceptor() {
        return new LogInterceptor();
    }

}
```

```java
@Configuration
@EnableAspectJAutoProxy
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class AspectJLogManagementConfiguration {

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public LoggingAspect loggingAspect() {
        return new LoggingAspect();
    }
}
```



* 增强器相关

```java
/**
 * @author 简单de快乐
 * @create 2024-07-03 15:24
 *
 * 日志增强器器
 */
public class BeanFactoryLogSourceAdvisor extends AbstractBeanFactoryPointcutAdvisor {
    private Pointcut logPointcut;

    public void setLogPointcut(Pointcut logPointcut) {
        this.logPointcut = logPointcut;
    }

    @Override
    public Pointcut getPointcut() {
        return logPointcut;
    }
}
```



```java
/**
 * @author 简单de快乐
 * @create 2024-07-03 15:28
 * 日志切入点
 */
public class LogSourcePointCut implements Pointcut {

    // 类过滤器
    private ClassFilter classFilter;

    // 方法匹配器
    private MethodMatcher methodMatcher;

    public LogSourcePointCut(Class<? extends Annotation> annotationType, boolean checkInherited) {
        this.classFilter = new LogSourceClassFilter();
        this.methodMatcher = new LogMethodMatcher(annotationType, checkInherited);
    }

    @Override
    public ClassFilter getClassFilter() {
        return classFilter;
    }

    @Override
    public MethodMatcher getMethodMatcher() {
        return methodMatcher;
    }

    private class LogSourceClassFilter implements ClassFilter {

        @Override
        public boolean matches(Class<?> clazz) {
            // 过滤ProxyLogManagementConfiguration类
            if (ProxyLogManagementConfiguration.class.isAssignableFrom(clazz)) {
                return false;
            }
            // 返回true。让后面的方法匹配器校验是否为符合条件的增强器
            return true;
        }
    }

    private class LogMethodMatcher extends StaticMethodMatcher {
        private MethodMatcher methodMatcher;

        private boolean checkInherited;

        private Class<? extends Annotation> annotationType;

        public LogMethodMatcher(Class<? extends Annotation> annotationType, boolean checkInherited) {
            // 是否考虑继承
            this.checkInherited = checkInherited;
            this.annotationType = annotationType;
            this.methodMatcher = new AnnotationMethodMatcher(annotationType, checkInherited);
        }

        private Map<MethodClassKey, Boolean> attributeCache = new ConcurrentHashMap<>();

        @Override
        public boolean matches(Method method, Class<?> targetClass) {
            // First, see if we have a cached value.
            MethodClassKey cacheKey = getCacheKey(method, targetClass);
            Boolean cached = attributeCache.get(cacheKey);
            if (cached != null) {
                return cached;
            }

            boolean matches = methodMatcher.matches(method, targetClass);
            if (matches) {
                // 方法或者继承上的方法有注解存在
                attributeCache.put(cacheKey, true);
                return true;
            }
            // 方法上没有注解，则从类上获取
            matches = checkInClass(targetClass);
            attributeCache.put(cacheKey, matches);
            return matches;
        }

        public boolean checkInClass(Class<?> clazz) {
            return (this.checkInherited ? AnnotatedElementUtils.hasAnnotation(clazz, this.annotationType) :
                    clazz.isAnnotationPresent(this.annotationType));
        }

        private MethodClassKey getCacheKey(Method method, @Nullable Class<?> targetClass) {
            return new MethodClassKey(method, targetClass);
        }
    }

}
```



```java
/**
 * @author 简单de快乐
 * @create 2024-07-03 16:38
 *
 * 日志通知
 */
public class LogInterceptor implements MethodInterceptor {

    public static final Logger LOGGER = LoggerFactory.getLogger(LogInterceptor.class);

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        ReflectiveMethodInvocation reflectiveMethodInvocation = (ReflectiveMethodInvocation)invocation;
        Method method = reflectiveMethodInvocation.getMethod();
        Object[] arguments = reflectiveMethodInvocation.getArguments();
        LOGGER.info("开始调用：{}类的{}方法，调用参数：{}", method.getDeclaringClass(), method.getName(), Arrays.toString(arguments));
        Object proceed = invocation.proceed();
        LOGGER.info("完成调用：{}类的{}方法，方法返回值：{}", method.getDeclaringClass(), method.getName(),  proceed);
        return proceed;
    }
}
```



* 切面

```java
@Aspect
public class LoggingAspect {

    private static final Logger logger = LoggerFactory.getLogger(LoggingAspect.class);

    // 定义切入点，匹配被 @Log 注解标记的类和方法
    @Pointcut("@within(com.mrlu.core.anno.Log) || @annotation(com.mrlu.core.anno.Log)")
    public void logAnnotated() {}

    // 在方法执行前执行日志记录
    @Before("logAnnotated()")
    public void logBefore(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getName();
        Object[] args = joinPoint.getArgs();
        logger.info("开始调用方法 {}#{}，参数为: {}", className, methodName, args);
    }

    // 在方法执行后执行日志记录
    @AfterReturning(pointcut = "logAnnotated()", returning = "result")
    public void logAfterReturning(JoinPoint joinPoint, Object result) {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getName();

        logger.info("方法 {}#{} 执行完成，返回值为: {}", className, methodName, result);
    }
}
```

示例工程： [aop-custom](F:\code\scene\aop-custom) 

# 六、补充

## 1、cglib动态代理示例

```java
import org.springframework.cglib.proxy.*;
import java.io.Serializable;
import java.lang.reflect.Method;

/**
 * cglib动态代理demo
 */
public class CglibProxyExample {
    public static void main(String[] args) {
        Enhancer enhancer = new Enhancer();
        // 设置被代理的类
        enhancer.setSuperclass(MyClass.class);
        // 设置回调列表
        Callback[] mainCallbacks = new Callback[] {
            new AopInterceptor(),
            NoOp.INSTANCE,
            new SerializableNoOp(),
            new TargetDispatcher(),
            new AdvisedDispatcher(),
            new EqualsInterceptor(),
            new HashCodeInterceptor()
        };
        enhancer.setCallbacks(mainCallbacks);
        // 设置回调过滤器。确定回调列表的索引，决定使用的回调器。
        // 多个回调器的时候，必须设置
        enhancer.setCallbackFilter(new MyCallbackFilter());
        MyClass proxy = (MyClass) enhancer.create();
        proxy.publicMethod();
        proxy.advisedDispatcherLoad();
        proxy.targetDispatcherLoad();
        proxy.noOp();
        System.out.println(proxy.hashCode());
        System.out.println(proxy.equals(new MyClass()));
        System.out.println("========================================");

        // 以下是指定一个回调，无需设置过滤器
        Enhancer enhancer02 = new Enhancer();
        // 设置被代理的类
        enhancer02.setSuperclass(MyClass02.class);
        enhancer02.setCallback(new AopInterceptor());
        MyClass02 myClass02 = (MyClass02) enhancer02.create();
        myClass02.publicMethod();
    }
}

class MyCallbackFilter implements CallbackFilter {
    @Override
    public int accept(Method method) {
        if (method.getName().equals("equals")) {
            return 5;
        } else if (method.getName().equals("hashCode")) {
            return 6;
        } else if (method.getName().equals("targetDispatcherLoad")) {
            return 3;
        } else if (method.getName().equals("advisedDispatcherLoad")) {
            return 4;
        } else if (method.getName().equals("noOp")) {
            return 1;
        }
        return 0;
    }
}
class MyClass02 {
    public void publicMethod() {
        System.out.println("MyClass02 Public method called");
    }

}


class MyClass {
    public void publicMethod() {
        System.out.println("Public method called");
    }

    public void targetDispatcherLoad() {
        System.out.println("targetDispatcherLoad");
    }

    public void advisedDispatcherLoad() {
        System.out.println("advisedDispatcherLoad");
    }

    public void noOp() {
        System.out.println("=========noOp=========");
    }


    @Override
    public boolean equals(Object obj) {
        return obj instanceof MyClass;
    }

    @Override
    public int hashCode() {
        return 42;
    }
}

class AopInterceptor implements MethodInterceptor {
    @Override
    public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
        System.out.println("begin AOP Interceptor: " + method.getName());
        Object value = proxy.invokeSuper(obj, args);
        System.out.println("finish AOP Interceptor: " + method.getName());
        return value;
    }
}

class SerializableNoOp implements NoOp, Serializable {}

class TargetDispatcher implements Dispatcher {
    @Override
    public Object loadObject() throws Exception {
        System.out.println("TargetDispatcher loadObject");
        return new MyClass();
    }
}

class AdvisedDispatcher implements Dispatcher {
    @Override
    public Object loadObject() throws Exception {
        System.out.println("AdvisedDispatcher loadObject");
        return new MyClass();
    }
}

class EqualsInterceptor implements MethodInterceptor {
    @Override
    public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
        System.out.println("Equals Interceptor");
        return proxy.invokeSuper(obj, args);
    }
}

class HashCodeInterceptor implements MethodInterceptor {
    @Override
    public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
        System.out.println("HashCode Interceptor");
        return proxy.invokeSuper(obj, args);
    }
}
```

