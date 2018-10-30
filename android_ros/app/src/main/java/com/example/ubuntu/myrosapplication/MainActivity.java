package com.example.ubuntu.myrosapplication;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.jilk.ros.rosbridge.ROSBridgeClient;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    @Bind(R.id.et_ip)
    EditText etIP;
    @Bind(R.id.et_port)
    EditText etPort;

    ROSBridgeClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
    }

    //This  function is used for connecting Ros!
    private void connect(String ip, String port){
        client = new ROSBridgeClient("ws://" + ip + ":" + port);
        boolean conneSuss = client.connect();
        if(conneSuss){
            client.setDebug(true);
            ((RCApplication)getApplication()).setRosClient(client);
           startActivity(new Intent(MainActivity.this,Main2Activity.class));
        }else {
            Toast.makeText(MainActivity.this,"Unable To Connect ROS, Make Sure ROSbridge Is Open!",Toast.LENGTH_SHORT);
        }
    }


    @OnClick({R.id.tv_ros,R.id.btn_connect})
    public void OnClick(View view){
        switch (view.getId()){
            case R.id.tv_ros:
                break;
            case R.id.btn_connect:
                String ip = etIP.getText().toString();
                String port = etPort.getText().toString();
                connect(ip, port);
            default:
                break;
        }
    }
}
