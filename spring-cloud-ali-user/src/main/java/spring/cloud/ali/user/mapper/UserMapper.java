package spring.cloud.ali.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import spring.cloud.ali.user.model.User;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
