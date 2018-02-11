package com.anas.wallpapers.utils;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;

import java.util.ArrayList;

public class PermissionHelper {
    private static int REQUEST_CODE = 1;

    private static String[] STORAGE_PERMISSIONS = {
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    public static boolean checkPermissions(final Activity activity) {
        boolean isPermissionGranted = true;
        ArrayList<String> permissionList = new ArrayList<>();
        for (String permission : STORAGE_PERMISSIONS) {
            if (PackageManager.PERMISSION_GRANTED != activity.checkSelfPermission(permission)) {
                permissionList.add(permission);
                isPermissionGranted = false;
            }
        }

        if (!isPermissionGranted) {
            String[] permissionArray = new String[permissionList.size()];
            permissionList.toArray(permissionArray);
            activity.requestPermissions(permissionArray, REQUEST_CODE);
        }

        return isPermissionGranted;
    }

    public static boolean checkPermissionResult(String[] permissions, int[] grantResults) {
        if (permissions == null || grantResults == null || permissions.length == 0
                || grantResults.length == 0) {
            return false;
        }

        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
}
