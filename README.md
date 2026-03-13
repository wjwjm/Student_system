# 学生信息管理系统（Spring Boot + Vue 离线架构）

## 1. 项目结构
- `backend/`：Spring Boot 后端（Controller-Service-Dao 分层）+ 本地静态前端资源。
- `sql/schema.sql`：MySQL 建库建表、外键约束、初始化数据。
- `deploy/docker-compose.yml`：离线可用的一键启动编排（MySQL + Tomcat）。

## 2. 后端能力
- 学生信息 CRUD：`/api/students`
- 导入导出：支持 `.xlsx` 与 `.csv`
  - 导入：`POST /api/students/import`
  - 导出：`GET /api/students/export?type=xlsx|csv`
  - 模板：`GET /api/students/template?type=xlsx|csv`
- 就业率聚合：`GET /api/students/stats/employment`
- 鉴权：`POST /api/auth/login`，基于本地 JWT 秘钥签发与验证（不依赖外网认证）。

## 3. 本地 Swagger/OpenAPI
启动后访问：
- `http://localhost:8080/student/swagger-ui.html`
- `http://localhost:8080/student/v3/api-docs`

## 4. Docker Compose 快速部署
> 第一次启动前，请先在宿主机编译 WAR

```bash
cd backend
mvn clean package -DskipTests
cd ../deploy
docker compose up -d
```

访问入口：
- 系统主页：`http://localhost:8080/student/`
- 默认账号：`admin / admin123`

## 5. 离线前端资源说明
为满足完全离线，前端依赖不走 CDN，统一从 `backend/src/main/resources/static/lib/` 读取：
- `vue/vue.global.prod.js`
- `element-plus/index.full.min.js` 与 `index.css`
- `echarts/echarts.min.js`

当前仓库提供了本地路径与占位文件，请将对应离线包放入上述路径。

---

## 6. README 进一步优化建议（可直接执行）

### 6.1 建议的构建与发布流程（更稳妥）
```bash
# 1) 后端打包
cd backend
mvn -U clean package -DskipTests

# 2) 校验 WAR 产物
ls -lh target/student-system-1.0.0.war

# 3) 启动容器
cd ../deploy
docker compose up -d

# 4) 查看容器状态与日志
docker compose ps
docker compose logs -f mysql tomcat
```

### 6.2 数据库初始化检查
```bash
# 查看 MySQL 初始化日志
docker compose logs mysql | tail -n 100

# 进入 MySQL 容器验证表结构
docker exec -it student-mysql mysql -uroot -proot123 -e "USE student_system; SHOW TABLES;"
```

---

## 7. 当前环境 Maven Central 403 的解决方案

你遇到的 403 通常是因为：
- 环境代理策略拦截；
- DNS/出口网络限制；
- Maven mirror 配置缺失或被错误覆盖。

下面给你一套“排查 + 修复”命令（按顺序执行）：

### 7.1 快速排查
```bash
# 1) 查看 Maven 生效配置与仓库地址
mvn -v
mvn help:effective-settings

# 2) 直连探测中央仓库（看是否 403/could not connect）
curl -I https://repo.maven.apache.org/maven2/

# 3) 检查环境代理变量
env | grep -Ei 'http_proxy|https_proxy|no_proxy'
```

### 7.2 使用国内镜像（联网环境推荐）
创建 `~/.m2/settings.xml`：
```bash
mkdir -p ~/.m2
cat > ~/.m2/settings.xml <<'XML'
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd">
  <mirrors>
    <mirror>
      <id>aliyunmaven</id>
      <mirrorOf>*</mirrorOf>
      <name>aliyun maven</name>
      <url>https://maven.aliyun.com/repository/public</url>
    </mirror>
  </mirrors>
</settings>
XML
```

然后执行：
```bash
cd backend
mvn -U clean package -DskipTests
```

### 7.3 完全离线环境（推荐用于内网/脱网）
在一台可联网机器上预下载依赖：
```bash
cd backend
mvn -U -DskipTests dependency:go-offline
```

打包本地仓库并拷贝到离线服务器：
```bash
tar -czf m2-cache.tar.gz ~/.m2/repository
# 拷贝到离线机器后
mkdir -p ~/.m2
tar -xzf m2-cache.tar.gz -C ~/.m2
```

离线机器上构建：
```bash
cd backend
mvn -o clean package -DskipTests
```

### 7.4 企业内网最佳实践（长期方案）
接入公司 Nexus/Artifactory，并把 `settings.xml` 的 mirror 指向内网私服，然后：
```bash
cd backend
mvn -U clean verify
```

---

## 8. 固化 Maven settings 与镜像策略（内网/离线可重复构建）

仓库已内置三套 Maven settings：
- 稳定在线配置（默认 Central）：`build-support/maven/settings.xml`
- 镜像加速配置（Aliyun）：`build-support/maven/settings-mirror.xml`
- 离线配置：`build-support/maven/settings-offline.xml`

### 8.1 在线环境（推荐，稳定优先）
```bash
cd backend
mvn -s ../build-support/maven/settings.xml clean compile
mvn -s ../build-support/maven/settings.xml test
```

### 8.2 在线环境（网络受限时，启用镜像加速）
```bash
cd backend
mvn -s ../build-support/maven/settings-mirror.xml clean compile
mvn -s ../build-support/maven/settings-mirror.xml test
```

### 8.3 预热依赖缓存（给离线环境准备）
```bash
cd backend
mvn -s ../build-support/maven/settings.xml -DskipTests dependency:go-offline
```

### 8.4 离线环境构建
```bash
cd backend
mvn -s ../build-support/maven/settings-offline.xml -o clean compile
mvn -s ../build-support/maven/settings-offline.xml -o test
```

> 若企业内网有 Nexus/Artifactory，可在 `build-support/maven/settings-mirror.xml` 中把 `<mirror>` 的 URL 替换为内网仓库地址，实现构建策略固化。
