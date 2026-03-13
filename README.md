# 学生信息管理系统（Spring Boot + Vue 离线架构）

## 1. 项目结构
- `backend/`：Spring Boot 后端（含静态前端页面）。
- `sql/schema.sql`：MySQL 建库建表、外键约束、初始化数据。
- `deploy/docker-compose.yml`：通用容器部署（MySQL + Tomcat）。
- `deploy/windows/`：Windows 本地测试用快速部署脚本与打包脚本。
- `docs/建设进度评估与下一步方向.md`：项目阶段性进度评估。

## 2. 当前完成进度（简版）
- ✅ 鉴权：`POST /api/auth/login`（JWT）
- ✅ 学生 CRUD：`/api/students`
- ✅ 导入导出与模板下载：
  - `POST /api/students/import`
  - `GET /api/students/export?type=xlsx|csv`
  - `GET /api/students/template?type=xlsx|csv`
- ✅ 统计：`GET /api/students/stats/employment`
- ✅ API 集成测试（鉴权/CRUD/导入导出/统计）
- ✅ 基础 CI（`compile + test`）
- ✅ 前端已补齐新增/编辑/删除、导入导出、模板下载、统一错误提示和 loading

## 3. 本地开发与测试
```bash
cd backend
mvn -s ../build-support/maven/settings.xml clean test
mvn -s ../build-support/maven/settings.xml clean package
```

## 4. Swagger/OpenAPI
启动后访问：
- `http://localhost:8080/student/swagger-ui.html`
- `http://localhost:8080/student/v3/api-docs`

## 5. Windows 本地快速部署（用于简单测试）

### 5.1 方式 A：直接使用项目源码
```bash
# 1) 打包 WAR
cd backend
mvn -s ../build-support/maven/settings.xml clean package

# 2) 生成 windows 发布包（zip）
cd ..
./deploy/windows/package-release.sh
```
生成：`deploy/windows/dist/student-system-windows.zip`

将该 zip 拷贝到 Windows 后：
1. 解压。
2. 双击 `start.bat`。
3. 打开 `http://localhost:8080/student/`。

默认账号：`admin / admin123`

### 5.2 方式 B：使用解压后的发布包
发布包目录内已包含：
- `student-system-1.0.0.war`
- `docker-compose.yml`
- `start.bat` / `stop.bat`
- `sql/schema.sql`
- `快速部署说明.md`

直接双击 `start.bat` 即可启动；测试完后双击 `stop.bat` 停止。

## 6. Docker Compose（源码目录下）
```bash
cd backend
mvn -s ../build-support/maven/settings.xml clean package -DskipTests
cd ../deploy
docker compose up -d
```

访问入口：
- 系统主页：`http://localhost:8080/student/`

## 7. 离线前端资源
前端依赖使用本地静态资源（不依赖 CDN）：
- `backend/src/main/resources/static/lib/vue/vue.global.prod.js`
- `backend/src/main/resources/static/lib/element-plus/index.full.min.js`
- `backend/src/main/resources/static/lib/element-plus/index.css`
- `backend/src/main/resources/static/lib/echarts/echarts.min.js`
