## 一、前言

在调用bean工厂后处理的方法(invokeBeanFactoryPostProcessors)中，会先获取所有的BeanDefinitionRegistryPostProcessor，调用postProcessBeanDefinitionRegistry方法。再获取所有的BeanFactoryPostProcessor，调用postProcessBeanFactory方法。

ConfigurationClassPostProcessor配置类后处理器，实现了BeanDefinitionRegistryPostProcessor接口，而BeanDefinitionRegistryPostProcessor继承了BeanFactoryPostProcessor接口，所以会调用配置类后处理器的postProcessBeanDefinitionRegistry方法处理配置类。

所以我们只需重点关注**ConfigurationClassPostProcessor的postProcessBeanDefinitionRegistry方法**

注意：bean工厂后处理的方法 位于spring的 refresh() 中，属于刷新上下文的一个阶段。



代码工程：

```
F:\code\scene\bean-definition-load
```



## 二、处理配置bean定义

ConfigurationClassPostProcessor的postProcessBeanDefinitionRegistry方法

```java
@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
        // 这里的registry实际上是DefaultListableBeanFactory
        // 通过native方法获取注册id，并保存到registriesPostProcessed
		int registryId = System.identityHashCode(registry);
		if (this.registriesPostProcessed.contains(registryId)) {
			throw new IllegalStateException(
					"postProcessBeanDefinitionRegistry already called on this post-processor against " + registry);
		}
		if (this.factoriesPostProcessed.contains(registryId)) {
			throw new IllegalStateException(
					"postProcessBeanFactory already called on this post-processor against " + registry);
		}
		this.registriesPostProcessed.add(registryId);
		
         // 处理配置bean定义
		processConfigBeanDefinitions(registry);
	}
```



```java
public static final AnnotationBeanNameGenerator IMPORT_BEAN_NAME_GENERATOR =
			FullyQualifiedAnnotationBeanNameGenerator.INSTANCE;

// 对于ImportSelector导入的配置类或者自动配置类，使用全类名bean名称生成器，获取全类目作为bean名称
private BeanNameGenerator importBeanNameGenerator = IMPORT_BEAN_NAME_GENERATOR;


public void processConfigBeanDefinitions(BeanDefinitionRegistry registry) {
		List<BeanDefinitionHolder> configCandidates = new ArrayList<>();
 // 获取bean定义的名称，初始时候IOC中只有6个bean定义信息。 前5个在new ApplicationContext的时候注册
/*
1、ConfigurationClassPostProcessor 属于BeanFactoryPostProcessor 
org.springframework.context.annotation.internalConfigurationAnnotationProcessor ConfigurationClassPostProcessor
2、AutowiredAnnotationBeanPostProcessor 属于BeanPostProcessor
org.springframework.context.annotation.internalAutowiredAnnotationProcessor  AutowiredAnnotationBeanPostProcessor   
3、CommonAnnotationBeanPostProcessor 属于BeanPostProcessor。用于处理常用的注解@PostConstruct、@PreDestroy、@Resource 
org.springframework.context.annotation.internalCommonAnnotationProcessor  CommonAnnotationBeanPostProcessor
4、EventListenerMethodProcessor 属于EventListenerMethodProcessor
org.springframework.context.event.internalEventListenerProcessor     EventListenerMethodProcessor
5、DefaultEventListenerFactory 属于EventListenerFactory
 org.springframework.context.event.internalEventListenerFactory DefaultEventListenerFactory  
6、  主启动类的bean定义信息
*/
		String[] candidateNames = registry.getBeanDefinitionNames();
	     // 遍历bean名称，获取bean定义，找到属于配置类的bean定义（第一次启动的时候，找到主启动配置类）
		for (String beanName : candidateNames) {
			BeanDefinition beanDef = registry.getBeanDefinition(beanName);
			if (beanDef.getAttribute(ConfigurationClassUtils.CONFIGURATION_CLASS_ATTRIBUTE) != null) {
				if (logger.isDebugEnabled()) {
					logger.debug("Bean definition has already been processed as a configuration class: " + beanDef);
				}
			}
             //【三、判断bean定义是否属于配置类】
			else if (ConfigurationClassUtils.checkConfigurationClassCandidate(beanDef, this.metadataReaderFactory)) {
				configCandidates.add(new BeanDefinitionHolder(beanDef, beanName));
			}
		}
		
    	// 如果配置类不存在，直接结束
		// Return immediately if no @Configuration classes were found
		if (configCandidates.isEmpty()) {
			return;
		}

         // 根据@Order注解的value排序
		// Sort by previously determined @Order value, if applicable
		configCandidates.sort((bd1, bd2) -> {
			int i1 = ConfigurationClassUtils.getOrder(bd1.getBeanDefinition());
			int i2 = ConfigurationClassUtils.getOrder(bd2.getBeanDefinition());
			return Integer.compare(i1, i2);
		});
		
         // 从application context获取自定义的bean名称生成器（默认是没有的）
		// Detect any custom bean name generation strategy supplied through the enclosing application context
		SingletonBeanRegistry sbr = null;
		if (registry instanceof SingletonBeanRegistry) {
			sbr = (SingletonBeanRegistry) registry;
			if (!this.localBeanNameGeneratorSet) {
				BeanNameGenerator generator = (BeanNameGenerator) sbr.getSingleton(
						AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR);
				if (generator != null) {
					this.componentScanBeanNameGenerator = generator;
					this.importBeanNameGenerator = generator;
				}
			}
		}

		if (this.environment == null) {
			this.environment = new StandardEnvironment();
		}

         // 创建配置类解析器，解析配置类。
		// Parse each @Configuration class
		ConfigurationClassParser parser = new ConfigurationClassParser(
				this.metadataReaderFactory, this.problemReporter, this.environment,
				this.resourceLoader, this.componentScanBeanNameGenerator, registry);
		
		Set<BeanDefinitionHolder> candidates = new LinkedHashSet<>(configCandidates);
		Set<ConfigurationClass> alreadyParsed = new HashSet<>(configCandidates.size());
		do {
			StartupStep processConfig = this.applicationStartup.start("spring.context.config-classes.parse");
             //	2.1 解析配置类
			parser.parse(candidates);
             // 2.2 校验配置类
			parser.validate();
             
             // 获取所有配置类
			Set<ConfigurationClass> configClasses = new LinkedHashSet<>(parser.getConfigurationClasses());
			configClasses.removeAll(alreadyParsed);

			// Read the model and create bean definitions based on its content
			if (this.reader == null) {
                 // 对于ImportSelector导入的配置类或者自动配置类，使用全类名bean名称生成器，获取全类目作为bean名称
				this.reader = new ConfigurationClassBeanDefinitionReader(
						registry, this.sourceExtractor, this.resourceLoader, this.environment,
						this.importBeanNameGenerator, parser.getImportRegistry());
			}
             // 【2.3 加载扫描路径外的bean定义】
			this.reader.loadBeanDefinitions(configClasses);
             // 保存已经处理的配置类
			alreadyParsed.addAll(configClasses);
			processConfig.tag("classCount", () -> String.valueOf(configClasses.size())).end();

			candidates.clear();
             // 第一次循环的时候registry.getBeanDefinitionCount()返回加载的bean数量 是大于 candidateNames.length的
             // 一开始candidateNames只有7个             
             // 筛选出还没有处理的配置类，保存到candidates，如果candidates不为空。
             // 继续执行while循环走解析加载bean定义的
			if (registry.getBeanDefinitionCount() > candidateNames.length) {
				String[] newCandidateNames = registry.getBeanDefinitionNames();
				Set<String> oldCandidateNames = new HashSet<>(Arrays.asList(candidateNames));
				Set<String> alreadyParsedClasses = new HashSet<>();
				for (ConfigurationClass configurationClass : alreadyParsed) {
					alreadyParsedClasses.add(configurationClass.getMetadata().getClassName());
				}
				for (String candidateName : newCandidateNames) {
					if (!oldCandidateNames.contains(candidateName)) {
						BeanDefinition bd = registry.getBeanDefinition(candidateName);
                          // 判断bean定义是否属于配置类
						if (ConfigurationClassUtils.checkConfigurationClassCandidate(bd, this.metadataReaderFactory) &&
								!alreadyParsedClasses.contains(bd.getBeanClassName())) {                         							 // 保存还没有处理过的配置类。
                               // 有没有这种情况呢？有的，在xml文件中，通过bean标签注入的bean
                               // 只是在前面注入bean本身的定义,
                            //如果这个bean也是一个配置类也设置了扫描路径和含有@Bean方法等情况，这些情况也是要处理的。
                               // 即通过xml设置的配置类
                               // 如示例工程里的TagDemoConfig类
							candidates.add(new BeanDefinitionHolder(bd, candidateName));
						}
					}
				}
				candidateNames = newCandidateNames;
			}
		}
		while (!candidates.isEmpty());

		// Register the ImportRegistry as a bean in order to support ImportAware @Configuration classes
		if (sbr != null && !sbr.containsSingleton(IMPORT_REGISTRY_BEAN_NAME)) {
			sbr.registerSingleton(IMPORT_REGISTRY_BEAN_NAME, parser.getImportRegistry());
		}

		if (this.metadataReaderFactory instanceof CachingMetadataReaderFactory) {
			// Clear cache in externally provided MetadataReaderFactory; this is a no-op
			// for a shared cache since it'll be cleared by the ApplicationContext.
			((CachingMetadataReaderFactory) this.metadataReaderFactory).clearCache();
		}
	}
```





### 2.1  解析配置类

```java
public void parse(Set<BeanDefinitionHolder> configCandidates) {
    	// 遍历候选的配置类【程序启动的时候，configCandidates中只包含主启动类的bean定义】	
		for (BeanDefinitionHolder holder : configCandidates) {
			BeanDefinition bd = holder.getBeanDefinition();
			try {
                  // 如果bean定义属于注解标注的bean定义
				if (bd instanceof AnnotatedBeanDefinition) {
                      // 有注解标注的，会调用这个方法，如@ComponentScan、@Component
					parse(((AnnotatedBeanDefinition) bd).getMetadata(), holder.getBeanName());
				}
                  // 如果bean定义属于AbstractBeanDefinition
				else if (bd instanceof AbstractBeanDefinition && ((AbstractBeanDefinition) bd).hasBeanClass()) {
					parse(((AbstractBeanDefinition) bd).getBeanClass(), holder.getBeanName());
				}
				else {
					parse(bd.getBeanClassName(), holder.getBeanName());
				}
			}
			catch (BeanDefinitionStoreException ex) {
				throw ex;
			}
			catch (Throwable ex) {
				throw new BeanDefinitionStoreException(
						"Failed to parse configuration class [" + bd.getBeanClassName() + "]", ex);
			}
		}
		
         // 【2.1.3 处理配置类@Import设置的DeferredImportSelector】
		this.deferredImportSelectorHandler.process();
	}
```



不管是AnnotatedBeanDefinition还是AbstractBeanDefinition，最后都会调用processConfigurationClass方法。

```java
protected final void parse(Class<?> clazz, String beanName) throws IOException {
    processConfigurationClass(new ConfigurationClass(clazz, beanName), DEFAULT_EXCLUSION_FILTER);
}

protected final void parse(AnnotationMetadata metadata, String beanName) throws IOException {
    processConfigurationClass(new ConfigurationClass(metadata, beanName), DEFAULT_EXCLUSION_FILTER);
}
private static final Predicate<String> DEFAULT_EXCLUSION_FILTER = className ->
			(className.startsWith("java.lang.annotation.") || className.startsWith("org.springframework.stereotype."));
```



```java
protected void processConfigurationClass(ConfigurationClass configClass, Predicate<String> filter) throws IOException {
    // 【2.1.1 是否跳过配置类】
    // 判断配置类是否有@Conditional注解，或者基于@Conditional注解的注解。例如@ConditionalOnMissingBean
    // 如果有，则判断配置类是否符合条件，如果不符合，直接跳过，不处理配置类
    // 一般来说，主启动类不会跳过
    if (this.conditionEvaluator.shouldSkip(configClass.getMetadata(), ConfigurationPhase.PARSE_CONFIGURATION)) {
        return;
    }
	
    // 从configurationClasses（map）中获取，是否已经存在加载过的配置类（ConfigurationClass类的hashcode为类名的hashcode）
    // 一般来说，主启动类一次执行的时候，existingClass为null 
    ConfigurationClass existingClass = this.configurationClasses.get(configClass);
    if (existingClass != null) {
        // 返回这个configClass配置类是通过@Import注册的还是由于嵌套在另一个配置类中而自动注册的。
        if (configClass.isImported()) {
            // 执行到这里说明configClass类是由其他类Import进来的，然后之前已经加载过了
            // configClass的importBy属性保留了是由什么类导入的
            
            // 如果existingClass是通过@Import注册或者是由另一个配置类导入的
            if (existingClass.isImported()) {
                // 合并importBy属性到existingClass
                // 这里的目的是为了记录当前配置类是由哪些配置类引入的
                existingClass.mergeImportedBy(configClass);
            }
            // Otherwise ignore new imported config class; existing non-imported class overrides it.
            return;
        }
        
        // 如果存在，而且configClass配置类不是通过@Import注册的还是由于嵌套在另一个配置类中而自动注册的，则从configurationClasses移除
        // 同时移除knownSuperclasses中保存的
        else {
            // 找到显式bean定义，可能替换了导入。我们把旧的拿掉，换上新的吧。
            // Explicit bean definition found, probably replacing an import.
            // Let's remove the old one and go with the new one.
            this.configurationClasses.remove(configClass);
            this.knownSuperclasses.values().removeIf(configClass::equals);
        }
    }
	
    // 递归地处理配置类及其超类层次结构。
    // Recursively process the configuration class and its superclass hierarchy.
    // 将configClass和filter封装成SourceClass类。
    SourceClass sourceClass = asSourceClass(configClass, filter);
    do {
        // (执行)递归地处理配置类及其超类层次结构。
        sourceClass = doProcessConfigurationClass(configClass, sourceClass, filter);
    }
    while (sourceClass != null);

    // 保存到configClass，记录已经处理过的configClass。
    this.configurationClasses.put(configClass, configClass);
}

private final Map<ConfigurationClass, ConfigurationClass> configurationClasses = new LinkedHashMap<>();
// key:父类类名 value:实际加载的配置类
private final Map<String, ConfigurationClass> knownSuperclasses = new HashMap<>();
```





```java
private final Set<ConfigurationClass> importedBy = new LinkedHashSet<>(1);
```



```java
/**
*  返回这个配置类是通过@Import注册的还是由于嵌套在另一个配置类中而自动注册的。
 * Return whether this configuration class was registered via @{@link Import} or
 * automatically registered due to being nested within another configuration class.
 * @since 3.1.1
 * @see #getImportedBy()
 */
public boolean isImported() {
    return !this.importedBy.isEmpty();
}
```



```java
/**
 * 将给定配置类中的import -by声明合并到这个配置类中。
 * Merge the imported-by declarations from the given configuration class into this one.
 * @since 4.0.5
 */
void mergeImportedBy(ConfigurationClass otherConfigClass) {
    this.importedBy.addAll(otherConfigClass.importedBy);
}
```



#### 2.1.1 是否跳过配置类

获取配置类的@Conditional注解，判断是否都满足条件

```java
/**
 * Determine if an item should be skipped based on {@code @Conditional} annotations.
 * @param metadata the meta data
 * @param phase the phase of the call
 * @return if the item should be skipped
 */
public boolean shouldSkip(@Nullable AnnotatedTypeMetadata metadata, @Nullable ConfigurationPhase phase) {
    // 如果没有@Conditional注解，或者基于@Conditional注解的注解，返回false，不跳过该配置类。
    if (metadata == null || !metadata.isAnnotated(Conditional.class.getName())) {
        return false;
    }

    if (phase == null) {
        if (metadata instanceof AnnotationMetadata &&
                ConfigurationClassUtils.isConfigurationCandidate((AnnotationMetadata) metadata)) {
            return shouldSkip(metadata, ConfigurationPhase.PARSE_CONFIGURATION);
        }
        return shouldSkip(metadata, ConfigurationPhase.REGISTER_BEAN);
    }
    
    //  获取@Conditional注解的Condition类,然后实例化保存
    List<Condition> conditions = new ArrayList<>();
    for (String[] conditionClasses : getConditionClasses(metadata)) {
        for (String conditionClass : conditionClasses) {
            Condition condition = getCondition(conditionClass, this.context.getClassLoader());
            conditions.add(condition);
        }
    }
	
    // 根据@Order注解排序
    AnnotationAwareOrderComparator.sort(conditions);
	
    // 遍历条件类，如果有一个条件类不符合条件，则返回true，跳过该配置类。
    for (Condition condition : conditions) {
        ConfigurationPhase requiredPhase = null;
        if (condition instanceof ConfigurationCondition) {
            // 获取ConfigurationPhase
            requiredPhase = ((ConfigurationCondition) condition).getConfigurationPhase();
        }
        // 调用Condition类的matches方法，如果不符合条件，返回false。
        /*
        @ConditionalOnBean, @ConditionalOnMissingBean,
        @ConditionalOnSingleCandidate注解的Condition类为OnBeanCondition        
        */
        if ((requiredPhase == null || requiredPhase == phase) && !condition.matches(this.context, metadata)) {
            return true;
        }
    }
    
    // 返回false说明不需要跳过
    return false;
}

// 获取@Conditional注解的Condition类
private List<String[]> getConditionClasses(AnnotatedTypeMetadata metadata) {
    MultiValueMap<String, Object> attributes = metadata.getAllAnnotationAttributes(Conditional.class.getName(), true);
    Object values = (attributes != null ? attributes.get("value") : null);
    return (List<String[]>) (values != null ? values : Collections.emptyList());
}
```



#### 2.1.2 递归处理配置类及其超类层次结构

```java
/**
 * Apply processing and build a complete {@link ConfigurationClass} by reading the
 * annotations, members and methods from the source class. This method can be called
 * multiple times as relevant sources are discovered.
 * @param configClass the configuration class being build
 * @param sourceClass a source class
 * @return the superclass, or {@code null} if none found or previously processed
 */
@Nullable
protected final SourceClass doProcessConfigurationClass(
      ConfigurationClass configClass, SourceClass sourceClass, Predicate<String> filter)
      throws IOException {

   // 判断类上是否有@Component注解，或者基于@Component注解。
   if (configClass.getMetadata().isAnnotated(Component.class.getName())) {
      // 【2.1.2.1 递归处理内部配置类】 加载内部配置类的bean定义
      // Recursively process any member (nested) classes first
      processMemberClasses(configClass, sourceClass, filter);
   }
   
   // 【2.1.2.2 处理@PropertySource注解】
   // 获取类上的 @PropertySource注解，解析@PropertySource注解的内容，读取外部配置，设置到配置类中
   // Process any @PropertySource annotations
   for (AnnotationAttributes propertySource : AnnotationConfigUtils.attributesForRepeatable(
         sourceClass.getMetadata(), PropertySources.class,
         org.springframework.context.annotation.PropertySource.class)) {
      if (this.environment instanceof ConfigurableEnvironment) {
         processPropertySource(propertySource);
      }
      else {
         logger.info("Ignoring @PropertySource annotation on [" + sourceClass.getMetadata().getClassName() +
               "]. Reason: Environment must implement ConfigurableEnvironment");
      }
   }
   
   // 【2.1.2.3 处理@ComponentScan注解，扫描加载符合条件的bean定义】
   // 获取@ComponentScan注解的属性
   // Process any @ComponentScan annotations
   Set<AnnotationAttributes> componentScans = AnnotationConfigUtils.attributesForRepeatable(
         sourceClass.getMetadata(), ComponentScans.class, ComponentScan.class);
   // 如果存在@ComponentScan注解，而且不跳过配置类
   if (!componentScans.isEmpty() &&
         !this.conditionEvaluator.shouldSkip(sourceClass.getMetadata(), ConfigurationPhase.REGISTER_BEAN)) {
      for (AnnotationAttributes componentScan : componentScans) {
          // 从扫描路径下面扫描并加载符合条件的bean定义
         // The config class is annotated with @ComponentScan -> perform the scan immediately
         Set<BeanDefinitionHolder> scannedBeanDefinitions =
               this.componentScanParser.parse(componentScan, sourceClass.getMetadata().getClassName());
                   
         // 获取已经扫描的bean定义，判断是否是配置类，如果是，递归扫描和加载这些配置类指定的路径下的bean定义
         // Check the set of scanned definitions for any further config classes and parse recursively if needed
         for (BeanDefinitionHolder holder : scannedBeanDefinitions) {
            BeanDefinition bdCand = holder.getBeanDefinition().getOriginatingBeanDefinition();
            if (bdCand == null) {
               bdCand = holder.getBeanDefinition();
            }
            // 【三、判断bean定义是否属于配置类】
            if (ConfigurationClassUtils.checkConfigurationClassCandidate(bdCand, this.metadataReaderFactory)) {
               // 递归扫描和加载配置类指定的路径下的bean定义，实际上又回到了【2.1解析配置类】的阶段
               parse(bdCand.getBeanClassName(), holder.getBeanName());
            }
         }
      }
   }

   // 【2.1.2.4 处理@Import 注解】
   // Process any @Import annotations
   // getImports(sourceClass)获取sourceClass类的@Import注解的value属性封装成SourceClass。即获取需要导入的类
   // 处理 @Import 注解，保存ImportSelector和ImportBeanDefinitionRegistrar，用于后续的加载。
   // 这里其实是为了加载不在@ComponentScan指定路径下的bean做准备。
   processImports(configClass, sourceClass, getImports(sourceClass), filter, true);

   // 【2.1.2.5 处理@ImportResource注解】
   // Process any @ImportResource annotations
   AnnotationAttributes importResource =
         AnnotationConfigUtils.attributesFor(sourceClass.getMetadata(), ImportResource.class);
   if (importResource != null) {
      String[] resources = importResource.getStringArray("locations");
      Class<? extends BeanDefinitionReader> readerClass = importResource.getClass("reader");
      for (String resource : resources) {
         String resolvedResource = this.environment.resolveRequiredPlaceholders(resource);
         configClass.addImportedResource(resolvedResource, readerClass);
      }
   }

   // 【2.1.2.6 处理配置类内的@Bean方法】
   // Process individual @Bean methods
   Set<MethodMetadata> beanMethods = retrieveBeanMethodMetadata(sourceClass);
   for (MethodMetadata methodMetadata : beanMethods) {
      configClass.addBeanMethod(new BeanMethod(methodMetadata, configClass));
   }

   // 【2.1.2.7 处理配置类实现的接口的default权限的@Bean方法】 
   // Process default methods on interfaces
   processInterfaces(configClass, sourceClass);

   // 【2.1.2.8 返回配置类的父类】 
   // Process superclass, if any
   if (sourceClass.getMetadata().hasSuperClass()) {
      String superclass = sourceClass.getMetadata().getSuperClassName();
      if (superclass != null && !superclass.startsWith("java") &&
            !this.knownSuperclasses.containsKey(superclass)) {
         this.knownSuperclasses.put(superclass, configClass);
         // Superclass found, return its annotation metadata and recurse
         return sourceClass.getSuperClass();
      }
   }

   // No superclass -> processing is complete
   return null;
}
```



##### 2.1.2.1 递归处理内部配置类

```java
private void processMemberClasses(ConfigurationClass configClass, SourceClass sourceClass,
      Predicate<String> filter) throws IOException {
   
   // 获取所有的内部类 
   Collection<SourceClass> memberClasses = sourceClass.getMemberClasses();
   if (!memberClasses.isEmpty()) {
      List<SourceClass> candidates = new ArrayList<>(memberClasses.size());
      for (SourceClass memberClass : memberClasses) {
         // 【三、判断bean定义是否属于配置类】
         if (ConfigurationClassUtils.isConfigurationCandidate(memberClass.getMetadata()) &&
               !memberClass.getMetadata().getClassName().equals(configClass.getMetadata().getClassName())) {
            candidates.add(memberClass);
         }
      }
      // 内部配置类根据@Order注解进行排序
      OrderComparator.sort(candidates);
      for (SourceClass candidate : candidates) {
         if (this.importStack.contains(configClass)) {
            this.problemReporter.error(new CircularImportProblem(configClass, this.importStack));
         }
         else {
            this.importStack.push(configClass);
            try {
               // 处理内部配置类 （即回到【2.1 解析配置类】，是一个递归处理的阶段）
               processConfigurationClass(candidate.asConfigClass(configClass), filter);
            }
            finally {
               this.importStack.pop();
            }
         }
      }
   }
}
```



##### 2.1.2.2 处理@PropertySource注解

获取类上的 @PropertySource注解，解析@PropertySource注解的属性，读取外部配置的内容，设置到配置类中



##### 2.1.2.3 处理@ComponentScan注解

获取@ComponentScan注解属性，从中获取扫描路径，从扫描路径下开始扫描bean，并加载符合条件的bean定义。

遍历已经加载到的bean定义，通过【三、判断bean定义是否属于配置类】的处理逻辑来判断它们是否属于配置类，如果是则递归地执行【2.1  解析配置类】的全过程。

```java
// 获取@ComponentScan注解的属性
// Process any @ComponentScan annotations
Set<AnnotationAttributes> componentScans = AnnotationConfigUtils.attributesForRepeatable(
     sourceClass.getMetadata(), ComponentScans.class, ComponentScan.class);
// 如果存在@ComponentScan注解，而且不跳过该配置类【2.1.1 是否跳过配置类】
if (!componentScans.isEmpty() &&
     !this.conditionEvaluator.shouldSkip(sourceClass.getMetadata(), ConfigurationPhase.REGISTER_BEAN)) {
  for (AnnotationAttributes componentScan : componentScans) {
     // 从扫描路径下面扫描并加载符合条件的bean定义
     // The config class is annotated with @ComponentScan -> perform the scan immediately
     Set<BeanDefinitionHolder> scannedBeanDefinitions =
           this.componentScanParser.parse(componentScan, sourceClass.getMetadata().getClassName());

     // 获取已经扫描的bean定义，判断是否是配置类，如果是，递归处理配置类，即递归执行【2.1 解析配置类】的过程。
     // Check the set of scanned definitions for any further config classes and parse recursively if needed
     for (BeanDefinitionHolder holder : scannedBeanDefinitions) {
        BeanDefinition bdCand = holder.getBeanDefinition().getOriginatingBeanDefinition();
        if (bdCand == null) {
           bdCand = holder.getBeanDefinition();
        }
        // 【三、判断bean定义是否属于配置类】
        if (ConfigurationClassUtils.checkConfigurationClassCandidate(bdCand, this.metadataReaderFactory)) {
           // 递归处理配置类。实际上又回到了【2.1解析配置类】的阶段
           parse(bdCand.getBeanClassName(), holder.getBeanName());
        }
     }
  }
}
```



* 扫描bean定义

```java
public Set<BeanDefinitionHolder> parse(AnnotationAttributes componentScan, final String declaringClass) {
    
    //【1、创建类路径bean定义扫描器】
    // componentScan.getBoolean("useDefaultFilters") @ComponentScan注解的useDefaultFilters属性默认为true
    ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(this.registry,
            componentScan.getBoolean("useDefaultFilters"), this.environment, this.resourceLoader);
	
    // 从@ComponentScan注解属性中获取bean名称生成器的类型，默认是BeanNameGenerator类型
    Class<? extends BeanNameGenerator> generatorClass = componentScan.getClass("nameGenerator");
    boolean useInheritedGenerator = (BeanNameGenerator.class == generatorClass);
    // 没有设置的话，默认使用this.beanNameGenerator。即AnnotationBeanNameGenerator
    // AnnotationBeanNameGenerator的generateBeanName方法，指定生成bean名称的规则，
    // 默认是获取类上面的@Component、@ManagedBean、@Named三个注解的value值(前提是value不为空串)
    // 获取value值的优先级别@Component > @ManagedBean > @Named
    // org.springframework.stereotype.Component
    // javax.annotation.ManagedBean
    // javax.inject.Named
    // 如果value为空串，则使用类名首字母转小写 (如果类名第二个字母也是大写，则首字母就不会转小写，直接使用类名)
    scanner.setBeanNameGenerator(useInheritedGenerator ? this.beanNameGenerator :
            BeanUtils.instantiateClass(generatorClass));

    ScopedProxyMode scopedProxyMode = componentScan.getEnum("scopedProxy");
    if (scopedProxyMode != ScopedProxyMode.DEFAULT) {
        scanner.setScopedProxyMode(scopedProxyMode);
    }
    else {
        Class<? extends ScopeMetadataResolver> resolverClass = componentScan.getClass("scopeResolver");
        scanner.setScopeMetadataResolver(BeanUtils.instantiateClass(resolverClass));
    }

    // 设置扫描资源的模式。@ComponentScan 默认使用DEFAULT_RESOURCE_PATTERN，即扫描类路径下的所有class文件。
    // static final String DEFAULT_RESOURCE_PATTERN = "**/*.class";
    scanner.setResourcePattern(componentScan.getString("resourcePattern"));

    // 设置扫描哪些类型，不扫描哪些类型。@ComponentScan注解不设置的话，就没有
    for (AnnotationAttributes filter : componentScan.getAnnotationArray("includeFilters")) {
        for (TypeFilter typeFilter : typeFiltersFor(filter)) {
            scanner.addIncludeFilter(typeFilter);
        }
    }
    for (AnnotationAttributes filter : componentScan.getAnnotationArray("excludeFilters")) {
        for (TypeFilter typeFilter : typeFiltersFor(filter)) {
            scanner.addExcludeFilter(typeFilter);
        }
    }

    // 是否懒加载
    boolean lazyInit = componentScan.getBoolean("lazyInit");
    if (lazyInit) {
        scanner.getBeanDefinitionDefaults().setLazyInit(true);
    }

    // 【2、获取扫描路径】
    // 获取@ComponentScan注解的basePackages属性指定的路径，保存到扫描路径
    Set<String> basePackages = new LinkedHashSet<>();
    String[] basePackagesArray = componentScan.getStringArray("basePackages");
    for (String pkg : basePackagesArray) {
        String[] tokenized = StringUtils.tokenizeToStringArray(this.environment.resolvePlaceholders(pkg),
                ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS);
        Collections.addAll(basePackages, tokenized);
    }
    // 获取@ComponentScan注解的basePackageClasses属性指定的类，获取类所在的包，保存到扫描路径
    for (Class<?> clazz : componentScan.getClassArray("basePackageClasses")) {
        basePackages.add(ClassUtils.getPackageName(clazz));
    }
    // 如果 @ComponentScan注解的basePackages属性和basePackageClasses属性都没有设置
    // 获取配置类所在的包，保存到扫描路径【这里可以看到获取了主启动类所在的包，作为扫描路径】
    if (basePackages.isEmpty()) {
        basePackages.add(ClassUtils.getPackageName(declaringClass));
    }

    //【3、设置过滤器，不扫描配置类自身】
    // 我们的主启动类在preparedContext的时候会设置主启动配置类的bean定义到spring中，
    // 那其他@Import的配置类本身，他们是如何加载到spring的呢？？？看后面的分析，是不在这里加载的
    scanner.addExcludeFilter(new AbstractTypeHierarchyTraversingFilter(false, false) {
        @Override
        protected boolean matchClassName(String className) {
            return declaringClass.equals(className);
        }
    });
    // 【4、解析扫描bean】
    return scanner.doScan(StringUtils.toStringArray(basePackages));
}
```



###### 1、创建类路径bean定义扫描器

```java
/**
 * Create a new {@code ClassPathBeanDefinitionScanner} for the given bean factory and
 * using the given {@link Environment} when evaluating bean definition profile metadata.
 * @param registry the {@code BeanFactory} to load bean definitions into, in the form
 * of a {@code BeanDefinitionRegistry}
 * @param useDefaultFilters whether to include the default filters for the
 * {@link org.springframework.stereotype.Component @Component},
 * {@link org.springframework.stereotype.Repository @Repository},
 * {@link org.springframework.stereotype.Service @Service}, and
 * {@link org.springframework.stereotype.Controller @Controller} stereotype annotations
 * @param environment the Spring {@link Environment} to use when evaluating bean
 * definition profile metadata
 * @param resourceLoader the {@link ResourceLoader} to use
 * @since 4.3.6
 */
public ClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry, boolean useDefaultFilters,
        Environment environment, @Nullable ResourceLoader resourceLoader) {
    
    Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
    this.registry = registry;
    // @ComponentScan注解的useDefaultFilters属性为true，所以会设置默认的过滤器
    if (useDefaultFilters) {
        registerDefaultFilters();
    }
    setEnvironment(environment);
    setResourceLoader(resourceLoader);
}
```



```java
/**
 * Register the default filter for {@link Component @Component}.
 * <p>This will implicitly register all annotations that have the
 * {@link Component @Component} meta-annotation including the
 * {@link Repository @Repository}, {@link Service @Service}, and
 * {@link Controller @Controller} stereotype annotations.
 * <p>Also supports Java EE 6's {@link javax.annotation.ManagedBean} and
 * JSR-330's {@link javax.inject.Named} annotations, if available.
 *
 */
@SuppressWarnings("unchecked")
protected void registerDefaultFilters() {
    // 创建@Component注解(或者基于@Component注解的注解)过滤器
    this.includeFilters.add(new AnnotationTypeFilter(Component.class));    
    // 创建javax.annotation.ManagedBean注解过滤器，如果类路径中存在这个注解
    ClassLoader cl = ClassPathScanningCandidateComponentProvider.class.getClassLoader();
    try {
        this.includeFilters.add(new AnnotationTypeFilter(
                ((Class<? extends Annotation>) ClassUtils.forName("javax.annotation.ManagedBean", cl)), false));
        logger.trace("JSR-250 'javax.annotation.ManagedBean' found and supported for component scanning");
    }
    catch (ClassNotFoundException ex) {
        // JSR-250 1.1 API (as included in Java EE 6) not available - simply skip.
    }
    
    // 创建javax.inject.Named注解过滤器，如果类路径中存在这个注解
    try {
        this.includeFilters.add(new AnnotationTypeFilter(
                ((Class<? extends Annotation>) ClassUtils.forName("javax.inject.Named", cl)), false));
        logger.trace("JSR-330 'javax.inject.Named' annotation found and supported for component scanning");
    }
    catch (ClassNotFoundException ex) {
        // JSR-330 API not available - simply skip.
    }
}
```



```java
/**
 * Create a new {@code AnnotationTypeFilter} for the given annotation type.
 * <p>The filter will also match meta-annotations. To disable the
 * meta-annotation matching, use the constructor that accepts a
 * '{@code considerMetaAnnotations}' argument.
 * <p>The filter will not match interfaces.
 * @param annotationType the annotation type to match
 */
public AnnotationTypeFilter(Class<? extends Annotation> annotationType) {
    this(annotationType, true, false);
}
/**
 * Create a new {@code AnnotationTypeFilter} for the given annotation type.
 * @param annotationType the annotation type to match
 * @param considerMetaAnnotations whether to also match on meta-annotations
 * @param considerInterfaces whether to also match interfaces
 */
public AnnotationTypeFilter(
        Class<? extends Annotation> annotationType, boolean considerMetaAnnotations, boolean considerInterfaces) {
	// @Component注解(或者基于@Component注解的注解)过滤器
    // false,false
    super(annotationType.isAnnotationPresent(Inherited.class), considerInterfaces);
    this.annotationType = annotationType;
    this.considerMetaAnnotations = considerMetaAnnotations;
}

protected AbstractTypeHierarchyTraversingFilter(boolean considerInherited, boolean considerInterfaces) {
    // 是否考虑继承的父类
    this.considerInherited = considerInherited;
    // 是否考虑接口
    this.considerInterfaces = considerInterfaces;
}
```



###### 2、获取扫描路径

* 获取@ComponentScan注解的basePackages属性指定的路径，保存到扫描路径

* 获取@ComponentScan注解的basePackageClasses属性指定的类，获取类所在的包，保存到扫描路径

* 如果以上都没有指定的话，获取配置类所在的包，作为扫描路径



###### 3、设置过滤器

设置过滤器，不扫描配置类自身



###### 4、解析扫描bean

```java
/**
 * Perform a scan within the specified base packages,
 * returning the registered bean definitions.
 * <p>This method does <i>not</i> register an annotation config processor
 * but rather leaves this up to the caller.
 * @param basePackages the packages to check for annotated classes
 * @return set of beans registered if any for tooling registration purposes (never {@code null})
 */
protected Set<BeanDefinitionHolder> doScan(String... basePackages) {
    Assert.notEmpty(basePackages, "At least one base package must be specified");
    Set<BeanDefinitionHolder> beanDefinitions = new LinkedHashSet<>();
    for (String basePackage : basePackages) {
        // 从类路径开始，获取扫描路径下所有符合条件bean定义
        Set<BeanDefinition> candidates = findCandidateComponents(basePackage);
        for (BeanDefinition candidate : candidates) {
            ScopeMetadata scopeMetadata = this.scopeMetadataResolver.resolveScopeMetadata(candidate);
            candidate.setScope(scopeMetadata.getScopeName());
            String beanName = this.beanNameGenerator.generateBeanName(candidate, this.registry);
            if (candidate instanceof AbstractBeanDefinition) {
                postProcessBeanDefinition((AbstractBeanDefinition) candidate, beanName);
            }
            if (candidate instanceof AnnotatedBeanDefinition) {
                AnnotationConfigUtils.processCommonDefinitionAnnotations((AnnotatedBeanDefinition) candidate);
            }
            
            // 检查是否和已经存在的bean定义冲突了。如果冲突了这里会抛出异常
            if (checkCandidate(beanName, candidate)) {              
                BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(candidate, beanName);
                definitionHolder =
                        AnnotationConfigUtils.applyScopedProxyMode(scopeMetadata, definitionHolder, this.registry);
                beanDefinitions.add(definitionHolder);
                // 注册bean定义到spring
                registerBeanDefinition(definitionHolder, this.registry);
            }
        }
    }
    return beanDefinitions;
}


/**
 *  检查是否和已经存在的bean定义冲突了。如果冲突了这里会抛出异常
 * Check the given candidate's bean name, determining whether the corresponding
 * bean definition needs to be registered or conflicts with an existing definition.
 * @param beanName the suggested name for the bean
 * @param beanDefinition the corresponding bean definition
 * @return {@code true} if the bean can be registered as-is;
 * {@code false} if it should be skipped because there is an
 * existing, compatible bean definition for the specified name
 * @throws ConflictingBeanDefinitionException if an existing, incompatible
 * bean definition has been found for the specified name
 */
protected boolean checkCandidate(String beanName, BeanDefinition beanDefinition) throws IllegalStateException {
    if (!this.registry.containsBeanDefinition(beanName)) {
        return true;
    }
    BeanDefinition existingDef = this.registry.getBeanDefinition(beanName);
    BeanDefinition originatingDef = existingDef.getOriginatingBeanDefinition();
    if (originatingDef != null) {
        existingDef = originatingDef;
    }
    if (isCompatible(beanDefinition, existingDef)) {
        return false;
    }
    throw new ConflictingBeanDefinitionException("Annotation-specified bean name '" + beanName +
            "' for bean class [" + beanDefinition.getBeanClassName() + "] conflicts with existing, " +
            "non-compatible bean definition of same name and class [" + existingDef.getBeanClassName() + "]");
}

/**
 * Determine whether the given new bean definition is compatible with
 * the given existing bean definition.
 * <p>The default implementation considers them as compatible when the existing
 * bean definition comes from the same source or from a non-scanning source.
 * @param newDefinition the new bean definition, originated from scanning
 * @param existingDefinition the existing bean definition, potentially an
 * explicitly defined one or a previously generated one from scanning
 * @return whether the definitions are considered as compatible, with the
 * new definition to be skipped in favor of the existing definition
 */
protected boolean isCompatible(BeanDefinition newDefinition, BeanDefinition existingDefinition) {
    return (!(existingDefinition instanceof ScannedGenericBeanDefinition) ||  // explicitly registered overriding bean
            (newDefinition.getSource() != null && newDefinition.getSource().equals(existingDefinition.getSource())) ||  // scanned same file twice
            newDefinition.equals(existingDefinition));  // scanned equivalent class twice
}
```





* 获取扫描路径下所有符合条件bean定义

```java
/**
 * Scan the class path for candidate components.
 * @param basePackage the package to check for annotated classes
 * @return a corresponding Set of autodetected bean definitions
 */
public Set<BeanDefinition> findCandidateComponents(String basePackage) {
    if (this.componentsIndex != null && indexSupportsIncludeFilters()) {
        return addCandidateComponentsFromIndex(this.componentsIndex, basePackage);
    }
    else {
        // 执行到这里，扫描候选的bean
        return scanCandidateComponents(basePackage);
    }
}
```



总结：

如果扫描的属于接口，不符合条件

如果扫描的属于类

* 具体的类：判断类上面是否有@Component、@ManagedBean注解，或者是基于这些注解的注解，如果有，说明符合条件。如果有@Conditional注解或者@Conditional元注解，还要满足对应的条件
* 抽象类：先要满足具体的类的条件，然后还要有@Lookup注解标注的方法。

```java
private Set<BeanDefinition> scanCandidateComponents(String basePackage) {
    Set<BeanDefinition> candidates = new LinkedHashSet<>();
    try {
        // 读取类路径下的扫描路径下的所有class文件，封装成Resource
        String packageSearchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX +
                resolveBasePackage(basePackage) + '/' + this.resourcePattern;
        Resource[] resources = getResourcePatternResolver().getResources(packageSearchPath);
        
        boolean traceEnabled = logger.isTraceEnabled();
        boolean debugEnabled = logger.isDebugEnabled();    
        for (Resource resource : resources) {
            if (traceEnabled) {
                logger.trace("Scanning " + resource);
            }
            try {
                // 获取注解元数据
                MetadataReader metadataReader = getMetadataReaderFactory().getMetadataReader(resource);
                
                // 见【4.1 根据注解元数据判断是否为候选的bean】
                // 本质：判断类上面是否有@Component、@ManagedBean注解，或者是基于这些注解的注解，如果有，说明符合条件。
                if (isCandidateComponent(metadataReader)) {
				  // 根据注解元数据创建bean定义                  
                    ScannedGenericBeanDefinition sbd = new ScannedGenericBeanDefinition(metadataReader);
                    sbd.setSource(resource);
                    // 见【4.4 根据bean定义判断是否为候选的bean】
				   // 如果是具体独立的类，则符合条件
			       // 如果是抽象独立的类，而且有@Lookup注解标注的方法，则符合条件
	                // 如果是接口，不符合条件
                    if (isCandidateComponent(sbd)) {
                        if (debugEnabled) {
                            logger.debug("Identified candidate component class: " + resource);
                        }
                        // 保存符合条件的bean定义，用于注册
                        candidates.add(sbd);
                    }
                    else {
                        if (debugEnabled) {
                            logger.debug("Ignored because not a concrete top-level class: " + resource);
                        }
                    }
                }
                else {
                    if (traceEnabled) {
                        logger.trace("Ignored because not matching any filter: " + resource);
                    }
                }
            }
            catch (FileNotFoundException ex) {
                if (traceEnabled) {
                    logger.trace("Ignored non-readable " + resource + ": " + ex.getMessage());
                }
            }
            catch (Throwable ex) {
                throw new BeanDefinitionStoreException(
                        "Failed to read candidate component class: " + resource, ex);
            }
        }
    }
    catch (IOException ex) {
        throw new BeanDefinitionStoreException("I/O failure during classpath scanning", ex);
    }
    return candidates;
}
```





##### 2.1.2.4 处理@Import 注解

解析@Import注解导入的类

获取配置类上所有的@Import注解，然后获取注解标注的 ImportSelector、ImportBeanDefinitionRegistrar、常规配置类，执行不同的逻辑

* 标注的是ImportSelector

获取ImportSelector的exclusionFilter排除过滤器

如果ImportSelector是DeferredImportSelector类型（推辞导入器），则将ImportSelector保存到集合中，用于后续加载（见 2.1.3 处理配置类@Import设置的DeferredImportSelector）

如果不是，则调用ImportSelector的selectImports(AnnotationMetadata importingClassMetadata)方法，获取所有需要加载的类名，然后递归执行【2.1.2.4 处理@Import 注解】的过程

* 标注的是ImportBeanDefinitionRegistrar

将ImportBeanDefinitionRegistrar保存到一个map中，用于后续加载（见 2.3.4 处理ImportBeanDefinitionRegistrar）

* 标注的是常规的配置类

就执行我们配置类整个解析和加载bean定义的过程，即【2.1 解析配置类】



```java
private void processImports(ConfigurationClass configClass, SourceClass currentSourceClass,
			Collection<SourceClass> importCandidates, Predicate<String> exclusionFilter,
			boolean checkForCircularImports) {
		// 导入的候选类为空直接结束
		if (importCandidates.isEmpty()) {
			return;
		}

		if (checkForCircularImports && isChainedImportOnStack(configClass)) {
			this.problemReporter.error(new CircularImportProblem(configClass, this.importStack));
		}
		else {
             // 加入导入栈
			this.importStack.push(configClass);
			try {
				for (SourceClass candidate : importCandidates) {
                      // 如果导入的类是ImportSelector类型
					if (candidate.isAssignable(ImportSelector.class)) {
						// Candidate class is an ImportSelector -> delegate to it to determine imports
						Class<?> candidateClass = candidate.loadClass();
						ImportSelector selector = ParserStrategyUtils.instantiateClass(candidateClass, ImportSelector.class,
								this.environment, this.resourceLoader, this.registry);
						Predicate<String> selectorFilter = selector.getExclusionFilter();
						if (selectorFilter != null) {
							exclusionFilter = exclusionFilter.or(selectorFilter);
						}
                          // 如果导入的类是延期的导入类
						if (selector instanceof DeferredImportSelector) {
                            // 保存延期导入的类到一个集合中【会在processConfigBeanDefinitions方法的parse方法的最后调用】
							this.deferredImportSelectorHandler.handle(configClass, (DeferredImportSelector) selector);
						}
						else {
                               // 如果导入的类不是延期的导入类，调用selectImports方法获取需要加载的bean，
                               // 然后递归调用processImports方法处理
							String[] importClassNames = selector.selectImports(currentSourceClass.getMetadata());
							Collection<SourceClass> importSourceClasses = asSourceClasses(importClassNames, exclusionFilter);
							processImports(configClass, currentSourceClass, importSourceClasses, exclusionFilter, false);
						}
					}
                    
                      // 如果导入的类是ImportBeanDefinitionRegistrar类型
					else if (candidate.isAssignable(ImportBeanDefinitionRegistrar.class)) {
						// Candidate class is an ImportBeanDefinitionRegistrar ->
						// delegate to it to register additional bean definitions
						Class<?> candidateClass = candidate.loadClass();
						ImportBeanDefinitionRegistrar registrar =
								ParserStrategyUtils.instantiateClass(candidateClass, ImportBeanDefinitionRegistrar.class,
										this.environment, this.resourceLoader, this.registry);
                          // 保存导入的类和用到导入的类的注解元数据到map中【会在processConfigBeanDefinitions方法的loadBeanDefinitions方法中调用】
						configClass.addImportBeanDefinitionRegistrar(registrar, currentSourceClass.getMetadata());
					}
                      
                      // 导入的类是普通的配置类，就走【2.1 解析配置类】的过程，这里实际上也是一个递归调用的过程
					else {
						// Candidate class not an ImportSelector or ImportBeanDefinitionRegistrar ->
						// process it as an @Configuration class
						this.importStack.registerImport(
								currentSourceClass.getMetadata(), candidate.getMetadata().getClassName());
                          //  candidate.asConfigClass(configClass) 创建ConfigurationClass
                          // 通过ConfigurationClass的importedBy记录candidate是由configClass导入的。
                          // 当我们后面是否加载配置类的自身的定义到spring的时候，就会通过importedBy来判断为空
                          // 如果importedBy不为空，则加载配置类自身，反之不加载。
						processConfigurationClass(candidate.asConfigClass(configClass), exclusionFilter);
					}
				}
			}
			catch (BeanDefinitionStoreException ex) {
				throw ex;
			}
			catch (Throwable ex) {
				throw new BeanDefinitionStoreException(
						"Failed to process import candidates for configuration class [" +
						configClass.getMetadata().getClassName() + "]", ex);
			}
			finally {
                  // 出栈
				this.importStack.pop();
			}
		}
	}


public ConfigurationClass asConfigClass(ConfigurationClass importedBy) {
    if (this.source instanceof Class) {
        return new ConfigurationClass((Class<?>) this.source, importedBy);
    }
    return new ConfigurationClass((MetadataReader) this.source, importedBy);
}
ConfigurationClass(MetadataReader metadataReader, @Nullable ConfigurationClass importedBy) {
    this.metadata = metadataReader.getAnnotationMetadata();
    this.resource = metadataReader.getResource();
    this.importedBy.add(importedBy);
}
ConfigurationClass(Class<?> clazz, @Nullable ConfigurationClass importedBy) {
    this.metadata = AnnotationMetadata.introspect(clazz);
    this.resource = new DescriptiveResource(clazz.getName());
    this.importedBy.add(importedBy);
}
private final Set<ConfigurationClass> importedBy = new LinkedHashSet<>(1);
```

总结：

@Import注解可以导入

* 实现ImportBeanDefinitionRegistrar接口的类
* 实现ImportSelector接口的类
* 常规的类（常规的类会走解析配置类的流程，因为可以在常规的类中使用@Service，@Bean等等注解）

ImportSelector的方法可以返回以下类的类名

* 实现ImportBeanDefinitionRegistrar接口的类
* 实现ImportSelector接口的类（因为会递归调用ImportSelector）
* 常规的类（常规的类会走解析配置类的流程，因为可以在常规的类中使用@Service，@Bean等等注解）



保存延期导入的类到一个集合中【会在processConfigBeanDefinitions方法的parse方法的最后调用】

```java
@Nullable
private List<DeferredImportSelectorHolder> deferredImportSelectors = new ArrayList<>();

/**
 * 处理指定的DeferredImportSelector。如果正在收集延迟导入选择器，则会将此实例注册到列表中。
 * 如果正在处理它们，那么也会根据其DeferredImportSelector. group立即处理DeferredImportSelector。
 * Handle the specified {@link DeferredImportSelector}. If deferred import
 * selectors are being collected, this registers this instance to the list. If
 * they are being processed, the {@link DeferredImportSelector} is also processed
 * immediately according to its {@link DeferredImportSelector.Group}.
 * @param configClass the source configuration class
 * @param importSelector the selector to handle
 */
public void handle(ConfigurationClass configClass, DeferredImportSelector importSelector) {
    DeferredImportSelectorHolder holder = new DeferredImportSelectorHolder(configClass, importSelector);
    if (this.deferredImportSelectors == null) {
        // 这里什么时候会执行到呢？？？
        // 如果我们在spring.factories文件中的org.springframework.boot.autoconfigure.EnableAutoConfiguration配置了
        // 一个我们自己定制的实现了DeferredImportSelector接口的类，返回我们需要加载的类
        // 执行到【2.1.3】AutoConfigurationImportSelector加载自动配置类之前会将deferredImportSelectors设置为null，然后加载自动配置类的过程中，遇到了DeferredImportSelector类型的实现类，就中会进入handle方法，然后通过deferredImportSelectors是否为null，决定是否进入这里，然后分组加载，同【2.1.3】，相当于递归处理了】
        DeferredImportSelectorGroupingHandler handler = new DeferredImportSelectorGroupingHandler();
        handler.register(holder);
        handler.processGroupImports();
    }
    else {
        // 第一次处理DeferredImportSelector的时候，只会简单的保存到deferredImportSelectors的集合中
        this.deferredImportSelectors.add(holder);
    }
}
```





##### 2.1.2.5 处理@ImportResource注解

获取和保存@ImportResource的扫描路径和BeanDefinitionReader

获取配置类上的@ImportResource注解。解析获取注解locations属性，得到扫描路径。获取注解的reader属性，得到BeanDefinitionReader（默认XmlBeanDefinitionReader，解析获取xml形式的bean定义）。保存扫描路径和BeanDefinitionReader到Map中，Map的key是扫描路径，value是BeanDefinitionReader的Class

（至于在哪加载bean定义呢？？？见  2.3.3 加载@ImportedResource引入的xml文件的bean定义）



##### 2.1.2.6 处理配置类内的@Bean方法

解析和保存@Bean方法

通过注解元数据获取标注@Bean注解的方法，然后保存到Set中返回。遍历Set，将表示标注@Bean注解方法的MethodMetadata和@Bean方法所在的类，封装成BeanMethod类，保存到Set<BeanMethod> beanMethods中。

```java
private Set<MethodMetadata> retrieveBeanMethodMetadata(SourceClass sourceClass) {
    	// 获取配置类的注解元数据
		AnnotationMetadata original = sourceClass.getMetadata();
    	// 获取标注@Bean注解方法
		Set<MethodMetadata> beanMethods = original.getAnnotatedMethods(Bean.class.getName());
         // 如果@Bean注解方法的个数大于1，而且配置类的注解元数据属于StandardAnnotationMetadata类型
		if (beanMethods.size() > 1 && original instanceof StandardAnnotationMetadata) {
			// Try reading the class file via ASM for deterministic declaration order...
			// Unfortunately, the JVM's standard reflection returns methods in arbitrary
			// order, even between different runs of the same application on the same JVM.
			try {
                 // 通过ASM获取表示类的元数据，再次获取@Bean注解标注的方法
				AnnotationMetadata asm =						this.metadataReaderFactory.getMetadataReader(original.getClassName()).getAnnotationMetadata();
				Set<MethodMetadata> asmMethods = asm.getAnnotatedMethods(Bean.class.getName());
                  
                  // 如果通过ASM获取到@Bean方法个数大于或等于直接通过元数据获取的@Bean方法
                  // 则遍历并保存 “ASM获取到@Bean方法名称”等于“通过元数据获取的@Bean方法名”的方法
				if (asmMethods.size() >= beanMethods.size()) {
					Set<MethodMetadata> selectedMethods = new LinkedHashSet<>(asmMethods.size());
					for (MethodMetadata asmMethod : asmMethods) {
						for (MethodMetadata beanMethod : beanMethods) {
							if (beanMethod.getMethodName().equals(asmMethod.getMethodName())) {
								selectedMethods.add(beanMethod);
								break;
							}
						}
					}
                      // 说明asm获取的方法全部都在通过元数据获取的@Bean方法中	
					if (selectedMethods.size() == beanMethods.size()) {
						// All reflection-detected methods found in ASM method set -> proceed
						beanMethods = selectedMethods;
					}
				}
			}
			catch (IOException ex) {
				logger.debug("Failed to read class file via ASM for determining @Bean method order", ex);
				// No worries, let's continue with the reflection metadata we started with...
			}
		}
    	// 返回获取到的@Bean方法
		return beanMethods;
	}
```



##### 2.1.2.7 处理配置类实现的接口的default权限的@Bean方法

解析和保存配置类实现的接口里 @Bean注解标注而且是default的方法

```java
/**
 * Register default methods on interfaces implemented by the configuration class.
 */
private void processInterfaces(ConfigurationClass configClass, SourceClass sourceClass) throws IOException {
    // 获取实现或者继承的接口
    for (SourceClass ifc : sourceClass.getInterfaces()) {
        // 同【2.1.2.6 处理配置类内的@Bean方法】过程，返回获取到的@Bean方法
        Set<MethodMetadata> beanMethods = retrieveBeanMethodMetadata(ifc);
        for (MethodMetadata methodMetadata : beanMethods) {
            // 如果@Bean方法不是抽象的，就说明是默认的(default)方法
            if (!methodMetadata.isAbstract()) {
			   // 保存default的@Bean方法
                // A default method or other concrete method on a Java 8+ interface...
                configClass.addBeanMethod(new BeanMethod(methodMetadata, configClass));
            }
        }
        // 当前接口可能也继承了其他的接口，其他的接口可能有default的@Bean方法。递归处理所有的上层接口。
        processInterfaces(configClass, ifc);
    }
}
```

* 实际加载@Bean方法的bean定义见：2.3.2 加载@Bean方法的bean定义



##### 2.1.2.8 返回配置类的父类

如果配置类继承的父类存在，则返回，递归处理父类【即父类也走一次 2.1.2 递归处理配置类及其超类层次结构 的过程】

如果父类不存在，说明全过程处理完成，直接返回null





#### 2.1.3 处理配置类@Import设置的DeferredImportSelector

通常这里处理主启动类@SpringBootApplication注解里的@EnableAutoConfiguration注解里的@Import注解设置的AutoConfigurationImportSelector类（加载spring.factories下的配置类），因为AutoConfigurationImportSelector属于DeferredImportSelector类型。

```java
public void process() {
        // 获取所有延迟导入的DeferredImportSelector 
        // deferredImportSelectors的内容会在 【2.1.2.4处理@Import 注解】的时候添加
        List<DeferredImportSelectorHolder> deferredImports = this.deferredImportSelectors;
    
        // 这里设置成null不是随便设置的，为什么设置为null呢？？？
        // 因为AutoConfigurationImportSelector读取spring.factories的org.springframework.boot.autoconfigure.EnableAutoConfiguration配置内容，在后面的分组处理导入过程中，又会调用到【2.1.2.4 处理@Import注解】的逻辑。此时org.springframework.boot.autoconfigure.EnableAutoConfiguration如果设置了DefaultDeferredImportSelector接口的实现类，就会根据deferredImportSelectors是否为null判断，是否进行递归处理。可以看【2.1.2.4 处理@Import注解】最后那里。仔细体会，精彩至极，妙哉妙哉。
        this.deferredImportSelectors = null;
        try {
            if (deferredImports != null) {
                // 创建延迟导入分组处理器
                DeferredImportSelectorGroupingHandler handler = new DeferredImportSelectorGroupingHandler();
                // deferredImports根据@Import注解排序
                deferredImports.sort(DEFERRED_IMPORT_COMPARATOR);
                //【2.1.3.1 延迟导入类分组保存】
                deferredImports.forEach(handler::register);
                //【2.1.3.2 分组处理导入的类】
                handler.processGroupImports();
            }
        }
        finally {
            this.deferredImportSelectors = new ArrayList<>();
        }
    }
```



##### 2.1.3.1 分组保存延迟导入类

```java

// key:分组类型 value：分组器。本质：分组类型 --> 分组器 --> DeferredImportSelector的List
private final Map<Object, DeferredImportSelectorGrouping> groupings = new LinkedHashMap<>();
private final Map<AnnotationMetadata, ConfigurationClass> configurationClasses = new HashMap<>();

public void register(DeferredImportSelectorHolder deferredImport) {
        // 获取分组的类型
        Class<? extends Group> group = deferredImport.getImportSelector().getImportGroup();
        // 根据分组类型获取或者创建 DeferredImportSelectorGrouping类返回
        DeferredImportSelectorGrouping grouping = this.groupings.computeIfAbsent(                
                (group != null ? group : deferredImport),
                // createGroup(group) 根据分组的类型进行实例化                
                key -> new DeferredImportSelectorGrouping(createGroup(group)));
        // 保存group类型下的deferredImport
        grouping.add(deferredImport);
        // 保存引入deferredImport的配置类
        this.configurationClasses.put(deferredImport.getConfigurationClass().getMetadata(),
                deferredImport.getConfigurationClass());
    }
```

说明：AutoConfigurationImportSelector返回的是AutoConfigurationGroup类型

```java
@Override
public Class<? extends Group> getImportGroup() {
    return AutoConfigurationGroup.class;
}
```



##### 2.1.3.2 分组处理导入的类

```java
public void processGroupImports() {
   // this.groupings.values()获取分组好的DeferredImportSelectorGrouping，获取它们需要自动导入的配置类，然后执行processImports方法（即执行【2.1.2.4 处理@Import注解的过程】）
        for (DeferredImportSelectorGrouping grouping : this.groupings.values()) {
            Predicate<String> exclusionFilter = grouping.getCandidateFilter();
            // grouping.getImports() 使用分组器解析DeferredImportSelector选择导入的类 
            // 通常这里只会通过主启动类设置的AutoConfigurationImportSelector获取自动导入的配置类
            // 本质上是【2.1.3.2.1 获取自动导入的配置类】
            grouping.getImports().forEach(entry -> {
                ConfigurationClass configurationClass = this.configurationClasses.get(entry.getMetadata());
                try {
                    // 执行【2.1.2.4 处理@Import注解】的过程
                    processImports(configurationClass, asSourceClass(configurationClass, exclusionFilter),
                            Collections.singleton(asSourceClass(entry.getImportClassName(), exclusionFilter)),
                            exclusionFilter, false);
                }
                catch (BeanDefinitionStoreException ex) {
                    throw ex;
                }
                catch (Throwable ex) {
                    throw new BeanDefinitionStoreException(
                            "Failed to process import candidates for configuration class [" +
                                    configurationClass.getMetadata().getClassName() + "]", ex);
                }
            });
        }
    }
```









###### 2.1.3.2.1 获取自动导入的配置类

```java
/**
 * Return the imports defined by the group.
 * @return each import with its associated configuration class
 */
public Iterable<Group.Entry> getImports() {
    for (DeferredImportSelectorHolder deferredImport : this.deferredImports) {
        // 主要执行具体分组器的process方法
        this.group.process(deferredImport.getConfigurationClass().getMetadata(),
                deferredImport.getImportSelector());
    }
    //主要执行具体分组器的selectImports方法
    return this.group.selectImports();
}
```



1、AutoConfigurationGroup

```java
private static class AutoConfigurationGroup
        implements DeferredImportSelector.Group, BeanClassLoaderAware, BeanFactoryAware, ResourceLoaderAware {

    private final Map<String, AnnotationMetadata> entries = new LinkedHashMap<>();

    private final List<AutoConfigurationEntry> autoConfigurationEntries = new ArrayList<>();
    
    
	/*
	执行处理逻辑
	Process the AnnotationMetadata of the importing @Configuration class using the specified 	DeferredImportSelector.
	*/
    @Override
    public void process(AnnotationMetadata annotationMetadata, DeferredImportSelector deferredImportSelector) {
        Assert.state(deferredImportSelector instanceof AutoConfigurationImportSelector,
                () -> String.format("Only %s implementations are supported, got %s",
                        AutoConfigurationImportSelector.class.getSimpleName(),
                        deferredImportSelector.getClass().getName()));      
        //【执行获取自动导入的配置类】
        // 不同的ImportSelector执行的逻辑不一样
        AutoConfigurationEntry autoConfigurationEntry = ((AutoConfigurationImportSelector) deferredImportSelector)
                .getAutoConfigurationEntry(annotationMetadata);
        // 保存
        this.autoConfigurationEntries.add(autoConfigurationEntry);
        for (String importClassName : autoConfigurationEntry.getConfigurations()) {
            // 遍历封装成entry
            // importClassName:自动导入的配置类的类名
            // annotationMetadata：引入@Import注解的类的元数据。如主启动类
            this.entries.putIfAbsent(importClassName, annotationMetadata);
        }
    }
    
    /**
    将导入的类的类名，和加载导入类的配置类封装成Entry接口，保存到节点返回   
	Return the entries of which class(es) should be imported for this group.
    */
    @Override
    public Iterable<Entry> selectImports() {
        // 如果需要导入的自动配置类为空，直接返回空集合
        if (this.autoConfigurationEntries.isEmpty()) {
            return Collections.emptyList();
        }
        // 去重，得到实际需要导入的自动配置类
        Set<String> allExclusions = this.autoConfigurationEntries.stream()
               .map(AutoConfigurationEntry::getExclusions).flatMap(Collection::stream).collect(Collectors.toSet());
        Set<String> processedConfigurations = this.autoConfigurationEntries.stream()
                .map(AutoConfigurationEntry::getConfigurations).flatMap(Collection::stream)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        processedConfigurations.removeAll(allExclusions);

        // 排序，然后封装成entry，收集返回
        return sortAutoConfigurations(processedConfigurations, getAutoConfigurationMetadata()).stream()
                .map((importClassName) -> new Entry(this.entries.get(importClassName), importClassName))
                .collect(Collectors.toList());
    }

    private AutoConfigurationMetadata getAutoConfigurationMetadata() {
        if (this.autoConfigurationMetadata == null) {
            this.autoConfigurationMetadata = AutoConfigurationMetadataLoader.loadMetadata(this.beanClassLoader);
        }
        return this.autoConfigurationMetadata;
    }

    private List<String> sortAutoConfigurations(Set<String> configurations,
            AutoConfigurationMetadata autoConfigurationMetadata) {
        return new AutoConfigurationSorter(getMetadataReaderFactory(), autoConfigurationMetadata)
                .getInPriorityOrder(configurations);
    }

    private MetadataReaderFactory getMetadataReaderFactory() {
        try {
            return this.beanFactory.getBean(SharedMetadataReaderFactoryContextInitializer.BEAN_NAME,
                    MetadataReaderFactory.class);
        }
        catch (NoSuchBeanDefinitionException ex) {
            return new CachingMetadataReaderFactory(this.resourceLoader);
        }
    }

}


/**
 * An entry that holds the {@link AnnotationMetadata} of the importing
 * {@link Configuration} class and the class name to import.
 */
class Entry {
	
    // 如主启动类的注解元数据
    private final AnnotationMetadata metadata;
    
    // 如spring.factories中org.springframework.boot.autoconfigure.EnableAutoConfiguration配置的类名
    private final String importClassName;
}
```



执行获取自动导入的配置类

* AutoConfigurationImportSelector 

```java
/**
 * Return the {@link AutoConfigurationEntry} based on the {@link AnnotationMetadata}
 * of the importing {@link Configuration @Configuration} class.
 * @param annotationMetadata the annotation metadata of the configuration class
 * @return the auto-configurations that should be imported
 */
protected AutoConfigurationEntry getAutoConfigurationEntry(AnnotationMetadata annotationMetadata) {
    // 是否开启了自动配置，默认是开启的
    if (!isEnabled(annotationMetadata)) {
        return EMPTY_ENTRY;
    }
    // 获取@EnableAutoConfiguration的exclude和excludeName属性，同@SpringBootApplication注解的这两个属性
    AnnotationAttributes attributes = getAttributes(annotationMetadata);
    // 从spring.factories中获取org.springframework.boot.autoconfigure.EnableAutoConfiguration设置的自动配置类
    List<String> configurations = getCandidateConfigurations(annotationMetadata, attributes);
    // 去重
    configurations = removeDuplicates(configurations);
    // 根据@EnableAutoConfiguration的exclude和excludeName属性和配置文件(如常见的yml、properties文件)的spring.autoconfigure.exclude配置内容，获取排除的自动配置类
    Set<String> exclusions = getExclusions(annotationMetadata, attributes);
    // 校验排除的类是否合法，即是否在configurations中，如果不在，就抛异常
    checkExcludedClasses(configurations, exclusions);
    // 去除排除的，保留实际需要加载的
    configurations.removeAll(exclusions);
    // 从spring.factories中获取所有的AutoConfigurationImportFilter，如OnBeanCondition用于@ConditionalOnMissingBean等
    // 本质上也是和 【2.1.1 是否跳过配置类】的 OnBeanCondition 调用match方法来判断是一样的。
    configurations = getConfigurationClassFilter().filter(configurations);
    
    // 从spring.factories中获取所有的AutoConfigurationImportListener，然后发布AutoConfigurationImportEvent自动配置类导入事件
    fireAutoConfigurationImportEvents(configurations, exclusions);
    // 保存最终需要加载和排序的自动配置类到AutoConfigurationEntry，然后返回AutoConfigurationEntry
    return new AutoConfigurationEntry(configurations, exclusions);
}

// 是否开启了自动配置，默认是开启的
protected boolean isEnabled(AnnotationMetadata metadata) {
    if (getClass() == AutoConfigurationImportSelector.class) {
        return getEnvironment().getProperty(EnableAutoConfiguration.ENABLED_OVERRIDE_PROPERTY, Boolean.class, true);
    }
    return true;
}
```



总结：获取org.springframework.boot.autoconfigure.EnableAutoConfiguration配置的自动配置类，去重，排除不导入的，筛选符合满足条件的（如ConditionalOnBean），然后返回导入。



2、DefaultDeferredImportSelectorGroup

org.springframework.boot.autoconfigure.EnableAutoConfiguration设置了DefaultDeferredImportSelector接口的实现类，在处理DefaultDeferredImportSelector接口的实现类时候，如果接口的实现类没有重写getImportGroup方法，就会用DefaultDeferredImportSelectorGroup。

```java
private static class DefaultDeferredImportSelectorGroup implements Group {

    private final List<Entry> imports = new ArrayList<>();

    @Override
    public void process(AnnotationMetadata metadata, DeferredImportSelector selector) {
        // 直接调用selectImports方法获取需要导入的类名
        for (String importClassName : selector.selectImports(metadata)) {
            this.imports.add(new Entry(metadata, importClassName));
        }
    }
	
    @Override
    public Iterable<Entry> selectImports() {
        return this.imports;
    }
}
```



获取完自动配置类后，然后遍历，逐个执行【2.1.2.4 处理@Import注解】的过程，加载bean定义。



对于org.springframework.boot.autoconfigure.EnableAutoConfiguration配置的自动配置类，配置的内容也是bean的名称

对于ImportSelector和DeferredImportSelector返回的配置类名称，也作为bean的名称。





### 2.2 校验配置类

```java
/**
 * 校验每个配置类
 * Validate each {@link ConfigurationClass} object.
 * @see ConfigurationClass#validate
 */
public void validate() {
    for (ConfigurationClass configClass : this.configurationClasses.keySet()) {
        configClass.validate(this.problemReporter);
    }
}
```



```java
void validate(ProblemReporter problemReporter) {
    // 获取配置类的@Configuration注解属性，如果属性不存在，则直接结束
    // 如果属性存在，而且proxyBeanMethods属性=true
    // 则判断配置类是否是final的，如果是final的，直接报错。（因为CGLIB的动态代理，是基于继承的，可能会基于配置类做AOP，所以加这个判断）    
    // A configuration class may not be final (CGLIB limitation) unless it declares proxyBeanMethods=false
    Map<String, Object> attributes = this.metadata.getAnnotationAttributes(Configuration.class.getName());
    if (attributes != null && (Boolean) attributes.get("proxyBeanMethods")) {
        if (this.metadata.isFinal()) {
            problemReporter.error(new FinalConfigurationProblem());
        }
        // 校验所有的@Bean注解标注的方法
        for (BeanMethod beanMethod : this.beanMethods) {
            beanMethod.validate(problemReporter);
        }
    }
}


@Override
public void validate(ProblemReporter problemReporter) {
    // @Bean注解标注的方法是静态方法，则直接结束
    if (getMetadata().isStatic()) {        
        // static @Bean methods have no constraints to validate -> return immediately
        return;
    }
	
    
    if (this.configurationClass.getMetadata().isAnnotated(Configuration.class.getName())) {
        // @Bean注解标注的方法不可以重写，则直接报错
        if (!getMetadata().isOverridable()) {
            // instance @Bean methods within @Configuration classes must be overridable to accommodate CGLIB
            problemReporter.error(new NonOverridableMethodError());
        }
    }
}

```



总结：要求@Configuration注解标注（而且注解的proxyBeanMethods=true）的配置类，不能是final的，而且里面的@Bean注解标注的方法不能是final和private的，但可以是static的。



### 2.3 加载扫描路径外的bean定义

```java
/**
 * Read {@code configurationModel}, registering bean definitions
 * with the registry based on its contents.
 */
public void loadBeanDefinitions(Set<ConfigurationClass> configurationModel) {
    TrackedConditionEvaluator trackedConditionEvaluator = new TrackedConditionEvaluator();
    for (ConfigurationClass configClass : configurationModel) {
        // 遍历每个配置类，加载bean定义
        loadBeanDefinitionsForConfigurationClass(configClass, trackedConditionEvaluator);
    }
}

/**
 * Read a particular {@link ConfigurationClass}, registering bean definitions
 * for the class itself and all of its {@link Bean} methods.
 */
private void loadBeanDefinitionsForConfigurationClass(
        ConfigurationClass configClass, TrackedConditionEvaluator trackedConditionEvaluator) {
	
    // 配置类是否需要跳过不加载，如果当前配置类是由其他配置类通过@Import形式加载进来的，
    // 其他的配置类不满足@Conditional的情况，则当前配置类就跳过也不加载。
    if (trackedConditionEvaluator.shouldSkip(configClass)) {
        String beanName = configClass.getBeanName();
        if (StringUtils.hasLength(beanName) && this.registry.containsBeanDefinition(beanName)) {
            this.registry.removeBeanDefinition(beanName);
        }
        this.importRegistry.removeImportingClass(configClass.getMetadata().getClassName());
        return;
    }
	
    // 当前配置类是否是通过@Import注解的ImportSelector注入的
    // 例如:自动配置类就是属于主启动类的@Import注解的ImportSelector注入的
    if (configClass.isImported()) {
        // 【2.3.1 加载配置类自身】
        registerBeanDefinitionForImportedConfigurationClass(configClass);
    }
    // 【2.3.2 加载@Bean方法的bean定义】
    for (BeanMethod beanMethod : configClass.getBeanMethods()) {
        loadBeanDefinitionsForBeanMethod(beanMethod);
    }
    
    // 【2.3.3 加载@ImportedResource引入的xml文件的bean定义】
    loadBeanDefinitionsFromImportedResources(configClass.getImportedResources());
    // 【2.3.4 处理ImportBeanDefinitionRegistrar】
    loadBeanDefinitionsFromRegistrars(configClass.getImportBeanDefinitionRegistrars());
}

```



```java
/**
 * Evaluate {@code @Conditional} annotations, tracking results and taking into
 * account 'imported by'.
 */
private class TrackedConditionEvaluator {

    private final Map<ConfigurationClass, Boolean> skipped = new HashMap<>();

    public boolean shouldSkip(ConfigurationClass configClass) {
        Boolean skip = this.skipped.get(configClass);
        if (skip == null) {
            if (configClass.isImported()) {
                boolean allSkipped = true;
                for (ConfigurationClass importedBy : configClass.getImportedBy()) {
                    if (!shouldSkip(importedBy)) {
                        allSkipped = false;
                        break;
                    }
                }
                if (allSkipped) {
                    // The config classes that imported this one were all skipped, therefore we are skipped...
                    skip = true;
                }
            }
            if (skip == null) {
                skip = conditionEvaluator.shouldSkip(configClass.getMetadata(), ConfigurationPhase.REGISTER_BEAN);
            }
            this.skipped.put(configClass, skip);
        }
        return skip;
    }
}
```



#### 2.3.1 加载配置类自身

```java
/**
 * Register the {@link Configuration} class itself as a bean definition.
 */
private void registerBeanDefinitionForImportedConfigurationClass(ConfigurationClass configClass) {
    // 通过注解元数据，创建bean定义
    AnnotationMetadata metadata = configClass.getMetadata();
    AnnotatedGenericBeanDefinition configBeanDef = new AnnotatedGenericBeanDefinition(metadata);
    
    ScopeMetadata scopeMetadata = scopeMetadataResolver.resolveScopeMetadata(configBeanDef);
    configBeanDef.setScope(scopeMetadata.getScopeName());
    // 通过importBeanNameGenerator生成bean名称，importBeanNameGenerator默认使用是全类目作为bean名称
    String configBeanName = this.importBeanNameGenerator.generateBeanName(configBeanDef, this.registry);
    // 获取@Lazy、@Primary、@DependsOn、@Role、@Description注解的属性，设置到bean定义中，如果有这些注解的话。
    AnnotationConfigUtils.processCommonDefinitionAnnotations(configBeanDef, metadata);

    BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(configBeanDef, configBeanName);
    definitionHolder = AnnotationConfigUtils.applyScopedProxyMode(scopeMetadata, definitionHolder, this.registry);
    // 注册配置类自身的bean定义到spring中。
    this.registry.registerBeanDefinition(definitionHolder.getBeanName(), definitionHolder.getBeanDefinition());
    configClass.setBeanName(configBeanName);

    if (logger.isTraceEnabled()) {
        logger.trace("Registered bean definition for imported class '" + configBeanName + "'");
    }
}
```





#### 2.3.2 加载@Bean方法的bean定义

```java
/**
 * Read the given {@link BeanMethod}, registering bean definitions
 * with the BeanDefinitionRegistry based on its contents.
 */
@SuppressWarnings("deprecation")  // for RequiredAnnotationBeanPostProcessor.SKIP_REQUIRED_CHECK_ATTRIBUTE
private void loadBeanDefinitionsForBeanMethod(BeanMethod beanMethod) {
    ConfigurationClass configClass = beanMethod.getConfigurationClass();
    // 获取@Bean方法的元数据
    MethodMetadata metadata = beanMethod.getMetadata();
    String methodName = metadata.getMethodName();
    
    // 判断是否需要跳过
    // Do we need to mark the bean as skipped by its condition?
    if (this.conditionEvaluator.shouldSkip(metadata, ConfigurationPhase.REGISTER_BEAN)) {
        configClass.skippedBeanMethods.add(methodName);
        return;
    }
    if (configClass.skippedBeanMethods.contains(methodName)) {
        return;
    }
 
    AnnotationAttributes bean = AnnotationConfigUtils.attributesFor(metadata, Bean.class);
    Assert.state(bean != null, "No @Bean annotation attributes");
    
    // 设置bean名称。如果@Bean的name属性没有设置的话，就用@Bean标注的方法名称作为bean名称
    // Consider name and any aliases
    List<String> names = new ArrayList<>(Arrays.asList(bean.getStringArray("name")));
    String beanName = (!names.isEmpty() ? names.remove(0) : methodName);

    // Register aliases even when overridden
    for (String alias : names) {
        this.registry.registerAlias(beanName, alias);
    }

    // Has this effectively been overridden before (e.g. via XML)?
    if (isOverriddenByExistingDefinition(beanMethod, beanName)) {
        if (beanName.equals(beanMethod.getConfigurationClass().getBeanName())) {
            throw new BeanDefinitionStoreException(beanMethod.getConfigurationClass().getResource().getDescription(),
                    beanName, "Bean name derived from @Bean method '" + beanMethod.getMetadata().getMethodName() +
                    "' clashes with bean name for containing configuration class; please make those names unique!");
        }
        return;
    }

    ConfigurationClassBeanDefinition beanDef = new ConfigurationClassBeanDefinition(configClass, metadata, beanName);
    beanDef.setSource(this.sourceExtractor.extractSource(metadata, configClass.getResource()));
    

    if (metadata.isStatic()) {
        // 静态的@Bean方法
        // static @Bean method
        if (configClass.getMetadata() instanceof StandardAnnotationMetadata) {
            beanDef.setBeanClass(((StandardAnnotationMetadata) configClass.getMetadata()).getIntrospectedClass());
        }
        else {
            // 指定bean定义的bean的类名
            beanDef.setBeanClassName(configClass.getMetadata().getClassName());
        }
        // 设置工厂方法的名称
        beanDef.setUniqueFactoryMethodName(methodName);
    }
    else {
        // 对应非静态的@Bean方法，这里实际上是设置我们的@Bean方法返回的bean是由工厂bean来生成的。
        // 工厂bean就是我们的配置类，工厂方法就是@Bean方法。
        // 即如果配置类中有@Bean方法，配置类就是工厂类，生产@Bean方法的对象。
        // instance @Bean method
        beanDef.setFactoryBeanName(configClass.getBeanName());
        beanDef.setUniqueFactoryMethodName(methodName);
    }

    if (metadata instanceof StandardMethodMetadata) {
        beanDef.setResolvedFactoryMethod(((StandardMethodMetadata) metadata).getIntrospectedMethod());
    }

    beanDef.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);
    beanDef.setAttribute(org.springframework.beans.factory.annotation.RequiredAnnotationBeanPostProcessor.
            SKIP_REQUIRED_CHECK_ATTRIBUTE, Boolean.TRUE);

    AnnotationConfigUtils.processCommonDefinitionAnnotations(beanDef, metadata);

    Autowire autowire = bean.getEnum("autowire");
    if (autowire.isAutowire()) {
        beanDef.setAutowireMode(autowire.value());
    }

    boolean autowireCandidate = bean.getBoolean("autowireCandidate");
    if (!autowireCandidate) {
        beanDef.setAutowireCandidate(false);
    }
	
    // 获取@Bean注解里设置的initMethod方法
    String initMethodName = bean.getString("initMethod");
    if (StringUtils.hasText(initMethodName)) {
        beanDef.setInitMethodName(initMethodName);
    }
    
    // 获取@Bean注解里设置的destroyMethod方法
    String destroyMethodName = bean.getString("destroyMethod");
    beanDef.setDestroyMethodName(destroyMethodName);

    // Consider scoping
    ScopedProxyMode proxyMode = ScopedProxyMode.NO;
    AnnotationAttributes attributes = AnnotationConfigUtils.attributesFor(metadata, Scope.class);
    if (attributes != null) {
        beanDef.setScope(attributes.getString("value"));
        proxyMode = attributes.getEnum("proxyMode");
        if (proxyMode == ScopedProxyMode.DEFAULT) {
            proxyMode = ScopedProxyMode.NO;
        }
    }

    // Replace the original bean definition with the target one, if necessary
    BeanDefinition beanDefToRegister = beanDef;
    if (proxyMode != ScopedProxyMode.NO) {
        BeanDefinitionHolder proxyDef = ScopedProxyCreator.createScopedProxy(
                new BeanDefinitionHolder(beanDef, beanName), this.registry,
                proxyMode == ScopedProxyMode.TARGET_CLASS);
        beanDefToRegister = new ConfigurationClassBeanDefinition(
                (RootBeanDefinition) proxyDef.getBeanDefinition(), configClass, metadata, beanName);
    }

    if (logger.isTraceEnabled()) {
        logger.trace(String.format("Registering bean definition for @Bean method %s.%s()",
                configClass.getMetadata().getClassName(), beanName));
    }
    
    // 【加载@Bean方法的bean定义到spring】
    this.registry.registerBeanDefinition(beanName, beanDefToRegister);
}
```



#### 2.3.3 加载@ImportedResource引入的xml文件的bean定义

```java
private void loadBeanDefinitionsFromImportedResources(
        Map<String, Class<? extends BeanDefinitionReader>> importedResources) {

    Map<Class<?>, BeanDefinitionReader> readerInstanceCache = new HashMap<>();
    // resource：xml文件的位置
    // readerClass: BeanDefinitionReader的class。（实际上是XmlBeanDefinitionReader.class）
    importedResources.forEach((resource, readerClass) -> {
        // Default reader selection necessary?
        if (BeanDefinitionReader.class == readerClass) {
            if (StringUtils.endsWithIgnoreCase(resource, ".groovy")) {
                // When clearly asking for Groovy, that's what they'll get...
                readerClass = GroovyBeanDefinitionReader.class;
            }
            else if (shouldIgnoreXml) {
                throw new UnsupportedOperationException("XML support disabled");
            }
            else {
                // Primarily ".xml" files but for any other extension as well
                readerClass = XmlBeanDefinitionReader.class;
            }
        }

        BeanDefinitionReader reader = readerInstanceCache.get(readerClass);
        if (reader == null) {
            try {
                // 实例化特定的bean定义阅读器
                // Instantiate the specified BeanDefinitionReader
                reader = readerClass.getConstructor(BeanDefinitionRegistry.class).newInstance(this.registry);
                // Delegate the current ResourceLoader to it if possible
                if (reader instanceof AbstractBeanDefinitionReader) {
                    // 设置resourceLoader和environment
                    AbstractBeanDefinitionReader abdr = ((AbstractBeanDefinitionReader) reader);
                    abdr.setResourceLoader(this.resourceLoader);
                    abdr.setEnvironment(this.environment);
                }
                readerInstanceCache.put(readerClass, reader);
            }
            catch (Throwable ex) {
                throw new IllegalStateException(
                        "Could not instantiate BeanDefinitionReader class [" + readerClass.getName() + "]");
            }
        }
	   
        // XmlBeanDefinitionReader从xml文件加载bean定义
        // TODO SPR-6310: qualify relative path locations as done in AbstractContextLoader.modifyLocations
        reader.loadBeanDefinitions(resource);
    });
}
```





```java
public int loadBeanDefinitions(String location) throws BeanDefinitionStoreException {
    return loadBeanDefinitions(location, null);
}
```



```java
/**
 * Load bean definitions from the specified resource location.
 * <p>The location can also be a location pattern, provided that the
 * ResourceLoader of this bean definition reader is a ResourcePatternResolver.
 * @param location the resource location, to be loaded with the ResourceLoader
 * (or ResourcePatternResolver) of this bean definition reader
 * @param actualResources a Set to be filled with the actual Resource objects
 * that have been resolved during the loading process. May be {@code null}
 * to indicate that the caller is not interested in those Resource objects.
 * @return the number of bean definitions found
 * @throws BeanDefinitionStoreException in case of loading or parsing errors
 * @see #getResourceLoader()
 * @see #loadBeanDefinitions(org.springframework.core.io.Resource)
 * @see #loadBeanDefinitions(org.springframework.core.io.Resource[])
 */
public int loadBeanDefinitions(String location, @Nullable Set<Resource> actualResources) throws BeanDefinitionStoreException {
    // 获取资源加载器
    ResourceLoader resourceLoader = getResourceLoader();
    if (resourceLoader == null) {
        throw new BeanDefinitionStoreException(
                "Cannot load bean definitions from location [" + location + "]: no ResourceLoader available");
    }
    
    // 这里我们的resourceLoader实际上是AnnotationConfigServletWebServerApplicationContext，属于ResourcePatternResolver类型
    if (resourceLoader instanceof ResourcePatternResolver) {
        // Resource pattern matching available.
        try {
            // 从location下获取所有的xml文件
            Resource[] resources = ((ResourcePatternResolver) resourceLoader).getResources(location);
            // 【2.3.3.1 加载xml文件下的所有bean定义】
            int count = loadBeanDefinitions(resources);
            if (actualResources != null) {
                Collections.addAll(actualResources, resources);
            }
            if (logger.isTraceEnabled()) {
                logger.trace("Loaded " + count + " bean definitions from location pattern [" + location + "]");
            }
            return count;
        }
        catch (IOException ex) {
            throw new BeanDefinitionStoreException(
                    "Could not resolve bean definition resource pattern [" + location + "]", ex);
        }
    }
    else {
        // Can only load single resources by absolute URL.
        Resource resource = resourceLoader.getResource(location);
        int count = loadBeanDefinitions(resource);
        if (actualResources != null) {
            actualResources.add(resource);
        }
        if (logger.isTraceEnabled()) {
            logger.trace("Loaded " + count + " bean definitions from location [" + location + "]");
        }
        return count;
    }
}
```



###### 2.3.3.1 加载xml文件下的所有bean定义

```java
@Override
public int loadBeanDefinitions(Resource... resources) throws BeanDefinitionStoreException {
    Assert.notNull(resources, "Resource array must not be null");
    int count = 0;
    for (Resource resource : resources) {
        count += loadBeanDefinitions(resource);
    }
    return count;
}
```



从特定路径下的xml文件下加载bean定义

```java
/**
 * Load bean definitions from the specified XML file.
 * @param resource the resource descriptor for the XML file
 * @return the number of bean definitions found
 * @throws BeanDefinitionStoreException in case of loading or parsing errors
 */
@Override
public int loadBeanDefinitions(Resource resource) throws BeanDefinitionStoreException {
    return loadBeanDefinitions(new EncodedResource(resource));
}


/**
 * Load bean definitions from the specified XML file.
 * @param encodedResource the resource descriptor for the XML file,
 * allowing to specify an encoding to use for parsing the file
 * @return the number of bean definitions found
 * @throws BeanDefinitionStoreException in case of loading or parsing errors
 */
public int loadBeanDefinitions(EncodedResource encodedResource) throws BeanDefinitionStoreException {
    Assert.notNull(encodedResource, "EncodedResource must not be null");
    if (logger.isTraceEnabled()) {
        logger.trace("Loading XML bean definitions from " + encodedResource);
    }

    Set<EncodedResource> currentResources = this.resourcesCurrentlyBeingLoaded.get();

    if (!currentResources.add(encodedResource)) {
        throw new BeanDefinitionStoreException(
                "Detected cyclic loading of " + encodedResource + " - check your import definitions!");
    }

    try (InputStream inputStream = encodedResource.getResource().getInputStream()) {
        InputSource inputSource = new InputSource(inputStream);
        if (encodedResource.getEncoding() != null) {
            inputSource.setEncoding(encodedResource.getEncoding());
        }
        // 执行加载bean定义
        return doLoadBeanDefinitions(inputSource, encodedResource.getResource());
    }
    catch (IOException ex) {
        throw new BeanDefinitionStoreException(
                "IOException parsing XML document from " + encodedResource.getResource(), ex);
    }
    finally {
        currentResources.remove(encodedResource);
        if (currentResources.isEmpty()) {
            this.resourcesCurrentlyBeingLoaded.remove();
        }
    }
}
```





```java
/**
 * Actually load bean definitions from the specified XML file.
 * @param inputSource the SAX InputSource to read from
 * @param resource the resource descriptor for the XML file
 * @return the number of bean definitions found
 * @throws BeanDefinitionStoreException in case of loading or parsing errors
 * @see #doLoadDocument
 * @see #registerBeanDefinitions
 */
protected int doLoadBeanDefinitions(InputSource inputSource, Resource resource)
        throws BeanDefinitionStoreException {

    try {
        // 解析xml文件，封装成Document对象
        Document doc = doLoadDocument(inputSource, resource);        
        int count = registerBeanDefinitions(doc, resource);
        if (logger.isDebugEnabled()) {
            logger.debug("Loaded " + count + " bean definitions from " + resource);
        }
        return count;
    }
    catch (BeanDefinitionStoreException ex) {
        throw ex;
    }
    catch (SAXParseException ex) {
        throw new XmlBeanDefinitionStoreException(resource.getDescription(),
                "Line " + ex.getLineNumber() + " in XML document from " + resource + " is invalid", ex);
    }
    catch (SAXException ex) {
        throw new XmlBeanDefinitionStoreException(resource.getDescription(),
                "XML document from " + resource + " is invalid", ex);
    }
    catch (ParserConfigurationException ex) {
        throw new BeanDefinitionStoreException(resource.getDescription(),
                "Parser configuration exception parsing XML from " + resource, ex);
    }
    catch (IOException ex) {
        throw new BeanDefinitionStoreException(resource.getDescription(),
                "IOException parsing XML document from " + resource, ex);
    }
    catch (Throwable ex) {
        throw new BeanDefinitionStoreException(resource.getDescription(),
                "Unexpected exception parsing XML document from " + resource, ex);
    }
}
```



通过Document注册bean定义

```java
/**
* Register the bean definitions contained in the given DOM document.
* Called by {@code loadBeanDefinitions}.
* <p>Creates a new instance of the parser class and invokes
* {@code registerBeanDefinitions} on it.
* @param doc the DOM document
* @param resource the resource descriptor (for context information)
* @return the number of bean definitions found
* @throws BeanDefinitionStoreException in case of parsing errors
* @see #loadBeanDefinitions
* @see #setDocumentReaderClass
* @see BeanDefinitionDocumentReader#registerBeanDefinitions
*/
public int registerBeanDefinitions(Document doc, Resource resource) throws BeanDefinitionStoreException {
    BeanDefinitionDocumentReader documentReader = createBeanDefinitionDocumentReader();
    int countBefore = getRegistry().getBeanDefinitionCount();
    documentReader.registerBeanDefinitions(doc, createReaderContext(resource));
    return getRegistry().getBeanDefinitionCount() - countBefore;
}
```





#### 2.3.4 处理ImportBeanDefinitionRegistrar

获取配置类通过@Import注解引入的所有ImportBeanDefinitionRegistrar，遍历ImportBeanDefinitionRegistrar，执行ImportBeanDefinitionRegistrar的registerBeanDefinitions方法，具体的加载bean逻辑，在不同的ImportBeanDefinitionRegistrar中。

```java
// loadBeanDefinitionsFromRegistrars(configClass.getImportBeanDefinitionRegistrars());
private void loadBeanDefinitionsFromRegistrars(Map<ImportBeanDefinitionRegistrar, AnnotationMetadata> registrars) {
    registrars.forEach((registrar, metadata) ->
            registrar.registerBeanDefinitions(metadata, this.registry, this.importBeanNameGenerator));
}
```



## 三、判断bean定义是否属于配置类

```java
	/**
	 * Check whether the given bean definition is a candidate for a configuration class
	 * (or a nested component class declared within a configuration/component class,
	 * to be auto-registered as well), and mark it accordingly.
	 * @param beanDef the bean definition to check
	 * @param metadataReaderFactory the current factory in use by the caller
	 * @return whether the candidate qualifies as (any kind of) configuration class
	 */
	public static boolean checkConfigurationClassCandidate(
			BeanDefinition beanDef, MetadataReaderFactory metadataReaderFactory) {
		// 获取类名
		String className = beanDef.getBeanClassName();
        // 如果类目和工厂方法名不存在，则说明不是配置类（@Bean标注的方法为工厂方法）
		if (className == null || beanDef.getFactoryMethodName() != null) {
			return false;
		}		
         
		AnnotationMetadata metadata;
         // 如果bean定义是注解标注类型的bean定义，而且类名和注解元数据获取的类名一致
		if (beanDef instanceof AnnotatedBeanDefinition &&
				className.equals(((AnnotatedBeanDefinition) beanDef).getMetadata().getClassName())) {
			// Can reuse the pre-parsed metadata from the given BeanDefinition...
             // spring主启动类的bean定义属于注解标注类型的
			metadata = ((AnnotatedBeanDefinition) beanDef).getMetadata();
		}
		else if (beanDef instanceof AbstractBeanDefinition && ((AbstractBeanDefinition) beanDef).hasBeanClass()) {
			// Check already loaded Class if present...
			// since we possibly can't even load the class file for this Class.
             // 如果是这4个类型的类，就说明不是配置类
			Class<?> beanClass = ((AbstractBeanDefinition) beanDef).getBeanClass();
			if (BeanFactoryPostProcessor.class.isAssignableFrom(beanClass) ||
					BeanPostProcessor.class.isAssignableFrom(beanClass) ||
					AopInfrastructureBean.class.isAssignableFrom(beanClass) ||
					EventListenerFactory.class.isAssignableFrom(beanClass)) {
				return false;
			}
			metadata = AnnotationMetadata.introspect(beanClass);
		}
		else {
			try {
				MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(className);
				metadata = metadataReader.getAnnotationMetadata();
			}
			catch (IOException ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Could not find class file for introspecting configuration annotations: " +
							className, ex);
				}
				return false;
			}
		}
		
         // 从注解元数据获取@Configuration注解的属性.
         // 通过isConfigurationCandidate(metadata)判断是否属于配置类。
		Map<String, Object> config = metadata.getAnnotationAttributes(Configuration.class.getName());
		if (config != null && !Boolean.FALSE.equals(config.get("proxyBeanMethods"))) {
			beanDef.setAttribute(CONFIGURATION_CLASS_ATTRIBUTE, CONFIGURATION_CLASS_FULL);
		}
		else if (config != null || isConfigurationCandidate(metadata)) {
			beanDef.setAttribute(CONFIGURATION_CLASS_ATTRIBUTE, CONFIGURATION_CLASS_LITE);
		}
		else {
			return false;
		}
		
         // 获取@Order的value值，然后设置到bean定义中
		// It's a full or lite configuration candidate... Let's determine the order value, if any.
		Integer order = getOrder(metadata);
		if (order != null) {
			beanDef.setAttribute(ORDER_ATTRIBUTE, order);
		}

         // 执行到这里说明是配置类，返回true
		return true;
	}
```



通过类的元数据判断该类是否为配置类

```java
/**
 * Check the given metadata for a configuration class candidate
 * (or nested component class declared within a configuration/component class).
 * @param metadata the metadata of the annotated class
 * @return {@code true} if the given class is to be registered for
 * configuration class processing; {@code false} otherwise
 */
public static boolean isConfigurationCandidate(AnnotationMetadata metadata) {
         // 如果是接口，直接返回false，说明不是配置类 
		// Do not consider an interface or an annotation...
		if (metadata.isInterface()) {
			return false;
		}
		
         /*
         是否有@Component、@ComponentScan、@Import、@ImportResource注解
         如果有，返回true，说明属于配置类。
         @Controller、@Service、@Repository、@Configuration注解是基于@Component注解的，也符合情况。
         */        
		// Any of the typical annotations found?
		for (String indicator : candidateIndicators) {
			if (metadata.isAnnotated(indicator)) {
				return true;
			}
		}
		
    	 // 如果前面的情况都不满足，查看是否有@Bean注解标注的方法。
         //  如果有，说明属于配置类.没有，说明不属于。
		// Finally, let's look for @Bean methods...
		return hasBeanMethods(metadata);
	}
private static final Set<String> candidateIndicators = new HashSet<>(8);

static {
    candidateIndicators.add(Component.class.getName());
    candidateIndicators.add(ComponentScan.class.getName());
    candidateIndicators.add(Import.class.getName());
    candidateIndicators.add(ImportResource.class.getName());
}
```

总结：

* 接口不属于配置类

* BeanFactoryPostProcessor、BeanPostProcessor、AopInfrastructureBean、EventListenerFactory这4种类型的类不属于配置类
* 标注@Configuration注解的属于配置类
* 标注@Component、@ComponentScan、@Import、@ImportResource注解，或者标注了基于这4类注解的注解属于配置类
* 含有@Bean标注的方法属于配置类（这里要和从主启动类下面扫描的类区分，从主启动类扫描路径下扫描的类不是通过这个方法来判断的）



补充：含有@Bean标注的方法属于配置类 的情况说明

* 情况一

在xml文件中配置具体的bean。（记得通过@ImportResource指定xml文件的位置）

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
           http://www.springframework.org/schema/beans/spring-beans-3.0.xsd">

    <bean id="tagDemoConfig" class="com.tag.TagDemoConfig">
    </bean>

</beans>
```

为了验证是否生效，我们不把TagDemoConfig的类创建在主启动类的扫描路径下。

```java
// 为什么不用@Configuration也可以呢？因为我们再xml文件通过bean标签配置了这个类，而且类里有@Bean方法，就会被认为是配置类。
//@Configuration
public class TagDemoConfig {

    @Bean
    public Demo tagDemo() {
        return new Demo();
    }
}
```

当加载完xml文件的时候，spring会判断TagDemoConfig类的bean定义是否是配置类，然后来决定是否按处理配置类的方式处理TagDemoConfig。

最终就是通过isConfigurationCandidate(AnnotationMetadata metadata)来判断是否是配置类。



* 情况二

内部配置类，也是通过这个判断的。在任意一个配置类里面加入以下内部类，发现NestedConfigWithoutConfigAnnotation都会被判断为配置类，因为它有单独的@Bean方法。

```java
// 测试判断内部配置类是否为配置类
public static class NestedConfigWithoutConfigAnnotation {
    @Bean
    public Demo nest() {
        return new Demo();
    }
}
```



## 四、扫描bean判断

ClassPathScanningCandidateComponentProvider

### 4.1 根据注解元数据判断是否为候选的bean

```java
/**
 * Determine whether the given class does not match any exclude filter
 * and does match at least one include filter.
 * @param metadataReader the ASM ClassReader for the class
 * @return whether the class qualifies as a candidate component
 */
protected boolean isCandidateComponent(MetadataReader metadataReader) throws IOException {
    // 如果有一个excludeFilter满足条件，则直接返回false，说明不符合条件
    // 这里我们在2.1.2.3 的3、设置过滤器，里面加了一个过滤配置类自身的过滤器。通过类名来判断
    for (TypeFilter tf : this.excludeFilters) {
        if (tf.match(metadataReader, getMetadataReaderFactory())) {
            return false;
        }
    }
    
    /**
    在2.1.2.3处理@ComponentScan注解的1、创建类路径bean定义扫描器的时候，
    会创建@Component注解和@ManagedBean注解AnnotationTypeFilter过滤器（因为我们类路径有@ManagedBean注解存在，所以会创建）
    */
    for (TypeFilter tf : this.includeFilters) {
        // 如果有一个includeFilter满足条件，则判断是否有@Conditional注解(或者基于@Conditional注解的注解)
        // 然后判断是否满足@Conditional注解(或者基于@Conditional注解的注解)的条件，如果满足，说明为候选条件的bean。
        // 如果没有@Conditional注解(或者基于@Conditional注解的注解)，符合includeFilter即可。
        if (tf.match(metadataReader, getMetadataReaderFactory())) {
            return isConditionMatch(metadataReader);
        }
    }
    
    // 不满足所有includeFilters，说明不是候选的bean
    return false;
}

// 过滤配置类自身的过滤器
new AbstractTypeHierarchyTraversingFilter(false, false) {
        @Override
        protected boolean matchClassName(String className) {
            return declaringClass.equals(className);
        }
}

private boolean isConditionMatch(MetadataReader metadataReader) {
    if (this.conditionEvaluator == null) {
        this.conditionEvaluator =
                new ConditionEvaluator(getRegistry(), this.environment, this.resourcePatternResolver);
    }
    // 同【2.1.1是否跳过配置类】是一个逻辑
    return !this.conditionEvaluator.shouldSkip(metadataReader.getAnnotationMetadata());
}
```

【注意】

无论是excludeFilter还是includeFilter，都会调用抽象类AbstractTypeHierarchyTraversingFilter的match方法，然后match方法里面定义了通用的判断流程，如里面的matchSelf、matchClassName方法。会根据实际的excludeFilter和includeFilter是否重写了matchSelf、matchClassName方法来调用，实际上采用了**模板方法的设计模式**。



### 4.2  AbstractTypeHierarchyTraversingFilter的match方法

```java
protected boolean matchSelf(MetadataReader metadataReader) {
    return false;
}
/**
 * Override this to match on type name.
 */
protected boolean matchClassName(String className) {
    return false;
}

@Override
public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory)
        throws IOException {    
    
    // This method optimizes avoiding unnecessary creation of ClassReaders
    // as well as visiting over those readers.
    // matchSelf默认返回false，过滤配置类自身的过滤器没有重写该方法
    if (matchSelf(metadataReader)) {
        return true;
    }
    ClassMetadata metadata = metadataReader.getClassMetadata();
    // 过滤配置类自身的过滤器重写该方法
    if (matchClassName(metadata.getClassName())) {
        return true;
    }
    
    // 默认false，不考虑父类
    if (this.considerInherited) {
        String superClassName = metadata.getSuperClassName();
        if (superClassName != null) {
            // Optimization to avoid creating ClassReader for super class.
            Boolean superClassMatch = matchSuperClass(superClassName);
            if (superClassMatch != null) {
                if (superClassMatch.booleanValue()) {
                    return true;
                }
            }
            else {
                // Need to read super class to determine a match...
                try {
                    if (match(metadata.getSuperClassName(), metadataReaderFactory)) {
                        return true;
                    }
                }
                catch (IOException ex) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Could not read super class [" + metadata.getSuperClassName() +
                                "] of type-filtered class [" + metadata.getClassName() + "]");
                    }
                }
            }
        }
    }
    
    // 默认false，不考虑继承或者实现的接口
    if (this.considerInterfaces) {
        for (String ifc : metadata.getInterfaceNames()) {
            // Optimization to avoid creating ClassReader for super class
            Boolean interfaceMatch = matchInterface(ifc);
            if (interfaceMatch != null) {
                if (interfaceMatch.booleanValue()) {
                    return true;
                }
            }
            else {
                // Need to read interface to determine a match...
                try {
                    if (match(ifc, metadataReaderFactory)) {
                        return true;
                    }
                }
                catch (IOException ex) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Could not read interface [" + ifc + "] for type-filtered class [" +
                                metadata.getClassName() + "]");
                    }
                }
            }
        }
    }

    return false;
}
```



### 4.3 AnnotationTypeFilter

对于includeFilters中的@Component注解和@ManagedBean注解AnnotationTypeFilter过滤器。只需考虑AnnotationTypeFilter过滤器的

```java
@Override
protected boolean matchSelf(MetadataReader metadataReader) {
    AnnotationMetadata metadata = metadataReader.getAnnotationMetadata();
    
    // metadata.hasAnnotation(this.annotationType.getName()) (类的)注解元数据是否有对应annotationType类型的注解
    // 如果是@Component注解的AnnotationTypeFilter，annotationType就是@Component
    // 如果是@ManagedBean注解的AnnotationTypeFilter，annotationType就是@ManagedBean
    
    // considerMetaAnnotations参数在创建AnnotationTypeFilter时候，就设置了true，表示考虑元注解(基于注解的注解)
    // 基于注解的注解，是否包含了annotationType类型的注解。如@Service，@Controller等包含@Component注解，就属于符合条件的元注解
    return metadata.hasAnnotation(this.annotationType.getName()) ||
            (this.considerMetaAnnotations && metadata.hasMetaAnnotation(this.annotationType.getName()));
}
```

总结：判断类上面是否有@Component、@ManagedBean注解，或者是基于这些注解的注解，如果有，说明符合条件。



### 4.4  根据bean定义判断是否为候选的bean

  总结：

* 如果是具体独立的类，则符合条件
* 如果是抽象独立的类，而且有@Lookup注解标注的方法，则符合条件
* 如果是接口，不符合条件

在扫描bean的时候，**通常这个方法不会独立调用，只有当4.1处的方法返回true的时候，才会调用**。

```java
/**
 * 确定给定的bean定义是否符合候选条件。
 * 默认实现检查类是否不是接口，是否依赖于封闭类。
 * 可以在子类中重写。
 * Determine whether the given bean definition qualifies as candidate.
 * <p>The default implementation checks whether the class is not an interface
 * and not dependent on an enclosing class.
 * <p>Can be overridden in subclasses.
 * @param beanDefinition the bean definition to check
 * @return whether the bean definition qualifies as a candidate component
 */
protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
    AnnotationMetadata metadata = beanDefinition.getMetadata();
    
    /**
    metadata.isIndependent() 判断这个类是不是一个顶层类，或者是静态内部类。true说明是
    metadata.isConcrete()：true表示这个类既不是接口，也不是抽象类，是一个具体的类
    
    metadata.isAbstract() && metadata.hasAnnotatedMethods(Lookup.class.getName()) 
    如果这个类是抽象类，获取抽象类上@Lookup注解标注的方法，如果存在，则这个表达式为true 
       
    @Lookup注解标注的方法，如果@Lookup注解设置了value值，则根据value的值，去ioc中获取bean
    如果没有设置，根据方法的返回值，去ioc中获取bean，如果找到多个bean就会抛异常。
    
    总结:
       如果是具体独立的类，则符合条件
       如果是抽象独立的类，而且有@Lookup注解标注的方法，则符合条件
       如果是接口，不符合条件
    */ 
    return (metadata.isIndependent() && (metadata.isConcrete() ||
            (metadata.isAbstract() && metadata.hasAnnotatedMethods(Lookup.class.getName()))));
}

/**
 * 确定底层类是否独立，即它是一个顶层类还是一个可以独立于封闭类构造的嵌套类(静态内部类)。
 * Determine whether the underlying class is independent, i.e. whether
 * it is a top-level class or a nested class (static inner class) that
 * can be constructed independently from an enclosing class.
 */
@Override
public boolean isIndependent() {
    // independentInnerClass: false有非静态内部类
    return (this.enclosingClassName == null || this.independentInnerClass);
}
```





## 五、补充

### 5.1 默认的bean名称生成规则（扫描到的bean）

AnnotationBeanNameGenerator

```java

/**
BeanNameGenerator implementation for bean classes annotated with the @Component annotation or with another annotation that is itself annotated with @Component as a meta-annotation. For example, Spring's stereotype annotations (such as @Repository) are themselves annotated with @Component.
Also supports Java EE 6's javax.annotation.ManagedBean and JSR-330's javax.inject.Named annotations, if available. Note that Spring component annotations always override such standard annotations.
If the annotation's value doesn't indicate a bean name, an appropriate name will be built based on the short name of the class (with the first letter lower-cased). For example:
com.xyz.FooServiceImpl -> fooServiceImpl
Since:
2.5
See Also:
org.springframework.stereotype.Component.value(), org.springframework.stereotype.Repository.value(), org.springframework.stereotype.Service.value(), org.springframework.stereotype.Controller.value(), javax.inject.Named.value(), FullyQualifiedAnnotationBeanNameGenerator
Author:
Juergen Hoeller, Mark Fisher
*/
public class AnnotationBeanNameGenerator implements BeanNameGenerator {

	/**
	 * A convenient constant for a default {@code AnnotationBeanNameGenerator} instance,
	 * as used for component scanning purposes.
	 * @since 5.2
	 */
	public static final AnnotationBeanNameGenerator INSTANCE = new AnnotationBeanNameGenerator();

	private static final String COMPONENT_ANNOTATION_CLASSNAME = "org.springframework.stereotype.Component";

	private final Map<String, Set<String>> metaAnnotationTypesCache = new ConcurrentHashMap<>();


	@Override
	public String generateBeanName(BeanDefinition definition, BeanDefinitionRegistry registry) {     
		if (definition instanceof AnnotatedBeanDefinition) {
            // 如果bean定义是注解标注的bean定义，则从注解中确定bean名称
			String beanName = determineBeanNameFromAnnotation((AnnotatedBeanDefinition) definition);
			if (StringUtils.hasText(beanName)) {  
                 // 如果bean名称不为空，则直接返回，否则执行生成默认的bean名称逻辑
				// Explicit bean name found.
				return beanName;
			}
		}
         // 生成默认的bean名称
		// Fallback: generate a unique default bean name.
		return buildDefaultBeanName(definition, registry);
	}

	/**
	 * 从注解中确定bean名称
	 * Derive a bean name from one of the annotations on the class.
	 * @param annotatedDef the annotation-aware bean definition
	 * @return the bean name, or {@code null} if none is found
	 */
	@Nullable
	protected String determineBeanNameFromAnnotation(AnnotatedBeanDefinition annotatedDef) {
		AnnotationMetadata amd = annotatedDef.getMetadata();、
         // 获取注解类型   
		Set<String> types = amd.getAnnotationTypes();
		String beanName = null;
		for (String type : types) {
             // 获取注解的属性值
			AnnotationAttributes attributes = AnnotationConfigUtils.attributesFor(amd, type);
			if (attributes != null) {
                  // 获取注解上标注的所有注解类型
				Set<String> metaTypes = this.metaAnnotationTypesCache.computeIfAbsent(type, key -> {
					Set<String> result = amd.getMetaAnnotationTypes(key);
					return (result.isEmpty() ? Collections.emptySet() : result);
				});
                  // 是否是符合条件的注解 
				if (isStereotypeWithNameValue(type, metaTypes, attributes)) {
                      // 获取注解的value属性
					Object value = attributes.get("value");
					if (value instanceof String) {
						String strVal = (String) value;
						if (StringUtils.hasLength(strVal)) {
                               // 如果value属性不为空和空串，就使用作为bean名称
							if (beanName != null && !strVal.equals(beanName)) {
                                   // 多个符合条件的注解，设置了不同的value就会报错，因为要保证bean名称唯一
								throw new IllegalStateException("Stereotype annotations suggest inconsistent " +
										"component names: '" + beanName + "' versus '" + strVal + "'");
							}
							beanName = strVal;
						}
					}
				}
			}
		}
		return beanName;
	}

	/**
	 *  是否是符合条件的注解 
	 * Check whether the given annotation is a stereotype that is allowed
	 * to suggest a component name through its annotation {@code value()}.
	 * @param annotationType the name of the annotation class to check
	 * @param metaAnnotationTypes the names of meta-annotations on the given annotation
	 * @param attributes the map of attributes for the given annotation
	 * @return whether the annotation qualifies as a stereotype with component name
	 */
	protected boolean isStereotypeWithNameValue(String annotationType,
			Set<String> metaAnnotationTypes, @Nullable Map<String, Object> attributes) {
		// 是否是@Component注解，或者注解是基于@Component注解的
         // 是否是@ManagedBean注解
         // 是否是@Named注解
         // 如果满足以上三种情况，说明是策略类型
		boolean isStereotype = annotationType.equals(COMPONENT_ANNOTATION_CLASSNAME) ||
				metaAnnotationTypes.contains(COMPONENT_ANNOTATION_CLASSNAME) ||
				annotationType.equals("javax.annotation.ManagedBean") ||
				annotationType.equals("javax.inject.Named");
		// 如果是策略类型，而且注解的属性不为空和有value属性，说明是符合条件的注解
		return (isStereotype && attributes != null && attributes.containsKey("value"));
	}

	/**
	 *  生成默认的bean名称
	 * Derive a default bean name from the given bean definition.
	 * <p>The default implementation delegates to {@link #buildDefaultBeanName(BeanDefinition)}.
	 * @param definition the bean definition to build a bean name for
	 * @param registry the registry that the given bean definition is being registered with
	 * @return the default bean name (never {@code null})
	 */
	protected String buildDefaultBeanName(BeanDefinition definition, BeanDefinitionRegistry registry) {
		return buildDefaultBeanName(definition);
	}

	/**
	 *  生成默认的bean名称
	 * Derive a default bean name from the given bean definition.
	 * <p>The default implementation simply builds a decapitalized version
	 * of the short class name: e.g. "mypackage.MyJdbcDao" &rarr; "myJdbcDao".
	 * <p>Note that inner classes will thus have names of the form
	 * "outerClassName.InnerClassName", which because of the period in the
	 * name may be an issue if you are autowiring by name.
	 * @param definition the bean definition to build a bean name for
	 * @return the default bean name (never {@code null})
	 */
	protected String buildDefaultBeanName(BeanDefinition definition) {
         // 获取bean的全类名
		String beanClassName = definition.getBeanClassName();
		Assert.state(beanClassName != null, "No bean class name set");
         // 获取短类名
		String shortClassName = ClassUtils.getShortName(beanClassName);
         // 优化短类名
		return Introspector.decapitalize(shortClassName);
	}

}


/**
 * Return the current bean class name of this bean definition.
 */
@Override
@Nullable
public String getBeanClassName() {
    Object beanClassObject = this.beanClass;
    if (beanClassObject instanceof Class) {
        return ((Class<?>) beanClassObject).getName();
    }
    else {
        return (String) beanClassObject;
    }
}


/**
 * Get the class name without the qualified package name.
 * @param className the className to get the short name for
 * @return the class name of the class without the package name
 * @throws IllegalArgumentException if the className is empty
 */
public static String getShortName(String className) {
    Assert.hasLength(className, "Class name must not be empty");
    // private static final char PACKAGE_SEPARATOR = '.';
    int lastDotIndex = className.lastIndexOf(PACKAGE_SEPARATOR);
    // 	public static final String CGLIB_CLASS_SEPARATOR = "$$";
    int nameEndIndex = className.indexOf(CGLIB_CLASS_SEPARATOR);
    if (nameEndIndex == -1) {
        nameEndIndex = className.length();
    }
    String shortName = className.substring(lastDotIndex + 1, nameEndIndex);
    // 内部类的分割符 /** The nested class separator character: {@code '$'}. */
    // 如：com.linkcm.server.NanshanServerApplication$NestedConfig
	// private static final char NESTED_CLASS_SEPARATOR = '$';
    shortName = shortName.replace(NESTED_CLASS_SEPARATOR, PACKAGE_SEPARATOR);
    return shortName;
}


/**
 * Utility method to take a string and convert it to normal Java variable
 * name capitalization.  This normally means converting the first
 * character from upper case to lower case, but in the (unusual) special
 * case when there is more than one character and both the first and
 * second characters are upper case, we leave it alone.
 * <p>
 * Thus "FooBah" becomes "fooBah" and "X" becomes "x", but "URL" stays
 * as "URL".
 *
 * @param  name The string to be decapitalized.
 * @return  The decapitalized version of the string.
 */
public static String decapitalize(String name) {
    if (name == null || name.length() == 0) {
        return name;
    }
    // 如果name长度>1,而且首字符和第二个字符都是大写，直接返回
    if (name.length() > 1 && Character.isUpperCase(name.charAt(1)) &&
                    Character.isUpperCase(name.charAt(0))){
        return name;
    }
    // 否则首字母转小写返回
    char chars[] = name.toCharArray();
    chars[0] = Character.toLowerCase(chars[0]);
    return new String(chars);
}
```





### 5.2 ImportSelector导入的类的bean名称生成规则

* 通过ImportSelector导入的类使用全类名作为bean名称

```java
public class FullyQualifiedAnnotationBeanNameGenerator extends AnnotationBeanNameGenerator {

	/**
	 * A convenient constant for a default {@code FullyQualifiedAnnotationBeanNameGenerator}
	 * instance, as used for configuration-level import purposes.
	 * @since 5.2.11
	 */
	public static final FullyQualifiedAnnotationBeanNameGenerator INSTANCE =
			new FullyQualifiedAnnotationBeanNameGenerator();

	
	@Override
	protected String buildDefaultBeanName(BeanDefinition definition) {
		String beanClassName = definition.getBeanClassName();
		Assert.state(beanClassName != null, "No bean class name set");
		return beanClassName;
	}

}
```





## 六、总结

![bean定义扫描](https://lu-note.oss-cn-shenzhen.aliyuncs.com/notes/work/bean%E5%AE%9A%E4%B9%89%E6%89%AB%E6%8F%8F.png)
