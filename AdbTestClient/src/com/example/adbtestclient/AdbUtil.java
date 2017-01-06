package com.example.adbtestclient;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

public class AdbUtil {
	private final static String TAG = "AdbTestClient_TAG";
	
	public final static String TYPE_RUNNING = "running";
	public final static String TYPE_ERROR = "error";
	public final static String TYPE_FAIL = "fail";
	public final static String TYPE_SUCCESS = "success";
	
	private final static int MESSAGE_OPTIONS_RESULT = 0;
	
	private static Context mContext = AdbApplication.getInstance();
	private static String checkCode;
	private static String currentState = TYPE_SUCCESS;
	private static Handler mHandler = new Handler(){

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			switch(msg.what){
			case MESSAGE_OPTIONS_RESULT:
            	handleOptionsResult(msg);
            	optionsResultWait(msg,msg.arg1 + 1);
            	vibrate();
            	break;
			}
			super.handleMessage(msg);
		}
		
	};
	private static void handleOptionsResult(Message msg) {
		// TODO Auto-generated method stub
		String key = (String)msg.obj;
		if(key.equals("bt")){
			BluetoothAdapter mBtAdapter = BluetoothAdapter.getDefaultAdapter();
			if(mBtAdapter == null){
				log("handleOptionsResult:BluetoothAdapter is null,Local bluetooth is not available.");
				setResult(key,msg.arg2,TYPE_ERROR);
			}
			if(mBtAdapter.isEnabled() && msg.arg2 == 1 || !mBtAdapter.isEnabled() && msg.arg2 == 0){
				setResult(key, msg.arg2, TYPE_SUCCESS);
			}
		}
		
		
	}
	private static void optionsResultWait(Message msg,int count) {
		mHandler.removeMessages(MESSAGE_OPTIONS_RESULT);
		String key = (String)msg.obj;
		Log.i(TAG, "optionsResultWait count = " + count + ",key = " + key + ",expect = " + msg.arg2);
		if(count >= 30){
			setResult(key,msg.arg2,TYPE_FAIL);
			return;
		}
		if(!currentState.equals(TYPE_RUNNING)){
			return;
		}
        Message m = new Message();
        m.what = MESSAGE_OPTIONS_RESULT;
        m.arg1 = count;
        m.obj = key;
        m.arg2 = msg.arg2;
        mHandler.sendMessageDelayed(m, 500);
    }
	public static void log(String msg) {
		// TODO Auto-generated method stub
		if(true){
			Log.i(TAG, msg);
		}
	}
	public static void toast(String msg) {
		// TODO Auto-generated method stub
		Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
	}
	
	public static void textToSpeak(String string) {
		// TODO Auto-generated method stub
		//TTSManager.getInstance().textToSpeak(string);
		Intent intent = new Intent("com.shuguo.playrecord.action");
		intent.putExtra("record_string", string);
		intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
		mContext.sendBroadcast(intent);
		log("textToSpeak tts: string = " + string);
	}
	
	public static String checkStatus(){
		
		return "";
	}
	
	public static void enableBt(String key,boolean enable) {
		// TODO Auto-generated method stub
		BluetoothAdapter mBtAdapter = BluetoothAdapter.getDefaultAdapter();
		if(mBtAdapter == null){
			log("BluetoothAdapter is null,Local bluetooth is not available.");
			setResult(key,enable ? 1:0,TYPE_ERROR);
		}
		log("bt address:" + mBtAdapter.getAddress() + ",name:" + mBtAdapter.getName());
		if(enable){
			if(!mBtAdapter.isEnabled()){
				mBtAdapter.enable();
			}
		}else{
			if(mBtAdapter.isEnabled()){
				mBtAdapter.disable();
			}
		}
		setResult(key,enable ? 1:0,TYPE_RUNNING);
	}
	
	public static void setResult(String key,int expect,String result) {
		// TODO Auto-generated method stub
		SystemProperties.set("persist.sys.shuguo."+key, result+checkCode);
		currentState = result;
		if(result.equals(TYPE_RUNNING)){
			Message msg = new Message();
			msg.obj = key;
			msg.arg1 = 0;
			msg.arg2 = expect;
			optionsResultWait(msg,0);
		}
		
		if(result.equals(TYPE_RUNNING)){
			if(expect == 1){
				textToSpeak("正在开启蓝牙");
			}else{
				textToSpeak("正在关闭蓝牙");
			}
		} else if(result.equals(TYPE_ERROR)){
				textToSpeak("不支持此功能");
		} else if(result.equals(TYPE_FAIL)){
			if(expect == 1){
				textToSpeak("开启蓝牙失败");
			}else{
				textToSpeak("关闭蓝牙失败");
			}
		} else if(result.equals(TYPE_SUCCESS)){
			if(expect == 1){
				textToSpeak("开启蓝牙成功");
			}else{
				textToSpeak("关闭蓝牙成功");
			}
		}
		
	}
	
	public static void vibrate(){
//		Vibrator vibrator = (Vibrator)mContext.getSystemService(Context.VIBRATOR_SERVICE);  
//        long [] pattern = {100,400};   // 停止 开启 停止 开启   
//        vibrator.vibrate(pattern,-1);  
	}
	public static void setCheckCode(String checkcode) {
		// TODO Auto-generated method stub
		checkCode = checkcode;
	}
}
