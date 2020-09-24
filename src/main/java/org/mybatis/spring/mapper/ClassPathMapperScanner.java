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
import org.mybatis.logging.Logger;
import org.mybatis.logging.LoggerFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.aop.scope.ScopedProxyFactoryBean;
import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

/**
 * A {@link ClassPathBeanDefinitionScanner} that registers Mappers by {@code basePackage}, {@code annotationClass}, or
 * {@code markerInterface}. If an {@code annotationClass} and/or {@code markerInterface} is specified, only the
 * specified types will be searched (searching for all interfaces will be disabled).
 * <p>
 * This functionality was previously a private class of {@link MapperScannerConfigurer}, but was broken out in version
 * 1.2.0.
 *
 * @author Hunter Presnall
 * @author Eduardo Macarron
 * @see MapperFactoryBean
 * @since 1.2.0
 */
public class ClassPathMapperScanner extends ClassPathBeanDefinitionScanner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClassPathMapperScanner.class);

    private boolean addToConfig = true;

    private boolean lazyInitialization;

    private SqlSessionFactory sqlSessionFactory;

    private SqlSessionTemplate sqlSessionTemplate;

    private String sqlSessionTemplateBeanName;

    private String sqlSessionFactoryBeanName;

    private Class<? extends Annotation> annotationClass;

    private Class<?> markerInterface;

    private Class<? extends MapperFactoryBean> mapperFactoryBeanClass = MapperFactoryBean.class;

    private String defaultScope;

    public ClassPathMapperScanner(BeanDefinitionRegistry registry) {
        super(registry, false);
    }

    public void setAddToConfig(boolean addToConfig) {
        this.addToConfig = addToConfig;
    }

    public void setAnnotationClass(Class<? extends Annotation> annotationClass) {
        this.annotationClass = annotationClass;
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
    public void setLazyInitialization(boolean lazyInitialization) {
        this.lazyInitialization = lazyInitialization;
    }

    public void setMarkerInterface(Class<?> markerInterface) {
        this.markerInterface = markerInterface;
    }

    public void setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }

    public void setSqlSessionTemplate(SqlSessionTemplate sqlSessionTemplate) {
        this.sqlSessionTemplate = sqlSessionTemplate;
    }

    public void setSqlSessionTemplateBeanName(String sqlSessionTemplateBeanName) {
        this.sqlSessionTemplateBeanName = sqlSessionTemplateBeanName;
    }

    public void setSqlSessionFactoryBeanName(String sqlSessionFactoryBeanName) {
        this.sqlSessionFactoryBeanName = sqlSessionFactoryBeanName;
    }

    /**
     * @deprecated Since 2.0.1, Please use the {@link #setMapperFactoryBeanClass(Class)}.
     */
    @Deprecated
    public void setMapperFactoryBean(MapperFactoryBean<?> mapperFactoryBean) {
        this.mapperFactoryBeanClass = mapperFactoryBean == null ? MapperFactoryBean.class : mapperFactoryBean.getClass();
    }

    /**
     * Set the {@code MapperFactoryBean} class.
     *
     * @param mapperFactoryBeanClass the {@code MapperFactoryBean} class
     * @since 2.0.1
     */
    public void setMapperFactoryBeanClass(Class<? extends MapperFactoryBean> mapperFactoryBeanClass) {
        this.mapperFactoryBeanClass = mapperFactoryBeanClass == null ? MapperFactoryBean.class : mapperFactoryBeanClass;
    }

    /**
     * Set the default scope of scanned mappers.
     * <p>
     * Default is {@code null} (equiv to singleton).
     * </p>
     *
     * @param defaultScope the scope
     * @since 2.0.6
     */
    public void setDefaultScope(String defaultScope) {
        this.defaultScope = defaultScope;
    }

    /**
     * Configures parent scanner to search for the right interfaces. It can search for all interfaces or just for those
     * that extends a markerInterface or/and those annotated with the annotationClass
     */
    public void registerFilters() {
        boolean acceptAllInterfaces = true;

        // if specified, use the given annotation and / or marker interface
        if (this.annotationClass != null) {
            addIncludeFilter(new AnnotationTypeFilter(this.annotationClass));
            acceptAllInterfaces = false;
        }

        // override AssignableTypeFilter to ignore matches on the actual marker interface
        if (this.markerInterface != null) {
            addIncludeFilter(new AssignableTypeFilter(this.markerInterface) {
                @Override
                protected boolean matchClassName(String className) {
                    return false;
                }
            });
            acceptAllInterfaces = false;
        }

        if (acceptAllInterfaces) {
            // default include filter that accepts all classes
            addIncludeFilter((metadataReader, metadataReaderFactory) -> true);
        }

        // exclude package-info.java
        addExcludeFilter((metadataReader, metadataReaderFactory) -> {
            String className = metadataReader.getClassMetadata().getClassName();
            return className.endsWith("package-info");
        });
    }

    /**
     * 重写了父类的doScan()方法，这里将mapper接口的bean定义进行注册.MapperFactoryBeans
     * <p>
     * Calls the parent search that will search and register all the candidates.
     * Then the registered objects are post processed to set them as MapperFactoryBeans
     */
    @Override
    public Set<BeanDefinitionHolder> doScan(String... basePackages) {
        // 这里调用父类ClassPathBeanDefinitionScanner进行将mapper接口进行扫描生成代理对象
        Set<BeanDefinitionHolder> beanDefinitions = super.doScan(basePackages);

        if (beanDefinitions.isEmpty()) {
            LOGGER.warn(() -> "No MyBatis mapper was found in '" + Arrays.toString(basePackages)
                    + "' package. Please check your configuration.");
        } else {
            // 处理扫描得到的BeanDefinitionHolder集合
            processBeanDefinitions(beanDefinitions);
        }

        return beanDefinitions; // (userMapper, MapperFactoryBean)
    }

    /**
     * 对bean定义的属性做一些处理
     *
     * @param beanDefinitions
     */
    private void processBeanDefinitions(Set<BeanDefinitionHolder> beanDefinitions) {
        AbstractBeanDefinition definition;
        BeanDefinitionRegistry registry = getRegistry();

        for (BeanDefinitionHolder holder : beanDefinitions) {
            definition = (AbstractBeanDefinition) holder.getBeanDefinition();
            boolean scopedProxy = false;
            if (ScopedProxyFactoryBean.class.getName().equals(definition.getBeanClassName())) {
                definition = (AbstractBeanDefinition) Optional
                        .ofNullable(((RootBeanDefinition) definition).getDecoratedDefinition())
                        .map(BeanDefinitionHolder::getBeanDefinition).orElseThrow(() -> new IllegalStateException(
                                "The target bean definition of scoped proxy bean not found. Root bean definition[" + holder + "]"));
                scopedProxy = true;
            }
            String beanClassName = definition.getBeanClassName();
            LOGGER.debug(() -> "Creating MapperFactoryBean with name '" + holder.getBeanName() + "' and '" + beanClassName
                    + "' mapperInterface");

            // the mapper interface is the original class of the bean
            // but, the actual class of the bean is MapperFactoryBean
            /**
             * !!!!!修改构造器
             * 设置definition构造器的输入参数为definition.getBeanClassName()，这里就是org.mybatis.test.mapper
             * 那么在getBean实例化时，通过反射调用构造器实例化时要将这个参数传进去
             */
            definition.getConstructorArgumentValues().addGenericArgumentValue(beanClassName); // issue #59

            /**
             * !!!!!修改BeanClass
             * https://blog.csdn.net/weixin_45714179/article/details/103026370?utm_medium=distribute.pc_relevant.none-task-blog-title-2&spm=1001.2101.3001.4242
             * 这里非常关键！！！ 在这里进行偷天换日 因为MapperFactoryBean实现了FactoryBean，因为mapper接口不能被实例化
             * 修改definition对应的Class
             * 看过Spring源码的都知道，getBean()返回的就是BeanDefinitionHolder中beanClass属性对应的实例
             * 所以我们后面ac.getBean(UserMapper.class)的返回值也就是MapperFactoryBean的实例
             * 但是最终被注入到Spring容器的对象的并不是MapperFactoryBean的实例，根据名字看，我们就知道MapperFactoryBean实现了FactoryBean接口
             * 那么最终注入Spring容器的必定是从MapperFactoryBean的实例的getObject()方法中返回
             */
            definition.setBeanClass(this.mapperFactoryBeanClass);
            definition.getPropertyValues().add("addToConfig", this.addToConfig);

            boolean explicitFactoryUsed = false;
            if (StringUtils.hasText(this.sqlSessionFactoryBeanName)) {
                definition.getPropertyValues().add("sqlSessionFactory",
                        new RuntimeBeanReference(this.sqlSessionFactoryBeanName));
                explicitFactoryUsed = true;
            } else if (this.sqlSessionFactory != null) {
                //往definition设置属性值sqlSessionFactory，那么在MapperFactoryBean实例化后，
                //进行属性赋值时populateBean(beanName, mbd, instanceWrapper);，会通过反射调用sqlSessionFactory的set方法进行赋值
                //也就是在MapperFactoryBean创建实例后，要调用setSqlSessionFactory方法将sqlSessionFactory注入进MapperFactoryBean实例
                definition.getPropertyValues().add("sqlSessionFactory", this.sqlSessionFactory);
                explicitFactoryUsed = true;
            }

            if (StringUtils.hasText(this.sqlSessionTemplateBeanName)) {
                if (explicitFactoryUsed) {
                    LOGGER.warn(
                            () -> "Cannot use both: sqlSessionTemplate and sqlSessionFactory together. sqlSessionFactory is ignored.");
                }
                definition.getPropertyValues().add("sqlSessionTemplate",
                        new RuntimeBeanReference(this.sqlSessionTemplateBeanName));
                explicitFactoryUsed = true;
            } else if (this.sqlSessionTemplate != null) {
                if (explicitFactoryUsed) {
                    LOGGER.warn(
                            () -> "Cannot use both: sqlSessionTemplate and sqlSessionFactory together. sqlSessionFactory is ignored.");
                }
                definition.getPropertyValues().add("sqlSessionTemplate", this.sqlSessionTemplate);
                explicitFactoryUsed = true;
            }

            if (!explicitFactoryUsed) {
                LOGGER.debug(() -> "Enabling autowire by type for MapperFactoryBean with name '" + holder.getBeanName() + "'.");
                definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
            }

            definition.setLazyInit(lazyInitialization);

            if (scopedProxy) {
                continue;
            }

            if (ConfigurableBeanFactory.SCOPE_SINGLETON.equals(definition.getScope()) && defaultScope != null) {
                definition.setScope(defaultScope);
            }
            // 如果不是单例
            if (!definition.isSingleton()) {
                BeanDefinitionHolder proxyHolder = ScopedProxyUtils.createScopedProxy(holder, registry, true);
                if (registry.containsBeanDefinition(proxyHolder.getBeanName())) {
                    registry.removeBeanDefinition(proxyHolder.getBeanName());
                }
                registry.registerBeanDefinition(proxyHolder.getBeanName(), proxyHolder.getBeanDefinition());
            }

        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
        return beanDefinition.getMetadata().isInterface() && beanDefinition.getMetadata().isIndependent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean checkCandidate(String beanName, BeanDefinition beanDefinition) {
        if (super.checkCandidate(beanName, beanDefinition)) {
            return true;
        } else {
            LOGGER.warn(() -> "Skipping MapperFactoryBean with name '" + beanName + "' and '"
                    + beanDefinition.getBeanClassName() + "' mapperInterface" + ". Bean already defined with the same name!");
            return false;
        }
    }

}
