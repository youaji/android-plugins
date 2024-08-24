@file:Suppress("DEPRECATION")
package com.youaji.android.plugin.publisher

import com.android.build.gradle.LibraryExtension
import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.component.SoftwareComponent
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.TaskAction
import org.gradle.jvm.tasks.Jar
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URI
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

open class PublisherTask : DefaultTask() {

    // 是否完成执行任务
    private var executeFinishFlag: AtomicBoolean = AtomicBoolean(false)

    // 检验状态是否通过
    private var checkStatus = false
    private var publisher: Publisher
    private var publishingTaskName = ""
    private var sourceTaskName = ""
    private var repoUrl: URI = URI("")
    private var isAndroidModule = false
    private var isSnapshot = false

    init {
        group = "publisher"

        project.run {
            publisher = extensions.getByName(Publisher.name) as Publisher
            // 动态为该模块引入上传插件
            apply(hashMapOf<String, String>(Pair("plugin", "maven-publish")))
            val publishing = project.extensions.getByType(PublishingExtension::class.java)
            // 当前模块是否为 android 模块，当前只支持 android 和 java 模块，非 android 即 java
            isAndroidModule = project.plugins.hasPlugin("com.android.library")
            val sourceTask = dynamicGenerateSourceTask(isAndroidModule)
            afterEvaluate {
                components.forEach {
                    if (isAndroidModule) {
                        // Android 只注册 release 的发布，如果需要 all/debug 或更多其他的可以自定义注册
                        if (it.name == "release") {
                            doRegisterTask(publishing, it.name, it, sourceTask)
                        }
                    } else {
                        // Java 只注册 java 的发布，如果需要 kotlin 或更多其他的可以自定义注册
                        if (it.name == "java") {
                            doRegisterTask(publishing, it.name, it, sourceTask)
                        }
                    }
                }
            }
        }
    }


    private fun doRegisterTask(
        publishing: PublishingExtension,
        name: String,
        component: SoftwareComponent,
        withSourceTask: Task?,
    ) {
        publishingTaskName = "publish${name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }}PublicationToMavenRepository"
        println("Publisher->register task:$name, sourceTask:$withSourceTask")

        publishing.publications { publications ->
            if (publications.findByName(name) == null) {
                publications.create(
                    name,
                    MavenPublication::class.java
                ) { publication ->
                    publication.groupId = publisher.lib.group
                    publication.artifactId = publisher.lib.artifact
                    publication.version = publisher.lib.version
                    withSourceTask?.let { publication.artifact(it) }
                    publication.from(component)
                }

                publishing.repositories { artifactRepositories ->

                    artifactRepositories.maven { mavenArtifactRepository ->

                        isSnapshot = publisher.lib.version.endsWith("SNAPSHOT")
                        repoUrl =
                            if (isSnapshot) URI(publisher.repo.snapshotUrl)
                            else URI(publisher.repo.releaseUrl)

                        mavenArtifactRepository.url = repoUrl
                        mavenArtifactRepository.credentials { credentials ->
                            credentials.username = publisher.repo.username
                            credentials.password = publisher.repo.password
                        }
                    }
                }
            }
        }
    }

    /**
     * 动态生成源码 task
     * @param isAndroidModule 是否为android模块
     */
    @Suppress("DEPRECATION")
    private fun dynamicGenerateSourceTask(isAndroidModule: Boolean): Task? {

        if (!publisher.lib.withSource) return null

        // 其实叫什么都无所谓
        sourceTaskName =
            if (isAndroidModule) "androidSourcesJar"
            else "sourcesJar"

        // 这个地方的 api 跟脚本中所有不同
        val sourceSetFiles =
            if (isAndroidModule) {
                // 获取 build.gradle 中的 android节点
                val androidSet = project.extensions.getByName("android") as LibraryExtension
                val sourceSet = androidSet.sourceSets
                // 获取 android 节点下的源码目录
                sourceSet.findByName("main")?.java?.srcDirs
            } else {
                // 获取 java 模块中的源码目录
                val plugin = project.convention.getPlugin(JavaPluginConvention::class.java)
                plugin.sourceSets.findByName("main")?.allSource
            }

        // 查找或注册源码 task
        val task =  project.tasks.findByName(sourceTaskName) ?: project.tasks.create(
            sourceTaskName,
            Jar::class.java
        ) {
            it.from(sourceSetFiles)
            it.archiveClassifier.set("sources")
        }
        println("Publisher->generate task:${task.name}")
        return task
    }

    private fun isWindows(): Boolean =
        System.getProperty("os.name").lowercase().contains("windows")

    private fun executeTask() {
        println("┌———————————————————————————————————————————————————————————")
        println("│ 上传[${project.name}]至${if (isSnapshot) "[快照]" else "[生产]"}仓库")
        println("│ version:     [${publisher.lib.version}]")
        println("│ url:         [$repoUrl]")
        println("└———————————————————————————————————————————————————————————")
        // 1、对 publisher 配置的信息进行基础校验
        // 2、把 publisher 上传到服务器端，做版本重复性校验
        checkStatus = requestCheckVersion()
        // 如果前两步都校验通过了，checkStatus 设置为 true

        val task =
            project.projectDir.absolutePath
                .removePrefix(project.rootDir.absolutePath)
                .replace(File.separator, ":") + ":$publishingTaskName"

        println("┌———————————————————————————————————————————————————————————")
        println("│      --- publisher.repo ---")
        println("│ username    = ${publisher.repo.username}")
//        println("│ password    = ${publisher.repo.password}")
        println("│ password    = ********")
        println("│ releaseUrl  = ${publisher.repo.releaseUrl}")
        println("│ snapshotUrl = ${publisher.repo.snapshotUrl}")
        println("│       --- publisher.lib ---")
        println("│ group       = ${publisher.lib.group}")
        println("│ artifact    = ${publisher.lib.artifact}")
        println("│ version     = ${publisher.lib.version}")
        println("│ desc        = ${publisher.lib.desc}")
        println("│ withSource  = ${publisher.lib.withSource}")
        println("│       --- publisher.pom ---")
        println("│ name        = ${publisher.pom.name}")
        println("│ url         = ${publisher.pom.url}")
        println("│ desc        = ${publisher.pom.desc}")
        println("│ publisher   = ${publisher.pom.publisher}")
        println("│         --- task info ---")
        println("│ isAndroid   = $isAndroidModule")
        println("│ source      = $sourceTaskName")
        println("│ publishing  = $publishingTaskName")

        val exeCommand =
            if (isWindows()) "${project.rootDir}${File.separator}gradlew.bat"
            else "${project.rootDir}${File.separator}gradlew"

        println("│         --- build info ---")
        println("│ command     = $exeCommand")
        println("│ task        = $task")

        if (checkStatus) {
            val out = ByteArrayOutputStream()
            // 通过命令行的方式进行调用上传 maven 的 task
            project.exec { exec ->
                exec.standardOutput = out
                exec.isIgnoreExitValue = true
                exec.commandLine(exeCommand, task)
            }
            val isBuildSuccessful = out.toString().contains("BUILD SUCCESSFUL")
            println("│ state       = ${if (isBuildSuccessful) "Success" else "fail"}")
            if (isBuildSuccessful) {
                requestUploadVersion()
//                // 上传 maven 仓库成功，上报到服务器
//                val isSuccess = requestUploadVersion()
//                if (isSuccess) {
//                    // 提示成功信息
//                } else {
//                    // 提示错误信息
//                }
                executeFinish()
            } else {
                println("│ state       = 上传 Maven 仓库失败，请检查配置！")
            }
        }
        println("└———————————————————————————————————————————————————————————")
    }

    private fun requestCheckVersion(): Boolean {
        //TODO 上报服务器进行版本检查,这里直接模拟返回成功
        return true
    }

    private fun requestUploadVersion(): Boolean {
        //TODO 上报服务器进行版本更新操作,这里直接模拟返回成功
        return true
    }

    /**
     * 任务执行完毕
     */
    private fun executeFinish() {
        executeFinishFlag.set(true)
    }

    @TaskAction
    fun doTask() {
        executeTask()

        // 开启线程守护，防止子线程任务还没执行完毕，task 就已经结束了
        while (!executeFinishFlag.get()) {
            Thread.sleep(500)
        }
    }
}