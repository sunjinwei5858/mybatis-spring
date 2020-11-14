/**
 * Copyright 2010-2020 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mybatis.spring.mapper;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyResourceConfigurer;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Optional;

import static org.springframework.util.Assert.notNull;

/**
 * 重点：我们用自己的话总结一下BeanFactoryPostProcessor的执行时机（不管内置的还是程序员提供）————
 * Ⅰ：如果是直接实现BeanFactoryPostProcessor的类是在spring完成扫描类之后
 * （所谓的扫描包括把类变成beanDefinition然后put到map之中），在实例化bean（第⑤步）之前执行；
 *
 * Ⅱ：如果是实现BeanDefinitionRegistryPostProcessor接口的类；
 * 诚然这种也叫bean工厂后置处理器他的执行时机是在执行直接实现BeanFactoryPostProcessor的类之前，和扫描（上面①②③步）是同期执行；
 * 假设你的程序扩展一个功能，需要在这个时期做某个功能则可以实现这个接口；
 * 但是笔者至今没有遇到这样的需求，如果以后遇到或者看到再来补上；（
 * 说明一下当笔者发表这篇博客有一个读者联系笔者说他看到了一个主流框架就是扩展了BeanDefinitionRegistryPostProcessor类，
 * 瞬间笔者如久旱遇甘霖，光棍遇寡妇；遂立马请教；那位读者说mybatis的最新代码里面便是扩展这个类来实现的，
 * 笔者记得以前mybatis是扩展没有用到这个接口，后来笔者看了一下最新的mybatis源码确实如那位读者所言，
 * 在下一篇笔者分析主流框架如何扩展spring的时候更新）
 *
 *
 * 1。MapperScannerConfigurer 类实现了 BeanDefinitionRegistryPostProcessor接口，
 *  该接口中的postProcessBeanDefinitionRegistry()方法会在系统初始化的过程中被调用
 *  注意：
 *  之前的版本是实现BeanFactoryPostProcessor接口进行修改bean定义，现在改成了BeanDefinitionRegistryPostProcessor接口，
 *  其实BeanDefinitionRegistryPostProcessor继承了BeanFactoryPostProcessor接口
 *
 * 2。这里面会获取到在MapperScannerRegistrar已经注册好的bean定义对象中的propertyValues中的basePackage，将mapper包下面的接口注册为MapperFactoryBean
 *
 * 3。!!!!!
 *  MapperScannerConfigurer 这个类是如何注册到spring容器中去的呢？ 通过@MapperScan注解中的@Import注解导入一个实现了
 *  ImportBeanDefinitionRegistrar接口的MapperScannerRegistrar，
 *  重写了registerBeanDefinitions方法，然后将MapperScannerConfigurer这个类注册到bean定义中去的
 *
 * <p>
 * BeanDefinitionRegistryPostProcessor that searches recursively starting from a base package for interfaces and
 * registers them as {@code MapperFactoryBean}. Note that only interfaces with at least one method will be registered;
 * concrete classes will be ignored.
 * <p>
 * This class was a {code BeanFactoryPostProcessor} until 1.0.1 version. It changed to
 * {@code BeanDefinitionRegistryPostProcessor} in 1.0.2. See https://jira.springsource.org/browse/SPR-8269 for the
 * details.
 * <p>
 * The {@code basePackage} property can contain more than one package name, separated by either commas or semicolons.
 * <p>
 * This class supports filtering the mappers created by either specifying a marker interface or an annotation. The
 * {@code annotationClass} property specifies an annotation to search for. The {@code markerInterface} property
 * specifies a parent interface to search for. If both properties are specified, mappers are added for interfaces that
 * match <em>either</em> criteria. By default, these two properties are null, so all interfaces in the given
 * {@code basePackage} are added as mappers.
 * <p>
 * This configurer enables autowire for all the beans that it creates so that they are automatically autowired with the
 * proper {@code SqlSessionFactory} or {@code SqlSessionTemplate}. If there is more than one {@code SqlSessionFactory}
 * in the application, however, autowiring cannot be used. In this case you must explicitly specify either an
 * {@code SqlSessionFactory} or an {@code SqlSessionTemplate} to use via the <em>bean name</em> properties. Bean names
 * are used rather than actual objects because Spring does not initialize property placeholders until after this class
 * is processed.
 * <p>
 * Passing in an actual object which may require placeholders (i.e. DB user password) will fail. Using bean names defers
 * actual object creation until later in the startup process, after all placeholder substitution is completed. However,
 * note that this configurer does support property placeholders of its <em>own</em> properties. The
 * <code>basePackage</code> and bean name properties all support <code>${property}</code> style substitution.
 * <p>
 * Configuration sample:
 *
 * <pre class="code">
 * {@code
 *   <bean class="org.mybatis.spring.mapper.MapperScannerConfigurer">
 *       <property name="basePackage" value="org.mybatis.spring.sample.mapper" />
 *       <!-- optional unless there are multiple session factories defined -->
 *       <property name="sqlSessionFactoryBeanName" value="sqlSessionFactory" />
 *   </bean>
 * }
 * </pre>
 *
 * @author Hunter Presnall
 * @author Eduardo Macarron
 * @see MapperFactoryBean
 * @see ClassPathMapperScanner
 */
public class MapperScannerConfigurer
        implements BeanDefinitionRegistryPostProcessor, InitializingBean, ApplicationContextAware, BeanNameAware {

    /**
     * basePackage 配置包扫描的包名
     */
    private String basePackage;

    private boolean addToConfig = true;

    private String lazyInitialization;

    /**
     * sqlSessionFactory
     */
    private SqlSessionFactory sqlSessionFactory;

    /**
     * sqlSessionTemplate
     */
    private SqlSessionTemplate sqlSessionTemplate;

    private String sqlSessionFactoryBeanName;

    private String sqlSessionTemplateBeanName;

    private Class<? extends Annotation> annotationClass;

    private Class<?> markerInterface;

    /**
     * mapper接口没办法实例化 只能使用MapperFactoryBean
     */
    private Class<? extends MapperFactoryBean> mapperFactoryBeanClass;

    private ApplicationContext applicationContext;

    /**
     * beanName
     */
    private String beanName;

    private boolean processPropertyPlaceHolders;

    private BeanNameGenerator nameGenerator;

    private String defaultScope;

    /**
     * This property lets you set the base package for your mapper interface files.
     * <p>
     * You can set more than one package by using a semicolon or comma as a separator.
     * <p>
     * Mappers will be searched for recursively starting in the specified package(s).
     *
     * @param basePackage base package name
     */
    public void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
    }

    /**
     * Same as {@code MapperFactoryBean#setAddToConfig(boolean)}.
     *
     * @param addToConfig a flag that whether add mapper to MyBatis or not
     * @see MapperFactoryBean#setAddToConfig(boolean)
     */
    public void setAddToConfig(boolean addToConfig) {
        this.addToConfig = addToConfig;
    }

    /**
     * Set whether enable lazy initialization for mapper bean.
     * <p>
     * Default is {@code false}.
     * </p>
     *
     * @param lazyInitialization Set the @{code true} to enable
     * @since 2.0.2
     */
    public void setLazyInitialization(String lazyInitialization) {
        this.lazyInitialization = lazyInitialization;
    }

    /**
     * This property specifies the annotation that the scanner will search for.
     * <p>
     * The scanner will register all interfaces in the base package that also have the specified annotation.
     * <p>
     * Note this can be combined with markerInterface.
     *
     * @param annotationClass annotation class
     */
    public void setAnnotationClass(Class<? extends Annotation> annotationClass) {
        this.annotationClass = annotationClass;
    }

    /**
     * This property specifies the parent that the scanner will search for.
     * <p>
     * The scanner will register all interfaces in the base package that also have the specified interface class as a
     * parent.
     * <p>
     * Note this can be combined with annotationClass.
     *
     * @param superClass parent class
     */
    public void setMarkerInterface(Class<?> superClass) {
        this.markerInterface = superClass;
    }

    /**
     * Specifies which {@code SqlSessionTemplate} to use in the case that there is more than one in the spring context.
     * Usually this is only needed when you have more than one datasource.
     * <p>
     *
     * @param sqlSessionTemplate a template of SqlSession
     * @deprecated Use {@link #setSqlSessionTemplateBeanName(String)} instead
     */
    @Deprecated
    public void setSqlSessionTemplate(SqlSessionTemplate sqlSessionTemplate) {
        this.sqlSessionTemplate = sqlSessionTemplate;
    }

    /**
     * Specifies which {@code SqlSessionTemplate} to use in the case that there is more than one in the spring context.
     * Usually this is only needed when you have more than one datasource.
     * <p>
     * Note bean names are used, not bean references. This is because the scanner loads early during the start process and
     * it is too early to build mybatis object instances.
     *
     * @param sqlSessionTemplateName Bean name of the {@code SqlSessionTemplate}
     * @since 1.1.0
     */
    public void setSqlSessionTemplateBeanName(String sqlSessionTemplateName) {
        this.sqlSessionTemplateBeanName = sqlSessionTemplateName;
    }

    /**
     * Specifies which {@code SqlSessionFactory} to use in the case that there is more than one in the spring context.
     * Usually this is only needed when you have more than one datasource.
     * <p>
     *
     * @param sqlSessionFactory a factory of SqlSession
     * @deprecated Use {@link #setSqlSessionFactoryBeanName(String)} instead.
     */
    @Deprecated
    public void setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }

    /**
     * Specifies which {@code SqlSessionFactory} to use in the case that there is more than one in the spring context.
     * Usually this is only needed when you have more than one datasource.
     * <p>
     * Note bean names are used, not bean references. This is because the scanner loads early during the start process and
     * it is too early to build mybatis object instances.
     *
     * @param sqlSessionFactoryName Bean name of the {@code SqlSessionFactory}
     * @since 1.1.0
     */
    public void setSqlSessionFactoryBeanName(String sqlSessionFactoryName) {
        this.sqlSessionFactoryBeanName = sqlSessionFactoryName;
    }

    /**
     * Specifies a flag that whether execute a property placeholder processing or not.
     * <p>
     * The default is {@literal false}. This means that a property placeholder processing does not execute.
     *
     * @param processPropertyPlaceHolders a flag that whether execute a property placeholder processing or not
     * @since 1.1.1
     */
    public void setProcessPropertyPlaceHolders(boolean processPropertyPlaceHolders) {
        this.processPropertyPlaceHolders = processPropertyPlaceHolders;
    }

    /**
     * The class of the {@link MapperFactoryBean} to return a mybatis proxy as spring bean.
     *
     * @param mapperFactoryBeanClass The class of the MapperFactoryBean
     * @since 2.0.1
     */
    public void setMapperFactoryBeanClass(Class<? extends MapperFactoryBean> mapperFactoryBeanClass) {
        this.mapperFactoryBeanClass = mapperFactoryBeanClass;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setBeanName(String name) {
        this.beanName = name;
    }

    /**
     * Gets beanNameGenerator to be used while running the scanner.
     *
     * @return the beanNameGenerator BeanNameGenerator that has been configured
     * @since 1.2.0
     */
    public BeanNameGenerator getNameGenerator() {
        return nameGenerator;
    }

    /**
     * Sets beanNameGenerator to be used while running the scanner.
     *
     * @param nameGenerator the beanNameGenerator to set
     * @since 1.2.0
     */
    public void setNameGenerator(BeanNameGenerator nameGenerator) {
        this.nameGenerator = nameGenerator;
    }

    /**
     * Sets the default scope of scanned mappers.
     * <p>
     * Default is {@code null} (equiv to singleton).
     * </p>
     *
     * @param defaultScope the default scope
     * @since 2.0.6
     */
    public void setDefaultScope(String defaultScope) {
        this.defaultScope = defaultScope;
    }

    /**
     * 进行对@MapperScan注解里面的包进行判空
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        notNull(this.basePackage, "Property 'basePackage' is required");
        System.out.println("2--MapperScannerConfigurer--执行空的--afterPropertiesSet()");
    }

    /**
     *
     * 这里是实现了根接口的后置处理器BeanFactoryPostProcessor，在此处没有进行任何处理
     *
     * {@inheritDoc}
     */
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        // left intentionally blank
        System.out.println("2--MapperScannerConfigurer--BeanFactoryPostProcessors--执行空的--postProcessBeanFactory");
    }

    /**
     * 这里是实现了BeanDefinitionRegistryPostProcessor后置处理器接口，比BeanFactoryPostProcessor更先调用
     * MapperScannerConfigurer 类实现了 BeanDefinitionRegistryPostProcessor 接口，=====>
     * 系统初始化时会调用，在BeanFactoryPostProcessors类之前
     * 该接口中的 postProcessBeanDefinitionRegistry() 方法会在系统初始化的过程中被调用，
     * 该方法扫描了配置文件中配置的basePackage 下的所有 Mapper 类，最终生成 Spring 的 Bean 对象，注册到容器中。
     * {@inheritDoc}
     *
     * @since 1.0.2
     */
    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
        System.out.println("2--MapperScannerConfigurer--执行postProcessBeanDefinitionRegistry--begin");
        if (this.processPropertyPlaceHolders) {
            // 先进行相关的属性配置 主要是@MapperScan注解中的包路径进行赋值
            processPropertyPlaceHolders();
        }
        // 创建ClassPathMapperScanner对象
        ClassPathMapperScanner scanner = new ClassPathMapperScanner(registry);
        scanner.setAddToConfig(this.addToConfig);
        scanner.setAnnotationClass(this.annotationClass);
        scanner.setMarkerInterface(this.markerInterface);
        scanner.setSqlSessionFactory(this.sqlSessionFactory);
        scanner.setSqlSessionTemplate(this.sqlSessionTemplate);
        scanner.setSqlSessionFactoryBeanName(this.sqlSessionFactoryBeanName);
        scanner.setSqlSessionTemplateBeanName(this.sqlSessionTemplateBeanName);
        scanner.setResourceLoader(this.applicationContext);
        scanner.setBeanNameGenerator(this.nameGenerator);
        scanner.setMapperFactoryBeanClass(this.mapperFactoryBeanClass);
        if (StringUtils.hasText(lazyInitialization)) {
            scanner.setLazyInitialization(Boolean.valueOf(lazyInitialization));
        }
        if (StringUtils.hasText(defaultScope)) {
            scanner.setDefaultScope(defaultScope);
        }
        scanner.registerFilters();
        /**
         * 调用ClassPathMapperScanner的父类ClassPathBeanDefinitionScanner方法scan()进行扫描basePackage
         * 子类重写了doScan方法，进行修改bean定义，因为接口不能被实例化 beanClass进行偷天换日 换成MappperFactoryBean，牛逼的闪光点!!!!
         */
        scanner.scan(
                StringUtils.tokenizeToStringArray(this.basePackage, ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS));
    }

    /**
     * !!!!! 这段注释说明了bean定义注册会在系统初始化之前被调用，在BeanFactoryPostProcessors
     *
     * BeanDefinitionRegistries are called early in application startup, before BeanFactoryPostProcessors. This means that
     * PropertyResourceConfigurers will not have been loaded and any property substitution of this class' properties will
     * fail. To avoid this, find any PropertyResourceConfigurers defined in the context and run them on this class' bean
     * definition. Then update the values.
     */
    private void processPropertyPlaceHolders() {
        Map<String, PropertyResourceConfigurer> prcs = applicationContext.getBeansOfType(PropertyResourceConfigurer.class,
                false, false);

        if (!prcs.isEmpty() && applicationContext instanceof ConfigurableApplicationContext) {
            BeanDefinition mapperScannerBean = ((ConfigurableApplicationContext) applicationContext).getBeanFactory()
                    .getBeanDefinition(beanName);

            // PropertyResourceConfigurer does not expose any methods to explicitly perform
            // property placeholder substitution. Instead, create a BeanFactory that just
            // contains this mapper scanner and post process the factory.
            DefaultListableBeanFactory factory = new DefaultListableBeanFactory();

            factory.registerBeanDefinition(beanName, mapperScannerBean);

            for (PropertyResourceConfigurer prc : prcs.values()) {
                prc.postProcessBeanFactory(factory);
            }
            PropertyValues values = mapperScannerBean.getPropertyValues();
            this.basePackage = getPropertyValue("basePackage", values);
            this.sqlSessionFactoryBeanName = getPropertyValue("sqlSessionFactoryBeanName", values);
            this.sqlSessionTemplateBeanName = getPropertyValue("sqlSessionTemplateBeanName", values);
            this.lazyInitialization = getPropertyValue("lazyInitialization", values);
            this.defaultScope = getPropertyValue("defaultScope", values);
        }
        /**
         * 这里进行获取@MapperScan注解下的包
         */
        this.basePackage = Optional.ofNullable(this.basePackage).map(getEnvironment()::resolvePlaceholders).orElse(null);
        this.sqlSessionFactoryBeanName = Optional.ofNullable(this.sqlSessionFactoryBeanName)
                .map(getEnvironment()::resolvePlaceholders).orElse(null);
        this.sqlSessionTemplateBeanName = Optional.ofNullable(this.sqlSessionTemplateBeanName)
                .map(getEnvironment()::resolvePlaceholders).orElse(null);
        this.lazyInitialization = Optional.ofNullable(this.lazyInitialization).map(getEnvironment()::resolvePlaceholders)
                .orElse(null);
        this.defaultScope = Optional.ofNullable(this.defaultScope).map(getEnvironment()::resolvePlaceholders).orElse(null);
    }

    private Environment getEnvironment() {
        return this.applicationContext.getEnvironment();
    }

    private String getPropertyValue(String propertyName, PropertyValues values) {
        PropertyValue property = values.getPropertyValue(propertyName);

        if (property == null) {
            return null;
        }

        Object value = property.getValue();

        if (value == null) {
            return null;
        } else if (value instanceof String) {
            return value.toString();
        } else if (value instanceof TypedStringValue) {
            return ((TypedStringValue) value).getValue();
        } else {
            return null;
        }
    }

}
