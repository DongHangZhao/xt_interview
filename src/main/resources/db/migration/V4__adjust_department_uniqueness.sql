-- ==================== 任务二补充：部门唯一性策略调整 ====================
--
-- 背景：code 上原建有 UNIQUE 唯一索引，但部门采用软删除（仅标记 deleted=1，物理行仍在）。
-- 唯一索引作用在物理行上，导致「已删除的部门编码被永久占用、无法复用」，
-- 且应用层校验（查全表）与数据库约束（查全表）之间产生语义错位。
--
-- 调整策略：唯一性改由应用层 Service 负责（校验「未删除」范围内的唯一），
-- 数据库层降级为普通索引，仅用于查询加速。
-- 好处：软删除后编码可正常复用，符合业务直觉；同时保留查询性能。

-- 1. code 唯一索引降级为普通索引
ALTER TABLE `sys_department` DROP INDEX `uk_department_code`;
ALTER TABLE `sys_department` ADD INDEX `idx_department_code` (`code`);

-- 2. 新增 name 索引，配合应用层校验「未删除范围内名称唯一」
ALTER TABLE `sys_department` ADD INDEX `idx_department_name` (`name`);

-- 3. 用户名同理：账号表也是软删除，uk_username 会导致软删除后无法重建同名用户
ALTER TABLE `sys_account` DROP INDEX `uk_username`;
ALTER TABLE `sys_account` ADD INDEX `idx_username` (`username`);
