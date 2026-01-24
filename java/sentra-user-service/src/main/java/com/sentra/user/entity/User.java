package com.sentra.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.sentra.common.entity.BaseEntity;
import com.sentra.user.enums.UserRole;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_user")
@Entity
@Table(name = "t_user")
public class User extends BaseEntity {

    private String username;

    private String password;

    @Enumerated(EnumType.STRING)
    private UserRole role;
}
