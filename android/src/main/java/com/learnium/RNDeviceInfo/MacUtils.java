package com.learnium.RNDeviceInfo;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class MacUtils {
    public static String getMac(Context context) {
        String mac = getMacAddressByWifiInfo(context);
        if (mac == null || mac.replaceAll(":", "").equals("020000000000")) {
            mac = getEtherMacByInterface();
        }
        if (mac == null || mac.replaceAll(":", "").equals("020000000000")) {
            mac = getWifiMacByInterface();
        }
        if (mac == null || mac.replaceAll(":", "").equals("020000000000")) {
            mac = getMacAddress(context);
        }
        if (mac == null || mac.replaceAll(":", "").equals("020000000000")) {
            mac = getWifiMacByFile();
        }
        if (mac == null || mac.replaceAll(":", "").equals("020000000000")) {
            mac = getEthernetMacByFile();
        }
        return mac != null && mac.replaceAll(":", "").equals("020000000000") ? null : mac;
    }

    /**
     * 使⽤ WifiManager 获取 MAC 地址。
     *
     * @param context 应⽤程序上下⽂
     */
    public static String getMacAddressByWifiInfo(Context context) {
        try {
            WifiManager wifi = (WifiManager)
                    context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifi != null) {
                WifiInfo info = wifi.getConnectionInfo();
                if (info != null) {
                    return info.getMacAddress();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 通过检查 "eth0" 接⼝获取有线 MAC 地址。
     */
    public static String getEtherMacByInterface() {
        try {
            Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
            while (nis.hasMoreElements()) {
                NetworkInterface ni = nis.nextElement();
                if (ni == null || !ni.getName().equalsIgnoreCase("eth0")) continue;
                byte[] macBytes = ni.getHardwareAddress();
                if (macBytes != null && macBytes.length > 0) {
                    StringBuilder sb = new StringBuilder();
                    for (byte b : macBytes) {
                        sb.append(String.format("%02x:", b));
                    }
                    return sb.substring(0, sb.length() - 1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 通过检查 "wlan0" 接⼝获取 WiFi MAC 地址。
     */
    public static String getWifiMacByInterface() {
        try {
            Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
            while (nis.hasMoreElements()) {
                NetworkInterface ni = nis.nextElement();
                if (ni == null || !ni.getName().equalsIgnoreCase("wlan0")) continue;
                byte[] macBytes = ni.getHardwareAddress();
                if (macBytes != null && macBytes.length > 0) {
                    StringBuilder sb = new StringBuilder();
                    for (byte b : macBytes) {
                        sb.append(String.format("%02x:", b));
                    }
                    return sb.substring(0, sb.length() - 1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 通过读取系统⽂件获取 WiFi MAC 地址。
     * 注意：需要 ShellUtils 或类似⼯具来执⾏ Shell 命令。
     */
    public static String getWifiMacByFile() {
        ShellUtils.CommandResult result = ShellUtils.execCommand("cat/sys/class/net/wlan0/address", false);
        if (result.result == 0) {
            String address = result.successMsg;
            if (address != null && address.length() > 0) {
                return address;
            }
        }
        return null;
    }

    /**
     * 通过读取系统⽂件获取 Ethernet MAC 地址。
     * 注意：需要 ShellUtils 或类似⼯具来执⾏ Shell 命令。
     */
    public static String getEthernetMacByFile() {
        ShellUtils.CommandResult result = ShellUtils.execCommand("cat/sys/class/net/eth0/address", false);
        if (result.result == 0) {
            String address = result.successMsg;
            if (address != null && address.length() > 0) {
                return address;
            }
        }
        return null;
    }


    /**
     * Check whether accessing wifi state is permitted
     *
     * @param context
     * @return
     */
    private static boolean isAccessWifiStateAuthorized(Context context) {
        if (PackageManager.PERMISSION_GRANTED == context
                .checkCallingOrSelfPermission("android.permission.ACCESS_WIFI_STATE")) {
            Log.e("----->" + "NetInfoManager", "isAccessWifiStateAuthorized:"
                    + "access wifi state is enabled");
            return true;
        } else
            return false;
    }

    private static String getMacAddress0(Context context) {
        if (isAccessWifiStateAuthorized(context)) {
            WifiManager wifiMgr = (WifiManager) context
                    .getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = null;
            try {
                wifiInfo = wifiMgr.getConnectionInfo();
                return wifiInfo.getMacAddress();
            } catch (Exception e) {
                Log.e("----->" + "NetInfoManager",
                        "getMacAddress0:" + e.toString());
            }

        }
        return "";

    }

    private static String loadReaderAsString(Reader reader) throws Exception {
        StringBuilder builder = new StringBuilder();
        char[] buffer = new char[4096];
        int readLength = reader.read(buffer);
        while (readLength >= 0) {
            builder.append(buffer, 0, readLength);
            readLength = reader.read(buffer);
        }
        return builder.toString();
    }

    private static String loadFileAsString(String fileName) throws Exception {
        FileReader reader = new FileReader(fileName);
        String text = loadReaderAsString(reader);
        reader.close();
        return text;
    }

    /**
     * android 6.0及以上、7.0以下 获取mac地址
     *
     * @param context
     * @return
     */
    public static String getMacAddress(Context context) {

        // 如果是6.0以下，直接通过wifimanager获取
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            String macAddress0 = getMacAddress0(context);
            if (!TextUtils.isEmpty(macAddress0)) {
                return macAddress0;
            }
        }
        String str = "";
        String macSerial = "";
        try {
            Process pp = Runtime.getRuntime().exec(
                    "cat /sys/class/net/wlan0/address");
            InputStreamReader ir = new InputStreamReader(pp.getInputStream());
            LineNumberReader input = new LineNumberReader(ir);
            for (; null != str; ) {
                str = input.readLine();
                if (str != null) {
                    macSerial = str.trim();// 去空格
                    break;
                }
            }
        } catch (Exception ex) {
            Log.e("----->" + "NetInfoManager", "getMacAddress:" + ex.toString());
        }
        if (macSerial == null || "".equals(macSerial)) {
            try {
                return loadFileAsString("/sys/class/net/eth0/address")
                        .toUpperCase().substring(0, 17);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("----->" + "NetInfoManager",
                        "getMacAddress:" + e.toString());
            }

        }
        return macSerial;
    }
}
