package com.thucnobita.adb.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.thucnobita.adb.utils.Adb;

import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainViewModel extends AndroidViewModel {
    private static final String TAG = "[VIEWMODEL-MAIN]";
    private Adb adb = null;
    private final Object mLock = new Object();
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    private final MutableLiveData<Boolean> _statusConnect = new MutableLiveData<>();
    private final MutableLiveData<CharSequence> _outputText = new MutableLiveData<>();

    public LiveData<Boolean> watchConnect(){ return _statusConnect; }
    public LiveData<CharSequence> watchOutputText(){ return _outputText; }

    public MainViewModel(@NonNull Application application) {
        super(application);
        adb = Adb.getInstance(getApplication().getApplicationContext());
    }

    public void connect(){
        executor.submit(() ->{
            boolean connected = false;
            try {
                connected = adb.connect(null,null);
            } catch (Exception e) {
                e.printStackTrace();
                _outputText.postValue(e.toString());
            }
            _statusConnect.postValue(connected);
        });
    }

    public void connect(String port, String pairingCode){
        executor.submit(() ->{
            boolean connected = false;
            try {
                connected = adb.connect(port, pairingCode);
            } catch (Exception e) {
                e.printStackTrace();
                _outputText.postValue(e.toString());
            }
            _statusConnect.postValue(connected);
        });
    }

    public void shell(String cmd){
        executor.submit(()->{
           try{
               adb.sendToShellProcess(cmd);
               new Thread(outputGenerator).start();
           }catch (Exception e){
               e.printStackTrace();
               _outputText.postValue(e.toString());
           }
        });
    }

    public void disconnect(){
        executor.submit(()->{
            try{
                adb.disconnect();
            }catch (Exception e){
                e.printStackTrace();
                _outputText.postValue(e.toString());
            }
            _statusConnect.postValue(false);
        });
    }

    private final Runnable outputGenerator = () -> {
        synchronized (mLock){
            try {
                if(adb.ready() && !adb.closed()){
                    Log.d(TAG, "=> Start getErrorStream");
                    InputStream errorStream = adb.getProcess().getErrorStream();
                    synchronized (errorStream){
                        Log.d(TAG, "Waiting for 2s... ");
                        errorStream.wait(2000);
                        Log.d(TAG, "Total size data: " + errorStream.available());
                        if(errorStream.available() > 0){
                            byte[] data = new byte[errorStream.available()];
                            errorStream.read(data);
                            String result = new String(data);
                            Log.d(TAG, "Result: " + result);
                            _outputText.postValue(result);
                        }
                        errorStream.notify();
                    }
                    Log.d(TAG, "=> End getErrorStream");

                    Log.d(TAG, "=> Start getInputStream");
                    InputStream inputStream = adb.getProcess().getInputStream();
                    synchronized (inputStream){
                        Log.d(TAG, "Waiting for 2s... ");
                        inputStream.wait(2000);
                        Log.d(TAG, "Total size data: " + inputStream.available());
                        if(inputStream.available() > 0){
                            byte[] data = new byte[inputStream.available()];
                            inputStream.read(data);
                            String result = new String(data);
                            Log.d(TAG, "Result: " + result);
                            _outputText.postValue(result);
                        }
                        inputStream.notify();
                    }
                    Log.d(TAG, "=> End getInputStream");
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.d(TAG, "Error func outputGenerator():" + e);
                _outputText.postValue(e.toString());
            }
            mLock.notifyAll();
        }
    };

}
