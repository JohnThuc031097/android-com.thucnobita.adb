package com.thucnobita.adb.views;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.app.AlertDialog;
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
    private Button btnADBShellDisconnect;
    private TextView txtOutput;

    private MainViewModel mainViewModel;
    private String port = "5555";
    private String pairCode = "123456";
    private boolean connected = false;
    private boolean runFirst = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);

        btnADBShellConnect = findViewById(R.id.btnADBShellConnect);
        btnADBShellDisconnect = findViewById(R.id.btnADBShellDisconnect);
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

    private void init(){
        lockBtn(btnADBShellConnect, false);
        lockBtn(btnADBShellDisconnect, true);

        btnADBShellConnect.setOnClickListener(v -> {
            lockBtn(btnADBShellConnect, false);
            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.R){
                runOnUiThread(() -> {
                    txtOutput.setText(null);
                    mainViewModel.connect();
                });
            }else{
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
                            runOnUiThread(() -> {
                                mainViewModel.connect(port, pairCode);
                            });
                        })
                        .setNegativeButton("Cancel", null)
                        .show();

            }
            lockBtn(btnADBShellConnect, true);
        });
        btnADBShellDisconnect.setOnClickListener(v -> {
            lockBtn(btnADBShellDisconnect, false);
            runOnUiThread(() -> {
                mainViewModel.disconnect();
            });
            lockBtn(btnADBShellDisconnect, true);
        });

        mainViewModel.watchConnect().observe(this, isConnected -> {
            connected = isConnected;
            if(connected){
                if(!runFirst){
                    runFirst = true;
                }
                runOnUiThread(()->{
                    Log.d(TAG, "Start UIAutomator");
                    String instrClass = "com.thucnobita.autoapp.MainTest";
                    String instrPackage = "com.thucnobita.autoapp.test";
                    mainViewModel.shell("am instrument -w -r -e debug false -e class " +
                            instrClass +
                            " \\" + instrPackage +
                            "/androidx.test.runner.AndroidJUnitRunner");
                });
                Toast.makeText(this, "ADB connected", Toast.LENGTH_SHORT).show();
                lockBtn(btnADBShellConnect, true);
                lockBtn(btnADBShellDisconnect, false);
            }else{
                Toast.makeText(this, "ADB disconnected", Toast.LENGTH_SHORT).show();
                lockBtn(btnADBShellConnect, false);
                lockBtn(btnADBShellDisconnect, true);
            }
        });
        mainViewModel.watchOutputText().observe(this, outputText -> {
            runOnUiThread(() -> {
                String result = txtOutput.getText().toString() + "\n" + outputText + "\n";
//                if(outputText.toString().indexOf("INSTRUMENTATION_STATUS: test=testUIAutomatorStub") > 0 &&
//                        outputText.toString().indexOf("INSTRUMENTATION_STATUS_CODE: 1") > 0){
//                    try {
//
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                        result += e + "\n";
//                    }
//                }
                txtOutput.setText(result);
            });
        });
    }

    private void lockBtn(Button btn, boolean status){
        runOnUiThread(() ->{
            btn.setEnabled(!status);
            if(!status){
                btn.setBackgroundColor(Color.parseColor("#3F51B5"));
            }else{
                btn.setBackgroundColor(Color.parseColor("#D5D6DF"));
            }
        });
    }

}