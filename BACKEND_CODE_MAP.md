# DataEase V3 后端代码定位与改动核对表

本文依据当前工作区源码整理，用于开始改功能前定位入口和检查影响范围。范围是 `core/core-backend`、`sdk/common`、`sdk/api`、`sdk/extensions`；本地 `de-xpack` 目录为空，不能据此判断 XPack 实现。以下均为静态代码结论，运行时配置还可能被环境变量、外置配置或启动参数覆盖。

## 先认识模块和调用链

| 位置 | 职责 |
| --- | --- |
| `core/core-backend` | Spring Boot 应用、接口实现、业务编排、元数据持久化、分析查询、任务调度 |
| `sdk/common` | 公共实体、认证注解、统一响应、异常、缓存、国际化和工具 |
| `sdk/api/api-base` | 核心业务的 `*Api` 接口及请求、响应模型 |
| `sdk/api/api-permissions` | 用户、组织、登录、角色、资源和数据集权限契约 |
| `sdk/api/api-sync` | 同步 API 契约 |
| `sdk/extensions` | 数据源、图表、填报、同步的插件接口与共享模型 |

常见链路：`前端 src/api → SDK *Api → 后端 server → manage → Repository（元数据）或 Provider（分析数据源）`。入口类是 `core/core-backend/src/main/java/io/dataease/CoreApplication.java`。不要仅凭 `controller`、`service` 包名查找，本项目的主要实现分别在 `server` 和 `manage`。

## 八个定位问题

### 1. 项目端口、数据库配在哪？

- `core/core-backend/src/main/resources/application.yml`：`server.port` 当前为 **8100**；`spring.profiles.active` 由 Maven 的 `@profiles.active@` 注入。还包含 Redis、缓存、Quartz 等通用配置。
- 同目录的 `application-standalone.yml`、`application-desktop.yml`、`application-distributed.yml`：分别设置对应运行模式的 `spring.datasource`，包括驱动、JDBC 地址和连接参数；部分模式还有其他数据源配置。`standalone`、`desktop` 的 JPA 配置含 `ddl-auto: update`。
- `core/core-backend/pom.xml`：查看 Profile 的启用方式；`standalone` 当前默认启用。`core/core-frontend/vite.config.ts` 及前端配置决定开发代理，不能当作后端监听端口。

配置文件含连接凭据。排查时只记录**配置项路径及用途**，不要把密码、Token、真实连接串复制到文档、日志或提交说明。部署时再核对外置配置及环境覆盖。

### 2. 所有接口的入口在哪？

在 `core/core-backend/src/main/java/io/dataease` 搜 `@RestController`、`@Controller`、`@RequestMapping`。接口实现主要在各业务目录的 `server/`，另有 `visualization/server/` 中以 `*Service` 命名的控制器、`template/service/`、`community/`、`home/`、`resource/`、`ai/service/` 和 `substitute/permissions/`。**不存在一个覆盖全部接口的 `controller` 包。**

接口路径和方法注解还可能定义在 `sdk/api/api-base`、`sdk/api/api-permissions`、`sdk/api/api-sync` 的 `*Api` 接口上；要把接口与实现合起来读。例如 `sdk/api/api-base/.../dataset/DatasetDataApi.java` 定义 `@PostMapping("previewSql")`，`core/core-backend/.../dataset/server/DatasetDataServer.java` 定义类级 `@RequestMapping("datasetData")`，合起来是 `/datasetData/previewSql`。新增或修改接口时检查 SDK、实现、前端 `src/api/`、权限注解以及扩展实现。

可复查：

```sh
rg -n --no-ignore '@RestController|@Controller|@RequestMapping' core/core-backend/src/main/java sdk/api -g '*.java' -g '!**/target/**'
```

### 3. 业务逻辑主要在哪个包？

主要在各业务域的 `manage/`，也有部分 `service/`、图表 `charts/impl/`、查询 `engine/` 和数据源 `provider/`。典型文件：

| 业务 | 主要实现 |
| --- | --- |
| 数据源 | `datasource/manage/DataSourceManage.java`、`EngineManage.java`；`datasource/provider/*Provider.java` |
| 数据集和自定义 SQL | `dataset/manage/DatasetGroupManage.java`、`DatasetDataManage.java`、`DatasetSQLManage.java`、`DatasetTableFieldManage.java` |
| 图表 | `chart/manage/ChartDataManage.java`、`ChartViewManege.java`；`chart/charts/impl/*Handler.java` |
| 仪表板/大屏 | `visualization/manage/CoreVisualizationManage.java`、`VisualizationLinkageManage.java`、`VisualizationOuterParamsManage.java` 等 |
| 导出与分享 | `exportCenter/manage/`、`share/manage/` |
| 模板与系统 | `template/manage/`、`template/service/`、`system/manage/` |

新增规则应放在拥有该规则的现有业务层，或在需要独立职责时新增相邻类；`server` 负责 HTTP 边界和编排，避免把业务规则堆进控制器。

### 4. 数据库表对应哪些实体？

元数据主要使用 Spring Data JPA / QueryDSL。共享实体在 `sdk/common/src/main/java/io/dataease/dao/auto/entity/`，业务实体散布在 `core/core-backend/src/main/java/io/dataease/*/dao/auto/entity/`，另有少量 `dao/entity/`。用 `@Entity`、`@Table` 精确定位，再找同域 `*Repository`。例如 `CoreDatasetGroup` 在 SDK common，`CoreDatasetGroupRepository` 在后端 `dataset/dao/auto/mapper/`；图表实体 `CoreChartView` 在 SDK common，Repository 在后端 `chart/dao/auto/mapper/`。目录名 `mapper` 不代表这里一定是 MyBatis。

`dao/ext/po/`、`dto/`、`vo/` 常是查询投影或传输模型，不一定对应一张表。分析数据来自外部数据源，经 `datasource/provider/` 和 `engine/` 查询；不要用元数据库 Repository 替代。

```sh
rg -n --no-ignore '@Entity|@Table\(' core/core-backend/src/main/java sdk/common/src/main/java -g '*.java' -g '!**/target/**'
rg --files --no-ignore core/core-backend/src/main/java sdk/common/src/main/java -g '*Repository.java' -g '!**/target/**'
```

### 5. 有没有统一返回格式？

有。`sdk/common/src/main/java/io/dataease/result/ResultMessage.java` 定义 `code`、`msg`、`data`；`ResultCode.java` 定义状态码；`ResultResponseBodyAdvice.java` 对适用的 JSON/String 响应自动包装普通返回值，已是 `ResultMessage` 的响应不重复包装。分页另看 `PageResult.java`。新增接口沿用这套格式，先确认返回类型与 `ResponseBodyAdvice` 的处理范围。

### 6. 有没有全局异常处理？

有。`sdk/common/src/main/java/io/dataease/exception/GlobalExceptionHandler.java` 标注 `@RestControllerAdvice`，处理参数校验、`DEException` 及特定空指针异常；`DEException.java` 是业务异常类型。它**没有显式覆盖所有异常**。新增业务失败优先沿用 `DEException`、`ResultCode` 和国际化机制，不把异常吞掉后返回成功或空数据。

### 7. 第三方 API 调用在哪？

单搜 `RestTemplate`、`@FeignClient`、`WebClient` 会漏掉实际调用。当前代码的主要入口：

- `sdk/common/.../utils/HttpClientUtil.java`：共享 HTTP 客户端工具。
- `core/core-backend/.../datasource/provider/ApiUtils.java`：API 数据源请求；`EsProvider.java`：Elasticsearch HTTP 请求；`StarRocksStreamLoadUtil.java`：StarRocks 导入；`ExcelUtils.java`：远程文件获取。
- `core/core-backend/.../template/manage/TemplateCenterManage.java`：模板资源请求。
- `core/core-backend/.../defeign/permissions/`：使用项目自定义 `@DeFeign` 声明权限、用户服务调用；注解定义在 `sdk/common/.../feign/DeFeign.java`。`de-xpack` 实现当前不可见。

可从 `HttpClientUtil` 的调用方、`@DeFeign`、`java.net`、Apache HttpClient 等继续反查；区分外部 API、内部服务调用和远程数据源访问。

### 8. 定时任务在哪？

`CoreApplication` 启用 `@EnableScheduling`。`@Scheduled` 出现在 `job/schedule/CleanScheduler.java`、`exportCenter/manage/ExportCenterDownLoadManage.java`、`ExportCenterManage.java`。**任务不止这些**：`job/schedule/` 还包含 Quartz `Job`、任务执行器和 `ScheduleManager`；`qrtz/dao/` 是 Quartz 表实体，配置见 `application.yml` 的 `quartz` 节。新增任务先确认使用 Spring 定时还是现有 Quartz 调度生命周期。

```sh
rg -n --no-ignore '@Scheduled|implements Job|extends DeScheduleJob|@EnableScheduling' core/core-backend/src/main/java -g '*.java' -g '!**/target/**'
```

## 具体改动怎么定位

### 修改“自定义 SQL 配置界面”

1. **只改编辑、展示、交互**：先看 `core/core-frontend/src/views/visualized/data/dataset/form/AddSql.vue`，以及承接 SQL 节点保存的 `DatasetUnion.vue` 和类型 `form/util.ts`。UI 文案需要同步 i18n。
2. **改请求参数或接口**：看前端 `core/core-frontend/src/api/dataset.ts` 的 `getPreviewSql`、`getTableField`、`saveDatasetTree`，以及 SDK 的 `DatasetDataApi.java`、`PreviewSqlDTO.java`、`DatasetTreeApi.java`。接口路径、权限和序列化变更才需要同步改对应 `server`；纯 UI 改动通常不需要改控制器。
3. **改 SQL 校验、预览、变量规则**：看 `dataset/server/DatasetDataServer.java` → `dataset/manage/DatasetDataManage.java`，以及 `commons/utils/SqlparserUtils.java`、`DeSqlparserUtils.java`。图表运行时仍可能读取 SQL 变量，需检查 `chart/manage/ChartDataManage.java`。
4. **改保存结构**：看 `dataset/server/DatasetTreeServer.java` → `dataset/manage/DatasetGroupManage.java` → `DatasetTableManage.java`、`DatasetTableFieldManage.java`，再查 SDK `DatasetGroupInfoDTO`、`DatasetTableDTO` 和对应实体。保存和预览是不同链路，要分别核对。
5. **验证**：至少覆盖 SQL 预览、变量默认值、保存后重开、图表取数、权限拒绝及受影响的数据源方言。

### 新增一条业务规则

从前端 `src/api/` 或 SDK `*Api` 找入口，追到 `server` 和 `manage`。规则属于取数、权限、数据源方言还是图表类型，就放在相应 `manage`、权限实现、`provider` 或 `Handler`。若要新增请求字段，按“前端类型/API → SDK DTO/API → 后端实现 → 扩展实现”核对；若规则只在内部生效，不必人为新增接口。明确旧数据和旧调用方行为，并为开关的默认值、关闭路径与开启路径提供验证。

### 新增表或字段

1. 定位现有实体、Repository、查询投影和 API 模型，确认字段属于**元数据**还是外部分析数据。元数据实体沿用 JPA、`jakarta.persistence` 和现有 ID 规则。
2. 修改实体与 Repository/QueryDSL 使用处；需要对外传递时同步 DTO、序列化、前端类型及权限检查。不要直接向接口暴露带内部状态的实体。
3. 为存量数据库增加**新的**版本化 `SqlBlock` 迁移，查看 `sdk/common/.../initSql/SqlBlock.java`、`core/core-backend/.../listener/InitSqlListener.java` 和 `listener/sql/CoreSqlBlock.java`；核对版本组、顺序、默认值、存量数据和失败重试。`ddl-auto: update` 不能替代迁移。
4. 修改保存逻辑前读 `config/JpaUpdateNonNullAspect.java`：Repository 的 `save` 系列会合并非空属性。需要显式清空字段时，用明确的更新语句并验证事务与后续读取。
5. 在隔离数据库验证新装、存量升级、失败重试、保存、读取和回滚路径；启动应用可能执行迁移。

## 每次改动的核对顺序

1. `git status --short`；涉及扩展时另查 `git -C de-xpack status --short`，保留既有修改。
2. 找真实入口及所有调用方；确认 SDK、前端、核心、扩展、分享/嵌入/导出是否涉及。
3. 明确持久化契约、权限、旧数据兼容与开关默认行为，再做最小完整改动。
4. 按影响选最小有效验证，最后检查 `git diff --check` 和差异；只报告实际执行的验证。

本文件是导航索引。实际修改前仍以当前源码、POM 和运行配置为准。
