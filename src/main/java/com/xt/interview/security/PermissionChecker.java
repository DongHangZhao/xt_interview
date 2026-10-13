package com.xt.interview.security;

import com.xt.interview.exception.BusinessException;

/**
 * 权限校验工具：负责取当前登录人并执行越权判定。
 *
 * 三条核心规则：
 * 1. 提权拦截：创建/修改时不得把目标权限设得比自己高
 * 2. 同级保护：不能操作权限不低于自己的账号（防止管理员互相踢下线）
 * 3. 自我保护：不能删除/停用自己，避免把自己锁在系统外
 */
public final class PermissionChecker {

    private PermissionChecker() {}

    /** 获取当前登录用户，未登录直接拒绝 */
    public static LoginUser currentUser() {
        LoginUser user = UserContext.get();
        if (user == null) {
            throw new BusinessException("未获取到登录用户信息，请重新登录", 401);
        }
        return user;
    }

    /**
     * 校验「创建账号」时是否越权。
     *
     * 规则分两种身份：
     * - 非超管（普通用户/管理员）：只能创建权限**严格低于**自己的账号。
     *   管理员(2) 只能建普通用户(1)，不能建同级管理员(2)——
     *   否则任何管理员都能凭空造出另一个管理员，形成"横向提权"：
     *   两个管理员可互相配合，把第三个管理员提升为超管。
     * - 超管(3)：不受此限制，可创建任意权限账号（含同级超管），
     *   否则系统将无法通过界面新增超管，只能靠改库。
     */
    public static void checkCreateAccount(Integer targetRoleLevel) {
        LoginUser operator = currentUser();

        // 普通用户无任何管理权限，连"创建用户"都不允许
        if (!operator.isAdminOrAbove()) {
            throw new BusinessException("权限不足，仅管理员及以上可创建账号", 403);
        }
        // 超管放行全部
        if (operator.isSuperAdmin()) {
            return;
        }
        int target = targetRoleLevel == null ? 1 : targetRoleLevel;
        if (target >= operator.getRoleLevel()) {
            throw new BusinessException("不能创建权限不低于自身的账号（当前 "
                    + operator.getRoleLevel() + "，目标 " + target + "）", 403);
        }
    }

    /**
     * 校验「修改账号」时是否越权。含提权与同级保护双重检查。
     *
     * 超管可操作除自己外的任何账号；非超管不能操作同级及以上。
     *
     * @param targetRoleLevel    该账号当前的角色级别
     * @param submittedRoleLevel 本次请求提交的角色级别
     * @param targetUserId       被操作账号的ID
     */
    public static void checkUpdateAccount(Integer targetRoleLevel, Integer submittedRoleLevel, Long targetUserId) {
        LoginUser operator = currentUser();
        int target = targetRoleLevel == null ? 1 : targetRoleLevel;

        // 规则1：不能操作自己（避免误操作把权限改掉）
        if (targetUserId != null && targetUserId.equals(operator.getUserId())) {
            throw new BusinessException("不能修改自己的账号", 403);
        }
        // 超管不受同级限制，可管理除自己外的所有账号
        if (operator.isSuperAdmin()) {
            return;
        }
        // 规则2：同级及以上保护 —— 管理员(2) 不能操作另一个管理员(2) 或超管(3)
        if (target >= operator.getRoleLevel()) {
            throw new BusinessException("不能操作权限不低于自己的账号（对方权限 " + target
                    + "，当前 " + operator.getRoleLevel() + "）", 403);
        }

        // 规则3：提权拦截 —— 不得把目标提升到高于自己或自身的权限
        int submitted = submittedRoleLevel == null ? 1 : submittedRoleLevel;
        if (submitted > operator.getRoleLevel()) {
            throw new BusinessException("不能将账号权限提升至高于自身（当前 "
                    + operator.getRoleLevel() + "，目标 " + submitted + "）", 403);
        }
    }

    /**
     * 校验「删除账号」时是否越权。
     * 规则：不能删除自己；非超管不能删除权限不低于自己的账号。
     */
    public static void checkDeleteAccount(Integer targetRoleLevel, Long targetUserId) {
        LoginUser operator = currentUser();
        int target = targetRoleLevel == null ? 1 : targetRoleLevel;

        // 不能删除自己
        if (targetUserId != null && targetUserId.equals(operator.getUserId())) {
            throw new BusinessException("不能删除自己的账号", 403);
        }
        // 超管可删除除自己外的任何账号
        if (operator.isSuperAdmin()) {
            return;
        }
        // 不能删除同级及以上
        if (target >= operator.getRoleLevel()) {
            throw new BusinessException("不能删除权限不低于自己的账号", 403);
        }
    }

    /**
     * 校验「修改账号状态（启用/禁用）」。
     * 规则：不能停用自己（会自锁）；非超管不能停用同级及以上。
     */
    public static void checkUpdateStatus(Integer targetRoleLevel, Integer submittedStatus, Long targetUserId) {
        LoginUser operator = currentUser();
        int target = targetRoleLevel == null ? 1 : targetRoleLevel;

        // 停用自己会直接导致下次登录失败，等同自锁
        if (targetUserId != null && targetUserId.equals(operator.getUserId())
                && submittedStatus != null && submittedStatus == 0) {
            throw new BusinessException("不能停用自己的账号", 403);
        }
        if (operator.isSuperAdmin()) {
            return;
        }
        if (target >= operator.getRoleLevel()) {
            throw new BusinessException("不能修改权限不低于自己的账号", 403);
        }
    }

    /** 通用：仅管理员及以上可执行的管理操作 */
    public static void requireAdmin() {
        LoginUser operator = currentUser();
        if (!operator.isAdminOrAbove()) {
            throw new BusinessException("权限不足，仅管理员及以上可执行此操作", 403);
        }
    }

    /**
     * 校验「查看账号详情」。
     * 规则：管理员及以上可看任意账号；普通用户只能看自己。
     */
    public static void checkReadAccount(Long targetUserId) {
        LoginUser operator = currentUser();
        if (operator.isAdminOrAbove()) {
            return;
        }
        if (targetUserId == null || !targetUserId.equals(operator.getUserId())) {
            throw new BusinessException("权限不足，普通用户只能查看自己的信息", 403);
        }
    }
}
