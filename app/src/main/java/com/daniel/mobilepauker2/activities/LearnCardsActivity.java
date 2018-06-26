/* 
 * Copyright 2011 Brian Ford
 * 
 * This file is part of Pocket Pauker.
 * 
 * Pocket Pauker is free software: you can redistribute it and/or modify it under the 
 * terms of the GNU General Public License as published by the Free Software Foundation, 
 * either version 3 of the License, or (at your option) any later version.
 * 
 * Pocket Pauker is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more 
 * details.
 * 
 * See http://www.gnu.org/licenses/.

*/

package com.daniel.mobilepauker2.activities;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.daniel.mobilepauker2.PaukerManager;
import com.daniel.mobilepauker2.R;
import com.daniel.mobilepauker2.model.CardPackAdapter;
import com.daniel.mobilepauker2.model.FlashCard;
import com.daniel.mobilepauker2.model.ModelManager.LearningPhase;
import com.daniel.mobilepauker2.model.SettingsManager;
import com.daniel.mobilepauker2.utils.Constants;
import com.daniel.mobilepauker2.utils.ErrorReporter;
import com.daniel.mobilepauker2.utils.Log;

import java.util.Locale;
import java.util.Random;

import static com.daniel.mobilepauker2.model.ModelManager.LearningPhase.FILLING_USTM;
import static com.daniel.mobilepauker2.model.ModelManager.LearningPhase.NOTHING;
import static com.daniel.mobilepauker2.model.ModelManager.LearningPhase.REPEATING_LTM;
import static com.daniel.mobilepauker2.model.ModelManager.LearningPhase.REPEATING_STM;
import static com.daniel.mobilepauker2.model.ModelManager.LearningPhase.REPEATING_USTM;
import static com.daniel.mobilepauker2.model.ModelManager.LearningPhase.SIMPLE_LEARNING;
import static com.daniel.mobilepauker2.model.SettingsManager.Keys.AUTO_SAVE;
import static com.daniel.mobilepauker2.model.SettingsManager.Keys.CENTER_TEXT;
import static com.daniel.mobilepauker2.model.SettingsManager.Keys.ENABLE_SPLITSCREEN;
import static com.daniel.mobilepauker2.model.SettingsManager.Keys.FONT_SIZE;
import static com.daniel.mobilepauker2.model.SettingsManager.Keys.STM;
import static com.daniel.mobilepauker2.model.SettingsManager.Keys.USTM;

public class LearnCardsActivity extends FlashCardSwipeScreenActivity {
    private static final Handler mHandler = new Handler();
    private final Context context = this;
    private final PaukerManager paukerManager = PaukerManager.instance();
    private boolean completedLearning = false;
    private boolean stopWaiting = false;
    private int startUnlearnedBatchSize = 0;
    private Button bNext;
    private Button bShowMe;
    private RelativeLayout lRepeatButtons;
    private RelativeLayout lSkipWaiting;
    private boolean flipCardSides;
    private boolean repeatingLTM = false;
    private long mStartTime = 0L;
    private int uSTMStartTimeSeconds = 0; //seconds
    private int sTMStartTimeSeconds = 0; //seconds
    private final Runnable mUpdateTimeTask = new Runnable() {
        public void run() {
            final long start = mStartTime;
            long upTime = SystemClock.uptimeMillis();
            long millis = upTime - start;
            int totalSeconds = (int) (millis / 1000);
            int minutes = totalSeconds / 60;
            int seconds = totalSeconds % 60;

            mHandler.postAtTime(this,
                    start + (((minutes * 60) + seconds + 1) * 1000));

            updateTimeText(seconds, minutes);}
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startUnlearnedBatchSize = modelManager.getUnlearnedBatchSize();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (modelManager.getLearningPhase() != REPEATING_LTM
                && (modelManager.getLearningPhase() != SIMPLE_LEARNING
                || modelManager.getLearningPhase() != NOTHING)) {
            // A check on mActivitySetupOk is done here as onCreate is called even if the
            // super (FlashCardSwipeScreenActivity) onCreate fails to find any cards and calls finish()
            if (mActivitySetupOk) {
                startUSTMTimer();
                startSTMTimer();
                startTimer();
            }
        } else if (modelManager.getLearningPhase() == REPEATING_LTM) {
            repeatingLTM = true;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings, menu);

        return true;
    }

    /**
     * Updates the contents of the current card to the contents of the
     * card at the current cursor position
     */
    public void updateCurrentCard() {
        try {
            if (isCardCursorAvailable()) {
                currentCard.setSideAText(mCardCursor.getString(CardPackAdapter.KEY_SIDEA_ID));
                currentCard.setSideBText(mCardCursor.getString(CardPackAdapter.KEY_SIDEB_ID));
                String learnStatus = mCardCursor.getString(CardPackAdapter.KEY_LEARN_STATUS_ID);

                if (learnStatus.contentEquals("1")) {
                    currentCard.setLearned(true);
                } else {
                    currentCard.setLearned(false);
                }
            } else {
                currentCard.setSideAText("");
                currentCard.setSideBText("");
                Log.d("FlashCardSwipeScreenActivity::updateCurrentCard", "Card Cursor not available");
            }

        } catch (Exception e) {
            Log.e("FlashCardSwipeScreenActivity::updateCurrentCard", "Caught Exception");
            e.printStackTrace();
            throw new RuntimeException("FlashCardSwipeScreenActivity::updateCurrentCard - cursor problem?");
        }

        LearningPhase learningPhase = modelManager.getLearningPhase();
        if (learningPhase == REPEATING_LTM || learningPhase == REPEATING_STM || learningPhase == REPEATING_USTM) {
            String flipMode = settingsManager.getStringPreference(context, SettingsManager.Keys.FLIP_CARD_SIDES);
            switch (flipMode) {
                case "1":
                    flipCardSides = true;
                    break;
                case "2":
                    Random rand = new Random();
                    flipCardSides = rand.nextBoolean();
                    break;
                default:
                    flipCardSides = false;
                    break;
            }
        } else {
            flipCardSides = false;
        }
        if (flipCardSides) {
            currentCard.setSide(FlashCard.SideShowing.SIDE_B);
        } else {
            currentCard.setSide(FlashCard.SideShowing.SIDE_A);
        }
    }

    private void setupSplitView() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        setContentView(R.layout.learn_cards_split);
    }

    private void setupNormalView() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.learn_cards);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    private void updateLearningPhase() {
        LearningPhase learningPhase = modelManager.getLearningPhase();

        boolean zeroUnlearnedCards = false;
        boolean zeroUSTMCards = false;
        boolean zeroSTMCards = false;

        if (modelManager.getUnlearnedBatchSize() <= 0) {
            zeroUnlearnedCards = true;
        }

        if (modelManager.getShortTermMemorySize() >= startUnlearnedBatchSize) {
            zeroUnlearnedCards = true;
        }

        if (modelManager.getUltraShortTermMemorySize() <= 0) {
            zeroUSTMCards = true;
        }

        if (modelManager.getShortTermMemorySize() <= 0) {
            zeroSTMCards = true;
        }

        switch (learningPhase) {

            case NOTHING: {
                break;
            }

            case SIMPLE_LEARNING: {

                if (completedLearning) {
                    completedLearning();
                } else {
                    setButtonVisibilityRepeating();
                }
                break;
            }

            case FILLING_USTM: {
                setButtonVisibilityFilling();

                if (checkSTMTimeout()) // STM timeout so go straight to repeating ustm cards
                {
                    //Toast.makeText(this, getString(R.string.learncards_repeat_ustm), Toast.LENGTH_SHORT).show();
                    setLearningPhase(LearningPhase.REPEATING_USTM);
                    updateLearningPhase();
                } else if (zeroUnlearnedCards && !checkUSTMTimeout()) {
                    setLearningPhase(LearningPhase.WAITING_FOR_USTM);
                    updateLearningPhase();
                } else if (checkUSTMTimeout()) {
                    //Toast.makeText(this, getString(R.string.learncards_repeat_ustm), Toast.LENGTH_SHORT).show();
                    setLearningPhase(LearningPhase.REPEATING_USTM);
                    updateLearningPhase();
                }

                break;
            }

            case WAITING_FOR_USTM: {
                Log.d("LearnCardsActivity::updateLearningPhase", "Waiting for USTM");
                setButtonVisibilityWaiting();

                // USTM Timeout
                if (checkUSTMTimeout() || stopWaiting) {
                    //Toast.makeText(this, getString(R.string.learncards_repeat_ustm), Toast.LENGTH_LONG).show();
                    stopWaiting = false;
                    setLearningPhase(LearningPhase.REPEATING_USTM);
                    updateLearningPhase();
                }

                break;
            }

            case REPEATING_USTM: {
                setButtonVisibilityRepeating();

                if (zeroUSTMCards) // We have learned all the ustm cards
                {
                    if (checkSTMTimeout()) //STM timer has timed out so move to repeating STM
                    {
                        //Toast.makeText(this, getString(R.string.learncards_repeat_stm), Toast.LENGTH_SHORT).show();
                        setLearningPhase(LearningPhase.REPEATING_STM);
                    } else if (!zeroUnlearnedCards) // Unlearned cards available so go back to filling ustm;
                    {
                        setLearningPhase(LearningPhase.FILLING_USTM);
                        startUSTMTimer();
                    } else {
                        //Toast.makeText(this, getString(R.string.learncards_waiting_stm_timer), Toast.LENGTH_SHORT).show();
                        setLearningPhase(LearningPhase.WAITING_FOR_STM);
                    }
                    {
                        Log.e("LearnCardsActivity::updateLearningPhase", "Unrecognised state while repeating USTM");
                    }

                    updateLearningPhase();
                }

                break;
            }

            case WAITING_FOR_STM: {
                setButtonVisibilityWaiting();

                // USTM Timeout
                if (checkSTMTimeout() || stopWaiting) {
                    //Toast.makeText(this, getString(R.string.learncards_repeat_stm), Toast.LENGTH_SHORT).show();
                    stopWaiting = false;
                    setLearningPhase(LearningPhase.REPEATING_STM);
                    updateLearningPhase();
                }
                break;
            }

            case REPEATING_STM: {
                if (zeroSTMCards) {
                    //Toast.makeText(this, getString(R.string.learncards_finished_learning), Toast.LENGTH_LONG).show();
                    completedLearning();
                } else {
                    setButtonVisibilityRepeating();
                }
                break;
            }

            case REPEATING_LTM: {
                if (modelManager.getExpiredCardsSize() <= 0) {
                    //Toast.makeText(this, getString(R.string.learncards_finished_learning), Toast.LENGTH_LONG).show();
                    completedLearning();
                } else {
                    setButtonVisibilityRepeating();
                }
                break;
            }
        }
    }

    public void setupButtons() {
        bNext = findViewById(R.id.bNext);
        bShowMe = findViewById(R.id.bShowMe);
        lRepeatButtons = findViewById(R.id.lBRepeat);
        lSkipWaiting = findViewById(R.id.lBSkipWaiting);
    }

    private void setButtonVisibilityWaiting() {
        bNext.setVisibility(View.GONE);
        bShowMe.setVisibility(View.GONE);
        lRepeatButtons.setVisibility(View.GONE);
        lSkipWaiting.setVisibility(View.VISIBLE);
    }

    private void setButtonVisibilityRepeating() {
        bNext.setVisibility(View.GONE);
        bShowMe.setVisibility(View.VISIBLE);
        lRepeatButtons.setVisibility(View.GONE);
        lSkipWaiting.setVisibility(View.GONE);
    }

    private void setButtonVisibilityFilling() {
        bNext.setVisibility(View.VISIBLE);
        bShowMe.setVisibility(View.GONE);
        lRepeatButtons.setVisibility(View.GONE);
        lSkipWaiting.setVisibility(View.GONE);
    }

    private void fillDataSplitView(String sideAText, String sideBText) {
        TextView sideATextView = findViewById(R.id.tCardSideA);
        TextView sideBTextView = findViewById(R.id.tCardSideB);

        // Textgröße setzen
        int textSize = Integer.parseInt(settingsManager.getStringPreference(context, FONT_SIZE));
        sideATextView.setTextSize(textSize);
        sideBTextView.setTextSize(textSize);

        // Text evtl mittig setzen
        int gravity = settingsManager.getBoolPreference(context, CENTER_TEXT) ?
                Gravity.CENTER : Gravity.NO_GRAVITY;
        sideATextView.setGravity(gravity);
        sideBTextView.setGravity(gravity);

        LearningPhase learningPhase = modelManager.getLearningPhase();
        // Layoutcontents setzen
        if (flipCardSides) {
            sideATextView.setText(sideBText);

            // Now work out if we should show side A in bottom box
            if (currentCard.getSide() == FlashCard.SideShowing.SIDE_A
                    || learningPhase == SIMPLE_LEARNING
                    || learningPhase == FILLING_USTM) {
                sideBTextView.setText(sideAText);
            } else {
                sideBTextView.setHint(getString(R.string.learncards_show_hint));
                sideBTextView.setText("");
            }
        } else {
            // Card sides not flipped so show side A in top box!
            sideATextView.setText(sideAText);

            if (currentCard.getSide() == FlashCard.SideShowing.SIDE_B
                    || learningPhase == SIMPLE_LEARNING
                    || learningPhase == FILLING_USTM) {
                sideBTextView.setText(sideBText);
            } else {
                sideBTextView.setHint(getString(R.string.learncards_show_hint));
                sideBTextView.setText("");
            }
        }

        fillHeader();
    }

    private void fillDataNormalView(String cardText) {
        TextView cardTextView = findViewById(R.id.tCardSide);

        // Textgröße setzen
        int textSize = Integer.parseInt(settingsManager.getStringPreference(context, FONT_SIZE));
        cardTextView.setTextSize(textSize);

        // Text evtl mittig setzen
        int gravity = settingsManager.getBoolPreference(context, CENTER_TEXT) ?
                Gravity.CENTER : Gravity.NO_GRAVITY;
        cardTextView.setGravity(gravity);

        // Layoutcontents setzen
        cardTextView.setText(cardText);
        fillHeader();
    }

    private void fillHeader() {
        TextView allCards = findViewById(R.id.tAllCards);
        TextView ustmCards = findViewById(R.id.tUKZGCards);
        TextView stmCards = findViewById(R.id.tKZGCards);

        String text;
        if (repeatingLTM) {
            text = getString(R.string.expired).concat(": %d");
            text = String.format(text, modelManager.getExpiredCardsSize());
            allCards.setText(text);
            ustmCards.setVisibility(View.GONE);
            stmCards.setVisibility(View.GONE);
        } else {
            text = getString(R.string.untrained).concat(": %d");
            text = String.format(text, modelManager.getUnlearnedBatchSize());
            allCards.setText(text);
            text = getString(R.string.ustm).concat(": %d");
            text = String.format(text, modelManager.getUltraShortTermMemorySize());
            ustmCards.setText(text);
            text = getString(R.string.stm).concat(": %d");
            text = String.format(text, modelManager.getShortTermMemorySize());
            stmCards.setText(text);
        }
    }

    private void completedLearning() {
        autoSaveLesson();
    }

    private void autoSaveLesson() {
        if (settingsManager.getBoolPreference(context, AUTO_SAVE)) {
            startActivityForResult(new Intent(context, SaveDialog.class), Constants.REQUEST_CODE_SAVE_DIALOG_NORMAL);
        } else {
            paukerManager.setSaveRequired(true);
            finish();
        }
    }

    /**
     * Prüft ob nach links geswipt werden darf.
     * @return <b>True</b>, wenn
     * <ul>
     * <li>Modus: <b>"Karte lernen"</b> und der USTM-Timer noch <b>nicht</b> abgelaufen ist</li>
     * <li>Modus: <b>"Karte lernen"</b> und der USTM-Timer noch <b>abgelaufen</b> und die Vorderseite sichtbar ist</li>
     * <li>Modus: <b>"Karte wiederholen"</b> und die Vorderseite sichtbar ist</li>
     * </ul>
     * sonst <b>false</b>
     */
    private boolean isLeftSwipeAllowed() {
        LearningPhase learningPhase = modelManager.getLearningPhase();
        if ((learningPhase == SIMPLE_LEARNING || learningPhase == FILLING_USTM)
                && !checkUSTMTimeout())
            return true;
        if ((learningPhase == SIMPLE_LEARNING || learningPhase == FILLING_USTM) && checkSTMTimeout()) {
            if (flipCardSides) {
                return currentCard.getSide() == FlashCard.SideShowing.SIDE_B;
            } else {
                return currentCard.getSide() == FlashCard.SideShowing.SIDE_A;
            }
        }
        if (learningPhase == REPEATING_LTM || learningPhase == REPEATING_STM || learningPhase == REPEATING_USTM) {
            if (flipCardSides) {
                return currentCard.getSide() == FlashCard.SideShowing.SIDE_B;
            } else {
                return currentCard.getSide() == FlashCard.SideShowing.SIDE_A;
            }
        }
        return false;
    }

    @Override
    void onLeftSwipe() {
        if (settingsManager.getBoolPreference(context, ENABLE_SPLITSCREEN)) return;

        if (isLeftSwipeAllowed()) {

            if (currentCard.getSide() == FlashCard.SideShowing.SIDE_A) {
                currentCard.setSide(FlashCard.SideShowing.SIDE_B);
            } else {
                currentCard.setSide(FlashCard.SideShowing.SIDE_A);
            }

            fillData();
            if (bShowMe.getVisibility() == View.VISIBLE) {
                bShowMe.setVisibility(View.GONE);
                lRepeatButtons.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    void onRightSwipe() {
        if (settingsManager.getBoolPreference(context, ENABLE_SPLITSCREEN)) return;

        LearningPhase learningPhase = modelManager.getLearningPhase();
        if ((learningPhase == SIMPLE_LEARNING || learningPhase == FILLING_USTM) && !checkUSTMTimeout()) {
            if (currentCard.getSide() == FlashCard.SideShowing.SIDE_A) {
                currentCard.setSide(FlashCard.SideShowing.SIDE_B);
            } else {
                currentCard.setSide(FlashCard.SideShowing.SIDE_A);
            }

            fillData();
        }
    }

    @Override
    public void screenTouched() {
        LearningPhase learningPhase = modelManager.getLearningPhase();
        if (settingsManager.getBoolPreference(context, ENABLE_SPLITSCREEN)
                && learningPhase == REPEATING_LTM || learningPhase == REPEATING_STM || learningPhase == REPEATING_USTM) {
            if (flipCardSides) {
                currentCard.setSide(FlashCard.SideShowing.SIDE_A);
            } else {
                currentCard.setSide(FlashCard.SideShowing.SIDE_B);
            }

            fillData();
            bShowMe.setVisibility(View.GONE);
            lRepeatButtons.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void moveCursorForwardToNextCard() {

    }

    @Override
    public void moveCursorBackToNextCard() {

    }

    @Override
    protected void fillData() {
        String mainCardText;
        if (currentCard.getSide() == FlashCard.SideShowing.SIDE_A) {
            mainCardText = currentCard.getSideAText();
        } else {
            mainCardText = currentCard.getSideBText();
        }

        if (settingsManager.getBoolPreference(context, ENABLE_SPLITSCREEN)) {
            fillDataSplitView(currentCard.getSideAText(), currentCard.getSideBText());
        } else {
            fillDataNormalView(mainCardText);
        }
    }

    @Override
    protected void cursorLoaded() {
        onResume();
    }

    @Override
    protected void onResume() {
        super.onResume();

        restartTimer();

        try {
            if (mCardCursor != null) {
                if (mCardCursor.getCount() >= mSavedCursorPosition)
                    mCardCursor.moveToPosition(mSavedCursorPosition);
                else
                    mCardCursor.moveToFirst();
            }
            updateCurrentCard();
        } catch (Exception e) {
            ErrorReporter.instance().AddCustomData("LearnCardsActivity::onResume", "cursor problem?");
            e.printStackTrace();

            if (mCardCursor != null) {
                mCardCursor.moveToFirst();
            }
        }

        if (settingsManager.getBoolPreference(context, ENABLE_SPLITSCREEN)) {
            setupSplitView();
        } else {
            setupNormalView();
        }

        if (modelManager.getLearningPhase() != REPEATING_LTM
                && (modelManager.getLearningPhase() != SIMPLE_LEARNING
                || modelManager.getLearningPhase() != NOTHING)) {
            findViewById(R.id.lTimerFrame).setVisibility(View.VISIBLE);
        }

        setupButtons();
        updateLearningPhase(); // Sets button visibility
        fillData();
    }

    @Override
    public void onPause() {
        super.onPause();
        mSavedCursorPosition = mCardCursor.getPosition();
        stopTimer();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.REQUEST_CODE_SAVE_DIALOG_NORMAL) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(context, R.string.saving_success, Toast.LENGTH_SHORT).show();
                paukerManager.setSaveRequired(false);
                modelManager.showExpireToast(context);
            } else {
                Toast.makeText(context, R.string.saving_error, Toast.LENGTH_SHORT).show();
            }
        }
        finish();
    }

    // Save UI state changes to the savedInstanceState.
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putLong(INSTANCESTATE_START_TIME, mStartTime);
        savedInstanceState.putInt(INSTANCESTATE_STM_START_TIME, uSTMStartTimeSeconds);
        savedInstanceState.putInt(INSTANCESTATE_USTM_START_TIME, sTMStartTimeSeconds);
        super.onSaveInstanceState(savedInstanceState);
    }

    // Restore UI state from the savedInstanceState.
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        Log.d("LearnCardsActivity::OnRestoreInstanceState", "Entry");
        super.onRestoreInstanceState(savedInstanceState);
        mStartTime = savedInstanceState.getLong(INSTANCESTATE_START_TIME);
        uSTMStartTimeSeconds = savedInstanceState.getInt(INSTANCESTATE_STM_START_TIME);
        sTMStartTimeSeconds = savedInstanceState.getInt(INSTANCESTATE_USTM_START_TIME);
        cursorLoaded();
        super.onRestoreInstanceState(savedInstanceState);
    }

    private void setLearningPhase(LearningPhase learningPhase) {
        modelManager.setLearningPhase(context, learningPhase);
        refreshCursor();
        updateCurrentCard();
        fillData();
    }

    public void showCard(View view) {
        if (settingsManager.getBoolPreference(context, ENABLE_SPLITSCREEN)) {
            screenTouched();
        } else {
            onLeftSwipe();
        }
    }

    public void nextCard(View view) {
        mCardPackAdapter.setCardLearned(mCardCursor.getLong(CardPackAdapter.KEY_ROWID_ID));

        if (!mCardCursor.isLast()) {
            mCardCursor.moveToNext();
            updateCurrentCard();
            fillData();
        }

        updateLearningPhase();
    }

    public void noClicked(View view) {
        mCardPackAdapter.setCardUnLearned(context, mCardCursor.getLong(CardPackAdapter.KEY_ROWID_ID));
        paukerManager.setSaveRequired(true);

        if (!mCardCursor.isLast()) {
            mCardCursor.moveToNext();
            updateCurrentCard();
            fillData();
        } else {
            completedLearning = true;
        }

        updateLearningPhase();
    }

    public void yesClicked(View view) {
        mCardPackAdapter.setCardLearned(mCardCursor.getLong(CardPackAdapter.KEY_ROWID_ID));
        paukerManager.setSaveRequired(true);

        if (!mCardCursor.isLast()) {
            mCardCursor.moveToNext();
            updateCurrentCard();
            fillData();
        } else {
            completedLearning = true;
        }

        updateLearningPhase();
    }

    public void skipWaiting(View view) {
        stopWaiting = true;
        updateLearningPhase();
    }

    public void mSettingsClicked(MenuItem item) {
        startActivity(new Intent(context, SettingsActivity.class));
    }

    //***************************************
    // Timers while learning
    // TODO move this to the model or the application -- application logic
    //****************************************

    private void startTimer() {
        Log.d("LearnCardsActivity::startTimer", "entry");
        mStartTime = SystemClock.uptimeMillis();
        mHandler.removeCallbacks(mUpdateTimeTask);
        mHandler.postDelayed(mUpdateTimeTask, 1000);
    }

    private void restartTimer() {
        mHandler.removeCallbacks(mUpdateTimeTask);
        mHandler.postDelayed(mUpdateTimeTask, 1000);
    }

    private void stopTimer() {
        mHandler.removeCallbacks(mUpdateTimeTask);
    }

    private void startUSTMTimer() {
        uSTMStartTimeSeconds = (int) (SystemClock.uptimeMillis() / 1000);
    }

    private void startSTMTimer() {
        sTMStartTimeSeconds = (int) (SystemClock.uptimeMillis() / 1000);
    }


    private boolean checkUSTMTimeout() {
        int ustmPref = Integer.parseInt(settingsManager.getStringPreference(context, USTM));
        int currentTime = (int) (SystemClock.uptimeMillis() / 1000);
        return ((currentTime - uSTMStartTimeSeconds) >= ustmPref);
    }

    private boolean checkSTMTimeout() {
        int currentTimeSeconds = (int) (SystemClock.uptimeMillis() / 1000);

        int stmPref = 60 * Integer.parseInt(settingsManager.getStringPreference(context, STM));
        return currentTimeSeconds - sTMStartTimeSeconds >= stmPref;
    }

    private void updateTimeText(int seconds, int minutes) {
        int _seconds = seconds;
        int _minutes = minutes;
        int currentTime = (int) (SystemClock.uptimeMillis() / 1000);
        int ustmTime = currentTime - uSTMStartTimeSeconds;

        int ustmPref = Integer.parseInt(settingsManager.getStringPreference(context, USTM));
        if (ustmTime >= ustmPref) {
            ustmTime = ustmPref;
        }

        int stmPref = Integer.parseInt(settingsManager.getStringPreference(context, STM));
        if (currentTime - sTMStartTimeSeconds >= 60 * stmPref) {
            _seconds = 0;
            _minutes = stmPref;
        }

        setTimerStrings(_minutes, _seconds, stmPref, ustmTime, ustmPref);
    }

    /**
     * Setzt die TimerStrings mit den richtigen Texten.
     * @param stmMin   Aktuelle Minuten vom KZG
     * @param stmSec   Aktuelle Sekunden vom KZG
     * @param stmPref  KZG-Zeit in den Einstellungen
     * @param ustm     Aktuelle Zeit vom UKZG
     * @param ustmPref UKZG-Zeit in den Einstellungen
     */
    private void setTimerStrings(int stmMin, int stmSec, int stmPref, int ustm, int ustmPref) {
        if (findViewById(R.id.lTimerFrame).getVisibility() == View.VISIBLE) {
            TextView stmTimer = findViewById(R.id.tKZGTimer);
            String text;
            if (stmSec < 10) {
                text = String.format(Locale.getDefault(),
                        "%s %d:0%d / %d:00min", getString(R.string.stm), stmMin, stmSec, stmPref);
            } else {
                text = String.format(Locale.getDefault(),
                        "%s %d:%d / %d:00min", getString(R.string.stm), stmMin, stmSec, stmPref);
            }
            stmTimer.setText(text);

            TextView ustmTimer = findViewById(R.id.tUKZGTimer);
            text = String.format(Locale.getDefault(), "%s %d / %ds", getString(R.string.ustm), ustm, ustmPref);
            ustmTimer.setText(text);
        }
    }
}
