package sh.siava.AOSPMods.Utils;

import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.findClassIfExists;

import android.os.FileUtils;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;

import com.topjohnwu.superuser.Shell;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

import de.robv.android.xposed.callbacks.XC_LoadPackage;
import sh.siava.AOSPMods.AOSPMods;

@SuppressWarnings("CommentedOutCode")
public class Helpers {
    private static final int KB = 1024;
    private static final int MB = 1024 * KB;
    private static final int GB = 1024 * MB;

    public static List<String> activeOverlays = null;

    @SuppressWarnings("unused")
    public static void dumpClass(String className, XC_LoadPackage.LoadPackageParam lpparam){
        Class<?> ourClass = findClassIfExists(className, lpparam.classLoader);
        if(ourClass == null)
        {
            log("Class: " + className + " not found");
            return;
        }
        Method[] ms = ourClass.getDeclaredMethods();
        log("Class: " + className);
        log("extends: " + ourClass.getSuperclass().getName());
        log("Methods:");

        for(Method m : ms)
        {
            log(m.getName() + " - " + m.getReturnType() + " - " + m.getParameterCount());
            Class<?>[] cs = m.getParameterTypes();
            for(Class<?> c: cs)
            {
                log("\t\t" + c.getTypeName());
            }
        }
        log("Fields:");

        Field[] fs = ourClass.getDeclaredFields();
        for(Field f: fs)
        {
            log("\t\t" + Modifier.toString(f.getModifiers()) + " " + f.getName() + "-" + f.getType().getName());
        }
        log("End dump");
    }

    public static void getActiveOverlays(){
        List<String> result = new ArrayList<>();
        List<String> lines = com.topjohnwu.superuser.Shell.cmd("cmd overlay list --user 0").exec().getOut();
        //List<String> lines = Shell.sh("cmd overlay list --user 0").exec().getOut();
        for(String thisLine : lines)
        {
            if(thisLine.startsWith("[x]"))
            {
                result.add(thisLine.replace("[x] ", ""));
            }
        }
        activeOverlays = result;
    }

    public static void setOverlay(String Key, boolean enabled, boolean refresh, boolean force) {
        if(refresh) getActiveOverlays();
        setOverlay(Key, enabled, force);
    }

    public static void setOverlay(String Key, boolean enabled, boolean force) {
        if(AOSPMods.isSecondProcess) return;
    
        if(activeOverlays == null) getActiveOverlays(); //make sure we have a list in hand

        String mode = (enabled) ? "enable" : "disable";
        String packname;
//        boolean exclusive = false;

        if(Key.endsWith("Overlay")) {
            Overlays.overlayProp op = (Overlays.overlayProp) Overlays.Overlays.get(Key);
            //noinspection ConstantConditions
            packname = op.name;
//            exclusive = op.exclusive;
        }
        else if(Key.endsWith("OverlayG")) //It's a group of overlays to work together as a team
        {
            try {
                setOverlayGroup(Key, enabled, force);
            }catch (Exception ignored){}
            return;
        }
        else
        {
            packname = Key;
//            exclusive = true;
        }

/*        if (enabled && exclusive) {
            mode += "-exclusive"; //since we are checking all overlays, we don't need exclusive anymore.
        }*/

        boolean wasEnabled = (activeOverlays.contains(packname));

        if(enabled == wasEnabled && !force)
        {
            return; //nothing to do. We're already set
        }

        try {
            com.topjohnwu.superuser.Shell.cmd("cmd overlay " + mode + " --user 0 " + packname).exec();
        }
        catch(Throwable t)
        {
            t.printStackTrace();
        }
    }

    private static void setOverlayGroup(String key, boolean enabled, boolean force) {
        Overlays.overlayGroup thisGroup = (Overlays.overlayGroup) Overlays.Overlays.get(key);

        //noinspection ConstantConditions
        for(Overlays.overlayProp thisProp : thisGroup.members)
        {
            Helpers.setOverlay(thisProp.name, enabled, force);
        }
    }

    public static SpannableStringBuilder getHumanizedBytes(long bytes, float unitSizeFactor, String unitSeparator, String indicatorSymbol, @Nullable @ColorInt Integer textColor)
    {
        DecimalFormat decimalFormat;
        CharSequence formattedData;
        SpannableString spanSizeString;
        SpannableString spanUnitString;
        String unit;
        if (bytes >= GB) {
            unit = "GB";
            decimalFormat = new DecimalFormat("0.00");
            formattedData =  decimalFormat.format(bytes / (float)GB);
        } else if (bytes >= 100 * MB) {
            decimalFormat = new DecimalFormat("000");
            unit = "MB";
            formattedData =  decimalFormat.format(bytes / (float)MB);
        } else if (bytes >= 10 * MB) {
            decimalFormat = new DecimalFormat("00.0");
            unit = "MB";
            formattedData =  decimalFormat.format(bytes / (float)MB);
        } else if (bytes >= MB) {
            decimalFormat = new DecimalFormat("0.00");
            unit = "MB";
            formattedData =  decimalFormat.format(bytes / (float)MB);
        } else if (bytes >= 100 * KB) {
            decimalFormat = new DecimalFormat("000");
            unit = "KB";
            formattedData =  decimalFormat.format(bytes / (float)KB);
        } else if (bytes >= 10 * KB) {
            decimalFormat = new DecimalFormat("00.0");
            unit = "KB";
            formattedData =  decimalFormat.format(bytes / (float)KB);
        } else {
            decimalFormat = new DecimalFormat("0.00");
            unit = "KB";
            formattedData = decimalFormat.format(bytes / (float)KB);
        }
        spanSizeString = new SpannableString(formattedData);

        if(textColor != null)
        {
            spanSizeString.setSpan(new NetworkTraffic.trafficStyle(textColor), 0 , (formattedData).length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        spanUnitString = new SpannableString(unit + indicatorSymbol);
        spanUnitString.setSpan(new RelativeSizeSpan(unitSizeFactor), 0, (unit + indicatorSymbol).length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return new SpannableStringBuilder().append(spanSizeString).append(unitSeparator).append(spanUnitString);
    }

    public static boolean installDoubleZip(String DoubleZipped) //installs the zip magisk module. even if it's zipped inside another zip
    {
        try {
            //copy it to somewhere under our control
            File tempFile = File.createTempFile("doubleZ",".zip");
            Shell.cmd(String.format("cp %s %s", DoubleZipped, tempFile.getAbsolutePath())).exec();

            //unzip once, IF double zipped
            ZipFile unzipper = new ZipFile(tempFile);

            File unzippedFile;
            if(unzipper.stream().count() == 1)
            {
                unzippedFile = File.createTempFile("singleZ", "zip");
                FileOutputStream unzipOutputStream = new FileOutputStream(unzippedFile);
                FileUtils.copy(unzipper.getInputStream(unzipper.entries().nextElement()), unzipOutputStream);
                unzipOutputStream.close();
            }
            else
            {
                unzippedFile = tempFile;
            }

            //install
            Shell.cmd(String.format("magisk --install-module %s", unzippedFile.getAbsolutePath())).exec();

            //cleanup
            //noinspection ResultOfMethodCallIgnored
            tempFile.delete();
            //noinspection ResultOfMethodCallIgnored
            unzippedFile.delete();
            return true;
        }
        catch(Exception e){
            e.printStackTrace();
            return false;
        }
    }

}
