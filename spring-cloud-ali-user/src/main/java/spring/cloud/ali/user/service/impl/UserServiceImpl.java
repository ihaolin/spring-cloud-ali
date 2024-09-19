package spring.cloud.ali.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import spring.cloud.ali.user.mapper.UserMapper;
import spring.cloud.ali.user.model.User;
import spring.cloud.ali.user.result.UserDetailResult;
import spring.cloud.ali.user.service.UserService;

import javax.annotation.Resource;

@Service
public class UserServiceImpl implements UserService {

    @Resource
    private UserMapper userMapper;

    @Override
    public UserDetailResult queryUserByName(String userName) {
        QueryWrapper<User> q = new QueryWrapper<>();
        q.eq("username", userName);
        User user = userMapper.selectOne(q);
        if (user == null){
            return null;
        }
        UserDetailResult ur = new UserDetailResult();
        BeanUtils.copyProperties(user, ur);
        return ur;
    }

    @Override
    public UserDetailResult queryUserById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null){
            return null;
        }
        UserDetailResult ur = new UserDetailResult();
        BeanUtils.copyProperties(user, ur);
        return ur;
    }
}
