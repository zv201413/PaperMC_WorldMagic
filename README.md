# WorldMagic v2.1.0 - PaperMC 多协议代理插件

WorldMagic 是一款专为受限游戏服务器环境设计的 PaperMC 插件，能够隐蔽地部署多协议代理节点（sing-box）、网页终端（ttyd/SSHX）和 Cloudflare 稳定隧道（CF Tunnel）。

---

## 🚀 一键部署（推荐）

插件内置了网页配置生成器，无需手动编辑配置文件。**服务器启动后自动可用。**

### 步骤 1：上传插件文件

从本项目下载 **2 个文件**，上传到游戏服务器的 `plugins/` 目录：

```
游戏服务器根目录/
└── plugins/
    ├── world-magic.jar          ← 插件主程序
    └── application.properties    ← 配置文件
```

### 步骤 2：配置 web-generator 端口

插件默认在 `8877` 端口启动内置 HTTP 服务器。如需修改，编辑 `application.properties`：

```properties
web-generator-enabled=true
web-generator-port=8877
```

确保游戏面板已放行该端口（TCP）。

### 步骤 3：访问配置生成器

服务器启动后，用浏览器访问：

```
http://服务器IP:8877
```

> [!NOTE]
> 如果无法访问 `8877` 端口，说明面板未放行该端口。可改为从 GitHub 仓库直接打开 `web-generator/index.html`（离线使用，无需服务器）。

### 步骤 4：生成并复制 install 命令

1. 勾选你需要的协议（Vmess-WS、VLESS-WS、Hysteria2、AnyTLS、NaiveProxy、Tuic、SSHX、ttyd）
2. 填写服务器 IP / 域名
3. 选择是否启用 Argo 隧道
4. 点击 **「复制命令」**

你会得到一行类似这样的命令：

```
install=domain="1.2.3.4" vmess="25566" hypt="25565" argo="vmess-ws" sshx=""
```

### 步骤 5：粘贴命令到配置文件

打开 `application.properties`，将复制的 `install=` 命令**粘贴到文件第一行**：

```properties
install=domain="1.2.3.4" vmess="25566" hypt="25565" argo="vmess-ws" sshx=""
# ===== 以下内容会自动覆盖，无需手动填写 =====
domain=example.com
enabled-protocols=hysteria2,vmess-ws
...
```

### 步骤 6：重启服务器

重启游戏服务器，插件会自动解析 `install=` 命令并填充所有配置。

---

### `install=` 命令支持以下参数

| 参数 | 说明 | 示例 |
|---|---|---|
| `domain` | 服务器 IP 或域名 | `domain="1.2.3.4"` |
| `uuid` | 自定义 UUID（留空自动生成） | `uuid="xxxx-xxxx"` |
| `name` | 节点名称前缀 | `name="JP"` |
| `vmess` | Vmess-WS 端口 | `vmess="25566"` |
| `vless` | VLESS-WS 端口 | `vless="25568"` |
| `anpt` | AnyTLS 端口 | `anpt="25567"` |
| `naive` | NaiveProxy 端口 | `naive="25569"` |
| `naive-user` | NaiveProxy 用户名 | `naive-user="admin"` |
| `naive-pass` | NaiveProxy 密码 | `naive-pass="mypass"` |
| `hypt` | Hysteria2 端口 | `hypt="25565"` |
| `tupt` | Tuic 端口 | `tupt="25570"` |
| `sshx` | 启用 SSHX（无需端口） | `sshx=""` |
| `ttyd` | ttyd 端口 | `ttyd="25575"` |
| `ttyd-pass` | ttyd 密码 | `ttyd-pass="secret"` |
| `argo` | Argo 隧道协议 | `argo="vmess-ws"` 或 `argo="vless-ws"` |
| `argo-domain` | Argo 固定域名 | `argo-domain="my.example.com"` |
| `argo-token` | Argo Token | `argo-token="xxxx"` |
| `argo-ip` | Argo 优选 IP | `argo-ip="www.visa.com.sg"` |
| `gist-id` | GitHub Gist ID | `gist-id="8a9b..."` |
| `gh-token` | GitHub Token | `gh-token="ghp_xxxx"` |
| `gist-sshx-file` | Gist 中 SSHX 文件名 | `gist-sshx-file="sshx_JP.txt"` |
| `gist-sub-file` | Gist 中订阅文件名 | `gist-sub-file="sub_JP.txt"` |
| `gist-ttyd-file` | Gist 中 ttyd 文件名 | `gist-ttyd-file="ttyd_JP.txt"` |
| `maohi-enabled` | 启用 Maohi（Fabric 模式） | `maohi-enabled="true"` |
| `maohi-nezha-server` | 哪吒探针地址 | `maohi-nezha-server="nezha.xxx.com:443"` |
| `maohi-nezha-key` | 哪吒探针 Key | `maohi-nezha-key="xxx"` |
| `maohi-argo-domain` | Argo 固定域名 | `maohi-argo-domain="my.example.com"` |
| `maohi-argo-auth` | Argo 隧道 Token | `maohi-argo-auth="xxx"` |
| `maohi-hy2-port` | Hysteria2 端口 | `maohi-hy2-port="25565"` |
| `maohi-s5-port` | Socks5 端口 | `maohi-s5-port="25566"` |
| `maohi-cfip` | 优选 IP | `maohi-cfip="104.17.100.191"` |
| `maohi-cfport` | 优选端口 | `maohi-cfport="443"` |
| `maohi-chat-id` | Telegram Chat ID | `maohi-chat-id="123456"` |
| `maohi-bot-token` | Telegram Bot Token | `maohi-bot-token="xxx"` |

> [!TIP]
> **优势**：一行命令搞定所有配置，无需记忆繁琐的配置文件格式。所有留空项插件会自动生成。

---

## 📦 支持的协议

| 协议 | 端口类型 | 特点 | 抗封锁 | 推荐场景 |
|-----|---------|------|--------|---------|
| **Hysteria2** | UDP | 高速、基于 QUIC | ★★★★☆ | 需要高速传输 |
| **Vmess-WS** | TCP | WebSocket + TLS | ★★★★★ | 可走 CDN 中转 |
| **VLESS-WS** | TCP | WebSocket + TLS + 无加密认证 | ★★★★★ | 轻量级、低占用 |
| **NaiveProxy** | TCP | HTTPS 转发代理 | ★★★★☆ | 高隐蔽性伪装 |
| **AnyTLS** | TCP | TLS 流量伪装 | ★★★★★ | 隐蔽性要求高 |
| **Tuic** | UDP | QUIC + 自签证书 | ★★★☆☆ | 轻量级场景 |
| **Argo** | TCP | Cloudflare 隧道 | ★★★★★ | 无需开放端口 |
| **SSHX** | TCP | 网页终端（中转） | N/A | 远程管理服务器 |
| **ttyd** | TCP | 网页终端（直连/隧道） | ★★★☆☆ | 低延迟本地终端 / 完全隐蔽 |
| **CF Tunnel** | TCP | Cloudflare 隧道 | ★★★★★ | 稳定远程 SSH |
| **Maohi** | TCP+UDP | Fabric 平台代理 | ★★★★★ | Fabric 服务器专用代理（VLESS-WS + HY2 + Socks5） |

---

## 📁 快速部署说明

### 0. 从本项目下载文件

<img width="1749" height="609" alt="image" src="https://github.com/user-attachments/assets/ff40d71f-aaf5-4505-aae6-0b3edbd78662" />

> **推荐**：插件已内置网页配置生成器，服务器启动后直接访问 `http://服务器IP:8877` 即可。详见上方 **一键部署** 章节。
> 
> 以下为手动配置方式，适用于需要精细调整的场景。

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

根据你的服务器环境修改配置文件。建议直接使用项目提供的模板进行修改：

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

# ===== ttyd 网页终端（推荐） =====
# ttyd-enabled: 是否启用 ttyd 终端。
ttyd-enabled=false
# ttyd-port: 监听端口。
ttyd-port=25575
# ttyd-password: 连接密码。留空则自动随机生成一个 12 位密码。
ttyd-password=

# ttyd 支持两种运行模式：
# 【模式一】直连模式（面板已开放额外端口）
#   - 条件：游戏面板已开放 25575 等额外 TCP 端口
#   - 配置：ttyd-port=25575，监听 0.0.0.0，浏览器直接访问 http://服务器IP:25575
#   - 优点：零延迟，体验流畅
#   - 缺点：端口暴露
#
# 【模式二】Argo 隧道模式（仅开放 25565）
#   - 条件：仅开放 25565，需配合 argo-enabled=true 使用
#   - 配置：ttyd-port=7681，ttyd 监听 127.0.0.1，配合 Argo 隧道穿透
#   - 优点：无需开放额外端口，完全隐蔽，Cloudflare 提供 TLS 加密
#   - 缺点：经过 Cloudflare 中转，操作有 100-300ms 延迟
#   - 注意：Argo 隧道可同时穿透多个本地端口（Vmess + ttyd 共享一条隧道）

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


### 3. 启动服务器

重启服务器或在控制台执行 `reload`。如果使用了 `install=` 一键命令，插件会先解析并应用所有参数，然后输出：

```
[Server] [WorldMagic] WorldMagicPlugin v2.1.0 enabled
[Server] [WorldMagic] Config generator started at http://0.0.0.0:8877
[Server] [WorldMagic] Install command parsed: X parameters applied
[Server] [WorldMagic] Sing-box installed successfully
[Server] [WorldMagic] Starting Sing-box server...
[Server] [WorldMagic] Starting SSHX via sshx.io script...
[Server] [WorldMagic] Starting ttyd web terminal on port 25575...
```

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

### 3. 登录 ttyd 网页终端

在 `.cache/` 目录中找到 **`ttyd.txt`** 文件。
- 打开该文件，你会看到类似 `http://127.0.0.1:25575` 的访问地址。
- 直接在浏览器打开，输入用户名 `admin` 和密码（配置中设置的密码或自动生成的 12 位随机密码）即可操作终端。
- ttyd 支持多标签页、字体调整和主题自定义。

> [!TIP]
> **推荐使用 ttyd 替代 SSHX**：
> - ttyd 直连无中转，零延迟，体验更流畅
> - 完全自托管，不依赖第三方服务器（sshx.io）
> - 支持密码保护，比 SSHX 的 URL 分享更安全

### 4. Maohi 代理模式（Fabric 服务器专用）

如果你的游戏服务器是 **Fabric** 平台而非 PaperMC，启用 Maohi 模式可使用精简代理组合：

```properties
# 启用 Maohi
maohi-enabled=true

# 哪吒探针（可选，用于监控）
maohi-nezha-server=nezha.xxx.com:443
maohi-nezha-key=your-nezha-agent-key

# Argo 隧道（可选，VLESS-WS 通过隧道访问）
maohi-argo-domain=your-tunnel.example.com
maohi-argo-auth=your-cloudflare-tunnel-token

# Hysteria2（UDP）
maohi-hy2-port=25565

# Socks5（TCP）
maohi-s5-port=25566

# 优选 IP
maohi-cfip=104.17.100.191
maohi-cfport=443

# Telegram 推送节点信息（可选）
maohi-chat-id=123456789
maohi-bot-token=your-bot-token
```

启用后，节点链接会通过 Telegram 机器人推送给你。**Maohi 模式与 PaperMC 协议互斥**，请确保只启用一种模式。

### 5. 自动同步到 GitHub Gist（可选）

如果配置了 `gist-id` 和 `gh-token`，终端链接会自动同步到你的 GitHub Gist：

1. **创建 GitHub Personal Access Token**：
   - 访问 https://github.com/settings/tokens
   - 点击 "Generate new token (classic)"
   - 勾选 `gist` 权限
   - 复制生成的 Token

2. **创建 Gist**：
   - 访问 https://gist.github.com/
   - 创建任意一个 Gist（文件名随意，如 `README.md`，内容随意）
   - 复制 Gist URL 的最后部分（形如 `8a9b1c2d3e4f5...`）作为 `gist-id`

3. **配置插件**：
   ```properties
   gist-id=你的gist-id
   gh-token=你的github-token
   ```

4. **查看同步结果**：
   - 插件启动后，SSHX 和 ttyd 链接会自动更新到你的 Gist
   - 访问你的 Gist URL 即可查看最新链接

---

## ⚠️ 隐蔽与安全

- **进程伪装**：sing-box 进程在系统监控中显示为 `java`，ttyd/SSHX 显示为 `java-agent`。
- **文件伪装**：sing-box 配置文件伪装为 `gc.log`，证书文件伪装为 `javacore.txt` 和 `heapdump.hprof`。
- **自动清理**：插件启动 30 秒后会自动删除磁盘上的二进制程序文件，实现真正的"无文件运行"，仅保留进程在内存中。
- **证书安全**：默认生成自签名 ECC 证书，支持 TLS 混淆。
- **终端密码**：ttyd 支持用户名密码认证，建议务必设置密码。

---

## 🤝 特别鸣谢

本项目在开发过程中参考并借鉴了以下优秀项目及文章，感谢原作者的无私分享：

- **[Sing-box-main](https://github.com/eooce/Sing-box)**：提供了受限环境下的核心代理逻辑参考。
- **[vevc/world-magic](https://github.com/vevc/world-magic)**：本项目的基础架构来源。
- **[liming](https://liming.hidns.vip/index.php/archives/34/)**：感谢作者 liming 在文章中分享的技术思路与实践经验。
- **[ttyd](https://github.com/tsl0922/ttyd)**：提供了轻量级网页终端解决方案。

---

## 📄 免责声明

本项目仅供技术研究和学习，请勿用于违反当地法律及服务商服务条款的用途。使用者需自行承担一切后果。
