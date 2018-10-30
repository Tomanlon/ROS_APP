package com.example.ubuntu.myrosapplication;


import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.jilk.ros.PublishEvent;
import com.jilk.ros.rosbridge.ROSBridgeClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;


import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTouch;
import de.greenrobot.event.EventBus;
import io.vov.vitamio.LibsChecker;
import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.Vitamio;
import io.vov.vitamio.widget.MediaController;

public class Main2Activity extends AppCompatActivity  implements MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnInfoListener {
    private static final String TAG = "Main2Activity";
    private ProgressBar pb;
    private TextView downloadRateView, loadRateView;
    private io.vov.vitamio.widget.VideoView mVideoView;
//    private VideoView mVideoView;
    @Bind(R.id.bt_map)
    Button btmap;
    @Bind(R.id.bt_unsub)
    Button unsub;
    @Bind(R.id.bt_stop)
    Button btstop;
    @Bind(R.id.bt_fwd)
    Button btfwd;
    @Bind(R.id.bt_bck)
    Button btbck;
    @Bind(R.id.bt_lef)
    Button btlef;
    @Bind(R.id.bt_rgt)
    Button btrgt;
    @Bind(R.id.im_map)
    PinchImageView img_View;
    ROSBridgeClient client;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main2);
        //检查Vitamio的库是否正常
        if (!LibsChecker.checkVitamioLibs(this)) {
            return;
        }
        //初始化框架
        Vitamio.isInitialized(getApplicationContext());
        setContentView(R.layout.activity_main2);
        //获取intent，并获取传递进来的URL参数
        Intent intent = getIntent();
//        path = intent.getStringExtra("VideoUrl");
        //初始化绑定相应控件
        pb = (ProgressBar) findViewById(R.id.probar);
        mVideoView = (io.vov.vitamio.widget.VideoView) findViewById(R.id.buffer);
        downloadRateView = (TextView) findViewById(R.id.download_rate);
        loadRateView = (TextView) findViewById(R.id.load_rate);
        mVideoView.setVideoURI(Uri.parse("http://192.168.0.11:8080/stream?topic=/camera/rgb/image_raw&type=vp8"));
        mVideoView.setMediaController(new MediaController(this));
        //获取焦点
        mVideoView.requestFocus();
        //设置状态信息监听
        mVideoView.setOnInfoListener(this);
        //设置数据更新监听
        mVideoView.setOnBufferingUpdateListener(this);
        //进行异步的准备
        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                //播放速率的调节，需要Vitamio 4.0 以上
                mediaPlayer.setPlaybackSpeed(1.0f);
            }
        });

        EventBus.getDefault().register(this);
        ButterKnife.bind(this);
        showTip("Connect To ROS Successfully!");
        Log.d(TAG,"Connect To ROS Successfully!");
        client = ((RCApplication)getApplication()).getRosClient();
//        moveControl();
    }

    /**
     * 数据更新监听的回调函数
     * @param mp      the MediaPlayer the update pertains to
     * @param percent the percentage (0-100) of the buffer that has been filled thus
     */
    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
//        Log.i("onBufferingUpdate",percent+"");
        loadRateView.setText(percent + "%");
    }

    /**
     * 视频状态更新监听的回调函数
     * @param mp    the MediaPlayer the info pertains to.
     * @param what  the type of info or warning.
     *              <ul>
     *              <li>{@link #MEDIA_INFO_VIDEO_TRACK_LAGGING}
     *              <li>{@link #MEDIA_INFO_BUFFERING_START} 缓存开始
     *              <li>{@link #MEDIA_INFO_BUFFERING_END} 缓存结束
     *              <li>{@link #MEDIA_INFO_NOT_SEEKABLE}
     *              <li>{@link #MEDIA_INFO_DOWNLOAD_RATE_CHANGED} 缓存率改变
     *              </ul>
     * @param extra an extra code, specific to the info. Typically implementation
     *              dependant.
     * @return
     */
    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        switch (what) {
            case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                //缓存开始，判断是否在播放，如果在，那就暂停播放，出现等待的响应提示
                if (mVideoView.isPlaying()) {
                    mVideoView.pause();
                    pb.setVisibility(View.VISIBLE);
                    downloadRateView.setText("");
                    loadRateView.setText("");
                    downloadRateView.setVisibility(View.VISIBLE);
                    loadRateView.setVisibility(View.VISIBLE);

                }
                break;
            case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                //缓存结束，视频开始播放，等待的响应相关控件消失
                mVideoView.start();
                pb.setVisibility(View.GONE);
                downloadRateView.setVisibility(View.GONE);
                loadRateView.setVisibility(View.GONE);
                break;
            case MediaPlayer.MEDIA_INFO_DOWNLOAD_RATE_CHANGED:
                //下载速率的显示
                downloadRateView.setText("" + extra + "kb/s" + "  ");
                break;
        }
        return false;
    }

    /**
     * Activity 失去焦点
     */
    @Override
    protected void onPause() {
        super.onPause();
        mVideoView.pause();//暂停
    }

    /**
     * Activity 获得焦点
     */
    @Override
    protected void onResume() {
        super.onResume();
        mVideoView.start();//开始
    }

    /**
     * Activity 停止
     * 停止摧毁后一定要判断是否有初始化，如果初始化后需要关闭释放Vitamio播放器
     * 不然会出现应用崩溃
     */
    @Override
    protected void onStop() {
        super.onStop();
        //判断是否初始化
        if (LibsChecker.checkVitamioLibs(this)) {
            //VideoView停止关闭
            mVideoView.stopPlayback();
        }
    }


    @OnTouch({R.id.bt_fwd, R.id.bt_bck, R.id.bt_lef, R.id.bt_rgt, R.id.bt_stop})
    public boolean Ontouch(View touchview, MotionEvent touchevent){
        switch (touchview.getId()){
            case R.id.bt_fwd:
                switch (touchevent.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        pubCmd_Vel(0.2,0);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        break;
                    case MotionEvent.ACTION_UP:
                        pubCmd_Vel(0,0);
                        break;
                    default:
                        break;
                }
                break;
            case R.id.bt_bck:
                switch (touchevent.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        pubCmd_Vel(-0.2,0);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        break;
                    case MotionEvent.ACTION_UP:
                        pubCmd_Vel(0,0);
                        break;
                    default:
                        break;
                }
                break;
            case R.id.bt_lef:
                switch (touchevent.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        pubCmd_Vel(0,0.4);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        break;
                    case MotionEvent.ACTION_UP:
                        pubCmd_Vel(0,0);
                        break;
                    default:
                        break;
                }
                break;
            case R.id.bt_rgt:
                switch (touchevent.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        pubCmd_Vel(0,-0.4);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        break;
                    case MotionEvent.ACTION_UP:
                        pubCmd_Vel(0,0);
                        break;
                    default:
                        break;
                }
                break;
            case R.id.bt_stop:
                switch (touchevent.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        pubCmd_Vel(0,0);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        break;
                    case MotionEvent.ACTION_UP:
                        break;
                    default:
                        break;
                }
                break;
            default:
                break;
        }
        return true;

    }

    @OnClick({R.id.bt_map, R.id.bt_unsub})
    public void Onclick(View view){
        switch (view.getId()){
            case R.id.bt_map:
                topSub();
                break;
            case R.id.bt_unsub:
                topUnsub();
                break;
            default:
                break;
        }
    }


    //Publish cmd_vel
    public void pubCmd_Vel(final double linearX, final double angularZ){
        client.send("{\"op\":\"publish\",\"topic\":\"/cmd_vel\",\"msg\":{\"linear\":{\"x\":" + linearX + ",\"y\":0,\"z\":0},\"angular\":{\"x\":0,\"y\":0,\"z\":" + angularZ + "}}}");
//        Timer timer = new Timer();
//        TimerTask timerTask = new TimerTask() {
//            @Override
//            public void run() {
//                Log.d(TAG,"mov!");
//                client.send("{\"op\":\"publish\",\"topic\":\"/cmd_vel\",\"msg\":{\"linear\":{\"x\":" + linearX + ",\"y\":0,\"z\":0},\"angular\":{\"x\":0,\"y\":0,\"z\":" + angularZ + "}}}");
//            }
//        };
//        timer.schedule(timerTask,0,500);
    }



    public void topSub(){
        client.send("{\"op\":\"subscribe\",\"topic\":\"" + "/base64_img/map_img" + "\"}");
    }

    public void topUnsub(){
        client.send("{\"op\":\"unsubscribe\",\"topic\":\"" + "/base64_img/map_img" + "\"}");

    }

    public void onEvent(final PublishEvent event){
        if("/base64_img/map_img".equals(event.name)){
            Log.d(TAG,"got map message0!");
            presentMap(event);
            Log.d(TAG,"got map message!");
            return;
        }
    }

    public void presentMap(PublishEvent event){
        boolean isSubscribe = true;
        String filename = "base64map.txt";
        String filename1 = "base64_filtered.txt";
        String base64data = parseJson(event.msg.replaceAll("\\\\",""));
        Log.d(TAG,"start map pross!");
//        while (isSubscribe){
            try {
                String base64map = event.msg;
                String base64_filtered = base64map.replaceAll("\\\\","");
                JSONObject mapjson = new JSONObject(base64_filtered);
                String map_String = mapjson.getString("data");
                final Bitmap bitmap_present = base64ToBitmap(map_String);
//                Bitmap bitmap_present = base64ToBitmap(map_String, 407,333);
                new  Thread(new Thread()){
                    @Override
                    public void run(){
                        Main2Activity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.d(TAG,"start update image ui!");
                                img_View.setImageBitmap(bitmap_present);

                            }
                        });
                    }
                }.start();
                savemap(filename,base64map);
                savemap(filename1,map_String);
            }
            catch(Exception e){
                e.printStackTrace();
            }
//        }

    }

    //save base64map data to .txt file
    public void savemap(String filename, String mapdata){
        File file = exStorageCheck(filename);
        try{
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(mapdata.getBytes());
//            fos.flush();
            fos.close();
        }catch (IOException e){
            e.printStackTrace();
        }

    }




    // convert base64 data to bitmap
    public Bitmap base64ToBitmap(String base64string){
        Log.d(TAG,"start bas64 pross!");
        byte[] data_byte = Base64.decode(base64string,Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(data_byte,0,data_byte.length);
        return bitmap;

    }




    // parse json data and obtain string

    public String parseJson(String json_data){
        String data = null;
        try{
            JSONArray jsonArray = new JSONArray(json_data);
            for(int i = 0; i < jsonArray.length(); i++){
                JSONObject jsonObject  = jsonArray.getJSONObject(i);
                data = jsonObject.getString("data");
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return data;
    }

    private void showTip(final String info) {
        runOnUiThread(new Runnable() {
        @Override
        public void run() {
            Toast.makeText(Main2Activity.this, info,Toast.LENGTH_SHORT).show();
          }
        });
    }

    public File exStorageCheck(String filename){
        File file = null;
        if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
            file = new File(Environment.getExternalStorageDirectory(),filename);
            Log.d(TAG,"create filepath successfully!");
        }else {
            Log.d(TAG,"unable to create filepath");
        }
        return  file;
    }


}
