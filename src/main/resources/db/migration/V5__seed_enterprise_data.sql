-- ==================== V5：企业级演示数据 ====================
-- 用途：构造一套贴近真实企业的组织架构 + 人员数据，
--       用于验证权限体系、搜索分页、部门关联等功能。
--
-- 🔴 设计要点：账号表的 department_id 一律用
--        (SELECT id FROM sys_department WHERE code='XXX') 子查询，
--        **绝不硬编码部门ID**。硬编码会在部门ID 因测试数据发生漂移时
--        触发外键约束失败（Error 1452），且换库执行同样会挂。
--
-- 说明：幂等写入，先按 code/username 清理再插入，可重复执行。

-- ---------- 1. 清理旧的演示数据 ----------
-- 先删账号（有外键指向部门，必须先删子表）
DELETE FROM `sys_account` WHERE `username` LIKE 'emp\_%';

-- 再删部门。ON DELETE SET NULL 会在部门被删时自动置空账号的department_id，
-- 但账号已全部清理，这里显式声明依赖顺序以免误解
DELETE FROM `sys_department` WHERE `code` IN (
    'HQ','IT','IT_DEV','IT_OPS','IT_QA','IT_SEC',
    'HR','HR_REC','HR_RBP',
    'FIN','FIN_ACC','FIN_AUD',
    'MKT','MKT_BRAND','MKT_SALES',
    'CS','CS_SERVICE','CS_TECH'
);

-- ---------- 2. 部门表：一级部门 + 二级部门 ----------
INSERT INTO `sys_department` (`name`, `code`, `description`, `status`, `deleted`) VALUES
('欣铁科技',       'HQ',         '公司总部',              1, 0),
('信息技术中心',   'IT',         '公司整体信息化与技术支持', 1, 0),
('研发部',         'IT_DEV',     '产品研发与技术攻坚',      1, 0),
('运维部',         'IT_OPS',     '系统运维、部署与监控',     1, 0),
('测试部',         'IT_QA',      '质量保障与测试验证',       1, 0),
('信息安全部',     'IT_SEC',     '安全合规与风控',         1, 0),
('人力资源部',     'HR',         '人力资源管理',           1, 0),
('招聘组',         'HR_REC',     '社会招聘与雇主品牌',      1, 0),
('薪酬绩效组',     'HR_RBP',     '薪酬福利与绩效管理',      1, 0),
('财务部',         'FIN',        '财务核算与资金管理',      1, 0),
('会计组',         'FIN_ACC',    '账务处理与报表',          1, 0),
('审计组',         'FIN_AUD',    '内部审计与合规检查',      1, 0),
('市场部',         'MKT',        '品牌与市场推广',         1, 0),
('品牌组',         'MKT_BRAND',  '品牌策略与传播',         1, 0),
('销售组',         'MKT_SALES',  '客户拓展与签约',         1, 0),
('客户服务部',     'CS',         '客户支持与满意度',        1, 0),
('客服组',         'CS_SERVICE', '在线咨询与工单处理',      1, 0),
('技术支持组',     'CS_TECH',    '技术咨询与客户培训',      1, 0);

-- ---------- 3. 账号表：企业级人员分布 ----------
-- 所有账号密码统一为 MD5('123456') = e10adc3949ba59abbe56e057f20f883e
-- 权限分布：
--   role_level=3 超管：3 人（系统管理员 / CEO / CTO）
--   role_level=2 管理员：8 人（每个业务部门 1 人）
--   role_level=1 普通员工：约 30 人
--   status=0 禁用：5 人（验证登录拦截与列表筛选）

-- 超级管理员
INSERT INTO `sys_account`
(`username`, `name`, `gender`, `phone`, `password`, `role_level`, `status`, `is_super`, `department_id`, `deleted`) VALUES
('emp_admin', '系统管理员', '男', '13800000001', 'e10adc3949ba59abbe56e057f20f883e', 3, 1, 1,
 (SELECT id FROM sys_department WHERE code='HQ'), 0),
('emp_ceo',   '陈志强',    '男', '13800000002', 'e10adc3949ba59abbe56e057f20f883e', 3, 1, 1,
 (SELECT id FROM sys_department WHERE code='IT'), 0),
('emp_cto',   '李文博',    '男', '13800000003', 'e10adc3949ba59abbe56e057f20f883e', 3, 1, 1,
 (SELECT id FROM sys_department WHERE code='IT_DEV'), 0);

-- 部门管理员（role_level=2）
INSERT INTO `sys_account`
(`username`, `name`, `gender`, `phone`, `password`, `role_level`, `status`, `is_super`, `department_id`, `deleted`) VALUES
('emp_it_mgr',  '王志强', '男', '13800000010', 'e10adc3949ba59abbe56e057f20f883e', 2, 1, 0,
 (SELECT id FROM sys_department WHERE code='IT'), 0),
('emp_dev_mgr', '张开发', '男', '13800000011', 'e10adc3949ba59abbe56e057f20f883e', 2, 1, 0,
 (SELECT id FROM sys_department WHERE code='IT_DEV'), 0),
('emp_ops_mgr', '赵运维', '男', '13800000012', 'e10adc3949ba59abbe56e057f20f883e', 2, 1, 0,
 (SELECT id FROM sys_department WHERE code='IT_OPS'), 0),
('emp_hr_mgr',  '钱人事', '女', '13800000013', 'e10adc3949ba59abbe56e057f20f883e', 2, 1, 0,
 (SELECT id FROM sys_department WHERE code='HR'), 0),
('emp_fin_mgr', '孙财务', '女', '13800000014', 'e10adc3949ba59abbe56e057f20f883e', 2, 1, 0,
 (SELECT id FROM sys_department WHERE code='FIN'), 0),
('emp_mkt_mgr', '吴市场', '女', '13800000015', 'e10adc3949ba59abbe56e057f20f883e', 2, 1, 0,
 (SELECT id FROM sys_department WHERE code='MKT'), 0),
('emp_cs_mgr',  '郑客服', '女', '13800000016', 'e10adc3949ba59abbe56e057f20f883e', 2, 1, 0,
 (SELECT id FROM sys_department WHERE code='CS'), 0),
('emp_sec_mgr', '冯安全', '男', '13800000017', 'e10adc3949ba59abbe56e057f20f883e', 2, 1, 0,
 (SELECT id FROM sys_department WHERE code='IT_SEC'), 0);

-- 普通员工（role_level=1），分部门插入
INSERT INTO `sys_account`
(`username`, `name`, `gender`, `phone`, `password`, `role_level`, `status`, `is_super`, `department_id`, `deleted`) VALUES
-- 研发部
('emp_dev01', '刘代码', '男', '13800000101', 'e10adc3949ba59abbe56e057f20f883e', 1, 1, 0, (SELECT id FROM sys_department WHERE code='IT_DEV'), 0),
('emp_dev02', '陈前端', '男', '13800000102', 'e10adc3949ba59abbe56e057f20f883e', 1, 1, 0, (SELECT id FROM sys_department WHERE code='IT_DEV'), 0),
('emp_dev03', '杨后端', '女', '13800000103', 'e10adc3949ba59abbe56e057f20f883e', 1, 1, 0, (SELECT id FROM sys_department WHERE code='IT_DEV'), 0),
('emp_dev04', '黄算法', '男', '13800000104', 'e10adc3949ba59abbe56e057f20f883e', 1, 1, 0, (SELECT id FROM sys_department WHERE code='IT_DEV'), 0),
('emp_dev05', '周架构', '男', '13800000105', 'e10adc3949ba59abbe56e057f20f883e', 1, 1, 0, (SELECT id FROM sys_department WHERE code='IT_DEV'), 0),
('emp_dev06', '吴数据', '女', '13800000106', 'e10adc3949ba59abbe56e057f20f883e', 1, 1, 0, (SELECT id FROM sys_department WHERE code='IT_DEV'), 0),
('emp_dev07', '徐测试', '女', '13800000107', 'e10adc3949ba59abbe56e057f20f883e', 1, 1, 0, (SELECT id FROM sys_department WHERE code='IT_DEV'), 0),
('emp_dev08', '孙移动', '男', '13800000108', 'e10adc3949ba59abbe56e057f20f883e', 1, 1, 0, (SELECT id FROM sys_department WHERE code='IT_DEV'), 0),
-- 运维部
('emp_ops01', '马运维',   '男', '13800000201', 'e10adc3949ba59abbe56e057f20f883e', 1, 1, 0, (SELECT id FROM sys_department WHERE code='IT_OPS'), 0),
('emp_ops02', '朱监控',   '男', '13800000202', 'e10adc3949ba59abbe56e057f20f883e', 1, 1, 0, (SELECT id FROM sys_department WHERE code='IT_OPS'), 0),
('emp_ops03', '胡部署',   '男', '13800000203', 'e10adc3949ba59abbe56e057f20f883e', 1, 1, 0, (SELECT id FROM sys_department WHERE code='IT_OPS'), 0),
('emp_ops04', '林网络',   '男', '13800000204', 'e10adc3949ba59abbe56e057f20f883e', 1, 1, 0, (SELECT id FROM sys_department WHERE code='IT_OPS'), 0),
-- 测试部
('emp_qa01',  '高测试',   '女', '13800000301', 'e10adc3949ba59abbe56e057f20f883e', 1, 1, 0, (SELECT id FROM sys_department WHERE code='IT_QA'), 0),
('emp_qa02',  '何质量',   '女', '13800000302', 'e10adc3949ba59abbe56e057f20f883e', 1, 1, 0, (SELECT id FROM sys_department WHERE code='IT_QA'), 0),
('emp_qa03',  '郭回归',   '男', '13800000303', 'e10adc3949ba59abbe56e057f20f883e', 1, 1, 0, (SELECT id FROM sys_department WHERE code='IT_QA'), 0),
-- 信息安全部
('emp_sec01', '林安全',   '女', '13800000401', 'e10adc3949ba59abbe56e057f20f883e', 1, 1, 0, (SELECT id FROM sys_department WHERE code='IT_SEC'), 0),
('emp_sec02', '徐风控',   '男', '13800000402', 'e10adc3949ba59abbe56e057f20f883e', 1, 1, 0, (SELECT id FROM sys_department WHERE code='IT_SEC'), 0),
('emp_sec03', '沈合规',   '女', '13800000901', 'e10adc3949ba59abbe56e057f20f883e', 1, 1, 0, (SELECT id FROM sys_department WHERE code='IT_SEC'), 0),
('emp_sec04', '韩应急',   '男', '13800000902', 'e10adc3949ba59abbe56e057f20f883e', 1, 1, 0, (SELECT id FROM sys_department WHERE code='IT_SEC'), 0),
-- 人力资源部
('emp_hr01',  '沈招聘',   '女', '13800000501', 'e10adc3949ba59abbe56e057f20f883e', 1, 1, 0, (SELECT id FROM sys_department WHERE code='HR_REC'), 0),
('emp_hr02',  '韩薪酬',   '女', '13800000502', 'e10adc3949ba59abbe56e057f20f883e', 1, 1, 0, (SELECT id FROM sys_department WHERE code='HR_RBP'), 0),
-- 财务部
('emp_fin01', '朱会计',   '女', '13800000601', 'e10adc3949ba59abbe56e057f20f883e', 1, 1, 0, (SELECT id FROM sys_department WHERE code='FIN_ACC'), 0),
('emp_fin02', '秦出纳',   '女', '13800000602', 'e10adc3949ba59abbe56e057f20f883e', 1, 1, 0, (SELECT id FROM sys_department WHERE code='FIN_ACC'), 0),
('emp_fin03', '尤审计',   '男', '13800000603', 'e10adc3949ba59abbe56e057f20f883e', 1, 1, 0, (SELECT id FROM sys_department WHERE code='FIN_AUD'), 0),
-- 市场部
('emp_mkt01', '许品牌',   '女', '13800000701', 'e10adc3949ba59abbe56e057f20f883e', 1, 1, 0, (SELECT id FROM sys_department WHERE code='MKT_BRAND'), 0),
('emp_mkt02', '何推广',   '女', '13800000702', 'e10adc3949ba59abbe56e057f20f883e', 1, 1, 0, (SELECT id FROM sys_department WHERE code='MKT_BRAND'), 0),
('emp_mkt03', '吕销售',   '男', '13800000703', 'e10adc3949ba59abbe56e057f20f883e', 1, 1, 0, (SELECT id FROM sys_department WHERE code='MKT_SALES'), 0),
-- 客户服务部
('emp_cs01',  '张客服',   '女', '13800000801', 'e10adc3949ba59abbe56e057f20f883e', 1, 1, 0, (SELECT id FROM sys_department WHERE code='CS_SERVICE'), 0),
('emp_cs02',  '孔工单',   '女', '13800000802', 'e10adc3949ba59abbe56e057f20f883e', 1, 1, 0, (SELECT id FROM sys_department WHERE code='CS_SERVICE'), 0),
('emp_cs03',  '曹咨询',   '男', '13800000803', 'e10adc3949ba59abbe56e057f20f883e', 1, 1, 0, (SELECT id FROM sys_department WHERE code='CS_TECH'), 0),
-- 总部职能
('emp_hq01',  '姚前台',   '女', '13800001001', 'e10adc3949ba59abbe56e057f20f883e', 1, 1, 0, (SELECT id FROM sys_department WHERE code='HQ'), 0),
('emp_hq02',  '崔助理',   '女', '13800001002', 'e10adc3949ba59abbe56e057f20f883e', 1, 1, 0, (SELECT id FROM sys_department WHERE code='HQ'), 0);

-- 禁用账号（status=0）：验证登录被拦截、列表筛选正常
INSERT INTO `sys_account`
(`username`, `name`, `gender`, `phone`, `password`, `role_level`, `status`, `is_super`, `department_id`, `deleted`) VALUES
('emp_leave01', '离职甲', '男', '13800001101', 'e10adc3949ba59abbe56e057f20f883e', 1, 0, 0, (SELECT id FROM sys_department WHERE code='IT_DEV'), 0),
('emp_leave02', '离职乙', '女', '13800001102', 'e10adc3949ba59abbe56e057f20f883e', 1, 0, 0, (SELECT id FROM sys_department WHERE code='IT_OPS'), 0),
('emp_leave03', '离职丙', '男', '13800001103', 'e10adc3949ba59abbe56e057f20f883e', 2, 0, 0, (SELECT id FROM sys_department WHERE code='FIN'), 0);
