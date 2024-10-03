package spring.cloud.ali.user.service;

import spring.cloud.ali.user.result.UserDetailResult;
import spring.cloud.ali.user.result.UserLoginResult;
import spring.cloud.ali.user.result.VerifyTokenResult;

public interface UserService {

    UserLoginResult login(String userName, String password);

    VerifyTokenResult verifyToken(String token);

    UserDetailResult queryUserByName(String userName);

    UserDetailResult queryUserById(Long userId);
}
