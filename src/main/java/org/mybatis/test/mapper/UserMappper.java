package org.mybatis.test.mapper;

import org.apache.ibatis.annotations.Select;
import org.mybatis.test.domain.User;

import java.util.List;

public interface UserMappper {

    @Select("SELECT id, name from user limit 1")
    List<User> findUserList();

}
