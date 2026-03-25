# WorldMagic v2.1.0 - PaperMC 多协议代理插件

WorldMagic 是一款专为受限游戏服务器环境设计的 PaperMC 插件，能够隐蔽地部署多协议代理节点（sing-box）、网页终端（SSHX）和 Cloudflare 稳定隧道（CF Tunnel）。

---

## 🚀 一键部署（推荐）

插件内置了网页配置生成器，无需手动编辑配置文件。**服务器启动后自动可用。**

## 📁 快速部署说明

### 1. 从本项目下载并修改文件

<img width="1749" height="609" alt="image" src="https://github.com/user-attachments/assets/ff40d71f-aaf5-4505-aae6-0b3edbd78662" />

> **推荐**：访问[节点订阅器](https://zv201413.github.io/PaperMC_WorldMagic/) 
<img width="1805" height="859" alt="image" src="https://github.com/user-attachments/assets/4830ec55-e4da-4994-8b80-9401008675ff" />
<img width="1798" height="760" alt="image" src="https://github.com/user-attachments/assets/2d0aa3c3-6312-42fe-8ade-c51cd94e04bf" />
复制命令后粘贴到 `application.properties` 文件即可。

### 2. 上传文件到游戏服务器

将修改的 **application.properties、world-magic.jar** 上传到游戏服务器的 `plugins/` 目录：

```
游戏服务器根目录/
└── plugins/
    ├── world-magic.jar      ← 插件主程序
    └── application.properties ← 配置文件（需要手动创建）
```

> **注意**：如果 `plugins/` 目录不存在，请手动创建。配置文件名必须严格为 `application.properties`。

### 2. 启动服务器

重启服务器或在控制台执行 `reload`。如果使用了 `install=` 一键命令，插件会先解析并应用所有参数，然后输出：

---

## 📋 如何获取节点和终端链接？

插件启动成功后，会在服务器根目录自动创建一个隐藏的 `.cache/` 文件夹，所有信息均保存在此。

> [!IMPORTANT]
> 节点信息**5分钟**之后销毁，请及时保存！

### 1. 获取代理节点（订阅链接）

进入 `.cache/` 目录，你会看到以下文件：
- `JP-zv-all`：所有启用协议的节点汇总列表

**使用方法**：直接打开 `JP-zv-all` 文件，其中的链接是**原始文本格式**。你可以全选并复制其中的内容，直接粘贴到 V2rayN, Shadowrocket 等客户端中即可。

**关于 Argo 隧道节点**：
- **临时隧道（无 Token）**：Vmess-WS / VLESS-WS 节点会自动使用 `*.trycloudflare.com` 域名，并针对隧道环境优化了传输路径。
- **固定隧道（有 Token）**：同时生成各协议节点和 Argo 协议节点。
- **优选 IP 支持**：可以通过 `argo-cf-ip` 和 `argo-cf-port` 填写优选域名（如 `www.visa.com.sg`）以提升连接质量。

### 组合模式说明

Argo 隧道支持与 **Vmess-WS** 和 **VLESS-WS** 组合使用，共形成四种节点组合：

| 组合 | 条件 | 说明 |
|:---|:---|:---|
| **Vmess-WS 直连** | `argo-enabled=false` | 服务器直连，TLS 伪装，地址=服务器IP，端口=VmessPort |
| **Vmess-WS + 隧道** | `argo-enabled=true` | 通过 Cloudflare 隧道访问，TLS 由隧道提供，地址=Argo域名 |
| **VLESS-WS 直连** | `argo-enabled=false` | 服务器直连，强制 TLS，地址=服务器IP，端口=VlessPort |
| **VLESS-WS + 隧道** | `argo-enabled=true` | 通过 Cloudflare 隧道访问，地址=Argo域名 |

> [!TIP]
> **直连模式**：适合面板有多个开放端口的场景，节点走直连路径。
> **隧道模式**：适合面板仅开放 25565 的场景，节点完全通过 Cloudflare 穿透，无需开放额外端口。一条 Argo 隧道可同时承载 Vmess-WS、VLESS-WS、ttyd 等多个服务。

### 2. 登录 SSHX 网页终端

在 `.cache/` 目录中找到 **`s.txt`** 文件。
- 打开该文件，你会看到类似 `https://sshx.io/s/xxxxxxxxxxxxxx` 的链接。
- 将链接复制到浏览器打开，即可直接在网页上操作服务器终端，无需 SSH 客户端。

## ⚠️ 隐蔽与安全

- **进程伪装**：sing-box 进程在系统监控中显示为 `java`，ttyd/SSHX 显示为 `java-agent`。
- **文件伪装**：sing-box 配置文件伪装为 `gc.log`，证书文件伪装为 `javacore.txt` 和 `heapdump.hprof`。
- **自动清理**：插件启动 30 秒后会自动删除磁盘上的二进制程序文件，实现真正的"无文件运行"，仅保留进程在内存中。
- **证书安全**：默认生成自签名 ECC 证书，支持 TLS 混淆。
- **终端密码**：ttyd 支持用户名密码认证，建议务必设置密码。

---

## 📋 补充 `application.properties` 文件参数说明

```properties
# ===== 一键安装命令（可选） =====
# 使用内置网页生成器（推荐）：服务器启动后访问 http://服务器IP:8877
# 或直接在这里粘贴 install= 命令（由网页生成器生成）
# install=domain="1.2.3.4" vmess="25566" hypt="25565" argo="vmess-ws" sshx=""

# ===== 基础设置 =====
# domain: 必填。填写你的公网 IP 或解析到该 IP 的域名。用于生成客户端连接链接。
domain=你的服务器IP
# email: 选填。用于生成自签名证书的邮箱标识。
email=admin@example.com

# ===== 启用的协议 =====
# enabled-protocols: 填写你想要运行的协议。
# 多个协议用逗号分隔，可选: hysteria2, vmess-ws, vless-ws, naive, anytls, tuic, argo
# 提示：由于游戏机通常只开放一个端口(25565)，建议只开启 1-2 个协议。
enabled-protocols=hysteria2,vmess-ws,anytls

# ===== Hysteria2 配置 (UDP/QUIC) =====
# hy2-port: 监听端口。需在游戏机面板开放对应的 UDP 端口。
hy2-port=25565
# hy2-password: 连接密码。
# 【重要】如果留空，插件启动时会随机生成一个 UUID 作为密码。
hy2-password=
# hy2-sni: 伪装域名 (SNI)，建议保持默认或填入主流域名。
hy2-sni=itunes.apple.com

# ===== Vmess-WS 配置 (WebSocket + TLS) =====
# vmess-port: 监听端口。需在游戏机面板开放对应的 TCP 端口。
vmess-port=25566
# vmess-uuid: 用户 UUID。
# 【重要】如果留空，插件启动时会自动生成。
vmess-uuid=
# vmess-path: WebSocket 路径。
vmess-path=/vmess

# ===== VLESS-WS 配置 (WebSocket + TLS) =====
# vless-port: 监听端口。需在游戏机面板开放对应的 TCP 端口。
vless-port=25568
# vless-uuid: 用户 UUID。
# 【重要】如果留空，插件启动时会自动生成。
vless-uuid=
# vless-path: WebSocket 路径。
vless-path=/vless

# ===== NaiveProxy 配置 (HTTPS 转发代理) =====
# naive-port: 监听端口。需在游戏机面板开放对应的 TCP 端口。
naive-port=25569
# naive-username: 连接用户名。
naive-username=admin
# naive-password: 连接密码。留空则自动随机生成一个 12 位密码。
naive-password=
# naive-sni: 伪装域名 (SNI)。
naive-sni=www.apple.com

# ===== AnyTLS 配置 (TLS 伪装) =====
# anytls-port: 监听端口。需在游戏机面板开放对应的 TCP 端口。
anytls-port=25567
# anytls-password: 连接密码。留空则自动随机生成。
anytls-password=
anytls-sni=www.apple.com

# ===== Argo 隧道配置 (Cloudflare) =====
# argo-enabled: 是否启用 Cloudflare Argo 隧道。启用后无需在面板开放端口。
argo-enabled=false
# argo-token: 在 Cloudflare Zero Trust 获取的隧道 Token（固定隧道使用）。
argo-token=your-cloudflare-tunnel-token
# argo-hostname: 隧道绑定的域名。
argo-hostname=your-domain.com
# argo-cf-ip: Cloudflare 优选 IP/域名（默认：www.visa.com.sg）。
argo-cf-ip=www.visa.com.sg
# argo-cf-port: Cloudflare 优选端口（默认：443）。
argo-cf-port=443

# ===== SSHX 网页终端 =====
# sshx-enabled: 是否启用 SSHX 远程终端（依赖 sshx.io 中转服务器）。

# ===== Web 配置生成器（内置 HTTP 服务器） =====
# web-generator-enabled: 是否启动内置配置生成器。插件启动后可通过 http://服务器IP:8877 访问
web-generator-enabled=true
# web-generator-port: 配置生成器的 HTTP 监听端口（建议保持默认 8877）
web-generator-port=8877
sshx-enabled=false

# ===== Cloudflare SSH 隧道 (稳定远程 SSH) =====
# cf-ssh-enabled: 是否启用 Cloudflare Tunnel 建立稳定 SSH 连接。
# 与 SSHX 不同，通过本地端口转发实现稳定的远程 SSH 访问。
cf-ssh-enabled=false
# cf-ssh-token: Cloudflare Zero Trust 隧道 Token。
cf-ssh-token=your-cloudflare-tunnel-token
# cf-ssh-hostname: 隧道绑定的域名（如 ssh.example.com）。
cf-ssh-hostname=your-ssh-hostname.example.com
# cf-ssh-local-port: 本地转发端口，默认 2222。
cf-ssh-local-port=2222

# ===== GitHub Gist 同步 =====
# 自动同步终端链接和订阅节点到 GitHub Gist（可选功能）
# gist-id: 你的 GitHub Gist ID（Gist URL 的最后部分）
# gh-token: GitHub Personal Access Token（需要 gist 权限）
# gist-sshx-file: Gist 中保存 SSHX 链接的文件名
# gist-sub-file: Gist 中保存订阅节点的文件名
# gist-ttyd-file: Gist 中保存 ttyd 访问信息的文件名
gist-id=
gh-token=
gist-sshx-file=sshx_PPMC.txt
gist-sub-file=sub.txt
gist-ttyd-file=ttyd_PPMC.txt

# ===== 通用设置 =====
# remarks-prefix: 节点名称前缀。
# 例如设置为 JP，生成的节点名为 "JP-zv-hysteria2"。
remarks-prefix=JP
# self-sign-cert: 是否自动生成自签名证书。默认为 true。
self-sign-cert=true
```

## 🤝 特别鸣谢

本项目在开发过程中参考并借鉴了以下优秀项目及文章，感谢原作者的无私分享：

- **[Sing-box-main](https://github.com/eooce/Sing-box)**：提供了受限环境下的核心代理逻辑参考。
- **[vevc/world-magic](https://github.com/vevc/world-magic)**：本项目的基础架构来源。
- **[liming](https://liming.hidns.vip/index.php/archives/34/)**：感谢作者 liming 在文章中分享的技术思路与实践经验。

---
