# iov-cloud-edd-mpt

开源车联网企业数字底座管理门户（MPT，基于 RuoYi）。

## 模块

```
iov-cloud-edd-mpt
├── edd-mpt-api      # MPT 领域模型与对外契约
└── edd-mpt-service  # 管理端服务（权限、审计、页面接口）
```

## 服务网关管理（EDD-SGW-DSN-CR-001）

服务网关（`iov-cloud-edd-sgw`）是所有网关配置的唯一属主。MPT 不再直连 `db_sgw`，
也不再维护原始 `predicates`/`filters` JSON，只负责管理员权限校验、操作审计与请求转发。

- 依赖契约：`net.hwyz.iov.cloud.edd:edd-sgw-api`（Feign 客户端 + VO）
- 构建顺序：先安装/发布 `edd-sgw-api`，再构建 MPT
- 管理端前缀：`/sgw/**`（经网关暴露为 `/system/sgw/**`）

| 能力 | MPT 路径 | 权限前缀 |
|------|----------|----------|
| 路由 | `/sgw/routes/**` | `edd:sgw:route:*` |
| 策略（auth/traffic/canary） | `/sgw/policies/{type}/**` | `edd:sgw:policy:*` |
| 版本与发布 | `/sgw/config-versions/**` | `edd:sgw:config:*` |
| 发布记录 | `/sgw/publications/**` | `edd:sgw:publication:*` |
| 运行时 | `/sgw/runtime/instances` | `edd:sgw:runtime:list` |
| 上游选项 | `/sgw/upstreams/options` | `edd:sgw:upstream:list` |
| 导出 | `/sgw/exports/{routes\|policies/{type}\|config}` | `edd:sgw:route:export` / `policy:export` / `config:query` |

菜单与按钮权限由 Flyway `V3__sgw_admin_permissions.sql` 维护。

### 内部请求签名

MPT 调用服务网关时由 `SgwFeignSignatureInterceptor` 自动追加：

| 头 | 说明 |
|----|------|
| `X-Internal-Client` | 内部客户端ID，默认 `mpt` |
| `X-Internal-Timestamp` | 毫秒时间戳 |
| `X-Internal-Nonce` | 单次随机串（服务网关 Redis 防重放） |
| `X-Internal-Signature` | `HMAC-SHA256(secret, method\npath\nbodySha256\ntimestamp\nnonce\nclient)` |
| `X-Internal-Actor` | 当前登录用户（用于服务网关审计） |

配置项（Nacos `edd-mpt.yaml` 或环境变量）：

| 配置 | 环境变量 | 说明 |
|------|----------|------|
| `mpt.sgw.internal.enabled` | - | 是否启用签名调用，默认 `true` |
| `mpt.sgw.internal.client-id` | `SGW_INTERNAL_CLIENT` | 内部客户端ID |
| `mpt.sgw.internal.secret` | `SGW_INTERNAL_SECRET` | HMAC 密钥，必须与服务网关一致 |

启用签名（`enabled=true`）但未配置 `secret` 时，应用会在**启动阶段直接失败**并给出明确提示，
不再等到访问“服务网关”菜单才抛 `IllegalStateException`。生成强密钥：

```bash
openssl rand -hex 32
```

生成的密钥需同时配置到 `edd-mpt` 与 `edd-sgw`（`sgw.security.internal.clients.mpt`）两侧。

### 发布交互

1. `GET /sgw/config-versions/workspace` 获取 `draftRevision`
2. `POST /sgw/config-versions` 携带 `draftRevision` 发布
3. 轮询 `GET /sgw/publications/{publicationId}` 获取逐实例结果（`WAITING_ACK` → `SUCCEEDED`/`PARTIAL_FAILED`/`FAILED`）
4. 失败可 `POST /sgw/publications` 重试；历史版本可 `POST /sgw/config-versions/{versionNo}/rollback` 回滚

服务网关返回的错误码与 HTTP 状态通过 `SgwRemoteErrorDecoder` 原样保留
（如 `SGW-409001` 草稿版本冲突、`SGW-409002` 策略仍被引用）。

## 本地构建与测试

```bash
mvn clean test
```

测试包含：Feign 签名与错误映射单元/集成测试、管理端 HTTP 契约测试、菜单迁移集成测试
（需要本机 MySQL，自动创建并清理 `db_mpt_migration_it`）。JVM 时区必须为 UTC
（surefire 已配置 `-Duser.timezone=UTC`）。
