package com.thucnobita.adb.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;

import com.thucnobita.adb.BuildConfig;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Adb {
    private static final String TAG = "[ADB]";
    private Context _context;
    private static Adb instance;
    private final Object lock = new Object();
    private String _adbPath = "";

    private boolean _ready = false;
    public boolean ready(){
        return _ready;
    }
    private boolean _closed = true;
    public boolean closed(){
        return _closed;
    }

    private Process _process = null;
    public Process getProcess (){ return _process; }

    public static Adb getInstance(@NonNull Context context) {
        if (instance == null) {
            instance = new Adb(context);
        }
        return instance;
    }
    public Adb(@NonNull Context context) {
        _context = context;
        _adbPath = context.getApplicationInfo().nativeLibraryDir + "/libadb.so";
    }

    public void sendToShellProcess(String msg) throws InterruptedException {
        if (_process == null || _process.getOutputStream() == null) return;
        PrintStream printStream = new PrintStream(_process.getOutputStream());
        printStream.println(msg + "\n");
        printStream.flush();
    }

    private Process shell(List<String> command) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(_context.getFilesDir());
        processBuilder.environment().put("HOME", _context.getFilesDir().getPath());
        processBuilder.environment().put("TMPDIR", _context.getCacheDir().getPath());
        return processBuilder.start();
    }

    private Process adb(List<String> command) throws IOException {
        List<String> cloneCommand = new ArrayList<>(command);
        cloneCommand.add(0, _adbPath);
        return shell(cloneCommand);
    }

    private void pair(String port, String pairingCode) throws IOException, InterruptedException {
        Process pairShell = adb(Arrays.asList("pair", "localhost:" + port));
        Thread.sleep(5000);
        PrintStream printStream = new PrintStream(pairShell.getOutputStream());
        printStream.println(pairingCode);
        printStream.flush();
        pairShell.wait(10000);
    }

    public void disconnect() throws IOException, InterruptedException {
        _ready = false;
        _process.destroy();
        adb(Collections.singletonList("disconnect")).waitFor();
        adb(Collections.singletonList("kill-server")).waitFor();
        _context.getFilesDir().deleteOnExit();
        _context.getCacheDir().deleteOnExit();
        _closed = true;
    }

    public boolean connect(String port, String pairingCode) throws IOException, InterruptedException {
        if(!_ready && _closed){
            boolean secureSettingsGranted = _context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED;
            Log.d(TAG, "Enabling wireless debugging");
            if (secureSettingsGranted) {
                Settings.Global.putInt(
                        _context.getContentResolver(),
                        "adb_wifi_enabled",
                        1
                );
                Thread.sleep(3000);
            }
            if(port != null && pairingCode != null){
                Log.d(TAG, "Check pair code");
                pair(port, pairingCode);
                _process = shell(Arrays.asList("sh", "-l"));
            }else{
                Log.d(TAG, "start-server");
                adb(Collections.singletonList("start-server")).waitFor();
                Log.d(TAG, "wait-for-device");
                adb(Collections.singletonList("wait-for-device")).waitFor();
                Log.d(TAG, "Shelling into device");
                if (Build.SUPPORTED_ABIS[0].equals("arm64-v8a")){
                    _process = adb(Arrays.asList("-t", "1", "shell"));
                }else{
                    _process = adb(Collections.singletonList("shell"));
                }
            }
            if (_process == null) {
                Log.d(TAG, "Failed to open shell connection");
                return false;
            }

            sendToShellProcess("alias adb=\"" + _adbPath + "\"");
            sendToShellProcess("pm grant " + BuildConfig.APPLICATION_ID + " android.permission.WRITE_SECURE_SETTINGS");

            Log.d(TAG, "Connected successful!");

            _ready = true;
            _closed = false;
        }
        return true;
    }
}

