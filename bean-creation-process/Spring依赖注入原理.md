## 前置说明

（1）要求熟悉bean的生命周期

（2）源码版本：本文源码基于spring-framework-5.3.12

（3）@Autowired：见AutowiredAnnotationBeanPostProcessor类

具体：org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor.postProcessProperties(PropertyValues, Object, String)

（4）@Resource：见CommonAnnotationBeanPostProcessor类 

具体：org.springframework.context.annotationCommonAnnotationBeanPostProcessor.postProcessProperties(PropertyValues pvs, Object bean, String beanName)

（5）依赖注入是在bean属性填充的阶段。主要在AbstractAutowireCapableBeanFactory的populateBean方法

```java
protected void populateBean(String beanName, RootBeanDefinition mbd, @Nullable BeanWrapper bw) {		
    // 获取bean定义里的属性值
    PropertyValues pvs = (mbd.hasPropertyValues() ? mbd.getPropertyValues() : null);
    // 默认是AUTOWIRE_NO。不进入if
    // 注册手动注册bean定义时候，指定了AUTOWIRE_BY_NAME或AUTOWIRE_BY_TYPE
    int resolvedAutowireMode = mbd.getResolvedAutowireMode();
    if (resolvedAutowireMode == AUTOWIRE_BY_NAME || resolvedAutowireMode == AUTOWIRE_BY_TYPE) {
        MutablePropertyValues newPvs = new MutablePropertyValues(pvs);
        // Add property values based on autowire by name if applicable.
        if (resolvedAutowireMode == AUTOWIRE_BY_NAME) {
            // 根据名称注入。这里的核心逻辑和bp.postProcessProperties的核心逻辑是一样的
            autowireByName(beanName, mbd, bw, newPvs);
        }
        // Add property values based on autowire by type if applicable.
        if (resolvedAutowireMode == AUTOWIRE_BY_TYPE) {
            // 根据类型注入。这里的核心逻辑和bp.postProcessProperties的核心逻辑是一样的
            autowireByType(beanName, mbd, bw, newPvs);
        }
        pvs = newPvs;
    }
    
    // ...
    boolean hasInstAwareBpps = hasInstantiationAwareBeanPostProcessors();	

    PropertyDescriptor[] filteredPds = null;
    if (hasInstAwareBpps) {
        if (pvs == null) {
            pvs = mbd.getPropertyValues();
        }
        // 遍历所有的BeanPostProcessor，执行postProcessProperties方法
        for (InstantiationAwareBeanPostProcessor bp : getBeanPostProcessorCache().instantiationAware) {
            // 默认在这里进行依赖注入。可以从这里开始阅读
            // @Autowired注解注入对应AutowiredAnnotationBeanPostProcessor
            // @Resource注解注入对应CommonAnnotationBeanPostProcessor
            PropertyValues pvsToUse = bp.postProcessProperties(pvs, bw.getWrappedInstance(), beanName);
            if (pvsToUse == null) {
                if (filteredPds == null) {
                    filteredPds = filterPropertyDescriptorsForDependencyCheck(bw, mbd.allowCaching);
                }
                pvsToUse = bp.postProcessPropertyValues(pvs, filteredPds, bw.getWrappedInstance(), beanName);
                if (pvsToUse == null) {
                    return;
                }
            }
            pvs = pvsToUse;
        }
    }
    // ...

    if (pvs != null) {
        // 设置（程序员手动指定的）属性。最终也是通过反射调用set方法设置属性值
        applyPropertyValues(beanName, mbd, bw, pvs);
    }
}

/**
 * Return property values for this bean (never {@code null}).
 */
@Override
public MutablePropertyValues getPropertyValues() {
    if (this.propertyValues == null) {
        this.propertyValues = new MutablePropertyValues();
    }
    return this.propertyValues;
}
```



## @Autowired注入

**注：包含了@Qualifier的情况**

```java
@Override
public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName) {
    // 寻找注入点(元数据)。即找到所有被@Autowired标注的Field或Method，封装返回
    InjectionMetadata metadata = findAutowiringMetadata(beanName, bean.getClass(), pvs);
    try {
        // 依赖注入。Field和Method的方式不一样
        metadata.inject(bean, beanName, pvs);
    }
    catch (BeanCreationException ex) {
        throw ex;
    }
    catch (Throwable ex) {
        throw new BeanCreationException(beanName, "Injection of autowired dependencies failed", ex);
    }
    // 返回所有的属性值对象
    return pvs;
}
```



```java
/**
* 依赖注入：循环每个注入点进行注入。区分属性和方法注入。
*/
public void inject(Object target, @Nullable String beanName, @Nullable PropertyValues pvs) throws Throwable {
    // 获取所有的注入点
    Collection<InjectedElement> checkedElements = this.checkedElements;
    Collection<InjectedElement> elementsToIterate =
            (checkedElements != null ? checkedElements : this.injectedElements);
    if (!elementsToIterate.isEmpty()) {
        // 遍历每个注入点进行依赖注入
        // 属性注入对应AutowiredFieldElement
        // 方法注入对应AutowiredMethodElement
        for (InjectedElement element : elementsToIterate) {           
            element.inject(target, beanName, pvs);
        }
    }
}
```

### 寻找注入点(元数据)

AutowiredAnnotationBeanPostProcessor

```java
// 注入点缓存
private final Map<String, InjectionMetadata> injectionMetadataCache = new ConcurrentHashMap<>(256);

private InjectionMetadata findAutowiringMetadata(String beanName, Class<?> clazz, @Nullable PropertyValues pvs) {
    // 获取缓存的key。如果beanName不是空串，则作为缓存的key，否则用类名。
    // Fall back to class name as cache key, for backwards compatibility with custom callers.
    String cacheKey = (StringUtils.hasLength(beanName) ? beanName : clazz.getName());
    // Quick check on the concurrent map first, with minimal locking.
    // 从缓存中获取
    InjectionMetadata metadata = this.injectionMetadataCache.get(cacheKey);
    // 是否需要刷新缓存
    if (InjectionMetadata.needsRefresh(metadata, clazz)) {
        // 双检
        synchronized (this.injectionMetadataCache) {
            metadata = this.injectionMetadataCache.get(cacheKey);
            if (InjectionMetadata.needsRefresh(metadata, clazz)) {
                if (metadata != null) {
                    // 从PropertyValues中移除该注入点
                    metadata.clear(pvs);
                }
                // 构建Autowired注入点(元数据)
                metadata = buildAutowiringMetadata(clazz);
                // 添加到缓存
                this.injectionMetadataCache.put(cacheKey, metadata);
            }
        }
    }
    // 返回注入点
    return metadata;
}

/**
 * 是否需要刷新注入点
 * Check whether the given injection metadata needs to be refreshed.
 * @param metadata the existing metadata instance
 * @param clazz the current target class
 * @return {@code true} indicating a refresh, {@code false} otherwise
 * @see #needsRefresh(Class)
 */
public static boolean needsRefresh(@Nullable InjectionMetadata metadata, Class<?> clazz) {
    // 注入点为空，或者注入点的类不等于clazz，就需要刷新
    return (metadata == null || metadata.needsRefresh(clazz));
}
/**
 * Determine whether this metadata instance needs to be refreshed.
 * @param clazz the current target class  当前目标类
 * @return {@code true} indicating a refresh, {@code false} otherwise
 * @since 5.2.4
 */
protected boolean needsRefresh(Class<?> clazz) {
    return this.targetClass != clazz;
}
```



####  构建Autowired注入点(元数据)

```java
private InjectionMetadata buildAutowiringMetadata(final Class<?> clazz) {    
    //（1）类名不是以java开头的注解
    //（2）clazz不是Order.class（org.springframework.core.Order）
    //（3）clazz的类名不是以java开头        
    // 说白，不是jdk包的注解和类，而且不是Order.class类，就都是候选类，返回true   
    if (!AnnotationUtils.isCandidateClass(clazz, this.autowiredAnnotationTypes)) {
        return InjectionMetadata.EMPTY;
    }

    // 创建elements集合，保存所有需要注入的方法或属性
    List<InjectionMetadata.InjectedElement> elements = new ArrayList<>();    
    Class<?> targetClass = clazz;

    do {
        final List<InjectionMetadata.InjectedElement> currElements = new ArrayList<>();

        // 通过反射获取@Autowired注解或@Value注解或@Inject注解的非静态属性，封装成AutowiredFieldElement注入点
        ReflectionUtils.doWithLocalFields(targetClass, field -> {
            MergedAnnotation<?> ann = findAutowiredAnnotation(field);
            if (ann != null) {
                // @Autowired注解不能用于静态属性
                if (Modifier.isStatic(field.getModifiers())) {
                    if (logger.isInfoEnabled()) {
                        logger.info("Autowired annotation is not supported on static fields: " + field);
                    }
                    return;
                }
                // 获取@Autowired注解的required属性
                boolean required = determineRequiredStatus(ann);
                // 【注意】属性创建的是AutowiredFieldElement
                currElements.add(new AutowiredFieldElement(field, required));
            }
        });
	
        // 通过反射获取@Autowired注解的非静态且有参数的方法，封装成AutowiredMethodElement注入点
        ReflectionUtils.doWithLocalMethods(targetClass, method -> {
            Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(method);
            if (!BridgeMethodResolver.isVisibilityBridgeMethodPair(method, bridgedMethod)) {
                return;
            }
            MergedAnnotation<?> ann = findAutowiredAnnotation(bridgedMethod);
            if (ann != null && method.equals(ClassUtils.getMostSpecificMethod(method, clazz))) {				                   // @Autowired注解不能用于静态方法
                if (Modifier.isStatic(method.getModifiers())) {
                    if (logger.isInfoEnabled()) {
                        logger.info("Autowired annotation is not supported on static methods: " + method);
                    }
                    return;
                }
                // @Autowired注解不能用于无参方法
                if (method.getParameterCount() == 0) {
                    if (logger.isInfoEnabled()) {
                        logger.info("Autowired annotation should only be used on methods with parameters: " +
                                method);
                    }
                }
                // 获取@Autowired注解的required属性
                boolean required = determineRequiredStatus(ann);
                // 获取属性的描述符。属性的set方法名称或get方法名称等于bridgedMethod方法名称的都是
                PropertyDescriptor pd = BeanUtils.findPropertyForMethod(bridgedMethod, clazz);
                // 【注意】方法创建的是AutowiredMethodElement
                currElements.add(new AutowiredMethodElement(method, required, pd));
            }
        });
        // 保存找到的注入点
        elements.addAll(0, currElements);
        // 继续从父类获取
        targetClass = targetClass.getSuperclass();
    }
    while (targetClass != null && targetClass != Object.class);
    
    // 创建注入点元数据并返回
    return InjectionMetadata.forElements(elements, clazz);
}

// 从访问对象中找到@Autowired注解或@Value注解或@Inject注解
private MergedAnnotation<?> findAutowiredAnnotation(AccessibleObject ao) {
    MergedAnnotations annotations = MergedAnnotations.from(ao);
    for (Class<? extends Annotation> type : this.autowiredAnnotationTypes) {
        MergedAnnotation<?> annotation = annotations.get(type);
        // 是否有相关注解存在
        if (annotation.isPresent()) {
            return annotation;
        }
    }
    return null;
}

// 自动装配的注解类型
private final Set<Class<? extends Annotation>> autowiredAnnotationTypes = new LinkedHashSet<>(4);
public AutowiredAnnotationBeanPostProcessor() {
    // 这里添加了@Autowired和@Value注解的类
    this.autowiredAnnotationTypes.add(Autowired.class);
    this.autowiredAnnotationTypes.add(Value.class);
    try {
        // 如果类路径有@Inject注解，则添加
        this.autowiredAnnotationTypes.add((Class<? extends Annotation>)
                ClassUtils.forName("javax.inject.Inject", AutowiredAnnotationBeanPostProcessor.class.getClassLoader()));
        logger.trace("JSR-330 'javax.inject.Inject' annotation found and supported for autowiring");
    }
    catch (ClassNotFoundException ex) {
        // JSR-330 API not available - simply skip.
    }
}

// 【注意】属性创建的是AutowiredFieldElement
private final boolean required;
public AutowiredFieldElement(Field field, boolean required) {
    super(field, null);
    // 是否是必须的。默认设置为true
    this.required = required;
}

// 【注意】方法创建的是AutowiredMethodElement
private final boolean required;
public AutowiredMethodElement(Method method, boolean required, @Nullable PropertyDescriptor pd) {
    super(method, pd);
    // 是否是必须的。默认设置为true
    this.required = required;
}

// super(field, null); 与 super(method, pd);都是调用这个
protected final Member member;
protected final boolean isField;
@Nullable
protected final PropertyDescriptor pd;
protected InjectedElement(Member member, @Nullable PropertyDescriptor pd) {
    this.member = member;
    this.isField = (member instanceof Field);
    this.pd = pd;
}


/**
 * Return an {@code InjectionMetadata} instance, possibly for empty elements.
 * @param elements the elements to inject (possibly empty)
 * @param clazz the target class
 * @return a new {@link #InjectionMetadata(Class, Collection)} instance
 * @since 5.2
 */
public static InjectionMetadata forElements(Collection<InjectedElement> elements, Class<?> clazz) {
    // 创建注入元数据
    return (elements.isEmpty() ? new InjectionMetadata(clazz, Collections.emptyList()) :
            new InjectionMetadata(clazz, elements));
}
/**
 * Create a new {@code InjectionMetadata instance}.
 * <p>Preferably use {@link #forElements} for reusing the {@link #EMPTY}
 * instance in case of no elements.
 * @param targetClass the target class
 * @param elements the associated elements to inject
 * @see #forElements
 */
public InjectionMetadata(Class<?> targetClass, Collection<InjectedElement> elements) {
    this.targetClass = targetClass;
    this.injectedElements = elements;
}

// 目标类
private final Class<?> targetClass;
// 所有的注入成员
private final Collection<InjectedElement> injectedElements;
```

【注意】**@Autowired注解不能用于静态方法和无参方法，不能用于静态属性。**



### 属性注入

AutowiredFieldElement

```java
@Override
protected void inject(Object bean, @Nullable String beanName, @Nullable PropertyValues pvs) throws Throwable {
    // 获取当前注入的属性
    Field field = (Field) this.member;
    // 定义一个具体注入的值
    Object value;
    // 如果有缓存，从缓存获取注入的值。如果没有，则解析注入的值
    if (this.cached) {        
        try {
            // 第一次创建的时候，找注入点，然后进行注入，也就是cached为false。注入完后cached才为true。
            // 第二次创建的时候，先找注入点（此时会拿到缓存好的注入点）
            // 即AutowiredFieldElement对象，此时缓存为true，就进入到这里
            // 注入点内并没有缓存被注入的具体Bean对象，而是beanName，这样就能保证注入到不同的原型Bean对象
            
            // 从缓存获取注入的值。
            value = resolvedCachedArgument(beanName, this.cachedFieldValue);
        }
        catch (NoSuchBeanDefinitionException ex) {
            // Unexpected removal of target bean for cached argument -> re-resolve
            value = resolveFieldValue(field, bean, beanName);
        }
    }
    else {
        // 解析注入的属性值。即根据field从beanFactory中获取匹配的bean对象
        value = resolveFieldValue(field, bean, beanName);
    }
    // 注入的值不等于null
    if (value != null) {       
        // 反射设置属性值到bean
        ReflectionUtils.makeAccessible(field);
        field.set(bean, value);
    }
}
```



```java
/**
 * 解析指定的缓存方法参数或字段值。
 * Resolve the specified cached method argument or field value.
 */
@Nullable
private Object resolvedCachedArgument(@Nullable String beanName, @Nullable Object cachedArgument) {
    if (cachedArgument instanceof DependencyDescriptor) {
        // 如果缓存值是DependencyDescriptor类型。如果有缓存的话，一般会执行到这里
        DependencyDescriptor descriptor = (DependencyDescriptor) cachedArgument;
        Assert.state(this.beanFactory != null, "No BeanFactory available");
        // 从缓存的依赖描述器获取注入的值。
        return this.beanFactory.resolveDependency(descriptor, beanName, null, null);
    }
    else {
        // 直接返回缓存的值。
        return cachedArgument;
    }
}
```



#### 解析注入的属性值

```java
/**
* 根据field从beanFactory中获取匹配的bean对象
*/
@Nullable
private Object resolveFieldValue(Field field, Object bean, @Nullable String beanName) {
      // 根据当前属性以及@Autowired(required=true)创建一个属性描述器
      DependencyDescriptor desc = new DependencyDescriptor(field, this.required);
      // 设置属性描述器对应的class为当前bean的class
      desc.setContainingClass(bean.getClass());
      // 创建注入的集合。保存所有需要注入的beanName。（所有自动装配的beanName）
      Set<String> autowiredBeanNames = new LinkedHashSet<>(1);
      Assert.state(beanFactory != null, "No BeanFactory available");
      // 获取当前bean工厂的类型转化器
      TypeConverter typeConverter = beanFactory.getTypeConverter();
      // 具体注入的值
      Object value;
      try {
         // 获取要注入的值
         value = beanFactory.resolveDependency(desc, beanName, autowiredBeanNames, typeConverter);
      }
      catch (BeansException ex) {
         throw new UnsatisfiedDependencyException(null, beanName, new InjectionPoint(field), ex);
      }
      synchronized (this) {
         // 缓存不存在，则添加缓存
         if (!this.cached) {
            // 缓存的属性值
            Object cachedFieldValue = null;
             // 要注入的值不等于null，或者是必须注入的
            if (value != null || this.required) {
               // 设置缓存的属性值为依赖描述器
               cachedFieldValue = desc;
               // 注册依赖的bean（记录beanName依赖autowiredBeanNames，同时记录autowiredBeanNames被beanName依赖了） 
               registerDependentBeans(beanName, autowiredBeanNames);
               // 如果只有一个自动注入的bean名称存在，则添加缓存。 
               if (autowiredBeanNames.size() == 1) {
                   // 获取自动注入的beanName
                  String autowiredBeanName = autowiredBeanNames.iterator().next();
                  // bean工厂中包含注入的bean名称，而且注入的bean的类型和属性的类型一致
                  if (beanFactory.containsBean(autowiredBeanName) &&
                        beanFactory.isTypeMatch(autowiredBeanName, field.getType())) {
                     //  创建快捷方式依赖描述器ShortcutDependencyDescriptor。
                     //  缓存注入的bean名称和类型以及依赖描述器，而不是具体的bean对象。
                     cachedFieldValue = new ShortcutDependencyDescriptor(
                           desc, autowiredBeanName, field.getType());
                  }
               }
            }
            // 全局缓存的属性描述符赋值
            this.cachedFieldValue = cachedFieldValue;
            // 注入点设置缓存标识为true，表示有缓存。
            this.cached = true;
         }
      }
      // 返回具体注入的值
      return value;
    }
}

/**
 *  注册依赖的bean 
 * Register the specified bean as dependent on the autowired beans.
 */
private void registerDependentBeans(@Nullable String beanName, Set<String> autowiredBeanNames) {
    if (beanName != null) {
        for (String autowiredBeanName : autowiredBeanNames) {
            if (this.beanFactory != null && this.beanFactory.containsBean(autowiredBeanName)) {
                this.beanFactory.registerDependentBean(autowiredBeanName, beanName);
            }
            if (logger.isTraceEnabled()) {
                logger.trace("Autowiring by type from bean name '" + beanName +
                        "' to bean named '" + autowiredBeanName + "'");
            }
        }
    }
}

//bean名称到依赖bean名称的集合。即bean的名称，和依赖了它的bean的名称集合
/** Map between dependent bean names: bean name to Set of dependent bean names. */
private final Map<String, Set<String>> dependentBeanMap = new ConcurrentHashMap<>(64);

// bean名称 和 当前bean下依赖了哪些bean的名称集合
/** Map between depending bean names: bean name to Set of bean names for the bean's dependencies. */
private final Map<String, Set<String>> dependenciesForBeanMap = new ConcurrentHashMap<>(64);

/**
 * Register a dependent bean for the given bean,
 * to be destroyed before the given bean is destroyed.
 * @param beanName the name of the bean
 * @param dependentBeanName the name of the dependent bean
 */
public void registerDependentBean(String beanName, String dependentBeanName) {
    String canonicalName = canonicalName(beanName);

    // 注册beanName依赖了dependentBeanName
    synchronized (this.dependentBeanMap) {
        Set<String> dependentBeans =
                this.dependentBeanMap.computeIfAbsent(canonicalName, k -> new LinkedHashSet<>(8));
        if (!dependentBeans.add(dependentBeanName)) {
            return;
        }
    }
    
    // 记录dependentBeanName被beanName依赖
    synchronized (this.dependenciesForBeanMap) {
        Set<String> dependenciesForBean =
                this.dependenciesForBeanMap.computeIfAbsent(dependentBeanName, k -> new LinkedHashSet<>(8));
        dependenciesForBean.add(canonicalName);
    }
}

// 是否包含该bean
@Override
public boolean containsBean(String name) {
    // 去除工厂bean前缀&，如果有的话
    String beanName = transformedBeanName(name);
    // 是否包含该单实例bean或包含bean定义
    if (containsSingleton(beanName) || containsBeanDefinition(beanName)) {
        // 不是工厂bean自身引用的名称，或者是工厂bean
        return (!BeanFactoryUtils.isFactoryDereference(name) || isFactoryBean(name));
    }
    // Not found -> check parent.
    BeanFactory parentBeanFactory = getParentBeanFactory();
    return (parentBeanFactory != null && parentBeanFactory.containsBean(originalBeanName(name)));
}

/**
 * Return whether the given name is a factory dereference
 * (beginning with the factory dereference prefix).
 * @param name the name of the bean
 * @return whether the given name is a factory dereference
 * @see BeanFactory#FACTORY_BEAN_PREFIX
 */
public static boolean isFactoryDereference(@Nullable String name) {
    return (name != null && name.startsWith(BeanFactory.FACTORY_BEAN_PREFIX));
}
String FACTORY_BEAN_PREFIX = "&";

@Override
public boolean isFactoryBean(String name) throws NoSuchBeanDefinitionException {
    String beanName = transformedBeanName(name);
    Object beanInstance = getSingleton(beanName, false);
    if (beanInstance != null) {
        return (beanInstance instanceof FactoryBean);
    }
    // No singleton instance found -> check bean definition.
    if (!containsBeanDefinition(beanName) && getParentBeanFactory() instanceof ConfigurableBeanFactory) {
        // No bean definition found in this factory -> delegate to parent.
        return ((ConfigurableBeanFactory) getParentBeanFactory()).isFactoryBean(name);
    }
    return isFactoryBean(beanName, getMergedLocalBeanDefinition(beanName));
}
```



#### 获取要注入的值

```java
/**
 * DependencyDescriptor descriptor：依赖描述器，可能是字段，方法
 * requestingBeanName：正在进行依赖注入的bean
 * autowiredBeanNames：需要注入的BeanName集合（保存所有需要注入的bean对象）
 * typeConverter：类型转换器
 */
@Override
@Nullable
public Object resolveDependency(DependencyDescriptor descriptor, @Nullable String requestingBeanName,
        @Nullable Set<String> autowiredBeanNames, @Nullable TypeConverter typeConverter) throws BeansException {
    // 初始化方法参数名称发现器，用于获取方法入参名字
    descriptor.initParameterNameDiscovery(getParameterNameDiscoverer());
    // 所需要的类型是Optional，包装成Optional对象，核心也是调用doResolveDependency方法
    if (Optional.class == descriptor.getDependencyType()) {
        return createOptionalDependency(descriptor, requestingBeanName);
    }
    // 所需要的类型是ObjectFactory或ObjectProvider，包装成ObjectFactory对象或ObjectProvider对象
    // 核心也是调用doResolveDependency方法
    else if (ObjectFactory.class == descriptor.getDependencyType() ||
            ObjectProvider.class == descriptor.getDependencyType()) {
        return new DependencyObjectProvider(descriptor, requestingBeanName);
    }
    // 所需要的类型是javaxInjectProviderClass，包装成javaxInjectProviderClass类型对象
    // 核心也是调用doResolveDependency方法
    else if (javaxInjectProviderClass == descriptor.getDependencyType()) {
        return new Jsr330Factory().createDependencyProvider(descriptor, requestingBeanName);
    }
    else {
        // 在属性或方法上使用@Lazy注解，则构造一个CGLB代理对象并返回，真正使用该代理对象时才进行类型筛选bean
        Object result = getAutowireCandidateResolver().getLazyResolutionProxyIfNecessary(
                descriptor, requestingBeanName);      
        if (result == null) {
            // 没有@Lazy注解的时候
            // descriptor：依赖描述器，字段或方法
            // requestingBeanName：表示正在进行依赖注入的Bean
            // 执行解析依赖的操作
            result = doResolveDependency(descriptor, requestingBeanName, autowiredBeanNames, typeConverter);
        }
        // 返回要注入的值
        return result;
    }
}
```

* getLazyResolutionProxyIfNecessary

```java
// ContextAnnotationAutowireCandidateResolver
public Object getLazyResolutionProxyIfNecessary(DependencyDescriptor descriptor, @Nullable String beanName) {
    // 如果是延迟解析，创建延迟解析代理对象，否则返回null    
    return (isLazy(descriptor) ? buildLazyResolutionProxy(descriptor, beanName) : null);
}

// 是否有@Lazy注解存在
protected boolean isLazy(DependencyDescriptor descriptor) {
    // 获取与包装字段或方法/构造函数参数相关的注解。
    for (Annotation ann : descriptor.getAnnotations()) {
        Lazy lazy = AnnotationUtils.getAnnotation(ann, Lazy.class);
        // 如果@Lazy注解不等于null，而且@Lazy注解的value值等于true，则表示存在。
        if (lazy != null && lazy.value()) {
            return true;
        }
    }
    // 获取方法参数器
    MethodParameter methodParam = descriptor.getMethodParameter();
    if (methodParam != null) {
        // 获取方法
        Method method = methodParam.getMethod();
        // 方法等于null，获取方法的返回值类型等于void类型
        if (method == null || void.class == method.getReturnType()) {
            // 获取方法上的@Lazy注解
            Lazy lazy = AnnotationUtils.getAnnotation(methodParam.getAnnotatedElement(), Lazy.class);
            // 如果@Lazy注解不等于null，而且@Lazy注解的value值等于true，则表示存在。
            if (lazy != null && lazy.value()) {
                return true;
            }
        }
    }
    // 返回false表示没有@Lazy注解
    return false;
}

// 创建延迟解析代理对象
protected Object buildLazyResolutionProxy(final DependencyDescriptor descriptor, final @Nullable String beanName) {
    BeanFactory beanFactory = getBeanFactory();
    Assert.state(beanFactory instanceof DefaultListableBeanFactory,
            "BeanFactory needs to be a DefaultListableBeanFactory");
    final DefaultListableBeanFactory dlbf = (DefaultListableBeanFactory) beanFactory;

    TargetSource ts = new TargetSource() {
        // 返回依赖类型作为目标类型
        @Override
        public Class<?> getTargetClass() {
            return descriptor.getDependencyType();
        }
        // 返回不是静态的
        @Override
        public boolean isStatic() {
            return false;
        }
        // 获取目标对象。本质上还是要解析依赖
        @Override
        public Object getTarget() {
            Set<String> autowiredBeanNames = (beanName != null ? new LinkedHashSet<>(1) : null);
            // descriptor：依赖描述器，字段或方法
            // 执行解析依赖的操作
            Object target = dlbf.doResolveDependency(descriptor, beanName, autowiredBeanNames, null);
            if (target == null) {
                Class<?> type = getTargetClass();
                if (Map.class == type) {
                    return Collections.emptyMap();
                }
                else if (List.class == type) {
                    return Collections.emptyList();
                }
                else if (Set.class == type || Collection.class == type) {
                    return Collections.emptySet();
                }
                throw new NoSuchBeanDefinitionException(descriptor.getResolvableType(),
                        "Optional dependency not present for lazy injection point");
            }
            if (autowiredBeanNames != null) {
                for (String autowiredBeanName : autowiredBeanNames) {
                    if (dlbf.containsBean(autowiredBeanName)) {
                        // 注册依赖关系
                        dlbf.registerDependentBean(autowiredBeanName, beanName);
                    }
                }
            }
            // 返回目标对象
            return target;
        }
        @Override
        public void releaseTarget(Object target) {
        }
    };
    
    // 创建代理工厂
    ProxyFactory pf = new ProxyFactory();
    // 设置目标来源
    pf.setTargetSource(ts);
    // 获取依赖类型
    Class<?> dependencyType = descriptor.getDependencyType();
    if (dependencyType.isInterface()) {
        // 添加代理接口
        pf.addInterface(dependencyType);
    }
    // 使用代理工厂、类加载器获取代理对象
    return pf.getProxy(dlbf.getBeanClassLoader());
}

/**
 * 获取与包装字段或方法/构造函数参数相关的注解。 
 * Obtain the annotations associated with the wrapped field or method/constructor parameter.
 */
public Annotation[] getAnnotations() {
    if (this.field != null) {
        Annotation[] fieldAnnotations = this.fieldAnnotations;
        if (fieldAnnotations == null) {
            // 获取属性上的注解
            fieldAnnotations = this.field.getAnnotations();
            this.fieldAnnotations = fieldAnnotations;
        }
        return fieldAnnotations;
    }
    else {
        // 获取方法或者构造方法上的注解
        return obtainMethodParameter().getParameterAnnotations();
    }
}

// MethodParameter
/**
 * 返回与特定方法/构造函数参数关联的注解。
 * Return the annotations associated with the specific method/constructor parameter.
 */
public Annotation[] getParameterAnnotations() {
    Annotation[] paramAnns = this.parameterAnnotations;
    if (paramAnns == null) {
        // 获取方法或者构造方法参数上的注解
        Annotation[][] annotationArray = this.executable.getParameterAnnotations();
        int index = this.parameterIndex;
        if (this.executable instanceof Constructor &&
                ClassUtils.isInnerClass(this.executable.getDeclaringClass()) &&
                annotationArray.length == this.executable.getParameterCount() - 1) {
            // Bug in javac in JDK <9: annotation array excludes enclosing instance parameter
            // for inner classes, so access it with the actual parameter index lowered by 1
            index = this.parameterIndex - 1;
        }
        paramAnns = (index >= 0 && index < annotationArray.length ?
                adaptAnnotationArray(annotationArray[index]) : EMPTY_ANNOTATION_ARRAY);
        this.parameterAnnotations = paramAnns;
    }
    return paramAnns;
}
```



#### 解析依赖（doResolveDependency）

依赖注入的核心方法

```java
@Nullable
public Object doResolveDependency(DependencyDescriptor descriptor, @Nullable String beanName,
        @Nullable Set<String> autowiredBeanNames, @Nullable TypeConverter typeConverter) throws BeansException {
	// 设置当前的descriptor(存储了方法、属性参数等信息)为当前的注入点
    InjectionPoint previousInjectionPoint = ConstructorResolver.setCurrentInjectionPoint(descriptor);
    try {
        // 如果descriptor之前做过了依赖注入
        // 则直接从shortcut中获取（缓存的descriptor是ShortcutDependencyDescriptor类型），相当从缓存获取。
        Object shortcut = descriptor.resolveShortcut(this);
        if (shortcut != null) {
            return shortcut;
        }
	   
        // 1、获取依赖的类型
        Class<?> type = descriptor.getDependencyType();
        // 获取@Value的值
        Object value = getAutowireCandidateResolver().getSuggestedValue(descriptor);
        if (value != null) {
            // 属性或方法存在@Value注解
            if (value instanceof String) {
                // 占位符填充（${}）
                String strVal = resolveEmbeddedValue((String) value);
                BeanDefinition bd = (beanName != null && containsBean(beanName) ?
                        getMergedBeanDefinition(beanName) : null);
                // 解析Spring表达式（#{}）
                value = evaluateBeanDefinitionString(strVal, bd);
            }
            // 将value转化为descriptor所对应的类型
            TypeConverter converter = (typeConverter != null ? typeConverter : getTypeConverter());
            try {
                // 如果有必要的，将value进行类型转换后返回
                return converter.convertIfNecessary(value, type, descriptor.getTypeDescriptor());
            }
            catch (UnsupportedOperationException ex) {
                // 不是string转bean，变为使用自定义的转化器
                // A custom TypeConverter which does not support TypeDescriptor resolution...
                return (descriptor.getField() != null ?
                        converter.convertIfNecessary(value, type, descriptor.getField()) :
                        converter.convertIfNecessary(value, type, descriptor.getMethodParameter()));
            }
        }
        
        // 2、解析多个bean
        // 如果依赖的类型是数组、集合、Map、Stream这些，则解析封装好并返回
        Object multipleBeans = resolveMultipleBeans(descriptor, beanName, autowiredBeanNames, typeConverter);
        if (multipleBeans != null) {
            return multipleBeans;
        }

        // 3、找到(所有)匹配类型的bean实例
        // key:bean名称 value：对应的Class或bean对象
        Map<String, Object> matchingBeans = findAutowireCandidates(beanName, type, descriptor);
        if (matchingBeans.isEmpty()) {
            // 没有匹配的结果，如果是必须注入的，则抛出异常
            if (isRequired(descriptor)) {
                // 抛出NoSuchBeanDefinitionException异常
                raiseNoMatchingBeanFound(type, descriptor.getResolvableType(), descriptor);
            }
            return null;
        }

        // 定义自动注入的bean名称
        String autowiredBeanName;
        // 定义要注入的bean实例
        Object instanceCandidate;

        if (matchingBeans.size() > 1) {
		   // 根据类型找到多个bean，进一步确定唯一依赖的bean名称
            // 4、确定唯一依赖的bean           
            // @Primary --> @Priority --> 候选bean名称等于依赖描述器的依赖名称的
            // 设置自动注入的bean名称（唯一依赖的bean名称）
            autowiredBeanName = determineAutowireCandidate(matchingBeans, descriptor);
            if (autowiredBeanName == null) {
                // autowiredBeanName == null说明没有找到唯一依赖的bean
                
                // isRequired(descriptor): 是否是必须注入的。@Autowired的required属性值，默认为true
                // indicatesMultipleBeans(type): 返回true说明是数组、Collection、Map其中一个类型。反之返回false
                // 如果是必须注入的或者注入的类型不是要求多个bean的，则抛出异常。               
                if (isRequired(descriptor) || !indicatesMultipleBeans(type)) {
                     // 抛出NoUniqueBeanDefinitionException异常
                    return descriptor.resolveNotUnique(descriptor.getResolvableType(), matchingBeans);
                }
                else {
                    // In case of an optional Collection/Map, silently ignore a non-unique case:
                    // possibly it was meant to be an empty collection of multiple regular beans
                    // (before 4.3 in particular when we didn't even look for collection beans).
                    return null;
                }
            }
            //给要注入的bean实例赋值（设置的值：唯一依赖的bean的Class或实例）
            instanceCandidate = matchingBeans.get(autowiredBeanName);
        }
        else {
            // 只找到一个匹配的值。
            // We have exactly one match.
            Map.Entry<String, Object> entry = matchingBeans.entrySet().iterator().next();
            // 给自动注入的beanName赋值
            autowiredBeanName = entry.getKey();
            // 给要注入的bean实例赋值（设置的值：唯一依赖的bean的Class或实例）
            instanceCandidate = entry.getValue();
        }
        
        // 记录匹配的beanName到autowiredBeanNames
        if (autowiredBeanNames != null) {
            autowiredBeanNames.add(autowiredBeanName);
        }
        // 有可能筛选出来的是bean的Class，调用beanFactory的getBean方法获取具体的实例。        
        if (instanceCandidate instanceof Class) {
            // 根据自动注入的名称和依赖的类型，获取bean实例
            instanceCandidate = descriptor.resolveCandidate(autowiredBeanName, type, this);
        }
        Object result = instanceCandidate;
        if (result instanceof NullBean) {
            // 获取到的结果是NullBean类型
            // 如果是必须注入的，则抛出异常
            if (isRequired(descriptor)) {
                // 抛出NoSuchBeanDefinitionException异常
                raiseNoMatchingBeanFound(type, descriptor.getResolvableType(), descriptor);
            }
            result = null;
        }
        // 注入的结果和类型不匹配抛出异常
        if (!ClassUtils.isAssignableValue(type, result)) {
            throw new BeanNotOfRequiredTypeException(autowiredBeanName, type, instanceCandidate.getClass());
        }
        // 返回要注入的bean实例
        return result;
    }
    finally {
        // 回退上一个注入点为当前注入点
        ConstructorResolver.setCurrentInjectionPoint(previousInjectionPoint);
    }
}
```

* 从shortcut(缓存)获取注入的值

```java
/**
 * 保存预先解析的目标bean名称的依赖描述器
 * DependencyDescriptor variant with a pre-resolved target bean name.
 */
@SuppressWarnings("serial")
private static class ShortcutDependencyDescriptor extends DependencyDescriptor {
	
    // bean名称
    private final String shortcut;

    // 需要的类型
    private final Class<?> requiredType;

    public ShortcutDependencyDescriptor(DependencyDescriptor original, String shortcut, Class<?> requiredType) {
        super(original);
        this.shortcut = shortcut;
        this.requiredType = requiredType;
    }

    @Override
    public Object resolveShortcut(BeanFactory beanFactory) {
        // 根据缓存的beanName和类型，从beanFactory获取具体类型的bean实例
        return beanFactory.getBean(this.shortcut, this.requiredType);
    }
}
```

##### 1、获取依赖的类型

```java
/**
 * Determine the declared (non-generic) type of the wrapped parameter/field.
 * @return the declared type (never {@code null})
 */
public Class<?> getDependencyType() {
    // 如果属性不为空
    if (this.field != null) {
        // 如果嵌套的层级大于1
        if (this.nestingLevel > 1) {
            // 类似这种，都是这种嵌套层级大于1的
            //@Autowired
            //private Optional<Dog> dogOptional;
            //@Autowired
            //private Optional<Collection<Dog>> optionalDogCollection;
            //@Autowired
            //private Optional<List<Dog>> optionalDogList;
            //@Autowired
            //private Optional<Map<String, DogService>> optionalDogServiceMap;
            //@Autowired
            //private Optional<DogService[]> optionalDogServiceArray;
            // DependencyObjectProvider
            //@Autowired
            //private ObjectFactory<Dog> dogFactory;
            // DependencyObjectProvider
            //@Autowired
            //private ObjectProvider<Dog> dogProvider;
            
            // getGenericType()方法返回该字段的泛型类型。
            // 对于普通类型的字段，它将返回 Class 对象；对于泛型类型的字段，它返回的是 ParameterizedType 或其他相关类型。
            Type type = this.field.getGenericType();
            for (int i = 2; i <= this.nestingLevel; i++) {
                if (type instanceof ParameterizedType) {
                    // 当字段是泛型类型时，获取实际泛型类型参数                    
                    Type[] args = ((ParameterizedType) type).getActualTypeArguments();
                    // 如 Optional<Dog> 获取到 Dog.class
                    // 如 Optional<List<Dog>> 获取到 java.util.List<..Dog> 属于ParameterizedType
                    type = args[args.length - 1];
                }
            }
            if (type instanceof Class) {
                // 如果是Class类型，直接返回
                return (Class<?>) type;
            }
            // 如 Optional<List<Dog>> 获取到 java.util.List<..Dog> 属于ParameterizedType
    	    // Optional<Map<String, DogService>> 获取到java.util.Map<java.lang.String, com.mrlu.service.DogService> 			属于 ParameterizedType
            else if (type instanceof ParameterizedType) {
                // getRawType() 返回一个 Class 对象，表示泛型类型的原始类型。
                // 如 java.util.List<..Dog> ParameterizedType 获取到的是java.util.List.class
                // 如 java.util.Map<java.lang.String,...DogService> 获取到  java.util.Map.class                     
                Type arg = ((ParameterizedType) type).getRawType();
                if (arg instanceof Class) {
                     // 如果是Class类型，直接返回
                    return (Class<?>) arg;
                }
            }
            
            // 以上都不是，则返回Object.class
            return Object.class;
        }
        else {
            // 嵌套的层级小于或等于1，例如
            // @Autowired
            // private Dog dog            
            
            // 返回属性的类型。
            return this.field.getType();
        }
    }
    else {
        // 返回方法/构造函数参数的嵌套类型。
        // 如 Dog dog 获取到的是 Dog.class。
        // Optional<Dog> 获取到 Dog.class 
        // Optional<List<Dog>> 获取到 java.util.List.class
        return obtainMethodParameter().getNestedParameterType();
    }
}

/**
 * 返回方法/构造函数参数的嵌套类型。
 * Return the nested type of the method/constructor parameter.
 * @return the parameter type (never {@code null})
 * @since 3.1
 * @see #getNestingLevel()
 */
public Class<?> getNestedParameterType() {
    if (this.nestingLevel > 1) {
        // 如果嵌套的层级大于1。如Optional<Dog> 、Optional<List<Dog>>等
        // 获取泛型参数
        Type type = getGenericParameterType();
        for (int i = 2; i <= this.nestingLevel; i++) {
            if (type instanceof ParameterizedType) {
                // 当字段是泛型类型时，获取实际泛型类型参数            
                Type[] args = ((ParameterizedType) type).getActualTypeArguments();
                // 确定嵌套层级里参数的位置。一般获取到null，
                Integer index = getTypeIndexForLevel(i);
                // 获取参数
                type = args[index != null ? index : args.length - 1];
            }
            // TODO: Object.class if unresolvable
        }
        if (type instanceof Class) {
            // 如果是Class类型，直接返回
            return (Class<?>) type;
        }
        // 如 Optional<List<Dog>> 获取到 java.util.List<..Dog> 属于ParameterizedType
        // Optional<Map<String, DogService>> 获取到java.util.Map<java.lang.String, com.mrlu.service.DogService> 			属于 ParameterizedType
        else if (type instanceof ParameterizedType) {
            // getRawType() 返回一个 Class 对象，表示泛型类型的原始类型。
            // 如 java.util.List<..Dog> ParameterizedType 获取到的是java.util.List.class
            // 如 java.util.Map<java.lang.String,...DogService> 获取到  java.util.Map.class   
            Type arg = ((ParameterizedType) type).getRawType();
            if (arg instanceof Class) {
			   // 如果是Class类型，直接返回
                return (Class<?>) arg;
            }
        }
        return Object.class;
    }
    else {
        // 嵌套的层级小于或等于1，无泛型嵌套
        // 获取方法或构造函数参数类型
        return getParameterType();
    }
}

@Nullable
public Integer getTypeIndexForLevel(int nestingLevel) {
    return getTypeIndexesPerLevel().get(nestingLevel);
}
/**
 * Obtain the (lazily constructed) type-indexes-per-level Map.
 */
private Map<Integer, Integer> getTypeIndexesPerLevel() {
    if (this.typeIndexesPerLevel == null) {
        this.typeIndexesPerLevel = new HashMap<>(4);
    }
    return this.typeIndexesPerLevel;
}
```

* 获取方法或构造函数参数类型

```java
/**
 * Return the type of the method/constructor parameter.
 * @return the parameter type (never {@code null})
 */
public Class<?> getParameterType() {
    // 一开始参数类型为空
    Class<?> paramType = this.parameterType;
    if (paramType != null) {
        // 参数类型不为空，直接返回
        return paramType;
    }
    // 如果方法参数声明的类和定义的类不一样，对于构造方法的参数来说是一样的。
    if (getContainingClass() != getDeclaringClass()) {
        paramType = ResolvableType.forMethodParameter(this, null, 1).resolve();
    }
    if (paramType == null) {
        // 计算参数类型
        paramType = computeParameterType();
    }
    // 设置参数类型
    this.parameterType = paramType;
    // 返回参数类型
    return paramType;
}

private Class<?> computeParameterType() {
    if (this.parameterIndex < 0) {
        Method method = getMethod();
        if (method == null) {
            return void.class;
        }
        if (KotlinDetector.isKotlinReflectPresent() && KotlinDetector.isKotlinType(getContainingClass())) {
            return KotlinDelegate.getReturnType(method);
        }
        return method.getReturnType();
    }
    // 根据参数类型的位置，获取方法参数。executable可以是Constructor或者Method
    return this.executable.getParameterTypes()[this.parameterIndex];
}

/**
 * Return the containing class for this method parameter.
 * @return a specific containing class (potentially a subclass of the
 * declaring class), or otherwise simply the declaring class itself
 * @see #getDeclaringClass()
 */
public Class<?> getContainingClass() {
    Class<?> containingClass = this.containingClass;
    return (containingClass != null ? containingClass : getDeclaringClass());
}
```



##### 2、解析多个bean

如果依赖的类型是数组、集合、Map、Stream这些，则解析封装好并返回

```java
@Nullable
private Object resolveMultipleBeans(DependencyDescriptor descriptor, @Nullable String beanName,
        @Nullable Set<String> autowiredBeanNames, @Nullable TypeConverter typeConverter) {
	// 获取当前要注入的类型
    Class<?> type = descriptor.getDependencyType();
    
    // 属于Stream相关的
    if (descriptor instanceof StreamDependencyDescriptor) {
        // 找到(所有)匹配类型的bean
        Map<String, Object> matchingBeans = findAutowireCandidates(beanName, type, descriptor);
        if (autowiredBeanNames != null) {
            autowiredBeanNames.addAll(matchingBeans.keySet());
        }
        // 构建Stream
        Stream<Object> stream = matchingBeans.keySet().stream()
                .map(name -> descriptor.resolveCandidate(name, type, this))
                .filter(bean -> !(bean instanceof NullBean));
        // 排序
        if (((StreamDependencyDescriptor) descriptor).isOrdered()) {
            stream = stream.sorted(adaptOrderComparator(matchingBeans));
        }
        return stream;
    }
    // 如果类型是数组
    else if (type.isArray()) {
        // 得到数组元素的类型
        Class<?> componentType = type.getComponentType();
        // 获取依赖描述器的类型
        ResolvableType resolvableType = descriptor.getResolvableType();
        Class<?> resolvedArrayType = resolvableType.resolve(type);
        if (resolvedArrayType != type) {
            // 类型不相等，用resolvableType解析的数组元素类型
            componentType = resolvableType.getComponentType().resolve();
        }
        if (componentType == null) {
            return null;
        }
        
         // 根据数组元素类型，找到(所有)匹配类型的bean
        Map<String, Object> matchingBeans = findAutowireCandidates(beanName, componentType,
                new MultiElementDescriptor(descriptor));
        if (matchingBeans.isEmpty()) {
            // 获取的结果为空，返回null
            return null;
        }
        
        // 记录匹配的beanName到autowiredBeanNames
        if (autowiredBeanNames != null) {
            autowiredBeanNames.addAll(matchingBeans.keySet());
        }
        // 获取类型转换器
        TypeConverter converter = (typeConverter != null ? typeConverter : getTypeConverter());
        // Map的values转成数组
        Object result = converter.convertIfNecessary(matchingBeans.values(), resolvedArrayType);
        if (result instanceof Object[]) {
            // 根据@Order注解指定的顺序或者实现Order接口指定的顺序进行排序
            Comparator<Object> comparator = adaptDependencyComparator(matchingBeans);
            if (comparator != null) {
                // 排序
                Arrays.sort((Object[]) result, comparator);
            }
        }
        return result;
    }
    // 如果类型是Collection及其子接口
    else if (Collection.class.isAssignableFrom(type) && type.isInterface()) {
        // 获取集合元素的类型
        Class<?> elementType = descriptor.getResolvableType().asCollection().resolveGeneric();
        if (elementType == null) {
            return null;
        }
        // 找到(所有)匹配类型的bean
        Map<String, Object> matchingBeans = findAutowireCandidates(beanName, elementType,
                new MultiElementDescriptor(descriptor));
        if (matchingBeans.isEmpty()) {
             // 获取的结果为空，返回null
            return null;
        }
        // 记录匹配的beanName到autowiredBeanNames
        if (autowiredBeanNames != null) {
            autowiredBeanNames.addAll(matchingBeans.keySet());
        }
  		// 获取类型转换器
        TypeConverter converter = (typeConverter != null ? typeConverter : getTypeConverter());
        // Map的values转成集合
        Object result = converter.convertIfNecessary(matchingBeans.values(), type);
        if (result instanceof List) {
            // 如果集合是List类型
            if (((List<?>) result).size() > 1) {
                // 根据@Order注解指定的顺序或者实现Order接口指定的顺序进行排序
                Comparator<Object> comparator = adaptDependencyComparator(matchingBeans);
                if (comparator != null) {
   				   // 排序
                    ((List<?>) result).sort(comparator);
                }
            }
        }
        // 返回结果
        return result;
    }
    // 如果是Map类型
    else if (Map.class == type) {
        ResolvableType mapType = descriptor.getResolvableType().asMap();
        // 获取Map的key的类型
        Class<?> keyType = mapType.resolveGeneric(0);
        if (String.class != keyType) {
            // 如果key的类型不是字符串，直接返回null
            return null;
        }
        // 获取Map的value的类型
        Class<?> valueType = mapType.resolveGeneric(1);
        if (valueType == null) {
            return null;
        }
        
        // 根据value的类型，找到(所有)匹配类型的bean
        Map<String, Object> matchingBeans = findAutowireCandidates(beanName, valueType,
                new MultiElementDescriptor(descriptor));
        if (matchingBeans.isEmpty()) {
            // 获取的结果为空，返回null
            return null;
        }
        if (autowiredBeanNames != null) {
            // 记录匹配的beanName到autowiredBeanNames
            autowiredBeanNames.addAll(matchingBeans.keySet());
        }
        // 返回找到的所有bean
        return matchingBeans;
    }
    else {
        // 其他类型，则返回null
        return null;
    }
}
```

【注意】**注入List、数组等类型，List或者数组里保存的实例可以根据@Order注解指定的顺序或者实现Order接口指定的顺序进行排序**

##### 3、找到(所有)匹配类型的bean实例

【注意】返回结果Map的value可能是bean实例，也可以是bean实例对应的Class对象

```java
protected Map<String, Object> findAutowireCandidates(
        @Nullable String beanName, Class<?> requiredType, DependencyDescriptor descriptor) {
	// 3.1 获取所有候选的beanName       
    // lbf - 当前beanFactory
    // type – 需要注入的bean类型
    // includeNonSingletons – whether to include prototype or scoped beans too or just singletons
    //     	(also applies to FactoryBeans) 是否考虑非单实例bean
    // allowEagerInit – 是否初始化由FactoryBeans（或由带有“factory-bean”引用的工厂方法）创建的惰性初始化单例和对象以进行类型检查。请注意，需要对FactoryBeans进行初始化以确定它们的类型：因此请注意，为该标志传入“true”将初始化FactoryBeans和“factory-bean”引用。创建DependencyDescriptor默认设置eager属性为true
    // 根据类型获取所有候选的beanName
    String[] candidateNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
            this, requiredType, true, descriptor.isEager());
    Map<String, Object> result = CollectionUtils.newLinkedHashMap(candidateNames.length);    
    
    // 遍历可解析的依赖，如果“可解析的依赖的类型”是“需要注入的类型及其子类”，则获取可解析的值。再结合需要注入的类型，获取匹配的实例。   
    for (Map.Entry<Class<?>, Object> classObjectEntry : this.resolvableDependencies.entrySet()) {
        Class<?> autowiringType = classObjectEntry.getKey();
        // 如果“可解析的依赖的类型”是“需要注入的类型及其子类”
        if (autowiringType.isAssignableFrom(requiredType)) {
            Object autowiringValue = classObjectEntry.getValue();            
            // 解析自动装配的值            
            autowiringValue = AutowireUtils.resolveAutowiringValue(autowiringValue, requiredType);
            // 如果自动装配的值是requiredType类型或子类型的实例，则保存结果
            if (requiredType.isInstance(autowiringValue)) {
                result.put(ObjectUtils.identityToString(autowiringValue), autowiringValue);
                break;
            }
        }
    }
    // 3.2 遍历所有的候选beanName，保存符合条件的结果
    for (String candidate : candidateNames) {
        // 满足以下两个条件       
        //（1）是否是依赖注入自身（即自己依赖自己）
        // (2) 是否应该将bean视为自动候选。isAutowireCandidate方法主要是检查@Qualifier注解是否匹配
        // 则保存结果
        if (!isSelfReference(beanName, candidate) && isAutowireCandidate(candidate, descriptor)) {
            // 3.3 保存候选的结果
            addCandidateEntry(result, candidate, descriptor, requiredType);
        }
    }
    
    // 查询结果为空
    if (result.isEmpty()) {        
        // 是否依赖多个bean（即注入的是数组，集合，Map类型）
        boolean multiple = indicatesMultipleBeans(requiredType);
        
        // Consider fallback matches if the first pass failed to find anything...        
        // 回退获取最初的依赖描述器，再次判断
        DependencyDescriptor fallbackDescriptor = descriptor.forFallbackMatch();
        for (String candidate : candidateNames) {
            //（1）不是依赖注入自身（即自己依赖自己）
            // (2) 是自动候选的bean
            //（3）不是依赖多个bean 或 有@Qualifier注解
            // 则保存结果
            if (!isSelfReference(beanName, candidate) && isAutowireCandidate(candidate, fallbackDescriptor) &&
                    (!multiple || getAutowireCandidateResolver().hasQualifier(descriptor))) {
                // 3.3 保存候选的结果
                addCandidateEntry(result, candidate, descriptor, requiredType);
            }
        }
        if (result.isEmpty() && !multiple) {            
            // 最后考虑自身引用。（即处理自己依赖注入自己的情况）
            // Consider self references as a final pass...
            // but in the case of a dependency collection, not the very same bean itself.
            // 但是在依赖集合的情况下，不是同一个bean本身。            
            
            /**
            @Service
            public class AppleServiceImpl implements AppleService {
                // 自己依赖自己的情况
                @Autowired
                private AppleService appleService;

                // 这种情况不是自己注入自己，会注入失败
                // @Autowired
                // private Collection<AppleService> appleService;
                // 传递的依赖描述器是MultiElementDescriptor。会走到下面判断的  !beanName.equals(candidate) ，得到的结果				 是false，最后不进入if保存结果，没有找到候选对象。

                // 这种情况不是自己注入自己，会注入失败
                // @Autowired
                // private Collection<AppleService> appleServiceCollection;                
                // 传递的依赖描述器是MultiElementDescriptor。会走到下面判断的  !beanName.equals(candidate) ，得到的结果				 是false，最后不进入if保存结果，没有找到候选对象。
            }
            */
            
            //【重点注意】依赖注入自身，只能注入单个对象
            for (String candidate : candidateNames) {
                if (isSelfReference(beanName, candidate) &&
                        (!(descriptor instanceof MultiElementDescriptor) || !beanName.equals(candidate)) &&
                        isAutowireCandidate(candidate, fallbackDescriptor)) {
                    // 3.3 保存候选的结果
                    addCandidateEntry(result, candidate, descriptor, requiredType);
                }
            }
        }
    }
    // 返回结果
    return result;
}

/**
 * 是否是依赖注入自身
 * Determine whether the given beanName/candidateName pair indicates a self reference,
 * i.e. whether the candidate points back to the original bean or to a factory method
 * on the original bean.
 */
private boolean isSelfReference(@Nullable String beanName, @Nullable String candidateName) {
    // 满足以下两种情况之一，说明是依赖注入自身
    // （1）beanName不等于null，候选的name不等于null，而且beanName等于候选的name，说明是自身引用
    // （2）beanName不等于null，候选的name不等于null，beanName不等于候选的name，但是beanName等于candidateName的工厂beanName，说明是自身引用。
    return (beanName != null && candidateName != null &&
            (beanName.equals(candidateName) || (containsBeanDefinition(candidateName) &&
                    beanName.equals(getMergedLocalBeanDefinition(candidateName).getFactoryBeanName()))));
}

/**
* 是否依赖多个bean（即注入的是数组，集合，Map类型）
*/
private boolean indicatesMultipleBeans(Class<?> type) {
    return (type.isArray() || (type.isInterface() &&
            (Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type))));
}
```

 【重点注意】**依赖注入自身，只能注入单个对象**



*  解析自动装配的值

```java
//（1）如果可解析的值不是ObjectFactory类型， 返回可解析的值           
//（2）如果可解析的值是ObjectFactory类型， 而且可解析的值是“需要注入类型”的实例，返回可解析的值
//（3）如果可解析的值是ObjectFactory类型， 而且可解析的值不是“需要注入类型”的实例，则进行以下处理：
//（3-1）如果可解析的值是Serializable类型，需要注入的类型是接口，则根据“需要注入的类型”创建代理对象返回（实际上代理对象还是调用ObjectFactory的getObject()方法，反之，调用ObjectFactory的getObject()方法
public static Object resolveAutowiringValue(Object autowiringValue, Class<?> requiredType) {
    if (autowiringValue instanceof ObjectFactory && !requiredType.isInstance(autowiringValue)) {
        ObjectFactory<?> factory = (ObjectFactory<?>) autowiringValue;
        if (autowiringValue instanceof Serializable && requiredType.isInterface()) {
            autowiringValue = Proxy.newProxyInstance(requiredType.getClassLoader(),
                    new Class<?>[] {requiredType}, new ObjectFactoryDelegatingInvocationHandler(factory));
        }
        else {
            return factory.getObject();
        }
    }
    return autowiringValue;
}
```



###### 3.1 获取所有候选的beanName

获取给定类型的所有bean名称，包括在父工厂中定义的名称

```java
/**
获取给定类型的所有bean名称，包括在父工厂中定义的名称。将在覆盖bean定义的情况下返回唯一名称。
如果设置了“allowEagerInit”标志，则考虑由FactoryBeans创建的对象，这意味着将初始化FactoryBeans。如果FactoryBean创建的对象不匹配，原始FactoryBean本身将根据类型进行匹配。如果没有设置“allowEagerInit”，则只检查原始的FactoryBean（不需要初始化每个FactoryBean）。

Get all bean names for the given type, including those defined in ancestor factories. Will return unique names in case of overridden bean definitions.
Does consider objects created by FactoryBeans if the "allowEagerInit" flag is set, which means that FactoryBeans will get initialized. If the object created by the FactoryBean doesn't match, the raw FactoryBean itself will be matched against the type. If "allowEagerInit" is not set, only raw FactoryBeans will be checked (which doesn't require initialization of each FactoryBean).
*/

// lbf - 当前beanFactory
// type - 需要注入的bean类型
// includeNonSingletons – whether to include prototype or scoped beans too or just singletons
//     	(also applies to FactoryBeans) 是否考虑非单实例bean
// allowEagerInit – 是否初始化由FactoryBeans（或由带有“factory-bean”引用的工厂方法）创建的惰性初始化单例和对象以进行类型检查。请注意，需要对FactoryBeans进行初始化以确定它们的类型：因此请注意，为该标志传入“true”将初始化FactoryBeans和“factory-bean”引用。创建DependencyDescriptor默认设置eager属性为true
public static String[] beanNamesForTypeIncludingAncestors(
        ListableBeanFactory lbf, Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {

    Assert.notNull(lbf, "ListableBeanFactory must not be null");
    // 根据类型获取所有beanName。【】
    // 如果执行到了FactoryBean，先调用FactoryBean的getObjectType方法，获取实际创建的bean类型，判断实际创建的类型是否匹配给定类型。如果匹配，则返回工厂bean的名称。不匹配的话，则判断FactoryBean自身是否匹配，匹配则返回。
    String[] result = lbf.getBeanNamesForType(type, includeNonSingletons, allowEagerInit);
    if (lbf instanceof HierarchicalBeanFactory) {
        HierarchicalBeanFactory hbf = (HierarchicalBeanFactory) lbf;
        if (hbf.getParentBeanFactory() instanceof ListableBeanFactory) {
            // 递归调用父工厂
            String[] parentResult = beanNamesForTypeIncludingAncestors(
                    (ListableBeanFactory) hbf.getParentBeanFactory(), type, includeNonSingletons, allowEagerInit);
            result = mergeNamesWithParent(result, parentResult, hbf);
        }
    }
    return result;
}
```

**工厂bean重要知识点**

（1）一个工厂bean，只对应一个bean定义。但是依赖注入的时候，可以注入工厂bean自身或者是工厂bean的getObject方法返回的实例

（2）默认情况下，“工厂bean的getObject方法返回的实例” 的bean名称，为工厂bean的“首字母小写的类名”。而工厂bean自身的bean名称为 & 拼接 “首字母小写的类名”。

（3）工厂bean的getObject方法获取的实例，也会应用bean的前置处理器和后置处理器



###### 3.2 是否应该将bean视为自动候选

```java
/**
确定指定的bean是否有资格作为自动候选对象，以注入“声明了匹配类型的依赖”的其他bean中。这个方法同样检查父工厂方法。
Determine whether the specified bean qualifies as an autowire candidate, to be injected into other beans which declare a dependency of matching type.
This method checks ancestor factories as well.
Params:
beanName – the name of the bean to check 检查的bean名称
descriptor – the descriptor of the dependency to resolve 要解析的依赖描述器
Returns: whether the bean should be considered as autowire candidate 是否应该将bean视为自动候选
Throws: NoSuchBeanDefinitionException – if there is no bean with the given name 
*/
@Override
public boolean isAutowireCandidate(String beanName, DependencyDescriptor descriptor)
        throws NoSuchBeanDefinitionException {	
    // getAutowireCandidateResolver方法获取到QualifierAnnotationAutowireCandidateResolver
    return isAutowireCandidate(beanName, descriptor, getAutowireCandidateResolver());
}
	
protected boolean isAutowireCandidate(
        String beanName, DependencyDescriptor descriptor, AutowireCandidateResolver resolver)
        throws NoSuchBeanDefinitionException {
    // 简化bean名称，去除FactoryBean的前缀&（如果有的话）	
    String bdName = BeanFactoryUtils.transformedBeanName(beanName);
    // 如果包含该bdName的bean定义
    if (containsBeanDefinition(bdName)) {
        //getMergedLocalBeanDefinition(bdName): 获取合并的bean定义。如果给定bean的定义是子bean定义，则通过与父bean合并，返回给定顶级bean的RootBeanDefinition。        
    	// 是否应该将bean视为自动候选。 【一般会执行到这里】       
        return isAutowireCandidate(beanName, getMergedLocalBeanDefinition(bdName), descriptor, resolver);
    }
    // 如果包含beanName对应的实例
    else if (containsSingleton(beanName)) {
        // 是否应该将bean视为自动候选
        return isAutowireCandidate(beanName, new RootBeanDefinition(getType(beanName)), descriptor, resolver);
    }
    
    // 当前beanFactory既不包含bean定义，也不包含bean实例。获取父的beanFactory进行判断。
    BeanFactory parent = getParentBeanFactory();
    // 如果父的beanFactory是DefaultListableBeanFactory类型
    if (parent instanceof DefaultListableBeanFactory) {        
        // No bean definition found in this factory -> delegate to parent.
        return ((DefaultListableBeanFactory) parent).isAutowireCandidate(beanName, descriptor, resolver);
    }
    // 如果父的beanFactory是ConfigurableListableBeanFactory类型
    else if (parent instanceof ConfigurableListableBeanFactory) {
        // If no DefaultListableBeanFactory, can't pass the resolver along.
        return ((ConfigurableListableBeanFactory) parent).isAutowireCandidate(beanName, descriptor);
    }
    else {
        return true;
    }
}

/**
 * 去除FactoryBean的前缀&（如果有的话），返回实际的beanName 
 * Return the actual bean name, stripping out the factory dereference
 * prefix (if any, also stripping repeated factory prefixes if found).
 * @param name the name of the bean
 * @return the transformed name
 * @see BeanFactory#FACTORY_BEAN_PREFIX
 */
public static String transformedBeanName(String name) {
    Assert.notNull(name, "'name' must not be null");
    if (!name.startsWith(BeanFactory.FACTORY_BEAN_PREFIX)) {
        return name;
    }
    return transformedBeanNameCache.computeIfAbsent(name, beanName -> {
        do {
            beanName = beanName.substring(BeanFactory.FACTORY_BEAN_PREFIX.length());
        }
        while (beanName.startsWith(BeanFactory.FACTORY_BEAN_PREFIX));
        return beanName;
    });
}
String FACTORY_BEAN_PREFIX = "&";

protected boolean isAutowireCandidate(String beanName, RootBeanDefinition mbd,
        DependencyDescriptor descriptor, AutowireCandidateResolver resolver) {
    // 简化bean名称，去除FactoryBean的前缀&（如果有的话）	
    String bdName = BeanFactoryUtils.transformedBeanName(beanName);
    // 解析beanClass类型
    resolveBeanClass(mbd, bdName);
    // bean定义有唯一的工厂方法（@Bean方法），但是bean定义设置factoryMethodToIntrospect
     // 一般如果有@Bean方法，这个属性都会被解析设置，不会进入if
    if (mbd.isFactoryMethodUnique && mbd.factoryMethodToIntrospect == null) {               
        // 需要解析factoryMethodToIntrospect属性。
        new ConstructorResolver(this).resolveFactoryMethodIfPossible(mbd);
    }
    // 创建bean定义持有对象
    BeanDefinitionHolder holder = (beanName.equals(bdName) ?
            this.mergedBeanDefinitionHolders.computeIfAbsent(beanName,
                    key -> new BeanDefinitionHolder(mbd, beanName, getAliases(bdName))) :
            //beanName不等于bdName，通常工厂bean                       
            new BeanDefinitionHolder(mbd, beanName, getAliases(bdName)));
    // 解析器解析是否是自动候选
    return resolver.isAutowireCandidate(holder, descriptor);
}
```



QualifierAnnotationAutowireCandidateResolver

```java
/**
 * 确定所提供的bean定义是否是自动候选的
 * Determine whether the provided bean definition is an autowire candidate.
 * 
 * （1）要被认为是自动候选的，bean定义的autowireCandidate属性不能设置为false，默认为true。
 * （2）如果要自动注入的属性或(方法)参数上被beanFactory识别有@Qualifier注解，bean必须匹配该注解及其所有可能的属性。
 *      bean定义必须包含相同的@Qualifier或者匹配所有可能的属性。
 * （3）如果@Qualifier或者属性不匹配，value属性将回退到匹配bean名称或别名。
 *
 * <p>To be considered a candidate the bean's <em>autowire-candidate</em>
 * attribute must not have been set to 'false'. Also, if an annotation on
 * the field or parameter to be autowired is recognized by this bean factory
 * as a <em>qualifier</em>, the bean must 'match' against the annotation as
 * well as any attributes it may contain. The bean definition must contain
 * the same qualifier or match by meta attributes. A "value" attribute will
 * fallback to match against the bean name or an alias if a qualifier or
 * attribute does not match.
 * @see Qualifier
 */
@Override
public boolean isAutowireCandidate(BeanDefinitionHolder bdHolder, DependencyDescriptor descriptor) {
    // 先调用super.isAutowireCandidate
    // (1) 检查bean定义的autowireCandidate属性是否设置为false。如果设置为false，则不匹配。
    // (2) 获取依赖描述器的泛型类型。如果有的话，检查是否匹配bean定义。没有的话，说明不是泛型类型，则返回true
    boolean match = super.isAutowireCandidate(bdHolder, descriptor);
    if (match) {
        // 根据候选的bean定义匹配给定的@Qualifier注解。
        // 如果无@Qualifier注解，返回true
        match = checkQualifiers(bdHolder, descriptor.getAnnotations());
        if (match) {
            // 如果匹配，获取依赖描述器的方法参数
            MethodParameter methodParam = descriptor.getMethodParameter();
            // 方法参数不为空
            if (methodParam != null) {
                // 获取方法
                Method method = methodParam.getMethod();
                // 方法为空或者方法返回值类型为void.class                
                if (method == null || void.class == method.getReturnType()) {
                    // 获取方法上的注解(包含@Qualifier)，检查是否匹配bean定义。如果无@Qualifier注解，返回true
                    match = checkQualifiers(bdHolder, methodParam.getMethodAnnotations());
                }
            }
        }
    }
    // 返回匹配的结果。true表示匹配，反之不匹配。
    return match;
}
```



###### 3.2.1  结论

```java
/**
 *
 * @Autowired  + @Qualifier 注入依赖源码概述
 * 一、根据方法进行Qualifier
 * 1、根据方法参数类型获取相应的bean
 * 2、获取方法参数的@Qualifier注解，作为期望注解，判断bean是否符合条件
 *      执行"判断bean是否属于候选的bean"逻辑，如果符合，则执行步骤3，不符合，则直接结束
 *    如果方法参数上不存在@Qualifier注解，则步骤2返回"符合条件"，则执行步骤3
 * 3、如果方法的返回值类型属于void类型，获取方法上的@Qualifier注解，作为期望注解，判断bean是否符合条件
 *        执行"判断bean是否属于候选的bean"，如果符合，说明是符合条件的bean
 *    如果方法的返回值类型不属于void类型，则采用步骤2的判断结果
 *
 * 总结：
 *    对于void返回值类型的方法，
 *    如果在方法或者方法参数上同时使用了 @Qualifier注解，
 *    会先判断方法参数的 @Qualifier注解，再判断方法上的@Qualifier注解，
 *    两者都符合条件才算符合条件
 *    对于不是void返回值类型的方法，判断方法参数的 @Qualifier注解，符合条件即可
 *
 *
 * 二、属性直接进行Qualifier
 *    获取属性上的@Qualifier注解，作为期望注解，执行“判断bean是否属于候选的bean”
 *    如果属于，则进行注入
 *
 *
 * 【判断bean是否属于候选的bean】
 *  分为以下两大情况
 *  一、bean定义没有(程序员手动指定) AutowireCandidateQualifier
 *   (1)如果bean定义中有@Qualifier注解，则直接获取。判断是否和期望注解相同，相同则说明符合条件
 *      如果没有，则执行(2)
 *   (2)如果该bean是由@Bean方法创建的，获取@Bean方法上的@Qualifier注解。判断是否和期望注解相同，相同则说明符合条件
 *      如果不是，则执行(3)
 *   (3)获取bean的Class对象, 获取Class对象的@Qualifier注解。判断是否和期望注解相同，相同则说明符合条件
 *      如果没有@Qualifier注解，则执行(4)
 *  （4）获取bean的名称, 获取期望注解的value属性。判断bean名称是否和value相同，相同则说明符合条件
 *      如果不相同，则说明不符合条件
 *   判断是否和期望注解相同：实际上重写了注解的equals方法，会判断注解的属性值是否相等。
 *
 *  二、bean定义有(程序员手动指定) AutowireCandidateQualifier
 *  如果注入的属性使用@Qualifier注解，没有指定value属性，则使用默认的空串作为期望值。
 * （1）候选的bean没有使用@Qualifier注解，则判断beanName是否等于期望值。
 * （2）候选的bean使用@Qualifier注解，但是没有指定value属性。都符合条件，都是候选bean
 * （3）候选的bean使用@Qualifier注解，指定value属性。判断指定的value值是否等于期望值
 *
 * 如果注入的属性使用@Qualifier注解， 指定value属性，则使用指定的值作为期望值。
 * （1）候选的bean没有使用@Qualifier注解，则判断beanName是否等于期望值。
 * （2）候选的bean使用@Qualifier注解，但是没有指定value属性。则判断beanName是否等于期望值。
 * （3）候选的bean使用@Qualifier注解，指定value属性。判断指定的value值是否等于期望值
 *
 * 见 AutowiredAnnotationBeanPostProcessor、QualifierAnnotationAutowireCandidateResolver类
 *
 */
```



* super.isAutowireCandidate方法

```java
@Override
public boolean isAutowireCandidate(BeanDefinitionHolder bdHolder, DependencyDescriptor descriptor) {
    // super.isAutowireCandidate(bdHolder, descriptor) 默认为true。即默认为候选
    if (!super.isAutowireCandidate(bdHolder, descriptor)) {
        // If explicitly false, do not proceed with any other checks...
        return false;
    }
    // 检查泛型类型是否匹配，一般无泛型类型，返回true。
    return checkGenericTypeMatch(bdHolder, descriptor);
}
@Override
public boolean isAutowireCandidate(BeanDefinitionHolder bdHolder, DependencyDescriptor descriptor) {
    // bean定义的autowireCandidate属性，默认为true
    return bdHolder.getBeanDefinition().isAutowireCandidate();
}
private boolean autowireCandidate = true;

/**
 * 
 * Match the given dependency type with its generic type information against the given
 * candidate bean definition.
 */
protected boolean checkGenericTypeMatch(BeanDefinitionHolder bdHolder, DependencyDescriptor descriptor) {
    ResolvableType dependencyType = descriptor.getResolvableType();
    if (dependencyType.getType() instanceof Class) {
        // 无泛型类型，直接返回true。根据前面的分析，一般会执行到这里。
        // No generic type -> we know it's a Class type-match, so no need to check again.
        return true;
    }

    ResolvableType targetType = null;
    boolean cacheType = false;
    RootBeanDefinition rbd = null;
    if (bdHolder.getBeanDefinition() instanceof RootBeanDefinition) {
        rbd = (RootBeanDefinition) bdHolder.getBeanDefinition();
    }
    if (rbd != null) {
        targetType = rbd.targetType;
        if (targetType == null) {
            cacheType = true;
            // First, check factory method return type, if applicable
            targetType = getReturnTypeForFactoryMethod(rbd, descriptor);
            if (targetType == null) {
                RootBeanDefinition dbd = getResolvedDecoratedDefinition(rbd);
                if (dbd != null) {
                    targetType = dbd.targetType;
                    if (targetType == null) {
                        targetType = getReturnTypeForFactoryMethod(dbd, descriptor);
                    }
                }
            }
        }
    }

    if (targetType == null) {
        // Regular case: straight bean instance, with BeanFactory available.
        if (this.beanFactory != null) {
            Class<?> beanType = this.beanFactory.getType(bdHolder.getBeanName());
            if (beanType != null) {
                targetType = ResolvableType.forClass(ClassUtils.getUserClass(beanType));
            }
        }
        // Fallback: no BeanFactory set, or no type resolvable through it
        // -> best-effort match against the target class if applicable.
        if (targetType == null && rbd != null && rbd.hasBeanClass() && rbd.getFactoryMethodName() == null) {
            Class<?> beanClass = rbd.getBeanClass();
            if (!FactoryBean.class.isAssignableFrom(beanClass)) {
                targetType = ResolvableType.forClass(ClassUtils.getUserClass(beanClass));
            }
        }
    }

    if (targetType == null) {
        return true;
    }
    if (cacheType) {
        rbd.targetType = targetType;
    }
    if (descriptor.fallbackMatchAllowed() &&
            (targetType.hasUnresolvableGenerics() || targetType.resolve() == Properties.class)) {
        // Fallback matches allow unresolvable generics, e.g. plain HashMap to Map<String,String>;
        // and pragmatically also java.util.Properties to any Map (since despite formally being a
        // Map<Object,Object>, java.util.Properties is usually perceived as a Map<String,String>).
        return true;
    }
    // Full check for complex generic type match...
    return dependencyType.isAssignableFrom(targetType);
}
```



* 根据候选的bean定义匹配给定的@Qualifier注解

```java
/**
* bdHolder: bean定义持有对象
* annotationsToSearch: 需要注入的属性或方法参数或方法上的注解
*/
protected boolean checkQualifiers(BeanDefinitionHolder bdHolder, Annotation[] annotationsToSearch) {
    // 注解为空，返回true，表示匹配。
    if (ObjectUtils.isEmpty(annotationsToSearch)) {        
        return true;
    }
    // 创建简单的类型转换器
    SimpleTypeConverter typeConverter = new SimpleTypeConverter();
    // 遍历所有的注解，如果是@Qualifier注解，检查bean定义是否匹配
    for (Annotation annotation : annotationsToSearch) {
        // 获取注解的类型        
        Class<? extends Annotation> type = annotation.annotationType();
        // 默认需要检查元注解
        boolean checkMeta = true;
        // 默认不需要回退到元注解中
        boolean fallbackToMeta = false;
        // 是否是@Qualifier注解
        if (isQualifier(type)) {
            // 检查bean定义是否匹配@Qualifier注解
            if (!checkQualifier(bdHolder, annotation, typeConverter)) {
                // 不匹配，设置fallbackToMeta=true，回退到获取注解上的注解继续检查（即从元注解查找）。如                
                /**
                @Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE, 					   ElementType.ANNOTATION_TYPE})
                @Retention(RetentionPolicy.RUNTIME)
                @Inherited
                @Documented
                public @interface Qualifier {
                	String value() default "";
                }
                可知@Qualifier注解上的注解是@Target、@Retention、@Inherited、@Documented
                */ 
                fallbackToMeta = true;
            }
            else {
                // 如果匹配，设置不需要检查元注解
                checkMeta = false;
            }
        }
        // 需要检查元注解
        if (checkMeta) {            
            // 默认没有从元注解中找到@Qualifier注解
            boolean foundMeta = false;
            // 获取所有的元注解
            for (Annotation metaAnn : type.getAnnotations()) {
                // 获取元注解的类型
                Class<? extends Annotation> metaType = metaAnn.annotationType();
                // 是否是@Qualifier注解
                if (isQualifier(metaType)) {
                    // 找到@Qualifier注解
                    foundMeta = true;
                    
                    //只接受回退匹配，如果@Qualifier注解有一个值…
				   //否则，它只是一个自定义@Qualifier注解的标记。
                    // 说白了，对于元注解，只检查@Qualifier的value不为空的                   
                    // Only accept fallback match if @Qualifier annotation has a value...
                    // Otherwise it is just a marker for a custom qualifier annotation.
                    
                    //（1）如果回退到元注解中查找，找到的@Qualifier的value为空，则返回不匹配
                    //（2）@Qualifier的value不为空，检查bean定义是否匹配。检查不通过，返回不匹配
                    if ((fallbackToMeta && ObjectUtils.isEmpty(AnnotationUtils.getValue(metaAnn))) ||               
                            !checkQualifier(bdHolder, metaAnn, typeConverter)) {                        
                        return false;
                    }
                }
            }
            // 回退给定注解的元注解查找，没有找到匹配的元注解，返回不匹配
            if (fallbackToMeta && !foundMeta) {
                return false;
            }
        }
    }
    // 返回true，表示匹配。
    return true;
}

private final Set<Class<? extends Annotation>> qualifierTypes = new LinkedHashSet<>(2);
public QualifierAnnotationAutowireCandidateResolver() {
    // 添加@Qualifier注解的Class
    this.qualifierTypes.add(Qualifier.class);
    try {
        this.qualifierTypes.add((Class<? extends Annotation>) ClassUtils.forName("javax.inject.Qualifier",
                        QualifierAnnotationAutowireCandidateResolver.class.getClassLoader()));
    }
    catch (ClassNotFoundException ex) {
        // JSR-330 API not available - simply skip.
    }
}
/**
 * 是否是@Qualifier注解
 * Checks whether the given annotation type is a recognized qualifier type.
 */
protected boolean isQualifier(Class<? extends Annotation> annotationType) {
    for (Class<? extends Annotation> qualifierType : this.qualifierTypes) {
        if (annotationType.equals(qualifierType) || annotationType.isAnnotationPresent(qualifierType)) {
            return true;
        }
    }
    return false;
}
```



* 检查bean定义是否匹配@Qualifier注解

```java
/**
 * Match the given qualifier annotation against the candidate bean definition.
 * bdHolder: bean定义持有对象
 * annotation: 需要注入的属性或方法参数或方法上的@Qualifier注解，即期望注解
 * typeConverter：类型转换器
 */
protected boolean checkQualifier(
        BeanDefinitionHolder bdHolder, Annotation annotation, TypeConverter typeConverter) {
    // 获取注解的类型。Qualifier.class
    Class<? extends Annotation> type = annotation.annotationType();
    // 从bean定义持有对象中获取bean定义
    RootBeanDefinition bd = (RootBeanDefinition) bdHolder.getBeanDefinition();

    // 从bean定义中获取注解类型对应的AutowireCandidateQualifier对象。
    // 一般情况是获取到null。什么情况会有呢？？？
    // 程序员手动注册bean定义时，通过addQualifier方法指定了AutowireCandidateQualifier。
    AutowireCandidateQualifier qualifier = bd.getQualifier(type.getName());
    if (qualifier == null) {
        qualifier = bd.getQualifier(ClassUtils.getShortName(type));
    }
    
    // 如果qualifier等于null（如果bean定义没有手动指定的AutowireCandidateQualifier）
    if (qualifier == null) {
        // 校验注解是否相等
        
        // 首先从bean定义中获取@Qualifier注解，如果有的话
        // First, check annotation on qualified element, if any
        Annotation targetAnnotation = getQualifiedElementAnnotation(bd, type);
        // 然后，检查工厂方法上的@Qualifier注解，如果合适的话
        // Then, check annotation on factory method, if applicable
        if (targetAnnotation == null) {            
            /**
            *  从工厂方法中获取@Qualifier注解。什么是工厂方法？？？
            *  通过@Bean注解标注的创建bean的方法
            */            
            targetAnnotation = getFactoryMethodAnnotation(bd, type);
        }
        if (targetAnnotation == null) {
            // 获取装饰后的bean定义。一般为null
            RootBeanDefinition dbd = getResolvedDecoratedDefinition(bd);
            if (dbd != null) {
                // 从装饰后的bean定义的工厂方法获取
                targetAnnotation = getFactoryMethodAnnotation(dbd, type);
            }
        }
        
        if (targetAnnotation == null) {            
            // 如果以上位置都没有找到目标注解
            
            // 从目标类寻找正在匹配的注解
            // Look for matching annotation on the target class
            if (getBeanFactory() != null) {
                try {
                    // 获取bean的Class对象
                    Class<?> beanType = getBeanFactory().getType(bdHolder.getBeanName());
                    if (beanType != null) {
                        // 获取类上的@Qualifier注解
                        targetAnnotation = AnnotationUtils.getAnnotation(ClassUtils.getUserClass(beanType), type);
                    }
                }
                catch (NoSuchBeanDefinitionException ex) {
                    // Not the usual case - simply forget about the type check...
                }
            }
            // 类上没有@Qualifier注解，bean定义有beanClass
            if (targetAnnotation == null && bd.hasBeanClass()) {
                // 从beanClass上获取
                targetAnnotation = AnnotationUtils.getAnnotation(ClassUtils.getUserClass(bd.getBeanClass()), type);
            }
        }
        // 找到目标注解，如果目标注解和期望注解相等，则返回true，表示匹配
        // 这里的equals方法重写过的了，会判断属性值是否相当。
        if (targetAnnotation != null && targetAnnotation.equals(annotation)) {
            return true;
        }
    }

    // 获取期望注解的属性
    Map<String, Object> attributes = AnnotationUtils.getAnnotationAttributes(annotation);
    // 如果期望注解属性为空，而且bean定义没有手动指定的AutowireCandidateQualifier，返回不匹配      
    // @Qualifer注解有默认属性值，不会为空，所以不会进入if。
    if (attributes.isEmpty() && qualifier == null) {             
        // If no attributes, the qualifier must be present
        return false;
    }    
    
    
    // 判断bean名称或者别名是否和@Qualifier注解的value值相等
    for (Map.Entry<String, Object> entry : attributes.entrySet()) {
        // 获取属性名称
        String attributeName = entry.getKey();
        // 获取期望注解@Qualifier的value属性值，即期望值
        Object expectedValue = entry.getValue();
        // 获取实际的值
        Object actualValue = null;
        // 首先检查qualifier
        // Check qualifier first
        if (qualifier != null) {
            // bean定义有手动指定的AutowireCandidateQualifier。从手动指定的qualifier获取实际的值。
            actualValue = qualifier.getAttribute(attributeName);
        }
        // 实际值为空
        if (actualValue == null) {
            // 回退到bean定义的属性获取，如果没有手动指定的话，一般获取不到。
            // Fall back on bean definition attribute
            actualValue = bd.getAttribute(attributeName);
        }
        
        /**
         如果同时满足：
        （1）实际值为null
        （2）注解的属性名称为value
        （3）期待值是字符串类型
        （4）bean名称（或别名）匹配期望值   
         则跳过，最后返回true表示匹配。因为@Qualifier注解只有一个属性，只会循环一次
        */        
        // bdHolder.matchesName((String) expectedValue)：确定给定的候选名称是否bean名称匹配，或者是别名匹配       
        if (actualValue == null && attributeName.equals(AutowireCandidateQualifier.VALUE_KEY) &&
                expectedValue instanceof String && bdHolder.matchesName((String) expectedValue)) {
            // bean名称（或别名）匹配期望值
            // Fall back on bean name (or alias) match
            // 跳过
            continue;
        }        
       
        if (actualValue == null && qualifier != null) {
            // 存在qualifier，但是没有指定实际值，而且从bean定义的属性也没有获取到。
            // 则获取注解的默认值作为实际值。
            // Fall back on default, but only if the qualifier is present
            actualValue = AnnotationUtils.getDefaultValue(annotation, attributeName);
        }
        if (actualValue != null) {
            // 必要时使用类型转化器
            actualValue = typeConverter.convertIfNecessary(actualValue, expectedValue.getClass());
        }
        // 如果期望值不等于实际值，则返回不匹配。
        if (!expectedValue.equals(actualValue)) {
            return false;
        }
    }
    // 返回true表示匹配。
    return true;
}

/**
 * 确定给定的候选名称是否与bean名称匹配或者与存储在这个bean定义中的别名匹配。
 * Determine whether the given candidate name matches the bean name
 * or the aliases stored in this bean definition.
 */
public boolean matchesName(@Nullable String candidateName) {
    return (candidateName != null && (candidateName.equals(this.beanName) ||
            candidateName.equals(BeanFactoryUtils.transformedBeanName(this.beanName)) ||
            ObjectUtils.containsElement(this.aliases, candidateName)));
}
```

**总结**

```
【判断bean是否属于候选的bean】                                                           
 分为以下两大情况                                                                     
 一、bean定义没有(程序员手动指定) AutowireCandidateQualifier                               
  (1)如果bean定义中有@Qualifier注解，则直接获取。判断是否和期望注解相同，相同则说明符合条件                       
     如果没有，则执行(2)                                                              
  (2)如果该bean是由@Bean方法创建的，获取@Bean方法上的@Qualifier注解。判断是否和期望注解相同，相同则说明符合条件        
     如果不是，则执行(3)                                                              
  (3)获取bean的Class对象, 获取Class对象的@Qualifier注解。判断是否和期望注解相同，相同则说明符合条件             
     如果没有@Qualifier注解，则执行(4)                                                                               
 （4）获取bean的名称, 获取期望注解的value属性。判断bean名称是否和value相同，相同则说明符合条件                    
     如果不相同，则说明不符合条件                                                           
  判断是否和期望注解相同：实际上重写了注解的equals方法，会判断注解的属性值是否相等。                                                                          
 二、bean定义有(程序员手动指定) AutowireCandidateQualifier                                
 如果注入的属性使用@Qualifier注解，没有指定value属性，则使用默认的空串作为期望值。                             
（1）候选的bean没有使用@Qualifier注解，则判断beanName是否等于期望值。                                
（2）候选的bean使用@Qualifier注解，但是没有指定value属性。都符合条件，都是候选bean                         
（3）候选的bean使用@Qualifier注解，指定value属性。判断指定的value值是否等于期望值                         
                                                                              
如果注入的属性使用@Qualifier注解， 指定value属性，则使用指定的值作为期望值。                                
（1）候选的bean没有使用@Qualifier注解，则判断beanName是否等于期望值。                                
（2）候选的bean使用@Qualifier注解，但是没有指定value属性。则判断beanName是否等于期望值。                    
（3）候选的bean使用@Qualifier注解，指定value属性。判断指定的value值是否等于期望值                         
```



* 程序员手动指定AutowireCandidateQualifier

```java
@Component
public class CustomRegister implements BeanDefinitionRegistryPostProcessor {

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        System.out.println("===================================开始手动注册bean=================================");
        // 注入beanName分别为f1Forest，f2Forest的Forest的Bean定义
        // 同时分别为它们指定@Qualifier("f1")、@Qualifier("f2");
        Class<Forest> forestClass = Forest.class;
        BeanDefinitionBuilder f1Builder = BeanDefinitionBuilder.genericBeanDefinition(forestClass);
        AbstractBeanDefinition f1 = f1Builder.getBeanDefinition();
        // 相当于手动添加@Qualifier("f1");
        AutowireCandidateQualifier f1Qualifier = new AutowireCandidateQualifier(Qualifier.class, "f1");
        // 相当于手动添加@Qualifier，不指定value属性
        //AutowireCandidateQualifier f1Qualifier = new AutowireCandidateQualifier(Qualifier.class);
        f1.addQualifier(f1Qualifier);
        // 指定beanName
        String f1ForestName = "f1Forest";
        // 注册bean定义
        registry.registerBeanDefinition(f1ForestName, f1);

        BeanDefinitionBuilder f2Builder = BeanDefinitionBuilder.genericBeanDefinition(forestClass);
        AbstractBeanDefinition f2 = f2Builder.getBeanDefinition();
        // 相当于手动添加@Qualifier("f2");
        AutowireCandidateQualifier f2Qualifier = new AutowireCandidateQualifier(Qualifier.class, "f2");
        // 相当于手动添加@Qualifier，不指定value属性
        //AutowireCandidateQualifier f2Qualifier = new AutowireCandidateQualifier(Qualifier.class);
        f2.addQualifier(f2Qualifier);
        // 指定beanName
        String f2ForestName = "f2Forest";
        // 注册bean定义
        registry.registerBeanDefinition(f2ForestName, f2);
    }    
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

    }
}
```

```java
@Data
public class Forest {
    private String name;
}
```



###### 3.3 保存候选的结果

```java
/**
 * Add an entry to the candidate map: a bean instance if available or just the resolved
 * type, preventing early bean initialization ahead of primary candidate selection.
 */
private void addCandidateEntry(Map<String, Object> candidates, String candidateName,
        DependencyDescriptor descriptor, Class<?> requiredType) {
    // 如果依赖描述器是MultiElementDescriptor 类型
    if (descriptor instanceof MultiElementDescriptor) {
        // 注入数组、集合、Map类型多个bean的时候，一般会走到这里。        
        // MultiElementDescriptor 继承 DependencyDescriptor，没重写descriptor的resolveCandidate。
   		// 默认是beanFactory.getBean(beanName);
        Object beanInstance = descriptor.resolveCandidate(candidateName, requiredType, this);
        if (!(beanInstance instanceof NullBean)) {
            // 保存beanName，及其对应的bean实例
            candidates.put(candidateName, beanInstance);
        }
    }
    // 如果beanFactory包含了该candidateName对应的bean实例，或者依赖描述器是StreamDependencyDescriptor类型，并且依赖描述器是排序的，则获取对应的bean实例并保存
    else if (containsSingleton(candidateName) || (descriptor instanceof StreamDependencyDescriptor &&
            ((StreamDependencyDescriptor) descriptor).isOrdered())) {
        // 默认是beanFactory.getBean(beanName);
        Object beanInstance = descriptor.resolveCandidate(candidateName, requiredType, this);
        // 保存beanName，及其对应的bean实例
        candidates.put(candidateName, (beanInstance instanceof NullBean ? null : beanInstance));
    }
    else {
        // 保存候选的beanName，及其对应的类型（即Class对象）
        candidates.put(candidateName, getType(candidateName));
    }
}

/** Cache of singleton objects: bean name to bean instance. */
private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256);
@Override
public boolean containsSingleton(String beanName) {
    return this.singletonObjects.containsKey(beanName);
}
```



##### 4、确定唯一依赖的bean

```java
/**
 * Determine the autowire candidate in the given set of beans.
 * <p>Looks for {@code @Primary} and {@code @Priority} (in that order).
 * @param candidates a Map of candidate names and candidate instances
 * that match the required type, as returned by {@link #findAutowireCandidates}
 * @param descriptor the target dependency to match against
 * @return the name of the autowire candidate, or {@code null} if none found
 */
@Nullable
protected String determineAutowireCandidate(Map<String, Object> candidates, DependencyDescriptor descriptor) {
    Class<?> requiredType = descriptor.getDependencyType();
     // 4.1、获取标注@Primary注解的bean名称
    String primaryCandidate = determinePrimaryCandidate(candidates, requiredType);
    if (primaryCandidate != null) {
        return primaryCandidate;
    }
    // 4.2、获取标注@Priority注解，而且是优先级最高的bean名称
    String priorityCandidate = determineHighestPriorityCandidate(candidates, requiredType);
    if (priorityCandidate != null) {
        return priorityCandidate;
    }
    // Fallback
    for (Map.Entry<String, Object> entry : candidates.entrySet()) {
        String candidateName = entry.getKey();
        Object beanInstance = entry.getValue();
        // 如果bean实例不等于null，已解析的依赖resolvableDependencies包含该bean实例，则直接返回。一般不包含。
        if ((beanInstance != null && this.resolvableDependencies.containsValue(beanInstance)) || 
              // 4.3 候选bean名称(或别名)是否等于依赖描述器的依赖名称
              //  如果等于，说明符合 
              matchesBeanName(candidateName, descriptor.getDependencyName())) {
            return candidateName;
        }
    }
    return null;
}

/** Map from dependency type to corresponding autowired value. */
private final Map<Class<?>, Object> resolvableDependencies = new ConcurrentHashMap<>(16);

//【注意】
//（1）属性注入：依赖名称是属性名称
//（2）方法注入：依赖名称是方法参数名称
// (3) 构造方法注入：依赖名称是构造方法参数名称
//（4）bean定义指定使用AUTOWIRE_BY_TYPE注入方式(autowireByType)：依赖名称为null

// 属性注入、方法注入、构造方法注入：DependencyDescriptor 
/**
 * Determine the name of the wrapped parameter/field.
 * @return the declared name (may be {@code null} if unresolvable)
 */
@Nullable
public String getDependencyName() {
    // 如果属性不等于null，则返回属性名称。反之返回方法/构造方法参数名称
    return (this.field != null ? this.field.getName() : obtainMethodParameter().getParameterName());
}

// MethodParameter
/**
 * Return the name of the method/constructor parameter.
 * @return the parameter name (may be {@code null} if no
 * parameter name metadata is contained in the class file or no
 * {@link #initParameterNameDiscovery ParameterNameDiscoverer}
 * has been set to begin with)
 */
@Nullable
public String getParameterName() {
    if (this.parameterIndex < 0) {
        return null;
    }
    ParameterNameDiscoverer discoverer = this.parameterNameDiscoverer;
    if (discoverer != null) {
        String[] parameterNames = null;
        if (this.executable instanceof Method) {
            // 获取方法所有的参数名称
            parameterNames = discoverer.getParameterNames((Method) this.executable);
        }
        else if (this.executable instanceof Constructor) {
            // 获取构造方法所有参数名称
            parameterNames = discoverer.getParameterNames((Constructor<?>) this.executable);
        }
        if (parameterNames != null) {
            // 获取具体参数类型的参数名称
            this.parameterName = parameterNames[this.parameterIndex];
        }
        this.parameterNameDiscoverer = null;
    }
    // 返回参数名称
    return this.parameterName;
}

// bean定义指定使用AUTOWIRE_BY_TYPE注入方式(autowireByType): AutowireByTypeDependencyDescriptor
// 一般是程序员指定
@Override
public String getDependencyName() {
    return null;
}
```

resolvableDependencies里一般是这8个元素，记录可解析的依赖。

![](https://lu-note.oss-cn-shenzhen.aliyuncs.com/notes/work/image-20241218211905812.png)



######  4.1、获取标注@Primary注解的bean名称

```java
/**
 * 确定标注了@Primary注解的bean的名称并返回
 * Determine the primary candidate in the given set of beans.
 * @param candidates a Map of candidate names and candidate instances
 * (or candidate classes if not created yet) that match the required type
 * @param requiredType the target dependency type to match against
 * @return the name of the primary candidate, or {@code null} if none found
 * @see #isPrimary(String, Object)
 */
@Nullable
protected String determinePrimaryCandidate(Map<String, Object> candidates, Class<?> requiredType) {
    String primaryBeanName = null;
    for (Map.Entry<String, Object> entry : candidates.entrySet()) {
        String candidateBeanName = entry.getKey();
        Object beanInstance = entry.getValue();
        // 是否有@Primary注解，如果是返回true
        if (isPrimary(candidateBeanName, beanInstance)) {
            if (primaryBeanName != null) {
                // 判断是否包含bean定义
                boolean candidateLocal = containsBeanDefinition(candidateBeanName);
                boolean primaryLocal = containsBeanDefinition(primaryBeanName);
                if (candidateLocal && primaryLocal) {
                    // 不止一个bean标注了@Primary注解，直接抛出NoUniqueBeanDefinitionException异常
                    throw new NoUniqueBeanDefinitionException(requiredType, candidates.size(),
                            "more than one 'primary' bean found among candidates: " + candidates.keySet());
                }
                else if (candidateLocal) {
                    primaryBeanName = candidateBeanName;
                }
            }
            else {
                 // 指定首要注入的beanName
                primaryBeanName = candidateBeanName;
            }
        }
    }
     // 返回首要注入的beanName
    return primaryBeanName;
}
```

【注意】**只能有一个被依赖的bean使用@Primary注解**



###### 4.2、获取标注@Priority注解，而且是优先级最高的bean名称

```java
/**
 * Determine the candidate with the highest priority in the given set of beans.
 * <p>Based on {@code @javax.annotation.Priority}. As defined by the related
 * {@link org.springframework.core.Ordered} interface, the lowest value has
 * the highest priority.
 * @param candidates a Map of candidate names and candidate instances
 * (or candidate classes if not created yet) that match the required type
 * @param requiredType the target dependency type to match against
 * @return the name of the candidate with the highest priority,
 * or {@code null} if none found
 * @see #getPriority(Object)
 */
@Nullable
protected String determineHighestPriorityCandidate(Map<String, Object> candidates, Class<?> requiredType) {
    String highestPriorityBeanName = null;
    Integer highestPriority = null;
    for (Map.Entry<String, Object> entry : candidates.entrySet()) {
        String candidateBeanName = entry.getKey();
        Object beanInstance = entry.getValue();
        if (beanInstance != null) {
            // javax.annotation.Priority
            // 获取@Priority注解的value值
            Integer candidatePriority = getPriority(beanInstance);
            if (candidatePriority != null) {
                if (highestPriorityBeanName != null) {                    
                    if (candidatePriority.equals(highestPriority)) {
                        // 最高优先级只能有一个，有多个的话，直接抛出异常
                        throw new NoUniqueBeanDefinitionException(requiredType, candidates.size(),
                                "Multiple beans found with the same priority ('" + highestPriority +
                                "') among candidates: " + candidates.keySet());
                    }
                    // @Priority注解的value值越小，优先级越高
                    else if (candidatePriority < highestPriority) {
                        // 指定最高优先级的beanName和最高优先级的值
                        highestPriorityBeanName = candidateBeanName;
                        highestPriority = candidatePriority;
                    }
                }
                else {
				   // 指定最高优先级的beanName和最高优先级的值
                    highestPriorityBeanName = candidateBeanName;
                    highestPriority = candidatePriority;
                }
            }
        }
    }
    // 返回最高优先级的beanName
    return highestPriorityBeanName;
}
```

【注意】**最高优先级的bean只能有一个**



###### 4.3 候选bean名称(或别名)是否等于依赖描述器的依赖名称

```java
/**
 * 确定给定的候选名称是否与bean名称或存储在此bean定义中的别名匹配。
 * Determine whether the given candidate name matches the bean name or the aliases
 * stored in this bean definition.
 */
protected boolean matchesBeanName(String beanName, @Nullable String candidateName) {
    return (candidateName != null &&
            (candidateName.equals(beanName) || ObjectUtils.containsElement(getAliases(beanName), candidateName)));
}

@Override
public String[] getAliases(String name) {
    // 返回bean名称，必要时去掉工厂bean引用前缀&，并将别名解析为规范名称。
    String beanName = transformedBeanName(name);
    List<String> aliases = new ArrayList<>();
    boolean factoryPrefix = name.startsWith(FACTORY_BEAN_PREFIX);
    String fullBeanName = beanName;
    if (factoryPrefix) {
        fullBeanName = FACTORY_BEAN_PREFIX + beanName;
    }
    // 如果fullBeanName不等于name，保存fullBeanName到别名
    if (!fullBeanName.equals(name)) {
        aliases.add(fullBeanName);
    }
    // 根据beanName获取所有别名
    String[] retrievedAliases = super.getAliases(beanName);
    String prefix = factoryPrefix ? FACTORY_BEAN_PREFIX : "";
    for (String retrievedAlias : retrievedAliases) {
        String alias = prefix + retrievedAlias;
        // retrievedAlias不等于name的都属于别名
        if (!alias.equals(name)) {
            aliases.add(alias);
        }
    }
    if (!containsSingleton(beanName) && !containsBeanDefinition(beanName)) {
        //当前BeanFactory不存在beanName，则从父BeanFactory中获取别名
        BeanFactory parentBeanFactory = getParentBeanFactory();
        if (parentBeanFactory != null) {
            aliases.addAll(Arrays.asList(parentBeanFactory.getAliases(fullBeanName)));
        }
    }
    // 返回所有别名
    return StringUtils.toStringArray(aliases);
}

//工厂bean前缀&
String FACTORY_BEAN_PREFIX = "&";

/**
 * 返回bean名称，必要时去掉工厂bean引用前缀&，并将别名解析为规范名称。
 * Return the bean name, stripping out the factory dereference prefix if necessary,
 * and resolving aliases to canonical names.
 * @param name the user-specified name
 * @return the transformed bean name
 */
protected String transformedBeanName(String name) {
    return canonicalName(BeanFactoryUtils.transformedBeanName(name));
}

/**
 * 返回实际的bean名称，去掉工厂解引用前缀(如果有的话，也去掉重复的工厂前缀)。
 * Return the actual bean name, stripping out the factory dereference
 * prefix (if any, also stripping repeated factory prefixes if found).
 * @param name the name of the bean
 * @return the transformed name
 * @see BeanFactory#FACTORY_BEAN_PREFIX
 */
public static String transformedBeanName(String name) {
    Assert.notNull(name, "'name' must not be null");
    if (!name.startsWith(BeanFactory.FACTORY_BEAN_PREFIX)) {
        return name;
    }
    return transformedBeanNameCache.computeIfAbsent(name, beanName -> {
        do {
            // 去除工厂bean的前缀
            beanName = beanName.substring(BeanFactory.FACTORY_BEAN_PREFIX.length());
        }
        while (beanName.startsWith(BeanFactory.FACTORY_BEAN_PREFIX));
        return beanName;
    });
}

/**
 * 确定原始名称，将别名解析为规范名称。
 * Determine the raw name, resolving aliases to canonical names.
 * @param name the user-specified name
 * @return the transformed name
 */
public String canonicalName(String name) {
    String canonicalName = name;
    // Handle aliasing...
    String resolvedName;
    do {
        resolvedName = this.aliasMap.get(canonicalName);
        if (resolvedName != null) {
            canonicalName = resolvedName;
        }
    }
    while (resolvedName != null);
    return canonicalName;
}

// 获取所有别名
@Override
public String[] getAliases(String name) {
    List<String> result = new ArrayList<>();
    synchronized (this.aliasMap) {        
        retrieveAliases(name, result);
    }
    return StringUtils.toStringArray(result);
}
/**
 * Transitively retrieve all aliases for the given name.
 * @param name the target name to find aliases for
 * @param result the resulting aliases list
 */
private void retrieveAliases(String name, List<String> result) {
    this.aliasMap.forEach((alias, registeredName) -> {
        if (registeredName.equals(name)) {
            result.add(alias);
            // 递归获取
            retrieveAliases(alias, result);
        }
    });
}
/** Map from alias to canonical name. */
private final Map<String, String> aliasMap = new ConcurrentHashMap<>(16);
```



### 方法注入

```java
@Override
protected void inject(Object bean, @Nullable String beanName, @Nullable PropertyValues pvs) throws Throwable {
    // 如果pvs中已经有当前注入点的值，则跳过注入。程序员自己设置了值
    if (checkPropertySkipping(pvs)) {
        return;
    }
    // 获取当前要注入的方法
    Method method = (Method) this.member;
    // 得到当前方法的参数值
    Object[] arguments;
    if (this.cached) {
        // 如果有缓存。第一次执行的时候是没有缓存的，只有注入过一次之后，才会有缓存
        try {
            // 从缓存中解析当前方法参数
            arguments = resolveCachedArguments(beanName);
        }
        catch (NoSuchBeanDefinitionException ex) {
            // Unexpected removal of target bean for cached argument -> re-resolve
            arguments = resolveMethodArguments(method, bean, beanName);
        }
    }
    else {
        // 解析方法(所有的)参数值。（从beanFactory中获取每个方法参数匹配的Bean对象）
        arguments = resolveMethodArguments(method, bean, beanName);
    }
    // 方法参数值不为空，通过反射调用方法
    if (arguments != null) {
        try {
            ReflectionUtils.makeAccessible(method);
            method.invoke(bean, arguments);
        }
        catch (InvocationTargetException ex) {
            throw ex.getTargetException();
        }
    }
}
```



*  解析方法(所有的)参数值

```java
private Object[] resolveMethodArguments(Method method, Object bean, @Nullable String beanName) {
        // 获取方法参数的个数
        int argumentCount = method.getParameterCount();
        // 创建一个参数的数组
        Object[] arguments = new Object[argumentCount];
        // 创建一个依赖描述器数组
        DependencyDescriptor[] descriptors = new DependencyDescriptor[argumentCount];
        // 定义要注入的每个bean的集合
        Set<String> autowiredBeans = new LinkedHashSet<>(argumentCount);
        Assert.state(beanFactory != null, "No BeanFactory available");
    	// 获取beanFactory类型转换器
        TypeConverter typeConverter = beanFactory.getTypeConverter();
        
        // 遍历每个方法参数，找到匹配的bean对象
        for (int i = 0; i < arguments.length; i++) {
            // 创建具体的方法参数对象
            MethodParameter methodParam = new MethodParameter(method, i);
            // 根据方法参数对象，创建依赖描述器
            DependencyDescriptor currDesc = new DependencyDescriptor(methodParam, this.required);
            // 设置依赖描述器对应的Class为当前Bean的Class
            currDesc.setContainingClass(bean.getClass());
            // 按顺序设置依赖描述器到数组中。
            descriptors[i] = currDesc;
            try {
                // 获取要注入的值。同属性注入的beanFactory.resolveDependency方法
                Object arg = beanFactory.resolveDependency(currDesc, beanName, autowiredBeans, typeConverter);
                // 参数不存在，并且当前方法不是必须要注入的，直接放弃继续寻找参数。
                if (arg == null && !this.required) {
                    // 设置所有的参数值为null
                    arguments = null;
                    // 跳出循环，直接放弃继续寻找参数。
                    break;
                }
                // 相应的参数赋值
                arguments[i] = arg;
            }
            catch (BeansException ex) {
                throw new UnsatisfiedDependencyException(null, beanName, new InjectionPoint(methodParam), ex);
            }
        }
        synchronized (this) {
            // 缓存不存在。第一次执行的时候不存在，第二次及之后执行不会进入。
            if (!this.cached) {
                // 参数数组不为null
                if (arguments != null) {
                    // 构建缓存的依赖描述器数组
                    DependencyDescriptor[] cachedMethodArguments = Arrays.copyOf(descriptors, arguments.length);
                    // 注册依赖的bean（记录beanName依赖autowiredBeanNames，同时记录autowiredBeanNames被beanName依赖了）
                    registerDependentBeans(beanName, autowiredBeans);
                    // 得到的参数值和需要的注入参数数量相同的时候
                    if (autowiredBeans.size() == argumentCount) {
                        // 获取要注入参数的迭代器
                        Iterator<String> it = autowiredBeans.iterator();
                        // 获取方法(所有的)参数类型数组
                        Class<?>[] paramTypes = method.getParameterTypes();
                        // 遍历每个参数的类型
                        for (int i = 0; i < paramTypes.length; i++) {
                            // 获取到类型对应要注入的beanName
                            String autowiredBeanName = it.next();
                            // bean工厂找到当前参数要注入的Bean，而且类型匹配
                            if (beanFactory.containsBean(autowiredBeanName) &&
                                    beanFactory.isTypeMatch(autowiredBeanName, paramTypes[i])) {
                                // 给当前参数，创建一个快捷方式的依赖描述器。即每个参数创建一个缓存。
                                // ShortcutDependencyDescriptor记录了当前参数的依赖描述器，注入的beanName，方法参数类型
                                cachedMethodArguments[i] = new ShortcutDependencyDescriptor(
                                        descriptors[i], autowiredBeanName, paramTypes[i]);
                            }
                        }
                    }
                    // 指定缓存依赖描述器数组
                    this.cachedMethodArguments = cachedMethodArguments;
                }
                else {
                    // 参数值不全的时候，或者没有参数的时候，设置缓存的方法参数值为null
                    this.cachedMethodArguments = null;
                }
                // 设置为true，表示已经有缓存
                this.cached = true;
            }
        }
        // 返回方法参数
        return arguments;
    }
}
```



* 从缓存中解析当前方法参数

```java
@Nullable
private Object[] resolveCachedArguments(@Nullable String beanName) {
    // ShortcutDependencyDescriptor数组
    Object[] cachedMethodArguments = this.cachedMethodArguments;
    if (cachedMethodArguments == null) {
        // 缓存不存在，返回null
        return null;
    }
    // 创建方法参数值数组
    Object[] arguments = new Object[cachedMethodArguments.length];
    for (int i = 0; i < arguments.length; i++) {
        // 从缓存的ShortcutDependencyDescriptor逐个解析方法参数值
        arguments[i] = resolvedCachedArgument(beanName, cachedMethodArguments[i]);
    }
    //返回方法参数值数组
    return arguments;
}

/**
 * 解析指定的缓存方法参数或字段值。
 * Resolve the specified cached method argument or field value.
 */
@Nullable
private Object resolvedCachedArgument(@Nullable String beanName, @Nullable Object cachedArgument) {
    if (cachedArgument instanceof DependencyDescriptor) {
        // 执行到这里
        DependencyDescriptor descriptor = (DependencyDescriptor) cachedArgument;
        Assert.state(this.beanFactory != null, "No BeanFactory available");
        // 同属性注入的beanFactory.resolveDependency方法调用。
        // 然后走到descriptor.resolveShortcut(this);
        // 调用ShortcutDependencyDescriptor的resolveShortcut方法，直接根据beanName和类型从beanFactory获取要注入的值
        return this.beanFactory.resolveDependency(descriptor, beanName, null, null);
    }
    else {
        return cachedArgument;
    }
}
```



### 构造方法注入

* 结论

```
1、只有一个构造方法，不管是有参还是无参，不管是否有@Autowired注解，都用来实例化。
   特殊的情况也包含：只有一个显式声明的无参构造方法，不管是否有@Autowired注解，都用来实例化。
2、多个构造方法，只能有一个构造方法使用@Autowired注解指定required=true，否则会报错，并使用required=true的构造方法实例化
3、多个构造方法，可以有多个构造方法使用@Autowired注解并required=false，选用参数最多、参数值和参数类型匹配最接近的public构造方法实例化
4、多个构造方法，所有的构造方法都无@Autowired注解，使用无参构造方法实例化
```

* 具体情况

（1）只有一个有参构造，不管是否有@Autowired注解，都用它来实例化

（2）有多个构造方法，包含无参和多个有参构造，有参构造都无@Autowired注解，则采用无参构造

（3）只有多个有参构造方法，有参构造方法都无@Autowired注解，会报错，因为找不到无参构造方法

（4）有多个构造方法，包含无参和多个有参构造，只能有一个有参构造方法使用@Autowired注解并指定required=true

（否则会报错)，并采用required=true的构造方法。

【注意】有多个构造方法的时候，如果所有构造方法使用@Autowired注解并指定required=false，这种情况是允许的

（5）只有多个有参构造方法，只能有一个有参构造方法使用@Autowired注解并指定required=true

（否则会报错)，并采用required=true的构造方法。

（6）有多个构造方法，包含无参和多个有参构造，所有有参构造都有@Autowired注解并指定required=false，则采用参数最多、

​	参数值和参数类型匹配最接近的public构造方法

（7）所有的构造方法，都有@Autowired注解并指定required=false（包括无参），则采用参数最多、参数值和参数类型匹配

​	最接近的public构造方法



```java
/**
 * 使用一个合适的策略：工厂方法（@Bean注解标注的方法），自动构造方法（@Autowired构造方法）或简单的实例化（无参构造），
 * 为指定的bean创建一个新的实例。
 * Create a new instance for the specified bean, using an appropriate instantiation strategy:
 * factory method, constructor autowiring, or simple instantiation.
 * @param beanName the name of the bean
 * @param mbd the bean definition for the bean
 * @param args explicit arguments to use for constructor or factory method invocation
 * @return a BeanWrapper for the new instance
 * @see #obtainFromSupplier
 * @see #instantiateUsingFactoryMethod
 * @see #autowireConstructor
 * @see #instantiateBean
 */
protected BeanWrapper createBeanInstance(String beanName, RootBeanDefinition mbd, @Nullable Object[] args) {
    // 确保bean的Class实际在这里被解析
    // Make sure bean class is actually resolved at this point.
    // 解析得到beanClass
    Class<?> beanClass = resolveBeanClass(mbd, beanName);

    // 如果beanClass不等于null，而且beanClass不是public的，而且bean的定义不允许非public访问，则抛出异常
    if (beanClass != null && !Modifier.isPublic(beanClass.getModifiers()) && !mbd.isNonPublicAccessAllowed()) {
        throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                "Bean class isn't public, and non-public access not allowed: " + beanClass.getName());
    }

    // 1、获取（bean定义指定的）bean实例的提供器
    Supplier<?> instanceSupplier = mbd.getInstanceSupplier();
    if (instanceSupplier != null) {
        // 从给定的提供器获取bean实例（@FeignClient就是通过回调函数来创建FeignClient）
        return obtainFromSupplier(instanceSupplier, beanName);
    }

    // 2、工厂方法（@Bean注解标注的方法）不为空，从工厂方法中获取bean实例
    if (mbd.getFactoryMethodName() != null) {
        // 使用工厂方法实例化
        return instantiateUsingFactoryMethod(beanName, mbd, args);
    }

    // 快捷方式获取，即检查是否有缓存
    // Shortcut when re-creating the same bean...
    boolean resolved = false;
    boolean autowireNecessary = false;
    if (args == null) {
        synchronized (mbd.constructorArgumentLock) {
            // 已解析的构造方法或工厂方法不为空
            if (mbd.resolvedConstructorOrFactoryMethod != null) {
                // 设置为已解析过
                resolved = true;
                // 是否有已解析的构造参数
                autowireNecessary = mbd.constructorArgumentsResolved;
            }
        }
    }
    // 是否已经解析过
    if (resolved) {
        // 执行到这里会走缓存
        if (autowireNecessary) {
            // 有已解析的构造参数，使用自动构造方法（@Autowired构造方法）实例化
            return autowireConstructor(beanName, mbd, null, null);
        }
        else {
            // 使用已解析的构造方法或工厂方法实例化
            return instantiateBean(beanName, mbd);
        }
    }

    // 3、(使用bean后处理器)确定候选的构造方法
    // Candidate constructors for autowiring?  一般来说这里获取到的都是自动注入的构造方法
    Constructor<?>[] ctors = determineConstructorsFromBeanPostProcessors(beanClass, beanName);
    // 如果满足以下其中一个条件，则进入if
    // （1）候选的构造方法不为空
    // （2）bean定义里指定了注入模式为AUTOWIRE_CONSTRUCTOR（一般程序员手动指定）
    // （3）bean定义里有指定的构造方法参数的值。（例如@Mapper的bean实例就是在bean定义中指定了构造方法参数）
    // （4）显式参数不为空
    if (ctors != null || mbd.getResolvedAutowireMode() == AUTOWIRE_CONSTRUCTOR ||
            mbd.hasConstructorArgumentValues() || !ObjectUtils.isEmpty(args)) {
        // 选择合适的候选构造方法实例化
        return autowireConstructor(beanName, mbd, ctors, args);
    }
    
    // Preferred constructors for default construction?
    ctors = mbd.getPreferredConstructors();
    // 4、使用更偏爱的构造方法获取实例。除非程序员在bean定义里指定，否则一般不存在
    if (ctors != null) {
        return autowireConstructor(beanName, mbd, ctors, null);
    }

    // 5、使用简单的无参构造方法获取实例
    // No special handling: simply use no-arg constructor.
    return instantiateBean(beanName, mbd);
}

/**
 * Determine preferred constructors to use for default construction, if any.
 * Constructor arguments will be autowired if necessary.
 * @return one or more preferred constructors, or {@code null} if none
 * (in which case the regular no-arg default constructor will be called)
 * @since 5.1
 */
@Nullable
public Constructor<?>[] getPreferredConstructors() {
    return null;
}
```

【注意】beanClass不是public的，而且bean的定义不允许非public访问，则抛出异常



* 从给定的提供器获取bean实例

```java
/**
 * Obtain a bean instance from the given supplier.
 * @param instanceSupplier the configured supplier
 * @param beanName the corresponding bean name
 * @return a BeanWrapper for the new instance
 * @since 5.0
 * @see #getObjectForBeanInstance
 */
protected BeanWrapper obtainFromSupplier(Supplier<?> instanceSupplier, String beanName) {
    Object instance;
	// 获取当前正在创建的beanName
    String outerBean = this.currentlyCreatedBean.get();
    // 设置当前正在创建的beanName
    this.currentlyCreatedBean.set(beanName);
    try {
        // 调用具体的Supplier的get方法获取实例
        instance = instanceSupplier.get();
    }
    finally {
        if (outerBean != null) {
            // 恢复先前正在创建的beanName
            this.currentlyCreatedBean.set(outerBean);
        }
        else {
            this.currentlyCreatedBean.remove();
        }
    }

    // 获取的实例为null，创建NullBean实例
    if (instance == null) {
        instance = new NullBean();
    }
    // 创建bean实例包装对象
    BeanWrapper bw = new BeanWrapperImpl(instance);
    // 初始化bean实例包装对象
    initBeanWrapper(bw);
    // 返回bean实例包装对象
    return bw;
}

// 函数式接口。重写get方法返回结果。
@FunctionalInterface
public interface Supplier<T> {

    /**
     * Gets a result.
     *
     * @return a result
     */
    T get();
}
```



* 使用简单的无参构造方法获取实例

```java
/**
 * Instantiate the given bean using its default constructor.
 * @param beanName the name of the bean
 * @param mbd the bean definition for the bean
 * @return a BeanWrapper for the new instance
 */
protected BeanWrapper instantiateBean(String beanName, RootBeanDefinition mbd) {
    try {
        Object beanInstance;
        if (System.getSecurityManager() != null) {
            beanInstance = AccessController.doPrivileged(
                    (PrivilegedAction<Object>) () -> getInstantiationStrategy().instantiate(mbd, beanName, this),
                    getAccessControlContext());
        }
        else {
            // getInstantiationStrategy()方法得到SimpleInstantiationStrategy
            // 使用默认构造函数实例化给定bean。见【实例化】
            beanInstance = getInstantiationStrategy().instantiate(mbd, beanName, this);
        }
        // 创建bean包装对象
        BeanWrapper bw = new BeanWrapperImpl(beanInstance);
        // 初始化bean包装对象
        initBeanWrapper(bw);
        // 返回bean包装对象
        return bw;
    }
    catch (Throwable ex) {
        throw new BeanCreationException(
                mbd.getResourceDescription(), beanName, "Instantiation of bean failed", ex);
    }
}
```



#### 确定所有候选的构造方法

* 使用bean后处理器确定候选的构造方法

```java
/**
 * 确定要用于给定bean的候选构造函数，检查所有已注册的SmartInstantiationAwareBeanPostProcessors。
 * Determine candidate constructors to use for the given bean, checking all registered
 * {@link SmartInstantiationAwareBeanPostProcessor SmartInstantiationAwareBeanPostProcessors}.
 * @param beanClass the raw class of the bean
 * @param beanName the name of the bean
 * @return the candidate constructors, or {@code null} if none specified
 * @throws org.springframework.beans.BeansException in case of errors
 * @see org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor#determineCandidateConstructors
 */
@Nullable
protected Constructor<?>[] determineConstructorsFromBeanPostProcessors(@Nullable Class<?> beanClass, String beanName) throws BeansException {
    if (beanClass != null && hasInstantiationAwareBeanPostProcessors()) {
        for (SmartInstantiationAwareBeanPostProcessor bp : getBeanPostProcessorCache().smartInstantiationAware) {
            Constructor<?>[] ctors = bp.determineCandidateConstructors(beanClass, beanName);
            if (ctors != null) {
                return ctors;
            }
        }
    }
    return null;
}
```



* AutowiredAnnotationBeanPostProcessor

```java
@Override
@Nullable
public Constructor<?>[] determineCandidateConstructors(Class<?> beanClass, final String beanName)
        throws BeanCreationException {

    // 检查beanClass所有标有@Lookup注解的方法
    // Let's check for lookup methods here...
    if (!this.lookupMethodsChecked.contains(beanName)) {
        if (AnnotationUtils.isCandidateClass(beanClass, Lookup.class)) {
            try {
                Class<?> targetClass = beanClass;
                do {
                    ReflectionUtils.doWithLocalMethods(targetClass, method -> {
                        // 获取方法上的@Lookup注解
                        Lookup lookup = method.getAnnotation(Lookup.class);
                        if (lookup != null) {
                            Assert.state(this.beanFactory != null, "No BeanFactory available");
                            // 创建LookupOverride
                            LookupOverride override = new LookupOverride(method, lookup.value());
                            try {
                                RootBeanDefinition mbd = (RootBeanDefinition)
                                        this.beanFactory.getMergedBeanDefinition(beanName);
                                // 添加LookupOverride到bean定义
                                mbd.getMethodOverrides().addOverride(override);
                            }
                            catch (NoSuchBeanDefinitionException ex) {
                                throw new BeanCreationException(beanName,
                                        "Cannot apply @Lookup to beans without corresponding bean definition");
                            }
                        }
                    });
                    // 从父类继续寻找
                    targetClass = targetClass.getSuperclass();
                }
                while (targetClass != null && targetClass != Object.class);

            }
            catch (IllegalStateException ex) {
                throw new BeanCreationException(beanName, "Lookup method resolution failed", ex);
            }
        }
        // 记录该beanClass已检查过
        this.lookupMethodsChecked.add(beanName);
    }

    // Quick check on the concurrent map first, with minimal locking.
    // 从缓存获取。第一次进来缓存为空
    Constructor<?>[] candidateConstructors = this.candidateConstructorsCache.get(beanClass);
    if (candidateConstructors == null) {        
        // Fully synchronized resolution now...
        synchronized (this.candidateConstructorsCache) {
            // 双检，再次从缓存获取。第一次进来缓存为空
            candidateConstructors = this.candidateConstructorsCache.get(beanClass);
            if (candidateConstructors == null) {
                Constructor<?>[] rawCandidates;
                try {
                    // 获取beanClass所有构造方法
                    rawCandidates = beanClass.getDeclaredConstructors();
                }
                catch (Throwable ex) {
                    throw new BeanCreationException(beanName,
                            "Resolution of declared constructors on bean Class [" + beanClass.getName() +
                            "] from ClassLoader [" + beanClass.getClassLoader() + "] failed", ex);
                }
                // 创建candidates保存所有的候选构造方法
                List<Constructor<?>> candidates = new ArrayList<>(rawCandidates.length);
                // 需要的构造方法（即@Autowired(required=true)的构造方法）
                Constructor<?> requiredConstructor = null;
                // 默认的构造方法（无参）
                Constructor<?> defaultConstructor = null;
                // 从beanClass获取首要的构造方法。一般为null
                Constructor<?> primaryConstructor = BeanUtils.findPrimaryConstructor(beanClass);
                // 非合成的构造方法个数
                int nonSyntheticConstructors = 0;
                // 遍历beanClass所有构造方法，判断是否有@Autowired的注解，找到候选的构造方法
                for (Constructor<?> candidate : rawCandidates) {
                    if (!candidate.isSynthetic()) {
                        // 构造方法不是合成的，非合成的构造方法个数加一
                        nonSyntheticConstructors++;
                    }
                    else if (primaryConstructor != null) {
                        // 首要构造方法不为空，跳过
                        continue;
                    }
                    // 获取构造方法上的@Autowired注解
                    MergedAnnotation<?> ann = findAutowiredAnnotation(candidate);
                    if (ann == null) {
                        // 注解为空，获取用户级别的类
                        Class<?> userClass = ClassUtils.getUserClass(beanClass);
                        if (userClass != beanClass) {
						  // 用户级别的类不等于beanClass
                            try {
                                // 从用户级别的类中获取相应参数类型的构造方法
                                Constructor<?> superCtor =
                                        userClass.getDeclaredConstructor(candidate.getParameterTypes());
                                // 获取构造方法上的@Autowired注解
                                ann = findAutowiredAnnotation(superCtor);
                            }
                            catch (NoSuchMethodException ex) {
                                // Simply proceed, no equivalent superclass constructor found...
                            }
                        }
                    }
                    // @Autowired注解不为空
                    if (ann != null) {
                        if (requiredConstructor != null) {
                            // 需要的构造方法已经存在，则抛出异常（即已经有一个@Autowired(required=true)的构造方法）
                            throw new BeanCreationException(beanName,
                                    "Invalid autowire-marked constructor: " + candidate +
                                    ". Found constructor with 'required' Autowired annotation already: " +
                                    requiredConstructor);
                        }
                        // 获取@Autowired注解的required属性
                        boolean required = determineRequiredStatus(ann);
                        if (required) {
                            // 是必须的（@Autowired注解的required属性为true）
                            
                            // 候选构造方法不为空，则抛出异常。什么情况呢？？？
                            // 一个构造方法使用@Autowired(required = false)
                            // 另一个使用@Autowired(required = true)，这也是不允许的。
                            if (!candidates.isEmpty()) {
                                // 只能有一个@Autowired(required=true)的构造方法
                                throw new BeanCreationException(beanName,
                                        "Invalid autowire-marked constructors: " + candidates +
                                        ". Found constructor with 'required' Autowired annotation: " +
                                        candidate);
                            }
                            // 指定候选的构造方法candidate为需要的构造方法
                            requiredConstructor = candidate;
                        }
                        // 标注了@Autowired注解的方法都作为候选构造方法，不管required属性是true还是false
                        candidates.add(candidate);
                    }
                    else if (candidate.getParameterCount() == 0) {
                        //@Autowired注解为null，候选的构造方法参数为0，设置为默认的构造方法
                        // 即无@Autowired注解的无参构造方法
                        defaultConstructor = candidate;
                    }
                }
                // 候选的构造方法不为空（@Autowired注解标注的构造方法）
                if (!candidates.isEmpty()) {
                    // Add default constructor to list of optional constructors, as fallback.
                    if (requiredConstructor == null) {
                        // @Autowired注解的required属性为true的构造方法为空                      
                        if (defaultConstructor != null) {
                            // 默认的构造方法不为空，添加到候选的构造方法。                            
                            // 什么情况会执行到这里呢？？？
                            // 有多个构造方法，包含无参和多个有参构造，有参构造都有@Autowired注解，
                            // 都指定required=false，则添加无参构造作为候选
                            candidates.add(defaultConstructor);
                        }                        
                        else if (candidates.size() == 1 && logger.isInfoEnabled()) {
                            logger.info("Inconsistent constructor declaration on bean with name '" + beanName +
                                    "': single autowire-marked constructor flagged as optional - " +
                                    "this constructor is effectively required since there is no " +
                                    "default constructor to fall back to: " + candidates.get(0));
                        }
                    }
                    // 返回候选的构造方法
                    // 如果只有@Autowired(required = true)的构造方法，则只有一个
				  // 如果都是@Autowired(required = false)的构造方法，则有多个	
                    candidateConstructors = candidates.toArray(new Constructor<?>[0]);
                }                
                // 只有一个有参构造（执行到这里说明没有@Autowired注解），用它来实例化，直接返回
                else if (rawCandidates.length == 1 && rawCandidates[0].getParameterCount() > 0) {
                    candidateConstructors = new Constructor<?>[] {rawCandidates[0]};
                }
                // 首要的构造方法和默认的构造方法都存在，而且不相等，则返回首要的构造方法和默认的构造方法
                else if (nonSyntheticConstructors == 2 && primaryConstructor != null &&
                        defaultConstructor != null && !primaryConstructor.equals(defaultConstructor)) {
                    candidateConstructors = new Constructor<?>[] {primaryConstructor, defaultConstructor};
                }
                // 只有一个首要的构造方法，直接返回
                else if (nonSyntheticConstructors == 1 && primaryConstructor != null) {
                    candidateConstructors = new Constructor<?>[] {primaryConstructor};
                }
                else {
                    // 执行到这里，说明没有候选的构造方法，设置为空数组
                    candidateConstructors = new Constructor<?>[0];
                }
                // 添加缓存
                this.candidateConstructorsCache.put(beanClass, candidateConstructors);
            }
        }
    }
    // 返回候选的构造方法
    return (candidateConstructors.length > 0 ? candidateConstructors : null);
}

private final Map<Class<?>, Constructor<?>[]> candidateConstructorsCache = new ConcurrentHashMap<>(256);

/**
 * 获取首要的构造方法
 * Return the primary constructor of the provided class. For Kotlin classes, this
 * returns the Java constructor corresponding to the Kotlin primary constructor
 * (as defined in the Kotlin specification). Otherwise, in particular for non-Kotlin
 * classes, this simply returns {@code null}.
 * @param clazz the class to check
 * @since 5.0
 * @see <a href="https://kotlinlang.org/docs/reference/classes.html#constructors">Kotlin docs</a>
 */
@Nullable
public static <T> Constructor<T> findPrimaryConstructor(Class<T> clazz) {
    Assert.notNull(clazz, "Class must not be null");
    if (KotlinDetector.isKotlinReflectPresent() && KotlinDetector.isKotlinType(clazz)) {
        return KotlinDelegate.findPrimaryConstructor(clazz);
    }
    // 返回null
    return null;
}
```



#### 选择合适的候选构造方法实例化

```java
/**
 * "autowire constructor" (with constructor arguments by type) behavior.
 * Also applied if explicit constructor argument values are specified,
 * matching all remaining arguments with beans from the bean factory.
 * <p>This corresponds to constructor injection: In this mode, a Spring
 * bean factory is able to host components that expect constructor-based
 * dependency resolution.
 * @param beanName the name of the bean
 * @param mbd the merged bean definition for the bean
 * @param chosenCtors chosen candidate constructors (or {@code null} if none)
 * @param explicitArgs argument values passed in programmatically via the getBean method,
 * or {@code null} if none (-> use constructor argument values from bean definition)
 * @return a BeanWrapper for the new instance
 */
public BeanWrapper autowireConstructor(String beanName, RootBeanDefinition mbd,
        @Nullable Constructor<?>[] chosenCtors, @Nullable Object[] explicitArgs) {
	// 创建bean实例包装对象
    BeanWrapperImpl bw = new BeanWrapperImpl();
    // 初始化bean包装对象
    this.beanFactory.initBeanWrapper(bw);

    // 使用的构造方法
    Constructor<?> constructorToUse = null;
    // 构造方法参数持有器
    ArgumentsHolder argsHolderToUse = null;
    // 构造方法使用的参数
    Object[] argsToUse = null;
	   
    if (explicitArgs != null) {
        // 明确的参数不为空，设置为构造方法使用的参数
        argsToUse = explicitArgs;
    }
    else {
        // 明确的参数为空
        
        // 需要解析的参数
        Object[] argsToResolve = null;
        synchronized (mbd.constructorArgumentLock) {
            // 获取已解析的（缓存的）构造方法或工厂方法不为空，作为使用的构造方法
            constructorToUse = (Constructor<?>) mbd.resolvedConstructorOrFactoryMethod;
            // 如果使用的构造方法不为空，构造方法的参数已解析
            if (constructorToUse != null && mbd.constructorArgumentsResolved) {
                // Found a cached constructor...
                // 获取已解析的（缓存的）构造方法参数，作为构造方法使用的参数
                argsToUse = mbd.resolvedConstructorArguments;
                if (argsToUse == null) {
   				   // 缓存的构造方法参数为空。
                    
                    // 第一次进来的时候，没有缓存，获取到null。
                    argsToResolve = mbd.preparedConstructorArguments;
                }
            }
        }
        // 如果需要解析的参数不为空，从中获取构造方法使用的参数
        if (argsToResolve != null) {
            // 5、解析存储在给定bean定义中的准备好的参数   
            argsToUse = resolvePreparedArguments(beanName, mbd, bw, constructorToUse, argsToResolve);
        }
    }

    // 使用的构造方法或构造方法使用的参数等于null
    if (constructorToUse == null || argsToUse == null) {
        // 获取指定的构造方法，如果有的话
        // Take specified constructors, if any.
        Constructor<?>[] candidates = chosenCtors;
        if (candidates == null) {
            // 候选的构造方法为null。一般不会执行到这里            
            // 获取beanClass
            Class<?> beanClass = mbd.getBeanClass();
            try {
                // 如果bean定义允许访问非public方法，则获取所有的构造方法，作为候选的构造方法
                // 反之，则获取所有的public构造方法，作为候选的构造方法
                candidates = (mbd.isNonPublicAccessAllowed() ?
                        beanClass.getDeclaredConstructors() : beanClass.getConstructors());
            }
            catch (Throwable ex) {
                throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                        "Resolution of declared constructors on bean Class [" + beanClass.getName() +
                        "] from ClassLoader [" + beanClass.getClassLoader() + "] failed", ex);
            }
        }

        // 使用无参的构造方法实例化        
        // 如果候选的构造方法只有一个，而且明确的构造方法参数为null，而且bean定义没有指定构造方法使用的参数
        if (candidates.length == 1 && explicitArgs == null && !mbd.hasConstructorArgumentValues()) {
            // 获取唯一的构造方法
            Constructor<?> uniqueCandidate = candidates[0];
            // 如果构造方法的参数为0
            if (uniqueCandidate.getParameterCount() == 0) {               
                synchronized (mbd.constructorArgumentLock) {
                    // 以下三个操作，相当于设置缓存
                    // 指定已解析的构造方法或工厂方法为无参构造方法
                    mbd.resolvedConstructorOrFactoryMethod = uniqueCandidate;
                    // 设置构造方法的参数为已解析
                    mbd.constructorArgumentsResolved = true;
                    // 设置解析的构造方法参数为空数组
                    mbd.resolvedConstructorArguments = EMPTY_ARGS;
                }
                // 使用uniqueCandidate和空数组进行实例化，即使用无参的构造方法实例化，并设置到bean的包装对象中。
                bw.setBeanInstance(instantiate(beanName, mbd, uniqueCandidate, EMPTY_ARGS));
                // 返回bean的包装对象
                return bw;
            }
        }
        
        // Need to resolve the constructor.
        // 需要解析构造方法。
        
        // 是否是自动注入     
        boolean autowiring = (chosenCtors != null ||
                mbd.getResolvedAutowireMode() == AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR);
        // 构造方法参数值对象
        ConstructorArgumentValues resolvedValues = null;

        // 最小的参数个数
        int minNrOfArgs;
        if (explicitArgs != null) {
            // 最小的参数个数为明确的参数个数
            minNrOfArgs = explicitArgs.length;
        }
        else {
            // 从bean定义获取构造方法参数值ConstructorArgumentValues
            // cargs中如果有构造方法参数值，通常是程序员手动指定的。具体可以参考：【注册bean定义时指定构造方法参数值】
            ConstructorArgumentValues cargs = mbd.getConstructorArgumentValues();
            // 创建已解析的构造方法参数值对象
            resolvedValues = new ConstructorArgumentValues();
            // 1、解析bean定义中已有的方法参数 
            // 将bean定义中已有的构造方法参数解析到resolvedValues对象，并将解析的构造方法值参数个数设置为最小的参数个数。
            // 如果程序员没有手动指定的构造方法参数，这里一般获取到0。
            minNrOfArgs = resolveConstructorArguments(beanName, mbd, bw, cargs, resolvedValues);            
        }
	    
        // 2、候选构造方法排序
        AutowireUtils.sortConstructors(candidates);
        // 设置最小类型差异的权重为int的最大值
        int minTypeDiffWeight = Integer.MAX_VALUE;
        // 创建集合保存“类型差异值等于最小类型差异的权重”的构造方法。
        // ambiguousFactoryMethods不为空表示有模棱两可的构造方法，即无法确定使用哪个构造方法
        // 但是由于使用的是宽松模式计算参数值的类型差异权重，使用第一个找到的类型差异权重值最小的构造方法。
        Set<Constructor<?>> ambiguousConstructors = null;
        // 创建队列，记录构造方法参数依赖注入失败的异常
        Deque<UnsatisfiedDependencyException> causes = null;

        // 遍历候选的构造方法，找到使用的构造方法和构造方法使用的参数
        for (Constructor<?> candidate : candidates) {
            // 获取构造方法参数的个数
            int parameterCount = candidate.getParameterCount();

            // 使用的构造方法不为null，而且构造方法使用的参数不为null， 构造方法使用的参数个数大于候选的构造方法参数。                   // 说明找到使用的构造方法和构造方法使用的参数，跳出循环
            if (constructorToUse != null && argsToUse != null && argsToUse.length > parameterCount) {
                // Already found greedy constructor that can be satisfied ->
                // do not look any further, there are only less greedy constructors left.               
			   //已经找到可以满足的贪心构造函数->
			   //不要再找了，只剩下不那么贪婪的构造函数了。
                break;
            }
            // 如果构造方法的参数小于最小的参数的个数，则跳过该候选的构造方法
            if (parameterCount < minNrOfArgs) {
                continue;
            }

            // 参数值持有对象
            ArgumentsHolder argsHolder;
            // 获取构造方法参数类型
            Class<?>[] paramTypes = candidate.getParameterTypes();
            
            if (resolvedValues != null) {
                // 构造方法参数值对象不为空。
                
                // 没有指定明确的参数，都会走到这里
                try {
                    // 获取构造方法所有的参数名称
                    String[] paramNames = ConstructorPropertiesChecker.evaluate(candidate, parameterCount);
                    if (paramNames == null) {
                        // 参数名称不存在。获取参数名称发现器
                        ParameterNameDiscoverer pnd = this.beanFactory.getParameterNameDiscoverer();
                        if (pnd != null) {
                            // 使用参数名称发现器获取参数名称
                            paramNames = pnd.getParameterNames(candidate);
                        }
                    }
                    // 3、获取构造方法参数值（包装到持有对象）
                    argsHolder = createArgumentArray(beanName, mbd, resolvedValues, bw, paramTypes, paramNames,
                            getUserDeclaredConstructor(candidate), autowiring, candidates.length == 1);
                }
                catch (UnsatisfiedDependencyException ex) {
                    // 构造方法参数依赖注入失败，记录异常，跳过当前候选的构造器，执行下一个
                    if (logger.isTraceEnabled()) {
                        logger.trace("Ignoring constructor [" + candidate + "] of bean '" + beanName + "': " + ex);
                    }
                    // Swallow and try next constructor.
                    if (causes == null) {
                        causes = new ArrayDeque<>(1);
                    }
                    causes.add(ex);
                    continue;
                }
            }
            else {
                // 给出显式参数->参数长度必须完全匹配。
                // Explicit arguments given -> arguments length must match exactly.
                if (parameterCount != explicitArgs.length) {
                    continue;
                }
                
                // 创建参数值持有对象，设置明确的参数值
                argsHolder = new ArgumentsHolder(explicitArgs);
            }

            // 4、获取类型差异的权重。
            // isLenientConstructorResolution:返回以宽松模式还是严格模式解析构造参数类型差异权重。默认为true，即宽松模式
            int typeDiffWeight = (mbd.isLenientConstructorResolution() ?          
                    argsHolder.getTypeDifferenceWeight(paramTypes) : argsHolder.getAssignabilityWeight(paramTypes));
            //如果它表示最接近的匹配，则选择此构造函数。
            // Choose this constructor if it represents the closest match.
            if (typeDiffWeight < minTypeDiffWeight) {
            	// 如果类型差异权重小于最小类型差异权重         
                
                // 设置候选的构造方法为使用的构造方法
                constructorToUse = candidate;
                // 设置使用的构造方法参数持有对象
                argsHolderToUse = argsHolder;
                // 设置使用的构造方法参数
                argsToUse = argsHolder.arguments;
                // 设置当前参数值的类型差异权重为最小类型差异权重
                minTypeDiffWeight = typeDiffWeight;
                // 设置ambiguousConstructors为空
                ambiguousConstructors = null;
            }
            else if (constructorToUse != null && typeDiffWeight == minTypeDiffWeight) {
                // 使用的构造方法不为null，参数值的类型差异权重等于最小类型差异权重，               
                if (ambiguousConstructors == null) {
                    // 初始化ambiguousConstructors
                    ambiguousConstructors = new LinkedHashSet<>();
                    // 添加使用的构造方法到ambiguousConstructors
                    ambiguousConstructors.add(constructorToUse);
                }
                 // 添加候选的构造方法到ambiguousConstructors
                ambiguousConstructors.add(candidate);
            }
        }

        // 执行到这里，如果还没有找到使用的构造方法，则抛出异常
        if (constructorToUse == null) {           
            if (causes != null) {
                UnsatisfiedDependencyException ex = causes.removeLast();
                for (Exception cause : causes) {
                    this.beanFactory.onSuppressedException(cause);
                }
                throw ex;
            }
            throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                    "Could not resolve matching constructor on bean class [" + mbd.getBeanClassName() + "] " +
                    "(hint: specify index/type/name arguments for simple parameters to avoid type ambiguities)");
        }
        // ambiguousConstructors不等于null，而且bean定义是以严格模式解析构造方法，而且抛出异常
        else if (ambiguousConstructors != null && !mbd.isLenientConstructorResolution()) {
            throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                    "Ambiguous constructor matches found on bean class [" + mbd.getBeanClassName() + "] " +
                    "(hint: specify index/type/name arguments for simple parameters to avoid type ambiguities): " +
                    ambiguousConstructors);
        }

        // 明确的参数为null，使用的构造方法参数持有器不为null，则缓存使用的构造方法和构造方法参数
        if (explicitArgs == null && argsHolderToUse != null) {
            argsHolderToUse.storeCache(mbd, constructorToUse);
        }
    }

    Assert.state(argsToUse != null, "Unresolved constructor arguments");
    // 使用constructorToUse和argsToUse进行实例化，并设置到bean的包装对象中。
    bw.setBeanInstance(instantiate(beanName, mbd, constructorToUse, argsToUse));
    // 返回bean的包装对象
    return bw;
}

// 空数组，表示参数为空
private static final Object[] EMPTY_ARGS = new Object[0];


public void storeCache(RootBeanDefinition mbd, Executable constructorOrFactoryMethod) {
    synchronized (mbd.constructorArgumentLock) {
        // 设置已解析的构造方法或工厂方法
        mbd.resolvedConstructorOrFactoryMethod = constructorOrFactoryMethod;
        // 设置构造方法参数为已解析
        mbd.constructorArgumentsResolved = true;
	    // 如果需要解析。默认为false，如果是自动注入，则会设置为true          
        if (this.resolveNecessary) {              
            // 设置准备好构造方法参数。
            // 如果是自动注入的话，preparedArguments是存有一个Object常量（即autowiredArgumentMarker）的数组。           
            mbd.preparedConstructorArguments = this.preparedArguments;
        }
        else {
            // 设置已解析的构造方法参数
            mbd.resolvedConstructorArguments = this.arguments;
        }
    }
}
```



##### 大致流程

![构造方法自动注入大致过程](https://lu-note.oss-cn-shenzhen.aliyuncs.com/notes/work/%E6%9E%84%E9%80%A0%E6%96%B9%E6%B3%95%E8%87%AA%E5%8A%A8%E6%B3%A8%E5%85%A5%E5%A4%A7%E8%87%B4%E8%BF%87%E7%A8%8B.jpg)



* 注册bean定义时指定构造方法值

以下是程序员手动注册bean定义的例子，其中Table、Mountain的bean定义都指定了构造方法参数值。

```java
package com.mrlu.register;

import com.mrlu.entity.*;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.stereotype.Component;

/**
 * @author 简单de快乐
 * @create 2024-12-22 22:51
 *
 * 自定义bean定义加载到Spring。
 *
 * @MppaerScan 注解加载的MapperScannerConfigurer 也是类似的原理
 */
@Component
public class CustomRegister implements BeanDefinitionRegistryPostProcessor {

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        // 注入house bean
        Class<House> houseClass = House.class;
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(houseClass);

        // 设置house的person属性
        Person person = new Person();
        person.setName("lu");
        builder.addPropertyValue("person", person);

        // 除person属性外，所有的属性设置根据类型注入。要求要有属性的设置方法。
        builder.getBeanDefinition().setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
        // 注册bean定义
        registry.registerBeanDefinition(getBeanName(houseClass), builder.getBeanDefinition());

        // 注入dragonClass bean
        Class<Dragon> dragonClass = Dragon.class;
        BeanDefinitionBuilder definition = BeanDefinitionBuilder.genericBeanDefinition(dragonClass);
        // 设置实例提供器
        definition.getBeanDefinition().setInstanceSupplier(() -> {
            Dragon dragon = new Dragon();
            dragon.setName("dragon");
            return dragon;
        });
        registry.registerBeanDefinition(getBeanName(dragonClass), definition.getBeanDefinition());

        // 创建Table实例，指定使用有参构造方法
        Class<Table> tableClass = Table.class;
        Desk desk = new Desk();
        desk.setBrand("Black-Desk");
        BeanDefinitionBuilder tableDefinition = BeanDefinitionBuilder.genericBeanDefinition(tableClass);
        // 设置构造方法使用的参数
        tableDefinition.getBeanDefinition().getConstructorArgumentValues()
                .addGenericArgumentValue(desk);
        registry.registerBeanDefinition(getBeanName(tableClass), tableDefinition.getBeanDefinition());

        // 创建并注册Mountain Bean定义
        Class<Mountain> mountainClass = Mountain.class;
        BeanDefinitionBuilder mountainDefinition = BeanDefinitionBuilder.genericBeanDefinition(mountainClass);
        // 添加构造方法参数
        ConstructorArgumentValues constructorArgumentValues = mountainDefinition.getBeanDefinition()
                .getConstructorArgumentValues();
        constructorArgumentValues.addIndexedArgumentValue(0, "珠峰");
        Tree tree = new Tree();
        tree.setName("树木");
        constructorArgumentValues.addIndexedArgumentValue(1, tree);
        Bird bird = new Bird();
        bird.setName("小鸟");
        constructorArgumentValues.addIndexedArgumentValue(2, bird);
        String mountainBean = getBeanName(mountainClass);
        // 对没有指定的参数值，使用构造方法参数注入
        mountainDefinition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);
        registry.registerBeanDefinition(mountainBean, mountainDefinition.getBeanDefinition());

        // 创建并注册country Bean定义
        Class<Country> countryClass = Country.class;
        BeanDefinitionBuilder countryBuilder = BeanDefinitionBuilder.genericBeanDefinition(countryClass);
        // 添加构造方法参数
        ConstructorArgumentValues argumentValues = countryBuilder.getBeanDefinition().getConstructorArgumentValues();
        argumentValues.addIndexedArgumentValue(0, "China");
        // 添加mountain Bean引用
        RuntimeBeanReference reference = new RuntimeBeanReference(mountainBean);
        argumentValues.addIndexedArgumentValue(1, reference);
        Man man = new Man();
        man.setName("男人");
        argumentValues.addGenericArgumentValue(man);
        // 对没有指定的参数值，使用构造方法参数注入
        countryBuilder.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);
        registry.registerBeanDefinition(getBeanName(countryClass), countryBuilder.getBeanDefinition());
    }

    public static String getBeanName(Class<?> beanClass) {
        return toLowerCamelCase(beanClass.getSimpleName());
    }

    /**
     * 将类名的首字母转换为小写
     * @param className 类名
     * @return 转换后的类名
     */
    public static String toLowerCamelCase(String className) {
        if (className == null || className.isEmpty()) {
            return className; // 如果输入为空，返回空字符串
        }
        return className.substring(0, 1).toLowerCase() + className.substring(1);
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

    }
}
```



```java
@Data
@ToString
public class Mountain implements InitializingBean {

    private String name;

    private Tree tree;

    private Bird bird;

    private River river;

    /**
     * Instantiates a new Mountain.
     */
    public Mountain() {
        System.out.println("Mountain无参构造方法");
    }

    /**
     * Instantiates a new Mountain.
     *
     * @param name the name
     * @param tree the tree
     */
    public Mountain(String name, Tree tree) {
        this.name = name;
        this.tree = tree;
        System.out.println("Mountain(String name, Tree tree)构造方法");
    }

    /**
     * Instantiates a new Mountain.
     *
     * @param tree the tree
     * @param bird the bird
     */
    public Mountain(Tree tree, Bird bird) {
        this.tree = tree;
        this.bird = bird;
        System.out.println("Mountain(Tree tree, Bird bird)构造方法");
    }

    /**
     * Instantiates a new Mountain.
     *
     * @see com.mrlu.register.CustomRegister
     * @param name the name
     * @param tree the tree
     * @param bird the bird
     */
    public Mountain(String name, Tree tree, Bird bird) {
        this.name = name;
        this.tree = tree;
        this.bird = bird;
        System.out.println("Mountain(String name, Tree tree, Bird bird)构造方法");
    }


    /**
     * Instantiates a new Mountain.
     *
     * 最终使用这个方法实例化。前三个参数从bean定义指定的构造方法参数获取，river参数从beanFactory解析依赖获取
     * @see com.mrlu.register.CustomRegister
     * @param name the name
     * @param tree the tree
     * @param bird the bird
     * @param river the river
     */
    public Mountain(String name, Tree tree, Bird bird, @Qualifier("r1") River river) {
        this.name = name;
        this.tree = tree;
        this.bird = bird;
        this.river = river;
        System.out.println("Mountain(String name, Tree tree, Bird bird, River river)构造方法");
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println(this);
    }
}

@Data
public class Tree {
    private String name;
}

@Data
public class Bird {
    private String name;
}

@Data
public class Table {
    public Table() {
    }
    public Table(Desk desk) {
        this.desk = desk;
    }
    private Desk desk;
}

@Data
@ToString
public class Desk {
    private String brand;
}


@ToString
public class Country implements InitializingBean {

    private String name;

    private Mountain mountain;

    private Man man;

    private River river;

    /**
     * Instantiates a new Country.
     *
     * @param name the name
     */
    public Country(String name) {
        this.name = name;
    }

    /**
     * Instantiates a new Country.
     *
     * @param name     the name
     * @param mountain the mountain
     */
    public Country(String name, Mountain mountain) {
        this.name = name;
        this.mountain = mountain;
    }

    /**
     * Instantiates a new Country.
     *
     * @param name     the name
     * @param mountain the mountain
     * @param man      the man
     */
    public Country(String name, Mountain mountain, Man man) {
        this.name = name;
        this.mountain = mountain;
        this.man = man;
    }

    /**
     * Instantiates a new Country.
     * 最终使用这个方法实例化。前三个参数从bean定义指定的构造方法参数获取，river参数从beanFactory解析依赖获取
     * @param name     the name
     * @param mountain the mountain
     * @param man      the man
     * @param river    the river
     */
    public Country(String name, Mountain mountain, Man man, @Qualifier("r1") River river) {
        this.name = name;
        this.mountain = mountain;
        this.man = man;
        this.river = river;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println(this);
    }
}
```

```java
@Configuration
public class RiverConfig {

    @Bean
    @Qualifier("r1")
    public River river1() {
        River river = new River();
        river.setName("长江");
        return river;
    }

    @Bean
    @Qualifier("r2")
    public River river2() {
        River river = new River();
        river.setName("黄河");
        return river;
    }

}
```



Mybatis注册Mapper的bean定义时，也是类似这样设置构造方法参数。

* 相关注册类

```
MapperScannerRegistrar
MapperScannerConfigurer
ClassPathMapperScanner
```

核心代码位于ClassPathMapperScanner的processBeanDefinitions方法

![2edce62e0a569d62de7823ff6f39262](https://lu-note.oss-cn-shenzhen.aliyuncs.com/notes/work/2edce62e0a569d62de7823ff6f39262.png)

![2114ddb6860472b739522be10cdd367](https://lu-note.oss-cn-shenzhen.aliyuncs.com/notes/work/2114ddb6860472b739522be10cdd367.png)



* BeanWrapperImpl

```java
@Nullable
Object wrappedObject;

@Nullable
Object rootObject;

/**
 * 将一个bean实例设置为持有，而不展开java.util.Optional。
 * Set a bean instance to hold, without any unwrapping of {@link java.util.Optional}.
 * @param object the actual target object
 * @since 4.3
 * @see #setWrappedInstance(Object)
 */
public void setBeanInstance(Object object) {
    this.wrappedObject = object;
    this.rootObject = object;
    this.typeConverterDelegate = new TypeConverterDelegate(this, this.wrappedObject);
    setIntrospectionClass(object.getClass());
}
```

* instantiate方法

使用构造方法和构造方法参数进行实例化

```java
private Object instantiate(
        String beanName, RootBeanDefinition mbd, Constructor<?> constructorToUse, Object[] argsToUse) {

    try {
        // 从beanFactory获取实例化策略
        InstantiationStrategy strategy = this.beanFactory.getInstantiationStrategy();
        if (System.getSecurityManager() != null) {
            return AccessController.doPrivileged((PrivilegedAction<Object>) () ->
                    strategy.instantiate(mbd, beanName, this.beanFactory, constructorToUse, argsToUse),
                    this.beanFactory.getAccessControlContext());
        }
        else {
            // 使用有参构造方法进行实例化（SimpleInstantiationStrategy）
            // 具体见【实例化】
            return strategy.instantiate(mbd, beanName, this.beanFactory, constructorToUse, argsToUse);
        }
    }
    catch (Throwable ex) {
        throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                "Bean instantiation via constructor failed", ex);
    }
}
```



##### 1、解析bean定义中已有的方法参数 

将bean定义中已有的构造方法参数（cargs）解析到resolvedValues对象中。实际上bean定义里的工厂方法参数值也是用这个解析

* ConstructorResolver

```java
/**
 * 将此bean的构造函数参数解析到resolvedValues对象中。这可能涉及到查找其他bean。
 * 此方法还用于处理静态工厂方法的调用。 
 *
 * Resolve the constructor arguments for this bean into the resolvedValues object.
 * This may involve looking up other beans.
 * <p>This method is also used for handling invocations of static factory methods.
 */
private int resolveConstructorArguments(String beanName, RootBeanDefinition mbd, BeanWrapper bw,
        ConstructorArgumentValues cargs, ConstructorArgumentValues resolvedValues) {
    // 获取beanFactory自定义的类型转换器
    TypeConverter customConverter = this.beanFactory.getCustomTypeConverter();
    // 设置类型转换器
    TypeConverter converter = (customConverter != null ? customConverter : bw);
    // 创建Bean定义值解析器。一般用于@Value注解解析
    BeanDefinitionValueResolver valueResolver =
            new BeanDefinitionValueResolver(this.beanFactory, beanName, mbd, converter);
	
    // 获取参数个数，作为最小的参数个数
    int minNrOfArgs = cargs.getArgumentCount();

    // 遍历索引参数值，解析保存到resolvedValues
    for (Map.Entry<Integer, ConstructorArgumentValues.ValueHolder> entry : cargs.getIndexedArgumentValues().entrySet()) {
        // 获取索引值，即参数在方法的位置。
        int index = entry.getKey();
        if (index < 0) {
            // 如果索引值小于0，则抛出异常。因为参数位置不可能有小于0的情况
            throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                    "Invalid constructor argument index: " + index);
        }
        // 如果索引值 + 1大于最小的参数个数，设置最小的参数个数等于索引值 + 1
        if (index + 1 > minNrOfArgs) {
            minNrOfArgs = index + 1;
        }
        // 获取参数值持有器
        ConstructorArgumentValues.ValueHolder valueHolder = entry.getValue();
        // 如果参数值持有器是已转换的，默认是false
        if (valueHolder.isConverted()) {
            // 参数值持有器是已转换的
            
            // 保存参数索引（参数位置），参数持有对象到resolvedValues
            resolvedValues.addIndexedArgumentValue(index, valueHolder);
        }
        else {
            // 参数值持有器不是已转换的
            
            // 获取参数值，如果有必要的话，使用Bean定义值解析器进行解析
            Object resolvedValue =
                    valueResolver.resolveValueIfNecessary("constructor argument", valueHolder.getValue());
            // 根据参数值、参数类型以及参数名称，创建已解析的参数持有对象
            ConstructorArgumentValues.ValueHolder resolvedValueHolder =
                    new ConstructorArgumentValues.ValueHolder(resolvedValue, valueHolder.getType(), valueHolder.getName());
            // 设置已解析的参数持有对象的来源为valueHolder
            resolvedValueHolder.setSource(valueHolder);
            // 保存参数索引（参数位置），已解析的参数持有对象到resolvedValues
            resolvedValues.addIndexedArgumentValue(index, resolvedValueHolder);
        }
    }

    // 遍历通用的参数值，解析保存到resolvedValues
    for (ConstructorArgumentValues.ValueHolder valueHolder : cargs.getGenericArgumentValues()) {
        // 如果参数值持有器是已转换的，默认是false
        if (valueHolder.isConverted()) {
            // 保存参数持有对象到resolvedValues
            resolvedValues.addGenericArgumentValue(valueHolder);
        }
        else {
		   // 获取参数值，如果有必要的话，使用Bean定义值解析器进行解析
            Object resolvedValue =
                    valueResolver.resolveValueIfNecessary("constructor argument", valueHolder.getValue());
            // 根据参数值、参数类型以及参数名称，创建已解析的参数持有对象
            ConstructorArgumentValues.ValueHolder resolvedValueHolder = new ConstructorArgumentValues.ValueHolder(
                    resolvedValue, valueHolder.getType(), valueHolder.getName());
            // 设置已解析的参数持有对象的来源为valueHolder
            resolvedValueHolder.setSource(valueHolder);
            // 保存已解析的参数持有对象到resolvedValues
            resolvedValues.addGenericArgumentValue(resolvedValueHolder);
        }
    }
    // 返回最小的参数个数
    return minNrOfArgs;
}

// ConstructorArgumentValues
private final Map<Integer, ValueHolder> indexedArgumentValues = new LinkedHashMap<>();
private final List<ValueHolder> genericArgumentValues = new ArrayList<>();
/**
 * 返回此实例中保存的参数值的数量，包括索引参数值和通用参数值。
 * Return the number of argument values held in this instance,
 * counting both indexed and generic argument values.
 */
public int getArgumentCount() {
    return (this.indexedArgumentValues.size() + this.genericArgumentValues.size());
}

/**
 * Add an argument value for the given index in the constructor argument list.
 * @param index the index in the constructor argument list
 * @param newValue the argument value in the form of a ValueHolder
 */
public void addIndexedArgumentValue(int index, ValueHolder newValue) {
    Assert.isTrue(index >= 0, "Index must not be negative");
    Assert.notNull(newValue, "ValueHolder must not be null");
    // 在构造函数参数列表中为给定索引添加一个参数值。
    addOrMergeIndexedArgumentValue(index, newValue);
}

/**
 * 在构造函数参数列表中为给定索引添加一个参数值。
 * Add an argument value for the given index in the constructor argument list,
 * merging the new value (typically a collection) with the current value
 * if demanded: see {@link org.springframework.beans.Mergeable}.
 * @param key the index in the constructor argument list
 * @param newValue the argument value in the form of a ValueHolder
 */
private void addOrMergeIndexedArgumentValue(Integer key, ValueHolder newValue) {
    ValueHolder currentValue = this.indexedArgumentValues.get(key);
    // 获取当前值，如果当前值不为null，而且新的值是Mergeable类型
    if (currentValue != null && newValue.getValue() instanceof Mergeable) {
        // 获取新的值
        Mergeable mergeable = (Mergeable) newValue.getValue();
        if (mergeable.isMergeEnabled()) {
            // 如果新的值是允许合并的，合并新的值和当前值，返回合并后的值作为新的值
            newValue.setValue(mergeable.merge(currentValue.getValue()));
        }
    }
    // 保存索引位置及其对应的新值
    this.indexedArgumentValues.put(key, newValue);
}

/**
 * Add a generic argument value to be matched by type or name (if available).
 * <p>Note: A single generic argument value will just be used once,
 * rather than matched multiple times.
 * @param newValue the argument value in the form of a ValueHolder
 * <p>Note: Identical ValueHolder instances will only be registered once,
 * to allow for merging and re-merging of argument value definitions. Distinct
 * ValueHolder instances carrying the same content are of course allowed.
 */
public void addGenericArgumentValue(ValueHolder newValue) {
    Assert.notNull(newValue, "ValueHolder must not be null");
    if (!this.genericArgumentValues.contains(newValue)) {
        addOrMergeGenericArgumentValue(newValue);
    }
}
/**
 * Add a generic argument value, merging the new value (typically a collection)
 * with the current value if demanded: see {@link org.springframework.beans.Mergeable}.
 * @param newValue the argument value in the form of a ValueHolder
 */
private void addOrMergeGenericArgumentValue(ValueHolder newValue) {
    // 如果新值的名称不等于null
    if (newValue.getName() != null) {        
        for (Iterator<ValueHolder> it = this.genericArgumentValues.iterator(); it.hasNext();) {
            // 获取当前值
            ValueHolder currentValue = it.next();
            // 如果当前值的名称等于新值的名称
            if (newValue.getName().equals(currentValue.getName())) {
                // 新值是Mergeable类型
                if (newValue.getValue() instanceof Mergeable) {
                    // 获取新值
                    Mergeable mergeable = (Mergeable) newValue.getValue();
                    if (mergeable.isMergeEnabled()) {
                        // 如果新的值是允许合并的，合并新的值和当前值，返回合并后的值作为新的值
                        newValue.setValue(mergeable.merge(currentValue.getValue()));
                    }
                }
                // 移除
                it.remove();
            }
        }
    }
    // 保存新值
    this.genericArgumentValues.add(newValue);
}
```



##### 2、候选构造方法排序

```java
/**
 * 对给定的构造函数进行排序，优先选择具有最大参数数量的公共构造函数和“贪婪”构造函数。
 * 结果将首先包含公共构造函数，参数数量减少，然后是非公共构造函数，再次包含参数数量减少。 
 * Sort the given constructors, preferring public constructors and "greedy" ones with
 * a maximum number of arguments. The result will contain public constructors first,
 * with decreasing number of arguments, then non-public constructors, again with
 * decreasing number of arguments.
 * @param constructors the constructor array to sort
 */
public static void sortConstructors(Constructor<?>[] constructors) {
    Arrays.sort(constructors, EXECUTABLE_COMPARATOR);
}

public static final Comparator<Executable> EXECUTABLE_COMPARATOR = (e1, e2) -> {
    // 先根据public排序，再根据参数数量倒序
    int result = Boolean.compare(Modifier.isPublic(e2.getModifiers()), Modifier.isPublic(e1.getModifiers()));
    return result != 0 ? result : Integer.compare(e2.getParameterCount(), e1.getParameterCount());
};
```



#####  3、获取构造方法参数值

**先从bean定义中指定的参数值获取，如果获取到，则保存。如果获取不到，再从beanFactory获取自动注入的值**

即：bean定义指定的参数值  -> beanFactory获取自动注入的值

```java
/**
 * 给定已解析的构造函数参数值，创建参数数组以调用构造函数或工厂方法。
 * Create an array of arguments to invoke a constructor or factory method,
 * given the resolved constructor argument values.
 */
private ArgumentsHolder createArgumentArray(
        String beanName, RootBeanDefinition mbd, @Nullable ConstructorArgumentValues resolvedValues,
        BeanWrapper bw, Class<?>[] paramTypes, @Nullable String[] paramNames, Executable executable,
        boolean autowiring, boolean fallback) throws UnsatisfiedDependencyException {
    // 获取beanFactory自定义的类型转换器
    TypeConverter customConverter = this.beanFactory.getCustomTypeConverter();
    // 设置类型转换器
    TypeConverter converter = (customConverter != null ? customConverter : bw);

    // 创建参数持有器
    ArgumentsHolder args = new ArgumentsHolder(paramTypes.length);
    // 创建集合保存使用的参数值持有器
    Set<ConstructorArgumentValues.ValueHolder> usedValueHolders = new HashSet<>(paramTypes.length);
    // 创建集合，记录依赖的beanName
    Set<String> autowiredBeanNames = new LinkedHashSet<>(4);

    // 遍历参数类型，先从bean定义中指定的参数值获取，如果获取到，则保存。如果获取不到，再从beanFactory获取自动注入的值
    for (int paramIndex = 0; paramIndex < paramTypes.length; paramIndex++) {
        // 获取构造方法参数类型
        Class<?> paramType = paramTypes[paramIndex];
        // 获取参数名称
        String paramName = (paramNames != null ? paramNames[paramIndex] : "");
        // 尝试查找匹配的构造函数参数值，无论是索引的还是通用的
        // Try to find matching constructor argument value, either indexed or generic.
        ConstructorArgumentValues.ValueHolder valueHolder = null;
        if (resolvedValues != null) {
            // 从前面的分析可知，构造方法参数值对象不等于null。resolvedValues = new ConstructorArgumentValues();
            
            // 3.1 从已解析的参数值对象获取参数值持有对象。
            // 如果没有手动指定构造方法参数值，resolvedValues里没有保存构造方法参数，获取到null。
            valueHolder = resolvedValues.getArgumentValue(paramIndex, paramType, paramName, usedValueHolders);
            // If we couldn't find a direct match and are not supposed to autowire,
            // let's try the next generic, untyped argument value as fallback:
            // it could match after type conversion (for example, String -> int).
            //如果我们找不到直接匹配，并且不应该自动加载，
		   //让我们尝试下一个泛型，无类型参数值作为回退：
		   //它可以在类型转换后匹配（例如String -> int）。
                       
            // valueHolder等于null是什么情况呢？？
            // （1）没有手动指定构造方法参数值，resolvedValues里没有保存构造方法参数
            // （2）手动指定了构造方法参数值（一般是程序员指定），但是getArgumentValue方法根据参数类型和名称没有获取到匹配的					参数值
            
            // 如果没有手动指定构造方法参数值， resolvedValues.getArgumentCount()获取到0。
            // 如果有，则是指定的构造方法参数值个数。            
            // 默认情况下，resolvedValues.getArgumentCount()获取到0
            
            // 如果满足以下其中一种情况：
            // （1）valueHolder等于null，而且不是自动注入
            // （2）valueHolder等于null，参数类型个数等于已解析的参数个数
            // 则通过已解析的参数值对象的getGenericArgumentValue方法获取具体的参数值持有对象
            if (valueHolder == null && (!autowiring || paramTypes.length == resolvedValues.getArgumentCount())) {
                valueHolder = resolvedValues.getGenericArgumentValue(null, null, usedValueHolders);
            }
            
            // 能从resolvedValues中获取到valueHolder是什么情况呢？？？
            // 一般是程序员在手动注册某个bean的BeanDefinition定义时，添加了相应的参数值到ConstructorArgumentValues。
            // 也就是在创建bean定义的时候就指定了使用的参数值，不需要自动注入。
        }
        
        // 参数值持有器不为空
        if (valueHolder != null) {
            // 执行到这里说明从bean定义手动指定的参数值中获取到了匹配的值。            
            // 从参数值持有器获取参数值
            
            // We found a potential match - let's give it a try.
            // Do not consider the same value definition multiple times!            
		   //我们找到了一个可能匹配的对象，让我们试一试。
		   //不要多次考虑相同的值定义！
            
            // 保存到已使用的参数值持有器集合
            usedValueHolders.add(valueHolder);
            // 获取原始的参数值
            Object originalValue = valueHolder.getValue();
            Object convertedValue;
            // 参数值是否是转换的
            if (valueHolder.isConverted()) {
                // 获取转换后的值
                convertedValue = valueHolder.getConvertedValue();
                // 设置转换后的值到参数持有器的“准备好的参数值”
                args.preparedArguments[paramIndex] = convertedValue;
            }
            else {  
                // 参数值不是转换的
                
                // 创建MethodParameter
                MethodParameter methodParam = MethodParameter.forExecutable(executable, paramIndex);
                try {
                    // 使用类型转换器获取转换后的值，如果有必要的话 
                    convertedValue = converter.convertIfNecessary(originalValue, paramType, methodParam);
                }
                catch (TypeMismatchException ex) {
                    throw new UnsatisfiedDependencyException(
                            mbd.getResourceDescription(), beanName, new InjectionPoint(methodParam),
                            "Could not convert argument value of type [" +
                                    ObjectUtils.nullSafeClassName(valueHolder.getValue()) +
                                    "] to required type [" + paramType.getName() + "]: " + ex.getMessage());
                }
                // 获取参数值的来源ValueHolder
                Object sourceHolder = valueHolder.getSource();
                // 如果sourceHolder是ValueHolder类型
                if (sourceHolder instanceof ConstructorArgumentValues.ValueHolder) {                  
                    // 从参数值的来源ValueHolder获取值，作为来源值
                    Object sourceValue = ((ConstructorArgumentValues.ValueHolder) sourceHolder).getValue();
                    // 设置需要解析为true
                    args.resolveNecessary = true;
                    // 设置来源值为准备好的参数值
                    args.preparedArguments[paramIndex] = sourceValue;
                }
            }
            // 设置参数值为转换后的值
            args.arguments[paramIndex] = convertedValue;
            // 设置原始的参数值
            args.rawArguments[paramIndex] = originalValue;
        }
        else {
            // 通过依赖注入，获取构造方法的参数值，通常第一次进来都会走这里            
            
            // 创建MethodParameter
            MethodParameter methodParam = MethodParameter.forExecutable(executable, paramIndex);
            // 没有找到明确的匹配：我们要么应该自动连接，要么
            // 必须为给定构造函数创建参数数组失败。
            // No explicit match found: we're either supposed to autowire or
            // have to fail creating an argument array for the given constructor.
            if (!autowiring) {
                // 不是自动注入，直接抛出异常
                throw new UnsatisfiedDependencyException(
                        mbd.getResourceDescription(), beanName, new InjectionPoint(methodParam),
                        "Ambiguous argument values for parameter of type [" + paramType.getName() +
                        "] - did you specify the correct bean references as arguments?");
            }
            try {
                // 3.2 获取自动注入的构造方法参数值
                Object autowiredArgument = resolveAutowiredArgument(
                        methodParam, beanName, autowiredBeanNames, converter, fallback);
                // 设置自动注入的参数值为原始的参数值
                args.rawArguments[paramIndex] = autowiredArgument;
                // 设置自动注入的参数值为构造方法的参数值
                args.arguments[paramIndex] = autowiredArgument;
                // 设置Object对象为准备好的参数（这里的Object对象是常量）
                args.preparedArguments[paramIndex] = autowiredArgumentMarker;
                // 设置需要解析为true
                args.resolveNecessary = true;
            }
            catch (BeansException ex) {
                throw new UnsatisfiedDependencyException(
                        mbd.getResourceDescription(), beanName, new InjectionPoint(methodParam), ex);
            }
        }
    }

    // 遍历依赖注入的bean
    for (String autowiredBeanName : autowiredBeanNames) {
        // 注册依赖的bean（记录beanName依赖autowiredBeanNames，同时记录autowiredBeanNames被beanName依赖了）
        this.beanFactory.registerDependentBean(autowiredBeanName, beanName);
        if (logger.isDebugEnabled()) {
            logger.debug("Autowiring by type from bean name '" + beanName +
                    "' via " + (executable instanceof Constructor ? "constructor" : "factory method") +
                    " to bean named '" + autowiredBeanName + "'");
        }
    }
    
    // 返回解析好的参数持有对象
    return args;
}

/**
 * 缓存参数数组中自动注入的参数的标记，将被解析的自动注入参数替换。
 * 
 * 如果preparedArguments有该标识，说明是要解析依赖的
 * Marker for autowired arguments in a cached argument array, to be replaced
 * by a {@linkplain #resolveAutowiredArgument resolved autowired argument}.
 */
private static final Object autowiredArgumentMarker = new Object();
```



```java
/**
 * Create a new MethodParameter for the given method or constructor.
 * <p>This is a convenience factory method for scenarios where a
 * Method or Constructor reference is treated in a generic fashion.
 * @param executable the Method or Constructor to specify a parameter for
 * @param parameterIndex the index of the parameter
 * @return the corresponding MethodParameter instance
 * @since 5.0
 */
public static MethodParameter forExecutable(Executable executable, int parameterIndex) {
    if (executable instanceof Method) {
        // 工厂方法注入走到这里
        return new MethodParameter((Method) executable, parameterIndex);
    }
    else if (executable instanceof Constructor) {
        // 构造方法注入走到这里
        return new MethodParameter((Constructor<?>) executable, parameterIndex);
    }
    else {
        throw new IllegalArgumentException("Not a Method/Constructor: " + executable);
    }
}

/**
 * Create a new {@code MethodParameter} for the given method, with nesting level 1.
 * @param method the Method to specify a parameter for
 * @param parameterIndex the index of the parameter: -1 for the method
 * return type; 0 for the first method parameter; 1 for the second method
 * parameter, etc.
 */
public MethodParameter(Method method, int parameterIndex) {
    this(method, parameterIndex, 1);
}

/**
 * Create a new {@code MethodParameter} for the given method.
 * @param method the Method to specify a parameter for
 * @param parameterIndex the index of the parameter: -1 for the method
 * return type; 0 for the first method parameter; 1 for the second method
 * parameter, etc.
 * @param nestingLevel the nesting level of the target type
 * (typically 1; e.g. in case of a List of Lists, 1 would indicate the
 * nested List, whereas 2 would indicate the element of the nested List)
 */
public MethodParameter(Method method, int parameterIndex, int nestingLevel) {
    Assert.notNull(method, "Method must not be null");
    this.executable = method;
    // 设置方法参数位置
    this.parameterIndex = validateIndex(method, parameterIndex);
    this.nestingLevel = nestingLevel;
}


/**
 * Create a new MethodParameter for the given constructor, with nesting level 1.
 * @param constructor the Constructor to specify a parameter for
 * @param parameterIndex the index of the parameter
 */
public MethodParameter(Constructor<?> constructor, int parameterIndex) {
    this(constructor, parameterIndex, 1);
}

/**
 * Create a new MethodParameter for the given constructor.
 * @param constructor the Constructor to specify a parameter for
 * @param parameterIndex the index of the parameter
 * @param nestingLevel the nesting level of the target type
 * (typically 1; e.g. in case of a List of Lists, 1 would indicate the
 * nested List, whereas 2 would indicate the element of the nested List)
 */
public MethodParameter(Constructor<?> constructor, int parameterIndex, int nestingLevel) {
    Assert.notNull(constructor, "Constructor must not be null");
    this.executable = constructor;
    // 设置构造方法参数位置
    this.parameterIndex = validateIndex(constructor, parameterIndex);
    this.nestingLevel = nestingLevel;
}
```



###### 3.1 从已解析的参数值对象获取参数值持有对象

```java
/**
 * 查找与构造函数参数列表中的给定索引对应或按类型一般匹配的参数值。
 *  
 * Look for an argument value that either corresponds to the given index
 * in the constructor argument list or generically matches by type.
 * @param index the index in the constructor argument list
 * @param requiredType the parameter type to match (can be {@code null}
 * to find an untyped argument value)
 * @param requiredName the parameter name to match (can be {@code null}
 * to find an unnamed argument value, or empty String to match any name)
 * @param usedValueHolders a Set of ValueHolder objects that have already
 * been used in the current resolution process and should therefore not
 * be returned again (allowing to return the next generic argument match
 * in case of multiple generic argument values of the same type)
 * @return the ValueHolder for the argument, or {@code null} if none set
 */
@Nullable
public ValueHolder getArgumentValue(int index, @Nullable Class<?> requiredType,
        @Nullable String requiredName, @Nullable Set<ValueHolder> usedValueHolders) {
    Assert.isTrue(index >= 0, "Index must not be negative");
    // 根据索引位置，参数类型，参数名称，获取参数值持有对象
    ValueHolder valueHolder = getIndexedArgumentValue(index, requiredType, requiredName);
    if (valueHolder == null) {
        // 如果参数值持有对象等于null，从通用的参数列表获取
        valueHolder = getGenericArgumentValue(requiredType, requiredName, usedValueHolders);
    }
    // 返回参数值持有对象
    return valueHolder;
}

/**
 * 查找与给定类型匹配的下一个泛型参数值，忽略在当前解析过程中已经使用的参数值。
 * 
 * Get argument value for the given index in the constructor argument list.
 * @param index the index in the constructor argument list
 * @param requiredType the type to match (can be {@code null} to match
 * untyped values only)
 * @param requiredName the type to match (can be {@code null} to match
 * unnamed values only, or empty String to match any name)
 * @return the ValueHolder for the argument, or {@code null} if none set
 */
@Nullable
public ValueHolder getIndexedArgumentValue(int index, @Nullable Class<?> requiredType, @Nullable String requiredName) {
    Assert.isTrue(index >= 0, "Index must not be negative");
    // 获取对应索引位置的参数值持有对象
    ValueHolder valueHolder = this.indexedArgumentValues.get(index);
    // 获取参数值持有对象的参数类型和名称，校验参数类型和名称是否匹配
    if (valueHolder != null &&
            (valueHolder.getType() == null || (requiredType != null &&
                    ClassUtils.matchesTypeName(requiredType, valueHolder.getType()))) &&
            (valueHolder.getName() == null || (requiredName != null &&
                    (requiredName.isEmpty() || requiredName.equals(valueHolder.getName()))))) {
        // 返回匹配的参数值持有对象
        return valueHolder;
    }
    // 不匹配返回null
    return null;
}

/**
 * 查找下一个与给定类型匹配的通用参数值，忽略在当前解析过程中已经使用的参数值。
 *
 * Look for the next generic argument value that matches the given type,
 * ignoring argument values that have already been used in the current
 * resolution process.
 * @param requiredType the type to match (can be {@code null} to find
 * an arbitrary next generic argument value)
 * @param requiredName the name to match (can be {@code null} to not
 * match argument values by name, or empty String to match any name)
 * @param usedValueHolders a Set of ValueHolder objects that have already been used
 * in the current resolution process and should therefore not be returned again
 * @return the ValueHolder for the argument, or {@code null} if none found
 */
@Nullable
public ValueHolder getGenericArgumentValue(@Nullable Class<?> requiredType, @Nullable String requiredName,
        @Nullable Set<ValueHolder> usedValueHolders) {
    // 遍历通用的参数列表获取，获取参数值持有对象的参数类型和名称，如果有的话，则校验是否匹配，返回匹配的参数值持有对象
    for (ValueHolder valueHolder : this.genericArgumentValues) {
        if (usedValueHolders != null && usedValueHolders.contains(valueHolder)) {
            continue;
        }
        if (valueHolder.getName() != null && (requiredName == null ||
                (!requiredName.isEmpty() && !requiredName.equals(valueHolder.getName())))) {
            continue;
        }
        if (valueHolder.getType() != null && (requiredType == null ||
                !ClassUtils.matchesTypeName(requiredType, valueHolder.getType()))) {
            continue;
        }
        if (requiredType != null && valueHolder.getType() == null && valueHolder.getName() == null &&
                !ClassUtils.isAssignableValue(requiredType, valueHolder.getValue())) {
            continue;
        }
        // 返回匹配的参数值持有对象
        return valueHolder;
    }
    // 匹配的参数值持有对象不存在，返回null
    return null;
}
```



###### 3.2 获取自动注入的构造方法参数值

```java
/**
 * 用于解析指定参数的模板方法，该参数应该是自动连接的。
 * Template method for resolving the specified argument which is supposed to be autowired.
 */
@Nullable
protected Object resolveAutowiredArgument(MethodParameter param, String beanName,
        @Nullable Set<String> autowiredBeanNames, TypeConverter typeConverter, boolean fallback) {
    // 获取方法或构造函数参数类型，这里我们获取的是构造函数的
    Class<?> paramType = param.getParameterType();
    if (InjectionPoint.class.isAssignableFrom(paramType)) {
        // 如果参数类型是InjectionPoint，返回当前注入点
        InjectionPoint injectionPoint = currentInjectionPoint.get();
        if (injectionPoint == null) {
            throw new IllegalStateException("No current InjectionPoint available for " + param);
        }
        return injectionPoint;
    }
    try {
        // 使用MethodParameter创建依赖描述器。
        // 解析依赖并返回。这里同属性注入的调用一样
        return this.beanFactory.resolveDependency(
                new DependencyDescriptor(param, true), beanName, autowiredBeanNames, typeConverter);
    }
    catch (NoUniqueBeanDefinitionException ex) {
        throw ex;
    }
    catch (NoSuchBeanDefinitionException ex) {
        // 如果回退
        if (fallback) {
            // 简单的构造方法或者工厂方法。返回一个空数组或集合
            // Single constructor or factory method -> let's return an empty array/collection
            // for e.g. a vararg or a non-null List/Set/Map parameter.
            if (paramType.isArray()) {
                return Array.newInstance(paramType.getComponentType(), 0);
            }
            else if (CollectionFactory.isApproximableCollectionType(paramType)) {
                return CollectionFactory.createCollection(paramType, 0);
            }
            else if (CollectionFactory.isApproximableMapType(paramType)) {
                return CollectionFactory.createMap(paramType, 0);
            }
        }
        throw ex;
    }
}

/**
 * 使用MethodParameter创建依赖描述器
 * Create a new descriptor for a method or constructor parameter.
 * Considers the dependency as 'eager'.
 * @param methodParameter the MethodParameter to wrap
 * @param required whether the dependency is required
 */
public DependencyDescriptor(MethodParameter methodParameter, boolean required) {
    this(methodParameter, required, true);
}

/**
 * Create a new descriptor for a method or constructor parameter.
 * @param methodParameter the MethodParameter to wrap
 * @param required whether the dependency is required
 * @param eager whether this dependency is 'eager' in the sense of
 * eagerly resolving potential target beans for type matching
 */
public DependencyDescriptor(MethodParameter methodParameter, boolean required, boolean eager) {
    super(methodParameter);

    this.declaringClass = methodParameter.getDeclaringClass();
    if (methodParameter.getMethod() != null) {
        this.methodName = methodParameter.getMethod().getName();
    }
    this.parameterTypes = methodParameter.getExecutable().getParameterTypes();
    this.parameterIndex = methodParameter.getParameterIndex();
    this.containingClass = methodParameter.getContainingClass();
    this.required = required;
    this.eager = eager;
}

//  super(methodParameter);
/**
 * Create an injection point descriptor for a method or constructor parameter.
 * @param methodParameter the MethodParameter to wrap
 */
public InjectionPoint(MethodParameter methodParameter) {
    Assert.notNull(methodParameter, "MethodParameter must not be null");
    this.methodParameter = methodParameter;
}
```

* 获取方法或构造函数参数类型

```java
/**
 * Return the type of the method/constructor parameter.
 * @return the parameter type (never {@code null})
 */
public Class<?> getParameterType() {
    // 获取参数类型。一开始参数类型为空
    Class<?> paramType = this.parameterType;
    if (paramType != null) {
        // 参数类型不为空，直接返回
        return paramType;
    }
    // 如果方法参数声明的类和定义的类不一样，对于构造方法的参数来说是一样的。
    if (getContainingClass() != getDeclaringClass()) {
        paramType = ResolvableType.forMethodParameter(this, null, 1).resolve();
    }
    if (paramType == null) {
        // 计算参数类型
        paramType = computeParameterType();
    }
    // 设置参数类型
    this.parameterType = paramType;
    // 返回参数类型
    return paramType;
}

// 计算参数类型
private Class<?> computeParameterType() {
    if (this.parameterIndex < 0) {
        Method method = getMethod();
        if (method == null) {
            return void.class;
        }
        if (KotlinDetector.isKotlinReflectPresent() && KotlinDetector.isKotlinType(getContainingClass())) {
            return KotlinDelegate.getReturnType(method);
        }
        return method.getReturnType();
    }
    // 根据参数类型的位置，获取方法参数类型。executable可以是Constructor或者Method
    return this.executable.getParameterTypes()[this.parameterIndex];
}

/**
 * 返回包含此方法参数的类。
 * Return the containing class for this method parameter.
 * @return a specific containing class (potentially a subclass of the
 * declaring class), or otherwise simply the declaring class itself
 * @see #getDeclaringClass()
 */
public Class<?> getContainingClass() {
    Class<?> containingClass = this.containingClass;
    return (containingClass != null ? containingClass : getDeclaringClass());
}

/**
 * 返回声明底层方法或构造函数的类。
 * Return the class that declares the underlying Method or Constructor.
 */
public Class<?> getDeclaringClass() {
    return this.executable.getDeclaringClass();
}
```



#####  4、获取类型差异的权重

ArgumentsHolder

```java
public int getTypeDifferenceWeight(Class<?>[] paramTypes) {
    // If valid arguments found, determine type difference weight.
    // Try type difference weight on both the converted arguments and
    // the raw arguments. If the raw weight is better, use it.
    // Decrease raw weight by 1024 to prefer it over equal converted weight.
    //如果找到有效参数，确定类型差异权重。
	//对转换后的参数和原始参数。如果原始的权重更好，就用它。
	//将原始权重降低1024以优先于相等的转换权重。
    int typeDiffWeight = MethodInvoker.getTypeDifferenceWeight(paramTypes, this.arguments);
    int rawTypeDiffWeight = MethodInvoker.getTypeDifferenceWeight(paramTypes, this.rawArguments) - 1024;
    return Math.min(rawTypeDiffWeight, typeDiffWeight);
}

/**
 判断候选方法的声明参数类型与调用该方法时应该使用的特定参数列表之间是否匹配的算法。
 
确定表示类型和参数之间的类层次结构差异的权重。直接匹配，即Integer类型→Integer类的arg，不会增加结果-所有直接匹配都意味着权重为0。Object类型和Integer类的arg之间的匹配将使权重增加2，因为在层次结构中往上走的超类2（即Object）是仍然匹配所需类型Object的最后一个。类型Number和类Integer将相应地增加权重1，因为超类1在层次结构（即Number）上一级仍然匹配所需的类型Number。因此，对于Integer类型的参数，构造函数（Integer）将优先于构造函数（Number），而构造函数（Number）又优先于构造函数（Object）。所有参数的权重都是累积的。

注意：这是MethodInvoker本身使用的算法，也是Spring的bean容器中用于构造函数和工厂方法选择的算法（在宽松的构造函数解析的情况下，这是常规bean定义的默认值）。

例如：Parent是Son的父类，只创建了Son的Bean。在创建AnimalServiceImpl的bean时，会选择 AnimalServiceImpl(Person person, House house, Son son) 这个构造方法来实例化。

    private Parent parent;
    @Autowired(required = false)
    public AnimalServiceImpl(Person person, House house, Parent parent) {
        this.person = person;
        this.house = house;
        this.parent = parent;
    }

    private  Son son;
    // 宽松模式，获取到匹配类型最近的
    @Autowired(required = false)
    public AnimalServiceImpl(Person person, House house, Son son) {
        this.person = person;
        this.house = house;
        this.son = son;
    }

    @Bean
    public Son son() {
        Son son = new Son();
        son.setName("Son");
        return son;
    }

 * Algorithm that judges the match between the declared parameter types of a candidate method
 * and a specific list of arguments that this method is supposed to be invoked with.
 * <p>Determines a weight that represents the class hierarchy difference between types and
 * arguments. A direct match, i.e. type Integer &rarr; arg of class Integer, does not increase
 * the result - all direct matches means weight 0. A match between type Object and arg of
 * class Integer would increase the weight by 2, due to the superclass 2 steps up in the
 * hierarchy (i.e. Object) being the last one that still matches the required type Object.
 * Type Number and class Integer would increase the weight by 1 accordingly, due to the
 * superclass 1 step up the hierarchy (i.e. Number) still matching the required type Number.
 * Therefore, with an arg of type Integer, a constructor (Integer) would be preferred to a
 * constructor (Number) which would in turn be preferred to a constructor (Object).
 * All argument weights get accumulated.
 * <p>Note: This is the algorithm used by MethodInvoker itself and also the algorithm
 * used for constructor and factory method selection in Spring's bean container (in case
 * of lenient constructor resolution which is the default for regular bean definitions).
 * @param paramTypes the parameter types to match
 * @param args the arguments to match
 * @return the accumulated weight for all arguments
 */
public static int getTypeDifferenceWeight(Class<?>[] paramTypes, Object[] args) {
    int result = 0;
    for (int i = 0; i < paramTypes.length; i++) {
        // 参数值不是paramType的类型及其子类，返回int最大值
        if (!ClassUtils.isAssignableValue(paramTypes[i], args[i])) {
            return Integer.MAX_VALUE;
        }
        if (args[i] != null) {
            Class<?> paramType = paramTypes[i];
            Class<?> superClass = args[i].getClass().getSuperclass();
            while (superClass != null) {
                if (paramType.equals(superClass)) {
                    // 参数类型等于父类，result + 2
                    result = result + 2;
                    superClass = null;
                }
                else if (ClassUtils.isAssignable(paramType, superClass)) {
                    // superClass是paramType的类型及其子类，result + 2
                    result = result + 2;
                    // 获取父类的父类
                    superClass = superClass.getSuperclass();
                }
                else {
                    superClass = null;
                }
            }            
            if (paramType.isInterface()) {
                result = result + 1;
            }
        }
    }
    return result;
}
```



##### 5、解析准备好的参数(缓存)

```java
/**
 * 解析存储在给定bean定义中的准备好的参数
 * Resolve the prepared arguments stored in the given bean definition.
 */
private Object[] resolvePreparedArguments(String beanName, RootBeanDefinition mbd, BeanWrapper bw,
        Executable executable, Object[] argsToResolve) {
    // 获取beanFactory自定义的类型转换器
    TypeConverter customConverter = this.beanFactory.getCustomTypeConverter();
    // 设置类型转换器
    TypeConverter converter = (customConverter != null ? customConverter : bw);
    // 创建Bean定义值解析器。一般用于@Value注解解析
    BeanDefinitionValueResolver valueResolver =
            new BeanDefinitionValueResolver(this.beanFactory, beanName, mbd, converter);
    // 获取(所有的)参数类型
    Class<?>[] paramTypes = executable.getParameterTypes();
    
    // 创建数组保存解析的参数值
    Object[] resolvedArgs = new Object[argsToResolve.length];
    // 遍历所有准备好的参数
    for (int argIndex = 0; argIndex < argsToResolve.length; argIndex++) {
        // 获取准备好的参数
        Object argValue = argsToResolve[argIndex];
        // 创建MethodParameter
        MethodParameter methodParam = MethodParameter.forExecutable(executable, argIndex);
        // 如果参数值等于自动注入标识。
        if (argValue == autowiredArgumentMarker) {
            // 因为在@Autowired的构造方法中，自动注入的参数解析后得到的所有“准备好的参数”都是autowiredArgumentMarker
          
            // 解析自动注入的值作为参数的值
            // 同 【3.2 获取自动注入的构造方法参数值】
            argValue = resolveAutowiredArgument(methodParam, beanName, null, converter, true);
        }
        else if (argValue instanceof BeanMetadataElement) {
            // 如果参数值是BeanMetadataElement类型，例如RuntimeBeanReference
            
            // 使用Bean定义值解析器获取具体的值，如果有必要的话
            argValue = valueResolver.resolveValueIfNecessary("constructor argument", argValue);
        }
        else if (argValue instanceof String) {
            // 计算bean定义中包含的给定String，可能将其解析为表达式
            argValue = this.beanFactory.evaluateBeanDefinitionString((String) argValue, mbd);
        }
        // 获取参数类型
        Class<?> paramType = paramTypes[argIndex];
        try {
            // 如果有必要的话，将参数值进行类型转换，然后保存
            resolvedArgs[argIndex] = converter.convertIfNecessary(argValue, paramType, methodParam);
        }
        catch (TypeMismatchException ex) {
            throw new UnsatisfiedDependencyException(
                    mbd.getResourceDescription(), beanName, new InjectionPoint(methodParam),
                    "Could not convert argument value of type [" + ObjectUtils.nullSafeClassName(argValue) +
                    "] to required type [" + paramType.getName() + "]: " + ex.getMessage());
        }
    }
    // 返回所有已解析的参数值
    return resolvedArgs;
}

/**
 * 缓存参数数组中自动连接参数的标记，将被解析的自动连接参数替换。
 * 
 * 自动注入标识。如果preparedArguments有该标识，说明是要解析依赖的
 * Marker for autowired arguments in a cached argument array, to be replaced
 * by a {@linkplain #resolveAutowiredArgument resolved autowired argument}.
 */
private static final Object autowiredArgumentMarker = new Object();
```



### 工厂方法(@Bean方法)注入

* 结论

```
1、对于同一个bean，如果存在参数个数相同，类型不同的重载工厂方法，实例化会报错
2、对于同一个bean，如果存在参数个数、类型相同的，但是名称不一样的工厂方法，则采用最先定义的
3、对于同一个bean，如果存在参数个数不同的重载工厂方法，则采用参数最多的public工厂方法
4、对于同一个bean，如果是以下情况一起出现 
（1）存在参数个数不同的重载工厂方法
（2）参数个数、类型相同的，但是名称不一样的工厂方法
 则采用最先定义的参数最多的public工厂方法
```

* 具体情况

（1）对于同一个bean（这里beanName=city），如果存在参数个数相同，类型不同的重载工厂方法，实例化会报错

```java
//throw new BeanCreationException(mbd.getResourceDescription(), beanName,
//                "Ambiguous factory method matches found on class [" + factoryClass.getName() + "] " +
//                        "(hint: specify index/type/name arguments for simple parameters to avoid type ambiguities): " +
//ambiguousFactoryMethods);
@Bean
public City city(House house, Person person, Parent parent) {
    City city = new City();
    city.setPerson(person);
    city.setHouse(house);
    city.setParent(parent);
    return city;
}

@Bean
public City city(House house, Person person, Son son) {
    City city = new City();
    city.setPerson(person);
    city.setHouse(house);
    city.setParent(son);
    return city;
}
```

2、对于同一个bean（这里beanName=city），如果存在参数个数、类型相同的，但是名称不一样的工厂方法，则采用最先定义的

```java
// 情况二：使用c1方法实例化。factoryMethodName=c1，c1方法先定义
@Bean(value="city")
public City c1(House house, Person person, Son son) {
    City city = new City();
    city.setPerson(person);
    city.setHouse(house);
    city.setParent(son);
    return city;
}

@Bean(value="city")
public City c2(House house, Person person, Son son) {
    City city = new City();
    city.setPerson(person);
    city.setHouse(house);
    city.setParent(son);
    return city;
}
```

3、对于同一个bean（这里beanName=city），如果存在参数个数不同的重载工厂方法，则采用参数最多的public工厂方法

```java
// 采用参数最多的public工厂方法实例化，即该方法
@Bean(value="city")
public City c1(House house, Person person, Son son) {
    City city = new City();
    city.setPerson(person);
    city.setHouse(house);
    city.setParent(son);
    return city;
}

@Bean(value="city")
public City c1(House house, Person person) {
    City city = new City();
    city.setPerson(person);
    city.setHouse(house);
    return city;
}
```

4、对于同一个bean，如果是以下情况一起出现 
（1）存在参数个数不同的重载工厂方法
（2）参数个数、类型相同的，但是名称不一样的工厂方法
 则采用最先定义的参数最多的public工厂方法

```java
// 使用c2方法作为候选方法。因为c2方法先定义，所以factoryMethodName=c2。
// 最终选择参数最多的c2方法实例化，即c2(House house, Person person, Son son) 方法
@Bean(value="city")
public City c2(House house, Person person) {
    City city = new City();
    city.setPerson(person);
    city.setHouse(house);
    return city;
}

// 使用该方法实例化
@Bean(value="city")
public City c2(House house, Person person, Son son) {
    City city = new City();
    city.setPerson(person);
    city.setHouse(house);
    city.setParent(son);
    return city;
}

@Bean(value="city")
public City c1(House house, Person person, Son son) {
    City city = new City();
    city.setPerson(person);
    city.setHouse(house);
    city.setParent(son);
    return city;
}

@Bean(value="city")
public City c1(House house, Person person) {
    City city = new City();
    city.setPerson(person);
    city.setHouse(house);
    return city;
}
```



从构造方法注入可以知道，工厂方法实例化和参数注入过程的代码位于以下位置。

```java
org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#instantiateUsingFactoryMethod(String, RootBeanDefinition, Object[])
org.springframework.beans.factory.support.ConstructorResolver#instantiateUsingFactoryMethod(String, RootBeanDefinition, Object[])
```



说明：工厂方法注入和构造方法注入的逻辑实际是差不多的，可以类比理解。

```java
/**
 * 使用命名工厂方法实例化bean
 * Instantiate the bean using a named factory method. The method may be static, if the
 * mbd parameter specifies a class, rather than a factoryBean, or an instance variable
 * on a factory object itself configured using Dependency Injection.
 * @param beanName the name of the bean
 * @param mbd the bean definition for the bean
 * @param explicitArgs argument values passed in programmatically via the getBean method,
 * or {@code null} if none (implying the use of constructor argument values from bean definition)
 * @return a BeanWrapper for the new instance
 * @see #getBean(String, Object[])
 */
protected BeanWrapper instantiateUsingFactoryMethod(
        String beanName, RootBeanDefinition mbd, @Nullable Object[] explicitArgs) {
    return new ConstructorResolver(this).instantiateUsingFactoryMethod(beanName, mbd, explicitArgs);
}
```

```java
/**
 * 使用命名工厂方法实例化bean
 * Instantiate the bean using a named factory method. The method may be static, if the
 * bean definition parameter specifies a class, rather than a "factory-bean", or
 * an instance variable on a factory object itself configured using Dependency Injection.
 * <p>Implementation requires iterating over the static or instance methods with the
 * name specified in the RootBeanDefinition (the method may be overloaded) and trying
 * to match with the parameters. We don't have the types attached to constructor args,
 * so trial and error is the only way to go here. The explicitArgs array may contain
 * argument values passed in programmatically via the corresponding getBean method.
 * @param beanName the name of the bean
 * @param mbd the merged bean definition for the bean
 * @param explicitArgs argument values passed in programmatically via the getBean
 * method, or {@code null} if none (-> use constructor argument values from bean definition)
 * @return a BeanWrapper for the new instance
 */
public BeanWrapper instantiateUsingFactoryMethod(
        String beanName, RootBeanDefinition mbd, @Nullable Object[] explicitArgs) {
    // 创建bean实例包装对象
    BeanWrapperImpl bw = new BeanWrapperImpl();
    // 初始化bean实例包装对象
    this.beanFactory.initBeanWrapper(bw);

    // 工厂bean实例
    Object factoryBean;
    // 工厂bean的Class
    Class<?> factoryClass;
    // 工厂方法是否是静态
    boolean isStatic;    
    
    /**
    例如：    
    @Configuration
    public class Config {
        @Bean
        public City city(House house, Person person) {
            City city = new City();
            city.setPerson(person);
            city.setHouse(house);
            return city;
        }
    }
    工厂bean实例factoryBean是Config实例，factoryClass=Config.class，isStatic=false
    工厂bean名称factoryBeanName=config    
    */

    // 获取工厂bean名称
    String factoryBeanName = mbd.getFactoryBeanName();
    if (factoryBeanName != null) {
        // 工厂bean名称不等于null        
        // 如果工厂bean名称等于beanName，则抛出异常。
        /**
        // 工厂bean名称等于beanName
        @Configuration
        public class Config {
            //工厂bean不能注入工厂自身。这种情况是不允许的，会报错
            @Bean
            public Config config() {
            	Config config = new Config();
            	return config;
            }
        }    
    	*/     
        if (factoryBeanName.equals(beanName)) {
            throw new BeanDefinitionStoreException(mbd.getResourceDescription(), beanName,
                    "factory-bean reference points back to the same bean definition");
        }
        
        // 根据工厂bean名称获取工厂bean实例
        factoryBean = this.beanFactory.getBean(factoryBeanName);
        // 如果bean定义单实例的，beanFactory已经包含了beanName的实例，说明已创建过了，则抛出异常
        if (mbd.isSingleton() && this.beanFactory.containsSingleton(beanName)) {
            throw new ImplicitlyAppearedSingletonException();
        }
        // 注册依赖关系。（记录factoryBeanName依赖beanName，同时记录beanName被factoryBeanName依赖了）
        this.beanFactory.registerDependentBean(factoryBeanName, beanName);
        // 获取工厂bean的Class
        factoryClass = factoryBean.getClass();
        // 设置工厂方法不是静态的
        isStatic = false;
    }
    else {
        // 工厂bean名称等于null          
        // 说明是静态的工厂方法在beanClass中
        // It's a static factory method on the bean class.
        // 如果bean定义没有beanClass，则抛出异常
        if (!mbd.hasBeanClass()) {
            throw new BeanDefinitionStoreException(mbd.getResourceDescription(), beanName,
                    "bean definition declares neither a bean class nor a factory-bean reference");
        }
        // 设置工厂bean为null
        factoryBean = null;
        // 从bean定义获取beanClass作为工厂bean的Class
        factoryClass = mbd.getBeanClass();
        // 设置工厂方法是静态的
        isStatic = true;
    }

    // 使用的工厂方法
    Method factoryMethodToUse = null;
    // 使用的参数持有对象
    ArgumentsHolder argsHolderToUse = null;
    //  使用的参数
    Object[] argsToUse = null;

    if (explicitArgs != null) {
        // 如果明确的参数不等于null，将明确的参数作为使用的参数
        argsToUse = explicitArgs;
    }
    else {
        // 需要解析的参数
        Object[] argsToResolve = null;
        
        // 先检查是否有缓存（这里的逻辑实际和构造方法注入是一样的）
        synchronized (mbd.constructorArgumentLock) {
            // 从bean定义获取已解析的工厂方法
            factoryMethodToUse = (Method) mbd.resolvedConstructorOrFactoryMethod;
            // 如果使用的工厂方法不等于null，而且参数已解析
            if (factoryMethodToUse != null && mbd.constructorArgumentsResolved) {
                // Found a cached factory method...
                // 执行到这里说明找到缓存的工厂方法
                
                // 获取已解析的参数作为使用的参数
                argsToUse = mbd.resolvedConstructorArguments;
                if (argsToUse == null) {
                    // 使用的参数等于null                    
                    // 获取“准备好的参数”作为需要解析的参数
                    argsToResolve = mbd.preparedConstructorArguments;
                }
            }
        }
        // 如果需要解析的参数不为空，从中获取使用的参数
        if (argsToResolve != null) {
            // 这里的逻辑实际和构造方法注入是一样的
            argsToUse = resolvePreparedArguments(beanName, mbd, bw, factoryMethodToUse, argsToResolve);
        }
    }

    // 如果使用的工厂方法或使用的参数等于null
    if (factoryMethodToUse == null || argsToUse == null) {
        // Need to determine the factory method...
        // Try all methods with this name to see if they match the given arguments.
        //需要确定工厂方法…
        //尝试所有带有此名称的方法，看看它们是否与给定的参数匹配。
        
        // 获取用户定义的类作为factoryClass（如果是CGLB代理的类，则获取到父类，反之获取到自身）
        factoryClass = ClassUtils.getUserClass(factoryClass);

        // 获取候选的工厂方法
        List<Method> candidates = null;
        // bean定义是否存在唯一的工厂方法
        if (mbd.isFactoryMethodUnique) {
            // 不存在唯一的工厂方法
            
            // 使用的工厂方法为null
            if (factoryMethodToUse == null) {
                // 从bean定义获取已解析的工厂方法作为使用的工厂方法
                factoryMethodToUse = mbd.getResolvedFactoryMethod();
            }
            if (factoryMethodToUse != null) {
                // 使用的工厂方法不为null，保存到候选的工厂方法
                candidates = Collections.singletonList(factoryMethodToUse);
            }
        }
        // 候选的工厂方法不存在
        if (candidates == null) {
            candidates = new ArrayList<>();
            // 根据factoryClass，mbd获取候选的方法
            Method[] rawCandidates = getCandidateMethods(factoryClass, mbd);
            // 筛选出匹配bean定义的工厂方法作为候选的工厂方法
            for (Method candidate : rawCandidates) {                   
                // 判断候选方法是否为工厂方法  
                if (Modifier.isStatic(candidate.getModifiers()) == isStatic && mbd.isFactoryMethod(candidate)) {
                    candidates.add(candidate);
                }
            }
        }

        // 调用无参的工厂方法实例化
        // 只有一个候选的工厂方法，而且没有明确的参数，bean定义里没有指定工厂方法的参数值
        if (candidates.size() == 1 && explicitArgs == null && !mbd.hasConstructorArgumentValues()) {
            // 获取唯一的候选工厂方法
            Method uniqueCandidate = candidates.get(0);            
            if (uniqueCandidate.getParameterCount() == 0) {
                // 如果唯一的候选工厂方法的方法参数个数为0（说明不需要解析自动注入的参数，直接调用即可）
                
                // 以下操作，相当于设置缓存
                // 设置factoryMethodToIntrospect为uniqueCandidate
                mbd.factoryMethodToIntrospect = uniqueCandidate;
                synchronized (mbd.constructorArgumentLock) {
                    // 设置已解析的工厂方法为uniqueCandidate
                    mbd.resolvedConstructorOrFactoryMethod = uniqueCandidate;
                    // 设置工厂方法参数为已解析
                    mbd.constructorArgumentsResolved = true;
                    // 设置已解析的工厂方法参数值为空数组
                    mbd.resolvedConstructorArguments = EMPTY_ARGS;
                }
                // 直接调用无参的工厂方法进行实例化并保存到bean包装对象
                bw.setBeanInstance(instantiate(beanName, mbd, factoryBean, uniqueCandidate, EMPTY_ARGS));
                // 返回bean包装对象
                return bw;
            }
        }

        // 1、候选的工厂方法排序（如果超过一个）
        if (candidates.size() > 1) {  // explicitly skip immutable singletonList
            candidates.sort(AutowireUtils.EXECUTABLE_COMPARATOR);
        }

        // 解析的参数值
        ConstructorArgumentValues resolvedValues = null;
        // 是否是自动注入。如果是工厂方法的bean定义的话，这里获取true。
        boolean autowiring = (mbd.getResolvedAutowireMode() == AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR);
        // 设置最小类型差异的权重为int的最大值
        int minTypeDiffWeight = Integer.MAX_VALUE;
        // 创建集合保存“类型差异值等于最小类型差异的权重”的工厂方法
        // ambiguousFactoryMethods表示是否有模棱两可的工厂方法，即无法确定使用哪个工厂方法
        Set<Method> ambiguousFactoryMethods = null;
		       
        // 最小参数个数
        int minNrOfArgs;
        if (explicitArgs != null) {
            // 最小的参数个数为明确的参数个数
            minNrOfArgs = explicitArgs.length;
        }
        else {
            // We don't have arguments passed in programmatically, so we need to resolve the
            // arguments specified in the constructor arguments held in the bean definition.
            //我们没有通过编程方式传入的参数，所以我们需要解析保存在bean定义的构造函数参数中指定的参数值。
            if (mbd.hasConstructorArgumentValues()) {
 				/**
 				什么时候会执行到这里呢？？？ 				
 				见下面【手动注册bean定义，指定工厂方法和工厂方法的参数值】例子				
 				*/                                                                
                 // 从bean定义获取指定的参数值
                ConstructorArgumentValues cargs = mbd.getConstructorArgumentValues();
                 // 创建工厂方法参数值对象
                resolvedValues = new ConstructorArgumentValues();                                
                // 从bean定义获取指定的参数值，解析保存到resolvedValues。可参考构造方法注入
                // minNrOfArgs为指定的参数个数 
                minNrOfArgs = resolveConstructorArguments(beanName, mbd, bw, cargs, resolvedValues);
            }
            else {
                // bean定义没有指定工厂方法使用的参数，设置最小参数个数为0。一般没有设置
                minNrOfArgs = 0;
            }
        }

        // 创建队列记录工厂方法参数依赖注入失败的异常
        Deque<UnsatisfiedDependencyException> causes = null;

        // 遍历候选的工厂方法，找到使用的工厂方法和参数
        for (Method candidate : candidates) {
           // 获取候选工厂方法参数的个数
            int parameterCount = candidate.getParameterCount();

            // 如果候选工厂方法参数的个数大于或等于最小的参数个数（小于的话，说明不符合）
            if (parameterCount >= minNrOfArgs) {
                // 参数值持有对象
                ArgumentsHolder argsHolder;
			   // 获取工厂方法参数类型
                Class<?>[] paramTypes = candidate.getParameterTypes();
                if (explicitArgs != null) {
                    // 明确的参数不为空                    
                    // 给出显式参数->参数长度必须完全匹配。
                    // Explicit arguments given -> arguments length must match exactly.
                    if (paramTypes.length != explicitArgs.length) {
                        continue;
                    }
                    // 创建参数值持有对象，设置明确的参数值
                    argsHolder = new ArgumentsHolder(explicitArgs);
                }
                else {
                    // 解析构造函数(这里说工厂方法，更贴切)参数：类型转换和/或自动装配是必需的。
                    // Resolved constructor arguments: type conversion and/or autowiring necessary.
                    try {                        
                        // 工厂方法参数名称                        
                        String[] paramNames = null;
                        // 获取参数名称发现器
                        ParameterNameDiscoverer pnd = this.beanFactory.getParameterNameDiscoverer();
                        if (pnd != null) {
                            // 使用参数名称发现器获取工厂方法所有的参数名称  
                            paramNames = pnd.getParameterNames(candidate);
                        }
                        // 2、获取工厂方法参数值（包装到持有对象）
                        // 【注意】这里的逻辑同构造方法注入的 【3、获取构造方法参数值】是一样的
                        argsHolder = createArgumentArray(beanName, mbd, resolvedValues, bw,
                                paramTypes, paramNames, candidate, autowiring, candidates.size() == 1);
                    }
                    catch (UnsatisfiedDependencyException ex) {
                         // 工厂方法参数依赖注入失败，记录异常，跳过当前候选的构造器，执行下一个
                        if (logger.isTraceEnabled()) {
                            logger.trace("Ignoring factory method [" + candidate + "] of bean '" + beanName + "': " + ex);
                        }
                        // Swallow and try next overloaded factory method.
                        if (causes == null) {
                            causes = new ArrayDeque<>(1);
                        }
                        causes.add(ex);
                        continue;
                    }
                }
                
  		        // 3、获取类型差异的权重。
                // isLenientConstructorResolution:返回以宽松模式还是严格模式解析工厂方法参数类型差异权重
                // 工厂方法使用严格模式来获取类型差异权重，和构造方法注入的不一样。
                int typeDiffWeight = (mbd.isLenientConstructorResolution() ?
                        argsHolder.getTypeDifferenceWeight(paramTypes) : argsHolder.getAssignabilityWeight(paramTypes));
                // 如果它表示最接近的匹配，则选择此工厂方法。
                // Choose this factory method if it represents the closest match.
                if (typeDiffWeight < minTypeDiffWeight) {
                    // 如果类型差异权重小于最小类型差异权重     
                    // 设置候选的工厂方法为使用的工厂方法
                    factoryMethodToUse = candidate;
                    // 设置使用的工厂方法参数持有对象
                    argsHolderToUse = argsHolder;
                    // 设置使用的工厂方法参数
                    argsToUse = argsHolder.arguments;
                    // 设置当前参数值的类型差异权重为最小类型差异权重
                    minTypeDiffWeight = typeDiffWeight;
                    // 设置ambiguousConstructors为空
                    ambiguousFactoryMethods = null;
                }
                // Find out about ambiguity: In case of the same type difference weight
                // for methods with the same number of parameters, collect such candidates
                // and eventually raise an ambiguity exception.
                // However, only perform that check in non-lenient constructor resolution mode,
                // and explicitly ignore overridden methods (with the same parameter signature).
                //发现歧义：如果相同类型的权重不同
                //对于具有相同数量参数的方法，收集这些候选参数
                //并最终引发歧义异常。
                //但是，只在非宽松的构造函数解析模式下执行该检查，
                //并显式忽略被覆盖的方法（具有相同的参数签名）。
                
                // 使用的工厂方法不等于null，类型差异权重等于最小类型差异权重，采用严格模式解析工厂方法参数类型差异权重
                // 参数类型个数等于“使用的工厂方法参数个数”，参数类型不等于“使用的工厂方法参数类型”
                else if (factoryMethodToUse != null && typeDiffWeight == minTypeDiffWeight &&
                        !mbd.isLenientConstructorResolution() &&
                        paramTypes.length == factoryMethodToUse.getParameterCount() &&
                        !Arrays.equals(paramTypes, factoryMethodToUse.getParameterTypes())) {
                    // 执行到这里说明有模棱两可的工厂方法，即无法确定使用哪个工厂方法，最后在下面的判断会抛出异常
                    if (ambiguousFactoryMethods == null) {
                        // 初始化ambiguousConstructors
                        ambiguousFactoryMethods = new LinkedHashSet<>();
                        // 添加使用的工厂方法到ambiguousConstructors
                        ambiguousFactoryMethods.add(factoryMethodToUse);
                    }
                     // 添加候选的工厂方法到ambiguousConstructors
                    ambiguousFactoryMethods.add(candidate);
                }
            }
        }

        // 经过上面的解析，如果使用的工厂方法或使用的参数等于null，则抛出异常
        if (factoryMethodToUse == null || argsToUse == null) {
            if (causes != null) {
                // 抛出方法参数依赖注入失败的异常
                UnsatisfiedDependencyException ex = causes.removeLast();
                for (Exception cause : causes) {
                    this.beanFactory.onSuppressedException(cause);
                }
                throw ex;
            }
            List<String> argTypes = new ArrayList<>(minNrOfArgs);
            if (explicitArgs != null) {
                for (Object arg : explicitArgs) {
                    argTypes.add(arg != null ? arg.getClass().getSimpleName() : "null");
                }
            }
            else if (resolvedValues != null) {
                Set<ValueHolder> valueHolders = new LinkedHashSet<>(resolvedValues.getArgumentCount());
                valueHolders.addAll(resolvedValues.getIndexedArgumentValues().values());
                valueHolders.addAll(resolvedValues.getGenericArgumentValues());
                for (ValueHolder value : valueHolders) {
                    String argType = (value.getType() != null ? ClassUtils.getShortName(value.getType()) :
                            (value.getValue() != null ? value.getValue().getClass().getSimpleName() : "null"));
                    argTypes.add(argType);
                }
            }
            // 抛出异常
            String argDesc = StringUtils.collectionToCommaDelimitedString(argTypes);
            throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                    "No matching factory method found on class [" + factoryClass.getName() + "]: " +
                    (mbd.getFactoryBeanName() != null ?
                        "factory bean '" + mbd.getFactoryBeanName() + "'; " : "") +
                    "factory method '" + mbd.getFactoryMethodName() + "(" + argDesc + ")'. " +
                    "Check that a method with the specified name " +
                    (minNrOfArgs > 0 ? "and arguments " : "") +
                    "exists and that it is " +
                    (isStatic ? "static" : "non-static") + ".");
        }
        // 如果工厂方法返回值类型为void，则抛出异常
        else if (void.class == factoryMethodToUse.getReturnType()) {
            throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                    "Invalid factory method '" + mbd.getFactoryMethodName() + "' on class [" +
                    factoryClass.getName() + "]: needs to have a non-void return type!");
        }
        // ambiguousFactoryMethods方法不等于null，则抛出异常。
        // 说明有多个工厂方法，不确定使用哪个
        else if (ambiguousFactoryMethods != null) {
            throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                    "Ambiguous factory method matches found on class [" + factoryClass.getName() + "] " +
                    "(hint: specify index/type/name arguments for simple parameters to avoid type ambiguities): " +
                    ambiguousFactoryMethods);
        }

        // 明确的参数等于null，使用的工厂方法参数持有器不等于null，而缓存使用的工厂方法和工厂方法参数
        if (explicitArgs == null && argsHolderToUse != null) {
            // 设置factoryMethodToIntrospect
            mbd.factoryMethodToIntrospect = factoryMethodToUse;
            // 存储缓存
            argsHolderToUse.storeCache(mbd, factoryMethodToUse);
        }
    }
    
    // 使用工厂方法，工厂方法参数，工厂bean实例化，将bean实例保存到bean包装对象
    bw.setBeanInstance(instantiate(beanName, mbd, factoryBean, factoryMethodToUse, argsToUse));
    // 返回bean包装对象
    return bw;
}

// 存储缓存
public void storeCache(RootBeanDefinition mbd, Executable constructorOrFactoryMethod) {
    synchronized (mbd.constructorArgumentLock) {
        // 设置已解析的工厂方法
        mbd.resolvedConstructorOrFactoryMethod = constructorOrFactoryMethod;
        // 设置工厂方法参数为已解析
        mbd.constructorArgumentsResolved = true;
	    // 如果需要解析。默认为false。如果是自动注入，resolveNecessary则会被设置为true          
        if (this.resolveNecessary) {              
            // 设置准备好工厂方法参数。
            // 如果是自动注入的话，preparedArguments是存有一个Object常量（即autowiredArgumentMarker）的数组。           
            mbd.preparedConstructorArguments = this.preparedArguments;
        }
        else {
            // 设置已解析的工厂方法参数
            mbd.resolvedConstructorArguments = this.arguments;
        }
    }
}

/**
 * 为指定的类获取用户定义的类
 * Return the user-defined class for the given class: usually simply the given
 * class, but the original class in case of a CGLIB-generated subclass.
 * @param clazz the class to check
 * @return the user-defined class
 */
public static Class<?> getUserClass(Class<?> clazz) {    
    if (clazz.getName().contains(CGLIB_CLASS_SEPARATOR)) {
        // 如果是CGLB代理类。获取父类
        Class<?> superclass = clazz.getSuperclass();
        // 如果父类不等于Object.class，则返回父类。
        if (superclass != null && superclass != Object.class) {
            return superclass;
        }
    }
    return clazz;
}
/** The CGLIB class separator: {@code "$$"}. */
public static final String CGLIB_CLASS_SEPARATOR = "$$";

// 空数组，表示参数为空
private static final Object[] EMPTY_ARGS = new Object[0];

/**
 * 自动注入的缓存标识
 * Marker for autowired arguments in a cached argument array, to be replaced
 * by a {@linkplain #resolveAutowiredArgument resolved autowired argument}.
 */
private static final Object autowiredArgumentMarker = new Object();
```

#### 大致过程

![工厂方法注入大致过程](https://lu-note.oss-cn-shenzhen.aliyuncs.com/notes/work/%E5%B7%A5%E5%8E%82%E6%96%B9%E6%B3%95%E6%B3%A8%E5%85%A5%E5%A4%A7%E8%87%B4%E8%BF%87%E7%A8%8B.jpg)



* 手动注册bean定义，指定工厂方法和工厂方法的参数值例子

```java
@Component
public class CustomRegister implements BeanDefinitionRegistryPostProcessor {

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        System.out.println("===================================开始手动注册bean================================="); 
        // 使用工厂方法创建Village。指定工厂方法的参数
        Class<Village> villageClass = Village.class;
        BeanDefinitionBuilder villageDefinition = BeanDefinitionBuilder.genericBeanDefinition(villageClass);
        // 设置工厂方法名称和工厂bean名称
        String factoryMethod = "village";
        villageDefinition.setFactoryMethodOnBean(factoryMethod, getBeanName(VillageConfig.class));
        // 指定工厂方法的三个参数
        ConstructorArgumentValues villageArgs = villageDefinition.getBeanDefinition().getConstructorArgumentValues();
        villageArgs.addIndexedArgumentValue(0, "梅里雪山");
        Tree huYang = new Tree();
        huYang.setName("胡杨");
        villageArgs.addIndexedArgumentValue(1, huYang);
        Bird maQue = new Bird();
        maQue.setName("麻雀");
        villageArgs.addIndexedArgumentValue(2, maQue);
        registry.registerBeanDefinition(getBeanName(villageClass), villageDefinition.getBeanDefinition());


        // 使用工厂方法创建Sky。指定工厂方法的参数
        Class<Sky> skyClass = Sky.class;
        BeanDefinitionBuilder skyBuilder = BeanDefinitionBuilder.genericBeanDefinition(skyClass);
        String skyFactoryMethod = "sky";
        skyBuilder.setFactoryMethodOnBean(skyFactoryMethod, getBeanName(SkyConfig.class));
        // 指定工厂方法的三个参数
        AbstractBeanDefinition skyBeanDefinition = skyBuilder.getBeanDefinition();
        ConstructorArgumentValues skyArgs = skyBeanDefinition.getConstructorArgumentValues();
        skyArgs.addIndexedArgumentValue(0, 666);
        Colour colour = new Colour();
        colour.setName("蓝色");
        skyArgs.addIndexedArgumentValue(1, colour);
        Bird dayan = new Bird();
        dayan.setName("大雁");
        skyArgs.addIndexedArgumentValue(2, dayan);
        // 对没有指定的参数值，使用构造方法参数注入
        skyBuilder.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);
        registry.registerBeanDefinition(getBeanName(skyClass), skyBeanDefinition);
        System.out.println("===================================手动注册bean完成=================================");
    }

    public static String getBeanName(Class<?> beanClass) {
        return toLowerCamelCase(beanClass.getSimpleName());
    }

    /**
     * 将类名的首字母转换为小写
     * @param className 类名
     * @return 转换后的类名
     */
    public static String toLowerCamelCase(String className) {
        if (className == null || className.isEmpty()) {
            return className; // 如果输入为空，返回空字符串
        }
        return className.substring(0, 1).toLowerCase() + className.substring(1);
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

    }
}

@Data
public class Village {

    private String name;

    private Tree tree;

    private Bird bird;
}

@Data
public class Table {

    public Table() {
    }

    public Table(Desk desk) {
        this.desk = desk;
    }

    private Desk desk;

}

@Data
@ToString
public class Desk {
    private String brand;
}

@Data
public class Bird {
    private String name;
}

@Data
@ToString
public class Sky implements InitializingBean {
    private Integer number;
    private Colour colour;
    private Bird bird;
    private Cloud cloud;

    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println(this);
    }
}

@Data
public class Colour {
    private String name;
}

@Data
public class Cloud {
    private String name;
}
```



```java
@Configuration
public class VillageConfig {

    /**
     * Village village.
     *
     * 最终使用这个方法实例化。
     * @see com.mrlu.register.CustomRegister
     *
     * @param name the name
     * @param tree the tree
     * @param bird the bird
     * @return the village
     */
    public Village village(String name, Tree tree, Bird bird) {
        Village village = new Village();
        village.setName(name);
        village.setTree(tree);
        village.setBird(bird);
        System.out.println("使用village(String name, Tree tree, Bird bird)工厂方法");
        return village;
    }

    /**
     * Village village.
     *
     * @param name the name
     * @param tree the tree
     * @return the village
     */
    public Village village(String name, Tree tree) {
        Village village = new Village();
        village.setName(name);
        village.setTree(tree);
        System.out.println("使用village(String name, Tree tree)工厂方法");
        return village;
    }

}
```



```java
/**
 * The type Sky config.
 *
 * @author 简单de快乐
 * @create 2024 -12-30 20:12
 *
 * 在CustomRegister里注册了Sky的工厂方法
 */
@Configuration
public class SkyConfig {

    /**
     * Sky sky.
     *
     * @return the sky
     */
    public Sky sky() {
        return new Sky();
    }

    /**
     * Sky sky.
     *
     * @param number the number
     * @return the sky
     */
    public Sky sky(Integer number) {
        Sky sky = new Sky();
        sky.setNumber(number);
        return sky;
    }

    /**
     * Sky sky.
     *
     * @param number the number
     * @param colour the colour
     * @return the sky
     */
    public Sky sky(Integer number, Colour colour) {
        Sky sky = new Sky();
        sky.setNumber(number);
        sky.setColour(colour);
        return sky;
    }

    /**
     * Sky sky.
     *
     * @param number the number
     * @param colour the colour
     * @param bird   the bird
     * @return the sky
     */
    public Sky sky(Integer number, Colour colour, Bird bird) {
        Sky sky = new Sky();
        sky.setNumber(number);
        sky.setColour(colour);
        sky.setBird(bird);
        return sky;
    }

    /**
     * Sky sky.
     * 最终使用这个工厂方法实例化。
     * 前三个参数从bean定义指定的工厂方法参数获取，第四个参数从bean工厂进行依赖注入
     *
     * @param number the number
     * @param colour the colour
     * @param bird   the bird
     * @param cloud  the cloud
     * @return the sky
     */
    public Sky sky(Integer number, Colour colour, Bird bird, Cloud cloud) {
        Sky sky = new Sky();
        sky.setNumber(number);
        sky.setColour(colour);
        sky.setBird(bird);
        sky.setCloud(cloud);
        return sky;
    }

    @Bean
    public Cloud cloud() {
        Cloud cloud = new Cloud();
        cloud.setName("白云");
        return cloud;
    }

}
```



* 根据factoryClass，mbd获取候选的方法

```java
/**
 * Retrieve all candidate methods for the given class, considering
 * the {@link RootBeanDefinition#isNonPublicAccessAllowed()} flag.
 * Called as the starting point for factory method determination.
 */
private Method[] getCandidateMethods(Class<?> factoryClass, RootBeanDefinition mbd) {
    if (System.getSecurityManager() != null) {
        return AccessController.doPrivileged((PrivilegedAction<Method[]>) () ->
                (mbd.isNonPublicAccessAllowed() ?
                    ReflectionUtils.getAllDeclaredMethods(factoryClass) : factoryClass.getMethods()));
    }
    else {
        // bean定义是否允许访问非public方法
        return (mbd.isNonPublicAccessAllowed() ?
                // 获取所有方法
                ReflectionUtils.getAllDeclaredMethods(factoryClass) : 
                // 获取所有public方法
                factoryClass.getMethods());
    }
}
```



#### 判断候选方法是否为工厂方法

```java
// ConfigurationClassBeanDefinition
@Override
public boolean isFactoryMethod(Method candidate) {
    //（1）方法名称是否匹配工厂方法名称
    // (2) 方法是否有@Bean注解
    // (3) 从候选方法中获取的bean名称是否等于bean定义里的beanName
    return (super.isFactoryMethod(candidate) && BeanAnnotationHelper.isBeanAnnotated(candidate) &&
            BeanAnnotationHelper.determineBeanNameFor(candidate).equals(this.derivedBeanName));
}

// super.isFactoryMethod(candidate)
/**
 * Check whether the given candidate qualifies as a factory method.
 */
public boolean isFactoryMethod(Method candidate) {
    return candidate.getName().equals(getFactoryMethodName());
}
/**
 * Return a factory method, if any.
 */
@Override
@Nullable
public String getFactoryMethodName() {
    // 同一个bean名称对应多个的工厂方法。哪个方法先定义，就用哪个方法名称作为工厂方法
    /*
    // 例如：
    （1）情况一：Parent是Son的父类，factoryMethodName=city。 这两个方法都是候选方法       
    @Bean
    public City city(House house, Person person, Parent parent) {
        City city = new City();
        city.setPerson(person);
        city.setHouse(house);
        city.setParent(parent);
        return city;
    }

    @Bean
    public City city(House house, Person person, Son son) {
        City city = new City();
        city.setPerson(person);
        city.setHouse(house);
        city.setParent(son);
        return city;
    }
   【注意】虽然可以这样定义，但是这种情况最后是不能实例化成功的，会抛出异常。
    因为这两个方法的类型差异权重一样，工厂方法实例化只允许单个匹配的工厂方法。
    throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                    "Ambiguous factory method matches found on class [" + factoryClass.getName() + "] " +
                    "(hint: specify index/type/name arguments for simple parameters to avoid type ambiguities): " +
                    ambiguousFactoryMethods);
    
    （2）情况二
    @Bean(value="city")
    public City c1(House house, Person person, Son son) {
        City city = new City();
        city.setPerson(person);
        city.setHouse(house);
        city.setParent(son);
        return city;
    }

    @Bean(value="city")
    public City c2(House house, Person person, Son son) {
        City city = new City();
        city.setPerson(person);
        city.setHouse(house);
        city.setParent(son);
        return city;
    }
    因为c1方法先定义，所以factoryMethodName=c1。只有c1方法是候选的方法，最终会选择c1方法实例化
    
    （3）情况三
   @Bean(value="city")
    public City c2(House house, Person person) {
        City city = new City();
        city.setPerson(person);
        city.setHouse(house);
        return city;
    }

    @Bean(value="city")
    public City c2(House house, Person person, Son son) {
        City city = new City();
        city.setPerson(person);
        city.setHouse(house);
        city.setParent(son);
        return city;
    }

    @Bean(value="city")
    public City c1(House house, Person person, Son son) {
        City city = new City();
        city.setPerson(person);
        city.setHouse(house);
        city.setParent(son);
        return city;
    }

    @Bean(value="city")
    public City c1(House house, Person person) {
        City city = new City();
        city.setPerson(person);
        city.setHouse(house);
        return city;
    }
    因为c2方法先定义，所以factoryMethodName=c2。这里两个c2方法是候选的方法，经过排序后，参数最多的在前面。
    参数多的方法是优先被选为使用的工厂方法，虽然这两个c1方法参数值的类型差异权重都是一样的，但是参数个数不匹配，所以不会报错
    */
    return this.factoryMethodName;
}

// 方法是否有@Bean注解
public static boolean isBeanAnnotated(Method method) {
    return AnnotatedElementUtils.hasAnnotation(method, Bean.class);
}

// 从bean方法中获取beanName
public static String determineBeanNameFor(Method beanMethod) {
    // 从缓存获取
    String beanName = beanNameCache.get(beanMethod);
    if (beanName == null) {
        // 缓存的beanName等于null
        // By default, the bean name is the name of the @Bean-annotated method
        // 默认情况下，bean名称是带有@Bean注解的方法的名称
        beanName = beanMethod.getName();
        
	    // 检查用户是否显式设置了自定义bean名称…
        // Check to see if the user has explicitly set a custom bean name...
        // 获取方法上的@Bean注解的属性
        AnnotationAttributes bean =
                AnnotatedElementUtils.findMergedAnnotationAttributes(beanMethod, Bean.class, false, false);
        if (bean != null) {
            // 获取name属性值
            String[] names = bean.getStringArray("name");
            if (names.length > 0) {
                // name属性值存在，获取第一个属性值作为beanName
                beanName = names[0];
            }
        }
        // 添加缓存
        beanNameCache.put(beanMethod, beanName);
    }
    // 返回beanName
    return beanName;
}
```



#### 候选的工厂方法排序

候选的工厂方法排序使用的比较器：**先根据public访问权限进行倒序，再根据方法参数个数进行倒序**

```java
public static final Comparator<Executable> EXECUTABLE_COMPARATOR = (e1, e2) -> {
    int result = Boolean.compare(Modifier.isPublic(e2.getModifiers()), Modifier.isPublic(e1.getModifiers()));
    return result != 0 ? result : Integer.compare(e2.getParameterCount(), e1.getParameterCount());
};
```



* 严格模式获取参数值类型差异权重

ArgumentsHolder

```java
public int getAssignabilityWeight(Class<?>[] paramTypes) {
    // 检查得到的参数值类型是否是（目标的）参数类型及其子类
    for (int i = 0; i < paramTypes.length; i++) {
        if (!ClassUtils.isAssignableValue(paramTypes[i], this.arguments[i])) {
            // 如果不是，返回int的最大值
            return Integer.MAX_VALUE;
        }
    }
    // 检查得到的原始参数值类型是否是（目标的）参数类型及其子类
    for (int i = 0; i < paramTypes.length; i++) {
        if (!ClassUtils.isAssignableValue(paramTypes[i], this.rawArguments[i])) {
            // 如果不是，返回int的最大值减512
            return Integer.MAX_VALUE - 512;
        }
    }
    // 如果类型都匹配，返回int的最大值减1024
    return Integer.MAX_VALUE - 1024;
}
```



### autowireByName（根据名称注入）

```
创建bean定义时，指定了AUTOWIRE_BY_NAME
```

```java
/**
 * 如果autowire设置为“byName”，则用该工厂中其他bean的引用填充任何缺失的属性值。
 * Fill in any missing property values with references to
 * other beans in this factory if autowire is set to "byName".
 * @param beanName the name of the bean we're wiring up.
 * Useful for debugging messages; not used functionally.
 * @param mbd bean definition to update through autowiring
 * @param bw the BeanWrapper from which we can obtain information about the bean
 * @param pvs the PropertyValues to register wired objects with
 */
protected void autowireByName(
        String beanName, AbstractBeanDefinition mbd, BeanWrapper bw, MutablePropertyValues pvs) {
    // 获取所有需要注入的属性名称
    String[] propertyNames = unsatisfiedNonSimpleProperties(mbd, bw);
    for (String propertyName : propertyNames) {
        // 是否存在名为propertyName的bean
        if (containsBean(propertyName)) {
            // 根据属性名称获取相应的bean
            Object bean = getBean(propertyName);
            // pvs添加属性名称和属性值（即依赖的bean），用于后续的applyPropertyValues方法中调用set方法
            pvs.add(propertyName, bean);
            // 注册依赖的bean（记录propertyName依赖beanName，同时记录propertyName被beanName依赖了） 
            registerDependentBean(propertyName, beanName);
            if (logger.isTraceEnabled()) {
                logger.trace("Added autowiring by name from bean name '" + beanName +
                        "' via property '" + propertyName + "' to bean named '" + propertyName + "'");
            }
        }
        else {
            if (logger.isTraceEnabled()) {
                logger.trace("Not autowiring property '" + propertyName + "' of bean '" + beanName +
                        "' by name: no matching bean found");
            }
        }
    }
}

// 是否包含该bean
@Override
public boolean containsBean(String name) {
    // 去除工厂bean前缀&
    String beanName = transformedBeanName(name);
    // 是否包含该单实例bean或包含bean定义
    if (containsSingleton(beanName) || containsBeanDefinition(beanName)) {
        // 不是工厂bean自身引用的名称，或者是工厂bean
        return (!BeanFactoryUtils.isFactoryDereference(name) || isFactoryBean(name));
    }
    // 从父工厂查找
    // Not found -> check parent.
    BeanFactory parentBeanFactory = getParentBeanFactory();
    return (parentBeanFactory != null && parentBeanFactory.containsBean(originalBeanName(name)));
}

// pvs添加属性名称和属性值（即依赖的bean），用于后续的applyPropertyValues方法中调用set方法
public MutablePropertyValues add(String propertyName, @Nullable Object propertyValue) {
    addPropertyValue(new PropertyValue(propertyName, propertyValue));
    return this;
}

/**
 * Create a new PropertyValue instance.
 * @param name the name of the property (never {@code null})
 * @param value the value of the property (possibly before type conversion)
 */
public PropertyValue(String name, @Nullable Object value) {
    Assert.notNull(name, "Name must not be null");
    this.name = name;
    this.value = value;
}
```



### autowireByType（根据类型注入）

```java
/**
 * Abstract method defining "autowire by type" (bean properties by type) behavior.
 * <p>This is like PicoContainer default, in which there must be exactly one bean
 * of the property type in the bean factory. This makes bean factories simple to
 * configure for small namespaces, but doesn't work as well as standard Spring
 * behavior for bigger applications.
 * @param beanName the name of the bean to autowire by type
 * @param mbd the merged bean definition to update through autowiring
 * @param bw the BeanWrapper from which we can obtain information about the bean
 * @param pvs the PropertyValues to register wired objects with
 */
protected void autowireByType(
        String beanName, AbstractBeanDefinition mbd, BeanWrapper bw, MutablePropertyValues pvs) {
    // 获取自定义的类型转换器
    TypeConverter converter = getCustomTypeConverter();
    if (converter == null) {
        converter = bw;
    }

    // 保存所有自动注入的beanName
    Set<String> autowiredBeanNames = new LinkedHashSet<>(4);
    // 获取所有需要注入的属性名称
    String[] propertyNames = unsatisfiedNonSimpleProperties(mbd, bw);
    // 遍历所有需要注入的属性名称，根据类型解析并保存注入的值
    for (String propertyName : propertyNames) {
        try {
            // 获取属性描述器
            PropertyDescriptor pd = bw.getPropertyDescriptor(propertyName);
            // Don't try autowiring by type for type Object: never makes sense,
		   // even if it technically is a unsatisfied, non-simple property.
            // Don't try autowiring by type for type Object: never makes sense,
            // even if it technically is a unsatisfied, non-simple property.
            // 注入Object类型没有意义，不进入
            if (Object.class != pd.getPropertyType()) {
                // 获取属性的写方法，程序员指定。通常是set方法                
                MethodParameter methodParam = BeanUtils.getWriteMethodParameter(pd);
                // Do not allow eager init for type matching in case of a prioritized post-processor.
                // 在有优先级的后处理器的情况下，不要允许急于初始化类型匹配。一般这种类型，都是后处理器
                boolean eager = !(bw.getWrappedInstance() instanceof PriorityOrdered);
                // 【注意】创建AutowireByTypeDependencyDescriptor依赖描述器
                //  设置MethodParameter。和方法参数的注入是相似的。
                DependencyDescriptor desc = new AutowireByTypeDependencyDescriptor(methodParam, eager);
                // 解析依赖（和属性注入使用的方法是一样的，只不过使用的依赖描述器不一样而已）
                Object autowiredArgument = resolveDependency(desc, beanName, autowiredBeanNames, converter);、
                // 自动注入的值不为空 
                if (autowiredArgument != null) {
                    // pvs添加属性名称和属性值（即依赖的bean），用于后续的applyPropertyValues方法中调用set方法
                    pvs.add(propertyName, autowiredArgument);
                }
                // 注册依赖的bean（记录beanName依赖autowiredBeanNames，同时记录autowiredBeanNames被beanName依赖了）
                for (String autowiredBeanName : autowiredBeanNames) {                  
                    registerDependentBean(autowiredBeanName, beanName);
                    if (logger.isTraceEnabled()) {
                        logger.trace("Autowiring by type from bean name '" + beanName + "' via property '" +
                                propertyName + "' to bean named '" + autowiredBeanName + "'");
                    }
                }
                // 清空自动注入的集合
                autowiredBeanNames.clear();
            }
        }
        catch (BeansException ex) {
            throw new UnsatisfiedDependencyException(mbd.getResourceDescription(), beanName, propertyName, ex);
        }
    }
}

private static class AutowireByTypeDependencyDescriptor extends DependencyDescriptor {

    public AutowireByTypeDependencyDescriptor(MethodParameter methodParameter, boolean eager) {
        // 设置required=false，即不是必须的注入的
        super(methodParameter, false, eager);
    }

    // 获取依赖描述器的依赖名称。
    // 【注意】这里重写返回null。
    // 如果获取到多个候选的bean，在确定唯一的依赖时，如果通过@Primary注解、@Priority注解都没有得到唯一的bean后，因为这里重写返回null，则依赖描述器的依赖名称都是不等于候选的beanName的，最后无法确定唯一的依赖。
    // 就会报错，即使AutowireByTypeDependencyDescriptor设置required=false。因为注入的类型不是集合、数组、Map等多个bean的类型。具体看resolveDependency方法
    @Override
    public String getDependencyName() {
        return null;
    }
}
```



* 获取所有需要注入的属性名称

unsatisfiedNonSimpleProperties

```java
/**
 * Return an array of non-simple bean properties that are unsatisfied.
 * These are probably unsatisfied references to other beans in the
 * factory. Does not include simple properties like primitives or Strings.
 * @param mbd the merged bean definition the bean was created with
 * @param bw the BeanWrapper the bean was created with
 * @return an array of bean property names
 * @see org.springframework.beans.BeanUtils#isSimpleProperty
 */
protected String[] unsatisfiedNonSimpleProperties(AbstractBeanDefinition mbd, BeanWrapper bw) {
    Set<String> result = new TreeSet<>();
    // 获取bean定义指定的所有属性值
    PropertyValues pvs = mbd.getPropertyValues();
    // 获取所有属性描述器（包含bean实例的所有属性）
    PropertyDescriptor[] pds = bw.getPropertyDescriptors();
    for (PropertyDescriptor pd : pds) {
        //（1）属性的写方法是否为空（通常是set方法）
        //（2）检查属性是否是排除的依赖  
        //（3）检查从bean定义获取的属性值pvs是否包含该属性
        //（4）检查属性是否是简单的属性
        
        // 属性的set方法不为空，不是需要排除的依赖检查，不包含在从bean定义获取的属性值pvs，不是简单的属性
        if (pd.getWriteMethod() != null && !isExcludedFromDependencyCheck(pd) && !pvs.contains(pd.getName()) &&
                !BeanUtils.isSimpleProperty(pd.getPropertyType())) {
            result.add(pd.getName());
        }
    }
    return StringUtils.toStringArray(result);
}

/**
 * 检查属性是否是排除的依赖  
 * Determine whether the given bean property is excluded from dependency checks.
 * <p>This implementation excludes properties defined by CGLIB and
 * properties whose type matches an ignored dependency type or which
 * are defined by an ignored dependency interface.
 * @param pd the PropertyDescriptor of the bean property
 * @return whether the bean property is excluded
 * @see #ignoreDependencyType(Class)
 * @see #ignoreDependencyInterface(Class)
 */
protected boolean isExcludedFromDependencyCheck(PropertyDescriptor pd) {
    return (AutowireUtils.isExcludedFromDependencyCheck(pd) ||
            this.ignoredDependencyTypes.contains(pd.getPropertyType()) ||
            // 判断属性的set方法是否定义在忽略依赖的接口中
            AutowireUtils.isSetterDefinedInInterface(pd, this.ignoredDependencyInterfaces));
}
/**
 * Determine whether the given bean property is excluded from dependency checks.
 * <p>This implementation excludes properties defined by CGLIB.
 * @param pd the PropertyDescriptor of the bean property
 * @return whether the bean property is excluded
 */
public static boolean isExcludedFromDependencyCheck(PropertyDescriptor pd) {
    Method wm = pd.getWriteMethod();
    if (wm == null) {
        // set方法等于null
        return false;
    }
    if (!wm.getDeclaringClass().getName().contains("$$")) {
        // 不是cglib动态代理的方法
        // Not a CGLIB method so it's OK.
        return false;
    }   
    // It was declared by CGLIB, but we might still want to autowire it
    // if it was actually declared by the superclass.
    Class<?> superclass = wm.getDeclaringClass().getSuperclass();
    return !ClassUtils.hasMethod(superclass, wm);
}
/**
* 判断属性的setter方法是否定义在任意的给定的接口中
 * Return whether the setter method of the given bean property is defined
 * in any of the given interfaces.
 * @param pd the PropertyDescriptor of the bean property
 * @param interfaces the Set of interfaces (Class objects)
 * @return whether the setter method is defined by an interface
 */
public static boolean isSetterDefinedInInterface(PropertyDescriptor pd, Set<Class<?>> interfaces) {
    Method setter = pd.getWriteMethod();
    if (setter != null) {
        Class<?> targetClass = setter.getDeclaringClass();
        for (Class<?> ifc : interfaces) {
            if (ifc.isAssignableFrom(targetClass) && ClassUtils.hasMethod(ifc, setter)) {
                return true;
            }
        }
    }
    return false;
}

//检查从bean定义获取的属性值pvs是否包含该属性（检查pvs是否处理过该属性）
@Override
public boolean contains(String propertyName) {
    // getPropertyValue(propertyName) != null 有值说明不需要依赖解析，使用指定的值。一般是程序员在注册bean定义时候指定的
    return (getPropertyValue(propertyName) != null ||
            (this.processedProperties != null && this.processedProperties.contains(propertyName)));
}
// 记录着已经处理过的属性
@Nullable
private Set<String> processedProperties;

// 检查属性是否是简单的属性 
/**
 * Check if the given type represents a "simple" property: a simple value
 * type or an array of simple value types.
 * <p>See {@link #isSimpleValueType(Class)} for the definition of <em>simple
 * value type</em>.
 * <p>Used to determine properties to check for a "simple" dependency-check.
 * @param type the type to check
 * @return whether the given type represents a "simple" property
 * @see org.springframework.beans.factory.support.RootBeanDefinition#DEPENDENCY_CHECK_SIMPLE
 * @see org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#checkDependencies
 * @see #isSimpleValueType(Class)
 */
public static boolean isSimpleProperty(Class<?> type) {
    Assert.notNull(type, "'type' must not be null");
    return isSimpleValueType(type) || (type.isArray() && isSimpleValueType(type.getComponentType()));
}

/**
 * Check if the given type represents a "simple" value type: a primitive or
 * primitive wrapper, an enum, a String or other CharSequence, a Number, a
 * Date, a Temporal, a URI, a URL, a Locale, or a Class.
 * <p>{@code Void} and {@code void} are not considered simple value types.
 * @param type the type to check
 * @return whether the given type represents a "simple" value type
 * @see #isSimpleProperty(Class)
 */
public static boolean isSimpleValueType(Class<?> type) {
    return (Void.class != type && void.class != type &&
            // 基本数据类型及其包装类
            (ClassUtils.isPrimitiveOrWrapper(type) ||
            Enum.class.isAssignableFrom(type) ||
            CharSequence.class.isAssignableFrom(type) ||
            Number.class.isAssignableFrom(type) ||
            Date.class.isAssignableFrom(type) ||
            Temporal.class.isAssignableFrom(type) ||
            URI.class == type ||
            URL.class == type ||
            Locale.class == type ||
            Class.class == type));
}
```



* 扩展

```
@MppaerScan注解加载MapperScannerRegistrar注册的MapperScannerConfigurer，然后MapperScannerConfigurer里的ClassPathMapperScanner扫描并加载mapper的bean定义，其中的bean定义就是指定了AUTOWIRE_BY_TYPE
```

![image-20241222232343993](https://lu-note.oss-cn-shenzhen.aliyuncs.com/notes/work/image-20241222232343993.png)



### 实例化

* SimpleInstantiationStrategy

```java
public class SimpleInstantiationStrategy implements InstantiationStrategy {

	private static final ThreadLocal<Method> currentlyInvokedFactoryMethod = new ThreadLocal<>();

	/**
	 * Return the factory method currently being invoked or {@code null} if none.
	 * <p>Allows factory method implementations to determine whether the current
	 * caller is the container itself as opposed to user code.
	 */
	@Nullable
	public static Method getCurrentlyInvokedFactoryMethod() {
		return currentlyInvokedFactoryMethod.get();
	}    

     /**
     * 无参构造方法实例化
	 * Return an instance of the bean with the given name in this factory.
	 * @param bd the bean definition bean定义
	 * @param beanName the name of the bean when it is created in this context.
	 * The name can be {@code null} if we are autowiring a bean which doesn't
	 * belong to the factory.     bean名称
	 * @param owner the owning BeanFactory  bean工厂
	 * @return a bean instance for this bean definition 返回bean定义的实例
	 * @throws BeansException if the instantiation attempt failed
	 */
	@Override
	public Object instantiate(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner) {
		// Don't override the class with CGLIB if no overrides.
		if (!bd.hasMethodOverrides()) {
             // 如果bean定义没有方法重写，默认没有
			Constructor<?> constructorToUse;
			synchronized (bd.constructorArgumentLock) {
                  // 获取已解析的构造方法或者工厂方法
				constructorToUse = (Constructor<?>) bd.resolvedConstructorOrFactoryMethod;
				if (constructorToUse == null) {
                      // 已解析的构造方法或者工厂方法为null
                      // 获取beanClass
					final Class<?> clazz = bd.getBeanClass();
					if (clazz.isInterface()) {
                          // 如果是接口，直接报错
						throw new BeanInstantiationException(clazz, "Specified class is an interface");
					}
					try {
						if (System.getSecurityManager() != null) {
							constructorToUse = AccessController.doPrivileged(
									(PrivilegedExceptionAction<Constructor<?>>) clazz::getDeclaredConstructor);
						}
						else {
                               // 获取无参构造方法
							constructorToUse = clazz.getDeclaredConstructor();
						}
                          // 设置无参构造方法为已解析的构造方法  
						bd.resolvedConstructorOrFactoryMethod = constructorToUse;
					}
					catch (Throwable ex) {
						throw new BeanInstantiationException(clazz, "No default constructor found", ex);
					}
				}
			}
             // BeanUtils使用已解析的构造方法(无参构造方法)实例化 
			return BeanUtils.instantiateClass(constructorToUse);
		}
		else {
			// Must generate CGLIB subclass.
             // 不允许方法注入，直接抛出异常UnsupportedOperationException
			return instantiateWithMethodInjection(bd, beanName, owner);
		}
	}

	/**
	 * Subclasses can override this method, which is implemented to throw
	 * UnsupportedOperationException, if they can instantiate an object with
	 * the Method Injection specified in the given RootBeanDefinition.
	 * Instantiation should use a no-arg constructor.
	 */
	protected Object instantiateWithMethodInjection(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner) {
		throw new UnsupportedOperationException("Method Injection not supported in SimpleInstantiationStrategy");
	}

    /**
     * 有参构造方法实例化
     * Return an instance of the bean with the given name in this factory,
     * creating it via the given constructor.
     * @param bd the bean definition   bean定义
     * @param beanName the name of the bean when it is created in this context.
     * The name can be {@code null} if we are autowiring a bean which doesn't
     * belong to the factory.          bean名称
     * @param owner the owning BeanFactory   bean工厂
     * @param ctor the constructor to use    实例化使用的构造方法
     * @param args the constructor arguments to apply   构造方法使用的参数
     * @return a bean instance for this bean definition   返回bean定义的实例
     * @throws BeansException if the instantiation attempt failed
     */
	@Override
	public Object instantiate(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner,
			final Constructor<?> ctor, Object... args) {
		if (!bd.hasMethodOverrides()) {
              // 如果bean定义没有方法重写，默认没有
			if (System.getSecurityManager() != null) {
				// use own privileged to change accessibility (when security is on)
				AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
					ReflectionUtils.makeAccessible(ctor);
					return null;
				});
			}
              // 使用BeanUtils进行调用有参构造方法进行实例化
			return BeanUtils.instantiateClass(ctor, args);
		}
		else {
              // 不允许方法注入，直接抛出异常UnsupportedOperationException
			return instantiateWithMethodInjection(bd, beanName, owner, ctor, args);
		}
	}

	/**
	 * Subclasses can override this method, which is implemented to throw
	 * UnsupportedOperationException, if they can instantiate an object with
	 * the Method Injection specified in the given RootBeanDefinition.
	 * Instantiation should use the given constructor and parameters.
	 */
	protected Object instantiateWithMethodInjection(RootBeanDefinition bd, @Nullable String beanName,
			BeanFactory owner, @Nullable Constructor<?> ctor, Object... args) {

		throw new UnsupportedOperationException("Method Injection not supported in SimpleInstantiationStrategy");
	}

    /**
     *  工厂方法(@Bean方法)实例化
	 * Return an instance of the bean with the given name in this factory,
	 * creating it via the given factory method.
	 * @param bd the bean definition
	 * @param beanName the name of the bean when it is created in this context.
	 * The name can be {@code null} if we are autowiring a bean which doesn't
	 * belong to the factory.
	 * @param owner the owning BeanFactory
	 * @param factoryBean the factory bean instance to call the factory method on,
	 * or {@code null} in case of a static factory method
	 * @param factoryMethod the factory method to use
	 * @param args the factory method arguments to apply
	 * @return a bean instance for this bean definition
	 * @throws BeansException if the instantiation attempt failed
	 */
	@Override
	public Object instantiate(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner,
			@Nullable Object factoryBean, final Method factoryMethod, Object... args) {

		try {
			if (System.getSecurityManager() != null) {
				AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
					ReflectionUtils.makeAccessible(factoryMethod);
					return null;
				});
			}
			else {
                  // 允许访问 
				ReflectionUtils.makeAccessible(factoryMethod);
			}

			Method priorInvokedFactoryMethod = currentlyInvokedFactoryMethod.get();
			try {
				currentlyInvokedFactoryMethod.set(factoryMethod);
                  // 通过工厂bean和工厂方法参数调用工厂方法
				Object result = factoryMethod.invoke(factoryBean, args);
				if (result == null) {
                      // 如果工厂方法返回null，则创建NullBean实例返回
					result = new NullBean();
				}
				return result;
			}
			finally {
				if (priorInvokedFactoryMethod != null) {
					currentlyInvokedFactoryMethod.set(priorInvokedFactoryMethod);
				}
				else {
					currentlyInvokedFactoryMethod.remove();
				}
			}
		}
		catch (IllegalArgumentException ex) {
			throw new BeanInstantiationException(factoryMethod,
					"Illegal arguments to factory method '" + factoryMethod.getName() + "'; " +
					"args: " + StringUtils.arrayToCommaDelimitedString(args), ex);
		}
		catch (IllegalAccessException ex) {
			throw new BeanInstantiationException(factoryMethod,
					"Cannot access factory method '" + factoryMethod.getName() + "'; is it public?", ex);
		}
		catch (InvocationTargetException ex) {
			String msg = "Factory method '" + factoryMethod.getName() + "' threw exception";
			if (bd.getFactoryBeanName() != null && owner instanceof ConfigurableBeanFactory &&
					((ConfigurableBeanFactory) owner).isCurrentlyInCreation(bd.getFactoryBeanName())) {
				msg = "Circular reference involving containing bean '" + bd.getFactoryBeanName() + "' - consider " +
						"declaring the factory method as static for independence from its containing instance. " + msg;
			}
			throw new BeanInstantiationException(factoryMethod, msg, ex.getTargetException());
		}
	}

}
```



* BeanUtils

```java
/**
 * Convenience method to instantiate a class using the given constructor.
 * <p>Note that this method tries to set the constructor accessible if given a
 * non-accessible (that is, non-public) constructor, and supports Kotlin classes
 * with optional parameters and default values.
 * @param ctor the constructor to instantiate
 * @param args the constructor arguments to apply (use {@code null} for an unspecified
 * parameter, Kotlin optional parameters and Java primitive types are supported)
 * @return the new instance
 * @throws BeanInstantiationException if the bean cannot be instantiated
 * @see Constructor#newInstance
 */
public static <T> T instantiateClass(Constructor<T> ctor, Object... args) throws BeanInstantiationException {
    Assert.notNull(ctor, "Constructor must not be null");
    try {
        ReflectionUtils.makeAccessible(ctor);
        if (KotlinDetector.isKotlinReflectPresent() && KotlinDetector.isKotlinType(ctor.getDeclaringClass())) {
            return KotlinDelegate.instantiateClass(ctor, args);
        }
        else {
            Class<?>[] parameterTypes = ctor.getParameterTypes();
            Assert.isTrue(args.length <= parameterTypes.length, "Can't specify more arguments than constructor parameters");
            // 获取构造方法调用的参数
            Object[] argsWithDefaultValues = new Object[args.length];
            for (int i = 0 ; i < args.length; i++) {
                if (args[i] == null) {
                    Class<?> parameterType = parameterTypes[i];
                    argsWithDefaultValues[i] = (parameterType.isPrimitive() ? DEFAULT_TYPE_VALUES.get(parameterType) : null);
                }
                else {
                    argsWithDefaultValues[i] = args[i];
                }
            }
            // 通过构造方法实例化
            return ctor.newInstance(argsWithDefaultValues);
        }
    }
    catch (InstantiationException ex) {
        throw new BeanInstantiationException(ctor, "Is it an abstract class?", ex);
    }
    catch (IllegalAccessException ex) {
        throw new BeanInstantiationException(ctor, "Is the constructor accessible?", ex);
    }
    catch (IllegalArgumentException ex) {
        throw new BeanInstantiationException(ctor, "Illegal arguments for constructor", ex);
    }
    catch (InvocationTargetException ex) {
        throw new BeanInstantiationException(ctor, "Constructor threw exception", ex.getTargetException());
    }
}

private static final Map<Class<?>, Object> DEFAULT_TYPE_VALUES;
static {
    Map<Class<?>, Object> values = new HashMap<>();
    values.put(boolean.class, false);
    values.put(byte.class, (byte) 0);
    values.put(short.class, (short) 0);
    values.put(int.class, 0);
    values.put(long.class, 0L);
    values.put(float.class, 0F);
    values.put(double.class, 0D);
    values.put(char.class, '\0');
    DEFAULT_TYPE_VALUES = Collections.unmodifiableMap(values);
}
```



### 流程图

说明：大概描述@Autowired的注入过程

![@Autowired注入流程](https://lu-note.oss-cn-shenzhen.aliyuncs.com/notes/work/@Autowired%E6%B3%A8%E5%85%A5%E6%B5%81%E7%A8%8B.jpg)



## @Resource注入

```java
@Override
public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName) {
    // 寻找注入点(元数据)。即找到所有被@Resource注解标注的Field或Method，封装返回
    InjectionMetadata metadata = findResourceMetadata(beanName, bean.getClass(), pvs);
    try {
        // 依赖注入。Field和Method注入都是用该方法
        metadata.inject(bean, beanName, pvs);
    }
    catch (Throwable ex) {
        throw new BeanCreationException(beanName, "Injection of resource dependencies failed", ex);
    }
    // 返回所有属性值对象
    return pvs;
}
```

###  寻找注入点(元数据)

```java
private InjectionMetadata findResourceMetadata(String beanName, final Class<?> clazz, @Nullable PropertyValues pvs) {
    // 获取缓存的key。如果beanName不是空串，则作为缓存的key，否则用类名。
    // Fall back to class name as cache key, for backwards compatibility with custom callers.
    String cacheKey = (StringUtils.hasLength(beanName) ? beanName : clazz.getName());
    // Quick check on the concurrent map first, with minimal locking.
    // 从缓存中获取
    InjectionMetadata metadata = this.injectionMetadataCache.get(cacheKey);
    // 是否需要刷新缓存
    if (InjectionMetadata.needsRefresh(metadata, clazz)) {
        // 双检
        synchronized (this.injectionMetadataCache) {
            metadata = this.injectionMetadataCache.get(cacheKey);
            if (InjectionMetadata.needsRefresh(metadata, clazz)) {
                if (metadata != null) {
                    //从缓存中获取到的元数据不等于null，则从PropertyValues中移除该注入点
                    metadata.clear(pvs);
                }
                // 构建Resource注入点(元数据)
                metadata = buildResourceMetadata(clazz);
                // 添加缓存
                this.injectionMetadataCache.put(cacheKey, metadata);
            }
        }
    }
    // 返回注入点元数据
    return metadata;
}

/**
 * 是否需要刷新注入点
 * Check whether the given injection metadata needs to be refreshed.
 * @param metadata the existing metadata instance
 * @param clazz the current target class
 * @return {@code true} indicating a refresh, {@code false} otherwise
 * @see #needsRefresh(Class)
 */
public static boolean needsRefresh(@Nullable InjectionMetadata metadata, Class<?> clazz) {
    // 注入点为空，或者注入点的类不等于clazz，就需要刷新
    return (metadata == null || metadata.needsRefresh(clazz));
}
/**
 * Determine whether this metadata instance needs to be refreshed.
 * @param clazz the current target class
 * @return {@code true} indicating a refresh, {@code false} otherwise
 * @since 5.2.4
 */
protected boolean needsRefresh(Class<?> clazz) {
    return this.targetClass != clazz;
}
```

#### 构建Resource注入点(元数据)

```java
private InjectionMetadata buildResourceMetadata(final Class<?> clazz) {
    //（1）类名不是以java开头的注解
    //（2）clazz不是Order.class（org.springframework.core.Order）
    //（3）clazz的类名不是以java开头        
    // 说白，不是jdk包的注解和类，而且不是Order.class类，就都是候选类，返回true   
    if (!AnnotationUtils.isCandidateClass(clazz, resourceAnnotationTypes)) {
        return InjectionMetadata.EMPTY;
    }

    // 创建elements集合，保存所有需要注入的方法或属性
    List<InjectionMetadata.InjectedElement> elements = new ArrayList<>();
    Class<?> targetClass = clazz;

    do {
        final List<InjectionMetadata.InjectedElement> currElements = new ArrayList<>();

        // 通过反射获取@Resource注解、@WebServiceRef注解或者@EJB的属性，封装成注入点
        ReflectionUtils.doWithLocalFields(targetClass, field -> {            
            if (webServiceRefClass != null && field.isAnnotationPresent(webServiceRefClass)) {
                 // 属性存在@WebServiceRef注解
                
                // @WebServiceRef注解存在于静态属性上，则抛出异常
                if (Modifier.isStatic(field.getModifiers())) {
                    throw new IllegalStateException("@WebServiceRef annotation is not supported on static fields");
                }
                currElements.add(new WebServiceRefElement(field, field, null));
            }
            else if (ejbClass != null && field.isAnnotationPresent(ejbClass)) {
                // 属性存在@EJB注解
                
                // @EJB注解存在于静态属性上，则抛出异常
                if (Modifier.isStatic(field.getModifiers())) {
                    throw new IllegalStateException("@EJB annotation is not supported on static fields");
                }
                currElements.add(new EjbRefElement(field, field, null));
            }
            else if (field.isAnnotationPresent(Resource.class)) {
                // 属性存在@Resource注解
                
                // @Resource注解存在于静态属性上，则抛出异常
                if (Modifier.isStatic(field.getModifiers())) {
                    throw new IllegalStateException("@Resource annotation is not supported on static fields");
                }
                // 如果属性类型不是忽略的类型，则保存结果
                if (!this.ignoredResourceTypes.contains(field.getType().getName())) {
                    currElements.add(new ResourceElement(field, field, null));
                }
            }
        });

        // 通过反射获取@Resource注解、@WebServiceRef注解或@EJB注解的非静态且只有一个参数的方法，封装成注入点
        ReflectionUtils.doWithLocalMethods(targetClass, method -> {
            Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(method);
            if (!BridgeMethodResolver.isVisibilityBridgeMethodPair(method, bridgedMethod)) {
                return;
            }
            if (method.equals(ClassUtils.getMostSpecificMethod(method, clazz))) {
                if (webServiceRefClass != null && bridgedMethod.isAnnotationPresent(webServiceRefClass)) {
                     // @WebServiceRef注解不能用于静态方法     
                    if (Modifier.isStatic(method.getModifiers())) {
                        throw new IllegalStateException("@WebServiceRef annotation is not supported on static methods");
                    }
                    // @WebServiceRef注解只能用于一个参数的方法
                    if (method.getParameterCount() != 1) {
                        throw new IllegalStateException("@WebServiceRef annotation requires a single-arg method: " + method);
                    }
                    PropertyDescriptor pd = BeanUtils.findPropertyForMethod(bridgedMethod, clazz);
                    currElements.add(new WebServiceRefElement(method, bridgedMethod, pd));
                }
                else if (ejbClass != null && bridgedMethod.isAnnotationPresent(ejbClass)) {
                     // @EJB注解不能用于静态方法    
                    if (Modifier.isStatic(method.getModifiers())) {
                        throw new IllegalStateException("@EJB annotation is not supported on static methods");
                    }
                    // @EJB注解只能用于一个参数的方法
                    if (method.getParameterCount() != 1) {
                        throw new IllegalStateException("@EJB annotation requires a single-arg method: " + method);
                    }
                    PropertyDescriptor pd = BeanUtils.findPropertyForMethod(bridgedMethod, clazz);
                    currElements.add(new EjbRefElement(method, bridgedMethod, pd));
                }
                else if (bridgedMethod.isAnnotationPresent(Resource.class)) {
                    // @Resource注解不能用于静态方法                    
                    if (Modifier.isStatic(method.getModifiers())) {
                        throw new IllegalStateException("@Resource annotation is not supported on static methods");
                    }
                    Class<?>[] paramTypes = method.getParameterTypes();
                    // @Resource注解只能用于一个参数的方法
                    if (paramTypes.length != 1) {
                        throw new IllegalStateException("@Resource annotation requires a single-arg method: " + method);
                    }
                     // 如果方法参数类型不是忽略的类型，则保存结果
                    if (!this.ignoredResourceTypes.contains(paramTypes[0].getName())) {
                        // 属性的set方法名称或get方法名称等于bridgedMethod方法名称的都是
                        PropertyDescriptor pd = BeanUtils.findPropertyForMethod(bridgedMethod, clazz);
                        currElements.add(new ResourceElement(method, bridgedMethod, pd));
                    }
                }
            }
        });

        elements.addAll(0, currElements);
        // 继续从父类获取
        targetClass = targetClass.getSuperclass();
    }
    while (targetClass != null && targetClass != Object.class);

    // 返回所有需要注入的方法或属性
    return InjectionMetadata.forElements(elements, clazz);
}

/**
 * Return an {@code InjectionMetadata} instance, possibly for empty elements.
 * @param elements the elements to inject (possibly empty)
 * @param clazz the target class
 * @return a new {@link #InjectionMetadata(Class, Collection)} instance
 * @since 5.2
 */
public static InjectionMetadata forElements(Collection<InjectedElement> elements, Class<?> clazz) {
    // 创建注入元数据
    return (elements.isEmpty() ? new InjectionMetadata(clazz, Collections.emptyList()) :
            new InjectionMetadata(clazz, elements));
}
/**
 * Create a new {@code InjectionMetadata instance}.
 * <p>Preferably use {@link #forElements} for reusing the {@link #EMPTY}
 * instance in case of no elements.
 * @param targetClass the target class
 * @param elements the associated elements to inject
 * @see #forElements
 */
public InjectionMetadata(Class<?> targetClass, Collection<InjectedElement> elements) {
    this.targetClass = targetClass;
    this.injectedElements = elements;
}

// 目标类
private final Class<?> targetClass;
// 注入的所有成员
private final Collection<InjectedElement> injectedElements;
```

【注意】**@Resource注解只能用于只有一个参数的实例方法，而且不能用于静态属性。**



* ResourceElement

```java
private transient StringValueResolver embeddedValueResolver;

/**
 * CommonAnnotationBeanPostProcessor的内部类
 *
 * Class representing injection information about an annotated field
 * or setter method, supporting the @Resource annotation.
 */
private class ResourceElement extends LookupElement {

    // 是否是懒(延迟)查找
    private final boolean lazyLookup;

    public ResourceElement(Member member, AnnotatedElement ae, @Nullable PropertyDescriptor pd) {
        // LookupElement
        super(member, pd);        
        // 获取@Resource注解
        Resource resource = ae.getAnnotation(Resource.class);
        // 获取@Resource注解指定的名称作为资源名称
        String resourceName = resource.name();
         // 获取@Resource注解指定的类型作为资源类型
        Class<?> resourceType = resource.type();
        // 如果resourceName等于空串，即@Resource注解没有指定的名称，则使用默认的名称作为注入名称
        this.isDefaultName = !StringUtils.hasLength(resourceName);
        if (this.isDefaultName) {
            // 获取属性名或者方法名作为资源名称
            resourceName = this.member.getName();
            
            // 如果注入的是方法，而且方法是以set开头，方法名的长度大于3。
            if (this.member instanceof Method && resourceName.startsWith("set") && resourceName.length() > 3) {
                // 方法名截去前3个字符后得到新的字符串。再将首字母转小写后，作为注入的资源名称。
                // 例如：setPerson，则变成person。
                resourceName = Introspector.decapitalize(resourceName.substring(3));
            }
        }
        else if (embeddedValueResolver != null) {
            // 解析占位符填充（${}）得到resourceName
            resourceName = embeddedValueResolver.resolveStringValue(resourceName);
        }
	    // 如果@Resource注解指定的类型不是Object类型
        if (Object.class != resourceType) {
       		// 检查注入属性的类型或方法的参数类型是否是@Resource注解指定的类型
            checkResourceType(resourceType);
        }
        else {
            // No resource type specified... check field/method.
            // @Resource注解没有指定的类型，从注入的属性或方法参数获取具体的类型作为资源类型
            resourceType = getResourceType();
        }
        // 如果资源名称不等于null，设置资源名称为注入的名称
        this.name = (resourceName != null ? resourceName : "");
        // 设置资源类型为查找类型
        this.lookupType = resourceType;
        //  获取@Resource注解指定的查找值
        String lookupValue = resource.lookup();
        // 如果查找值不等于空串，则设置查找值为映射的名称。否则获取@Resource注解指定的映射名称
        this.mappedName = (StringUtils.hasLength(lookupValue) ? lookupValue : resource.mappedName());
        // 获取@Lazy注解
        Lazy lazy = ae.getAnnotation(Lazy.class);
        // @Lazy注解不等于null而且@Lazy注解的value属性值设置为true，则设置为懒(延迟)查找
        this.lazyLookup = (lazy != null && lazy.value());
    }

    @Override
    protected Object getResourceToInject(Object target, @Nullable String requestingBeanName) {
        return (this.lazyLookup ? buildLazyResourceProxy(this, requestingBeanName) :
                getResource(this, requestingBeanName));
    }
      
}

// 从注入的属性或方法参数获取具体的类型作为资源类型
// InjectedElement
protected final Class<?> getResourceType() {
    // 如果是属性
    if (this.isField) {
        // 返回属性的类型
        return ((Field) this.member).getType();
    }
    // 如果属性描述器不等于null
    else if (this.pd != null) {
        // 返回属性描述符的属性类型
        return this.pd.getPropertyType();
    }
    else {
        // 返回方法第一个参数的类型（@Resource注解只允许单个方法参数）
        return ((Method) this.member).getParameterTypes()[0];
    }
}

// 检查注入属性的类型或方法的参数类型是否是@Resource注解指定的类型
protected final void checkResourceType(Class<?> resourceType) {
    if (this.isField) {
        Class<?> fieldType = ((Field) this.member).getType();
        // 检查注入属性的类型是否是@Resource注解指定的类型，如果不是，则抛出异常
        if (!(resourceType.isAssignableFrom(fieldType) || fieldType.isAssignableFrom(resourceType))) {
            throw new IllegalStateException("Specified field type [" + fieldType +
                    "] is incompatible with resource type [" + resourceType.getName() + "]");
        }
    }
    else {
        // 获取参数类型。如果属性描述器不等于null，从属性描述器获取，否则获取方法第一个参数的类型。
        Class<?> paramType =
                (this.pd != null ? this.pd.getPropertyType() : ((Method) this.member).getParameterTypes()[0]);
        // 检查注入方法的参数类型是否是@Resource注解指定的类型，如果不是，则抛出异常
        if (!(resourceType.isAssignableFrom(paramType) || paramType.isAssignableFrom(resourceType))) {
            throw new IllegalStateException("Specified parameter type [" + paramType +
                    "] is incompatible with resource type [" + resourceType.getName() + "]");
        }
    }
}
```



```java
protected abstract static class LookupElement extends InjectionMetadata.InjectedElement {

    // 注入的名称
    protected String name = "";

    // 是否使用默认的名称作为注入名称
    protected boolean isDefaultName = false;

    // 注入的类型
    protected Class<?> lookupType = Object.class;

    @Nullable
    protected String mappedName;

    public LookupElement(Member member, @Nullable PropertyDescriptor pd) {
        // InjectedElement
        super(member, pd);
    }

    // ..
}
```



```java
/**
 * A single injected element.
 */
public abstract static class InjectedElement {
	// 注入的属性Field或者方法Method
    protected final Member member;
    // 是否是属性
    protected final boolean isField;
    // 属性描述符
    @Nullable
    protected final PropertyDescriptor pd;

    @Nullable
    protected volatile Boolean skip;

    protected InjectedElement(Member member, @Nullable PropertyDescriptor pd) {
        // 设置注入的属性Field或者方法Method
        this.member = member;
        // 是否是属性
        this.isField = (member instanceof Field);
        // 设置属性描述符
        this.pd = pd;
    }
    // ...	
}
```



###  依赖注入

```java
/**
* 依赖注入：循环每个注入点进行注入。不区分属性和方法注入。
*/
public void inject(Object target, @Nullable String beanName, @Nullable PropertyValues pvs) throws Throwable {
    // 获取所有的注入点
    Collection<InjectedElement> checkedElements = this.checkedElements;
    Collection<InjectedElement> elementsToIterate =
            (checkedElements != null ? checkedElements : this.injectedElements);
    if (!elementsToIterate.isEmpty()) {
        // 遍历每个注入点进行依赖注入。Field和Method注入都是一样    
        for (InjectedElement element : elementsToIterate) {           
            element.inject(target, beanName, pvs);
        }
    }
}
```

* InjectionMetadata

```java
/**
 * Either this or {@link #getResourceToInject} needs to be overridden.
 */
protected void inject(Object target, @Nullable String requestingBeanName, @Nullable PropertyValues pvs)
        throws Throwable {
	// 如果是属性
    if (this.isField) {        
        Field field = (Field) this.member;
        ReflectionUtils.makeAccessible(field);
        // getResourceToInject方法：获取注入资源
        // 通过反射，设置getResourceToInject方法获取到的“注入的属性值”到具体的属性中。
        field.set(target, getResourceToInject(target, requestingBeanName));
    }
    else {
        // 执行到这里说明是（只有一个参数的实例）方法
        if (checkPropertySkipping(pvs)) {
            return;
        }
        try {            
            Method method = (Method) this.member;
            ReflectionUtils.makeAccessible(method);
            // getResourceToInject方法：获取注入资源
            // 通过反射，使用getResourceToInject方法获取到的方法参数值(即需要注入的属性值)，调用具体的方法。
            method.invoke(target, getResourceToInject(target, requestingBeanName));
        }
        catch (InvocationTargetException ex) {
            throw ex.getTargetException();
        }
    }
}
```



### 获取注入资源

```java
@Override
protected Object getResourceToInject(Object target, @Nullable String requestingBeanName) {
    // 是否是懒查找(@Lazy)。如果是，则获取延迟解析资源代理。否则，(直接)获取资源。
    return (this.lazyLookup ? buildLazyResourceProxy(this, requestingBeanName) :
            getResource(this, requestingBeanName));
}
```

#### 获取延迟解析资源代理

```java
/**
 * 获取给定名称和类型的延迟解析资源代理，在方法调用进入后按需委托给getResource。
 * Obtain a lazily resolving resource proxy for the given name and type,
 * delegating to {@link #getResource} on demand once a method call comes in.
 * @param element the descriptor for the annotated field/method
 * @param requestingBeanName the name of the requesting bean
 * @return the resource object (never {@code null})
 * @since 4.2
 * @see #getResource
 * @see Lazy
 */
protected Object buildLazyResourceProxy(final LookupElement element, final @Nullable String requestingBeanName) {
    TargetSource ts = new TargetSource() {
        @Override
        public Class<?> getTargetClass() {
            return element.lookupType;
        }
        @Override
        public boolean isStatic() {
            return false;
        }
        @Override
        public Object getTarget() {
            // 本质还是通过getResource方法获取资源
            return getResource(element, requestingBeanName);
        }
        @Override
        public void releaseTarget(Object target) {
        }
    };

    // 创建代理工厂
    ProxyFactory pf = new ProxyFactory();
    // 设置目标来源
    pf.setTargetSource(ts);
    if (element.lookupType.isInterface()) {
        // 设置代理接口
        pf.addInterface(element.lookupType);
    }
    // 获取类加载器
    ClassLoader classLoader = (this.beanFactory instanceof ConfigurableBeanFactory ?
            ((ConfigurableBeanFactory) this.beanFactory).getBeanClassLoader() : null);
    // 通过代理工厂获取代理对象
    return pf.getProxy(classLoader);
}
```



#### (直接)获取资源

```java
/**
 * 获取给定名称和类型的资源对象。
 * Obtain the resource object for the given name and type.
 * @param element the descriptor for the annotated field/method
 * @param requestingBeanName the name of the requesting bean
 * @return the resource object (never {@code null})
 * @throws NoSuchBeanDefinitionException if no corresponding target resource found
 */
protected Object getResource(LookupElement element, @Nullable String requestingBeanName)
        throws NoSuchBeanDefinitionException {

    // 执行JNDI查找。这里我们可以不用管
    // JNDI lookup to perform?
    String jndiName = null;
    if (StringUtils.hasLength(element.mappedName)) {
        jndiName = element.mappedName;
    }
    else if (this.alwaysUseJndiLookup) {
        jndiName = element.name;
    }
    if (jndiName != null) {
        if (this.jndiFactory == null) {
            throw new NoSuchBeanDefinitionException(element.lookupType,
                    "No JNDI factory configured - specify the 'jndiFactory' property");
        }
        return this.jndiFactory.getBean(jndiName, element.lookupType);
    }

    // 常规的资源自动装配。重点关注这里
    // Regular resource autowiring
    if (this.resourceFactory == null) {
        // beanFactory等于null，直接抛出异常
        throw new NoSuchBeanDefinitionException(element.lookupType,
                "No resource factory configured - specify the 'resourceFactory' property");
    }
    // 返回自动装配的资源
    return autowireResource(this.resourceFactory, element, requestingBeanName);
}
```



```java
// 回调到默认的类型匹配
private boolean fallbackToDefaultTypeMatch = true;

/**
 * 通过基于给定工厂的自动装配获得给定名称和类型的资源对象。
 * Obtain a resource object for the given name and type through autowiring
 * based on the given factory.
 * @param factory the factory to autowire against
 * @param element the descriptor for the annotated field/method
 * @param requestingBeanName the name of the requesting bean
 * @return the resource object (never {@code null})
 * @throws NoSuchBeanDefinitionException if no corresponding target resource found
 */
protected Object autowireResource(BeanFactory factory, LookupElement element, @Nullable String requestingBeanName)
        throws NoSuchBeanDefinitionException {
     
    Object resource;
    Set<String> autowiredBeanNames;
    // 获取给定的名称
    String name = element.name;

    // beanFactory是AutowireCapableBeanFactory类型。一般都是，直接进入if
    if (factory instanceof AutowireCapableBeanFactory) {
        AutowireCapableBeanFactory beanFactory = (AutowireCapableBeanFactory) factory;
        // 根据属性或方法，获取依赖描述器
        DependencyDescriptor descriptor = element.getDependencyDescriptor();
        
        // 如果回退到默认类型匹配，注入的成员使用默认的名称(@Resource没有指定name属性时)， beanFactory没有给定名称的bean
        if (this.fallbackToDefaultTypeMatch && element.isDefaultName && !factory.containsBean(name)) {            
            // beanFactory没有给定名称的bean.                 
            /**            
            例如：
            private Grape g2ByMethod;
            @Resource
            public void setByMethod(Grape ggg) {
                this.g2 = ggg;
            }
            
            @Configuration
            public class FruitConfig {
                // 指定bean名称为g1
                @Bean(value = {"g1"}, destroyMethod = "grapeCustomDestroy")
                public Grape grape1() {
                    Grape g1 = new Grape();
                    g1.setName("g1");
                    return g1;
                }

                // 指定bean名称为g2
                @Bean("g2")
                public Grape grape2() {
                    Grape g2 = new Grape();
                    g2.setName("g2");
                    return g2;
                }
            }
             因为 @Resource没有指定name属性值，则使用默认生成的名称。
             即查找的名称为byMethod，而bean工厂没有该名称的bean。会走以下逻辑
             
              根据Grape类型获取bean，获取到beanName等于g1、g2的实例。
     		 找到有多个实例，但是需要注入的类型不是多个实例的（如List，Map，数组），就要确定唯一的bean实例。
     		 根据方法参数名称ggg，判断是否和g1或者g2相等，从而确定唯一的bean实例。
              因为都不相等，获取到null。而@Resource不允许注入null，就会抛出异常
              具体逻辑同【属性注入】的resolveDependency方法调用。这里只描述大概逻辑
                           
            */
            // 执行到这个if里，等于是@Autowired的注入方式。
           
            autowiredBeanNames = new LinkedHashSet<>();
            // 根据类型解析依赖资源。
            //【这里实际同@Autowired注入调用的方法是一样的】如果找到多个，没有确定唯一注入的值，也是会报错的
            resource = beanFactory.resolveDependency(descriptor, requestingBeanName, autowiredBeanNames, null);
            if (resource == null) {
                // 没有找到资源，则抛出异常
                throw new NoSuchBeanDefinitionException(element.getLookupType(), "No resolvable resource object");
            }
        }
        else {
            // 根据给定的名称和类型，获取bean实例作为自动装配的资源            
            resource = beanFactory.resolveBeanByName(name, descriptor);
            // 设置自动装配的beanName
            autowiredBeanNames = Collections.singleton(name);
        }
    }
    else {
        // beanFactory不是AutowireCapableBeanFactory类型
         // 一般不会执行到这里
        // 根据给定的名称和查找类型，获取bean实例作为自动装配的资源            
        resource = factory.getBean(name, element.lookupType);
         // 设置自动装配的beanName
        autowiredBeanNames = Collections.singleton(name);
    }

    if (factory instanceof ConfigurableBeanFactory) {
        ConfigurableBeanFactory beanFactory = (ConfigurableBeanFactory) factory;
        for (String autowiredBeanName : autowiredBeanNames) {
            if (requestingBeanName != null && beanFactory.containsBean(autowiredBeanName)) {
                // 注册依赖关系。
                beanFactory.registerDependentBean(autowiredBeanName, requestingBeanName);
            }
        }
    }

    // 返回资源（bean实例）
    return resource;
}

// 是否包含该bean
@Override
public boolean containsBean(String name) {
    // 去除工厂bean前缀&，如果有的话
    String beanName = transformedBeanName(name);
    // 是否包含该单实例bean或包含bean定义
    if (containsSingleton(beanName) || containsBeanDefinition(beanName)) {
        // 不是工厂bean自身引用的名称，或者是工厂bean
        return (!BeanFactoryUtils.isFactoryDereference(name) || isFactoryBean(name));
    }
    // Not found -> check parent.
    BeanFactory parentBeanFactory = getParentBeanFactory();
    return (parentBeanFactory != null && parentBeanFactory.containsBean(originalBeanName(name)));
}

/**
 * Return whether the given name is a factory dereference
 * (beginning with the factory dereference prefix).
 * @param name the name of the bean
 * @return whether the given name is a factory dereference
 * @see BeanFactory#FACTORY_BEAN_PREFIX
 */
public static boolean isFactoryDereference(@Nullable String name) {
    return (name != null && name.startsWith(BeanFactory.FACTORY_BEAN_PREFIX));
}
String FACTORY_BEAN_PREFIX = "&";

@Override
public boolean isFactoryBean(String name) throws NoSuchBeanDefinitionException {
    String beanName = transformedBeanName(name);
    Object beanInstance = getSingleton(beanName, false);
    if (beanInstance != null) {
        // 如果是FactoryBean类型，返回true
        return (beanInstance instanceof FactoryBean);
    }    
    // No singleton instance found -> check bean definition.
    // 从父工厂获取
    if (!containsBeanDefinition(beanName) && getParentBeanFactory() instanceof ConfigurableBeanFactory) {
        // No bean definition found in this factory -> delegate to parent.
        return ((ConfigurableBeanFactory) getParentBeanFactory()).isFactoryBean(name);
    }
    return isFactoryBean(beanName, getMergedLocalBeanDefinition(beanName));
}
```



* 根据属性或方法，获取依赖描述器

```java
/**
 * Build a DependencyDescriptor for the underlying field/method.
 */
public final DependencyDescriptor getDependencyDescriptor() {
    if (this.isField) {
        // 根据属性和查找的类型，创建依赖描述器
        return new LookupDependencyDescriptor((Field) this.member, this.lookupType);
    }
    else {
        // 根据方法和查找的类型，创建依赖描述器
        return new LookupDependencyDescriptor((Method) this.member, this.lookupType);
    }
}

/**
 * Extension of the DependencyDescriptor class,
 * overriding the dependency type with the specified resource type.
 */
private static class LookupDependencyDescriptor extends DependencyDescriptor {

    private final Class<?> lookupType;

    public LookupDependencyDescriptor(Field field, Class<?> lookupType) {
        // 【注意】这里required设置为true
        super(field, true);
        this.lookupType = lookupType;
    }

    public LookupDependencyDescriptor(Method method, Class<?> lookupType) {
        // 【注意】这里设置参数位置为0，required设置为true
        super(new MethodParameter(method, 0), true);
        this.lookupType = lookupType;
    }
    
    // 返回查找的类型，作为依赖类型
    @Override
    public Class<?> getDependencyType() {
        return this.lookupType;
    }
}

public MethodParameter(Method method, int parameterIndex) {
    this(method, parameterIndex, 1);
}
public MethodParameter(Method method, int parameterIndex, int nestingLevel) {
    Assert.notNull(method, "Method must not be null");
    this.executable = method;
    this.parameterIndex = validateIndex(method, parameterIndex);
    this.nestingLevel = nestingLevel;
}
```



```java
/**
 * Create a new descriptor for a field.
 * Considers the dependency as 'eager'.
 * @param field the field to wrap
 * @param required whether the dependency is required
 */
public DependencyDescriptor(Field field, boolean required) {
    this(field, required, true);
}

/**
 * Create a new descriptor for a field.
 * @param field the field to wrap
 * @param required whether the dependency is required
 * @param eager whether this dependency is 'eager' in the sense of
 * eagerly resolving potential target beans for type matching
 */
public DependencyDescriptor(Field field, boolean required, boolean eager) {
    super(field);

    this.declaringClass = field.getDeclaringClass();
    this.fieldName = field.getName();
    this.required = required;
    this.eager = eager;
}

// super(field);
public InjectionPoint(Field field) {
    Assert.notNull(field, "Field must not be null");
    this.field = field;
}

/**
 * Create a new descriptor for a method or constructor parameter.
 * Considers the dependency as 'eager'.
 * @param methodParameter the MethodParameter to wrap
 * @param required whether the dependency is required
 */
public DependencyDescriptor(MethodParameter methodParameter, boolean required) {
    this(methodParameter, required, true);
}

/**
 * Create a new descriptor for a method or constructor parameter.
 * @param methodParameter the MethodParameter to wrap
 * @param required whether the dependency is required
 * @param eager whether this dependency is 'eager' in the sense of
 * eagerly resolving potential target beans for type matching
 */
public DependencyDescriptor(MethodParameter methodParameter, boolean required, boolean eager) {
    super(methodParameter);

    this.declaringClass = methodParameter.getDeclaringClass();
    if (methodParameter.getMethod() != null) {
        this.methodName = methodParameter.getMethod().getName();
    }
    this.parameterTypes = methodParameter.getExecutable().getParameterTypes();
    this.parameterIndex = methodParameter.getParameterIndex();
    this.containingClass = methodParameter.getContainingClass();
    this.required = required;
    this.eager = eager;
}

/**
 * Create an injection point descriptor for a method or constructor parameter.
 * @param methodParameter the MethodParameter to wrap
 */
public InjectionPoint(MethodParameter methodParameter) {
    Assert.notNull(methodParameter, "MethodParameter must not be null");
    this.methodParameter = methodParameter;
}
```



* 根据给定的名称和类型获取bean实例作为自动装配的资源       

```java
@Override
public Object resolveBeanByName(String name, DependencyDescriptor descriptor) {
    // 设置当前注入点
    InjectionPoint previousInjectionPoint = ConstructorResolver.setCurrentInjectionPoint(descriptor);
    try {
        // descriptor.getDependencyType()：获取依赖类型
        // 根据名称和类型获取bean实例
        return getBean(name, descriptor.getDependencyType());
    }
    finally {
        // 回退当前注入点为上一个注入点
        ConstructorResolver.setCurrentInjectionPoint(previousInjectionPoint);
    }
}

// LookupDependencyDescriptor 获取依赖类型
@Override
public Class<?> getDependencyType() {
    // 返回查找类型
    return this.lookupType;
}

// AbstractBeanFactory
@Override
public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
    return doGetBean(name, requiredType, null, false);
}
```



### 大致过程

![@Resource注入](img\@Resource注入.jpg)



## 扩展

* bean实例化方式选择

![实例化方式选择](https://lu-note.oss-cn-shenzhen.aliyuncs.com/notes/work/%E5%AE%9E%E4%BE%8B%E5%8C%96%E6%96%B9%E5%BC%8F%E9%80%89%E6%8B%A9.jpg)

NullBean：通过工厂方法(@Bean方法)或bean定义指定的Supplier方法实例化返回null的时候，就会创建的NullBean类型的实例返回



## 参考

[【spring】依赖注入之@Autowired依赖注入 - 程序java圈 - 博客园 (cnblogs.com)](https://www.cnblogs.com/zfcq/p/15925553.html)

[Spring依赖注入源码分析-腾讯云开发者社区-腾讯云 (tencent.com)](https://cloud.tencent.com/developer/article/2062653)
