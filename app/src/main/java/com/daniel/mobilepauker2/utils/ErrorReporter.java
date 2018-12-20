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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;

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
    private String Manufacturer;
    private String Model;
    private String Product;
    private String Tags;
    private long Time;
    private String Type;
    private String User;
    private final HashMap<String, String> CustomParameters = new HashMap<>();

    private Thread.UncaughtExceptionHandler PreviousHandler;
    private static ErrorReporter S_mInstance;
    private Context CurContext;

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
        if (S_mInstance == null) {
            S_mInstance = new ErrorReporter();
        }
        return S_mInstance;
    }

    public void init(Context context) {
        PreviousHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
        CurContext = context;
    }

    public long getAvailableInternalMemorySize() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        long availableBlocks = stat.getAvailableBlocksLong();
        return availableBlocks * blockSize;
    }

    public long getTotalInternalMemorySize() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        long totalBlocks = stat.getBlockCountLong();
        return totalBlocks * blockSize;
    }

    void RecoltInformations(Context context) {
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

//			StringBuilder bldr = new StringBuilder();
//			Class<Build> build = android.os.Build.class;
//
//			for(Field curr: build.getFields())
//			{
//				if(curr.getType().equals(String.class))
//				{
//					try
//					{
//						bldr.append("Build->" + curr.getName() + ":").append("" + curr.get(build)).append("\n");
//					}
//					catch(Exception e)
//					{
//						Log.d("Error Reporter", "Build info exception.", e);
//					}
//				}
//			}

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Exception in ErrorReporter!");
        }
    }

    public String CreateInformationString() {
        RecoltInformations(CurContext);

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
        //SendErrorMail( Report );
        PreviousHandler.uncaughtException(t, e);
    }

    private void SendErrorMail(Context _context, String ErrorContent) {
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
//        String body = _context.getResources().getString(R.string.CrashReport_MailBody) +
//                "\n\n" +
//                ErrorContent +
//                "\n\n";

		 /* Create the Intent */
        final Intent emailIntent = new Intent(Intent.ACTION_SEND);
		
		/* Fill it with Data */
        emailIntent.setType("plain/text");
//        emailIntent.putExtra(Intent.EXTRA_TEXT, body);
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"simsoftrd@gmail.com"});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Android Pauker: Bug report");
				
		/* Send it off to the Activity-Chooser */
        _context.startActivity(Intent.createChooser(emailIntent, "Send mail..."));

//		sendIntent.putExtra(Intent.EXTRA_EMAIL,
//				new String[] {"postmaster@alocaly.com"});
//		sendIntent.putExtra(Intent.EXTRA_TEXT, body);
//		sendIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
//		sendIntent.setType("message/rfc822");
//		_context.startActivity( Intent.createChooser(sendIntent, "Title:") );
    }

    private void SaveAsFile(String ErrorContent) {
        try {
            Random generator = new Random();
            int random = generator.nextInt(99999);
            String FileName = "stack-" + random + ".stacktrace";
            FileOutputStream trace = CurContext.openFileOutput(FileName, Context.MODE_PRIVATE);
            trace.write(ErrorContent.getBytes());
            trace.close();
        } catch (Exception e) {
            throw new RuntimeException("Exception in ErrorReporter!");
        }
    }

    private String[] GetErrorFileList() {
        File dir = new File(FilePath + "/");
        // Try to create the files folder if it doesn't exist
        if(!dir.mkdir()) return new String[]{};
        // Filter for ".stacktrace" files
        FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".stacktrace");
            }
        };
        return dir.list(filter);
    }

    private boolean bIsThereAnyErrorFile() {
        return GetErrorFileList().length > 0;
    }

    public boolean isThereAnyErrorsToReport(Context _context) {
        FilePath = _context.getFilesDir().getAbsolutePath();
        return (bIsThereAnyErrorFile());
    }

    public void deleteErrorFiles(Context _context) {
        try {
            FilePath = _context.getFilesDir().getAbsolutePath();
            if (bIsThereAnyErrorFile()) {
                String[] ErrorFileList = GetErrorFileList();
                for (String curString : ErrorFileList) {
                    File curFile = new File(FilePath + "/" + curString);
                    curFile.delete();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Exception in ErrorReporter!");
        }
    }

    public void CheckErrorAndSendMail(Context _context) {
        try {
            FilePath = _context.getFilesDir().getAbsolutePath();
            if (bIsThereAnyErrorFile()) {
                StringBuilder WholeErrorText = new StringBuilder();
                // on limite à N le nombre d'envois de rapports ( car trop lent )
                String[] ErrorFileList = GetErrorFileList();
                int curIndex = 0;
                final int MaxSendMail = 5;
                for (String curString : ErrorFileList) {
                    if (curIndex++ <= MaxSendMail) {
                        WholeErrorText.append("New Trace collected :\n");
                        WholeErrorText.append("=====================\n ");
                        String filePath = FilePath + "/" + curString;
                        BufferedReader input = new BufferedReader(new FileReader(filePath));
                        String line;
                        while ((line = input.readLine()) != null) {
                            WholeErrorText.append(line).append("\n");
                        }
                        input.close();
                    }

                    // DELETE FILES !!!!
                    File curFile = new File(FilePath + "/" + curString);
                    curFile.delete();
                }
                SendErrorMail(_context, WholeErrorText.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Exception in ErrorReporter!");
        }
    }
}
