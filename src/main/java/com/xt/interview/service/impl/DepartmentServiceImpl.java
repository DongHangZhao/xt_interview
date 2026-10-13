package com.xt.interview.service.impl;

import com.xt.interview.dto.DepartmentDTO;
import com.xt.interview.entity.Account;
import com.xt.interview.entity.Department;
import com.xt.interview.exception.BusinessException;
import com.xt.interview.repository.AccountRepository;
import com.xt.interview.repository.DepartmentRepository;
import com.xt.interview.security.PermissionChecker;
import com.xt.interview.service.DepartmentService;
import com.xt.interview.util.SoftDeleteCodeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class DepartmentServiceImpl implements DepartmentService {

    private static final Integer NOT_DELETED = 0;
    private static final Integer DELETED = 1;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Override
    public DepartmentDTO createDepartment(DepartmentDTO dto) {
        // 权限校验：普通用户不得创建部门
        PermissionChecker.requireAdmin();

        String name = dto.getName().trim();
        String code = dto.getCode().trim();

        // 唯一性只查「未删除」范围：软删除的部门编码/名称允许被复用
        if (departmentRepository.existsByCodeAndDeleted(code, NOT_DELETED)) {
            throw new BusinessException("部门编码已存在");
        }
        if (departmentRepository.existsByNameAndDeleted(name, NOT_DELETED)) {
            throw new BusinessException("部门名称已存在");
        }

        Department department = new Department();
        department.setName(name);
        department.setCode(code);
        department.setDescription(dto.getDescription());
        department.setStatus(dto.getStatus());
        department.setDeleted(NOT_DELETED);

        department = departmentRepository.save(department);
        return convertToDTO(department);
    }

    @Override
    public DepartmentDTO updateDepartment(Long id, DepartmentDTO dto) {
        PermissionChecker.requireAdmin();

        Department department = departmentRepository.findByIdAndDeleted(id, NOT_DELETED)
                .orElseThrow(() -> new BusinessException("部门不存在"));

        String name = dto.getName().trim();
        String code = dto.getCode().trim();

        // 唯一性校验需排除自身，否则编辑时不改编码/名称也会被判重复
        if (departmentRepository.existsByCodeAndDeletedAndIdNot(code, NOT_DELETED, id)) {
            throw new BusinessException("部门编码已存在");
        }
        if (departmentRepository.existsByNameAndDeletedAndIdNot(name, NOT_DELETED, id)) {
            throw new BusinessException("部门名称已存在");
        }

        department.setName(name);
        department.setCode(code);
        department.setDescription(dto.getDescription());
        department.setStatus(dto.getStatus());

        department = departmentRepository.save(department);
        return convertToDTO(department);
    }

    @Override
    public void deleteDepartment(Long id) {
        PermissionChecker.requireAdmin();

        Department department = departmentRepository.findByIdAndDeleted(id, NOT_DELETED)
                .orElseThrow(() -> new BusinessException("部门不存在"));

        // 关联校验：部门下仍有未删除用户时禁止删除，否则会产生悬空的用户-部门引用
        long userCount = accountRepository.countByDepartmentIdAndDeleted(id, NOT_DELETED);
        if (userCount > 0) {
            throw new BusinessException("该部门下还有 " + userCount + " 个用户，无法删除");
        }

        department.setDeleted(DELETED);
        /**
         * 软删除时同步改写唯一编码（test → test__del_时间戳）。
         *
         * 原因：唯一索引作用在物理行上，仅标记 deleted=1 无法释放编码，
         *会导致删除后该编码被永久占用、无法再创建。
         * 改写后：历史记录仍保留（可审计、可人工恢复），原编码立即可复用。
         */
        department.setCode(SoftDeleteCodeUtil.buildDeletedCode(department.getCode()));
        departmentRepository.save(department);
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentDTO getDepartmentById(Long id) {
        Department department = departmentRepository.findByIdAndDeleted(id, NOT_DELETED)
                .orElseThrow(() -> new BusinessException("部门不存在"));
        return convertToDTO(department);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentDTO> getAllDepartments() {
        return departmentRepository.findByDeleted(NOT_DELETED).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private DepartmentDTO convertToDTO(Department department) {
        DepartmentDTO dto = new DepartmentDTO();
        dto.setId(department.getId());
        dto.setName(department.getName());
        dto.setCode(department.getCode());
        dto.setDescription(department.getDescription());
        dto.setStatus(department.getStatus());
        dto.setDeleted(department.getDeleted());
        dto.setCreateTime(department.getCreateTime());
        dto.setUpdateTime(department.getUpdateTime());
        return dto;
    }
}
