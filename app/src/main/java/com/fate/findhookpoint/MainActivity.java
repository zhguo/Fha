package com.fate.findhookpoint;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Fate";

    public Button btnCP;

    private Button btnFindHook;

    private Button btnDump;

    private AlertDialog.Builder builder;

    private AlertDialog alertDialog;

    private List<Process> processes = new ArrayList<>();

    private Process selectProcess;

    private Spinner processSpinner;

    private String selectSoName;

    private ListView addrList;

    public static SocketClient client;
    String[] addres;

    private static final int REQUEST_EXTERNAL_STORAGE = 1;

    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE"};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        Intent intent = new Intent(this, MyService.class);
        // startService(intent);
        verifyStoragePermissions(this);
        bindView();
        initSocket();
    }

    /**
     * 绑定控件
     */
    void bindView() {
        btnCP = (Button) findViewById(R.id.btnCP);
        btnCP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showProcess();
            }
        });
        processSpinner = (Spinner) findViewById(R.id.processSpinner);
        btnFindHook = (Button) findViewById(R.id.btnFindHook);
        btnDump = (Button) findViewById(R.id.btnDump);
        addrList = (ListView) findViewById(R.id.addrList);
        btnFindHook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectProcess != null && selectSoName != null) {
                    sendFindHook(selectProcess.getPackageName(), selectSoName);

                } else {
                    Toast.makeText(getBaseContext(), "请选择进程及目标So", Toast.LENGTH_SHORT).show();
                }
            }
        });
        btnDump.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectProcess != null && selectSoName != null) {
                    sendDump(selectProcess.getPackageName(), selectSoName);
                }
            }
        });
    }


    //然后通过一个函数来申请
    public static void verifyStoragePermissions(Activity activity) {
        try {
            //检测是否有写的权限
            int permission = ActivityCompat.checkSelfPermission(activity,
                    "android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getAppList() {
        @SuppressLint("WrongConstant") UsageStatsManager usm = (UsageStatsManager) getSystemService("usagestats");
        long time = System.currentTimeMillis();
        List<UsageStats> appList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY,
                time - 1000 * 1000, time);
        PackageManager pm = getPackageManager();
        // Return a List of all packages that are installed on the device.
        List<PackageInfo> packages = pm.getInstalledPackages(0);
        if (processes.size() > 0) {
            processes.clear();
        }
        for (PackageInfo packageInfo : packages) {
            // 判断系统/非系统应用
            if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) // 非系统应用
            {
                if (appList != null && appList.size() > 0) {
                    for (UsageStats us : appList) {
                        if (us.getPackageName().equals(packageInfo.packageName)) {
                            Process process = new Process();
                            process.setPackageName(packageInfo.packageName);
                            process.setAppName(packageInfo.applicationInfo.loadLabel(getPackageManager()).toString());
                            process.setIcon(packageInfo.applicationInfo.loadIcon(getPackageManager()));
                            process.setApplicationInfo(packageInfo.applicationInfo);
                            processes.add(process);
                        }
                    }
                }
            } else {
                // 系统应用
            }
        }
    }

    void initSocket() {
        if (client == null) {
            SocketCallBack back = new SocketCallBack() {
                @Override
                public void Print(String info) {
                    showMsg(info);
                }
            };
            client = new SocketClient(back, "localhost", 8688);
            if (client != null) {
                client.start();
            }
        }
    }

    /**
     * 在信息显示区显示信息
     */
    private void showMsg(final String msg) {
        ThreadTool.RunInMainThread(new ThreadTool.ThreadPram() {
            @Override
            public void Function() {
                if (msg.contains("0x")) {
                    addres = msg.split(",");
                    for (int i = 0; i < addres.length; i++) {
                        addres[i] = selectSoName + ":               地址:" + addres[i];
                    }
                    if (addres.length > 0) {
                        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getBaseContext(), android.R.layout.simple_list_item_1, android.R.id.text1, addres);
                        addrList.setAdapter(adapter);
                    }
                } else if(msg.contains("dump")) {
                    Toast.makeText(getBaseContext(), msg, Toast.LENGTH_SHORT).show();
                }

            }
        });
    }


    /**
     * 显示进程
     */
    private void showProcess() {
        builder = new AlertDialog.Builder(this);
        View list_view = LayoutInflater.from(this).inflate(R.layout.list_view, null);
        ListView listView = list_view.findViewById(R.id.appList);
        getAppList();
        final ProcessAdapter processAdapter = new ProcessAdapter();
        listView.setAdapter(processAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectProcess = new Process(processes.get(position));
                addrList.setAdapter(null);
                btnCP.setBackground(selectProcess.getIcon());
                String soPath = selectProcess.getApplicationInfo().nativeLibraryDir;
                File file = new File(soPath);
                final String[] list = file.list();
                if (list != null) {
                    ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getBaseContext(), android.R.layout.simple_spinner_dropdown_item, android.R.id.text1, list);
                    processSpinner.setAdapter(arrayAdapter);
                    processSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            selectSoName = list[position];
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {

                        }
                    });
                } else {
                    Toast.makeText(getBaseContext(), "该应用没有lib目录", Toast.LENGTH_SHORT).show();
                }
                alertDialog.dismiss();
            }
        });
        alertDialog = builder.create();
        alertDialog.setView(list_view);
        alertDialog.show();
    }


    //检测用户是否对本app开启了“Apps with usage access”权限
    private boolean hasPermission() {
        AppOpsManager appOps = (AppOpsManager)
                getSystemService(Context.APP_OPS_SERVICE);
        int mode = 0;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
            mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(), getPackageName());
        }
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    private static final int MY_PERMISSIONS_REQUEST_PACKAGE_USAGE_STATS = 1101;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == MY_PERMISSIONS_REQUEST_PACKAGE_USAGE_STATS) {
            if (!hasPermission()) {
                //若用户未开启权限，则引导用户开启“Apps with usage access”权限
                startActivityForResult(
                        new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS),
                        MY_PERMISSIONS_REQUEST_PACKAGE_USAGE_STATS);
            }
        }
    }

    private void sendMessage(String message) {
        client.Send(message);
    }

    private void sendFindHook(String packageName, String soName) {
        String message = "0\t" + packageName + "\t" + soName;
        sendMessage(message);
    }

    private void sendDump(String packageName, String soName) {
        String message = "1\t" + packageName + "\t" + soName;
        sendMessage(message);

    }

    class ProcessAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return processes.size();
        }

        @Override
        public Object getItem(int position) {
            return processes.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.process_item, null);
            TextView appName = (TextView) itemView.findViewById(R.id.appName);
            ImageView appIcon = (ImageView) itemView.findViewById(R.id.appIcon);
            appName.setText(processes.get(position).getAppName());
            appIcon.setImageDrawable(processes.get(position).getIcon());
            return itemView;
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        client.disconnect();
    }


}
