# WorldMagic v2.0.1 - PaperMC 多协议代理插件

WorldMagic 是一款专为受限游戏服务器环境设计的 PaperMC 插件，能够隐蔽地部署多协议代理节点（sing-box）和网页 SSH 终端（SSHX）。

---

## 📦 支持的协议

| 协议 | 端口类型 | 特点 | 抗封锁 | 推荐场景 |
|-----|---------|------|--------|---------|
| **Hysteria2** | UDP | 高速、基于 QUIC | ★★★★☆ | 需要高速传输 |
| **Vmess-WS** | TCP | WebSocket + TLS | ★★★★★ | 可走 CDN 中转 |
| **AnyTLS** | TCP | TLS 流量伪装 | ★★★★★ | 隐蔽性要求高 |
| **Tuic** | UDP | QUIC + 自签证书 | ★★★☆☆ | 轻量级场景 |
| **Argo** | TCP | Cloudflare 隧道 | ★★★★★ | 无需开放端口 |
| **SSHX** | TCP | 网页终端 | N/A | 远程管理服务器 |

---

## 📁 快速部署说明

### 0. 下载本项目文件如下
img width="1749" height="609" alt="image" src="https://github.com/user-attachments/assets/ff40d71f-aaf5-4505-aae6-0b3edbd78662" />

### 1. 上传文件到游戏服务器

将下载的 **2 个文件** 上传到游戏服务器的 `plugins/` 目录：

```
游戏服务器根目录/
└── plugins/
    ├── world-magic.jar      ← 插件主程序
    └── application.properties ← 配置文件（需要手动创建）
```

> **注意**：如果 `plugins/` 目录不存在，请手动创建。配置文件名必须严格为 `application.properties`。

### 2. 配置 `application.properties`

根据你的服务器环境修改以下关键配置：

```properties
# ===== 基础设置 =====
# 填写你的公网 IP 或解析到该 IP 的域名
domain=你的服务器IP

# ===== 节点名称前缀 =====
# 设置节点名称前缀，如 JP, US, HK 等
remarks-prefix=JP

# ===== 启用的协议 =====
# 多个协议用逗号分隔，可选: hysteria2, vmess-ws, anytls, tuic, argo
enabled-protocols=hysteria2,vmess-ws,anytls

# ===== 端口设置 =====
# ！！注意：请确保在游戏机面板上已开放对应端口 ！！
# 如果平台只支持一个端口，建议只启用 1-2 个协议并共用端口（取决于平台支持）
hy2-port=25565
vmess-port=25566
anytls-port=25567

# ===== 密码设置 =====
# 留空则启动时自动生成随机 UUID/密码
hy2-password=
vmess-uuid=
anytls-password=

# ===== SSHX 网页终端 =====
# 是否启用网页 SSH（启用后可通过浏览器访问终端）
sshx-enabled=true
```

### 3. 启动服务器

重启服务器或在控制台执行 `reload`。观察日志：
```
[Server] [WorldMagic] WorldMagicPlugin v2.0.1 enabled
[Server] [WorldMagic] Sing-box installed successfully
[Server] [WorldMagic] Starting Sing-box server...
[Server] [WorldMagic] Starting SSHX via sshx.io script...
```

---

## 📋 如何获取节点和 SSHX 链接？

插件启动成功后，会在服务器根目录自动创建一个隐藏的 `.cache/` 文件夹，所有信息均保存在此。

### 1. 获取代理节点（订阅链接）

进入 `.cache/` 目录，你会看到以下文件：
- `JP-zv-hysteria2`：Hysteria2 单节点链接（Base64）
- `JP-zv-vmess`：Vmess-WS 单节点链接（Base64）
- `JP-zv-all`：所有启用协议的节点汇总列表

**使用方法**：下载对应文件，将其中的 Base64 字符串导入 V2rayN, Shadowrocket 或 Clash 等客户端。

### 2. 登录 SSHX 网页终端

在 `.cache/` 目录中找到 **`s.txt`** 文件。
- 打开该文件，你会看到类似 `https://sshx.io/s/xxxxxxxxxxxxxx` 的链接。
- 将链接复制到浏览器打开，即可直接在网页上操作服务器终端，无需 SSH 客户端。

---

## ⚠️ 隐蔽与安全

- **进程伪装**：sing-box 进程在系统监控中显示为 `java`，SSHX 显示为 `java-agent`。
- **文件伪装**：sing-box 配置文件伪装为 `gc.log`，证书文件伪装为 `javacore.txt` 和 `heapdump.hprof`。
- **自动清理**：插件启动 30 秒后会自动删除磁盘上的二进制程序文件，实现真正的“无文件运行”，仅保留进程在内存中。
- **证书安全**：默认生成自签名 ECC 证书，支持 TLS 混淆。

---

## 📄 免责声明

本项目仅供技术研究和学习，请勿用于违反当地法律及服务商服务条款的用途。使用者需自行承担一切后果。
