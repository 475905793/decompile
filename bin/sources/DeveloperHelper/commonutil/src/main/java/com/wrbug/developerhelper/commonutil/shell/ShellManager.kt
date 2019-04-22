package com.wrbug.developerhelper.commonutil.shell

import com.jaredrummler.android.shell.CommandResult
import com.wrbug.developerhelper.commonutil.CommonUtils
import com.wrbug.developerhelper.commonutil.ShellUtils
import com.wrbug.developerhelper.commonutil.entity.FragmentInfo
import com.wrbug.developerhelper.commonutil.entity.LsFileInfo
import com.wrbug.developerhelper.commonutil.entity.TopActivityInfo
import java.io.File
import java.util.regex.Pattern


object ShellManager {
    private const val SHELL_TOP_ACTIVITY = "dumpsys activity top"
    private const val SHELL_PROCESS_PID_1 = "ps -ef | grep \"%1\$s\" | grep -v %1\$s:| grep -v grep | awk '{print \$2}'"
    private const val SHELL_PROCESS_PID_2 = "top -b -n 1 |grep %1\$s |grep -v grep|grep -v %1\$s:"
    private const val SHELL_PROCESS_PID_3 = "top -n 1 |grep %1\$s |grep -v grep|grep -v %1\$s:"
    private var SHELL_OPEN_ACCESSiBILITY_SERVICE = arrayOf(
        "settings put secure enabled_accessibility_services com.wrbug.developerhelper/com.wrbug.developerhelper.service.DeveloperHelperAccessibilityService",
        "settings put secure accessibility_enabled 1"
    )
    private const val SHELL_LS_FILE = "ls -l %1\$s"
    private const val SHELL_GET_ZIP_FILE_LIST =
        "app_process -Djava.class.path=/data/local/tmp/zip.dex /data/local/tmp Zip %s"
    private const val SHELL_CHECK_IS_SQLITE = "od -An -tx %1\$s  |grep '694c5153'"
    private const val SHELL_UNINSTALL_APP = "pm uninstall %1\$s"
    private const val SHELL_CLEAR_APP_DATA = "pm clear %1\$s"
    private const val SHELL_FORCE_STOP_APP = "am force-stop %1\$s"
    fun getTopActivity(callback: Callback<TopActivityInfo?>) {
        ShellUtils.runWithSu(arrayOf(SHELL_TOP_ACTIVITY), object : ShellUtils.ShellResultCallback() {
            override fun onComplete(result: CommandResult) {
                callback.onSuccess(getTopActivity(result))
            }

            override fun onError(msg: String) {
                callback.onSuccess(null)
            }
        })

    }

    fun getTopActivity(result: CommandResult): TopActivityInfo {
        val stdout = result.getStdout()
        val topActivityInfo = TopActivityInfo()
        val task_s = stdout.split("TASK ")
        for (task_ in task_s) {
            if (task_.contains("mResumed=true")) {
                val regex = "ACTIVITY .* [0-9a-fA-F]+ pid.*"
                val pattern = Pattern.compile(regex)
                val matcher = pattern.matcher(task_)
                if (matcher.find()) {
                    topActivityInfo.activity = matcher.group().split(" ")[1]
                }
                val split =
                    task_.split("\n[ ]{4}[A-Z]".toRegex()).dropLastWhile { it.isEmpty() }
                for (s in split) {
                    if (s.contains("iew Hierarchy")) {
                        for (s1 in s.split("\n".toRegex()).dropLastWhile { it.isEmpty() }) {
                            if (s1.matches(".*\\{.*\\}.*".toRegex())) {
                                val data = s1.substring(s1.indexOf("{") + 1, s1.lastIndexOf("}"))
                                val split1 =
                                    data.split(" ".toRegex()).dropLastWhile { it.isEmpty() }
                                if (split1.size != 6 || split1[5].contains("id/").not()) {
                                    continue
                                }
                                topActivityInfo.viewIdHex[split1[5].substring(split1[5].indexOf("id/"))] =
                                        split1[4]
                            }
                        }
                    } else if (s.contains("ocal Activity")) {
                    } else if (s.contains("ctive Fragments")) {
                        val list = ArrayList<FragmentInfo>()
                        val split2 =
                            s.split("\n {6}#[0-9]+:".toRegex()).filterNot { it.isBlank() }
                        for (s2 in split2) {
                            if (s2.contains("mFragmentId=")) {
                                val name = s2.trim { it <= ' ' }.substring(0, s2.indexOf("{") - 1)
                                val fragmentInfo = FragmentInfo()
                                list.add(fragmentInfo)
                                fragmentInfo.name = name
                                val split3 =
                                    s2.replace("Child FragmentManager", "Child_FragmentManager").split(" ".toRegex())
                                        .dropLastWhile { it.isEmpty() }
                                for (s31 in split3) {
                                    if (s31.contains("Child_FragmentManager")) {
                                        break
                                    }
                                    val s3 = s31.replace("\n", "").replace(" ", "")
                                    when {
                                        s3.startsWith("mFragmentId=") -> fragmentInfo.fragmentId =
                                                s3.replace("mFragmentId=", "")
                                        s3.startsWith("mContainerId=") -> fragmentInfo.containerId =
                                                s3.replace("mContainerId=", "")
                                        s3.startsWith("mTag=") -> fragmentInfo.tag = s3.replace("mTag=", "")
                                        s3.startsWith("mState=") -> fragmentInfo.state =
                                                s3.replace("mState=", "").toInt()
                                        s3.startsWith("mIndex=") -> fragmentInfo.index =
                                                s3.replace("mIndex=", "").toInt()
                                        s3.startsWith("mWho=") -> fragmentInfo.who = s3.replace("mWho=", "")
                                        s3.startsWith("mBackStackNesting=") -> fragmentInfo.backStackNesting =
                                                s3.replace("mBackStackNesting=", "").toInt()
                                        s3.startsWith("mAdded=") -> fragmentInfo.added = s3.replace("mAdded=", "") ==
                                                "true"
                                        s3.startsWith("mRemoving=") -> fragmentInfo.removing = s3.replace(
                                            "mRemoving=",
                                            ""
                                        ) == "true"
                                        s3.startsWith("mFromLayout=") -> fragmentInfo.fromLayout = s3.replace(
                                            "mFromLayout=",
                                            ""
                                        ) ==
                                                "true"
                                        s3.startsWith("mInLayout=") -> fragmentInfo.inLayout = s3.replace(
                                            "mInLayout=",
                                            ""
                                        ) == "true"
                                        s3.startsWith("mHidden=") -> fragmentInfo.hidden = s3.replace("mHidden=", "") ==
                                                "true"
                                        s3.startsWith("mDetached=") -> fragmentInfo.detached = s3.replace(
                                            "mDetached=",
                                            ""
                                        ) == "true"
                                    }
                                }
                            }
                        }
                        topActivityInfo.fragments = list.toTypedArray()
                    }
                }
                break
            }
        }
        return topActivityInfo
    }

    fun lsFile(file: String): LsFileInfo? {
        val result: CommandResult = ShellUtils.runWithSu(String.format(SHELL_LS_FILE, file))
        if (result.isSuccessful.not()) {
            return null
        }
        val split = result.getStdout().split(" ")
        if (split.size <= 4) {
            return null
        }
        val info = LsFileInfo()
        info.permission = split[0]
        info.user = split[2]
        info.group = split[3]
        return info
    }

    fun getPid(packageName: String): String {
        var result: CommandResult = ShellUtils.runWithSu(String.format(SHELL_PROCESS_PID_1, packageName))
        if (result.isSuccessful) {
            return result.getStdout()
        }
        result = ShellUtils.runWithSu(String.format(SHELL_PROCESS_PID_2, packageName))
        if (result.isSuccessful.not()) {
            result = ShellUtils.runWithSu(String.format(SHELL_PROCESS_PID_3, packageName))
        }
        if (result.isSuccessful.not()) {
            return ""
        }
        return result.getStdout().trim().split(" ")[0]
    }

    fun getSqliteFiles(packageName: String): Array<File> {
        val dbPath = "/data/data/$packageName/databases"
        val list = lsDir(dbPath)
        val files = ArrayList<File>()
        for (file in list) {
            file?.run {
                val cmd = String.format(SHELL_CHECK_IS_SQLITE, "$dbPath/$file")
                val result = ShellUtils.runWithSu(cmd)
                if (result.isSuccessful && result.getStdout().isNullOrEmpty().not()) {
                    files.add(File(dbPath, file))

                }
            }
        }
        return files.toTypedArray()
    }

    fun openAccessibilityService(callback: Callback<Boolean>? = null) {
        ShellUtils.runWithSu(SHELL_OPEN_ACCESSiBILITY_SERVICE, object : ShellUtils.ShellResultCallback() {
            override fun onComplete(result: CommandResult) {
                callback?.onSuccess(result.isSuccessful && result.getStdout().isEmpty())
            }

            override fun onError(msg: String) {
                callback?.onSuccess(false)
            }
        })
    }

    fun catFile(filaPath: String): String {
        val commandResult = ShellUtils.runWithSu("cat $filaPath")
        return commandResult.getStdout()
    }

    fun rmFile(file: String): Boolean {
        val commandResult = ShellUtils.runWithSu("rm -rf $file")
        return commandResult.isSuccessful
    }

    fun modifyFile(filaPath: String, content: String): Boolean {
        val commandResult = ShellUtils.runWithSu("echo $content >> $filaPath")
        return commandResult.isSuccessful
    }

    fun cpFile(source: String, dst: String, mod: String = "666"): Boolean {
        val dir = dst.substring(0, dst.lastIndexOf("/"))
        var commandResult = ShellUtils.runWithSu("mkdir -p $dir")
        if (commandResult.isSuccessful.not()) {
            return false
        }
        commandResult = ShellUtils.runWithSu("cp -R $source $dst && chmod $mod $dst")
        return commandResult.isSuccessful || commandResult.getStderr()?.contains("Operation not permitted") ?: false
    }

    fun catFile(source: String, dst: String, mod: String? = null): Boolean {
        val cmds = arrayListOf<String>()
        cmds.add("cat $source  > $dst")
        if (mod != null) {
            cmds.add("chmod $mod $dst")
        }
        val commandResult = ShellUtils.runWithSu(*(cmds.toTypedArray()))
        return commandResult.isSuccessful
    }

    fun getZipFileList(path: String): List<String?> {
        val file = File(CommonUtils.application.cacheDir, "zip.dex")
        if (file.exists()) {
            ShellUtils.runWithSu("cp ${file.absolutePath} /data/local/tmp", "rm -rf ${file.absolutePath}")
        }
        val commandResult = ShellUtils.runWithSu(String.format(SHELL_GET_ZIP_FILE_LIST, path))
        return commandResult.stdout
    }


    fun lsDir(path: String): List<String?> {
        val commandResult = ShellUtils.runWithSu("ls $path")
        return commandResult.stdout
    }

    fun findApkDir(packageName: String): String {
        val cmd = "ls /data/app/|grep $packageName"
        val dir = ShellUtils.runWithSu(cmd).getStdout()
        return "/data/app/$dir/base.apk"
    }

    fun uninstallApp(packageName: String): Boolean {
        val commandResult = ShellUtils.runWithSu(String.format(SHELL_UNINSTALL_APP, packageName))
        return commandResult.isSuccessful
    }

    fun clearAppData(packageName: String): Boolean {
        val commandResult = ShellUtils.runWithSu(String.format(SHELL_CLEAR_APP_DATA, packageName))
        return commandResult.isSuccessful
    }

    fun forceStopApp(packageName: String): Boolean {
        val commandResult = ShellUtils.runWithSu(String.format(SHELL_FORCE_STOP_APP, packageName))
        return commandResult.isSuccessful
    }

}
