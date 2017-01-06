package com.example.adbtestclient;

import android.app.Application;

public class AdbApplication extends Application {
	private static AdbApplication instance;
	
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		instance = this;
		super.onCreate();
	}

	public static Application getInstance(){
		return instance;
	}
}
