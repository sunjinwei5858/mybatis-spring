package org.mybatis.test;

import org.junit.jupiter.api.Test;
import org.mybatis.test.config.MybatisConfig;
import org.mybatis.test.domain.User;
import org.mybatis.test.mapper.UserMappper;
import org.mybatis.test.service.UserService;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.List;

public class MyTest {

    @Test
    public void test001() {

        AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext(MybatisConfig.class);


        UserMappper userMappper = (UserMappper) annotationConfigApplicationContext.getBean("userMappper");

        UserMappper userMappper02 = (UserMappper) annotationConfigApplicationContext.getBean("userMappper");

        List<User> userList = userMappper.findUserList();

        userList.forEach(result -> System.out.println(result));


        UserService userService = (UserService) annotationConfigApplicationContext.getBean("userService");


    }




}
