/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.adb;

import java.util.Random;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/* Main activity for the adb test program */
public class AdbTestActivity extends Activity {

    private static final String TAG = "AdbTestActivity";
    private final String ACTION_OPTIONS = "action.adb.client.options";
	private final String KEY_OPTIONS = "key_options";
	private final String VALUE_OPTIONS = "value_options";
	private final String CHECK_OPTIONS = "check_options";
    private TextView mLog,stateTv;
    private Button submitBn,reConnectBn,checkBn;
    private ScrollView scrollView;
    private EditText keyEt,valueEt;
    private UsbManager mManager;
    private UsbDevice mDevice;
    private UsbDeviceConnection mDeviceConnection;
    private UsbInterface mInterface;
    private AdbDevice mAdbDevice;

    private static final int MESSAGE_LOG = 1;
    private static final int MESSAGE_DEVICE_ONLINE = 2;
    private static final int MESSAGE_OPTIONS_RESULT = 3;
    private CommandItem currentItem = new CommandItem();
    
    
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.adb);
        mManager = (UsbManager)getSystemService(Context.USB_SERVICE);
        mLog = (TextView)findViewById(R.id.log);
        stateTv = (TextView)findViewById(R.id.options_state);
        keyEt = (EditText)findViewById(R.id.key);
        valueEt = (EditText)findViewById(R.id.value);
        scrollView = (ScrollView)findViewById(R.id.scroll);
        reConnectBn = (Button)findViewById(R.id.reconnect);
        reConnectBn.setText("重置");
        reConnectBn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub 

				mLog.setText("");
				setResult("", true);
//		        // check for existing devices
		        for (UsbDevice device :  mManager.getDeviceList().values()) {
		            UsbInterface intf = findAdbInterface(device);
		            if (setAdbInterface(device, intf)) {
		                break;
		            }
		        }
			}
		});
        checkBn = (Button)findViewById(R.id.check_result);
        checkBn.setText("查看结果");
        checkBn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub 
				handleOptionsResult();
				log("currentItem:key = " + currentItem.key + ",value = " + currentItem.value + "+checkCode = " + currentItem.checkCode);
			}
		});
        submitBn = (Button)findViewById(R.id.submit);
        submitBn.setText("发送数据");
        submitBn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				if(TextUtils.isEmpty(keyEt.getText().toString()) || TextUtils.isEmpty(valueEt.getText().toString())){
					Toast.makeText(getApplicationContext(), "Please input key and value.", Toast.LENGTH_SHORT).show();
					return;
				}
//				for (UsbDevice device :  mManager.getDeviceList().values()) {
//		            UsbInterface intf = findAdbInterface(device);
//		            if (setAdbInterface(device, intf)) {
//		                break;
//		            }
//		        }
				sendData();
			}
		});
        // check for existing devices
        for (UsbDevice device :  mManager.getDeviceList().values()) {
            UsbInterface intf = findAdbInterface(device);
            if (setAdbInterface(device, intf)) {
                break;
            }
        }

        // listen for new devices
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mUsbReceiver, filter);
    }

    private String getCheckCode(){
    	Random rand = new Random();
    	int checkCode = rand.nextInt(1000);
    	log("random checkCode = " + checkCode);
    	return checkCode+"";
    }
    
    protected void sendData() {
		// TODO Auto-generated method stub
		if(mDeviceConnection != null && mDevice != null && mInterface != null && mAdbDevice != null){
			CommandItem temp = new CommandItem();
			temp.key = keyEt.getText().toString();
			temp.value = valueEt.getText().toString();
			temp.checkCode = getCheckCode();
			if(temp.equals(temp,currentItem)){
				log("此命令已经在执行");
				return;
			}
			if(!temp.checkAvailable()){
				setResult("不支持此功能", true);
				return;
			}
			currentItem = temp;
			new Thread(new Runnable() {
				
				@Override
				public void run() {
					// TODO Auto-generated method stub
					mAdbDevice.openSocket(getShellString());
				}
			}).start();
			optionsResultWait(currentItem.key,0);
			setResult("正在发送请求", false);
			
		}
	}
    
	private String getShellString() {
		// TODO Auto-generated method stub
		String shellCmd = "shell:exec am broadcast -a " + ACTION_OPTIONS + 
				" -e " + KEY_OPTIONS + " " + currentItem.key + 
				" -e " + CHECK_OPTIONS + " " + currentItem.checkCode +
				" -e " + VALUE_OPTIONS + " " + currentItem.value;
		return shellCmd;
	}

	@Override
    public void onDestroy() {
        unregisterReceiver(mUsbReceiver);
        setAdbInterface(null, null);
        super.onDestroy();
    }

	public void optionsResultWait(String key,int count) {
		if(mHandler.hasMessages(MESSAGE_OPTIONS_RESULT)){
			mHandler.removeMessages(MESSAGE_OPTIONS_RESULT);
		}
		log("key:" + key + "tryCount:" + count);
		if(count > 15){
			setResult("执行超时。",true);
			return;
		}
        Message m = new Message();
        m.what = MESSAGE_OPTIONS_RESULT;
        m.arg1 = count;
        m.obj = key;
        if(count == 0){
        	mHandler.sendMessageDelayed(m, 2500);
        }else{
        	mHandler.sendMessageDelayed(m, 1000); 
        }
    }
	
    public void log(String s) {
        Message m = Message.obtain(mHandler, MESSAGE_LOG);
        m.obj = s;
        mHandler.sendMessage(m);
    }

    private void appendLog(String text) {
        Rect r = new Rect();
        mLog.getDrawingRect(r);
        int maxLines = r.height() / mLog.getLineHeight() - 1;
        text = mLog.getText() + "\n" + text;

        // see how many lines we have
        int index = text.lastIndexOf('\n');
        int count = 0;
        while (index > 0 && count <= maxLines) {
            count++;
            index = text.lastIndexOf('\n', index - 1);
        }

        // truncate to maxLines
        if (index > 0) {
            text = text.substring(index + 1);
        }
        mLog.setText(text);
        scrollView.post(new Runnable() {  
            
    	   @Override  
    	   public void run() {  
    	    // TODO Auto-generated method stub  
    	    scrollView.fullScroll(ScrollView.FOCUS_DOWN);  
    	   }  
    	  });
    }

    public void deviceOnline(AdbDevice device) {
        Message m = Message.obtain(mHandler, MESSAGE_DEVICE_ONLINE);
        m.obj = device;
        mHandler.sendMessage(m);
    }

    private void handleDeviceOnline(AdbDevice device) {
        log("device online: " + device.getSerial());
//        String shellCmd = "shell:exec am broadcast -a " + ACTION_OPTIONS + 
//				" -e " + KEY_OPTIONS + " " + "bt" + 
//				" -e " + CHECK_OPTIONS + " " + "bt" + 
//				" -e " + VALUE_OPTIONS + " " + "true";
        //device.openSocket("shell:exec getprop | grep shuguo");
        //device.openSocket("shell:exec getprop | grep usb");
        //device.openSocket(shellCmd);
        //device.openSocket("shell:exec setprop aaaaabbbbbcccccdddddeeeeefffff aaaaabbbbbcccccdddddeeeeefffffggggghhhhh");
        
    }

    // Sets the current USB device and interface
    private boolean setAdbInterface(UsbDevice device, UsbInterface intf) {
        if (mDeviceConnection != null) {
            if (mInterface != null) {
                mDeviceConnection.releaseInterface(mInterface);
                mInterface = null;
            }
            mDeviceConnection.close();
            mDevice = null;
            mDeviceConnection = null;
        }

        if (device != null && intf != null) {
            UsbDeviceConnection connection = mManager.openDevice(device);
            if (connection != null) {
                log("open succeeded");
                if (connection.claimInterface(intf, false)) {
                    log("claim interface succeeded");
                    mDevice = device;
                    mDeviceConnection = connection;
                    mInterface = intf;
                    mAdbDevice = new AdbDevice(this, mDeviceConnection, intf);
                    log("call start");
                    mAdbDevice.start();
                    return true;
                } else {
                    log("claim interface failed");
                    connection.close();
                }
            } else {
                log("open failed");
            }
        }

        if (mDeviceConnection == null && mAdbDevice != null) {
            mAdbDevice.stop();
            mAdbDevice = null;
        }
        return false;
    }

    // searches for an adb interface on the given USB device
    static private UsbInterface findAdbInterface(UsbDevice device) {
        Log.d(TAG, "findAdbInterface " + device);
        int count = device.getInterfaceCount();
        for (int i = 0; i < count; i++) {
            UsbInterface intf = device.getInterface(i);
            if (intf.getInterfaceClass() == 255 && intf.getInterfaceSubclass() == 66 &&
                    intf.getInterfaceProtocol() == 1) {
                return intf;
            }
        }
        return null;
    }

    BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                UsbInterface intf = findAdbInterface(device);
                if (intf != null) {
                    log("Found adb interface " + intf);
                    setAdbInterface(device, intf);
                }
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
            	Toast.makeText(getApplicationContext(), "ACTION_USB_DEVICE_DETACHED", 0).show();
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                String deviceName = device.getDeviceName();
                if (mDevice != null && mDevice.equals(deviceName)) {
                    log("adb interface removed");
                    setAdbInterface(null, null);
                }
            }
        }
    };

    public Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_LOG:
                    appendLog((String)msg.obj);
                    checkResult((String)msg.obj);
                    break;
                case MESSAGE_DEVICE_ONLINE:
                    handleDeviceOnline((AdbDevice)msg.obj);
                    break;
                case MESSAGE_OPTIONS_RESULT:
                	Log.i("aaaaaaaaaaaa","count = "+msg.arg2);
                	handleOptionsResult();
                	optionsResultWait((String)msg.obj,msg.arg1 + 1);
                	break;
            }
        }
    };

	protected void handleOptionsResult() {
		// TODO Auto-generated method stub
		//mAdbDevice.openSocket("shell:exec getprop | grep persist.sys.shuguo."+key);
		if(mDeviceConnection != null && mDevice != null && mInterface != null && mAdbDevice != null){
			new Thread(new Runnable() {
				
				@Override
				public void run() {
					// TODO Auto-generated method stub
					mAdbDevice.openSocket("shell:exec getprop | grep persist.sys.shuguo");
				}
			}).start();
		}
		//checkBn.performClick();
	}

	protected void checkResult(String str) {
		// TODO Auto-generated method stub
		Log.i(TAG, str);
		if(TextUtils.isEmpty(str)){
			return;
		}
		//if(!TextUtils.isEmpty(str) && str.contains("[persist.sys.mute.state]: [2]")){
		if(str.contains("[persist.sys.shuguo."+currentItem.key+"]: [success" + currentItem.checkCode)){
			setResult("执行成功",true);
		}
		if(str.contains("[persist.sys.shuguo."+currentItem.key+"]: [running" + currentItem.checkCode)){
			setResult("正在执行...",false);
		}
		if(str.contains("[persist.sys.shuguo."+currentItem.key+"]: [error" + currentItem.checkCode)){
			setResult("执行错误，可能设备不支持此功能。",true);
		}
		if(str.contains("[persist.sys.shuguo."+currentItem.key+"]: [fail" + currentItem.checkCode)){
			setResult("执行失败。",true);
		}
	}
	
	private void setResult(String result,boolean isEnd){
		stateTv.setText(result);
		if(isEnd){
			mHandler.removeMessages(MESSAGE_OPTIONS_RESULT);
			currentItem.clear();
		}
	}
	
	public class CommandItem {
		public String key;
		public String value;
		public String checkCode;
		
		public boolean equals(CommandItem temp1,CommandItem temp2){
			if(temp1.key.equals(temp2.key) && temp1.value.equals(temp2.value)){
				return true;
			}else{
				return false;
			}
		}
		
		public void clear(){
			key = "";
			value = "";
			checkCode = "";
		}
		
		public boolean checkAvailable(){
			return OptionsUtil.checkAvailable(key, value);
		}
	}
	
}


