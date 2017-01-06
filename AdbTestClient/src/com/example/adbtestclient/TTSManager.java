package com.example.adbtestclient;

import java.util.concurrent.LinkedBlockingQueue;

import android.content.Context;
import android.media.AudioManager;
import android.util.Log;

import cn.yunzhisheng.tts.offline.TTSPlayerListener;
import cn.yunzhisheng.tts.offline.basic.ITTSControl;
import cn.yunzhisheng.tts.offline.basic.TTSFactory;
import cn.yunzhisheng.tts.offline.common.USCError;

public class TTSManager implements TTSPlayerListener {
	private static final String TAG = "TTSManager";
	private Context mContext = null;
	private static final Object mLock = new Object();
	private static TTSManager mInstance = null;
	private LinkedBlockingQueue<String> mSpeakTextQueue = null;
	//public static final String appKey = "zgbdpwpgurrmaigrnkzfm4tyyke3zn3753lyjwiy";
	public static final String appKey = "_appKey_";
	//public static final String  secret = "f7982c656b1296bf94dad4c4b7c5f5c2";
	private ITTSControl mTTSPlayer = null;

	static{
		System.loadLibrary("yzstts");
	}
	
	public static TTSManager getInstance(){
		if(mInstance == null){
			synchronized (mLock) {
				if(mInstance == null){
					mInstance = new TTSManager();
				}
			}
		}
		return mInstance;
	}
	private TTSManager(){
		mContext = AdbApplication.getInstance();
		mSpeakTextQueue = new LinkedBlockingQueue<String>();
		init();
	}

	public void onDestory(){
		if(mTTSPlayer != null){
			mTTSPlayer.release();
			mTTSPlayer = null;
		}
		mInstance = null;
	}
	public synchronized  void textToSpeak(String text){
		Log.i(TAG, "text to spak text = " + text + "mSpeakTextQueue.size() = " + mSpeakTextQueue.size());
		try {
			if(mSpeakTextQueue.size() == 0){
				mSpeakTextQueue.put(text);
				mTTSPlayer.play(text);
			}else{
				if(mSpeakTextQueue.contains(text)){
					mSpeakTextQueue.remove(text);
				}
				mSpeakTextQueue.put(text);
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void clearSpeakText(){
		if(mTTSPlayer != null){
			mTTSPlayer.stop();
		}
		mSpeakTextQueue.clear();
	}
	private void init() {
		mTTSPlayer = TTSFactory.createTTSControl(mContext, appKey);// 初始化语音合成对象
		mTTSPlayer.setTTSListener(this);// 设置回调监听
		mTTSPlayer.setStreamType(AudioManager.STREAM_RING);//设置音频流
		mTTSPlayer.setVoiceSpeed(2.5f);//设置播报语速,播报语速，数值范围 0.1~2.5 默认为 1.0
		mTTSPlayer.setVoicePitch(1.1f);//设置播报音高,调节音高，数值范围 0.9～1.1 默认为 1.0
		mTTSPlayer.init();// 初始化合成引擎
	}
	@Override
	public void onBuffer() {

	}

	@Override
	public void onPlayBegin() {

	}

	@Override
	public void onCancel() {

	}

	@Override
	public void onError(USCError uscError) {
		Log.i(TAG,"tts error = "+uscError);
	}

	@Override
	public void onPlayEnd() {
		mSpeakTextQueue.poll();
		String text = mSpeakTextQueue.peek();
		Log.i(TAG,"onSpeechFinish text = "+text);
		if(text != null){
			mTTSPlayer.play(text);
		}
	}

	@Override
	public void onInitFinish() {

	}
}
