package spring.cloud.ali.user.service;

import spring.cloud.ali.user.result.UserDetailResult;
import spring.cloud.ali.user.result.UserLoginResult;

public interface UserService {

    UserLoginResult login(String userName);

    UserDetailResult queryUserByName(String userName);

    UserDetailResult queryUserById(Long userId);
}
