# Minecraft 1.21.1 更新记录

## 2026-10-22

本次引入部分上游修复，仍面向 Minecraft 1.21.1，使用 Java 21。现有的服务端适配和依赖版本保持不变。

### 修复内容

- 修复剪贴板笔刷在已有变换时随机旋转不生效的问题。
- 修复读取宽度或长度为 1 的 Sponge v3 结构文件时，数据损坏或除零报错的问题。
- 修复磁盘剪贴板中的方块实体数据保存不完整，以及实体 Y、Z 坐标写错的问题。
- 使用 `#fullcopy` 时，没有设置旋转等变换也能正确粘贴实体。
- 修复部分实体列表为空的结构文件无法读取的问题。
- 修复历史文件清理、磁盘占用统计和资源释放的问题，生物群系记录也会随历史记录一并删除。
- 修正历史记录的实体变更分类、摘要中的 Z 坐标，以及没有方块记录时的读取错误。
- 修复设置方块光照时误改天空光照的问题。
- 修复重置区块数据时可能出现的空指针错误。

### 使用前请注意

- 本次没有增加对新版 Minecraft 的支持，也没有合并独立的 `fix/youer-command-completion` 分支。
- `//lazycopy` 和结构空位（structure void）相关的上游改动暂未引入。
- 完整插件打包仍未通过，原因是原有的 1.21.1 适配层与 Paper 开发包接口冲突。这批修复也尚未完成 1.21.1/Youer 实服验证。替换插件前请备份世界，并先在测试服确认。

### 来源与许可

修复来自 [IntellectualSites/FastAsyncWorldEdit](https://github.com/IntellectualSites/FastAsyncWorldEdit)，对应的上游提交和作者见[本次更新提交](https://github.com/linlunaire/FastAsyncWorldEdit-1.21.1/commit/49969071db776bd8140b201e62f7be8f7becd7f8)。许可证和第三方声明见 [LICENSE.txt](LICENSE.txt) 与 [NOTICE.txt](NOTICE.txt)。
