package com.daniel.mobilepauker2.main

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.Settings
import android.text.method.ScrollingMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.daniel.mobilepauker2.core.PaukerManager
import com.daniel.mobilepauker2.R
import com.daniel.mobilepauker2.browse.SearchActivity
import com.daniel.mobilepauker2.editor.EditDescrptionActivity
import com.daniel.mobilepauker2.lessonexport.SaveDialog
import com.daniel.mobilepauker2.lessonimport.LessonImportActivity
import com.daniel.mobilepauker2.core.notification.NotificationService
import com.daniel.mobilepauker2.settings.SettingsActivity
import com.daniel.mobilepauker2.main.statistics.ChartAdapter
import com.daniel.mobilepauker2.main.statistics.ChartAdapter.ChartAdapterCallback
import com.daniel.mobilepauker2.core.Constants
import com.daniel.mobilepauker2.core.PaukerApplication
import com.daniel.mobilepauker2.pauker_native.ErrorReporter
import com.daniel.mobilepauker2.pauker_native.Log
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelState
import org.koin.android.ext.android.get
import org.koin.android.viewmodel.ext.android.viewModel

/**
 * Created by Daniel on 24.02.2018.
 * Masterarbeit++ - Intuitiv, plattformübergreifend lernen
 * Daniel Fritsch
 * hs-augsburg
 */
@Suppress("UNUSED_PARAMETER")
open class MainMenu : PaukerApplication() {
    private val context: Context = this
    private var firstStart = true
    private var search: MenuItem? = null
    private var chartView: RecyclerView? = null

    private val viewModel: MainMenuViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Channel erstellen, falls noch nicht vorhanden
        Log.d(
                "AlamNotificationReceiver::onReceive",
                "Create Channels"
        )
        createNotificationChannels()
        val action = intent.action
        if (action != null && action == "Open Lesson") {
            startActivity(Intent(context, LessonImportActivity::class.java))
        }
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)
        setContentView(R.layout.main_menu)
        checkErrors()
        if (!viewModel.isLessonSetup()) {
            viewModel.createNewLesson()
        }
        init()
    }

    /**
     * Prüft ob Errors vorliegen und fragt gegebenenfalls ob diese gesendet werden sollen.
     */
    private fun checkErrors() {
        val errorReporter: ErrorReporter = get()
        if (errorReporter.isThereAnyErrorsToReport) {
            val alt_bld = AlertDialog.Builder(this)
            alt_bld.setTitle(getString(R.string.crash_report_title))
                    .setMessage(getString(R.string.crash_report_message))
                    .setCancelable(false)
                    .setPositiveButton(
                            getString(R.string.ok)
                    ) { _, _ -> errorReporter.checkErrorAndSendMail() }
                    .setNeutralButton(
                            getString(R.string.cancel)
                    ) { dialog, _ ->
                        errorReporter.deleteErrorFiles()
                        dialog.cancel()
                    }
            val alert = alt_bld.create()
            alert.setIcon(R.mipmap.ic_launcher)
            alert.show()
        }
    }

    private fun initButtons() {
        val hasCardsToLearn = viewModel.hasCardsToLearn()
        val hasExpiredCards = viewModel.hasExpiredCards()
        findViewById<View>(R.id.bLearnNewCard).isEnabled = hasCardsToLearn
        findViewById<View>(R.id.bLearnNewCard).isClickable = hasCardsToLearn
        findViewById<View>(R.id.tLearnNewCardDesc).isEnabled = hasCardsToLearn
        findViewById<View>(R.id.bRepeatExpiredCards).isEnabled = hasExpiredCards
        findViewById<View>(R.id.bRepeatExpiredCards).isClickable = hasExpiredCards
        findViewById<View>(R.id.tRepeatExpiredCardsDesc).isEnabled = hasExpiredCards
    }

    private fun initView() {
        invalidateOptionsMenu()
        val description = viewModel.getDescription()
        val descriptionView = findViewById<TextView>(R.id.infoText)
        descriptionView.text = description
        if (description.isNotEmpty()) {
            descriptionView.movementMethod = ScrollingMovementMethod()
        }
        val drawer = findViewById<SlidingUpPanelLayout>(R.id.drawerPanel)
        if (drawer != null) {
            drawer.addPanelSlideListener(object : SlidingUpPanelLayout.PanelSlideListener {
                override fun onPanelSlide(
                        panel: View,
                        slideOffset: Float
                ) {
                }

                override fun onPanelStateChanged(
                        panel: View,
                        previousState: PanelState,
                        newState: PanelState
                ) {
                    if (newState == PanelState.EXPANDED) findViewById<View>(R.id.drawerImage).rotation =
                            180f
                    if (newState == PanelState.COLLAPSED) findViewById<View>(R.id.drawerImage).rotation =
                            0f
                }
            })
            drawer.panelState = PanelState.COLLAPSED
        }

        title = viewModel.getTitle()
    }

    private fun initChartList() { // Im Thread laufen lassen um MainThread zu entlasten
        val initThread = Thread(Runnable {
            chartView = findViewById(R.id.chartListView)
            chartView?.let {
                val layoutManager = LinearLayoutManager(
                        context,
                        LinearLayoutManager.HORIZONTAL, false
                )
                it.layoutManager = layoutManager
                it.overScrollMode = View.OVER_SCROLL_NEVER
                it.isScrollContainer = true
                it.isNestedScrollingEnabled = true
                runOnUiThread {
                    val onClickListener =
                            object : ChartAdapterCallback {
                                override fun onClick(position: Int) {
                                    viewModel.showBatchDetails(position)
                                }
                            }
                    val adapter = ChartAdapter(context, onClickListener)
                    it.adapter = adapter
                }
            }
        })
        initThread.run()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        val save = menu.findItem(R.id.mSaveFile)
        search = menu.findItem(R.id.mSearch)
        val open = menu.findItem(R.id.mOpenLesson)
        menu.setGroupEnabled(
                R.id.mGroup,
                viewModel.isLessonNotNew() || !viewModel.isLessonAndDescriptionEmpty()
        )
        if (viewModel.isLessonEmpty()) {
            search?.isVisible = true
            open.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        } else {
            search?.isVisible = false
            open.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }
        save.isVisible = viewModel.isSaveRequired()
        if (search?.isVisible == true) {
            val searchView =
                    search?.actionView as SearchView
            searchView.setIconifiedByDefault(false)
            searchView.isIconified = false
            searchView.queryHint = getString(R.string.search_hint)
            searchView.setOnQueryTextListener(object :
                    SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String): Boolean {
                    val browseIntent = Intent(Intent.ACTION_SEARCH)
                    browseIntent.setClass(context, SearchActivity::class.java)
                    browseIntent.putExtra(SearchManager.QUERY, query)
                    startActivity(browseIntent)
                    searchView.setQuery("", false)
                    return true
                }

                override fun onQueryTextChange(newText: String): Boolean {
                    return false
                }
            })
            searchView.setOnQueryTextFocusChangeListener { _, hasFocus -> if (!hasFocus) searchView.clearFocus() }
        }
        return true
    }

    override fun onPause() {
        chartView = null
        super.onPause()
    }

    public override fun onResume() {
        Log.d("MainMenuActivity::onResume", "ENTRY")
        super.onResume()
        viewModel.resetLesson()

        if (search != null) {
            search!!.collapseActionView()
        }

        if (!firstStart) {
            init()
            invalidateOptionsMenu()
        }

        firstStart = false
        isPaukerActive = true

        NotificationManagerCompat.from(context).cancelAll()
    }

    override fun onBackPressed() {
        if (viewModel.isSaveRequired()) {
            val builder = AlertDialog.Builder(context)
            builder.setMessage(R.string.close_without_saving_dialog_msg)
                    .setPositiveButton(R.string.cancel, null)
                    .setNeutralButton(R.string.close) { _, _ -> finish() }
            val dialog = builder.create()
            dialog.show()
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                    .setTextColor(getColor(R.color.unlearned))
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(getColor(R.color.learned))
        } else super.onBackPressed()
    }

    override fun onDestroy() {
        NotificationService.enqueueWork(context)
        isPaukerActive = false
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
            requestCode: Int, permissions: Array<String>,
            grantResults: IntArray
    ) {
        if (requestCode == RQ_WRITE_EXT_OPEN && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            openLesson()
        }
        if (requestCode == RQ_WRITE_EXT_SAVE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            saveLesson(Constants.REQUEST_CODE_SAVE_DIALOG_NORMAL)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == Constants.REQUEST_CODE_SAVE_DIALOG_NORMAL) {
            if (resultCode == Activity.RESULT_OK) {
                viewModel.saveLesson()
            }
            invalidateOptionsMenu()
        } else if (requestCode == Constants.REQUEST_CODE_SAVE_DIALOG_NEW_LESSON && resultCode == Activity.RESULT_OK) {
            createNewLesson()
        } else if (requestCode == Constants.REQUEST_CODE_SAVE_DIALOG_OPEN && resultCode == Activity.RESULT_OK) {
            startActivity(Intent(context, LessonImportActivity::class.java))
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    fun addNewCard(view: View?) {
        viewModel.addNewCardClicked()
    }

    fun learnNewCard(view: View?) {
        viewModel.learnNewCardClicked()
    }

    fun repeatCards(view: View?) {
        viewModel.repeatCardsClicked()
    }

    /**
     * Speichert die Lektion.
     * @param requestCode Wird für onActivityResult benötigt
     */
    private fun saveLesson(requestCode: Int) {
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            checkPermission(RQ_WRITE_EXT_SAVE)
        } else {
            viewModel.saveLessonForResult(requestCode)
        }
    }

    /**
     * Fragt, wenn notwendig, die Permission ab und zeigt davor einen passenden Infodialog an.
     */
    private fun openLesson() {
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            checkPermission(RQ_WRITE_EXT_OPEN)
        } else {
            if (viewModel.isSaveRequired()) {
                val builder =
                        AlertDialog.Builder(context)
                builder.setTitle(R.string.lesson_not_saved_dialog_title)
                        .setMessage(R.string.save_lesson_before_question)
                        .setPositiveButton(R.string.save) { _, _ ->
                            saveLesson(
                                    Constants.REQUEST_CODE_SAVE_DIALOG_OPEN
                            )
                        }
                        .setNeutralButton(
                                R.string.open_lesson
                        ) { dialog, _ ->
                            startActivity(Intent(context, LessonImportActivity::class.java))
                            dialog.dismiss()
                        }
                builder.create().show()
            } else startActivity(Intent(context, LessonImportActivity::class.java))
        }
    }

    /**
     * Erstellt eine neue Lektion
     */
    private fun createNewLesson() {
        viewModel.createNewLesson()
        viewModel.setupNewLesson()
        init()
        PaukerManager.showToast(
                context as Activity,
                R.string.new_lession_created,
                Toast.LENGTH_SHORT
        )
    }

    /**
     * Fragt den Nutzer nach der Erlaubnis.
     * @param requestCode Welche Funktion danach ausgeführt werden soll.
     */
    private fun checkPermission(requestCode: Int) {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        val builder = AlertDialog.Builder(context)
        builder.setTitle(R.string.app_name)
                .setPositiveButton(R.string.next) { dialog, _ ->
                    pref.edit().putBoolean("FirstTime", false).apply()
                    requestPermissions(
                            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                            requestCode
                    )
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
                            val intent = Intent()
                            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            val uri =
                                    Uri.fromParts("package", packageName, null)
                            intent.data = uri
                            startActivity(intent)
                            dialog.dismiss()
                        }
            }
        }
        val dialog = builder.create()
        dialog.show()
    }

    fun mSaveFileClicked(ignored: MenuItem?) {
        saveLesson(Constants.REQUEST_CODE_SAVE_DIALOG_NORMAL)
    }

    /**
     * Aktion des Menubuttons
     * @param ignored Nicht benötigt
     */
    fun mOpenLessonClicked(ignored: MenuItem?) {
        openLesson()
    }

    fun mResetLessonClicked(item: MenuItem?) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(R.string.reset_lesson_dialog_title)
                .setMessage(R.string.reset_lesson_dialog_info)
                .setPositiveButton(R.string.reset) { dialog, _ ->
                    viewModel.forgetCards()
                    init()
                    PaukerManager.showToast(
                            context as Activity,
                            R.string.lektion_zurückgesetzt,
                            Toast.LENGTH_SHORT
                    )
                    dialog.cancel()
                }
                .setNeutralButton(R.string.cancel) { dialog, _ -> dialog.cancel() }
        builder.create().show()
    }

    fun mNewLessonClicked(item: MenuItem?) {
        if (viewModel.isSaveRequired()) {
            val builder = AlertDialog.Builder(context)
            builder.setTitle(R.string.lesson_not_saved_dialog_title)
                    .setMessage(R.string.save_lesson_before_question)
                    .setPositiveButton(R.string.save) { _, _ -> saveLesson(Constants.REQUEST_CODE_SAVE_DIALOG_NEW_LESSON) }
                    .setNeutralButton(R.string.no) { _, _ -> createNewLesson() }
            builder.create().show()
        } else createNewLesson()
    }

    fun mEditInfoTextClicked(ignored: MenuItem?) {
        startActivity(Intent(context, EditDescrptionActivity::class.java))
        overridePendingTransition(R.anim.slide_in_bottom, R.anim.stay)
    }

    fun mSettingsClicked(item: MenuItem?) {
        startActivity(Intent(context, SettingsActivity::class.java))
    }

    fun mOpenSearchClicked(item: MenuItem) {
        val searchView =
                item.actionView as SearchView
        searchView.isIconified = false
    }

    fun mFlipSidesClicked(item: MenuItem?) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(R.string.reverse_sides_dialog_title)
                .setMessage(R.string.reverse_sides_dialog_info)
                .setPositiveButton(R.string.flip_cards) { dialog, _ ->
                    viewModel.flipCards()
                    init()
                    PaukerManager.showToast(
                            context as Activity,
                            R.string.flip_sides_complete,
                            Toast.LENGTH_SHORT
                    )
                    dialog.cancel()
                }
                .setNeutralButton(R.string.cancel) { dialog, _ -> dialog.cancel() }
        builder.create().show()
    }

    private fun init() {
        initButtons()
        initChartList()
        initView()
    }

    /**
     * Erstellt alle Channels
     */
    private fun createNotificationChannels() { // Für Notification
        createNotificationChannel(
                getString(R.string.channel_notify_name_other),
                null,
                NotificationManager.IMPORTANCE_DEFAULT,
                Constants.NOTIFICATION_CHANNEL_ID,
                true
        )
        // Timerbar
        createNotificationChannel(
                getString(R.string.channel_timerbar_name),
                getString(R.string.channel_timerbar_description),
                NotificationManager.IMPORTANCE_LOW,
                Constants.TIMER_BAR_CHANNEL_ID,
                false
        )
        // Falls der TimerChannel noch existiert
        val notificationManager = getSystemService(
                NotificationManager::class.java
        )
        if (notificationManager != null) {
            notificationManager.deleteNotificationChannel("Timers")
            Log.d(
                    "MainMenu::createNotificationChannel",
                    "Timer channel deleted"
            )
        }
    }

    /**
     * Hilfsmethode zum Erstellen der Channels
     */
    private fun createNotificationChannel(
            channelName: String,
            description: String?,
            importance: Int,
            ID: String?,
            playSound: Boolean
    ) {
        val channel = NotificationChannel(ID, channelName, importance)
        if (description != null) {
            channel.description = description
        }
        if (!playSound) {
            channel.setSound(null, null)
        }
        val notificationManager = getSystemService(
                NotificationManager::class.java
        )
        notificationManager?.createNotificationChannel(channel)
        Log.d(
                "AlamNotificationReceiver::createNotificationChannel",
                "Channel created: $channelName"
        )
    }

    companion object {
        private const val RQ_WRITE_EXT_SAVE = 98
        private const val RQ_WRITE_EXT_OPEN = 99
        var isPaukerActive = false
    }
}