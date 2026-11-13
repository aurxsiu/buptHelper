package com.aurxsiu.bupthelper.config;

import com.aurxsiu.bupthelper.user.entity.User;
import com.aurxsiu.bupthelper.user.entity.UserType;
import com.aurxsiu.bupthelper.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 启动时保证内置管理员账号存在（《需求说明.md》三.1：admin / admin）。
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByUsername(ADMIN_USERNAME)) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        User admin = new User();
        admin.setUsername(ADMIN_USERNAME);
        admin.setPassword(passwordEncoder.encode(ADMIN_PASSWORD));
        admin.setUserType(UserType.ADMIN);
        admin.setRealName("系统管理员");
        admin.setPhone("13800000000");
        admin.setBio("系统内置管理员账号");
        admin.setRegisterTime(now);
        admin.setUpdateTime(now);
        userRepository.save(admin);
        log.info("已初始化内置管理员账号：{}", ADMIN_USERNAME);
    }
}
