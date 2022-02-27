package sh.siava.AOSPMods.systemui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.ImageView;
import android.widget.LinearLayout;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.CircleBatteryDrawable;
import sh.siava.AOSPMods.aModManager;


//TODO: unknown battery symbol / percent text beside icon / update shape upon request / other shapes / dual tone

public class BatteryStyleManager extends aModManager {

    private int frameColor;
    protected int BatteryStyle;
    protected boolean ShowPercent;


    public BatteryStyleManager(XC_LoadPackage.LoadPackageParam lpparam, int BatteryStyle, boolean ShowPercent){
        super(lpparam);
        this.BatteryStyle = BatteryStyle;
        this.ShowPercent = ShowPercent;

        //Xposedbridge.log("BSIAPOSED: Init done");
    }

    public void setBatteryStyle(int BatteryStyle)
    {
        this.BatteryStyle = BatteryStyle;
    }

    @Override
    protected void hookMethods() {
        //Xposedbridge.log("BSIAPOSED: hook start");

        XposedHelpers.findAndHookConstructor("com.android.settingslib.graph.ThemedBatteryDrawable", lpparam.classLoader, Context.class, int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                frameColor = (int) param.args[1];
            }
        });

        //Xposedbridge.log("BSIAPOSED: part 2");

        XposedHelpers.findAndHookConstructor("com.android.systemui.BatteryMeterView", lpparam.classLoader, Context.class, AttributeSet.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Context context = (Context) param.args[0];
                //Resources res = context.getResources();


/*                TypedArray atts = context.obtainStyledAttributes((AttributeSet) param.args[1],
                        R.styleable.BatteryMeterView ,//res.getIntArray(res.getIdentifier("BatteryMeterView", "styleable", context.getPackageName())),
                        (int) param.args[2],
                        0);

                final int frameColor = atts.getColor(res.getIdentifier("BatteryMeterView_frameColor", "styleable", context.getPackageName()),
                        context.getColor(res.getIdentifier("meter_background_color", "color", context.getPackageName())));
*/
                //context.getColor(res.getIdentifier("meter_background_color", "color", context.getPackageName()))

                CircleBatteryDrawable circl = new CircleBatteryDrawable((Context) param.args[0], frameColor);
                circl.setShowPercent(ShowPercent);
                circl.setMeterStyle(BatteryStyle);

                XposedHelpers.setAdditionalInstanceField(param.thisObject, "mCircleDrawable", circl);

                ImageView mBatteryIconView = (ImageView) XposedHelpers.getObjectField(param.thisObject, "mBatteryIconView");

                Drawable mCircleDrawable = (Drawable) XposedHelpers.getAdditionalInstanceField(param.thisObject, "mCircleDrawable");
                mBatteryIconView.setImageDrawable(mCircleDrawable);
                XposedHelpers.setObjectField(param.thisObject, "mBatteryIconView", mBatteryIconView);
            }
        });

        //Xposedbridge.log("BSIAPOSED: part 3");


        XposedHelpers.findAndHookMethod("com.android.systemui.BatteryMeterView", lpparam.classLoader,
                "onBatteryUnknownStateChanged", boolean.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        ImageView mBatteryIconView = (ImageView) XposedHelpers.getObjectField(param.thisObject, "mBatteryIconView");
                        CircleBatteryDrawable mCircleDrawable = (CircleBatteryDrawable) XposedHelpers.getAdditionalInstanceField(param.thisObject, "mCircleDrawable");

                        mCircleDrawable.setMeterStyle(BatteryStyle);

                        mBatteryIconView.setImageDrawable(mCircleDrawable);
                        XposedHelpers.setObjectField(param.thisObject, "mBatteryIconView", mBatteryIconView);

                        //Xposedbridge.log("SIAPOSED: unknown called for no reason!");
                    }
                });

        //Xposedbridge.log("BSIAPOSED: part 4");

        XposedHelpers.findAndHookMethod("com.android.systemui.BatteryMeterView", lpparam.classLoader,
                "scaleBatteryMeterViews", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        ImageView mBatteryIconView = (ImageView) XposedHelpers.getObjectField(param.thisObject, "mBatteryIconView");
                        if (mBatteryIconView == null)
                            param.setResult(null);

                        Context context = (Context) XposedHelpers.callMethod(param.thisObject, "getContext");
                        Resources res = context.getResources();

                        TypedValue typedValue = new TypedValue();

                        res.getValue(res.getIdentifier("status_bar_icon_scale_factor", "dimen", context.getPackageName()), typedValue, true);
                        float iconScaleFactor = typedValue.getFloat();

                        //Xposedbridge.log("SIAPOSED: scalefac " + iconScaleFactor);

                        int batteryHeight = res.getDimensionPixelSize(res.getIdentifier("status_bar_battery_icon_height", "dimen", context.getPackageName()));
                        int batteryWidth = res.getDimensionPixelSize(res.getIdentifier("status_bar_battery_icon_height", "dimen", context.getPackageName()));
                        int marginBottom = res.getDimensionPixelSize(res.getIdentifier("battery_margin_bottom", "dimen", context.getPackageName()));

                        //Xposedbridge.log("SIAPOSED: height " + batteryHeight);

                        LinearLayout.LayoutParams scaledLayoutParams = new LinearLayout.LayoutParams(
                                (int) (batteryWidth * iconScaleFactor), (int) (batteryHeight * iconScaleFactor));

                        scaledLayoutParams.setMargins(0, 0, 0, marginBottom);

                        mBatteryIconView.setLayoutParams(scaledLayoutParams);

                        param.setResult(null);
                    }
                });

        //Xposedbridge.log("BSIAPOSED: part 5");

        XposedHelpers.findAndHookMethod("com.android.systemui.BatteryMeterView", lpparam.classLoader,
                "onBatteryLevelChanged", int.class, boolean.class, boolean.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        CircleBatteryDrawable mCircleDrawable = (CircleBatteryDrawable) XposedHelpers.getAdditionalInstanceField(param.thisObject, "mCircleDrawable");
                        mCircleDrawable.setCharging((boolean) param.args[1]);
                        mCircleDrawable.setBatteryLevel((int) param.args[0]);
                    }
                });

        //Xposedbridge.log("BSIAPOSED: part 6");

        XposedHelpers.findAndHookMethod("com.android.systemui.BatteryMeterView", lpparam.classLoader,
                "onPowerSaveChanged", boolean.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        CircleBatteryDrawable mCircleDrawable = (CircleBatteryDrawable) XposedHelpers.getAdditionalInstanceField(param.thisObject, "mCircleDrawable");
                        mCircleDrawable.setPowerSaveEnabled((boolean) param.args[0]);
                    }
                });

        //Xposedbridge.log("BSIAPOSED: part 7");

        XposedHelpers.findAndHookMethod("com.android.systemui.BatteryMeterView", lpparam.classLoader,
                "updateColors", int.class, int.class, int.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        CircleBatteryDrawable mCircleDrawable = (CircleBatteryDrawable) XposedHelpers.getAdditionalInstanceField(param.thisObject, "mCircleDrawable");
                        if(mCircleDrawable == null) return;
                        mCircleDrawable.setColors((int) param.args[0], (int) param.args[1], (int) param.args[2]);
                    }
                });
        //Xposedbridge.log("BSIAPOSED: part 8");
        //Xposedbridge.log("BSIAPOSED: hook finished?");
    }
}