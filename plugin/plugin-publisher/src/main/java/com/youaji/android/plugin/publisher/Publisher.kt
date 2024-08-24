package com.youaji.android.plugin.publisher

import org.gradle.api.Action

open class Publisher {

    companion object {
        internal const val name = "publisher"
    }

    /** 仓库信息 */
    var repo = Repo()

    /** 库信息 */
    var lib = Lib()

    /** 版本信息 */
    var pom = Pom()

    fun repo(action: Action<Repo>) {
        action.execute(repo)
    }

    fun lib(action: Action<Lib>) {
        action.execute(lib)
    }

    fun pom(action: Action<Pom>) {
        action.execute(pom)
    }

    open class Repo {
        /** 用户名 */
        var username = ""

        /** 密码 */
        var password = ""

        /***/
        var releaseUrl = ""

        /***/
        var snapshotUrl = ""
    }

    open class Lib {
        /***/
        var artifact = ""

        /***/
        var group = ""

        /***/
        var version = ""

        /***/
        var desc = ""

        /** 是否携带源码发布 */
        var withSource = false
    }

    open class Pom {
        /***/
        var name = ""

        /***/
        var desc = ""

        /***/
        var url = ""

        /** 发布人 */
        var publisher = ""
    }

}