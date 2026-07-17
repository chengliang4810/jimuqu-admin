-- 积木区管理系统初始化数据脚本
-- 生成时间: 2025-09-18
-- 说明: 本文件包含系统所有初始数据，用于数据库初始化

-- ===========================================================
-- 1. 客户端初始值
-- ===========================================================

REPLACE INTO `sys_client` (`id`, `client_id`, `client_key`, `client_secret`, `grant_type`, `device_type`, `active_timeout`, `timeout`, `status`, `del_flag`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (1, 'e5cd7e4891bf95d1d19206ce24a7b32e', 'pc', 'pc123', 'password,social', 'pc', 1800, 604800, '0', '0', 103, 1, '2024-07-31 18:46:52', 1, '2024-07-31 18:46:52');
REPLACE INTO `sys_client` (`id`, `client_id`, `client_key`, `client_secret`, `grant_type`, `device_type`, `active_timeout`, `timeout`, `status`, `del_flag`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (2, '428a8310cd442757ae699df5d894f051', 'app', 'app123', 'password,sms,social', 'android', 1800, 604800, '0', '0', 103, 1, '2024-07-31 18:46:52', 1, '2024-07-31 18:46:52');

-- ===========================================================
-- 2. 系统配置初始值
-- ===========================================================

REPLACE INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (1, '主框架页-默认皮肤样式名称', 'sys.index.skinName', 'skin-blue', 'Y', '蓝色 skin-blue、绿色 skin-green、紫色 skin-purple、红色 skin-red、黄色 skin-yellow', 103, 1, '2024-07-31 18:46:52', NULL, NULL);
REPLACE INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (2, '用户管理-账号初始密码', 'sys.user.initPassword', '123456', 'Y', '初始化密码 123456', 103, 1, '2024-07-31 18:46:52', NULL, NULL);
REPLACE INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (3, '主框架页-侧边栏主题', 'sys.index.sideTheme', 'theme-dark', 'Y', '深色主题theme-dark，浅色主题theme-light', 103, 1, '2024-07-31 18:46:52', NULL, NULL);
REPLACE INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (5, '账号自助-是否开启用户注册功能', 'sys.account.registerUser', 'false', 'Y', '是否开启注册用户功能（true开启，false关闭）', 103, 1, '2024-07-31 18:46:52', NULL, NULL);
REPLACE INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (11, 'OSS预览列表资源开关', 'sys.oss.previewListResource', 'true', 'Y', 'true:开启, false:关闭', 103, 1, '2024-07-31 18:46:52', NULL, NULL);

-- ===========================================================
-- 3. 部门初始值
-- ===========================================================

REPLACE INTO `sys_dept` (`id`, `parent_id`, `ancestors`, `dept_name`, `order_num`, `leader`, `phone`, `email`, `status`, `del_flag`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (100, 0, '0', '积木区科技有限公司', 0, NULL, '15888888888', 'xxx@qq.com', '0', '0', 103, 1, '2024-07-31 18:46:50', NULL, NULL);
REPLACE INTO `sys_dept` (`id`, `parent_id`, `ancestors`, `dept_name`, `order_num`, `leader`, `phone`, `email`, `status`, `del_flag`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (101, 100, '0,100', '深圳总公司', 1, NULL, '15888888888', 'xxx@qq.com', '0', '0', 103, 1, '2024-07-31 18:46:50', NULL, NULL);
REPLACE INTO `sys_dept` (`id`, `parent_id`, `ancestors`, `dept_name`, `order_num`, `leader`, `phone`, `email`, `status`, `del_flag`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (102, 100, '0,100', '长沙分公司', 2, NULL, '15888888888', 'xxx@qq.com', '0', '0', 103, 1, '2024-07-31 18:46:50', NULL, NULL);
REPLACE INTO `sys_dept` (`id`, `parent_id`, `ancestors`, `dept_name`, `order_num`, `leader`, `phone`, `email`, `status`, `del_flag`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (103, 101, '0,100,101', '研发部门', 1, 1, '15888888888', 'xxx@qq.com', '0', '0', 103, 1, '2024-07-31 18:46:50', NULL, NULL);
REPLACE INTO `sys_dept` (`id`, `parent_id`, `ancestors`, `dept_name`, `order_num`, `leader`, `phone`, `email`, `status`, `del_flag`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (104, 101, '0,100,101', '市场部门', 2, NULL, '15888888888', 'xxx@qq.com', '0', '0', 103, 1, '2024-07-31 18:46:50', NULL, NULL);
REPLACE INTO `sys_dept` (`id`, `parent_id`, `ancestors`, `dept_name`, `order_num`, `leader`, `phone`, `email`, `status`, `del_flag`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (105, 101, '0,100,101', '测试部门', 3, NULL, '15888888888', 'xxx@qq.com', '0', '0', 103, 1, '2024-07-31 18:46:50', NULL, NULL);
REPLACE INTO `sys_dept` (`id`, `parent_id`, `ancestors`, `dept_name`, `order_num`, `leader`, `phone`, `email`, `status`, `del_flag`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (106, 101, '0,100,101', '财务部门', 4, NULL, '15888888888', 'xxx@qq.com', '0', '0', 103, 1, '2024-07-31 18:46:50', NULL, NULL);
REPLACE INTO `sys_dept` (`id`, `parent_id`, `ancestors`, `dept_name`, `order_num`, `leader`, `phone`, `email`, `status`, `del_flag`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (107, 101, '0,100,101', '运维部门', 5, NULL, '15888888888', 'xxx@qq.com', '0', '0', 103, 1, '2024-07-31 18:46:50', NULL, NULL);
REPLACE INTO `sys_dept` (`id`, `parent_id`, `ancestors`, `dept_name`, `order_num`, `leader`, `phone`, `email`, `status`, `del_flag`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (108, 102, '0,100,102', '市场部门', 1, NULL, '15888888888', 'xxx@qq.com', '0', '0', 103, 1, '2024-07-31 18:46:50', NULL, NULL);
REPLACE INTO `sys_dept` (`id`, `parent_id`, `ancestors`, `dept_name`, `order_num`, `leader`, `phone`, `email`, `status`, `del_flag`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (109, 102, '0,100,102', '财务部门', 2, NULL, '15888888888', 'xxx@qq.com', '0', '0', 103, 1, '2024-07-31 18:46:50', NULL, NULL);

-- ===========================================================
-- 4. 字典类型初始值
-- ===========================================================

REPLACE INTO `sys_dict_type` (`dict_id`, `dict_key`, `dict_name`, `dict_type`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (1, 'sys_user_sex', '用户性别', 'L', '用户性别列表', 103, 1, '2024-07-31 18:46:52', NULL, NULL);
REPLACE INTO `sys_dict_type` (`dict_id`, `dict_key`, `dict_name`, `dict_type`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (2, 'sys_show_hide', '菜单状态', 'L', '菜单状态列表', 103, 1, '2024-07-31 18:46:52', NULL, NULL);
REPLACE INTO `sys_dict_type` (`dict_id`, `dict_key`, `dict_name`, `dict_type`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (3, 'sys_normal_disable', '系统开关', 'L', '系统开关列表', 103, 1, '2024-07-31 18:46:52', NULL, NULL);
REPLACE INTO `sys_dict_type` (`dict_id`, `dict_key`, `dict_name`, `dict_type`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (6, 'sys_yes_no', '系统是否', 'L', '系统是否列表', 103, 1, '2024-07-31 18:46:52', NULL, NULL);
REPLACE INTO `sys_dict_type` (`dict_id`, `dict_key`, `dict_name`, `dict_type`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (7, 'sys_notice_type', '通知类型', 'L', '通知类型列表', 103, 1, '2024-07-31 18:46:52', NULL, NULL);
REPLACE INTO `sys_dict_type` (`dict_id`, `dict_key`, `dict_name`, `dict_type`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (8, 'sys_notice_status', '通知状态', 'L', '通知状态列表', 103, 1, '2024-07-31 18:46:52', NULL, NULL);
REPLACE INTO `sys_dict_type` (`dict_id`, `dict_key`, `dict_name`, `dict_type`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (9, 'sys_oper_type', '操作类型', 'L', '操作类型列表', 103, 1, '2024-07-31 18:46:52', NULL, NULL);
REPLACE INTO `sys_dict_type` (`dict_id`, `dict_key`, `dict_name`, `dict_type`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (10, 'sys_common_status', '系统状态', 'L', '登录状态列表', 103, 1, '2024-07-31 18:46:52', NULL, NULL);
REPLACE INTO `sys_dict_type` (`dict_id`, `dict_key`, `dict_name`, `dict_type`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (11, 'sys_grant_type', '授权类型', 'L', '认证授权类型', 103, 1, '2024-07-31 18:46:52', NULL, NULL);
REPLACE INTO `sys_dict_type` (`dict_id`, `dict_key`, `dict_name`, `dict_type`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (12, 'sys_device_type', '设备类型', 'L', '客户端设备类型', 103, 1, '2024-07-31 18:46:52', NULL, NULL);

-- ===========================================================
-- 5. 字典选项初始值
-- ===========================================================

REPLACE INTO `sys_dict_data` (`id`, `parent_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type_key`, `css_class`, `list_class`, `is_default`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (1, 0, 1, '男', '0', 'sys_user_sex', '', '', '0', '性别男', 103, 1, '2024-07-31 18:46:52', NULL, NULL);
REPLACE INTO `sys_dict_data` (`id`, `parent_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type_key`, `css_class`, `list_class`, `is_default`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (2, 0, 2, '女', '1', 'sys_user_sex', '', '', '0', '性别女', 103, 1, '2024-07-31 18:46:52', NULL, NULL);
REPLACE INTO `sys_dict_data` (`id`, `parent_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type_key`, `css_class`, `list_class`, `is_default`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (3, 0, 3, '未知', '2', 'sys_user_sex', '', '', '0', '性别未知', 103, 1, '2024-07-31 18:46:52', NULL, NULL);
REPLACE INTO `sys_dict_data` (`id`, `parent_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type_key`, `css_class`, `list_class`, `is_default`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (4, 0, 1, '显示', '0', 'sys_show_hide', '', 'primary', '0', '显示菜单', 103, 1, '2024-07-31 18:46:52', NULL, NULL);
REPLACE INTO `sys_dict_data` (`id`, `parent_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type_key`, `css_class`, `list_class`, `is_default`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (5, 0, 2, '隐藏', '1', 'sys_show_hide', '', 'danger', '0', '隐藏菜单', 103, 1, '2024-07-31 18:46:52', NULL, NULL);
REPLACE INTO `sys_dict_data` (`id`, `parent_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type_key`, `css_class`, `list_class`, `is_default`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (6, 0, 1, '正常', '0', 'sys_normal_disable', '', 'primary', '0', '正常状态', 103, 1, '2024-07-31 18:46:52', NULL, NULL);
REPLACE INTO `sys_dict_data` (`id`, `parent_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type_key`, `css_class`, `list_class`, `is_default`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (7, 0, 2, '停用', '1', 'sys_normal_disable', '', 'danger', '0', '停用状态', 103, 1, '2024-07-31 18:46:52', NULL, NULL);
REPLACE INTO `sys_dict_data` (`id`, `parent_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type_key`, `css_class`, `list_class`, `is_default`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (12, 0, 1, '是', 'Y', 'sys_yes_no', '', 'primary', '0', '系统默认是', 103, 1, '2024-07-31 18:46:52', NULL, NULL);
REPLACE INTO `sys_dict_data` (`id`, `parent_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type_key`, `css_class`, `list_class`, `is_default`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (13, 0, 2, '否', 'N', 'sys_yes_no', '', 'danger', '0', '系统默认否', 103, 1, '2024-07-31 18:46:52', NULL, NULL);
REPLACE INTO `sys_dict_data` (`id`, `parent_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type_key`, `css_class`, `list_class`, `is_default`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (14, 0, 1, '通知', '1', 'sys_notice_type', '', 'warning', '0', '通知', 103, 1, '2024-07-31 18:46:52', NULL, NULL);
REPLACE INTO `sys_dict_data` (`id`, `parent_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type_key`, `css_class`, `list_class`, `is_default`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (15, 0, 2, '公告', '2', 'sys_notice_type', '', 'success', '0', '公告', 103, 1, '2024-07-31 18:46:52', NULL, NULL);
REPLACE INTO `sys_dict_data` (`id`, `parent_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type_key`, `css_class`, `list_class`, `is_default`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (16, 0, 1, '正常', '0', 'sys_notice_status', '', 'primary', '0', '正常状态', 103, 1, '2024-07-31 18:46:52', NULL, NULL);
REPLACE INTO `sys_dict_data` (`id`, `parent_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type_key`, `css_class`, `list_class`, `is_default`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (17, 0, 2, '关闭', '1', 'sys_notice_status', '', 'danger', '0', '关闭状态', 103, 1, '2024-07-31 18:46:52', NULL, NULL);
REPLACE INTO `sys_dict_data` (`id`, `parent_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type_key`, `css_class`, `list_class`, `is_default`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (18, 0, 1, '新增', '1', 'sys_oper_type', '', 'info', '0', '新增操作', 103, 1, '2024-07-31 18:46:52', NULL, NULL);
REPLACE INTO `sys_dict_data` (`id`, `parent_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type_key`, `css_class`, `list_class`, `is_default`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (19, 0, 2, '修改', '2', 'sys_oper_type', '', 'info', '0', '修改操作', 103, 1, '2024-07-31 18:46:52', NULL, NULL);
REPLACE INTO `sys_dict_data` (`id`, `parent_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type_key`, `css_class`, `list_class`, `is_default`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (20, 0, 3, '删除', '3', 'sys_oper_type', '', 'danger', '0', '删除操作', 103, 1, '2024-07-31 18:46:52', NULL, NULL);
REPLACE INTO `sys_dict_data` (`id`, `parent_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type_key`, `css_class`, `list_class`, `is_default`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (21, 0, 4, '授权', '4', 'sys_oper_type', '', 'primary', '0', '授权操作', 103, 1, '2024-07-31 18:46:52', NULL, NULL);
REPLACE INTO `sys_dict_data` (`id`, `parent_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type_key`, `css_class`, `list_class`, `is_default`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (22, 0, 5, '导出', '5', 'sys_oper_type', '', 'warning', '0', '导出操作', 103, 1, '2024-07-31 18:46:52', NULL, NULL);
REPLACE INTO `sys_dict_data` (`id`, `parent_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type_key`, `css_class`, `list_class`, `is_default`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (23, 0, 6, '导入', '6', 'sys_oper_type', '', 'warning', '0', '导入操作', 103, 1, '2024-07-31 18:46:52', NULL, NULL);
REPLACE INTO `sys_dict_data` (`id`, `parent_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type_key`, `css_class`, `list_class`, `is_default`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (24, 0, 7, '强退', '7', 'sys_oper_type', '', 'danger', '0', '强退操作', 103, 1, '2024-07-31 18:46:52', NULL, NULL);
REPLACE INTO `sys_dict_data` (`id`, `parent_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type_key`, `css_class`, `list_class`, `is_default`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (25, 0, 8, '生成代码', '8', 'sys_oper_type', '', 'warning', '0', '生成操作', 103, 1, '2024-07-31 18:46:52', NULL, NULL);
REPLACE INTO `sys_dict_data` (`id`, `parent_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type_key`, `css_class`, `list_class`, `is_default`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (26, 0, 9, '清空数据', '9', 'sys_oper_type', '', 'danger', '0', '清空操作', 103, 1, '2024-07-31 18:46:52', NULL, NULL);
REPLACE INTO `sys_dict_data` (`id`, `parent_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type_key`, `css_class`, `list_class`, `is_default`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (27, 0, 1, '成功', '0', 'sys_common_status', '', 'primary', '0', '正常状态', 103, 1, '2024-07-31 18:46:52', NULL, NULL);
REPLACE INTO `sys_dict_data` (`id`, `parent_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type_key`, `css_class`, `list_class`, `is_default`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (28, 0, 2, '失败', '1', 'sys_common_status', '', 'danger', '0', '停用状态', 103, 1, '2024-07-31 18:46:52', NULL, NULL);
REPLACE INTO `sys_dict_data` (`id`, `parent_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type_key`, `css_class`, `list_class`, `is_default`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (29, 0, 99, '其他', '0', 'sys_oper_type', '', 'info', '0', '其他操作', 103, 1, '2024-07-31 18:46:52', NULL, NULL);
REPLACE INTO `sys_dict_data` (`id`, `parent_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type_key`, `css_class`, `list_class`, `is_default`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (30, 0, 0, '密码认证', 'password', 'sys_grant_type', 'el-check-tag', 'default', '0', '密码认证', 103, 1, '2024-07-31 18:46:52', NULL, NULL);
REPLACE INTO `sys_dict_data` (`id`, `parent_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type_key`, `css_class`, `list_class`, `is_default`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (31, 0, 0, '短信认证', 'sms', 'sys_grant_type', 'el-check-tag', 'default', '0', '短信认证', 103, 1, '2024-07-31 18:46:52', NULL, NULL);
REPLACE INTO `sys_dict_data` (`id`, `parent_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type_key`, `css_class`, `list_class`, `is_default`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (32, 0, 0, '邮件认证', 'email', 'sys_grant_type', 'el-check-tag', 'default', '0', '邮件认证', 103, 1, '2024-07-31 18:46:52', NULL, NULL);
REPLACE INTO `sys_dict_data` (`id`, `parent_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type_key`, `css_class`, `list_class`, `is_default`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (33, 0, 0, '小程序认证', 'xcx', 'sys_grant_type', 'el-check-tag', 'default', '0', '小程序认证', 103, 1, '2024-07-31 18:46:52', NULL, NULL);
REPLACE INTO `sys_dict_data` (`id`, `parent_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type_key`, `css_class`, `list_class`, `is_default`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (34, 0, 0, '三方登录认证', 'social', 'sys_grant_type', 'el-check-tag', 'default', '0', '三方登录认证', 103, 1, '2024-07-31 18:46:52', NULL, NULL);
REPLACE INTO `sys_dict_data` (`id`, `parent_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type_key`, `css_class`, `list_class`, `is_default`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (35, 0, 0, 'PC', 'pc', 'sys_device_type', '', 'default', '0', 'PC', 103, 1, '2024-07-31 18:46:52', NULL, NULL);
REPLACE INTO `sys_dict_data` (`id`, `parent_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type_key`, `css_class`, `list_class`, `is_default`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (36, 0, 0, '安卓', 'android', 'sys_device_type', '', 'default', '0', '安卓', 103, 1, '2024-07-31 18:46:52', NULL, NULL);
REPLACE INTO `sys_dict_data` (`id`, `parent_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type_key`, `css_class`, `list_class`, `is_default`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (37, 0, 0, 'iOS', 'ios', 'sys_device_type', '', 'default', '0', 'iOS', 103, 1, '2024-07-31 18:46:52', NULL, NULL);
REPLACE INTO `sys_dict_data` (`id`, `parent_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type_key`, `css_class`, `list_class`, `is_default`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (38, 0, 0, '小程序', 'xcx', 'sys_device_type', '', 'default', '0', '小程序', 103, 1, '2024-07-31 18:46:52', NULL, NULL);

-- ===========================================================
-- 6. 菜单初始值
-- ===========================================================

REPLACE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (1, 0, '系统管理', 1, 'system', NULL, '', '1', '0', 'M', '0', '0', '', 'eos-icons:system-group', '系统管理目录', 103, 1, '2025-05-27 13:19:44', NULL, NULL);
REPLACE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (2, 0, '系统监控', 3, 'monitor', '', '', '1', '0', 'M', '0', '0', '', 'solar:monitor-camera-outline', '系统监控目录', 103, 1, '2025-05-27 13:19:44', 1, '2025-06-13 06:07:39');
REPLACE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (4, 0, '官网', 5, 'https://doc.jimuqu.com', NULL, '', '0', '0', 'M', '0', '0', '', 'flat-color-icons:plus', '官网地址', 103, 1, '2025-05-27 13:19:44', NULL, NULL);
REPLACE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (100, 1, '用户管理', 1, 'user', 'system/user/index', '', '1', '0', 'C', '0', '0', 'system:user:list', 'ant-design:user-outlined', '用户管理菜单', 103, 1, '2025-05-27 13:19:44', NULL, NULL);
REPLACE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (101, 1, '角色管理', 2, 'role', 'system/role/index', '', '1', '0', 'C', '0', '0', 'system:role:list', 'eos-icons:role-binding-outlined', '角色管理菜单', 103, 1, '2025-05-27 13:19:44', NULL, NULL);
REPLACE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (102, 1, '菜单管理', 3, 'menu', 'system/menu/index', '', '1', '0', 'C', '0', '0', 'system:menu:list', 'ic:sharp-menu', '菜单管理菜单', 103, 1, '2025-05-27 13:19:44', NULL, NULL);
REPLACE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (103, 1, '部门管理', 4, 'dept', 'system/dept/index', '', '1', '0', 'C', '0', '0', 'system:dept:list', 'mingcute:department-line', '部门管理菜单', 103, 1, '2025-05-27 13:19:44', NULL, NULL);
REPLACE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (104, 1, '岗位管理', 5, 'post', 'system/post/index', '', '1', '0', 'C', '0', '0', 'system:post:list', 'icon-park-outline:appointment', '岗位管理菜单', 103, 1, '2025-05-27 13:19:44', NULL, NULL);
REPLACE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (105, 1, '字典管理', 6, 'dict', 'system/dict/index', '', '1', '0', 'C', '0', '0', 'system:dict:list', 'fluent-mdl2:dictionary', '字典管理菜单', 103, 1, '2025-05-27 13:19:44', NULL, NULL);
REPLACE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (106, 1, '参数设置', 7, 'config', 'system/config/index', '', '1', '0', 'C', '0', '0', 'system:config:list', 'ant-design:setting-outlined', '参数设置菜单', 103, 1, '2025-05-27 13:19:44', NULL, NULL);
REPLACE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (107, 1, '通知公告', 8, 'notice', 'system/notice/index', '', '1', '0', 'C', '0', '1', 'system:notice:list', 'fe:notice-push', '通知公告菜单', 103, 1, '2025-05-27 13:19:44', NULL, NULL);
REPLACE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (108, 1, '日志管理', 9, 'log', '', '', '1', '0', 'M', '0', '1', '', 'material-symbols:logo-dev-outline', '日志管理菜单', 103, 1, '2025-05-27 13:19:44', NULL, NULL);
REPLACE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (109, 2, '在线用户', 1, 'online', 'monitor/online/index', '', '1', '0', 'C', '0', '1', 'monitor:online:list', 'material-symbols:generating-tokens-outline', '在线用户菜单', 103, 1, '2025-05-27 13:19:44', 1, '2025-06-06 15:25:17');
REPLACE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (118, 1, '文件管理', 10, 'oss', 'system/oss/index', '', '1', '0', 'C', '0', '1', 'system:oss:list', 'solar:folder-with-files-outline', '文件管理菜单', 103, 1, '2025-05-27 13:19:44', NULL, NULL);
REPLACE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (123, 1, '客户端管理', 11, 'client', 'system/client/index', '', '1', '0', 'C', '0', '0', 'system:client:list', 'solar:monitor-smartphone-outline', '客户端管理菜单', 103, 1, '2025-05-27 13:19:44', NULL, NULL);

-- 用户管理按钮
REPLACE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (1001, 100, '用户查询', 1, '', '', '', '1', '0', 'F', '0', '0', 'system:user:query', '#', '', 103, 1, '2025-05-27 13:19:44', NULL, NULL);
REPLACE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (1002, 100, '用户新增', 2, '', '', '', '1', '0', 'F', '0', '0', 'system:user:add', '#', '', 103, 1, '2025-05-27 13:19:44', NULL, NULL);
REPLACE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (1003, 100, '用户修改', 3, '', '', '', '1', '0', 'F', '0', '0', 'system:user:edit', '#', '', 103, 1, '2025-05-27 13:19:44', NULL, NULL);
REPLACE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (1004, 100, '用户删除', 4, '', '', '', '1', '0', 'F', '0', '0', 'system:user:remove', '#', '', 103, 1, '2025-05-27 13:19:44', NULL, NULL);
REPLACE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (1005, 100, '用户导出', 5, '', '', '', '1', '0', 'F', '0', '0', 'system:user:export', '#', '', 103, 1, '2025-05-27 13:19:44', NULL, NULL);
REPLACE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (1006, 100, '用户导入', 6, '', '', '', '1', '0', 'F', '0', '0', 'system:user:import', '#', '', 103, 1, '2025-05-27 13:19:44', NULL, NULL);
REPLACE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (1007, 100, '重置密码', 7, '', '', '', '1', '0', 'F', '0', '0', 'system:user:resetPwd', '#', '', 103, 1, '2025-05-27 13:19:44', NULL, NULL);

-- 角色管理按钮
REPLACE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (1008, 101, '角色查询', 1, '', '', '', '1', '0', 'F', '0', '0', 'system:role:query', '#', '', 103, 1, '2025-05-27 13:19:44', NULL, NULL);
REPLACE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (1009, 101, '角色新增', 2, '', '', '', '1', '0', 'F', '0', '0', 'system:role:add', '#', '', 103, 1, '2025-05-27 13:19:44', NULL, NULL);
REPLACE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (1010, 101, '角色修改', 3, '', '', '', '1', '0', 'F', '0', '0', 'system:role:edit', '#', '', 103, 1, '2025-05-27 13:19:44', NULL, NULL);
REPLACE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (1011, 101, '角色删除', 4, '', '', '', '1', '0', 'F', '0', '0', 'system:role:remove', '#', '', 103, 1, '2025-05-27 13:19:44', NULL, NULL);
REPLACE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (1012, 101, '角色导出', 5, '', '', '', '1', '0', 'F', '0', '0', 'system:role:export', '#', '', 103, 1, '2025-05-27 13:19:44', NULL, NULL);

-- 菜单管理按钮
REPLACE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (1013, 102, '菜单查询', 1, '', '', '', '1', '0', 'F', '0', '0', 'system:menu:query', '#', '', 103, 1, '2025-05-27 13:19:44', NULL, NULL);
REPLACE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (1014, 102, '菜单新增', 2, '', '', '', '1', '0', 'F', '0', '0', 'system:menu:add', '#', '', 103, 1, '2025-05-27 13:19:44', NULL, NULL);
REPLACE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (1015, 102, '菜单修改', 3, '', '', '', '1', '0', 'F', '0', '0', 'system:menu:edit', '#', '', 103, 1, '2025-05-27 13:19:44', NULL, NULL);
REPLACE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (1016, 102, '菜单删除', 4, '', '', '', '1', '0', 'F', '0', '0', 'system:menu:remove', '#', '', 103, 1, '2025-05-27 13:19:44', NULL, NULL);

-- 部门管理按钮
REPLACE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (1017, 103, '部门查询', 1, '', '', '', '1', '0', 'F', '0', '0', 'system:dept:query', '#', '', 103, 1, '2025-05-27 13:19:44', NULL, NULL);
REPLACE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (1018, 103, '部门新增', 2, '', '', '', '1', '0', 'F', '0', '0', 'system:dept:add', '#', '', 103, 1, '2025-05-27 13:19:44', NULL, NULL);
REPLACE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (1019, 103, '部门修改', 3, '', '', '', '1', '0', 'F', '0', '0', 'system:dept:edit', '#', '', 103, 1, '2025-05-27 13:19:44', NULL, NULL);
REPLACE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (1020, 103, '部门删除', 4, '', '', '', '1', '0', 'F', '0', '0', 'system:dept:remove', '#', '', 103, 1, '2025-05-27 13:19:44', NULL, NULL);

-- 岗位管理按钮
REPLACE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (1021, 104, '岗位查询', 1, '', '', '', '1', '0', 'F', '0', '0', 'system:post:query', '#', '', 103, 1, '2025-05-27 13:19:44', NULL, NULL);
REPLACE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (1022, 104, '岗位新增', 2, '', '', '', '1', '0', 'F', '0', '0', 'system:post:add', '#', '', 103, 1, '2025-05-27 13:19:44', NULL, NULL);
REPLACE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (1023, 104, '岗位修改', 3, '', '', '', '1', '0', 'F', '0', '0', 'system:post:edit', '#', '', 103, 1, '2025-05-27 13:19:44', NULL, NULL);
REPLACE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (1024, 104, '岗位删除', 4, '', '', '', '1', '0', 'F', '0', '0', 'system:post:remove', '#', '', 103, 1, '2025-05-27 13:19:44', NULL, NULL);
REPLACE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (1025, 104, '岗位导出', 5, '', '', '', '1', '0', 'F', '0', '0', 'system:post:export', '#', '', 103, 1, '2025-05-27 13:19:44', NULL, NULL);

-- 字典管理按钮
REPLACE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (1026, 105, '字典查询', 1, '#', '', '', '1', '0', 'F', '0', '0', 'system:dict:query', '#', '', 103, 1, '2025-05-27 13:19:44', NULL, NULL);
REPLACE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (1027, 105, '字典新增', 2, '#', '', '', '1', '0', 'F', '0', '0', 'system:dict:add', '#', '', 103, 1, '2025-05-27 13:19:44', NULL, NULL);
REPLACE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (1028, 105, '字典修改', 3, '#', '', '', '1', '0', 'F', '0', '0', 'system:dict:edit', '#', '', 103, 1, '2025-05-27 13:19:44', NULL, NULL);
REPLACE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (1029, 105, '字典删除', 4, '#', '', '', '1', '0', 'F', '0', '0', 'system:dict:remove', '#', '', 103, 1, '2025-05-27 13:19:44', NULL, NULL);
REPLACE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (1030, 105, '字典导出', 5, '#', '', '', '1', '0', 'F', '0', '0', 'system:dict:export', '#', '', 103, 1, '2025-05-27 13:19:44', NULL, NULL);

-- 参数设置按钮
REPLACE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (1031, 106, '参数查询', 1, '#', '', '', '1', '0', 'F', '0', '0', 'system:config:query', '#', '', 103, 1, '2025-05-27 13:19:44', NULL, NULL);
REPLACE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (1032, 106, '参数新增', 2, '#', '', '', '1', '0', 'F', '0', '0', 'system:config:add', '#', '', 103, 1, '2025-05-27 13:19:44', NULL, NULL);
REPLACE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (1033, 106, '参数修改', 3, '#', '', '', '1', '0', 'F', '0', '0', 'system:config:edit', '#', '', 103, 1, '2025-05-27 13:19:44', NULL, NULL);
REPLACE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (1034, 106, '参数删除', 4, '#', '', '', '1', '0', 'F', '0', '0', 'system:config:remove', '#', '', 103, 1, '2025-05-27 13:19:44', NULL, NULL);
REPLACE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (1035, 106, '参数导出', 5, '#', '', '', '1', '0', 'F', '0', '0', 'system:config:export', '#', '', 103, 1, '2025-05-27 13:19:44', NULL, NULL);


-- 客户端管理按钮
REPLACE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (1061, 123, '客户端管理查询', 1, '#', '', '', '1', '0', 'F', '0', '0', 'system:client:query', '#', '', 103, 1, '2025-05-27 13:19:44', NULL, NULL);
REPLACE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (1062, 123, '客户端管理新增', 2, '#', '', '', '1', '0', 'F', '0', '0', 'system:client:add', '#', '', 103, 1, '2025-05-27 13:19:44', NULL, NULL);
REPLACE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (1063, 123, '客户端管理修改', 3, '#', '', '', '1', '0', 'F', '0', '0', 'system:client:edit', '#', '', 103, 1, '2025-05-27 13:19:44', NULL, NULL);
REPLACE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (1064, 123, '客户端管理删除', 4, '#', '', '', '1', '0', 'F', '0', '0', 'system:client:remove', '#', '', 103, 1, '2025-05-27 13:19:44', NULL, NULL);
REPLACE INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (1065, 123, '客户端管理导出', 5, '#', '', '', '1', '0', 'F', '0', '0', 'system:client:export', '#', '', 103, 1, '2025-05-27 13:19:44', NULL, NULL);

-- ===========================================================
-- 9. 岗位初始值
-- ===========================================================

REPLACE INTO `sys_post` (`post_id`, `dept_id`, `post_code`, `post_category`, `post_name`, `post_sort`, `status`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (1, 103, 'ceo', NULL, '董事长', 1, '0', '', 103, 1, '2025-06-04 17:22:27', NULL, NULL);
REPLACE INTO `sys_post` (`post_id`, `dept_id`, `post_code`, `post_category`, `post_name`, `post_sort`, `status`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (2, 100, 'se', NULL, '项目经理', 2, '0', '', 103, 1, '2025-06-04 17:22:27', NULL, NULL);
REPLACE INTO `sys_post` (`post_id`, `dept_id`, `post_code`, `post_category`, `post_name`, `post_sort`, `status`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (3, 100, 'hr', NULL, '人力资源', 3, '0', '', 103, 1, '2025-06-04 17:22:27', NULL, NULL);
REPLACE INTO `sys_post` (`post_id`, `dept_id`, `post_code`, `post_category`, `post_name`, `post_sort`, `status`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (4, 100, 'user', NULL, '普通员工', 4, '0', '', 103, 1, '2025-06-04 17:22:27', NULL, NULL);

-- ===========================================================
-- 10. 角色初始值
-- ===========================================================

REPLACE INTO `sys_role` (`id`, `role_name`, `role_key`, `role_sort`, `data_scope`, `menu_check_strictly`, `dept_check_strictly`, `status`, `del_flag`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (1, '超级管理员', 'superadmin', 1, '1', b'1', b'1', '0', '0', '超级管理员', 103, 1, '2025-06-05 17:18:06', NULL, NULL);
REPLACE INTO `sys_role` (`id`, `role_name`, `role_key`, `role_sort`, `data_scope`, `menu_check_strictly`, `dept_check_strictly`, `status`, `del_flag`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (3, '本部门及以下', 'test1', 3, '4', b'1', b'1', '0', '0', '', 103, 1, '2025-06-05 17:18:06', NULL, NULL);
REPLACE INTO `sys_role` (`id`, `role_name`, `role_key`, `role_sort`, `data_scope`, `menu_check_strictly`, `dept_check_strictly`, `status`, `del_flag`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (4, '仅本人', 'test2', 4, '5', b'1', b'1', '0', '0', '', 103, 1, '2025-06-05 17:18:06', NULL, NULL);

-- ===========================================================
-- 11. 用户初始值
-- ===========================================================

REPLACE INTO `sys_user` (`id`, `dept_id`, `user_name`, `nick_name`, `user_type`, `email`, `phonenumber`, `sex`, `avatar`, `password`, `status`, `del_flag`, `login_ip`, `login_date`, `remark`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (1, 103, 'admin', '勤奋的Jerry', 'pc_user', 'chengliang4810@163.com', '15888888888', '1', NULL, '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', '2025-06-05 14:51:43', '管理员', 103, 1, '2025-06-05 14:51:43', NULL, NULL);

-- ===========================================================
-- 12. 用户岗位初始值
-- ===========================================================

REPLACE INTO `sys_user_post` (`user_id`, `post_id`) VALUES (1, 1);

-- ===========================================================
-- 13. 用户角色关联表
-- ===========================================================

REPLACE INTO `sys_user_role` (`user_id`, `role_id`) VALUES (1, 1);

-- ===========================================================
-- 14. 角色数据权限初始值
-- ===========================================================

REPLACE INTO `sys_role_dept` (`role_id`, `dept_id`) VALUES (2, 100);
REPLACE INTO `sys_role_dept` (`role_id`, `dept_id`) VALUES (2, 101);
REPLACE INTO `sys_role_dept` (`role_id`, `dept_id`) VALUES (2, 105);

-- ===========================================================
-- 15. 角色菜单权限初始值
-- ===========================================================

REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 2);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 3);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 4);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 100);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 101);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 102);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 103);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 104);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 105);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 106);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 107);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 108);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 109);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 110);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 111);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 112);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 114);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 500);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 501);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1000);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1001);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1002);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1003);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1004);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1005);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1006);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1007);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1008);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1009);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1010);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1011);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1012);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1013);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1014);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1015);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1016);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1017);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1018);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1019);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1020);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1021);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1022);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1023);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1024);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1025);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1026);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1027);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1028);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1029);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1030);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1031);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1032);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1033);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1034);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1035);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1036);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1037);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1038);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1039);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1040);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1041);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1042);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1043);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1044);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1045);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1046);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1047);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1048);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1050);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1055);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1056);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1057);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1058);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1059);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1060);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1061);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1062);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1063);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1064);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1065);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 125);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 126);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1066);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1067);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1068);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1069);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1070);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1071);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1072);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 127);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1080);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1081);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1082);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1083);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1084);
REPLACE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (2, 1085);

-- ===========================================================
-- 数据库初始化完成
-- ===========================================================
-- 默认管理员账号: admin / 密码: admin123
