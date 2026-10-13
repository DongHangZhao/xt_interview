package com.xt.interview.controller;

import com.xt.interview.dto.AccountDTO;
import com.xt.interview.dto.PageResult;
import com.xt.interview.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@CrossOrigin(origins = "*")
public class AccountController {

    @Autowired
    private AccountService accountService;

    @GetMapping
    public ResponseEntity<List<AccountDTO>> getAll() {
        return ResponseEntity.ok(accountService.getAllAccounts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(accountService.getAccountById(id));
    }

    @PostMapping
    public ResponseEntity<AccountDTO> create(@RequestBody AccountDTO dto) {
        return ResponseEntity.ok(accountService.createAccount(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AccountDTO> update(@PathVariable Long id, @RequestBody AccountDTO dto) {
        return ResponseEntity.ok(accountService.updateAccount(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        accountService.deleteAccount(id);
        return ResponseEntity.ok().build();
    }

    /**
     * 分页查询用户，支持按用户名模糊搜索、状态筛选、权限级别筛选。
     *
     * 参数均为可选：username 支持模糊匹配（同时匹配用户名与姓名），
     * status / roleLevel 不传则不过滤。
     *
     * 注意：路径需放在 /{id} 之前声明，否则 "page" 会被当作 id 解析。
     */
    @GetMapping("/page")
    public ResponseEntity<PageResult<AccountDTO>> getPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer roleLevel) {

        // 边界防护：页码不能为负，每页条数限制在 1~100，防止一次拉全表
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        return ResponseEntity.ok(accountService.getAccountsPage(
                safePage, safeSize, username, status, roleLevel));
    }

    /**
     * 修改「我的资料」。
     *
     * 无需传 id —— 后端从 token 中识别当前登录人，从根上杜绝
     * 「改别人资料」的可能。仅开放姓名/手机/性别/用户名/密码，
     * 权限、状态、部门等管理字段不接受。
     */
    @PutMapping("/my-profile")
    public ResponseEntity<AccountDTO> updateMyProfile(@RequestBody AccountDTO dto) {
        return ResponseEntity.ok(accountService.updateMyProfile(dto));
    }
}
