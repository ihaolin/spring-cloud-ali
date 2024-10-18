package spring.cloud.ali.user.model;

import lombok.Data;

@Data
public class User {

    private Long id;

    private String username;

    private String password;

    private Boolean enabled;
}
