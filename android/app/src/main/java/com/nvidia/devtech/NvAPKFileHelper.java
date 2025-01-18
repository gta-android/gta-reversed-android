package com.nvidia.devtech;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class NvAPKFileHelper {
    private static final boolean logAssetFiles = false;
    private static NvAPKFileHelper instance = new NvAPKFileHelper();
    int apkCount = 0;
    String[] apkFiles;
    boolean hasAPKFiles = false;
    int myApkCount = 0;
    private Context context = null;

    public static NvAPKFileHelper getInstance() {
        return instance;
    }

    private int findInAPKFiles(String str) {
        if (this.myApkCount == 0) {
            return -1;
        }
        String str2 = str + ".mp3";
        for (int i = 0; i < this.apkFiles.length; i++) {
            if (str.compareToIgnoreCase(this.apkFiles[i]) == 0 || str2.compareToIgnoreCase(this.apkFiles[i]) == 0) {
                str.compareTo(this.apkFiles[i]);
                return i;
            }
        }
        return -1;
    }

    public void AddAssetFile(String str) {
        String[] strArr = this.apkFiles;
        int i = this.myApkCount;
        this.myApkCount = i + 1;
        strArr[i] = str;
    }

    public void GetAssetList() {
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(this.context.getAssets().open("assetfile.txt")));
            int parseInt = Integer.parseInt(bufferedReader.readLine());
            this.myApkCount = 0;
            if (parseInt > 0) {
                this.apkFiles = new String[parseInt];
                while (true) {
                    String readLine = bufferedReader.readLine();
                    if (readLine != null) {
                        String[] strArr = this.apkFiles;
                        int i = this.myApkCount;
                        this.myApkCount = i + 1;
                        strArr[i] = readLine;
                    } else {
                        return;
                    }
                }
            }
        } catch (Exception e) {
            AssetManager assets = this.context.getAssets();
            getDirectoryListing(assets, "", 0);
            getDirectoryListing(assets, "", this.apkCount);
        }
    }

    public void closeFileAndroid(NvAPKFile nvAPKFile) {
        try {
            nvAPKFile.is.close();
        } catch (IOException e) {
        }
        nvAPKFile.data = new byte[0];
        nvAPKFile.is = null;
    }

    public int getDirectoryListing(AssetManager assetManager, String str, int i) {
        try {
            if (this.apkFiles == null && i > 0) {
                this.apkFiles = new String[i];
            }
            String[] list = assetManager.list(str);
            if (list.length == 0) {
                if (i > 0) {
                    AddAssetFile(str);
                } else {
                    this.apkCount++;
                }
            } else if (i == 0) {
            }
            for (int i2 = 0; i2 < list.length; i2++) {
                if (list[i2].indexOf(46) == -1) {
                    getDirectoryListing(assetManager, str.length() > 0 ? str + "/" + list[i2] : list[i2], i);
                } else if (i > 0) {
                    AddAssetFile(str.length() > 0 ? str + "/" + list[i2] : list[i2]);
                } else {
                    this.apkCount++;
                }
            }
        } catch (Exception e) {
            System.out.println("ERROR: getDirectoryListing " + e.getMessage());
        }
        return 0;
    }

    public NvAPKFile openFileAndroid(String str) {
        if (!this.hasAPKFiles) {
            this.apkCount = 0;
            this.apkFiles = null;
            GetAssetList();
            this.hasAPKFiles = true;
        }
        int findInAPKFiles = findInAPKFiles(str);
        if (findInAPKFiles == -1) {
            return null;
        }
        NvAPKFile nvAPKFile = new NvAPKFile();
        nvAPKFile.is = null;
        nvAPKFile.length = 0;
        nvAPKFile.position = 0;
        nvAPKFile.bufferSize = 0;
        try {
            nvAPKFile.is = this.context.getAssets().open(this.apkFiles[findInAPKFiles]);
            nvAPKFile.length = nvAPKFile.is.available();
            int MODE_READ_ONLY = 268435456;
            nvAPKFile.is.mark(MODE_READ_ONLY);
            nvAPKFile.bufferSize = 1024;
            nvAPKFile.data = new byte[nvAPKFile.bufferSize];
            return nvAPKFile;
        } catch (Exception e) {
            return null;
        }
    }

    public void readFileAndroid(NvAPKFile nvAPKFile, int i) {
        if (i > nvAPKFile.bufferSize) {
            nvAPKFile.data = new byte[i];
            nvAPKFile.bufferSize = i;
        }
        try {
            nvAPKFile.is.read(nvAPKFile.data, 0, i);
            nvAPKFile.position += i;
        } catch (IOException e) {
        }
    }

    public long seekFileAndroid(NvAPKFile nvAPKFile, int i) {
        long j = 0;
        int i2 = 128;
        long j2 = 0;
        try {
            nvAPKFile.is.reset();
            while (i > 0 && i2 > 0) {
                try {
                    j2 = nvAPKFile.is.skip((long) i);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                j += j2;
                i = (int) (((long) i) - j2);
                i2--;
            }
        } catch (IOException e2) {
        }
        nvAPKFile.position = (int) j;
        return j;
    }

    public void setContext(Context context2) {
        this.context = context2;
    }
}
