package spring.cloud.ali.user.service;

import spring.cloud.ali.user.result.UserDetailResult;

public interface UserService {

    UserDetailResult queryUserByName(String userName);

    UserDetailResult queryUserById(Long userId);
}
