package com.xt.interview.dto;

import java.time.LocalDateTime;

public class AccountDTO {
    private Long id;
    private String username;
    private String name;
    private String gender;
    private String phone;
    private String password;

    /** 新密码专用字段：仅「修改我的资料」接口使用，避免与管理端改密码语义混淆 */
    private String newPassword;
    private Integer roleLevel;
    private Integer status;
    private Integer isSuper;

    /** 所属部门ID（任务二新增，可空表示未分配部门） */
    private Long departmentId;

    /** 所属部门名称，仅用于列表展示，由Service 层组装 */
    private String departmentName;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public AccountDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }

    public Integer getRoleLevel() { return roleLevel; }
    public void setRoleLevel(Integer roleLevel) { this.roleLevel = roleLevel; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public Integer getIsSuper() { return isSuper; }
    public void setIsSuper(Integer isSuper) { this.isSuper = isSuper; }

    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
