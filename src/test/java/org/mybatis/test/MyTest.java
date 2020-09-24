package org.mybatis.test;

import org.junit.jupiter.api.Test;
import org.mybatis.test.config.MybatisConfig;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class MyTest {

    @Test
    public void test001() {

        AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext(MybatisConfig.class);

       /* BeanDefinition sqlSessionFactoryBeanDefinition = annotationConfigApplicationContext.getBeanDefinition("sqlSessionFactoryBean");

        System.out.println("sqlSessionFactory BeanDefinition: "+sqlSessionFactoryBeanDefinition);


        BeanDefinition userMappperBeanDefinition = annotationConfigApplicationContext.getBeanDefinition("userMappper");

        System.out.println("userMappper beanDefinition : " + userMappperBeanDefinition);


        UserMappper userMappper = (UserMappper) annotationConfigApplicationContext.getBean("userMappper");

        List<User> userList = userMappper.findUserList();

        userList.forEach(result -> System.out.println(result));*/

        String[] beanDefinitionNames = annotationConfigApplicationContext.getBeanDefinitionNames();
        for (String beanDefinitionName : beanDefinitionNames) {
            System.out.println("==="+beanDefinitionName);
        }

    }
}
