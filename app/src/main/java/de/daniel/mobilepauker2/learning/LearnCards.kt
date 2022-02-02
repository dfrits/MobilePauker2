package de.daniel.mobilepauker2.learning

import android.app.Activity
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.*
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat.*
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.res.ResourcesCompat
import androidx.preference.PreferenceManager
import com.danilomendes.progressbar.InvertedTextProgressbar
import de.daniel.mobilepauker2.R
import de.daniel.mobilepauker2.application.PaukerApplication
import de.daniel.mobilepauker2.data.DataManager
import de.daniel.mobilepauker2.editcard.EditCard
import de.daniel.mobilepauker2.learning.TimerService.LocalBinder
import de.daniel.mobilepauker2.lesson.batch.BatchType
import de.daniel.mobilepauker2.lesson.card.CardPackAdapter
import de.daniel.mobilepauker2.lesson.card.FlashCard.SideShowing.SIDE_A
import de.daniel.mobilepauker2.lesson.card.FlashCard.SideShowing.SIDE_B
import de.daniel.mobilepauker2.models.Font
import de.daniel.mobilepauker2.models.LearningPhase
import de.daniel.mobilepauker2.models.LearningPhase.*
import de.daniel.mobilepauker2.models.LearningPhase.Companion.currentPhase
import de.daniel.mobilepauker2.models.view.MPEditText
import de.daniel.mobilepauker2.models.view.MPTextView
import de.daniel.mobilepauker2.settings.SettingsManager
import de.daniel.mobilepauker2.settings.SettingsManager.Keys.*
import de.daniel.mobilepauker2.utils.Constants
import de.daniel.mobilepauker2.utils.Constants.NOTIFICATION_CHANNEL_ID
import de.daniel.mobilepauker2.utils.Constants.NOTIFICATION_ID
import de.daniel.mobilepauker2.utils.Constants.TIME_BAR_ID
import de.daniel.mobilepauker2.utils.ErrorReporter
import de.daniel.mobilepauker2.utils.Log
import de.daniel.mobilepauker2.utils.Toaster
import de.daniel.mobilepauker2.utils.Utility.Companion.isAppRunning
import java.util.*
import javax.inject.Inject

class LearnCards : FlashCardSwipeScreen() {
    private val context: Context = this
    private var pendingIntent: Intent? = null
    private var notificationManager: NotificationManagerCompat? = null
    private var flipCardSides = false
    private var completedLearning = false
    private var repeatingLTM = false
    private var stopWaiting = false
    private var firstStart = true
    private var timerServiceConnection: ServiceConnection? = null
    private var timerService: TimerService? = null
    private lateinit var timerServiceIntent: Intent
    private lateinit var ustmTimerBar: InvertedTextProgressbar
    private lateinit var stmTimerBar: InvertedTextProgressbar
    private var bNext: Button? = null
    private var bShowMe: Button? = null
    private var lRepeatButtons: RelativeLayout? = null
    private var lSkipWaiting: RelativeLayout? = null
    private var pauseButton: MenuItem? = null
    private var restartButton: MenuItem? = null
    private var timerAnimation: RelativeLayout? = null
    private var ustmTimerText: String? = null

    @Inject
    lateinit var errorReporter: ErrorReporter

    @Inject
    lateinit var toaster: Toaster

    @Inject
    lateinit var dataManager: DataManager

    @Inject
    lateinit var settingsManager: SettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isRunning = true

        (applicationContext as PaukerApplication).applicationSingletonComponent.inject(this)

        @Suppress("Deprecation")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        init()
    }

    override fun updateCurrentCard() {
        try {
            if (isCardCursorAvailable()) {
                currentCard.sideAText = mCardCursor.getString(CardPackAdapter.KEY_SIDEA_ID)
                currentCard.sideBText = mCardCursor.getString(CardPackAdapter.KEY_SIDEB_ID)
                val learnStatus = mCardCursor.getString(CardPackAdapter.KEY_LEARN_STATUS_ID)
                currentCard.isLearned = learnStatus!!.contentEquals("1")
            } else {
                currentCard.sideAText = ""
                currentCard.sideBText = ""
                Log.d(
                    "FlashCardSwipeScreenActivity::updateCurrentCard",
                    "Card Cursor not available"
                )
            }
        } catch (e: Exception) {
            Log.e("FlashCardSwipeScreenActivity::updateCurrentCard", "Caught Exception")
            toaster.showToast(
                context as Activity,
                R.string.load_card_data_error,
                Toast.LENGTH_SHORT
            )
            errorReporter.addCustomData("LearnCardsActivity::updateCurrentCard", "cursor problem?")
            finish()
        }
    }

    override fun screenTouched() {
        timerService?.let {
            if (it.isUstmTimerPaused() || it.isStmTimerPaused()) return
        }

        if (currentPhase == REPEATING_LTM || currentPhase == REPEATING_STM || currentPhase == REPEATING_USTM) {
            if (lessonManager.getCardFromCurrentPack(mCardCursor.position)!!.isRepeatedByTyping) {
                showInputDialog()
            } else {
                if (flipCardSides) {
                    currentCard.side = SIDE_A
                } else {
                    currentCard.side = SIDE_B
                }
                fillInData(flipCardSides)
                bShowMe!!.visibility = View.GONE
                lRepeatButtons!!.visibility = View.VISIBLE
            }
        }
    }

    override fun fillData() {
        // Prüfen, ob getauscht werden soll
        flipCardSides = hasCardsToBeFlipped()
        if (flipCardSides) {
            currentCard.side = SIDE_B
        } else {
            currentCard.side = SIDE_A
        }
        fillInData(flipCardSides)
    }

    override fun cursorLoaded() {
        Log.d(
            "LearnCardsActivity::cursorLoaded", "cursor loaded: " +
                "savedPos= " + mSavedCursorPosition
        )

        if (mSavedCursorPosition == -1) {
            setCursorToFirst()
            updateCurrentCard()
            fillData()
            setButtonsVisibility()
        } else {
            mCardCursor.moveToPosition(mSavedCursorPosition)
            updateCurrentCard()
            fillInData(flipCardSides)
            if (bShowMe!!.visibility == View.VISIBLE
                && (flipCardSides && currentCard.side == SIDE_A
                    || !flipCardSides && currentCard.side == SIDE_B)
            ) {
                bShowMe!!.visibility = View.GONE
                lRepeatButtons!!.visibility = View.VISIBLE
            }
        }
        mSavedCursorPosition = -1
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == Constants.REQUEST_CODE_EDIT_CARD && resultCode == RESULT_OK) {
            updateCurrentCard()
            fillInData(flipCardSides)
            setButtonsVisibility()
            if (bShowMe?.visibility == View.VISIBLE
                && (flipCardSides && currentCard.side == SIDE_A
                    || !flipCardSides && currentCard.side == SIDE_B)
            ) {
                bShowMe!!.visibility = View.GONE
                lRepeatButtons!!.visibility = View.VISIBLE
            }
        } else if (requestCode == Constants.REQUEST_CODE_SAVE_DIALOG_NORMAL) {
            if (resultCode == RESULT_OK) {
                toaster.showToast(context as Activity, R.string.saving_success, Toast.LENGTH_SHORT)
                dataManager.saveRequired = false
                toaster.showExpireToast(context)
            } else {
                toaster.showToast(context as Activity, R.string.saving_error, Toast.LENGTH_SHORT)
            }
            finish()
        }
        pendingIntent = null

        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onBackPressed() {
        Log.d("LearnCardsActivity::onBackPressed", "Back Button pressed")
        val builder = AlertDialog.Builder(context)
        builder.setTitle(R.string.exit_learning_dialog)
            .setPositiveButton(R.string.yes) { _, _ ->
                stopBothTimer()
                Log.d("LearnCardsActivity::onBackPressed", "Finish and Timer stopped")
                finish()
            }
            .setNeutralButton(R.string.cancel) { dialog, _ -> dialog.cancel() }
            .create().show()
    }

    override fun onPause() {
        super.onPause()
        mSavedCursorPosition = try {
            mCardCursor.position
        } catch (e: Exception) {
            -1
        }
    }

    override fun onResume() {
        super.onResume()
        if (notificationManager == null) {
            notificationManager = NotificationManagerCompat.from(context)
        }
        notificationManager?.cancelAll()
        if (!firstStart && !restartButton!!.isVisible) {
            restartTimer()
            if (mSavedCursorPosition != -1) {
                refreshCursor()
            }
        }

        if (currentPhase === WAITING_FOR_USTM || currentPhase === WAITING_FOR_STM) {
            showHideTimerAnimation()
        }
        firstStart = false
    }

    override fun onDestroy() {
        isRunning = false

        if (timerServiceConnection != null) {
            stopService(timerServiceIntent)
            unbindService(timerServiceConnection!!)
        }
        notificationManager?.cancelAll()
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.learning_cards, menu)
        pauseButton = menu.findItem(R.id.mPauseButton)
        restartButton = menu.findItem(R.id.mRestartButton)
        if (currentPhase === REPEATING_LTM || currentPhase === SIMPLE_LEARNING
            || currentPhase === NOTHING || currentPhase === REPEATING_STM
        ) {
            pauseButton?.isVisible = false
            restartButton?.isVisible = false
        }

        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        menu.findItem(R.id.mHideTimer).isChecked = pref.getBoolean(Constants.HIDE_TIMER_KEY, false)
        findViewById<RelativeLayout>(R.id.lTimerFrame).foreground =
            if (pref.getBoolean(Constants.HIDE_TIMER_KEY, false))
                ResourcesCompat.getDrawable(resources, R.color.defaultBackground, null)
            else ResourcesCompat.getDrawable(resources, android.R.color.transparent, null)

        return true
    }

    private fun init() {
        if (currentPhase != REPEATING_LTM
            && (currentPhase != SIMPLE_LEARNING
                || currentPhase != NOTHING)
        ) {
            // A check on mActivitySetupOk is done here as onCreate is called even if the
            // super (FlashCardSwipeScreenActivity) onCreate fails to find any cards and calls finish()
            if (mActivitySetupOk) {
                initTimer()
            }
        } else if (currentPhase === REPEATING_LTM) {
            repeatingLTM = true
        }

        initButtons()
    }

    private fun initTimer() {
        val ustmTotalTime: Int = settingsManager.getStringPreference(USTM)?.toInt() ?: 0
        ustmTimerBar = findViewById(R.id.UKZGTimerBar)
        ustmTimerBar.maxProgress = ustmTotalTime
        ustmTimerBar.minProgress = 0
        val stmTotalTime: Int = settingsManager.getStringPreference(STM)?.toInt() ?: 0
        stmTimerBar = findViewById(R.id.KZGTimerBar)
        stmTimerBar.maxProgress = stmTotalTime * 60
        stmTimerBar.minProgress = 0
        timerServiceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                Log.d("LearnActivity::initTimer", "onServiceConnectedCalled")
                val binder: LocalBinder = service as LocalBinder
                timerService = binder.serviceInstance
                registerListener()
                timerService!!.startUstmTimer()
                timerService!!.startStmTimer()
                findViewById<RelativeLayout>(R.id.lTimerFrame).visibility = View.VISIBLE
                timerAnimation = findViewById(R.id.timerAnimationPanel)
            }

            override fun onServiceDisconnected(name: ComponentName) {
                Log.d("LearnActivity::initTimer", "onServiceDisconnectedCalled")
                stopBothTimer()
            }

            override fun onBindingDied(name: ComponentName) {}
        }
        timerServiceIntent = Intent(context, TimerService::class.java)
        timerServiceIntent.putExtra(TimerService.USTM_TOTAL_TIME, ustmTotalTime)
        timerServiceIntent.putExtra(TimerService.STM_TOTAL_TIME, stmTotalTime)
        startService(timerServiceIntent)
        bindService(timerServiceIntent, timerServiceConnection!!, BIND_AUTO_CREATE)
    }

    private fun stopBothTimer() {
        unregisterListener()
        timerService?.stopUstmTimer()
        timerService?.stopStmTimer()
    }

    private fun registerListener() {
        registerReceiver(ustmTimeBroadcastReceiver, IntentFilter(TimerService.ustm_receiver))
        registerReceiver(stmTimeBroadcastReceiver, IntentFilter(TimerService.stm_receiver))
        registerReceiver(
            ustmFinishedBroadcastReceiver,
            IntentFilter(TimerService.ustm_finished_receiver)
        )
        registerReceiver(
            stmFinishedBroadcastReceiver,
            IntentFilter(TimerService.stm_finished_receiver)
        )
    }

    private fun unregisterListener() {
        try {
            unregisterReceiver(ustmTimeBroadcastReceiver)
            unregisterReceiver(stmTimeBroadcastReceiver)
            unregisterReceiver(ustmFinishedBroadcastReceiver)
            unregisterReceiver(stmFinishedBroadcastReceiver)
        } catch (e: Exception) {
        }
    }

    private fun hasCardsToBeFlipped(): Boolean {
        val flipMode = settingsManager.getStringPreference(FLIP_CARD_SIDES)

        return if (currentPhase == REPEATING_LTM
            || currentPhase == REPEATING_STM
            || currentPhase == REPEATING_USTM
        ) {
            when (flipMode) {
                "1" -> true
                "2" -> {
                    val rand = Random()
                    rand.nextBoolean()
                }
                else -> false
            }
        } else {
            false
        }
    }

    private fun pauseTimer() {
        timerService!!.pauseTimers()
        if (!timerService!!.isStmTimerFinished()) {
            disableButtons()
        }
    }

    private fun restartTimer() {
        timerService!!.restartTimers()
        enableButtons()
    }

    private fun updateLearningPhase() {
        var zeroUnlearnedCards = false
        var zeroUSTMCards = false
        var zeroSTMCards = false
        if (lessonManager.getBatchSize(BatchType.UNLEARNED) <= 0) {
            zeroUnlearnedCards = true
        }
        if (lessonManager.getBatchSize(BatchType.ULTRA_SHORT_TERM) <= 0) {
            zeroUSTMCards = true
        }
        if (lessonManager.getBatchSize(BatchType.SHORT_TERM) <= 0) {
            zeroSTMCards = true
        }
        when (currentPhase) {
            SIMPLE_LEARNING -> {
                if (completedLearning) {
                    finishLearning()
                } else {
                    setButtonVisibilityRepeating()
                }
            }
            FILLING_USTM -> {
                setButtonVisibilityFilling()
                if (timerService!!.isStmTimerFinished()) // STM timeout so go straight to repeating ustm cards
                {
                    setLearningPhase(REPEATING_USTM)
                    updateLearningPhase()
                } else if (zeroUnlearnedCards && !timerService!!.isUstmTimerFinished()) {
                    setLearningPhase(WAITING_FOR_USTM)
                    updateLearningPhase()
                } else if (timerService!!.isUstmTimerFinished()) {
                    setLearningPhase(REPEATING_USTM)
                    updateLearningPhase()
                }
            }
            WAITING_FOR_USTM -> {
                Log.d("LearnCardsActivity::updateLearningPhase", "Waiting for USTM")
                // Gif zeigen
                showHideTimerAnimation()

                // USTM Timeout
                if (timerService!!.isUstmTimerFinished() || stopWaiting) {
                    stopWaiting = false
                    setLearningPhase(REPEATING_USTM)
                    timerService!!.stopUstmTimer()
                    updateLearningPhase()
                }
            }
            REPEATING_USTM -> {
                setButtonsVisibility()
                if (zeroUSTMCards) // We have learned all the ustm cards
                {
                    if (timerService!!.isStmTimerFinished()) //STM timer has timed out so move to repeating STM
                    {
                        setLearningPhase(REPEATING_STM)
                    } else if (!zeroUnlearnedCards) // Unlearned cards available so go back to filling ustm;
                    {
                        setLearningPhase(FILLING_USTM)
                        timerService!!.startUstmTimer()
                    } else {
                        setLearningPhase(WAITING_FOR_STM)
                    }
                    updateLearningPhase()
                } else if (mCardPackAdapter!!.isLastCard) {
                    setLearningPhase(REPEATING_USTM)
                }
            }
            WAITING_FOR_STM -> {

                // Gif zeigen
                showHideTimerAnimation()

                // USTM Timeout
                if (timerService!!.isStmTimerFinished() || stopWaiting) {
                    stopWaiting = false
                    timerService!!.stopStmTimer()
                    setLearningPhase(REPEATING_STM)
                    invalidateOptionsMenu()
                    updateLearningPhase()
                }
            }
            REPEATING_STM -> {
                setButtonsVisibility()
                if (zeroSTMCards) {
                    finishLearning()
                } else if (mCardPackAdapter!!.isLastCard) {
                    setLearningPhase(REPEATING_STM)
                }
            }
            REPEATING_LTM -> {
                if (completedLearning && lessonManager.getBatchSize(BatchType.EXPIRED) <= 0) {
                    finishLearning()
                } else if (completedLearning) {
                    pushCursorToNext()
                } else {
                    setButtonsVisibility()
                }
            }
            else -> {}
        }
    }

    private fun setLearningPhase(newLearningsPhase: LearningPhase) {
        //Neue Phase dem Modelmanager mitteilen und Deck aktualisieren
        LearningPhase.setLearningPhase(newLearningsPhase)
        lessonManager.setupCurrentPack()
        //Cursor an erste Stelle setzen
        mSavedCursorPosition = -1
        refreshCursor()
    }

    private fun initButtons() {
        bNext = findViewById(R.id.bNext)
        bShowMe = findViewById(R.id.bShowMe)
        lRepeatButtons = findViewById(R.id.lBRepeat)
        lSkipWaiting = findViewById(R.id.lBSkipWaiting)
    }

    private fun showHideTimerAnimation() {
        if (timerAnimation == null) return
        timerAnimation!!.visibility = if (stopWaiting) View.GONE else View.VISIBLE
    }

    private fun finishLearning() {
        dataManager.saveRequired = true
        finish()
    }

    private fun setButtonsVisibility() {
        if (currentPhase === WAITING_FOR_USTM || currentPhase === WAITING_FOR_STM) {
            setButtonVisibilityWaiting()
        } else if (currentPhase === SIMPLE_LEARNING || currentPhase === REPEATING_USTM || currentPhase === REPEATING_STM || currentPhase === REPEATING_LTM) {
            setButtonVisibilityRepeating()
        } else if (currentPhase === FILLING_USTM) {
            setButtonVisibilityFilling()
        }
    }

    private fun setButtonVisibilityWaiting() {
        bNext?.visibility = View.GONE
        bShowMe?.visibility = View.GONE
        lRepeatButtons?.visibility = View.GONE
        lSkipWaiting?.visibility = View.VISIBLE
    }

    private fun setButtonVisibilityFilling() {
        bNext!!.visibility = View.VISIBLE
        bShowMe!!.visibility = View.GONE
        lRepeatButtons!!.visibility = View.GONE
        lSkipWaiting!!.visibility = View.GONE
    }

    private fun setButtonVisibilityRepeating() {
        bNext!!.visibility = View.GONE
        bShowMe!!.visibility = View.VISIBLE
        val currentCard = try {
            lessonManager.getCardFromCurrentPack(mCardCursor.position)
        } catch (e: Exception) {
            Log.d("LearnCards::setButtons()", "Cursor not loades yet.")
            toaster.showToast(
                context as Activity,
                R.string.load_card_data_error,
                Toast.LENGTH_SHORT
            )

            finish()
            null
        }
        val text: String =
            if (currentCard?.isRepeatedByTyping == true) getString(R.string.enter_answer)
            else getString(R.string.show_me)

        bShowMe!!.text = text
        lRepeatButtons!!.visibility = View.GONE
        lSkipWaiting!!.visibility = View.GONE
    }

    private fun disableButtons() {
        bNext!!.isEnabled = false
        bShowMe!!.isEnabled = false
        findViewById<Button>(R.id.bYes).isEnabled = false
        findViewById<Button>(R.id.bNo).isEnabled = false
        findViewById<Button>(R.id.bSkipWaiting).isEnabled = false
    }

    private fun enableButtons() {
        bNext!!.isEnabled = true
        bShowMe!!.isEnabled = true
        findViewById<Button>(R.id.bYes).isEnabled = true
        findViewById<Button>(R.id.bNo).isEnabled = true
        findViewById<Button>(R.id.bSkipWaiting).isEnabled = true
    }

    private fun showInputDialog() {
        val builder = AlertDialog.Builder(context)
        val view = layoutInflater.inflate(R.layout.dialog_input, null)
        val inputField: MPEditText = view.findViewById(R.id.eTInput)
        builder.setView(view)
            .setPositiveButton(R.string.proof) { dialog, _ ->
                val cardText: String = if (flipCardSides) {
                    currentCard.sideAText
                } else {
                    currentCard.sideBText
                }
                val caseSensitive =
                    settingsManager.getBoolPreference(CASE_SENSITIV)
                val input: String = inputField.text.toString()
                if (caseSensitive && cardText == input) {
                    yesClicked(null)
                } else if (cardText.equals(input, ignoreCase = true)) {
                    yesClicked(null)
                } else showResultDialog(cardText, input)
                dialog.dismiss()
            }
            .setNeutralButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .setOnDismissListener {
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                if (currentFocus != null && imm.isAcceptingText) {
                    imm.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
                }
            }
        if (flipCardSides) {
            inputField.setFont(
                lessonManager.getCardFont(
                    CardPackAdapter.KEY_SIDEA_ID,
                    mCardCursor.position
                )
            )
        } else {
            inputField.setFont(
                lessonManager.getCardFont(
                    CardPackAdapter.KEY_SIDEB_ID,
                    mCardCursor.position
                )
            )
        }
        builder.create().show()
    }

    private fun showResultDialog(cardText: String, input: String) {
        val builder = AlertDialog.Builder(context, R.style.ResultDialogTheme)
        val view = layoutInflater.inflate(R.layout.dialog_result, null)
        builder.setView(view)
            .setPositiveButton(R.string.put_further) { _, _ -> yesClicked(null) }
            .setNeutralButton(R.string.put_back) { _, _ -> noClicked(null) }
            .setCancelable(false)
        (view.findViewById<View>(R.id.tVRightAnswerText) as TextView).text = cardText
        (view.findViewById<View>(R.id.tVInputText) as TextView).text = input
        builder.create().show()
    }

    private fun fillInData(flipCardSides: Boolean) {
        // Daten setzen
        val fontA: Font =
            lessonManager.getCardFont(CardPackAdapter.KEY_SIDEA_ID, mCardCursor.position)
        val fontB: Font =
            lessonManager.getCardFont(CardPackAdapter.KEY_SIDEB_ID, mCardCursor.position)

        // Layoutcontents setzen
        if (flipCardSides) {
            fillSideA(R.string.back, currentCard.sideBText, fontB)
            var sideAText = ""
            if (currentCard.side == SIDE_A || currentPhase == SIMPLE_LEARNING
                || currentPhase == FILLING_USTM
            ) {
                sideAText = currentCard.sideAText
            } else if (lessonManager.getCardFromCurrentPack(mCardCursor.position)?.isRepeatedByTyping == true) {
                sideAText = getString(R.string.tap_enter_answer)
            }
            fillSideB(R.string.front, sideAText, fontA)
        } else {
            // Card sides not flipped so show side A in top box!
            fillSideA(R.string.front, currentCard.sideAText, fontA)
            var sideBText = ""
            if (currentCard.side == SIDE_B || currentPhase == SIMPLE_LEARNING
                || currentPhase == FILLING_USTM
            ) {
                sideBText = currentCard.sideBText
            } else if (lessonManager.getCardFromCurrentPack(mCardCursor.position)?.isRepeatedByTyping == true) {
                sideBText = "Tap to enter your answer."
            }
            fillSideB(R.string.back, sideBText, fontB)
        }
        fillHeader()
    }

    private fun fillSideA(titleResource: Int, text: String, font: Font) {
        findViewById<TextView>(R.id.titelCardSideA).text = getString(titleResource)
        val sideA: MPTextView = findViewById(R.id.tCardSideA)
        sideA.setFont(font)
        sideA.text = text
    }

    private fun fillSideB(titleResource: Int, text: String, font: Font) {
        findViewById<TextView>(R.id.titelCardSideB).text = getString(titleResource)
        val sideB: MPTextView = findViewById(R.id.tCardSideB_TV)
        if (text.isEmpty()
            || lessonManager.getCardFromCurrentPack(mCardCursor.position)?.isRepeatedByTyping == true
        ) {
            sideB.hint = getString(R.string.learncards_show_hint)
            sideB.setFont(Font())
        } else {
            sideB.setFont(font)
        }
        sideB.text = text
    }

    private fun fillHeader() {
        val allCards: TextView = findViewById(R.id.tAllCards)
        val ustmCards: TextView = findViewById(R.id.tUKZGCards)
        val stmCards: TextView = findViewById(R.id.tKZGCards)
        var text: String
        if (repeatingLTM) {
            text = "${getString(R.string.expired)}:%d"
            text = String.format(text, mCardCursor.count - mCardCursor.position)
            allCards.text = text
            ustmCards.visibility = View.GONE
            stmCards.visibility = View.GONE
        } else {
            text = "${getString(R.string.untrained)}:%d"
            text = String.format(text, lessonManager.getBatchSize(BatchType.UNLEARNED))
            allCards.text = text
            text = "${getString(R.string.ustm)}:%d"
            text = String.format(text, lessonManager.getBatchSize(BatchType.ULTRA_SHORT_TERM))
            ustmCards.text = text
            text = "${getString(R.string.stm)}:%d"
            text = String.format(text, lessonManager.getBatchSize(BatchType.SHORT_TERM))
            stmCards.text = text
        }
    }

    private fun pushCursorToNext() {
        if (currentPhase == REPEATING_LTM) {
            lessonManager.setupCurrentPack()
            reloadStack()
        } else {
            mCardCursor.moveToNext()
            updateCurrentCard()
            fillData()
        }
    }

    /* MenuButton clicks */

    fun mEditClicked(item: MenuItem?) {
        val intent = Intent(context, EditCard::class.java)
        intent.putExtra(Constants.CURSOR_POSITION, mCardCursor.position)
        pendingIntent = intent
        startActivityForResult(intent, Constants.REQUEST_CODE_EDIT_CARD)
    }

    fun mDeleteClicked(item: MenuItem?) {
        val builder = AlertDialog.Builder(context)
        builder.setMessage(R.string.delete_card_message)
            .setPositiveButton(R.string.delete) { dialog, _ ->
                // Muss vorher gespeichert werden, da sonst im Nachhinein der Wert
                // verfälscht werden kann!
                val isLast = mCardCursor.isLast
                if (lessonManager.deleteCard(mCardCursor.position)) {
                    if (isLast) {
                        // Letzte Karte oder Timer abgelaufen. Darum Lernphase aktualisieren
                        updateLearningPhase()
                    } else {
                        updateCurrentCard()
                        fillData()
                        setButtonsVisibility()
                    }
                } else {
                    toaster.showToast(
                        context as Activity,
                        R.string.deleting_impossible,
                        Toast.LENGTH_SHORT
                    )
                }
                dialog.cancel()
            }
            .setNeutralButton(R.string.cancel) { dialog, _ -> dialog.cancel() }
        builder.create().show()
    }

    fun mPauseTimerClicked(item: MenuItem) {
        if (restartButton != null && !timerService!!.isStmTimerFinished()) {
            pauseTimer()
            item.isVisible = false
            restartButton!!.isVisible = true
        } else {
            toaster.showToast(context as Activity, R.string.pause_timer_error, Toast.LENGTH_LONG)
        }
    }

    fun mRestartTimerClicked(item: MenuItem) {
        if (pauseButton != null) {
            restartTimer()
            item.isVisible = false
            pauseButton!!.isVisible = true
        } else {
            toaster.showToast(context as Activity, R.string.restart_timer_error, Toast.LENGTH_LONG)
        }
    }

    fun mFlipSidesClicked(item: MenuItem?) {
        lessonManager.getCardFromCurrentPack(mCardCursor.position)?.flipSides()
        dataManager.saveRequired = true
        updateCurrentCard()

        if (currentPhase == REPEATING_LTM || currentPhase == REPEATING_STM || currentPhase == REPEATING_USTM) {
            flipCardSides = !flipCardSides
            if (flipCardSides) {
                currentCard.side = SIDE_B
            } else {
                currentCard.side = SIDE_A
            }
        }
        fillInData(flipCardSides)
        setButtonsVisibility()
    }

    fun mHideTimerClicked(item: MenuItem?) {
        val checked = item?.isChecked ?: false
        findViewById<RelativeLayout>(R.id.lTimerFrame).foreground =
            if (!checked) ResourcesCompat.getDrawable(resources, R.color.defaultBackground, null)
            else ResourcesCompat.getDrawable(resources, android.R.color.transparent, null)
        item?.isChecked = !checked

        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putBoolean(Constants.HIDE_TIMER_KEY, !checked).apply()
    }

    /* Button clicks */
    fun nextCard(view: View?) {
        // Karte ein Deck weiterschieben
        mCardPackAdapter!!.setCardLearned()
        if (!mCardCursor.isLast && !timerService!!.isUstmTimerFinished()) {
            pushCursorToNext()
        } else {
            // Letzte Karte oder Timer abgelaufen. Darum Lernphase aktualisieren
            updateLearningPhase()
        }
    }

    fun skipWaiting(view: View?) {
        stopWaiting = true
        updateLearningPhase()
    }

    fun noClicked(view: View?) {
        mCardPackAdapter!!.setCardUnLearned()
        dataManager.saveRequired = true
        if (!mCardCursor.isLast) {
            pushCursorToNext()
        } else {
            completedLearning = true
        }
        updateLearningPhase()
    }

    fun yesClicked(view: View?) {
        mCardPackAdapter!!.setCardLearned()
        dataManager.saveRequired = true
        if (!mCardCursor.isLast) {
            pushCursorToNext()
        } else {
            completedLearning = true
        }
        updateLearningPhase()
    }

    fun showCard(view: View?) {
        screenTouched()
    }

    //Broadcast Receiver
    private val ustmFinishedBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            runOnUiThread {
                Log.d("LearnActivity::USTM-Timer finished", "Timer finished")
                ustmTimerBar.setProgress(timerService!!.getUstmTotalTime() * 60)
                ustmTimerBar.text = " "
                if (currentPhase == WAITING_FOR_USTM) {
                    Log.d("LearnActivity::onUSTMTimerFinish", "USTM Timer finished, stop waiting!")
                    stopWaiting = true
                    updateLearningPhase()
                }
            }
        }
    }
    private val stmFinishedBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            runOnUiThread {
                Log.d("LearnActivity::STM-Timer finished", "Timer finished")
                notificationManager!!.cancel(TIME_BAR_ID)
                stmTimerBar.text = " "
                stmTimerBar.setProgress(timerService!!.getStmTotalTime() * 60)
                if (pauseButton != null) {
                    pauseButton!!.isVisible = false
                }
                if (currentPhase == WAITING_FOR_STM) {
                    Log.d("Learnactivity::onSTMTimerFinish", "STM Timer finished, stop waiting!")
                    stopWaiting = true
                    updateLearningPhase()
                }

                // Ist die App pausiert, soll in der Titelleiste die Zeit angezeigt werden
                val showNotify = settingsManager.getBoolPreference(SHOW_TIMER_BAR)
                if (!isAppRunning(context) && timerService!!.isStmTimerFinished() && showNotify) {
                    Log.d("LearnActivity::STM-Timer finished", "Acivity is visible")
                    val mBuilder: Builder =
                        Builder(context, NOTIFICATION_CHANNEL_ID)
                            .setContentText(getString(R.string.stm_expired_notify_message))
                            .setSmallIcon(R.drawable.notify_icon)
                            .setContentTitle(getString(R.string.app_name))
                            .setPriority(PRIORITY_DEFAULT)
                            .setContentIntent(PendingIntent.getActivity(context, 0, getIntent(), 0))
                            .setAutoCancel(true)
                            .setVisibility(VISIBILITY_PUBLIC)
                    Log.d("LearnActivity::STM-Timer finished", "Notification created")
                    Timer().schedule(object : TimerTask() {
                        override fun run() {
                            notificationManager!!.notify(NOTIFICATION_ID, mBuilder.build())
                        }
                    }, 1000)
                    Log.d("LearnActivity::STM-Timer finished", "Notification shown")
                }
            }
        }
    }
    private val ustmTimeBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val timeElapsed = intent.getIntExtra(TimerService.ustm_time, 0)
            runOnUiThread {
                if (ustmTimerBar.visibility == View.VISIBLE && !timerService!!.isUstmTimerFinished()) {
                    val sec = timeElapsed % 60
                    ustmTimerText = java.lang.String.format(
                        Locale.getDefault(),
                        "%d / %ds",
                        sec,
                        timerService!!.getUstmTotalTime()
                    )
                    ustmTimerBar.setProgress(timeElapsed)
                    ustmTimerBar.text = ustmTimerText
                }
            }
        }
    }
    private val stmTimeBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val timeElapsed = intent.getIntExtra(TimerService.stm_time, 0)
            runOnUiThread {
                val timerText: String
                val sec = timeElapsed % 60
                val min = timeElapsed / 60
                timerText = if (sec < 10) {
                    java.lang.String.format(
                        Locale.getDefault(),
                        "%d:0%d / %d:00min", min, sec, timerService!!.getStmTotalTime()
                    )
                } else {
                    java.lang.String.format(
                        Locale.getDefault(),
                        "%d:%d / %d:00min", min, sec, timerService!!.getStmTotalTime()
                    )
                }
                stmTimerBar.setProgress(timeElapsed)
                stmTimerBar.text = timerText

                // Ist die App pausiert, soll in der Titelleiste die Zeit angezeigt werden
                if (!isAppRunning(context) && !timerService!!.isStmTimerFinished()) {
                    Log.d("LearnActivity::STM-onStmTimerUpdate", "Acivity is not visible")
                    val ustmTimerBarText =
                        if (timerService!!.isUstmTimerFinished() && ustmTimerText != null) "" else getString(
                            R.string.ustm
                        ) + " " + ustmTimerText
                    val timerbarText =
                        ustmTimerBarText + "  " + getString(R.string.stm) + " " + timerText
                    val contentIntent = if (pendingIntent == null) getIntent() else pendingIntent!!
                    val mBuilder: Builder = Builder(context, Constants.TIMER_BAR_CHANNEL_ID)
                        .setContentText(timerbarText)
                        .setSmallIcon(R.drawable.notify_icon)
                        .setPriority(PRIORITY_DEFAULT)
                        .setContentIntent(
                            PendingIntent.getActivity(
                                context,
                                0,
                                contentIntent,
                                0
                            )
                        )
                        .setAutoCancel(true)
                        .setOngoing(true)
                    Log.d("LearnActivity::STM-onStmTimerUpdate", "Notification created")
                    notificationManager!!.notify(TIME_BAR_ID, mBuilder.build())
                    Log.d("LearnActivity::STM-onStmTimerUpdate", "Show Notification")
                }
            }
        }
    }

    companion object {
        var isRunning = false
            private set
    }
}