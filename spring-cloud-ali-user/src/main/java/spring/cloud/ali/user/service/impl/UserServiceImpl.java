package spring.cloud.ali.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import spring.cloud.ali.common.util.JsonUtil;
import spring.cloud.ali.user.mapper.UserMapper;
import spring.cloud.ali.user.model.User;
import spring.cloud.ali.user.result.UserDetailResult;
import spring.cloud.ali.user.service.UserService;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl implements UserService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

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

        Object cached = redisTemplate.opsForValue().get("users:" + userId);
        if (cached != null){
            return JsonUtil.toObject(String.valueOf(cached), UserDetailResult.class);
        }

        User user = userMapper.selectById(userId);
        if (user == null){
            return null;
        }
        UserDetailResult ur = new UserDetailResult();
        BeanUtils.copyProperties(user, ur);

        redisTemplate.opsForValue().set("users:" + userId, JsonUtil.toJson(ur), 30, TimeUnit.SECONDS);

        return ur;
    }
}
