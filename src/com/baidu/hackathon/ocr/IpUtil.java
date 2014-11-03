package com.baidu.hackathon.ocr;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.TextUtils;

public class IpUtil {

	public static String getIpByWifi(Context cx) {
		WifiManager wifiManager = (WifiManager) cx
				.getSystemService(Context.WIFI_SERVICE);
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		int ipAddress = wifiInfo.getIpAddress();
		return (ipAddress & 0xFF) + "." + ((ipAddress >> 8) & 0xFF) + "."
				+ ((ipAddress >> 16) & 0xFF) + "." + (ipAddress >> 24 & 0xFF);
	}

	public static String getIpByGprs() {
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface
					.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf
						.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress()) {
						return inetAddress.getHostAddress().toString();
					}
				}
			}
		} catch (SocketException ex) {
		}
		return null;

	}

	public static String getIp(Context cx) {
		WifiManager wifiManager = (WifiManager) cx
				.getSystemService(Context.WIFI_SERVICE);
		String ip = null;
		if (wifiManager.isWifiEnabled()) {
			ip = getIpByWifi(cx);
		}

		if (TextUtils.isEmpty(ip)) {
			ip = getIpByGprs();
		}

		return ip;
	}
}
