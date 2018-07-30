package com.zeroingin.autoinputbootpin;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {

//    String filePath = "/data/AutoInputBootPIN/config";
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {"android.permission.WRITE_EXTERNAL_STORAGE" };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        verifyStoragePermissions(this);
        //当前应用的代码执行目录
        upgradeRootPermission(getPackageCodePath());
    }

    EditText etPIN1;
    EditText etPIN2;
    EditText etPIN3;
    EditText etPIN_Time_Interval;
    Button btApply;
    private void initViews() {
        etPIN1 = findViewById(R.id.textInputEditText_PIN1);
        etPIN2 = findViewById(R.id.textInputEditText_PIN2);
        etPIN3 = findViewById(R.id.textInputEditText_PIN3);
        etPIN_Time_Interval = findViewById(R.id.textInputEditText_TimeInterval);
        btApply = findViewById(R.id.bt_Apply);
        btApply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(etPIN1.getText().toString().equals("") && etPIN2.getText().toString().equals("") && etPIN3.getText().toString().equals("")){
                    Toast.makeText(MainActivity.this,R.string.PIN_is_empty, Toast.LENGTH_SHORT).show();
                }
                if(etPIN_Time_Interval.getText().toString().equals(""))
                    etPIN_Time_Interval.setText("2");

                //用引号包括起来作为一行整体
                String config = String.format("\"PIN1=%s\\nPIN2=%s\\nPIN3=%s\\nTimeInterval=%s\"",etPIN1.getText().toString()
                        ,etPIN2.getText().toString(),etPIN3.getText().toString(),etPIN_Time_Interval.getText().toString());
                saveConfig(config);
            }
        });
    }

    private void saveConfig(String config)
    {
        String mkdirCode = "if [ ! -d \"/data/AutoInputBootPIN\" ]; then\n" +
                "        mkdir /data/AutoInputBootPIN\n" +
                "        fi";
        String saveConfigCode = String.format("echo %s>/data/AutoInputBootPIN/config",config);

        if (runRootCommand(mkdirCode)) {
//            Toast.makeText(this, "创建成功目录", Toast.LENGTH_SHORT).show();
            if (runRootCommand(saveConfigCode)) {
                Toast.makeText(this, R.string.apply_success, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.apply_fail, Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, R.string.apply_fail, Toast.LENGTH_SHORT).show();
        }
    }


    public boolean runRootCommand(String command) {
        Process process = null;
        DataOutputStream dataOutputStream = null;
        DataInputStream dataInputStream = null;
        StringBuffer wifiConf = new StringBuffer();
        try {
            process = Runtime.getRuntime().exec("su");
            dataOutputStream = new DataOutputStream(process.getOutputStream());
            dataInputStream = new DataInputStream(process.getInputStream());
            dataOutputStream
                    .writeBytes(command+"\n");
            dataOutputStream.writeBytes("exit\n");
            dataOutputStream.flush();
            InputStreamReader inputStreamReader = new InputStreamReader(
                    dataInputStream, "UTF-8");
            BufferedReader bufferedReader = new BufferedReader(
                    inputStreamReader);
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                wifiConf.append(line);
            }
            bufferedReader.close();
            inputStreamReader.close();
            process.waitFor();
            Log.d("shell命令执行结果：",process.exitValue()+"");
            if (process.exitValue() == 0)
                return true;
            else
                return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (dataOutputStream != null) {
                    dataOutputStream.close();
                }
                if (dataInputStream != null) {
                    dataInputStream.close();
                }
                process.destroy();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean upgradeRootPermission(String pkgCodePath) {
        Process process = null;
        DataOutputStream os = null;
        try {
            String cmd="chmod 777 " + pkgCodePath;
            process = Runtime.getRuntime().exec("su"); //切换到root帐号
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(cmd + "\n");
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();
        } catch (Exception e) {
            return false;
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
                process.destroy();
            } catch (Exception e) {
            }
        }
        return true;
    }

    public static void verifyStoragePermissions(Activity activity) {
        try {
            //检测是否有写的权限
            int permission = ActivityCompat.checkSelfPermission(activity,
                    "android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE,REQUEST_EXTERNAL_STORAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
