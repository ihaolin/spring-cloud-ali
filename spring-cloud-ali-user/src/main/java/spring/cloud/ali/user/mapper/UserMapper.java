package spring.cloud.ali.user.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import spring.cloud.ali.user.model.User;

@Mapper
public interface UserMapper {

    @Select("SELECT * FROM users WHERE username = #{userName}")
    User queryByUserName(@Param("userName") String userName);

    @Select("SELECT * FROM users WHERE id = #{userId}")
    User queryById(@Param("userId") Long userId);
}
