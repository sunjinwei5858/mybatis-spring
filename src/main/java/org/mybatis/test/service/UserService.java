package org.mybatis.test.service;

import org.mybatis.test.domain.User;
import org.mybatis.test.mapper.UserMappper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class UserService {

    @Autowired
    private UserMappper userMappper;

    public List<User> queryUserList() {

        for (int i = 0; i < 2; i++) {
            List<User> userList = userMappper.findUserList();
        }

        return new ArrayList<>();

    }
}
