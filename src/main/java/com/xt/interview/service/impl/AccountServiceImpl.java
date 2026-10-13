package com.xt.interview.service.impl;

import com.xt.interview.dto.AccountDTO;
import com.xt.interview.dto.PageResult;
import com.xt.interview.entity.Account;
import com.xt.interview.entity.Department;
import com.xt.interview.exception.BusinessException;
import com.xt.interview.repository.AccountRepository;
import com.xt.interview.repository.DepartmentRepository;
import com.xt.interview.security.LoginUser;
import com.xt.interview.security.PermissionChecker;
import com.xt.interview.service.AccountService;
import com.xt.interview.util.MD5Util;
import com.xt.interview.util.SoftDeleteCodeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class AccountServiceImpl implements AccountService {

    private static final Integer NOT_DELETED = 0;
    private static final Integer DELETED = 1;

    private static final String DEFAULT_PASSWORD = "123456";

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Override
    public AccountDTO createAccount(AccountDTO dto) {
        // 权限校验：普通用户不得创建账号；管理员不得创建高于自身权限的账号（防提权）
        PermissionChecker.checkCreateAccount(dto.getRoleLevel());

        // 唯一性只查「未删除」范围：软删除的账号用户名允许被复用
        if (accountRepository.existsByUsernameAndDeleted(dto.getUsername(), NOT_DELETED)) {
            throw new BusinessException("用户名已存在");
        }
        Account account = new Account();
        account.setUsername(dto.getUsername());
        account.setName(dto.getName());
        account.setGender(dto.getGender());
        account.setPhone(dto.getPhone());
        String password = (dto.getPassword() == null || dto.getPassword().isEmpty())
                ? DEFAULT_PASSWORD : dto.getPassword();
        account.setPassword(MD5Util.encode(password));
        account.setRoleLevel(dto.getRoleLevel());
        account.setStatus(dto.getStatus());
        // isSuper 同样受提权约束：非超管不得把 isSuper 置为 1
        account.setIsSuper(resolveIsSuper(dto.getIsSuper()));
        account.setDepartmentId(resolveDepartmentId(dto.getDepartmentId(), null));
        account.setDeleted(NOT_DELETED);
        account = accountRepository.save(account);
        // 带出部门名，与列表/详情接口字段保持一致
        return convertToDTO(account, loadDepartmentNameMap(List.of(account)));
    }

    @Override
    public AccountDTO updateAccount(Long id, AccountDTO dto) {
        Account account = accountRepository.findByIdAndDeleted(id, NOT_DELETED)
                .orElseThrow(() -> new BusinessException("账号不存在"));

        // 权限校验：同级保护 + 提权拦截 + 自我停用保护，一次性覆盖
        PermissionChecker.checkUpdateAccount(
                account.getRoleLevel(), dto.getRoleLevel(), account.getId());
        PermissionChecker.checkUpdateStatus(
                account.getRoleLevel(), dto.getStatus(), account.getId());

        if (!account.getUsername().equals(dto.getUsername())
                && accountRepository.existsByUsernameAndDeleted(dto.getUsername(), NOT_DELETED)) {
            throw new BusinessException("用户名已存在");
        }

        account.setUsername(dto.getUsername());
        account.setName(dto.getName());
        account.setGender(dto.getGender());
        account.setPhone(dto.getPhone());
        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            account.setPassword(MD5Util.encode(dto.getPassword()));
        }
        account.setRoleLevel(dto.getRoleLevel());
        account.setStatus(dto.getStatus());
        account.setIsSuper(resolveIsSuper(dto.getIsSuper()));
        // 传入该账号原有的部门ID：若部门后来被禁用，用户保持原状不应被拦截
        Long originalDeptId = account.getDepartmentId();
        account.setDepartmentId(resolveDepartmentId(dto.getDepartmentId(), originalDeptId));
        account = accountRepository.save(account);
        return convertToDTO(account, loadDepartmentNameMap(List.of(account)));
    }

    @Override
    public void deleteAccount(Long id) {
        Account account = accountRepository.findByIdAndDeleted(id, NOT_DELETED)
                .orElseThrow(() -> new BusinessException("账号不存在"));

        PermissionChecker.checkDeleteAccount(account.getRoleLevel(), account.getId());

        account.setDeleted(DELETED);
        /**
         * 软删除时同步改写用户名（zhangsan → zhangsan__del_时间戳）。
         * 理由同部门：唯一索引作用在物理行上，仅标记 deleted=1 无法释放用户名，
         * 会导致删除后该用户名被永久占用。改写后原用户名立即可复用。
         */
        account.setUsername(SoftDeleteCodeUtil.buildDeletedCode(account.getUsername()));
        accountRepository.save(account);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountDTO getAccountById(Long id) {
        Account account = accountRepository.findByIdAndDeleted(id, NOT_DELETED)
                .orElseThrow(() -> new BusinessException("账号不存在"));

        // 数据级权限：普通用户只能查看自己的账号
        PermissionChecker.checkReadAccount(account.getId());

        // 与列表接口保持一致：详情也要返回部门名，避免前端同一页面两个接口字段不一致
        Map<Long, String> deptNameMap = loadDepartmentNameMap(List.of(account));
        return convertToDTO(account, deptNameMap);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountDTO> getAllAccounts() {
        // 数据级权限：普通用户只能看到自己
        if (!PermissionChecker.currentUser().isAdminOrAbove()) {
            return accountRepository.findByDeleted(NOT_DELETED).stream()
                    .filter(a -> a.getId().equals(PermissionChecker.currentUser().getUserId()))
                    .map(a -> convertToDTO(a, loadDepartmentNameMap()))
                    .collect(Collectors.toList());
        }

        // 一次联表拿全部部门名，避免逐个用户查部门造成 N+1 查询
        Map<Long, String> deptNameMap = loadDepartmentNameMap();

        return accountRepository.findByDeleted(NOT_DELETED).stream()
                .map(account -> convertToDTO(account, deptNameMap))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AccountDTO login(String username, String password) {
        Account account = accountRepository.findByUsernameAndPasswordAndDeleted(
                username, MD5Util.encode(password), NOT_DELETED)
                .orElseThrow(() -> new BusinessException("用户名或密码错误"));
        if (account.getStatus() != 1) {
            throw new BusinessException("账号已禁用");
        }
        return convertToDTO(account, loadDepartmentNameMap(List.of(account)));
    }

    /**
     * 分页查询：支持用户名模糊搜索 + 状态筛选 + 权限级别筛选。
     *
     * 用 JPA Specification 动态拼接条件，避免为每种条件组合各写一个 Repository 方法
     * （那是 2^N 的方法爆炸）。条件为 null / 空串时不参与过滤。
     */
    @Override
    @Transactional(readOnly = true)
    public PageResult<AccountDTO> getAccountsPage(int page, int size, String username,
                                                  Integer status, Integer roleLevel) {
        Specification<Account> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            // 软删除数据一律不可见
            predicates.add(cb.equal(root.get("deleted"), NOT_DELETED));

            // 数据级权限：普通用户分页结果也只能看到自己
            LoginUser operator = PermissionChecker.currentUser();
            if (!operator.isAdminOrAbove()) {
                predicates.add(cb.equal(root.get("id"), operator.getUserId()));
            }

            if (username != null && !username.trim().isEmpty()) {
                String kw = "%" + username.trim() + "%";
                // 同时匹配用户名和姓名，符合"按用户名模糊搜索"的直觉
                predicates.add(cb.or(
                        cb.like(root.get("username"), kw),
                        cb.like(root.get("name"), kw)
                ));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (roleLevel != null) {
                predicates.add(cb.equal(root.get("roleLevel"), roleLevel));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
        Page<Account> result = accountRepository.findAll(spec, pageable);

        // 只为当前页的用户取部门名，避免为全表做联表
        Map<Long, String> deptNameMap = loadDepartmentNameMap(result.getContent());

        List<AccountDTO> dtoList = result.getContent().stream()
                .map(account -> convertToDTO(account, deptNameMap))
                .collect(Collectors.toList());

        return new PageResult<>(dtoList, result.getTotalElements(), page, size);
    }

    /**
     * 普通用户修改自己的资料。
     *
     * 严格白名单：只处理 name / phone / gender / username / password 五个字段，
     * **完全不读取** roleLevel、isSuper、status、departmentId——
     * 即使前端恶意提交这些字段也不会落库，从根上杜绝自我提权。
     */
    @Override
    public AccountDTO updateMyProfile(AccountDTO dto) {
        LoginUser operator = PermissionChecker.currentUser();

        Account account = accountRepository.findByIdAndDeleted(operator.getUserId(), NOT_DELETED)
                .orElseThrow(() -> new BusinessException("账号不存在"));

        // 改用户名需校验未被占用（只查未删除范围，软删除码已自动改写故不冲突）
        if (dto.getUsername() != null && !dto.getUsername().trim().isEmpty()
                && !dto.getUsername().trim().equals(account.getUsername())) {
            String newUsername = dto.getUsername().trim();
            if (accountRepository.existsByUsernameAndDeleted(newUsername, NOT_DELETED)) {
                throw new BusinessException("用户名已存在");
            }
            // 不允许改成软删除生成的废弃码
            if (SoftDeleteCodeUtil.isDeletedCode(newUsername)) {
                throw new BusinessException("用户名格式不合法");
            }
            account.setUsername(newUsername);
        }

        if (dto.getName() != null && !dto.getName().trim().isEmpty()) {
            account.setName(dto.getName().trim());
        }
        if (dto.getPhone() != null) {
            account.setPhone(dto.getPhone().trim());
        }
        if (dto.getGender() != null) {
            account.setGender(dto.getGender());
        }
        // 改密码：走专用字段 newPassword，且不允许与当前密码相同
        if (dto.getNewPassword() != null && !dto.getNewPassword().isEmpty()) {
            if (dto.getNewPassword().equals(dto.getPassword())) {
                throw new BusinessException("新密码不能与当前密码相同");
            }
            account.setPassword(MD5Util.encode(dto.getNewPassword()));
        }

        account = accountRepository.save(account);
        return convertToDTO(account, loadDepartmentNameMap(List.of(account)));
    }

    /** 构建「用户ID → 部门名称」映射，单条 SQL 完成 */
    private Map<Long, String> loadDepartmentNameMap() {
        return loadDepartmentNameMap(accountRepository.findByDeleted(NOT_DELETED));
    }

    /**
     * 构建「用户ID → 部门名称」映射。
     * 入参限定为当前页的账号列表，避免分页场景下为全表做联表查询。
     */
    private Map<Long, String> loadDepartmentNameMap(List<Account> accounts) {
        if (accounts.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> ids = accounts.stream().map(Account::getId).toList();
        Map<Long, String> map = new HashMap<>();
        for (Object[] row : accountRepository.findDepartmentNamesByIdIn(ids)) {
            map.put(((Number) row[0]).longValue(), (String) row[1]);
        }
        return map;
    }

    /**
     * 校验并返回 isSuper 标记。
     * 防止绕过 roleLevel 校验：即使 roleLevel 没被提权，
     * 也不能单独把 isSuper 置 1 来获得超管标记。
     */
    private Integer resolveIsSuper(Integer submittedIsSuper) {
        if (submittedIsSuper == null || submittedIsSuper != 1) {
            return 0;
        }
        LoginUser operator = PermissionChecker.currentUser();
        if (!operator.isSuperAdmin()) {
            throw new BusinessException("不能设置超级管理员标记，仅超管可操作", 403);
        }
        return 1;
    }

    /**
     * 校验并返回部门ID。
     *
     * 部门是可选字段，传 null 表示未分配；传了值则必须指向真实存在且未删除的部门，
     * 避免前端传入脏数据或指向已删除部门。
     *
     * 禁用部门（status=0）的处理策略：**禁止新分配，但不影响已分配的用户**。
     * 关键细节：更新场景传入「该用户原本就属于的部门」时允许通过，
     * 否则部门一旦被禁用，其下所有用户连改个手机号都保存不了。
     * 这类"保持原状"的操作不算"新分配"。
     *
     * @param departmentId    前端提交的部门ID
     * @param originalDeptId  该记录更新前的部门ID，新增场景传 null
     */
    private Long resolveDepartmentId(Long departmentId, Long originalDeptId) {
        if (departmentId == null) {
            return null;
        }
        Department department = departmentRepository.findByIdAndDeleted(departmentId, NOT_DELETED)
                .orElseThrow(() -> new BusinessException("所选部门不存在或已删除"));

        // 部门被禁用时，仅拦截"改投"行为，用户留在原部门不算违规
        boolean stayingInSameDepartment = departmentId.equals(originalDeptId);
        boolean disabled = department.getStatus() == null || department.getStatus() != 1;
        if (disabled && !stayingInSameDepartment) {
            throw new BusinessException("部门【" + department.getName() + "】已禁用，无法分配用户");
        }
        return department.getId();
    }

    private AccountDTO convertToDTO(Account account) {
        return convertToDTO(account, null);
    }
    private AccountDTO convertToDTO(Account account, Map<Long, String> deptNameMap) {
        AccountDTO dto = new AccountDTO();
        dto.setId(account.getId());
        dto.setUsername(account.getUsername());
        dto.setName(account.getName());
        dto.setGender(account.getGender());
        dto.setPhone(account.getPhone());
        dto.setRoleLevel(account.getRoleLevel());
        dto.setStatus(account.getStatus());
        dto.setIsSuper(account.getIsSuper());
        dto.setDepartmentId(account.getDepartmentId());
        dto.setDepartmentName(deptNameMap == null ? null : deptNameMap.get(account.getId()));
        dto.setCreateTime(account.getCreateTime());
        dto.setUpdateTime(account.getUpdateTime());
        return dto;
    }
}
