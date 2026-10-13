package com.xt.interview.service;

import com.xt.interview.dto.DepartmentDTO;

import java.util.List;

public interface DepartmentService {

    DepartmentDTO createDepartment(DepartmentDTO dto);

    DepartmentDTO updateDepartment(Long id, DepartmentDTO dto);

    /** 软删除：仅标记 deleted=1，不物理删除 */
    void deleteDepartment(Long id);

    DepartmentDTO getDepartmentById(Long id);

    List<DepartmentDTO> getAllDepartments();
}
