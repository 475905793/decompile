package com.wrbug.developerhelper.xposed.dumpdex

import com.wrbug.developerhelper.xposed.BuildConfig
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * OreoDump
 *
 * @author WrBug
 * @since 2018/3/23
 */
object OreoDump {

    fun log(txt: String) {
        if (!BuildConfig.DEBUG) {
            return
        }
        XposedBridge.log("developerhelper.xposed.native--> $txt")
    }

    fun init(lpparam: XC_LoadPackage.LoadPackageParam) {
        Native.dump(lpparam.packageName)
    }
}
