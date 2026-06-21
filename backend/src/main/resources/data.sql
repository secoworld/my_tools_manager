-- 命令模块初始化数据
INSERT INTO command_module (id, name, description, icon, sort_order, created_at) VALUES (1, 'Git常用命令', 'Git 版本控制常用命令集合', 'git', 1, CURRENT_TIMESTAMP);
INSERT INTO command_module (id, name, description, icon, sort_order, created_at) VALUES (2, 'Docker常用命令', 'Docker 容器管理常用命令集合', 'docker', 2, CURRENT_TIMESTAMP);
INSERT INTO command_module (id, name, description, icon, sort_order, created_at) VALUES (3, 'Linux常用命令', 'Linux 系统管理常用命令集合', 'linux', 3, CURRENT_TIMESTAMP);

-- Git 常用命令
INSERT INTO command (module_id, name, command, description, category, sort_order, created_at) VALUES (1, '克隆仓库', 'git clone <repository-url>', '克隆远程仓库到本地', 'Git', 1, CURRENT_TIMESTAMP);
INSERT INTO command (module_id, name, command, description, category, sort_order, created_at) VALUES (1, '查看状态', 'git status', '查看工作区状态', 'Git', 2, CURRENT_TIMESTAMP);
INSERT INTO command (module_id, name, command, description, category, sort_order, created_at) VALUES (1, '提交更改', 'git commit -m "commit message"', '提交暂存区的更改', 'Git', 3, CURRENT_TIMESTAMP);
INSERT INTO command (module_id, name, command, description, category, sort_order, created_at) VALUES (1, '推送远程', 'git push origin <branch>', '推送本地提交到远程分支', 'Git', 4, CURRENT_TIMESTAMP);

-- Docker 常用命令
INSERT INTO command (module_id, name, command, description, category, sort_order, created_at) VALUES (2, '列出容器', 'docker ps', '列出正在运行的容器', 'Docker', 1, CURRENT_TIMESTAMP);
INSERT INTO command (module_id, name, command, description, category, sort_order, created_at) VALUES (2, '拉取镜像', 'docker pull <image>:<tag>', '从仓库拉取镜像', 'Docker', 2, CURRENT_TIMESTAMP);
INSERT INTO command (module_id, name, command, description, category, sort_order, created_at) VALUES (2, '运行容器', 'docker run -d --name <name> <image>', '后台运行一个容器', 'Docker', 3, CURRENT_TIMESTAMP);
INSERT INTO command (module_id, name, command, description, category, sort_order, created_at) VALUES (2, '停止容器', 'docker stop <container-id>', '停止正在运行的容器', 'Docker', 4, CURRENT_TIMESTAMP);

-- Linux 常用命令
INSERT INTO command (module_id, name, command, description, category, sort_order, created_at) VALUES (3, '查看目录', 'ls -al', '列出目录所有文件含隐藏文件', 'Linux', 1, CURRENT_TIMESTAMP);
INSERT INTO command (module_id, name, command, description, category, sort_order, created_at) VALUES (3, '查看进程', 'ps -ef', '查看系统所有进程', 'Linux', 2, CURRENT_TIMESTAMP);
INSERT INTO command (module_id, name, command, description, category, sort_order, created_at) VALUES (3, '查看磁盘', 'df -h', '查看磁盘使用情况', 'Linux', 3, CURRENT_TIMESTAMP);
INSERT INTO command (module_id, name, command, description, category, sort_order, created_at) VALUES (3, '查找文件', 'find /path -name "filename"', '在指定路径查找文件', 'Linux', 4, CURRENT_TIMESTAMP);
