package com.thucnobita.adb.views;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.thucnobita.adb.R;
import com.thucnobita.adb.viewmodels.MainViewModel;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private Button btnADBShellConnect;
    private TextView txtOutput;

    private final Object mLock = new Object();
    private MainViewModel mainViewModel;
    private String port = "5555";
    private String pairCode = "123456";
    private boolean isRunning = false;
    private boolean connected = false;
    private boolean runFirst = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);
        btnADBShellConnect = findViewById(R.id.btnConnect);
        txtOutput = findViewById(R.id.txtOutput);
        init();
        askPermissions();
    }

    protected void askPermissions() {
        String[] permissions = {
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.WRITE_EXTERNAL_STORAGE"
        };
        int requestCode = 200;
        requestPermissions(permissions, requestCode);
    }

    @SuppressLint("SetTextI18n")
    private void init(){
        btnADBShellConnect.setOnClickListener(v -> {
            runOnUiThread(() ->{
                btnADBShellConnect.setEnabled(false);
                if(btnADBShellConnect.getText().toString().equals("Connect") && !isRunning){
                    isRunning = true;
                    btnADBShellConnect.setText("Disconnect");
                    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.R){
                        txtOutput.setText(null);
                        mainViewModel.connect();
                    }else{
                        synchronized (mLock){
                            View view = getLayoutInflater().inflate(R.layout.dialog_pair_code, null);
                            EditText txtDialogPort = view.findViewById(R.id.txtDialogPort);
                            EditText txtDialogPairCode = view.findViewById(R.id.txtDialogPairCode);
                            new AlertDialog.Builder(this)
                                    .setTitle(R.string.dialog_title_pair_code)
                                    .setView(R.layout.dialog_pair_code)
                                    .setPositiveButton("OK", (dialog, which) -> {
                                        if(txtDialogPort.length() > 0){
                                            port = txtDialogPort.getText().toString();
                                        }
                                        if(txtDialogPairCode.length() > 0){
                                            pairCode = txtDialogPairCode.getText().toString();
                                        }
                                        mainViewModel.connect(port, pairCode);
                                        dialog.dismiss();
                                        mLock.notify();
                                    })
                                    .setNegativeButton("Cancel", (dialog, which) -> {
                                        dialog.cancel();
                                        mLock.notify();
                                    })
                                    .show();
                        }
                    }
                }else {
                    mainViewModel.disconnect();
                    isRunning = false;
                    btnADBShellConnect.setText("Connect");
                }
            });
        });

        mainViewModel.watchConnect().observe(this, isConnected -> {
            connected = isConnected;
            if(connected){
                if(!runFirst){
                    runFirst = true;
                }
                Toast.makeText(this, "ADB connected", Toast.LENGTH_SHORT).show();
                runOnUiThread(() -> {
                    txtOutput.setText(txtOutput.getText().toString() + "\n" + "+ ADB connected");
                    txtOutput.setText(txtOutput.getText().toString() + "\n" + "+ Start ADB shell...");
                });
                Log.d(TAG, "Start UIAutomator");
                String IGClass = "com.thucnobita.autoapp.MainTest";
                String IGPackage = "com.thucnobita.autoapp.test";
                mainViewModel.shell("am instrument -w -r -e debug false -e class " +
                        IGClass +
                        " \\" + IGPackage +
                        "/androidx.test.runner.AndroidJUnitRunner");
                btnADBShellConnect.setBackgroundColor(Color.parseColor("#ffffff"));
                btnADBShellConnect.setEnabled(true);
            }else{
                Toast.makeText(this, "+ ADB disconnected", Toast.LENGTH_SHORT).show();
                runOnUiThread(() -> {
                    txtOutput.setText(txtOutput.getText().toString() + "\n" + "+ ADB disconnected");
                });
                btnADBShellConnect.setBackgroundColor(Color.parseColor("#ffffff"));
                btnADBShellConnect.setEnabled(true);
            }
        });
        mainViewModel.watchOutputText().observe(this, outputText -> {
            runOnUiThread(() -> {
                String result = txtOutput.getText().toString() + "\n" + outputText;
                if(outputText.toString().indexOf("INSTRUMENTATION_STATUS: test=loadTest") > 0 &&
                        outputText.toString().indexOf("INSTRUMENTATION_STATUS_CODE: 1") > 0)
                {
                    txtOutput.setText(txtOutput.getText().toString() + "\n" + "+ ADB connect to com.thucnobita.autoapp Ok");
                }else{
                    txtOutput.setText(txtOutput.getText().toString() + "\n" + "+ ADB connect to com.thucnobita.autoapp Failed");
                    txtOutput.setText(result);
                }
            });
        });
    }

}