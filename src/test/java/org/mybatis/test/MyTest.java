package org.mybatis.test;

import org.junit.jupiter.api.Test;
import org.mybatis.test.config.MybatisConfig;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class MyTest {

    @Test
    public void test001() {

        AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext(MybatisConfig.class);


       /* UserMappper userMappper = (UserMappper) annotationConfigApplicationContext.getBean("userMappper");

        UserMappper userMappper02 = (UserMappper) annotationConfigApplicationContext.getBean("userMappper");

        List<User> userList = userMappper.findUserList();

        userList.forEach(result -> System.out.println(result));


        UserService userService = (UserService) annotationConfigApplicationContext.getBean("userService");*/





    }




}
