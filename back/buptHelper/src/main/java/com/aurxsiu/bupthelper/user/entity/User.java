package com.aurxsiu.bupthelper.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 用户信息（《需求说明.md》三.1）。密码以 BCrypt 加密存储，任何接口都不得返回。
 */
@Entity
@Table(name = "sys_user", uniqueConstraints = @UniqueConstraint(name = "uk_sys_user_username", columnNames = "username"))
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Column(name = "password", nullable = false, length = 100)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false, length = 20)
    private UserType userType;

    @Column(name = "real_name", nullable = false, length = 50)
    private String realName;

    @Column(name = "phone", nullable = false, length = 11)
    private String phone;

    @Column(name = "bio", length = 200)
    private String bio;

    @Column(name = "register_time", nullable = false)
    private LocalDateTime registerTime;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;
}
