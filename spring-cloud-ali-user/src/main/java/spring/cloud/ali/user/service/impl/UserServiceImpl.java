package spring.cloud.ali.user.service.impl;

import com.alibaba.nacos.shaded.com.google.common.collect.ImmutableMap;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.common.base.Strings;
import io.jsonwebtoken.Claims;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import spring.cloud.ali.common.util.JsonUtil;
import spring.cloud.ali.common.util.JwtTokenUtil;
import spring.cloud.ali.user.config.AppConfig;
import spring.cloud.ali.user.mapper.UserMapper;
import spring.cloud.ali.user.model.User;
import spring.cloud.ali.user.result.UserDetailResult;
import spring.cloud.ali.user.result.UserLoginResult;
import spring.cloud.ali.user.result.VerifyTokenResult;
import spring.cloud.ali.user.service.UserService;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static spring.cloud.ali.common.context.LoginContext.HTTP_HEADER_LOGIN_USER_ID;
import static spring.cloud.ali.common.exception.BizException.USER_PWD_ERROR;
import static spring.cloud.ali.user.result.VerifyTokenResult.NOT_PASS;

@Service
public class UserServiceImpl implements UserService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private AppConfig appConfig;

    @Resource
    private MeterRegistry meterRegistry;

    @Override
    public UserLoginResult login(String userName, String password) {

        QueryWrapper<User> q = new QueryWrapper<>();
        q.eq("username", userName);
        User user = userMapper.selectOne(q);

        if (user == null || !Objects.equals(password, user.getPassword())){
            throw USER_PWD_ERROR;
        }

        String token = JwtTokenUtil.generate(appConfig.getLoginSignKey(),
                ImmutableMap.of(HTTP_HEADER_LOGIN_USER_ID, String.valueOf(user.getId())), 3600 * 24 * 30);

        UserLoginResult loginResult = new UserLoginResult();
        loginResult.setToken(token);

        meterRegistry.counter("user_login").increment();

        return loginResult;
    }

    @Override
    public VerifyTokenResult verifyToken(String token) {
        Claims claims = JwtTokenUtil.parse(appConfig.getLoginSignKey(), token);
        if (claims == null || claims.getExpiration().before(new Date())){
            return NOT_PASS;
        }

        String loginUserId = claims.get(HTTP_HEADER_LOGIN_USER_ID, String.class);
        if (Strings.isNullOrEmpty(loginUserId)){
            return NOT_PASS;
        }

        return new VerifyTokenResult(true, Long.valueOf(loginUserId));
    }

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
