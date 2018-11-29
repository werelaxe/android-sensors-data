package com.example.werelaxe.sandbox;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    Thread sendThread;
    SensorData sensorData = new SensorData();

    String serverHost;
    String serverPort;

    TextView accelerometerXTextView;
    TextView accelerometerYTextView;
    TextView accelerometerZTextView;

    TextView rotationXTextView;
    TextView rotationYTextView;
    TextView rotationZTextView;

    TextView magneticXTextView;
    TextView magneticYTextView;
    TextView magneticZTextView;

    EditText portTextField;
    EditText hostTextField;

    boolean needReconnect = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        accelerometerXTextView = findViewById(R.id.accelerometerXTextView);
        accelerometerYTextView = findViewById(R.id.accelerometerYTextView);
        accelerometerZTextView = findViewById(R.id.accelerometerZTextView);

        rotationXTextView = findViewById(R.id.rotationXTextView);
        rotationYTextView = findViewById(R.id.rotationYTextView);
        rotationZTextView = findViewById(R.id.rotationZTextView);

        magneticXTextView = findViewById(R.id.magneticXTextView);
        magneticYTextView = findViewById(R.id.magneticYTextView);
        magneticZTextView = findViewById(R.id.magneticZTextView);

        portTextField = findViewById(R.id.portTextField);
        hostTextField = findViewById(R.id.hostTextFIeld);

        serverHost = hostTextField.getText().toString();
        serverPort = portTextField.getText().toString();

        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorManager.registerListener(MainActivity.this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(MainActivity.this, sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR), SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(MainActivity.this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL);

        portTextField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                serverPort = s.toString();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        hostTextField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                serverHost = s.toString();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        sendThread = new Thread(new SendThread());
        sendThread.start();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        String xValue = "X: " + Float.toString(event.values[0]);
        String yValue = "Y: " + Float.toString(event.values[1]);
        String zValue = "Z: " + Float.toString(event.values[2]);

        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            accelerometerXTextView.setText(xValue);
            accelerometerYTextView.setText(yValue);
            accelerometerZTextView.setText(zValue);
            sensorData.accelerometerValues = event.values;
        } else if (sensor.getType() == Sensor.TYPE_GAME_ROTATION_VECTOR) {
            rotationXTextView.setText(xValue);
            rotationYTextView.setText(yValue);
            rotationZTextView.setText(zValue);
            sensorData.rotationValues = event.values;
        } else if (sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            magneticXTextView.setText(xValue);
            magneticYTextView.setText(yValue);
            magneticZTextView.setText(zValue);
            sensorData.magneticValues = event.values;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    public void reconnect(View view) {
        needReconnect = true;
    }

    class SendThread implements Runnable {
        TextView connectionStatusTextView;
        TextView logTextView;

        void setConnectedStatus() {
            connectionStatusTextView.setText(R.string.connectedStatus);
            connectionStatusTextView.setBackgroundColor(getResources().getColor(R.color.colorConnected));
        }

        void setDisconnectedStatus() {
            connectionStatusTextView.setText(R.string.disconnectedStatus);
            connectionStatusTextView.setBackgroundColor(getResources().getColor(R.color.colorDisconnected));
        }

        PrintWriter connect(String host, int port) throws IOException {
            InetAddress serverAddr = InetAddress.getByName(host);
            Socket socket = new Socket(serverAddr, port);
            return new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
        }

        @Override
        public void run() {
            connectionStatusTextView = findViewById(R.id.connectionTextView);
            logTextView = findViewById(R.id.logTextView);
            while (hostTextField == null || portTextField == null || !sensorData.isReady()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e1) {}
            }
            while (true) {
                try {
                    PrintWriter out = connect(serverHost, Integer.parseInt(serverPort));
                    needReconnect = false;
                    while (!needReconnect) {
                        if (out.checkError()) {
                            setDisconnectedStatus();
                            out = connect(serverHost, Integer.parseInt(serverPort));
                        } else {
                            setConnectedStatus();
                            logTextView.setText("");
                            out.println(sensorData.serialize());
                            Thread.sleep(1000);
                        }
                    }
                    if (!out.checkError()) {
                        out.close();
                    }
                } catch (Exception e) {
                    setDisconnectedStatus();
                    logTextView.setText(e.toString());
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {}
                }
            }
        }
    }
}
