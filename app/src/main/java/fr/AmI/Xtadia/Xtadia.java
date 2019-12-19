package fr.AmI.Xtadia;

import android.net.Network;
import android.net.NetworkCapabilities;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import static de.robv.android.xposed.XC_MethodReplacement.returnConstant;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

public class Xtadia implements IXposedHookLoadPackage {
    private boolean DEBUG_MODE = false;
    private LoadPackageParam lpparam;

    //Create and store a single NetworkCapabilities object that we will always send to the hooked app
    public static NetworkCapabilities fakeWifiCapabilities = CreateFakeWifiNetworkCapabilities();

    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals("com.google.stadia.android"))
            return;
        this.lpparam = lpparam;
        if (DEBUG_MODE) XposedBridge.log("Xtadia - Hooking app !");

        HookPhoneModel();

        HookConnectionType();

    }

    void HookPhoneModel() {

        Class<?> buildClass = XposedHelpers.findClass("android.os.Build", lpparam.classLoader);

        if (DEBUG_MODE) XposedBridge.log("Hook - MANUFACTURER = Google");
        XposedHelpers.setStaticObjectField(buildClass, "MANUFACTURER", "Google");

        if (DEBUG_MODE) XposedBridge.log("Hook - MODEL = Pixel 4");
        XposedHelpers.setStaticObjectField(buildClass, "MODEL", "Pixel 4");
    }

    void HookConnectionType() {

        //When the app request a network's capabilities, we always return our fake capabilities object.
        findAndHookMethod("android.net.ConnectivityManager", lpparam.classLoader,
                "getNetworkCapabilities", Network.class, returnConstant(fakeWifiCapabilities));

        //In older Android versions, other methods (now deprecated) were used to query the connection type.
        //The outdated "Fake Wifi Connections" module shows quite a few of these, but they are not used here.
    }


    //Creates a fake NetworkCapabilities object, corresponding to a wifi network with usual (unlimited) capabilities.
    static NetworkCapabilities CreateFakeWifiNetworkCapabilities() {

        NetworkCapabilities nc = (NetworkCapabilities) XposedHelpers.newInstance(NetworkCapabilities.class);

        int[] transportTypes = new int[]{NetworkCapabilities.TRANSPORT_WIFI};

        int[] capabilities = new int[]{NetworkCapabilities.NET_CAPABILITY_NOT_METERED,
                NetworkCapabilities.NET_CAPABILITY_NOT_METERED,
                NetworkCapabilities.NET_CAPABILITY_INTERNET,
                NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED,
                NetworkCapabilities.NET_CAPABILITY_TRUSTED,
                NetworkCapabilities.NET_CAPABILITY_NOT_VPN,
                NetworkCapabilities.NET_CAPABILITY_VALIDATED
                /* Seemingly unneeded capabilities, + available only starting with SDK 28.
                NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING,
                NetworkCapabilities.NET_CAPABILITY_FOREGROUND,
                NetworkCapabilities.NET_CAPABILITY_NOT_CONGESTED,
                NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED,*/
        };
        for (int transportType : transportTypes) {
            XposedHelpers.callMethod(nc, "addTransportType", transportType);
        }
        for (int capability : capabilities) {
            XposedHelpers.callMethod(nc, "addCapability", capability);
        }

        return nc;
    }

}
