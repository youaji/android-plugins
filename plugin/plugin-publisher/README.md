### 集成步骤
##### 1. 依赖
```groovy
buildscript {
    dependencies {
        classpath("com.youaji.plugin:publisher:x.x.x")
    }
}

plugins { }
```
##### 2. 引用
```groovy
plugins {
    id 'youaji-publisher'
}
```

##### 3. 配置
```groovy
publisher {
    repo {
        username = "repo.username"
        password = "repo.password"
        releaseUrl = "repo.releaseUrl"
        snapshotUrl = "repo.snapshotUrl"
    }

    lib {
        artifact = "artifact"
        group = "group"
        version = "version"
        desc = "desc"
        withSource = true
    }

    pom {
        name = "name"
        desc = "desc"
        url = "url"
        publisher = "publisher"
    }
}
android { }
```
##### 4. 使用

版本说明：正式版本，还是快照版本，取决于版本号后是否带有`-SNAPSHOT`

使用说明：对应 module Tasks 目录下找到对应 task，双击执行！

Task 说明：
- 发布至 maven 库
    - `publisher/publishToMaven`
      直接发布至 maven 库。
      publisher 插件的执行入口。
- Android module 子任务
    - `publishing/publishReleasePublicationToMavenLocal`
      发布到本地。
    - `publishing/publishReleasePublicationToMavenRepository`
      发布正式包到 maven 仓库。
      `publisher/publishToMaven` 的最终上传操作，其实就是调用的这个 task，直接执行的话就不会调用 publisher 插件的任务逻辑。
- Java module 子任务
    - `publishing/publishJavaPublicationToMavenLocal`
      同 Android module，只是 module 类型不一样。
    - `publishing/publishJavaPublicationToMavenRepository`
      同 Android module，只是 module 类型不一样。
