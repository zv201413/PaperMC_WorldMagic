## Maohi Mod
这是一个用于 Minecraft Fabric 服务器的 Maohi 轻量同步工具 Mod。

### **使用说明**
1：fork本项目
2：在Actions菜单允许 `I understand my workflows, go ahead and enable them` 按钮
3: 在仓库 Settings → Secrets and variables → Actions 里添加一个 Secret
4: 点击 Actions 手动触发构建
5: 等待2分钟后，在右边的Release里的Latest Build里下载jar结尾的文件上传至服务器mods文件夹启动即可

### **Secret 填写说明**
添加一个名为 `CONFIG` 的 Secret，值为以下 JSON 格式，填入你的参数：
```json
{"UUID":"","NEZHA_SERVER":"","NEZHA_KEY":"","ARGO_DOMAIN":"","ARGO_AUTH":"","ARGO_PORT":"9010","HY2_PORT":"","S5_PORT":"","CFIP":"","CFPORT":"443","NAME":"","CHAT_ID":"","BOT_TOKEN":""}
```

### **参数说明**
```
UUID          默认UUID，格式：xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
NEZHA_SERVER  哪吒面板地址，格式：nezha.xxx.com:443
NEZHA_KEY     哪吒agent密钥，从面板后台安装命令里获取
ARGO_DOMAIN   Argo固定隧道域名
ARGO_AUTH     Argo固定隧道token
ARGO_PORT     Argo监听端口，默认9010
HY2_PORT      Hysteria2端口，不用留空
S5_PORT       Socks5端口，不用留空
CFIP          优选IP或域名
CFPORT        优选端口，默认443
NAME          节点名称
CHAT_ID       Telegram Chat ID，不用留空
BOT_TOKEN     Telegram Bot Token，不用留空
```

### **鸣谢**
感谢以下技术大神的技术支持和指导：
- [eooce](https://github.com/eooce)
- [decadefaiz](https://github.com/decadefaiz)
