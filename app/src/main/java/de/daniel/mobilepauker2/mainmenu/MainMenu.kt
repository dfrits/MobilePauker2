package de.daniel.mobilepauker2.mainmenu

import android.Manifest
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
import android.text.method.ScrollingMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelState
import de.daniel.mobilepauker2.R
import de.daniel.mobilepauker2.application.PaukerApplication
import de.daniel.mobilepauker2.data.DataManager
import de.daniel.mobilepauker2.data.SaveAsCallback
import de.daniel.mobilepauker2.data.SaveAsDialog
import de.daniel.mobilepauker2.dropbox.SyncDialog
import de.daniel.mobilepauker2.editcard.AddCard
import de.daniel.mobilepauker2.learning.LearnCards
import de.daniel.mobilepauker2.lesson.EditDescription
import de.daniel.mobilepauker2.lesson.LessonManager
import de.daniel.mobilepauker2.lesson.batch.BatchType
import de.daniel.mobilepauker2.lessonimport.LessonImport
import de.daniel.mobilepauker2.models.LearningPhase.*
import de.daniel.mobilepauker2.models.LearningPhase.Companion.setLearningPhase
import de.daniel.mobilepauker2.notification.NotificationService
import de.daniel.mobilepauker2.search.Search
import de.daniel.mobilepauker2.settings.PaukerSettings
import de.daniel.mobilepauker2.settings.SettingsManager
import de.daniel.mobilepauker2.settings.SettingsManager.Keys.AUTO_UPLOAD
import de.daniel.mobilepauker2.settings.SettingsManager.Keys.HIDE_TIMES
import de.daniel.mobilepauker2.statistics.ChartAdapter
import de.daniel.mobilepauker2.statistics.ChartAdapter.ChartAdapterCallback
import de.daniel.mobilepauker2.utils.Constants
import de.daniel.mobilepauker2.utils.Constants.ACCESS_TOKEN
import de.daniel.mobilepauker2.utils.Constants.FILES
import de.daniel.mobilepauker2.utils.Constants.NOTIFICATION_CHANNEL_ID
import de.daniel.mobilepauker2.utils.Constants.REQUEST_CODE_SAVE_DIALOG_NEW_LESSON
import de.daniel.mobilepauker2.utils.Constants.REQUEST_CODE_SAVE_DIALOG_NORMAL
import de.daniel.mobilepauker2.utils.Constants.REQUEST_CODE_SAVE_DIALOG_OPEN
import de.daniel.mobilepauker2.utils.Constants.TIMER_BAR_CHANNEL_ID
import de.daniel.mobilepauker2.utils.Constants.UPLOAD_FILE_ACTION
import de.daniel.mobilepauker2.utils.ErrorReporter
import de.daniel.mobilepauker2.utils.Log
import de.daniel.mobilepauker2.utils.Toaster
import javax.inject.Inject

class MainMenu : AppCompatActivity(R.layout.main_menu) {
    @Inject
    lateinit var viewModel: MainMenuViewModel

    @Inject
    lateinit var lessonManager: LessonManager

    @Inject
    lateinit var dataManager: DataManager

    @Inject
    lateinit var toaster: Toaster

    @Inject
    lateinit var errorReporter: ErrorReporter

    @Inject
    lateinit var settingsManager: SettingsManager

    private val context = this
    private val RQ_WRITE_EXT_SAVE_NEW = 97
    private val RQ_WRITE_EXT_SAVE = 98
    private val RQ_WRITE_EXT_OPEN = 99
    private var chartView: RecyclerView? = null
    private var firstStart = true
    private var search: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        (applicationContext as PaukerApplication).applicationSingletonComponent.inject(this)

        PreferenceManager.setDefaultValues(this, R.xml.preferences_main, false)
        PreferenceManager.setDefaultValues(this, R.xml.preferences_dropbox, false)
        PreferenceManager.setDefaultValues(this, R.xml.preferences_notifications, false)
        createNotificationChannels()

        setContentView(R.layout.main_menu)

        viewModel.checkLessonIsSetup()

        errorReporter.init()
        checkErrors()

        initButtons()
        initView()
        initChartList()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        val save = menu.findItem(R.id.mSaveFile)
        search = menu.findItem(R.id.mSearch)
        val open = menu.findItem(R.id.mOpenLesson)
        menu.setGroupEnabled(
            R.id.mGroup,
            lessonManager.isLessonNotNew() || !lessonManager.isLessonEmpty()
        )
        open.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        if (viewModel.getBatchSize(BatchType.LESSON) > 0) {
            search?.isVisible = true
            open.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        } else {
            search?.isVisible = false
            if (!dataManager.saveRequired) {
                open.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            }
        }
        save.isVisible = dataManager.saveRequired
        if (search?.isVisible == true) {
            val searchView = search?.actionView as SearchView
            searchView.isIconifiedByDefault = false
            searchView.isIconified = false
            searchView.queryHint = getString(R.string.search_hint)
            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String): Boolean {
                    val browseIntent = Intent(Intent.ACTION_SEARCH)
                    browseIntent.setClass(context, Search::class.java)
                    browseIntent.putExtra(SearchManager.QUERY, query)
                    startActivity(browseIntent)
                    searchView.setQuery("", false)
                    return true
                }

                override fun onQueryTextChange(newText: String): Boolean {
                    return false
                }
            })
            searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
                if (!hasFocus) searchView.clearFocus()
            }
            search?.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
                override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                    return true
                }

                override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                    (item?.actionView as SearchView?)?.setQuery("", false)
                    return true
                }

            })
        }
        return true
    }

    override fun onPause() {
        chartView = null
        super.onPause()
    }

    override fun onResume() {
        Log.d("MainMenuActivity::onResume", "ENTRY")
        super.onResume()
        viewModel.resetShortTerms()
        search?.collapseActionView()
        if (!firstStart) {
            initButtons()
            initView()
            initChartList()
            invalidateOptionsMenu()
        }
        firstStart = false
        NotificationManagerCompat.from(context).cancelAll()
    }

    override fun onBackPressed() {
        if (dataManager.saveRequired) {
            val builder = AlertDialog.Builder(context)
            builder.setMessage(R.string.close_without_saving_dialog_msg)
                .setPositiveButton(R.string.cancel, null)
                .setNeutralButton(R.string.close) { _, _ -> finish() }
            val dialog = builder.create()
            dialog.show()
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(getColor(R.color.unlearned))
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getColor(R.color.learned))
        } else super.onBackPressed()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RQ_WRITE_EXT_OPEN && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openLesson()
        }
        if (requestCode == RQ_WRITE_EXT_SAVE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            checkLessonNameThenSave(REQUEST_CODE_SAVE_DIALOG_NORMAL)
        }
        if (requestCode == RQ_WRITE_EXT_SAVE_NEW && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            checkLessonNameThenSave(REQUEST_CODE_SAVE_DIALOG_NEW_LESSON)
        }
    }

    override fun onDestroy() {
        NotificationService.enqueueWork(context)
        super.onDestroy()
    }

    private fun saveFinished(requestCode: Int) {
        if (settingsManager.getBoolPreference(AUTO_UPLOAD)) {
            uploadCurrentFile()
        }

        when (requestCode) {
            REQUEST_CODE_SAVE_DIALOG_NORMAL -> {
                toaster.showToast(context as Activity, R.string.saving_success, Toast.LENGTH_SHORT)
                dataManager.saveRequired = false

                toaster.showExpireToast(context as Activity)
                onResume()
                invalidateOptionsMenu()
            }
            REQUEST_CODE_SAVE_DIALOG_NEW_LESSON -> {
                createNewLesson()
            }
            REQUEST_CODE_SAVE_DIALOG_OPEN -> {
                startActivity(Intent(context, LessonImport::class.java))
            }
        }
    }

    private fun uploadCurrentFile() {
        val accessToken = getDefaultSharedPreferences(context)
            .getString(Constants.DROPBOX_ACCESS_TOKEN, null)
        val intent = Intent(context, SyncDialog::class.java)
        intent.putExtra(ACCESS_TOKEN, accessToken)
        val file = dataManager.getPathOfCurrentFile()
        intent.putExtra(FILES, file)
        intent.action = UPLOAD_FILE_ACTION
        startActivity(intent)
    }

    private fun initButtons() {
        val hasCardsToLearn = viewModel.getBatchSize(BatchType.UNLEARNED) != 0
        val hasExpiredCards = viewModel.getBatchSize(BatchType.EXPIRED) != 0

        findViewById<ImageButton>(R.id.bLearnNewCard)?.let {
            it.isEnabled = hasCardsToLearn
            it.isClickable = hasCardsToLearn
        }
        findViewById<TextView>(R.id.tLearnNewCardDesc)?.isEnabled = hasCardsToLearn

        findViewById<ImageButton>(R.id.bRepeatExpiredCards)?.let {
            it.isEnabled = hasExpiredCards
            it.isClickable = hasExpiredCards
        }
        findViewById<TextView>(R.id.tRepeatExpiredCardsDesc)?.isEnabled = hasExpiredCards
    }

    private fun initView() {
        invalidateOptionsMenu()

        val description: String = viewModel.getDescription()
        val descriptionView: TextView = findViewById(R.id.infoText)
        descriptionView.text = description
        if (description.isNotEmpty()) {
            descriptionView.movementMethod = ScrollingMovementMethod()
        }

        findViewById<SlidingUpPanelLayout>(R.id.drawerPanel)?.let {
            it.addPanelSlideListener(object : SlidingUpPanelLayout.PanelSlideListener {
                override fun onPanelSlide(panel: View, slideOffset: Float) {}
                override fun onPanelStateChanged(
                    panel: View,
                    previousState: PanelState,
                    newState: PanelState
                ) {
                    if (newState == PanelState.EXPANDED)
                        findViewById<ImageView>(R.id.drawerImage).rotation = 180f
                    if (newState == PanelState.COLLAPSED)
                        findViewById<ImageView>(R.id.drawerImage).rotation = 0f
                }
            })
            it.panelState = PanelState.COLLAPSED
        }

        title =
            if (lessonManager.isLessonNotNew()) dataManager.getReadableCurrentFileName()
            else getString(R.string.app_name)
    }

    private fun initChartList() {
        // Im Thread laufen lassen um MainThread zu entlasten
        chartView = findViewById(R.id.chartListView)
        val layoutManager = LinearLayoutManager(
            context,
            LinearLayoutManager.HORIZONTAL, false
        )
        chartView!!.layoutManager = layoutManager
        chartView!!.overScrollMode = View.OVER_SCROLL_NEVER
        chartView!!.isScrollContainer = true
        chartView!!.isNestedScrollingEnabled = true
        val onClickListener: ChartAdapterCallback = object : ChartAdapterCallback {
            override fun onClick(position: Int) {
                Log.d("MainMenu::ChartCallback", "On Bar clicked")
                showBatchDetails(position)
            }
        }
        Log.d("MainMenu::ChartInit", "Callback initialized")
        val adapter = ChartAdapter(application as PaukerApplication, onClickListener)
        chartView!!.adapter = adapter
        Log.d("MainMenu::ChartInit", "Adapter set")
    }

    private fun showBatchDetails(index: Int) {
        if (lessonManager.getBatchSize(BatchType.LESSON) == 0) return
        val browseIntent = Intent(Intent.ACTION_SEARCH)
        browseIntent.setClass(context, Search::class.java)
        browseIntent.putExtra(SearchManager.QUERY, "")
        if (index > 1 && lessonManager.getBatchStatistics()[index - 2].batchSize == 0
            || index == 1 && lessonManager.getBatchSize(BatchType.UNLEARNED) == 0
        ) {
            return
        }
        browseIntent.putExtra(Constants.STACK_INDEX, index)
        startActivity(browseIntent)
    }

    private fun openLesson() {
        if (!hasPermission()) {
            showPermissionDialog(RQ_WRITE_EXT_OPEN)
        } else {
            if (dataManager.saveRequired) {
                val builder = AlertDialog.Builder(context)
                builder.setTitle(R.string.lesson_not_saved_dialog_title)
                    .setMessage(R.string.save_lesson_before_question)
                    .setPositiveButton(R.string.save) { _, _ ->
                        checkSavePermissionThenSave(REQUEST_CODE_SAVE_DIALOG_OPEN)
                    }
                    .setNeutralButton(R.string.open_lesson) { dialog, _ ->
                        startActivity(Intent(context, LessonImport::class.java))
                        dialog.dismiss()
                    }
                builder.create().show()
            } else startActivity(Intent(context, LessonImport::class.java))
        }
    }

    private fun createNewLesson() {
        viewModel.createNewLesson()
        toaster.showToast(context as Activity, R.string.new_lession_created, Toast.LENGTH_SHORT)
        initButtons()
        initChartList()
        initView()
    }

    private fun hasPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()
            ) {
                return true
            }
        } else if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }

    private fun showPermissionDialog(requestCode: Int) {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        val builder = AlertDialog.Builder(context)
        builder.setTitle(R.string.app_name)
            .setPositiveButton(R.string.next) { dialog, _ ->
                PreferenceManager.getDefaultSharedPreferences(context)
                    .edit().putBoolean("FirstTime", false).apply()
                requestPermission(requestCode)
                dialog.dismiss()
            }
            .setNeutralButton(R.string.not_now) { dialog, _ -> dialog.dismiss() }

        if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            builder.setMessage(R.string.write_permission_rational_message)
        } else {
            if (pref.getBoolean("FirstTime", true)) {
                builder.setMessage(R.string.write_permission_info_message)
            } else {
                builder.setMessage(R.string.write_permission_rational_message)
                    .setPositiveButton(R.string.settings) { dialog, _ ->
                        showPermissionSettings()
                        dialog.dismiss()
                    }
            }
        }
        val dialog = builder.create()
        dialog.show()
    }

    private fun requestPermission(requestCode: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent()
            intent.action = ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
            startActivity(intent)
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                requestCode
            )
        }
    }

    private fun showPermissionSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent()
            intent.action = ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
            startActivity(intent)
        } else {
            val intent = Intent()
            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            val uri = Uri.fromParts("package", packageName, null)
            intent.data = uri
            startActivity(intent)
        }
    }

    private fun checkErrors() {
        if (errorReporter.isThereAnyErrorsToReport) {
            val altBld = AlertDialog.Builder(this)
            altBld.setTitle(getString(R.string.crash_report_title))
                .setMessage(getString(R.string.crash_report_message))
                .setCancelable(false)
                .setPositiveButton(
                    getString(R.string.ok)
                ) { _, _ ->
                    val errorReportIntent = errorReporter.checkErrorAndSendMail()
                    startActivity(
                        Intent.createChooser(
                            errorReportIntent,
                            "Send mail..."
                        )
                    )
                }
                .setNeutralButton(
                    getString(R.string.cancel)
                ) { dialog, _ ->
                    errorReporter.deleteErrorFiles()
                    dialog.cancel()
                }
            val alert = altBld.create()
            alert.setIcon(R.mipmap.ic_launcher)
            alert.show()
        }
    }

    private fun checkSavePermissionThenSave(requestCode: Int) {
        if (!hasPermission()) {
            when (requestCode) {
                REQUEST_CODE_SAVE_DIALOG_NORMAL -> showPermissionDialog(RQ_WRITE_EXT_SAVE)
                REQUEST_CODE_SAVE_DIALOG_NEW_LESSON -> showPermissionDialog(RQ_WRITE_EXT_SAVE_NEW)
                REQUEST_CODE_SAVE_DIALOG_OPEN -> showPermissionDialog(RQ_WRITE_EXT_OPEN)
            }
        } else {
            checkLessonNameThenSave(requestCode)
        }
    }

    private fun checkLessonNameThenSave(requestCode: Int) {
        val fileName = dataManager.getReadableCurrentFileName()
        if (fileName == Constants.DEFAULT_FILE_NAME) {
            val saveAsDialog = SaveAsDialog(object : SaveAsCallback {
                override fun okClicked(fileName: String) {
                    dataManager.setNewFileName(fileName)
                    invalidateOptionsMenu()
                    saveLesson(true, requestCode)
                }

                override fun cancelClicked() {
                    // TODO
                }
            })
            saveAsDialog.show(supportFragmentManager, "SaveAs")
        } else {
            saveLesson(false, requestCode)
        }
    }

    private fun saveLesson(isNewFile: Boolean, requestCode: Int) {
        val result = dataManager.writeLessonToFile(isNewFile)
        if (result.successful) {
            saveFinished(requestCode)
        } else {
            toaster.showToast(
                context,
                result.errorMessage ?: result.errorMessageRes?.let { getString(it) }
                ?: getString(R.string.saving_error),
                Toast.LENGTH_LONG
            )
        }
    }

    // Menu clicks
    fun mSaveFileClicked(menuItem: MenuItem) {
        checkSavePermissionThenSave(REQUEST_CODE_SAVE_DIALOG_NORMAL)
    }

    fun mOpenSearchClicked(menuItem: MenuItem) {
        (menuItem.actionView as SearchView).isIconified = false
    }

    fun mOpenLessonClicked(menuItem: MenuItem) {
        openLesson()
    }

    fun mNewLessonClicked(menuItem: MenuItem) {
        if (dataManager.saveRequired) {
            val builder = AlertDialog.Builder(context)
            builder.setTitle(R.string.lesson_not_saved_dialog_title)
                .setMessage(R.string.save_lesson_before_question)
                .setPositiveButton(R.string.save) { _, _ ->
                    checkSavePermissionThenSave(REQUEST_CODE_SAVE_DIALOG_NEW_LESSON)
                }
                .setNeutralButton(R.string.no) { _, _ -> createNewLesson() }
            builder.create().show()
        } else createNewLesson()
    }

    fun mResetLessonClicked(menuItem: MenuItem) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(R.string.reset_lesson_dialog_title)
            .setMessage(R.string.reset_lesson_dialog_info)
            .setPositiveButton(R.string.reset) { dialog, _ ->
                lessonManager.resetLongTermBatches()
                dataManager.saveRequired = true
                initButtons()
                initChartList()
                initView()
                toaster.showToast(
                    context as Activity,
                    R.string.lektion_zurückgesetzt,
                    Toast.LENGTH_SHORT
                )
                dialog.cancel()
            }
            .setNeutralButton(R.string.cancel) { dialog, _ -> dialog.cancel() }
        builder.create().show()
    }

    fun mFlipSidesClicked(menuItem: MenuItem) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(R.string.reverse_sides_dialog_title)
            .setMessage(R.string.reverse_sides_dialog_info)
            .setPositiveButton(R.string.flip_cards) { dialog, _ ->
                lessonManager.flipAllCards()
                dataManager.saveRequired = true
                initButtons()
                initChartList()
                initView()
                toaster.showToast(
                    context as Activity,
                    R.string.flip_sides_complete,
                    Toast.LENGTH_SHORT
                )
                dialog.cancel()
            }
            .setNeutralButton(R.string.cancel) { dialog, _ -> dialog.cancel() }
        builder.create().show()
    }

    fun mEditInfoTextClicked(menuItem: MenuItem) {
        startActivity(Intent(context, EditDescription::class.java))
        overridePendingTransition(R.anim.slide_in_bottom, R.anim.stay)
    }

    fun mSettingsClicked(menuItem: MenuItem) {
        startActivity(Intent(context, PaukerSettings::class.java))
    }

    // Button clicks

    fun addNewCard(view: View) {
        startActivity(Intent(context, AddCard::class.java))
    }

    fun learnNewCard(view: View) {
        if (settingsManager.getBoolPreference(HIDE_TIMES)) {
            setLearningPhase(SIMPLE_LEARNING)
        } else {
            setLearningPhase(FILLING_USTM)
        }
        lessonManager.setupCurrentPack()

        startActivity(Intent(context, LearnCards::class.java))
    }

    fun repeatCards(view: View) {
        setLearningPhase(REPEATING_LTM)
        lessonManager.setupCurrentPack()

        startActivity(Intent(context, LearnCards::class.java))
    }

    // Notification
    fun createNotificationChannels() {
        // Für Notification
        createNotificationChannel(
            getString(R.string.channel_notify_name_other),
            null,
            NotificationManager.IMPORTANCE_DEFAULT,
            NOTIFICATION_CHANNEL_ID,
            true
        )

        // Timerbar
        createNotificationChannel(
            getString(R.string.channel_timerbar_name),
            getString(R.string.channel_timerbar_description),
            NotificationManager.IMPORTANCE_LOW,
            TIMER_BAR_CHANNEL_ID,
            false
        )

        // Falls der TimerChannel noch existiert
        val notificationManager = getSystemService(NotificationManager::class.java)
        if (notificationManager != null) {
            notificationManager.deleteNotificationChannel("Timers")
            Log.d("MainMenu::createNotificationChannel", "Timer channel deleted")
        }
    }

    private fun createNotificationChannel(
        channelName: String,
        description: String?,
        importance: Int,
        ID: String,
        playSound: Boolean
    ) {
        val channel = NotificationChannel(ID, channelName, importance)

        if (description != null) {
            channel.description = description
        }

        if (!playSound) {
            channel.setSound(null, null)
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.createNotificationChannel(channel)
        Log.d(
            "AlamNotificationReceiver::createNotificationChannel",
            "Channel created: $channelName"
        )
    }
}