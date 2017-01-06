package com.example.adbtestclient;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

public class AdbOptionsReceiver extends BroadcastReceiver{
	private final String ACTION_OPTIONS = "action.adb.client.options";
	private final String KEY_OPTIONS = "key_options";
	private final String CHECK_OPTIONS = "check_options";
	private final String VALUE_OPTIONS = "value_options";
	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		String action = intent.getAction();
		if(ACTION_OPTIONS.equals(action)){
			handlerOptions(intent);
		}
	}
	private void handlerOptions(Intent intent) {
		// TODO Auto-generated method stub
		String key = intent.getStringExtra(KEY_OPTIONS);
		String value = intent.getStringExtra(VALUE_OPTIONS);
		String checkCode = intent.getStringExtra(CHECK_OPTIONS);
		AdbUtil.log("AdbOptionsReceiver: key = " + key + ",value = " + value + ",checkCode = " + checkCode);
		AdbUtil.vibrate();
		if(TextUtils.isEmpty(key) || TextUtils.isEmpty(value)){
			AdbUtil.log("AdbOptionsReceiver: return ---key or value is null return.");
			return;
		}
		AdbUtil.setCheckCode(checkCode);
		if(key.equals("bt")){
			if(value.equals("true")){
				AdbUtil.enableBt(key,true);
			}else if(value.equals("false")){
				AdbUtil.enableBt(key,false);
			}else{
				AdbUtil.setResult(key, 0, AdbUtil.TYPE_ERROR);
			}
			AdbUtil.vibrate();
		}else{
			AdbUtil.setResult(key, 0, AdbUtil.TYPE_ERROR);
		}
		AdbUtil.toast("key = " + key + ",value = " + value);
		//AdbUtil.textToSpeak("key = " + key + ",value = " + value);
	}

}
