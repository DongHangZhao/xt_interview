package com.xt.interview.enums;

/**
 * 角色级别枚举。
 *
 * 数值与 sys_account.role_level 字段一一对应。
 * 权限越高，数值越大，越容易被"禁止动同级及以上"规则保护。
 */
public enum RoleLevel {

    /** 普通用户：只能查看自己的信息 */
    USER(1, "用户权限"),

    /** 管理员：可管理用户与部门，但不能操作同级及更高权限账号 */
    ADMIN(2, "管理员权限"),

    /** 超级管理员：拥有全部权限 */
    SUPER_ADMIN(3, "超级管理员权限");

    private final int code;
    private final String label;

    RoleLevel(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static RoleLevel fromCode(Integer code) {
        if (code == null) {
            return USER;
        }
        for (RoleLevel role : values()) {
            if (role.code == code) {
                return role;
            }
        }
        return USER;
    }

    /** 是否为管理员及以上 */
    public static boolean isAdminOrAbove(Integer code) {
        return code != null && code >= ADMIN.code;
    }
}
