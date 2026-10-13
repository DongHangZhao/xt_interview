package com.xt.interview.service;

import com.xt.interview.dto.AccountDTO;
import com.xt.interview.dto.PageResult;

import java.util.List;

public interface AccountService {

    AccountDTO createAccount(AccountDTO dto);

    AccountDTO updateAccount(Long id, AccountDTO dto);

    void deleteAccount(Long id);

    AccountDTO getAccountById(Long id);

    List<AccountDTO> getAllAccounts();

    AccountDTO login(String username, String password);

    /**
     * 分页查询用户，支持动态条件组合。
     *
     * @param page     页码，从 0 开始
     * @param size     每页条数
     * @param username 用户名模糊搜索，可为空
     * @param status   状态筛选（0-禁用/1-启用），可为 null
     * @param roleLevel 权限级别筛选，可为 null
     */
    PageResult<AccountDTO> getAccountsPage(int page, int size, String username,
                                           Integer status, Integer roleLevel);

    /**
     * 普通用户修改自己的资料。
     *
     * 仅允许修改：姓名、手机号、性别、登录用户名、登录密码。
     * 刻意**不允许**修改 roleLevel / isSuper / status / departmentId——
     * 这些属于管理字段，必须由管理员分配，否则用户可自行提权。
     */
    AccountDTO updateMyProfile(AccountDTO dto);
}
