/*
 * Email error reporter
 * Original author Androidblogger
 * http://androidblogger.blogspot.com/2009/12/how-to-improve-your-application-crash.html
 */
package com.daniel.mobilepauker2.pauker_native

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.os.Build
import android.os.Environment
import android.os.StatFs
import com.daniel.mobilepauker2.R
import java.io.*
import java.util.*

class ErrorReporter(val context: Context) : Thread.UncaughtExceptionHandler {
    private var VersionName: String? = null
    private var PackageName: String? = null
    private var filePath: String? = null
    private var PhoneModel: String? = null
    private var AndroidVersion: String? = null
    private var Board: String? = null
    private var Brand: String? = null
    private var Device: String? = null
    private var Display: String? = null
    private var FingerPrint: String? = null
    private var Host: String? = null
    private var ID: String? = null
    private var Model: String? = null
    private var Product: String? = null
    private var Tags: String? = null
    private var Time: Long = 0
    private var Type: String? = null
    private var User: String? = null
    private val CustomParameters = HashMap<String, String>()

    fun addCustomData(Key: String, Value: String) {
        CustomParameters[Key] = Value
    }

    private fun createCustomInfoString(): String {
        val CustomInfo = StringBuilder()
        for (CurrentKey in CustomParameters.keys) {
            val CurrentVal = CustomParameters[CurrentKey]
            CustomInfo.append(CurrentKey).append(" = ").append(CurrentVal).append("\n")
        }
        return CustomInfo.toString()
    }

    fun init() {
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    private val availableInternalMemorySize: Long
        get() {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val availableBlocks = stat.availableBlocksLong
            return availableBlocks * blockSize
        }

    private val totalInternalMemorySize: Long
        get() {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            return totalBlocks * blockSize
        }

    private fun recoltInformations() {
        try {
            val pm = context.packageManager
            val pi: PackageInfo
            // Version
            if (pm != null) {
                pi = pm.getPackageInfo(context.packageName, 0)
                VersionName = pi.versionName
                // Package name
                PackageName = pi.packageName
                // Device model
                PhoneModel = Build.MODEL
                // Android version
                AndroidVersion = Build.VERSION.RELEASE
                Board = Build.BOARD
                Brand = Build.BRAND
                Device = Build.DEVICE
                Display = Build.DISPLAY
                FingerPrint = Build.FINGERPRINT
                Host = Build.HOST
                ID = Build.ID
                Model = Build.MODEL
                Product = Build.PRODUCT
                Tags = Build.TAGS
                Time = Build.TIME
                Type = Build.TYPE
                User = Build.USER
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException("Exception in ErrorReporter!")
        }
    }

    private fun createInformationString(): String {
        recoltInformations()
        var ReturnVal = ""
        ReturnVal += "Version : $VersionName"
        ReturnVal += "\n"
        ReturnVal += "Package : $PackageName"
        ReturnVal += "\n"
        ReturnVal += "FilePath : $filePath"
        ReturnVal += "\n"
        ReturnVal += "Phone Model$PhoneModel"
        ReturnVal += "\n"
        ReturnVal += "Android Version : $AndroidVersion"
        ReturnVal += "\n"
        ReturnVal += "Board : $Board"
        ReturnVal += "\n"
        ReturnVal += "Brand : $Brand"
        ReturnVal += "\n"
        ReturnVal += "Device : $Device"
        ReturnVal += "\n"
        ReturnVal += "Display : $Display"
        ReturnVal += "\n"
        ReturnVal += "Finger Print : $FingerPrint"
        ReturnVal += "\n"
        ReturnVal += "Host : $Host"
        ReturnVal += "\n"
        ReturnVal += "ID : $ID"
        ReturnVal += "\n"
        ReturnVal += "Model : $Model"
        ReturnVal += "\n"
        ReturnVal += "Product : $Product"
        ReturnVal += "\n"
        ReturnVal += "Tags : $Tags"
        ReturnVal += "\n"
        ReturnVal += "Time : $Time"
        ReturnVal += "\n"
        ReturnVal += "Type : $Type"
        ReturnVal += "\n"
        ReturnVal += "User : $User"
        ReturnVal += "\n"
        ReturnVal += "Total Internal memory : $totalInternalMemorySize"
        ReturnVal += "\n"
        ReturnVal += "Available Internal memory : $availableInternalMemorySize"
        ReturnVal += "\n"
        return ReturnVal
    }

    override fun uncaughtException(t: Thread?, e: Throwable) {
        Log.d(
                "ErrorReporter::uncaughtException",
                "Building error report"
        )
        val Report = StringBuilder()
        val CurDate = Date()
        Report.append("Error Report collected on : ").append(CurDate.toString())
        Report.append("\n")
        Report.append("\n")
        Report.append("Informations :")
        Report.append("\n")
        Report.append("==============")
        Report.append("\n")
        Report.append("\n")
        Report.append(createInformationString())
        Report.append("Custom Informations :\n")
        Report.append("=====================\n")
        Report.append(createCustomInfoString())
        Report.append("\n\n")
        Report.append("Stack : \n")
        Report.append("======= \n")
        val result: Writer = StringWriter()
        val printWriter = PrintWriter(result)
        e.printStackTrace(printWriter)
        val stacktrace = result.toString()
        Report.append(stacktrace)
        Report.append("\n")
        Report.append("Cause : \n")
        Report.append("======= \n")
        // If the exception was thrown in a background thread inside
        // AsyncTask, then the actual exception can be found with getCause
        var cause = e.cause
        while (cause != null) {
            cause.printStackTrace(printWriter)
            Report.append(result.toString())
            cause = cause.cause
        }
        printWriter.close()
        Report.append("****  End of current Report ***")
        saveAsFile(Report.toString())
    }

    private fun sendErrorMail(ErrorContent: String) {
        val body = context.resources?.getString(R.string.crash_report_mail_body) +
                "\n\n" +
                ErrorContent +
                "\n\n"
        /* Create the Intent */
        val emailIntent = Intent(Intent.ACTION_SEND)
        /* Fill it with Data */emailIntent.type = "plain/text"
        emailIntent.putExtra(Intent.EXTRA_TEXT, body)
        emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf("fritsch_daniel@gmx.de"))
        emailIntent.putExtra(
                Intent.EXTRA_SUBJECT,
                context.getString(R.string.crash_report_mail_subject)
        )
        /* Send it off to the Activity-Chooser */
        (context as Activity).startActivity(
                Intent.createChooser(
                        emailIntent,
                        "Send mail..."
                )
        )
    }

    private fun saveAsFile(ErrorContent: String) {
        try {
            val FileName = "error.stacktrace"
            val trace = context.openFileOutput(FileName, Context.MODE_PRIVATE)
            trace?.write(ErrorContent.toByteArray())
            trace?.close()
        } catch (e: Exception) {
            throw RuntimeException("Exception in ErrorReporter!")
        }
    }

    private fun bIsThereAnyErrorFile(): Boolean {
        var bis: BufferedReader? = null
        return try {
            val inputStream = context.openFileInput("error.stacktrace")
            bis = BufferedReader(InputStreamReader(inputStream))
            val line = bis.readLine()

            return line.isNotBlank() && line.isNotEmpty()
        } catch (e: FileNotFoundException) {
            false
        } catch (e: IOException) {
            false
        } finally {
            try {
                bis?.close()
            } catch (ignored: IOException) {
            }
        }
    }

    val isThereAnyErrorsToReport: Boolean
        get() {
            filePath = context?.filesDir?.absolutePath
            return bIsThereAnyErrorFile()
        }

    fun deleteErrorFiles() {
        try {
            filePath = context?.filesDir?.absolutePath
            if (bIsThereAnyErrorFile()) {
                val fos =
                        context?.openFileOutput("error.stacktrace", Context.MODE_PRIVATE)
                val text = "\n"
                fos?.write(text.toByteArray())
                fos?.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException("Exception in ErrorReporter!")
        }
    }

    fun checkErrorAndSendMail() {
        try {
            filePath = context.filesDir?.absolutePath ?: filePath
            if (bIsThereAnyErrorFile()) {
                val WholeErrorText = StringBuilder()
                WholeErrorText.append("New Trace collected :\n")
                WholeErrorText.append("=====================\n ")
                val input =
                        BufferedReader(InputStreamReader(context.openFileInput("error.stacktrace")))
                var line: String?
                while (input.readLine().also { line = it } != null) {
                    WholeErrorText.append(line).append("\n")
                }
                input.close()
                // DELETE FILES !!!!
                deleteErrorFiles()
                sendErrorMail(WholeErrorText.toString())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException("Exception in ErrorReporter! {${e.message}")
        }
    }

    companion object {
        lateinit var reporter: ErrorReporter

        fun instance(): ErrorReporter {
            return reporter
        }
    }
}