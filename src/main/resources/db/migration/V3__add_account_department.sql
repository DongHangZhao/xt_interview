-- ==================== 任务二：用户-部门关联 ====================
-- 给 sys_account 表新增 department_id 字段，外键关联 sys_department.id
-- 说明：字段可空，表示用户尚未分配部门（符合需求文档"添加/编辑时部门为可选字段"）
ALTER TABLE `sys_account`
    ADD COLUMN `department_id` BIGINT DEFAULT NULL COMMENT '所属部门ID' AFTER `is_super`;

-- 外键约束：确保 department_id 只能指向真实存在的部门
ALTER TABLE `sys_account`
    ADD CONSTRAINT `fk_account_department`
        FOREIGN KEY (`department_id`) REFERENCES `sys_department` (`id`)
        ON DELETE SET NULL
        ON UPDATE CASCADE;

-- 索引：加速按部门筛选用户的查询
CREATE INDEX `idx_account_department_id` ON `sys_account` (`department_id`);
