package com.daniel.mobilepauker2.learning

import android.annotation.SuppressLint
import android.app.*
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.daniel.mobilepauker2.core.PaukerManager
import com.daniel.mobilepauker2.R
import com.daniel.mobilepauker2.lessonexport.SaveDialog
import com.daniel.mobilepauker2.editor.EditCardActivity
import com.daniel.mobilepauker2.pauker_native.CardPackAdapter
import com.daniel.mobilepauker2.pauker_native.FlashCard.SideShowing
import com.daniel.mobilepauker2.core.model.ui.MPEditText
import com.daniel.mobilepauker2.core.model.ui.MPTextView
import com.daniel.mobilepauker2.pauker_native.ModelManager.LearningPhase
import com.daniel.mobilepauker2.settings.SettingsManager.Keys
import com.daniel.mobilepauker2.learning.TimerService.LocalBinder
import com.daniel.mobilepauker2.pauker_native.Font
import com.daniel.mobilepauker2.core.Constants
import com.daniel.mobilepauker2.pauker_native.ErrorReporter
import com.daniel.mobilepauker2.pauker_native.Log
import com.danilomendes.progressbar.InvertedTextProgressbar
import java.util.*

class LearnCardsActivity : FlashCardSwipeScreenActivity(),
    TimerService.Callback {
    private val paukerManager: PaukerManager? = PaukerManager.instance()
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
    private var timerServiceIntent: Intent? = null
    private var ustmTimerBar: InvertedTextProgressbar? = null
    private var stmTimerBar: InvertedTextProgressbar? = null
    private var bNext: Button? = null
    private var bShowMe: Button? = null
    private var lRepeatButtons: RelativeLayout? = null
    private var lSkipWaiting: RelativeLayout? = null
    private var pauseButton: MenuItem? = null
    private var restartButton: MenuItem? = null
    private var timerAnimation: RelativeLayout? = null
    private var ustmTimerText: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isLearningRunning = true
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.learn_cards)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (modelManager?.learningPhase != LearningPhase.REPEATING_LTM
            && (modelManager?.learningPhase != LearningPhase.SIMPLE_LEARNING
                    || modelManager.learningPhase != LearningPhase.NOTHING)
        ) { // A check on mActivitySetupOk is done here as onCreate is called even if the
// super (FlashCardSwipeScreenActivity) onCreate fails to find any cards and calls finish()
            if (mActivitySetupOk) {
                initTimer()
            }
        } else if (modelManager.learningPhase == LearningPhase.REPEATING_LTM) {
            repeatingLTM = true
        }
        initButtons()
    }

    public override fun updateCurrentCard() {
        try {
            if (isCardCursorAvailable) {
                currentCard.sideAText =
                    mCardCursor!!.getString(CardPackAdapter.Companion.KEY_SIDEA_ID)
                currentCard.sideBText =
                    mCardCursor!!.getString(CardPackAdapter.Companion.KEY_SIDEB_ID)
                val learnStatus =
                    mCardCursor!!.getString(CardPackAdapter.Companion.KEY_LEARN_STATUS_ID)
                if (learnStatus.contentEquals("1")) {
                    currentCard.isLearned = true
                } else {
                    currentCard.isLearned = false
                }
            } else {
                currentCard.sideAText = ""
                currentCard.sideBText = ""
                Log.d(
                    "FlashCardSwipeScreenActivity::updateCurrentCard",
                    "Card Cursor not available"
                )
            }
        } catch (e: Exception) {
            Log.e(
                "FlashCardSwipeScreenActivity::updateCurrentCard",
                "Caught Exception"
            )
            PaukerManager.Companion.showToast(
                context as Activity,
                R.string.load_card_data_error,
                Toast.LENGTH_SHORT
            )
            ErrorReporter.instance()
                .AddCustomData("LearnCardsActivity::updateCurrentCard", "cursor problem?")
            finish()
        }
    }

    override fun screenTouched() {
        if (timerService != null && (timerService!!.isUstmTimerPaused || timerService!!.isStmTimerPaused)) return
        val learningPhase = modelManager.learningPhase
        if (learningPhase == LearningPhase.REPEATING_LTM || learningPhase == LearningPhase.REPEATING_STM || learningPhase == LearningPhase.REPEATING_USTM
        ) {
            if (modelManager.getCard(mCardCursor!!.position)!!.isRepeatedByTyping) {
                showInputDialog()
            } else {
                if (flipCardSides) {
                    currentCard.side = SideShowing.SIDE_A
                } else {
                    currentCard.side = SideShowing.SIDE_B
                }
                fillInData(flipCardSides)
                bShowMe!!.visibility = View.GONE
                lRepeatButtons!!.visibility = View.VISIBLE
            }
        }
    }

    override fun fillData() { // Prüfen, ob getauscht werden soll
        val flipCardSides = hasCardsToBeFlipped()
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
            mCardCursor!!.moveToPosition(mSavedCursorPosition)
            updateCurrentCard()
            fillInData(flipCardSides)
            if (bShowMe!!.visibility == View.VISIBLE
                && (flipCardSides && currentCard.side == SideShowing.SIDE_A
                        || !flipCardSides && currentCard.side == SideShowing.SIDE_B)
            ) {
                bShowMe!!.visibility = View.GONE
                lRepeatButtons!!.visibility = View.VISIBLE
            }
        }
        mSavedCursorPosition = -1
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == Constants.REQUEST_CODE_EDIT_CARD && resultCode == Activity.RESULT_OK) {
            updateCurrentCard()
            fillInData(flipCardSides)
            setButtonsVisibility()
            if (bShowMe!!.visibility == View.VISIBLE
                && (flipCardSides && currentCard.side == SideShowing.SIDE_A
                        || !flipCardSides && currentCard.side == SideShowing.SIDE_B)
            ) {
                bShowMe!!.visibility = View.GONE
                lRepeatButtons!!.visibility = View.VISIBLE
            }
        } else if (requestCode == Constants.REQUEST_CODE_SAVE_DIALOG_NORMAL) {
            if (resultCode == Activity.RESULT_OK) {
                PaukerManager.Companion.showToast(
                    context as Activity,
                    R.string.saving_success,
                    Toast.LENGTH_SHORT
                )
                paukerManager?.isSaveRequired = false
                modelManager.showExpireToast(context)
            } else {
                PaukerManager.Companion.showToast(
                    context as Activity,
                    R.string.saving_error,
                    Toast.LENGTH_SHORT
                )
            }
            finish()
        }
        pendingIntent = null
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onBackPressed() {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(R.string.exit_learning_dialog)
            .setPositiveButton(R.string.yes) { dialog, which ->
                if (timerService != null) {
                    timerService!!.stopStmTimer()
                    timerService!!.stopUstmTimer()
                    Log.d(
                        "LearnCardsActivity::onBackPressed",
                        "Finish and Timer stopped"
                    )
                } else {
                    Log.d(
                        "LearnCardsActivity::onBackPressed",
                        "Finish and Service is null"
                    )
                }
                finish()
            }
            .setNeutralButton(R.string.cancel) { dialog, which -> dialog.cancel() }
            .create().show()
    }

    override fun onPause() {
        super.onPause()
        isActivityVisible = false
        mSavedCursorPosition = try {
            mCardCursor!!.position
        } catch (e: Exception) {
            -1
        }
    }

    override fun onResume() {
        super.onResume()
        isActivityVisible = true
        if (notificationManager == null) {
            notificationManager = NotificationManagerCompat.from(context)
        }
        notificationManager!!.cancelAll()
        if (!firstStart && !restartButton!!.isVisible) {
            restartTimer()
            if (mCardCursor != null && mSavedCursorPosition != -1) {
                refreshCursor()
            }
        }
        val learningPhase = modelManager.learningPhase
        if (learningPhase == LearningPhase.WAITING_FOR_USTM || learningPhase == LearningPhase.WAITING_FOR_STM) {
            showHideTimerAnimation()
        }
        firstStart = false
    }

    /**
     * Falls die Acitvity vom System beendet wird, die Timer aber noch laufen.
     */
    override fun onDestroy() {
        if (timerServiceConnection != null && timerServiceIntent != null) {
            stopService(timerServiceIntent)
            unbindService(timerServiceConnection)
        }
        val notifyManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notifyManager?.cancelAll()
        isLearningRunning = false
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.learning_cards, menu)
        pauseButton = menu.findItem(R.id.mPauseButton)
        restartButton = menu.findItem(R.id.mRestartButton)
        val learningPhase = modelManager.learningPhase
        if (learningPhase == LearningPhase.REPEATING_LTM || learningPhase == LearningPhase.SIMPLE_LEARNING || learningPhase == LearningPhase.NOTHING || learningPhase == LearningPhase.REPEATING_STM
        ) {
            pauseButton?.setVisible(false)
            restartButton?.setVisible(false)
        }
        return true
    }

    private fun initTimer() {
        val ustmTotalTime = settingsManager.getStringPreference(context, Keys.USTM).toInt()
        ustmTimerBar = findViewById(R.id.UKZGTimerBar)
        ustmTimerBar?.setMaxProgress(ustmTotalTime)
        ustmTimerBar?.setMinProgress(0)
        val stmTotalTime = settingsManager.getStringPreference(context, Keys.STM).toInt()
        stmTimerBar = findViewById(R.id.KZGTimerBar)
        stmTimerBar?.setMaxProgress(stmTotalTime * 60)
        stmTimerBar?.setMinProgress(0)
        timerServiceConnection = object : ServiceConnection {
            override fun onServiceConnected(
                name: ComponentName,
                service: IBinder
            ) {
                Log.d(
                    "LearnActivity::initTimer",
                    "onServiceConnectedCalled"
                )
                val binder = service as LocalBinder
                timerService = binder.serviceInstance
                timerService!!.registerClient(context as Activity)
                timerService!!.startUstmTimer()
                timerService!!.startStmTimer()
                findViewById<View>(R.id.lTimerFrame).visibility = View.VISIBLE
                timerAnimation = findViewById(R.id.timerAnimationPanel)
            }

            override fun onServiceDisconnected(name: ComponentName) {
                Log.d(
                    "LearnActivity::initTimer",
                    "onServiceDisconnectedCalled"
                )
                timerService!!.stopUstmTimer()
                timerService!!.stopStmTimer()
            }
        }
        timerServiceIntent = Intent(context, TimerService::class.java)
        timerServiceIntent!!.putExtra(TimerService.Companion.USTM_TOTAL_TIME, ustmTotalTime)
        timerServiceIntent!!.putExtra(TimerService.Companion.STM_TOTAL_TIME, stmTotalTime)
        startService(timerServiceIntent)
        bindService(
            timerServiceIntent,
            timerServiceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    private fun pauseTimer() {
        if (timerService != null) {
            timerService!!.pauseTimers()
            if (!timerService!!.isStmTimerFinished) {
                disableButtons()
            }
        }
    }

    private fun restartTimer() {
        if (timerService != null) {
            timerService!!.restartTimers()
            enableButtons()
        }
    }

    private fun updateLearningPhase() {
        val learningPhase = modelManager.learningPhase
        var zeroUnlearnedCards = false
        var zeroUSTMCards = false
        var zeroSTMCards = false
        if (modelManager.unlearnedBatchSize <= 0) {
            zeroUnlearnedCards = true
        }
        if (modelManager.ultraShortTermMemorySize <= 0) {
            zeroUSTMCards = true
        }
        if (modelManager.shortTermMemorySize <= 0) {
            zeroSTMCards = true
        }
        when (learningPhase) {
            LearningPhase.NOTHING -> {
            }
            LearningPhase.SIMPLE_LEARNING -> {
                if (completedLearning) {
                    finishLearning()
                } else {
                    setButtonVisibilityRepeating()
                }
            }
            LearningPhase.FILLING_USTM -> {
                setButtonVisibilityFilling()
                if (timerService!!.isStmTimerFinished) // STM timeout so go straight to repeating ustm cards
                {
                    setLearningPhase(LearningPhase.REPEATING_USTM)
                    updateLearningPhase()
                } else if (zeroUnlearnedCards && !timerService!!.isUstmTimerFinished) {
                    setLearningPhase(LearningPhase.WAITING_FOR_USTM)
                    updateLearningPhase()
                } else if (timerService!!.isUstmTimerFinished) {
                    setLearningPhase(LearningPhase.REPEATING_USTM)
                    updateLearningPhase()
                }
            }
            LearningPhase.WAITING_FOR_USTM -> {
                Log.d(
                    "LearnCardsActivity::updateLearningPhase",
                    "Waiting for USTM"
                )
                // Gif zeigen
                showHideTimerAnimation()
                // USTM Timeout
                if (timerService!!.isUstmTimerFinished || stopWaiting) {
                    stopWaiting = false
                    setLearningPhase(LearningPhase.REPEATING_USTM)
                    timerService!!.stopUstmTimer()
                    updateLearningPhase()
                }
            }
            LearningPhase.REPEATING_USTM -> {
                setButtonsVisibility()
                if (zeroUSTMCards) // We have learned all the ustm cards
                {
                    if (timerService!!.isStmTimerFinished) //STM timer has timed out so move to repeating STM
                    {
                        setLearningPhase(LearningPhase.REPEATING_STM)
                    } else if (!zeroUnlearnedCards) // Unlearned cards available so go back to filling ustm;
                    {
                        setLearningPhase(LearningPhase.FILLING_USTM)
                        timerService!!.startUstmTimer()
                    } else {
                        setLearningPhase(LearningPhase.WAITING_FOR_STM)
                    }
                    updateLearningPhase()
                } else if (mCardPackAdapter!!.isLastCard) {
                    setLearningPhase(LearningPhase.REPEATING_USTM)
                }
            }
            LearningPhase.WAITING_FOR_STM -> {
                // Gif zeigen
                showHideTimerAnimation()
                // USTM Timeout
                if (timerService!!.isStmTimerFinished || stopWaiting) {
                    stopWaiting = false
                    timerService!!.stopStmTimer()
                    setLearningPhase(LearningPhase.REPEATING_STM)
                    invalidateOptionsMenu()
                    updateLearningPhase()
                }
            }
            LearningPhase.REPEATING_STM -> {
                setButtonsVisibility()
                if (zeroSTMCards) {
                    finishLearning()
                } else if (mCardPackAdapter!!.isLastCard) {
                    setLearningPhase(LearningPhase.REPEATING_STM)
                }
            }
            LearningPhase.REPEATING_LTM -> {
                if (completedLearning && modelManager.expiredCardsSize <= 0) {
                    finishLearning()
                } else if (completedLearning) {
                    pushCursorToNext()
                } else {
                    setButtonsVisibility()
                }
            }
        }
    }

    private fun setLearningPhase(newLearningsPhase: LearningPhase) { //Neue Phase dem Modelmanager mitteilen und Deck aktualisieren
        modelManager!!.setLearningPhase(context, newLearningsPhase)
        //Cursor an erste Stelle setzen
        mSavedCursorPosition = -1
        refreshCursor()
    }

    private fun finishLearning() {
        if (settingsManager!!.getBoolPreference(context, Keys.AUTO_SAVE)) {
            startActivityForResult(
                Intent(context, SaveDialog::class.java),
                Constants.REQUEST_CODE_SAVE_DIALOG_NORMAL
            )
        } else {
            paukerManager?.isSaveRequired = true
            finish()
        }
    }

    fun initButtons() {
        bNext = findViewById(R.id.bNext)
        bShowMe = findViewById(R.id.bShowMe)
        lRepeatButtons = findViewById(R.id.lBRepeat)
        lSkipWaiting = findViewById(R.id.lBSkipWaiting)
        setButtonsVisibility()
    }

    private fun setButtonsVisibility() {
        val learningPhase = modelManager.learningPhase
        if (learningPhase == LearningPhase.WAITING_FOR_USTM || learningPhase == LearningPhase.WAITING_FOR_STM) {
            setButtonVisibilityWaiting()
        } else if (learningPhase == LearningPhase.SIMPLE_LEARNING || learningPhase == LearningPhase.REPEATING_USTM || learningPhase == LearningPhase.REPEATING_STM || learningPhase == LearningPhase.REPEATING_LTM
        ) {
            setButtonVisibilityRepeating()
        } else if (learningPhase == LearningPhase.FILLING_USTM) {
            setButtonVisibilityFilling()
        }
    }

    private fun setButtonVisibilityWaiting() {
        bNext!!.visibility = View.GONE
        bShowMe!!.visibility = View.GONE
        lRepeatButtons!!.visibility = View.GONE
        lSkipWaiting!!.visibility = View.VISIBLE
    }

    private fun setButtonVisibilityRepeating() {
        bNext!!.visibility = View.GONE
        bShowMe!!.visibility = View.VISIBLE
        val text: String
        text = if (mCardCursor != null) {
            val currentCard = modelManager!!.getCard(mCardCursor!!.position)
            if (currentCard != null && currentCard.isRepeatedByTyping) getString(R.string.enter_answer) else getString(
                R.string.show_me
            )
        } else {
            getString(R.string.show_me)
        }
        bShowMe!!.text = text
        lRepeatButtons!!.visibility = View.GONE
        lSkipWaiting!!.visibility = View.GONE
    }

    private fun setButtonVisibilityFilling() {
        bNext!!.visibility = View.VISIBLE
        bShowMe!!.visibility = View.GONE
        lRepeatButtons!!.visibility = View.GONE
        lSkipWaiting!!.visibility = View.GONE
    }

    private fun disableButtons() {
        bNext!!.isEnabled = false
        bShowMe!!.isEnabled = false
        findViewById<View>(R.id.bYes).isEnabled = false
        findViewById<View>(R.id.bNo).isEnabled = false
        findViewById<View>(R.id.bSkipWaiting).isEnabled = false
    }

    private fun enableButtons() {
        bNext!!.isEnabled = true
        bShowMe!!.isEnabled = true
        findViewById<View>(R.id.bYes).isEnabled = true
        findViewById<View>(R.id.bNo).isEnabled = true
        findViewById<View>(R.id.bSkipWaiting).isEnabled = true
    }

    private fun showHideTimerAnimation() {
        if (timerAnimation == null) return
        timerAnimation!!.visibility = if (stopWaiting) View.GONE else View.VISIBLE
        setButtonsVisibility()
    }

    @SuppressLint("InflateParams")
    private fun showInputDialog() {
        val builder = AlertDialog.Builder(context)
        val view = layoutInflater.inflate(R.layout.dialog_input, null)
        val inputField: MPEditText = view.findViewById(R.id.eTInput)
        builder.setView(view)
            .setPositiveButton(R.string.proof) { dialog, which ->
                val cardText: String?
                cardText = if (flipCardSides) {
                    currentCard.sideAText
                } else {
                    currentCard.sideBText
                }
                val caseSensitive =
                    settingsManager!!.getBoolPreference(context, Keys.CASE_SENSITIV)
                val input = inputField.text.toString()
                if (caseSensitive && cardText == input) {
                    yesClicked(null)
                } else if (cardText.equals(input, ignoreCase = true)) {
                    yesClicked(null)
                } else showResultDialog(cardText, input)
                dialog.dismiss()
            }
            .setNeutralButton(R.string.cancel) { dialog, which -> dialog.dismiss() }
            .setOnDismissListener {
                val imm =
                    getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                if (imm != null && currentFocus != null && imm.isAcceptingText) {
                    imm.hideSoftInputFromWindow(currentFocus.windowToken, 0)
                }
            }
        if (flipCardSides) {
            inputField.setFont(
                modelManager!!.getCardFont(
                    CardPackAdapter.Companion.KEY_SIDEA_ID,
                    mCardCursor!!.position
                )
            )
        } else {
            inputField.setFont(
                modelManager!!.getCardFont(
                    CardPackAdapter.Companion.KEY_SIDEB_ID,
                    mCardCursor!!.position
                )
            )
        }
        builder.create().show()
    }

    @SuppressLint("InflateParams")
    private fun showResultDialog(cardText: String?, input: String) {
        val builder =
            AlertDialog.Builder(context, R.style.ResultDialogTheme)
        val view = layoutInflater.inflate(R.layout.dialog_result, null)
        builder.setView(view)
            .setPositiveButton("Weiterlegen") { dialog, which -> yesClicked(null) }
            .setNeutralButton("Zurücklegen") { dialog, which -> noClicked(null) }
            .setCancelable(false)
        (view.findViewById<View>(R.id.tVRightAnswerText) as TextView).text = cardText
        (view.findViewById<View>(R.id.tVInputText) as TextView).text = input
        builder.create().show()
    }

    private fun fillInData(flipCardSides: Boolean) { // Daten setzen
        val fontA = modelManager!!.getCardFont(
            CardPackAdapter.Companion.KEY_SIDEA_ID,
            mCardCursor!!.position
        )
        val fontB = modelManager.getCardFont(
            CardPackAdapter.Companion.KEY_SIDEB_ID,
            mCardCursor!!.position
        )
        val learningPhase = modelManager.learningPhase
        // Layoutcontents setzen
        if (flipCardSides) {
            fillSideA(R.string.back, currentCard.sideBText, fontB)
            var sideAText: String? = ""
            if (currentCard.side == SideShowing.SIDE_A || learningPhase == LearningPhase.SIMPLE_LEARNING || learningPhase == LearningPhase.FILLING_USTM
            ) {
                sideAText = currentCard.sideAText
            } else if (modelManager.getCard(mCardCursor!!.position)!!.isRepeatedByTyping) {
                sideAText = "Tap to enter your answer."
            }
            fillSideB(R.string.front, sideAText, fontA)
        } else { // Card sides not flipped so show side A in top box!
            fillSideA(R.string.front, currentCard.sideAText, fontA)
            var sideBText: String? = ""
            if (currentCard.side == SideShowing.SIDE_B || learningPhase == LearningPhase.SIMPLE_LEARNING || learningPhase == LearningPhase.FILLING_USTM
            ) {
                sideBText = currentCard.sideBText
            } else if (modelManager.getCard(mCardCursor!!.position)!!.isRepeatedByTyping) {
                sideBText = "Tap to enter your answer."
            }
            fillSideB(R.string.back, sideBText, fontB)
        }
        fillHeader()
    }

    private fun hasCardsToBeFlipped(): Boolean {
        val flipMode =
            settingsManager!!.getStringPreference(context, Keys.FLIP_CARD_SIDES)
        val learningPhase = modelManager.learningPhase
        flipCardSides =
            if (learningPhase == LearningPhase.REPEATING_LTM || learningPhase == LearningPhase.REPEATING_STM || learningPhase == LearningPhase.REPEATING_USTM) {
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
        if (flipCardSides) {
            currentCard.side = SideShowing.SIDE_B
        } else {
            currentCard.side = SideShowing.SIDE_A
        }
        return flipCardSides
    }

    private fun fillSideA(
        titleResource: Int,
        text: String?,
        font: Font?
    ) {
        (findViewById<View>(R.id.titelCardSideA) as TextView).text = getString(
            titleResource
        )
        val sideA = findViewById<MPTextView>(R.id.tCardSideA)
        sideA.setFont(font)
        sideA.text = text
    }

    /**
     * Füllt die Kartenseite mit Text und setzt Titel und Font. Benutzt beim Wiederholen durch
     * Erinnern.
     * @param titleResource Ttiel der Karte
     * @param text          Anzuzeigender Text
     */
    private fun fillSideB(
        titleResource: Int,
        text: String?,
        font: Font?
    ) {
        (findViewById<View>(R.id.titelCardSideB) as TextView).text = getString(
            titleResource
        )
        val sideB = findViewById<MPTextView>(R.id.tCardSideB_TV)
        if (text!!.isEmpty() || modelManager!!.getCard(mCardCursor!!.position)!!.isRepeatedByTyping) {
            sideB.hint = getString(R.string.learncards_show_hint)
            sideB.setFont(Font())
        } else {
            sideB.setFont(font)
        }
        sideB.text = text
    }

    private fun fillHeader() {
        val allCards = findViewById<TextView>(R.id.tAllCards)
        val ustmCards = findViewById<TextView>(R.id.tUKZGCards)
        val stmCards = findViewById<TextView>(R.id.tKZGCards)
        var text: String
        if (repeatingLTM) {
            text = getString(R.string.expired) + ": %d"
            text = String.format(text, mCardCursor!!.count - mCardCursor!!.position)
            allCards.text = text
            ustmCards.visibility = View.GONE
            stmCards.visibility = View.GONE
        } else {
            text = getString(R.string.untrained) + ": %d"
            text = String.format(text, modelManager.unlearnedBatchSize)
            allCards.text = text
            text = getString(R.string.ustm) + ": %d"
            text = String.format(text, modelManager.ultraShortTermMemorySize)
            ustmCards.text = text
            text = getString(R.string.stm) + ": %d"
            text = String.format(text, modelManager.shortTermMemorySize)
            stmCards.text = text
        }
    }

    /**
     * Werden abgelaufene Karten wiederholt, wird der Stack neugeladen. Sonst wird der Standartweg
     * gegangen.
     */
    private fun pushCursorToNext() { /*initStackSize--;
        if (checkStackSize()) {
            setLearningPhase(modelManager.getLearningPhase());
            reloadStack();
        } else {
            mCardCursor.moveToNext();
        }*/
        if (modelManager.learningPhase == LearningPhase.REPEATING_LTM) {
            modelManager!!.setLearningPhase(context, modelManager.learningPhase)
            reloadStack()
        } else {
            mCardCursor!!.moveToNext()
            updateCurrentCard()
            fillData()
        }
    }

    fun mEditClicked(item: MenuItem?) {
        val intent = Intent(context, EditCardActivity::class.java)
        intent.putExtra(
            Constants.CURSOR_POSITION,
            mCardCursor!!.position
        )
        pendingIntent = intent
        startActivityForResult(
            intent,
            Constants.REQUEST_CODE_EDIT_CARD
        )
    }

    fun mDeleteClicked(item: MenuItem?) {
        val builder = AlertDialog.Builder(context)
        builder.setMessage(R.string.delete_card_message)
            .setPositiveButton(R.string.delete) { dialog, which ->
                // Muss vorher gespeichert werden, da sonst im Nachhinein der Wert
// verfälscht werden kann!
                val isLast = mCardCursor!!.isLast
                if (modelManager!!.deleteCard(mCardCursor!!.position)) {
                    if (isLast) { // Letzte Karte oder Timer abgelaufen. Darum Lernphase aktualisieren
                        updateLearningPhase()
                    } else {
                        updateCurrentCard()
                        fillData()
                        setButtonsVisibility()
                    }
                } else {
                    PaukerManager.Companion.showToast(
                        context as Activity,
                        "Löschen nicht möglich!",
                        Toast.LENGTH_SHORT
                    )
                }
                dialog.cancel()
            }
            .setNeutralButton(R.string.cancel) { dialog, which -> dialog.cancel() }
        builder.create().show()
    }

    fun mPauseTimerClicked(item: MenuItem) {
        if (restartButton != null && !timerService!!.isStmTimerFinished) {
            pauseTimer()
            item.isVisible = false
            restartButton!!.isVisible = true
        } else {
            PaukerManager.Companion.showToast(
                context as Activity,
                R.string.pause_timer_error,
                Toast.LENGTH_LONG
            )
        }
    }

    fun mRestartTimerClicked(item: MenuItem) {
        if (pauseButton != null) {
            restartTimer()
            item.isVisible = false
            pauseButton!!.isVisible = true
        } else {
            PaukerManager.Companion.showToast(
                context as Activity,
                R.string.restart_timer_error,
                Toast.LENGTH_LONG
            )
        }
    }

    fun mFlipSidesClicked(item: MenuItem?) {
        modelManager!!.getCard(mCardCursor!!.position)!!.flip()
        paukerManager?.isSaveRequired = true
        updateCurrentCard()
        val learningPhase = modelManager.learningPhase
        if (learningPhase == LearningPhase.REPEATING_LTM || learningPhase == LearningPhase.REPEATING_STM || learningPhase == LearningPhase.REPEATING_USTM) {
            flipCardSides = !flipCardSides
            if (flipCardSides) {
                currentCard.side = SideShowing.SIDE_B
            } else {
                currentCard.side = SideShowing.SIDE_A
            }
        }
        fillInData(flipCardSides)
        setButtonsVisibility()
    }

    // Aktionen der Buttons
    fun nextCard(view: View?) { // Karte ein Deck weiterschieben
        mCardPackAdapter!!.setCardLearned(mCardCursor!!.getLong(CardPackAdapter.Companion.KEY_ROWID_ID))
        if (!mCardCursor!!.isLast && !timerService!!.isUstmTimerFinished) {
            pushCursorToNext()
        } else { // Letzte Karte oder Timer abgelaufen. Darum Lernphase aktualisieren
            updateLearningPhase()
        }
    }

    fun skipWaiting(view: View?) {
        stopWaiting = true
        updateLearningPhase()
    }

    fun noClicked(view: View?) {
        mCardPackAdapter!!.setCardUnLearned(
            context,
            mCardCursor!!.getLong(CardPackAdapter.Companion.KEY_ROWID_ID)
        )
        paukerManager?.isSaveRequired = true
        if (!mCardCursor!!.isLast) {
            pushCursorToNext()
        } else {
            completedLearning = true
        }
        updateLearningPhase()
    }

    fun yesClicked(view: View?) {
        mCardPackAdapter!!.setCardLearned(mCardCursor!!.getLong(CardPackAdapter.Companion.KEY_ROWID_ID))
        paukerManager?.isSaveRequired = true
        if (!mCardCursor!!.isLast) {
            pushCursorToNext()
        } else {
            completedLearning = true
        }
        updateLearningPhase()
    }

    fun showCard(view: View?) {
        screenTouched()
    }

    override fun onUstmTimerUpdate(timeElapsed: Int) {
        runOnUiThread {
            if (ustmTimerBar!!.visibility == View.VISIBLE && !timerService!!.isUstmTimerFinished) {
                val sec = timeElapsed % 60
                ustmTimerText = String.format(
                    Locale.getDefault()
                    , "%d / %ds", sec, timerService?.ustmTotalTime
                )
                ustmTimerBar!!.setProgress(timeElapsed)
                ustmTimerBar!!.text = ustmTimerText
            }
        }
    }

    override fun onStmTimerUpdate(timeElapsed: Int) {
        runOnUiThread {
            val timerText: String
            val sec = timeElapsed % 60
            val min = timeElapsed / 60
            timerText = if (sec < 10) {
                String.format(
                    Locale.getDefault(),
                    "%d:0%d / %d:00min", min, sec, timerService?.stmTotalTime
                )
            } else {
                String.format(
                    Locale.getDefault(),
                    "%d:%d / %d:00min", min, sec, timerService?.stmTotalTime
                )
            }
            stmTimerBar!!.setProgress(timeElapsed)
            stmTimerBar!!.text = timerText
            // Ist die App pausiert, soll in der Titelleiste die Zeit angezeigt werden
            if (!isActivityVisible && !timerService!!.isStmTimerFinished) {
                Log.d(
                    "LearnActivity::STM-onStmTimerUpdate",
                    "Acivity is not visible"
                )
                val ustmTimerBarText =
                    if (timerService!!.isUstmTimerFinished && ustmTimerText != null) "" else getString(
                        R.string.ustm
                    ) + " " + ustmTimerText
                val timerbarText =
                    ustmTimerBarText + "  " + getString(R.string.stm) + " " + timerText
                val contentIntent =
                    if (pendingIntent == null) intent else pendingIntent!!
                val mBuilder =
                    NotificationCompat.Builder(
                        context,
                        Constants.TIMER_BAR_CHANNEL_ID
                    )
                        .setContentText(timerbarText)
                        .setSmallIcon(R.drawable.notify_icon)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
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
                Log.d(
                    "LearnActivity::STM-onStmTimerUpdate",
                    "Notification created"
                )
                notificationManager!!.notify(
                    Constants.TIME_BAR_ID,
                    mBuilder.build()
                )
                Log.d(
                    "LearnActivity::STM-onStmTimerUpdate",
                    "Show Notification"
                )
            }
        }
    }

    override fun onUstmTimerFinish() {
        runOnUiThread {
            Log.d(
                "LearnActivity::USTM-Timer finished",
                "Timer finished"
            )
            ustmTimerBar?.setProgress(timerService?.ustmTotalTime ?: 0 * 60)
            ustmTimerBar?.text = " "
            if (modelManager.learningPhase == LearningPhase.WAITING_FOR_USTM) {
                Log.d(
                    "Learnactivity::onUSTMTimerFinish",
                    "USTM Timer finished, stop waiting!"
                )
                stopWaiting = true
                updateLearningPhase()
            }
        }
    }

    override fun onStmTimerFinish() {
        runOnUiThread {
            Log.d(
                "LearnActivity::STM-Timer finished",
                "Timer finished"
            )
            notificationManager!!.cancel(Constants.TIME_BAR_ID)
            stmTimerBar?.text = " "
            stmTimerBar?.setProgress(timerService?.stmTotalTime ?: 0 * 60)
            if (pauseButton != null) {
                pauseButton!!.isVisible = false
            }
            if (modelManager.learningPhase == LearningPhase.WAITING_FOR_STM) {
                Log.d(
                    "LearnActivity::onSTMTimerFinish",
                    "STM Timer finished, stop waiting!"
                )
                stopWaiting = true
                updateLearningPhase()
            }
            // Ist die App pausiert, soll in der Titelleiste die Zeit angezeigt werden
            val showNotify =
                settingsManager.getBoolPreference(context, Keys.SHOW_TIMER_BAR)
            if (!isActivityVisible && timerService!!.isStmTimerFinished && showNotify) {
                Log.d(
                    "LearnActivity::STM-Timer finished",
                    "Acivity is visible"
                )
                val mBuilder =
                    NotificationCompat.Builder(
                        context,
                        Constants.NOTIFICATION_CHANNEL_ID
                    )
                        .setContentText(getString(R.string.stm_expired_notify_message))
                        .setSmallIcon(R.drawable.notify_icon)
                        .setContentTitle(getString(R.string.app_name))
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setContentIntent(PendingIntent.getActivity(context, 0, intent, 0))
                        .setAutoCancel(true)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                Log.d(
                    "LearnActivity::STM-Timer finished",
                    "Notification created"
                )
                Timer().schedule(object : TimerTask() {
                    override fun run() {
                        notificationManager!!.notify(
                            Constants.NOTIFICATION_ID,
                            mBuilder.build()
                        )
                    }
                }, 1000)
                Log.d(
                    "LearnActivity::STM-Timer finished",
                    "Notification shown"
                )
            }
        }
    }

    companion object {
        var isLearningRunning = false
            private set
        private var isActivityVisible = false
    }
}