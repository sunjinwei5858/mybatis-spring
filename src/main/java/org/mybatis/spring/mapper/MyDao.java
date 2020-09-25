package org.mybatis.spring.mapper;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

@Component
public class MyDao implements InitializingBean {
    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("hahahhahahahhahah");
    }
}
