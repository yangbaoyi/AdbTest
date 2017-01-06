package com.android.adb;

public class OptionsUtil {
	public static final String FUNCTION_BT = "bt";
	public static final String[] OPTIONS_BT = {"true","false"};

	public static boolean checkAvailable(String function,String option){
		boolean isAvailable = false;
		if(FUNCTION_BT.equals(function) && checkItem(option,OPTIONS_BT)){
			return true;
		}
		return isAvailable;
	}
	
	private static boolean checkItem(String option,String[] options){
		for(int i = 0 ; i < options.length;i++){
			if(options[i].equals(option)){
				return true;
			}
		}
		return false;
	}
	
}
