package com.xt.interview.repository;

import com.xt.interview.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 继承 JpaSpecificationExecutor 以支持 findAll(Specification, Pageable) 动态条件分页查询。
 *
 * 注意：Spring Data JPA 4（Spring Boot 4 起）不再把 Specification 查询
 * 方法混入 JpaRepository，必须显式继承 JpaSpecificationExecutor。
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, Long>, JpaSpecificationExecutor<Account> {
    List<Account> findByDeleted(Integer deleted);
    Optional<Account> findByIdAndDeleted(Long id, Integer deleted);
    Optional<Account> findByUsernameAndDeleted(String username, Integer deleted);
    Optional<Account> findByUsernameAndPasswordAndDeleted(String username, String password, Integer deleted);
    /**
     * 用户名唯一性校验（只查「未删除」范围）。
     *
     * 背景同部门：username 上建有唯一索引 uk_username，软删除只标记 deleted=1，
     * 物理行仍在且仍占用索引，会导致软删除后无法重建同名用户。
     * 应用层改查「未删除」范围，使软删除的用户名可以被复用。
     * 并发场景由 GlobalExceptionHandler 的 DataIntegrityViolationException 兜底翻译。
     */
    boolean existsByUsernameAndDeleted(String username, Integer deleted);

    /** 统计某部门下未删除的用户数，用于部门删除前的关联校验 */
    long countByDepartmentIdAndDeleted(Long departmentId, Integer deleted);

    /**
     * 联表查出「用户ID → 部门名称」映射。
     *
     * 用一条 SQL 搞定，避免在 Service 层循环里逐个查部门造成 N+1 查询。
     * 写原生 SQL 而非 JPQL：JPQL 的 `LEFT JOIN ... ON` 属Hibernate 扩展语法，
     * 原生 SQL 在任何 JPA 实现下行为都一致。
     *
     * @return List of [accountId, departmentName]，仅包含已分配部门的用户
     */
    @Query(value = "SELECT a.id, d.name FROM sys_account a "
            + "JOIN sys_department d ON a.department_id = d.id "
            + "WHERE a.deleted = :deleted AND a.department_id IS NOT NULL",
            nativeQuery = true)
    List<Object[]> findDepartmentNamesByDeleted(@Param("deleted") Integer deleted);

    /**
     * 按账号ID 集合取部门名称，用于分页场景（只处理当前页数据）。
     *
     * @return List of [accountId, departmentName]
     */
    @Query(value = "SELECT a.id, d.name FROM sys_account a "
            + "JOIN sys_department d ON a.department_id = d.id "
            + "WHERE a.id IN (:ids)",
            nativeQuery = true)
    List<Object[]> findDepartmentNamesByIdIn(@Param("ids") List<Long> ids);
}