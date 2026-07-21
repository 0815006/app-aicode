# Fastjson 1.x → Fastjson2 升级替换方案

## 1. 背景

Fastjson 1.x 已停止维护，存在多项已知安全漏洞（CVE），各类安全扫描工具（如 BlackDuck、SonarQube、Fortify 等）均会将其标记为高危组件。**Fastjson2** 是官方推出的替代方案，在性能、安全性和 API 设计上全面优于 1.x 版本。

| | fastjson 1.x (已停止维护) | fastjson2 (活跃维护) |
|---|---|---|
| 最新版本 | 1.2.83 | 2.0.53+ |
| 安全漏洞 | 多个 CVE（反序列化 RCE 等高危） | 已修复，持续安全更新 |
| 性能 | 基准 | JSON 解析速度提升 2~5 倍 |
| Java 版本 | Java 6+ | Java 8+ |
| 包名 | `com.alibaba.fastjson` | `com.alibaba.fastjson2` |

---

## 2. 两种升级路径

### 路径 A：兼容包迁移（不推荐）

fastjson2 提供了一个兼容包，**包名保持不变**（仍是 `com.alibaba.fastjson`），只需替换 Maven 坐标即可，**不需要修改任何 Java 代码**。

```xml
<!-- 原依赖 -->
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>fastjson</artifactId>
    <version>1.2.83</version>
</dependency>

<!-- 替换为兼容包 -->
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>fastjson</artifactId>
    <version>2.0.53</version>
</dependency>
```

**缺点**：部分安全扫描工具仍可能根据包名将其误报为 fastjson 1.x，导致告警无法彻底消除。

### 路径 B：原生包迁移（推荐⭐）

使用 fastjson2 原生包，**包名和 Maven 坐标同时变更**，需要修改 Java 代码中的 `import` 语句。

```xml
<!-- 替换为 fastjson2 原生包 -->
<dependency>
    <groupId>com.alibaba.fastjson2</groupId>
    <artifactId>fastjson2</artifactId>
    <version>2.0.53</version>
</dependency>
```

**优点**：彻底解决安全扫描告警，享受 fastjson2 全部特性。
**缺点**：需要修改所有使用 fastjson 的 Java 源文件。

> 以下内容均基于 **路径 B** 展开。

---

## 3. 升级步骤

### 步骤一：扫描项目中的 fastjson 引用

使用 IDE 全局搜索（Ctrl+Shift+F）或命令行扫描：

```bash
# 搜索 fastjson 依赖声明 (Maven/Gradle)
grep -rn "com.alibaba" pom.xml
grep -rn "com.alibaba:fastjson" build.gradle

# 搜索 Java 文件中的引用
grep -rn "import com.alibaba.fastjson" --include="*.java" .
grep -rn "com\.alibaba\.fastjson\." --include="*.java" .
```

重点关注两种引用方式：
- **import 导入**：`import com.alibaba.fastjson.JSON;`
- **全限定名调用**：`com.alibaba.fastjson.JSON.parseObject(xxx)`（容易遗漏！）

### 步骤二：替换 Maven/Gradle 依赖

**Maven (`pom.xml`)**：

```xml
<!-- 删除 -->
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>fastjson</artifactId>
    <version>1.2.83</version>
</dependency>

<!-- 替换为 -->
<dependency>
    <groupId>com.alibaba.fastjson2</groupId>
    <artifactId>fastjson2</artifactId>
    <version>2.0.53</version>
</dependency>
```

**Gradle (`build.gradle`)**：

```groovy
// 删除
implementation 'com.alibaba:fastjson:1.2.83'

// 替换为
implementation 'com.alibaba.fastjson2:fastjson2:2.0.53'
```

### 步骤三：替换 Java 代码中的包引用

| 原引用（fastjson 1.x） | 改为（fastjson2） |
|---|---|
| `import com.alibaba.fastjson.JSON;` | `import com.alibaba.fastjson2.JSON;` |
| `import com.alibaba.fastjson.JSONObject;` | `import com.alibaba.fastjson2.JSONObject;` |
| `import com.alibaba.fastjson.JSONArray;` | `import com.alibaba.fastjson2.JSONArray;` |
| `import com.alibaba.fastjson.JSONException;` | `import com.alibaba.fastjson2.JSONException;` |
| `import com.alibaba.fastjson.annotation.JSONField;` | `import com.alibaba.fastjson2.annotation.JSONField;` |
| `import com.alibaba.fastjson.TypeReference;` | `import com.alibaba.fastjson2.TypeReference;` |
| `import com.alibaba.fastjson.JSONPath;` | `import com.alibaba.fastjson2.JSONPath;` |

**批量替换正则（适用于 VS Code / IntelliJ IDEA）**：

```
查找: import com\.alibaba\.fastjson\.
替换: import com.alibaba.fastjson2.
```

```
查找: com\.alibaba\.fastjson\.
替换: com.alibaba.fastjson2.
```

### 步骤四：处理传递依赖冲突（可选）

检查是否有其他依赖间接引入了 fastjson 1.x：

```bash
# Maven 依赖树
mvn dependency:tree -Dincludes=com.alibaba:fastjson

# Gradle 依赖树
gradle dependencies --configuration runtimeClasspath | grep fastjson
```

如果存在传递依赖，需在对应依赖上添加 `<exclusion>`：

```xml
<dependency>
    <groupId>some.library</groupId>
    <artifactId>some-artifact</artifactId>
    <exclusions>
        <exclusion>
            <groupId>com.alibaba</groupId>
            <artifactId>fastjson</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

### 步骤五：编译验证

```bash
mvn clean compile
```

确认编译通过且无 fastjson 1.x 相关错误。

---

## 4. API 兼容性说明

fastjson2 核心 API 与 fastjson 1.x **高度兼容**，以下常用方法签名和行为完全一致：

### 4.1 JSON 工具类

```java
// 解析 JSON 字符串 → 对象
JSON.parseObject(jsonStr, TargetClass.class);
JSON.parseArray(jsonStr, TargetClass.class);

// 对象 → JSON 字符串
JSON.toJSONString(object);
JSON.toJSONString(object, SerializerFeature.PrettyFormat);

// JSON 对象/数组
JSONObject obj = JSON.parseObject(jsonStr);
JSONArray arr = JSON.parseArray(jsonStr);
```

### 4.2 JSONObject / JSONArray

```java
// 获取值
obj.getString("key");
obj.getInteger("key");
obj.getBoolean("key");
obj.getJSONObject("key");
obj.getJSONArray("key");

// 设置值
obj.put("key", value);

// 转为 JSON 字符串
obj.toJSONString();
arr.toJSONString();

// 反射创建
JSONObject.parseObject(jsonStr);
```

### 4.3 注解

```java
// fastjson 1.x
import com.alibaba.fastjson.annotation.JSONField;

// fastjson2（名称和用法完全一致）
import com.alibaba.fastjson2.annotation.JSONField;

@JSONField(name = "field_name")
private String fieldName;

@JSONField(format = "yyyy-MM-dd")
private Date createTime;
```

### 4.4 TypeReference

```java
// fastjson 1.x
import com.alibaba.fastjson.TypeReference;

// fastjson2
import com.alibaba.fastjson2.TypeReference;

List<User> users = JSON.parseObject(jsonStr, new TypeReference<List<User>>() {});
```

---

## 5. 常见不兼容情况与处理

### 5.1 `JSON.parseObject` 返回类型变化

fastjson2 的 `JSON.parseObject(String)` 返回 `JSONObject`，与 1.x 行为一致。但某些边界场景下反序列化策略可能略有差异。如果遇到解析异常，可显式指定类型：

```java
// 不推荐（可能因类型推断差异导致异常）
Object obj = JSON.parseObject(jsonStr);

// 推荐（显式指定目标类型）
TargetClass obj = JSON.parseObject(jsonStr, TargetClass.class);
```

### 5.2 `SerializerFeature` 枚举值

大部分枚举值兼容，但少数已废弃或重命名：

| fastjson 1.x | fastjson2 |
|---|---|
| `SerializerFeature.WriteNullListAsEmpty` | `JSONWriter.Feature.WriteNullListAsEmpty` |
| `SerializerFeature.WriteMapNullValue` | `JSONWriter.Feature.WriteMapNullValue` |
| `SerializerFeature.PrettyFormat` | `JSONWriter.Feature.PrettyFormat` |

```java
// fastjson 1.x
JSON.toJSONString(obj, SerializerFeature.PrettyFormat, SerializerFeature.WriteMapNullValue);

// fastjson2
JSON.toJSONString(obj, JSONWriter.Feature.PrettyFormat, JSONWriter.Feature.WriteMapNullValue);
```

### 5.3 `JSONPath` 差异

fastjson2 的 JSONPath 与 1.x 基本兼容，但部分高级表达式有差异。如果项目中大量使用 JSONPath，建议升级后全量回归测试。

### 5.4 Spring Boot 默认 JSON 序列化

如果项目中已将 fastjson 配置为 Spring MVC 的默认消息转换器（如通过 `HttpMessageConverter`），需同步修改配置类中的引用：

```java
// fastjson 1.x
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;

// fastjson2
import com.alibaba.fastjson2.support.spring.http.converter.FastJsonHttpMessageConverter;
```

---

## 6. 常见框架兼容性

| 框架 | fastjson2 兼容情况 | 说明 |
|---|---|---|
| Spring Boot 2.x | ✅ 完全兼容 | 需检查 `WebMvcConfigurer` 中的消息转换器配置 |
| Spring Boot 3.x | ✅ 完全兼容 | fastjson2 支持 Jakarta EE |
| MyBatis-Plus 3.x | ✅ 不依赖 fastjson | 无影响 |
| Shiro 1.4+ | ✅ 不依赖 fastjson | 无影响 |
| Dubbo | ⚠️ 需检查版本 | Dubbo 2.7.x 默认使用 fastjson 1.x 序列化，需额外配置 |
| Sentinel | ⚠️ 需检查版本 | 部分版本传递依赖 fastjson 1.x |

### Dubbo 特殊处理

如果项目使用 Dubbo，且序列化方式配置为 fastjson，需同步修改：

```xml
<!-- Dubbo 协议配置 -->
<dubbo:protocol name="dubbo" serialization="fastjson2" />
```

并确保 Dubbo 版本支持 fastjson2 序列化（Dubbo 3.1+ 内置支持）。

---

## 7. 验证清单

升级完成后，请逐项确认：

- [ ] `pom.xml` / `build.gradle` 中无 `com.alibaba:fastjson:1.x` 依赖
- [ ] `mvn dependency:tree` 中无 fastjson 1.x 传递依赖
- [ ] 所有 Java 文件的 `import` 和全限定名已改为 `com.alibaba.fastjson2`
- [ ] 项目编译通过（`mvn clean compile`）
- [ ] 单元测试全部通过
- [ ] 安全扫描不再报告 fastjson 1.x 相关漏洞
- [ ] 涉及 JSON 序列化/反序列化的接口回归测试通过

---

## 8. 快速替换脚本参考

以下 Shell 脚本可帮助批量替换 Java 和 XML 文件（请根据项目实际情况调整）：

```bash
#!/bin/bash
# Fastjson 1.x → 2.x 批量替换脚本

# 1. 替换 Java 文件中的 import
find . -type f -name "*.java" -exec sed -i 's/import com\.alibaba\.fastjson\./import com.alibaba.fastjson2./g' {} \;

# 2. 替换 Java 文件中全限定名调用
find . -type f -name "*.java" -exec sed -i 's/com\.alibaba\.fastjson\.JSON/com.alibaba.fastjson2.JSON/g' {} \;
find . -type f -name "*.java" -exec sed -i 's/com\.alibaba\.fastjson\.JSONObject/com.alibaba.fastjson2.JSONObject/g' {} \;
find . -type f -name "*.java" -exec sed -i 's/com\.alibaba\.fastjson\.JSONArray/com.alibaba.fastjson2.JSONArray/g' {} \;

# 3. 检查是否还有遗漏
echo "=== 检查遗漏的 fastjson 1.x 引用 ==="
grep -rn "com\.alibaba\.fastjson[^2]" --include="*.java" .
echo "=== 检查完毕 ==="
```

> ⚠️ 使用脚本前请先提交代码或备份，批量替换后务必人工审查和编译验证。

---

## 9. 常见问题 FAQ

### Q1: fastjson2 是否支持 Java 8？
**支持。** fastjson2 最低要求 Java 8，与公司的项目技术栈完全兼容。

### Q2: 升级后历史遗留的 JSON 序列化数据能否正常反序列化？
**可以。** fastjson2 完全兼容 fastjson 1.x 的序列化格式，历史数据无需迁移。

### Q3: 项目同时用了 fastjson 和 Jackson，会冲突吗？
**不会。** fastjson2 与 Jackson 包名完全独立，可共存。

### Q4: 升级后发现某个 API 行为不一致怎么办？
- 首选：调整调用代码适配 fastjson2
- 备选：使用 fastjson2 的兼容模式（路径 A），但需在安全扫描工具中加白名单

### Q5: Dubbo 项目中如何彻底移除 fastjson 1.x？
1. 全局搜索确认 Dubbo 序列化配置
2. 升级 Dubbo 至 3.1+，或替换序列化方式为 hessian2/kryo
3. 在 `pom.xml` 中排除 Dubbo 传递的 fastjson 1.x 依赖

---

## 10. 参考链接

- [fastjson2 官方文档](https://github.com/alibaba/fastjson2)
- [fastjson2 1.x 兼容模式说明](https://github.com/alibaba/fastjson2/wiki/fastjson2_compatible)
- [fastjson 1.x CVE 列表](https://nvd.nist.gov/vuln/search/results?query=fastjson)

---

> **文档版本**: v1.0 | **适用**: Java 8+ / Maven / Gradle / Spring Boot 全系
