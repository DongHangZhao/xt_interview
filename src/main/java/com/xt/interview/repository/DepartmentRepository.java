package com.xt.interview.repository;

import com.xt.interview.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    List<Department> findByDeleted(Integer deleted);

    Optional<Department> findByIdAndDeleted(Long id, Integer deleted);

    /**
     * 编码唯一性校验：只查「未删除」范围。
     *
     * 背景：V4 迁移已把 uk_department_code 唯一索引降级为普通索引，
     * 唯一性改由应用层负责。这样软删除的部门编码可以被重新使用，
     * 避免「物理行仍在、唯一索引仍占用」导致编码永久无法复用。
     */
    boolean existsByCodeAndDeleted(String code, Integer deleted);

    /** 编码唯一性校验（更新场景，排除自身） */
    boolean existsByCodeAndDeletedAndIdNot(String code, Integer deleted, Long id);

    /** 名称唯一性校验：只查「未删除」范围，避免出现多个同名部门 */
    boolean existsByNameAndDeleted(String name, Integer deleted);

    /** 名称唯一性校验（更新场景，排除自身） */
    boolean existsByNameAndDeletedAndIdNot(String name, Integer deleted, Long id);
}
