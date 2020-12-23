package dev.megh.gyroscopecontroller;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity {
    TextView textX, textY, textZ,textServer;
    Button btnReset;
    SensorManager sensorManager;
    Sensor sensor;
    ServerService mService;
    boolean mBound = false,initSet=false;
    float initX,initY,initZ;
    float currentX,currentY,currentZ;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        textX = findViewById(R.id.textX);
        textY = findViewById(R.id.textY);
        textZ = findViewById(R.id.textZ);
        textServer = findViewById(R.id.server);
        btnReset = findViewById(R.id.reset);

        btnReset.setOnClickListener((View view)->{
            initX = currentX;
            initY = currentY;
            initZ = currentZ;
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        //Sensor
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        // Bind to ServerService
        Intent intent = new Intent(this, ServerService.class);
        startService(intent);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }


    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            ServerService.LocalBinder binder = (ServerService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            binder.getService().findServer((String text)->{
                runOnUiThread(() -> textServer.setText(text));
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    public void onResume() {
        super.onResume();
        sensorManager.registerListener(gyroListener, sensor, SensorManager.SENSOR_DELAY_NORMAL);
    }
    public void onStop() {
        super.onStop();
        sensorManager.unregisterListener(gyroListener);
        unbindService(connection);
        mBound = false;
    }
    public SensorEventListener gyroListener = new SensorEventListener() {
        public void onAccuracyChanged(Sensor sensor, int acc) {
        }
        public void onSensorChanged(SensorEvent event) {
            currentX = event.values[0];
            currentY = event.values[1];
            currentZ = event.values[2];
            if(!initSet){
                initX = currentX;
                initY = currentY;
                initZ = currentZ;
                initSet = true;
            }
            float deltaX = currentX - initX;
            float deltaY = currentY - initY;
            float deltaZ = currentZ - initZ;
            textX.setText("dX : " +  String.format("%.3f",deltaX) + " rad/s");
            textY.setText("dY : " +  String.format("%.3f",deltaY) + " rad/s");
            textZ.setText("dZ : " +  String.format("%.3f",deltaZ) + " rad/s");
            mService.postData(deltaX,deltaY,deltaZ);
        }
    };

}