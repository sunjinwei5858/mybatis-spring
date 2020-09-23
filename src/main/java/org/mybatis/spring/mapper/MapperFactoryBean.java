/**
 * Copyright 2010-2019 the original author or authors.
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

import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.support.SqlSessionDaoSupport;
import org.springframework.beans.factory.FactoryBean;

import static org.springframework.util.Assert.notNull;

/**
 * mybatis的mapper接口和sqlSessionFactory都是通过实现FactoryBean接口，前者是泛型FactoryBean<T>，后者直接是FactoryBean<SqlSessionFactory>
 * 这样就可以注入多个mappers,还可以设置SqlSessionFactory属性
 * <p>
 * <p>
 * BeanFactory that enables injection of MyBatis mapper interfaces.
 * It can be set up with a SqlSessionFactory or a pre-configured SqlSessionTemplate.
 * <p>
 * Sample configuration:
 *
 * <pre class="code">
 * {@code
 *   <bean id="baseMapper" class="org.mybatis.spring.mapper.MapperFactoryBean" abstract="true" lazy-init="true">
 *     <property name="sqlSessionFactory" ref="sqlSessionFactory" />
 *   </bean>
 *
 *   <bean id="oneMapper" parent="baseMapper">
 *     <property name="mapperInterface" value="my.package.MyMapperInterface" />
 *   </bean>
 *
 *   <bean id="anotherMapper" parent="baseMapper">
 *     <property name="mapperInterface" value="my.package.MyAnotherMapperInterface" />
 *   </bean>
 * }
 * </pre>
 * <p>
 * Note that this factory can only inject <em>interfaces</em>, not concrete classes.
 *
 * @author Eduardo Macarron
 * @see SqlSessionTemplate
 */
//继承SqlSessionDaoSupport、实现FactoryBean，那么最终注入Spring容器的对象要从getObject()中取
public class MapperFactoryBean<T> extends SqlSessionDaoSupport implements FactoryBean<T> {

    // 这里其实就是mapper接口 使用这种将mapper传进来 这样MapperFactoryBean就不需要加@component注解 ！！！！ 奇妙
    private Class<T> mapperInterface;

    private boolean addToConfig = true;

    public MapperFactoryBean() {
        // intentionally empty
    }

    //构造器，在BeanDefinition中已经设置了构造器输入参数
    //所以在通过反射调用构造器实例化时，会获取在BeanDefinition设置的构造器输入参数
    //也就是对应得每个Mapper接口Class
    public MapperFactoryBean(Class<T> mapperInterface) {
        System.out.println("4--MapperFactoryBean构造初始化----" + mapperInterface);
        this.mapperInterface = mapperInterface;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void checkDaoConfig() {
        super.checkDaoConfig();

        notNull(this.mapperInterface, "Property 'mapperInterface' is required");

        Configuration configuration = getSqlSession().getConfiguration();
        if (this.addToConfig && !configuration.hasMapper(this.mapperInterface)) {
            try {
                configuration.addMapper(this.mapperInterface);
            } catch (Exception e) {
                logger.error("Error while adding the mapper '" + this.mapperInterface + "' to configuration.", e);
                throw new IllegalArgumentException(e);
            } finally {
                ErrorContext.instance().reset();
            }
        }
    }

    /**
     * 最终注入Spring容器的就是这里的返回对象
     * <p>
     * 这里进行获取mapper，通过抽象父类SqlSessionDaoSupport的getSqlSession
     * {@inheritDoc}
     */
    @Override
    public T getObject() throws Exception {
        //获取父类setSqlSessionFactory方法中创建的SqlSessionTemplate
        //通过SqlSessionTemplate获取mapperInterface的代理类
        //获取到Mapper接口的代理类后，就把这个Mapper的代理类对象注入Spring容器
        SqlSession sqlSession = getSqlSession();
        T mapper = sqlSession.getMapper(this.mapperInterface);
        System.out.println("4--MapperFactoryBean执行getObject()方法--" + mapper);
        return mapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<T> getObjectType() {
        return this.mapperInterface;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSingleton() {
        return true;
    }

    // ------------- mutators --------------

    /**
     * Sets the mapper interface of the MyBatis mapper
     *
     * @param mapperInterface class of the interface
     */
    public void setMapperInterface(Class<T> mapperInterface) {
        this.mapperInterface = mapperInterface;
    }

    /**
     * Return the mapper interface of the MyBatis mapper
     *
     * @return class of the interface
     */
    public Class<T> getMapperInterface() {
        return mapperInterface;
    }

    /**
     * If addToConfig is false the mapper will not be added to MyBatis. This means it must have been included in
     * mybatis-config.xml.
     * <p>
     * If it is true, the mapper will be added to MyBatis in the case it is not already registered.
     * <p>
     * By default addToConfig is true.
     *
     * @param addToConfig a flag that whether add mapper to MyBatis or not
     */
    public void setAddToConfig(boolean addToConfig) {
        this.addToConfig = addToConfig;
    }

    /**
     * Return the flag for addition into MyBatis config.
     *
     * @return true if the mapper will be added to MyBatis in the case it is not already registered.
     */
    public boolean isAddToConfig() {
        return addToConfig;
    }
}
