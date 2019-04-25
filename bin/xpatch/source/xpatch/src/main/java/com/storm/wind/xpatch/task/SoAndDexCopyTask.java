package com.storm.wind.xpatch.task;

import com.storm.wind.xpatch.util.FileUtils;

import java.io.File;
import java.util.HashMap;

/**
 * Created by Wind
 */
public class SoAndDexCopyTask implements Runnable {

    private static final String SO_FILE_NAME = "libxpatch_wl.so";
    private static final String XPOSED_MODULE_FILE_NAME_PREFIX = "libxpatch_xp_module_";
    private static final String SO_FILE_SUFFIX = ".so";

    private final String[] APK_LIB_PATH_ARRAY = {
            "lib/armeabi-v7a/",
            "lib/armeabi/",
            "lib/arm64-v8a/"
    };

    private final HashMap<String, String> SO_FILE_PATH_MAP = new HashMap<String, String>() {
        {
            put(APK_LIB_PATH_ARRAY[0], "assets/lib/armeabi-v7a/" + SO_FILE_NAME);
            put(APK_LIB_PATH_ARRAY[1], "assets/lib/armeabi-v7a/" + SO_FILE_NAME);
            put(APK_LIB_PATH_ARRAY[2], "assets/lib/arm64-v8a/" + SO_FILE_NAME);
        }
    };

    private int dexFileCount;
    private String unzipApkFilePath;
    private String[] xposedModuleArray;

    public SoAndDexCopyTask(int dexFileCount, String unzipApkFilePath, String[] xposedModuleArray) {
        this.dexFileCount = dexFileCount;
        this.unzipApkFilePath = unzipApkFilePath;
        this.xposedModuleArray = xposedModuleArray;
    }

    @Override
    public void run() {
        // 复制xposed兼容层的dex文件以及so文件到当前目录下
        copySoFile();
        copyDexFile(dexFileCount);

        // 删除签名信息
        deleteMetaInfo();
    }

    private void copySoFile() {
        String apkSoLibPath = findTargetLibPath();
        String apkSoFullPath = fullLibPath(apkSoLibPath);

        copyLibFile(apkSoFullPath, SO_FILE_PATH_MAP.get(apkSoLibPath));

        // copy xposed modules into the lib path
        if (xposedModuleArray != null && xposedModuleArray.length > 0) {
            int index = 0;
            for (String modulePath : xposedModuleArray) {
                modulePath = modulePath.trim();
                if (modulePath == null || modulePath.length() == 0) {
                    continue;
                }
                File moduleFile = new File(modulePath);
                if (!moduleFile.exists()) {
                    continue;
                }
                String outputModuleFile = XPOSED_MODULE_FILE_NAME_PREFIX + index + SO_FILE_SUFFIX;
                FileUtils.copyFile(moduleFile, new File(apkSoFullPath, outputModuleFile));
                index++;
            }
        }
    }

    private void copyDexFile(int dexFileCount) {
        //  copy dex file to root dir, rename it first
        String copiedDexFileName = "classes" + (dexFileCount + 1) + ".dex";
        // assets/classes.dex分隔符不能使用File.seperater,否则在windows上无法读取到文件，IOException
        FileUtils.copyFileFromJar("assets/classes.dex", unzipApkFilePath + copiedDexFileName);
    }

    private String fullLibPath(String libPath) {
        return unzipApkFilePath + libPath.replace("/", File.separator);
    }

    private void copyLibFile(String libFilePath, String srcSoPath) {
        File apkSoParentFile = new File(libFilePath);
        if (!apkSoParentFile.exists()) {
            apkSoParentFile.mkdirs();
        }

        // get the file name first
        int lastIndex = srcSoPath.lastIndexOf('/');
        int length = srcSoPath.length();
        String soFileName = srcSoPath.substring(lastIndex, length);

        // do copy
        FileUtils.copyFileFromJar(srcSoPath, new File(apkSoParentFile, soFileName).getAbsolutePath());
    }

    // Try to find the lib path where the so file should put.
    // If there is many lib path, try to find the path which has the most so files
    private String findTargetLibPath() {
        int maxChildFileCount = 0;
        int maxChildFileIndex = 0;
        int index = 0;
        for (String libPath : APK_LIB_PATH_ARRAY) {
            String fullPath = fullLibPath(libPath);
            File file = new File(fullPath);
            if (file.exists()) {
                String[] childList = file.list();
                int childCount = 0;
                if (childList != null) {
                    childCount = childList.length;
                }
                if (childCount > maxChildFileCount) {
                    maxChildFileCount = childCount;
                    maxChildFileIndex = index;
                }
            }
            index++;
        }

        return APK_LIB_PATH_ARRAY[maxChildFileIndex];
    }

    private void deleteMetaInfo() {
        String metaInfoFilePath = "META-INF";
        File metaInfoFileRoot = new File(unzipApkFilePath + metaInfoFilePath);
        if (!metaInfoFileRoot.exists()) {
            return;
        }
        File[] childFileList = metaInfoFileRoot.listFiles();
        if (childFileList == null || childFileList.length == 0) {
            return;
        }
        for (File file : childFileList) {
            String fileName = file.getName().toUpperCase();
            if (fileName.endsWith(".MF") || fileName.endsWith(".RAS") || fileName.endsWith(".SF")) {
                file.delete();
            }
        }
    }
}
