/*
 * Email error reporter
 * Original author Androidblogger
 * http://androidblogger.blogspot.com/2009/12/how-to-improve-your-application-crash.html
 */

package com.daniel.mobilepauker2.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;

import com.daniel.mobilepauker2.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Date;
import java.util.HashMap;

import static android.content.Context.MODE_PRIVATE;

public class ErrorReporter implements Thread.UncaughtExceptionHandler {
    private String VersionName;
    private String PackageName;
    private String FilePath;
    private String PhoneModel;
    private String AndroidVersion;
    private String Board;
    private String Brand;
    private String Device;
    private String Display;
    private String FingerPrint;
    private String Host;
    private String ID;
    private String Model;
    private String Product;
    private String Tags;
    private long Time;
    private String Type;
    private String User;
    private final HashMap<String, String> CustomParameters = new HashMap<>();

    private static ErrorReporter instance;
    private Context context;

    public void AddCustomData(String Key, String Value) {
        CustomParameters.put(Key, Value);
    }

    private String CreateCustomInfoString() {
        StringBuilder CustomInfo = new StringBuilder();
        for (String CurrentKey : CustomParameters.keySet()) {
            String CurrentVal = CustomParameters.get(CurrentKey);
            CustomInfo.append(CurrentKey).append(" = ").append(CurrentVal).append("\n");
        }
        return CustomInfo.toString();
    }

    public static ErrorReporter instance() {
        if (instance == null) {
            instance = new ErrorReporter();
        }
        return instance;
    }

    public void init(Context context) {
        Thread.setDefaultUncaughtExceptionHandler(this);
        this.context = context;
    }

    private long getAvailableInternalMemorySize() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        long availableBlocks = stat.getAvailableBlocksLong();
        return availableBlocks * blockSize;
    }

    private long getTotalInternalMemorySize() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        long totalBlocks = stat.getBlockCountLong();
        return totalBlocks * blockSize;
    }

    private void RecoltInformations() {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi;
            // Version

            if (pm != null) {
                pi = pm.getPackageInfo(context.getPackageName(), 0);
                VersionName = pi.versionName;
                // Package name
                PackageName = pi.packageName;
                // Device model
                PhoneModel = Build.MODEL;
                // Android version
                AndroidVersion = Build.VERSION.RELEASE;

                Board = Build.BOARD;
                Brand = Build.BRAND;
                Device = Build.DEVICE;
                Display = Build.DISPLAY;
                FingerPrint = Build.FINGERPRINT;
                Host = Build.HOST;
                ID = Build.ID;
                Model = Build.MODEL;
                Product = Build.PRODUCT;
                Tags = Build.TAGS;
                Time = Build.TIME;
                Type = Build.TYPE;
                User = Build.USER;
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Exception in ErrorReporter!");
        }
    }

    private String CreateInformationString() {
        RecoltInformations();

        String ReturnVal = "";
        ReturnVal += "Version : " + VersionName;
        ReturnVal += "\n";
        ReturnVal += "Package : " + PackageName;
        ReturnVal += "\n";
        ReturnVal += "FilePath : " + FilePath;
        ReturnVal += "\n";
        ReturnVal += "Phone Model" + PhoneModel;
        ReturnVal += "\n";
        ReturnVal += "Android Version : " + AndroidVersion;
        ReturnVal += "\n";
        ReturnVal += "Board : " + Board;
        ReturnVal += "\n";
        ReturnVal += "Brand : " + Brand;
        ReturnVal += "\n";
        ReturnVal += "Device : " + Device;
        ReturnVal += "\n";
        ReturnVal += "Display : " + Display;
        ReturnVal += "\n";
        ReturnVal += "Finger Print : " + FingerPrint;
        ReturnVal += "\n";
        ReturnVal += "Host : " + Host;
        ReturnVal += "\n";
        ReturnVal += "ID : " + ID;
        ReturnVal += "\n";
        ReturnVal += "Model : " + Model;
        ReturnVal += "\n";
        ReturnVal += "Product : " + Product;
        ReturnVal += "\n";
        ReturnVal += "Tags : " + Tags;
        ReturnVal += "\n";
        ReturnVal += "Time : " + Time;
        ReturnVal += "\n";
        ReturnVal += "Type : " + Type;
        ReturnVal += "\n";
        ReturnVal += "User : " + User;
        ReturnVal += "\n";
        ReturnVal += "Total Internal memory : " + getTotalInternalMemorySize();
        ReturnVal += "\n";
        ReturnVal += "Available Internal memory : " + getAvailableInternalMemorySize();
        ReturnVal += "\n";

        return ReturnVal;
    }

    public void uncaughtException(Thread t, Throwable e) {
        Log.d("ErrorReporter::uncaughtException", "Building error report");
        StringBuilder Report = new StringBuilder();
        Date CurDate = new Date();
        Report.append("Error Report collected on : ").append(CurDate.toString());
        Report.append("\n");
        Report.append("\n");
        Report.append("Informations :");
        Report.append("\n");
        Report.append("==============");
        Report.append("\n");
        Report.append("\n");
        Report.append(CreateInformationString());

        Report.append("Custom Informations :\n");
        Report.append("=====================\n");
        Report.append(CreateCustomInfoString());

        Report.append("\n\n");
        Report.append("Stack : \n");
        Report.append("======= \n");
        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        e.printStackTrace(printWriter);
        String stacktrace = result.toString();
        Report.append(stacktrace);

        Report.append("\n");
        Report.append("Cause : \n");
        Report.append("======= \n");

        // If the exception was thrown in a background thread inside
        // AsyncTask, then the actual exception can be found with getCause
        Throwable cause = e.getCause();
        while (cause != null) {
            cause.printStackTrace(printWriter);
            Report.append(result.toString());
            cause = cause.getCause();
        }
        printWriter.close();
        Report.append("****  End of current Report ***");
        SaveAsFile(Report.toString());
    }

    private void SendErrorMail(String ErrorContent) {
        String body = context.getResources().getString(R.string.crash_report_mail_body) +
                "\n\n" +
                ErrorContent +
                "\n\n";

        /* Create the Intent */
        final Intent emailIntent = new Intent(Intent.ACTION_SEND);

        /* Fill it with Data */
        emailIntent.setType("plain/text");
        emailIntent.putExtra(Intent.EXTRA_TEXT, body);
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"fritsch_daniel@gmx.de"});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.crash_report_mail_subject));

        /* Send it off to the Activity-Chooser */
        context.startActivity(Intent.createChooser(emailIntent, "Send mail..."));
    }

    private void SaveAsFile(String ErrorContent) {
        try {
            String FileName = "error.stacktrace";
            FileOutputStream trace = context.openFileOutput(FileName, Context.MODE_PRIVATE);
            trace.write(ErrorContent.getBytes());
            trace.close();
        } catch (Exception e) {
            throw new RuntimeException("Exception in ErrorReporter!");
        }
    }

    private boolean bIsThereAnyErrorFile() {
        BufferedReader bis = null;
        try {
            FileInputStream inputStream = context.openFileInput("error.stacktrace");
            bis = new BufferedReader(new InputStreamReader(inputStream));
            return !bis.readLine().equals("");
        } catch (FileNotFoundException e) {
            return false;
        } catch (IOException e) {
            return false;
        } finally {
            try {
                if (bis != null) bis.close();
            } catch (IOException ignored) {
            }
        }
    }

    public boolean isThereAnyErrorsToReport() {
        FilePath = context.getFilesDir().getAbsolutePath();
        return (bIsThereAnyErrorFile());
    }

    public void deleteErrorFiles() {
        try {
            FilePath = context.getFilesDir().getAbsolutePath();
            if (bIsThereAnyErrorFile()) {
                FileOutputStream fos = context.openFileOutput("error.stacktrace", MODE_PRIVATE);
                String text = "\n";
                fos.write(text.getBytes());
                fos.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Exception in ErrorReporter!");
        }
    }

    public void CheckErrorAndSendMail() {
        try {
            FilePath = context.getFilesDir().getAbsolutePath();
            if (bIsThereAnyErrorFile()) {
                StringBuilder WholeErrorText = new StringBuilder();
                WholeErrorText.append("New Trace collected :\n");
                WholeErrorText.append("=====================\n ");
                BufferedReader input = new BufferedReader(new InputStreamReader(context.openFileInput("error.stacktrace")));
                String line;
                while ((line = input.readLine()) != null) {
                    WholeErrorText.append(line).append("\n");
                }
                input.close();

                // DELETE FILES !!!!
                deleteErrorFiles();

                SendErrorMail(WholeErrorText.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Exception in ErrorReporter!");
        }
    }
}
