/*
 * Email error reporter
 * Original author Androidblogger
 * http://androidblogger.blogspot.com/2009/12/how-to-improve-your-application-crash.html
 */
package de.daniel.mobilepauker2.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.os.Build
import android.os.Environment
import android.os.StatFs
import de.daniel.mobilepauker2.R
import java.io.*
import java.util.*
import javax.inject.Inject

class ErrorReporter @Inject constructor(private val context: Context) :
    Thread.UncaughtExceptionHandler {

    private val customParameters = HashMap<String, String>()
    private var versionName: String = ""
    private var packageName: String = ""
    private var filePath: String = ""
    private var phoneModel: String = ""
    private var androidVersion: String = ""
    private var board: String = ""
    private var brand: String = ""
    private var device: String = ""
    private var display: String = ""
    private var fingerPrint: String = ""
    private var host: String = ""
    private var iD: String = ""
    private var model: String = ""
    private var product: String = ""
    private var tags: String = ""
    private var time: Long = 0
    private var type: String = ""
    private var user: String = ""

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

    val isThereAnyErrorsToReport: Boolean
        get() {
            filePath = context.filesDir.absolutePath
            return bIsThereAnyErrorFile()
        }

    override fun uncaughtException(t: Thread?, e: Throwable) {
        Log.d("ErrorReporter::uncaughtException", "Building error report")
        val report = StringBuilder()
        val curDate = Date()
        report.append("Error Report collected on : ").append(curDate.toString())
        report.append("\n")
        report.append("\n")
        report.append("Informations :")
        report.append("\n")
        report.append("==============")
        report.append("\n")
        report.append("\n")
        report.append(createInformationString())
        report.append("Custom Informations :\n")
        report.append("=====================\n")
        report.append(createCustomInfoString())
        report.append("\n\n")
        report.append("Stack : \n")
        report.append("======= \n")
        val result: Writer = StringWriter()
        val printWriter = PrintWriter(result)
        e.printStackTrace(printWriter)
        val stacktrace = result.toString()
        report.append(stacktrace)
        report.append("\n")
        report.append("Cause : \n")
        report.append("======= \n")

        // If the exception was thrown in a background thread inside
        // AsyncTask, then the actual exception can be found with getCause
        var cause = e.cause
        while (cause != null) {
            cause.printStackTrace(printWriter)
            report.append(result.toString())
            cause = cause.cause
        }
        printWriter.close()
        report.append("****  End of current Report ***")
        saveAsFile(report.toString())
    }

    fun init() {
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    fun addCustomData(Key: String, Value: String) {
        customParameters[Key] = Value
    }

    fun deleteErrorFiles() {
        try {
            filePath = context.filesDir.absolutePath
            if (bIsThereAnyErrorFile()) {
                val fos = context.openFileOutput("error.stacktrace", Context.MODE_PRIVATE)
                val text = "\n"
                fos.write(text.toByteArray())
                fos.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException("Exception in ErrorReporter!")
        }
    }

    fun checkErrorAndSendMail(): Intent? {
        try {
            filePath = context.filesDir.absolutePath
            if (bIsThereAnyErrorFile()) {
                val wholeErrorText = StringBuilder()
                wholeErrorText.append("New Trace collected :\n")
                wholeErrorText.append("=====================\n ")
                val input = BufferedReader(
                    InputStreamReader(
                        context.openFileInput("error.stacktrace")
                    )
                )
                var line: String?
                while (input.readLine().also { line = it } != null) {
                    wholeErrorText.append(line).append("\n")
                }
                input.close()

                // DELETE FILES !!!!
                deleteErrorFiles()
                return sendErrorMail(wholeErrorText.toString())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException(e.message)
        }
        return null
    }

    private fun createCustomInfoString(): String {
        val customInfo = StringBuilder()
        for (CurrentKey in customParameters.keys) {
            val currentVal = customParameters[CurrentKey]
            customInfo.append(CurrentKey).append(" = ").append(currentVal).append("\n")
        }
        return customInfo.toString()
    }

    private fun recoltInformations() {
        try {
            val pm = context.packageManager
            val pi: PackageInfo
            // Version
            if (pm != null) {
                pi = pm.getPackageInfo(context.packageName, 0)
                versionName = pi.versionName
                // Package name
                packageName = pi.packageName
                // Device model
                phoneModel = Build.MODEL
                // Android version
                androidVersion = Build.VERSION.RELEASE
                board = Build.BOARD
                brand = Build.BRAND
                device = Build.DEVICE
                display = Build.DISPLAY
                fingerPrint = Build.FINGERPRINT
                host = Build.HOST
                iD = Build.ID
                model = Build.MODEL
                product = Build.PRODUCT
                tags = Build.TAGS
                time = Build.TIME
                type = Build.TYPE
                user = Build.USER
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException("Exception in ErrorReporter!")
        }
    }

    private fun createInformationString(): String {
        recoltInformations()
        var returnVal = ""
        returnVal += "Version : $versionName"
        returnVal += "\n"
        returnVal += "Package : $packageName"
        returnVal += "\n"
        returnVal += "FilePath : $filePath"
        returnVal += "\n"
        returnVal += "Phone Model$phoneModel"
        returnVal += "\n"
        returnVal += "Android Version : $androidVersion"
        returnVal += "\n"
        returnVal += "Board : $board"
        returnVal += "\n"
        returnVal += "Brand : $brand"
        returnVal += "\n"
        returnVal += "Device : $device"
        returnVal += "\n"
        returnVal += "Display : $display"
        returnVal += "\n"
        returnVal += "Finger Print : $fingerPrint"
        returnVal += "\n"
        returnVal += "Host : $host"
        returnVal += "\n"
        returnVal += "ID : $iD"
        returnVal += "\n"
        returnVal += "Model : $model"
        returnVal += "\n"
        returnVal += "Product : $product"
        returnVal += "\n"
        returnVal += "Tags : $tags"
        returnVal += "\n"
        returnVal += "Time : $time"
        returnVal += "\n"
        returnVal += "Type : $type"
        returnVal += "\n"
        returnVal += "User : $user"
        returnVal += "\n"
        returnVal += "Total Internal memory : $totalInternalMemorySize"
        returnVal += "\n"
        returnVal += "Available Internal memory : $availableInternalMemorySize"
        returnVal += "\n"
        return returnVal
    }

    private fun sendErrorMail(ErrorContent: String): Intent {
        val body = """
            ${context.resources.getString(R.string.crash_report_mail_body)}
            
            $ErrorContent
            
            
            """

        val emailIntent = Intent(Intent.ACTION_SEND)

        /* Fill it with Data */emailIntent.type = "plain/text"
        emailIntent.putExtra(Intent.EXTRA_TEXT, body)
        emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf("fritsch_daniel@gmx.de"))
        emailIntent.putExtra(
            Intent.EXTRA_SUBJECT,
            context.getString(R.string.crash_report_mail_subject)
        )
        emailIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

        return emailIntent
    }

    private fun saveAsFile(ErrorContent: String) {
        try {
            val fileName = "error.stacktrace"
            val trace = context.openFileOutput(fileName, Context.MODE_PRIVATE)
            trace.write(ErrorContent.toByteArray())
            trace.close()
        } catch (e: Exception) {
            throw RuntimeException("Exception in ErrorReporter!")
        }
    }

    private fun bIsThereAnyErrorFile(): Boolean {
        var bis: BufferedReader? = null
        return try {
            val inputStream = context.openFileInput("error.stacktrace")
            bis = BufferedReader(InputStreamReader(inputStream))
            bis.readLine() != ""
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
}