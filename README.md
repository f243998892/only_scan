# 二维码扫描上传应用

这是一个安卓App，主要功能是扫描二维码并上传至自建数据库。

## 解决Gradle下载问题

当前遇到的问题是Gradle无法正常下载和配置。以下是解决方案：

### 方案1：修改Gradle配置

1. 创建`gradle/wrapper/gradle-wrapper.jar`文件（可以从其他Android项目复制）
2. 修改Gradle缓存目录：
   - 在系统环境变量中设置`GRADLE_USER_HOME`为自定义路径，如`D:\gradle_home`
   - 确保该路径有写入权限

### 方案2：使用离线Gradle包

1. 手动下载Gradle 8.1：https://services.gradle.org/distributions/gradle-8.1-bin.zip
2. 将下载的zip包放到本地Gradle缓存目录
3. 修改`gradle/wrapper/gradle-wrapper.properties`指向本地文件

### 方案3：解决Java路径问题

如果遇到错误`Value 'C:\Program Files\Java\jdk-17' given for org.gradle.java.home Gradle property is invalid`：
1. 删除`local.properties`和`gradle.properties`中的`org.gradle.java.home`配置
2. 确保系统已正确安装JDK并配置JAVA_HOME环境变量
3. 在Android Studio的设置中正确配置JDK路径

## 项目结构

- `app/src/main/java/com/qrscan/` - 应用程序源代码
- `app/src/main/res/` - 资源文件
- `app/src/main/AndroidManifest.xml` - 应用配置文件

## 功能特点

- 高识别率的二维码扫描（支持低光照环境）
- 本地数据缓存，支持离线扫描
- 直连数据库上传数据
- 支持单个和连续扫码模式

## 技术栈

- 语言：Kotlin
- 架构：MVVM + Jetpack
- 相机：CameraX
- 二维码识别：ZXing
- 本地存储：Room
- 网络请求：直连数据库

## 使用说明

1. 登录：输入姓名后登录
2. 选择工序类型：浸漆或车止口
3. 扫码方式：
   - 单个扫码：扫描后自动上传
   - 连续扫码：可扫描多个二维码，点击上传按钮一次性上传

## 数据库配置

- 主机：s5.gnip.vip
- 端口：33946
- 数据库：scan_db
- 用户：fh
- 表：public.products

## 开发者

- 创建日期：2023年5月 