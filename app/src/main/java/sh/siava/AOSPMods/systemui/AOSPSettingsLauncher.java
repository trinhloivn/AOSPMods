package sh.siava.AOSPMods.systemui;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.IXposedModPack;

public class AOSPSettingsLauncher implements IXposedModPack {
    private static final String listenPackage = "com.android.systemui";

    private static Object activityStarter = null;
    @Override
    public void updatePrefs() {

    }

    @Override
    public String getListenPack() {
        return listenPackage;
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if(!lpparam.packageName.equals(listenPackage)) return;

        Class FooterActionsControllerClass = XposedHelpers.findClass("com.android.systemui.qs.FooterActionsController", lpparam.classLoader);

        View.OnLongClickListener listener = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                try {
                    Context context = v.getContext();
                    Intent launchInent = context.getPackageManager().getLaunchIntentForPackage("sh.siava.AOSPMods");
                    XposedHelpers.callMethod(activityStarter, "startActivity", launchInent, true, null);
                }catch(Exception ignored){}
                XposedBridge.log("on new method");
                return true;
            }
        };

        XposedBridge.hookAllMethods(FooterActionsControllerClass,
                "onViewAttached", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        Object settingsButton = XposedHelpers.getObjectField(param.thisObject, "settingsButton");
                        activityStarter = XposedHelpers.getObjectField(param.thisObject, "activityStarter");
                        XposedHelpers.callMethod(settingsButton, "setOnLongClickListener", listener);
                    }
                });
    }
}