package com.youaji.android.plugin.publisher

import org.gradle.api.Plugin
import org.gradle.api.Project

class PublisherPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        // 注册自定义参数
        project.extensions.create(Publisher.name, Publisher::class.java)

        val currProjectName = project.displayName

        // 在 afterProject 生命周期之后，才可以从 build.gradle 文件中获取到配置的参数
        project.gradle.afterProject { currProject ->
            // 如果是当前模块，才进行 task 的注册，避免冗余注册
            if (currProjectName == currProject.displayName) {
                // 注册上传 task，这里不要 dependsOn
                project.tasks.create("publishToMaven", PublisherTask::class.java)
            }
        }
    }
}