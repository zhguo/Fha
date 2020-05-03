package com.fate.findhookpoint;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MyService extends Service {
    private static final String FHA_OPEN = "./data/local/tmp/fha";

    private static final String FHA_CLOSE = "kill -9";

    private static final String PS = "ps -ef|grep fha|grep -v grep";

    private static int pid = -1;

    private static String res = "";

    private static final String TAG = "Fate";

    private static boolean mHaveRoot;

    public MyService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        haveRoot();
        if (!isInstallfha()) {
            releaseFha();
        }
        new Thread(new Runnable() {
            @Override
            public void run() {

                openFha();
            }
        }).start();
        if (!isFhaOpen()) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


    }

    /**
     * 判断机器Android是否已经root，即是否获取root权限
     */
    public static boolean haveRoot() {
        if (!mHaveRoot) {
            int ret = execRootCmdSilent("echo test"); // 通过执行测试命令来检测
            if (ret != -1) {
                Log.i(TAG, "have root!");
                mHaveRoot = true;
            } else {
                Log.i(TAG, "not root!");
            }
        } else {
            Log.i(TAG, "mHaveRoot = true, have root!");
        }
        return mHaveRoot;
    }

    /**
     * 关闭fha
     */
    public void closeFha() {
        execRootCmd(FHA_CLOSE + " " + pid);
        Toast.makeText(getApplicationContext(), "断开成功!!", Toast.LENGTH_SHORT).show();
        pid = -1;
    }

    /**
     * 开启fha
     *
     * @param
     */
    public void openFha() {
        execRootCmd(FHA_OPEN);
    }

    /**
     * 释放fha文件
     */
    public void releaseFha() {
        InputStream is = null;
        try {
            is = getResources().getAssets().open("fha");
            int length = is.available();
            byte[] buffer = new byte[length];
            is.read(buffer);
            is.close();
            FileOutputStream out = new FileOutputStream(new File("/sdcard/fha"));
            out.write(buffer);
            out.close();
            moveFileToSystem("/sdcard/fha", "/data/local/tmp/fha");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //移动文件
    public static void moveFileToSystem(String filePath, String targetPath) {
        execRootCmd("mv  " + filePath + " " + targetPath);
        String cmd = "chmod 777 " + targetPath;
        execRootCmd(cmd);
    }

    /**
     * 是否安装fha
     *
     * @return
     */
    public boolean isInstallfha() {
        File file = new File("/data/local/tmp/fha");
        return file.exists();
    }

    /**
     * fha后台是否已开启
     *
     * @return
     */
    public boolean isFhaOpen() {
        res = execRootCmd(PS);
        if (!res.isEmpty() && res != "") {
            List<String> ss = new ArrayList<String>();
            for (String sss : res.replaceAll("[^0-9]", ",").split(",")) {
                if (sss.length() > 0)
                    ss.add(sss);
            }
            pid = Integer.parseInt(ss.get(0));//获取pid
            return res.contains("fha");
        } else {
            return false;
        }
    }

    /**
     * 执行命令并且输出结果
     */
    public static String execRootCmd(String cmd) {
        String result = "";
        DataOutputStream dos = null;
        DataInputStream dis = null;
        try {
            java.lang.Process p = Runtime.getRuntime().exec("su");// 经过Root处理的android系统即有su命令
            dos = new DataOutputStream(p.getOutputStream());
            dis = new DataInputStream(p.getInputStream());

            Log.i(TAG, cmd);
            dos.writeBytes(cmd + "\n");
            dos.flush();
            dos.writeBytes("exit\n");
            dos.flush();
            String line = null;
            while ((line = dis.readLine()) != null) {
                Log.d("result", line);
                result += line;
            }
            p.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (dos != null) {
                try {
                    dos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (dis != null) {
                try {
                    dis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    /**
     * 执行命令但不关注结果输出
     */
    public static int execRootCmdSilent(String cmd) {
        int result = -1;
        DataOutputStream dos = null;

        try {
            java.lang.Process p = Runtime.getRuntime().exec("su");
            dos = new DataOutputStream(p.getOutputStream());

            Log.i(TAG, cmd);
            dos.writeBytes(cmd + "\n");
            dos.flush();
            dos.writeBytes("exit\n");
            dos.flush();
            p.waitFor();
            result = p.exitValue();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (dos != null) {
                try {
                    dos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
