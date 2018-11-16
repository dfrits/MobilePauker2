package com.daniel.mobilepauker2.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ankushgrover.hourglass.Hourglass;
import com.daniel.mobilepauker2.PaukerManager;
import com.daniel.mobilepauker2.R;
import com.daniel.mobilepauker2.model.CardPackAdapter;
import com.daniel.mobilepauker2.model.FlashCard;
import com.daniel.mobilepauker2.model.ModelManager.LearningPhase;
import com.daniel.mobilepauker2.model.SettingsManager;
import com.daniel.mobilepauker2.utils.Constants;
import com.daniel.mobilepauker2.utils.Log;

import java.util.Locale;
import java.util.Random;

import static android.view.View.VISIBLE;
import static com.daniel.mobilepauker2.model.ModelManager.LearningPhase.*;
import static com.daniel.mobilepauker2.model.SettingsManager.Keys.*;

public class LearnCardsActivity2 extends FlashCardSwipeScreenActivity {
    private final PaukerManager paukerManager = PaukerManager.instance();
    private final Context context = this;
    private boolean flipCardSides = false;
    private boolean completedLearning = false;
    private boolean repeatingLTM = false;
    private boolean stopWaiting = false;
    private boolean firstStart = true;
    private boolean ustmTimerFinished;
    private boolean stmTimerFinished;
    private Hourglass ustmTimer;
    private Hourglass stmTimer;
    private int ustmTotalTime;
    private int stmTotalTime;
    private TextView ustmTimerText;
    private TextView stmTimerText;
    private Button bNext;
    private Button bShowMe;
    private RelativeLayout lRepeatButtons;
    private RelativeLayout lSkipWaiting;
    private TextView sideA;
    private TextView sideB;
    private MenuItem pauseButton;
    private MenuItem restartButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        setContentView(R.layout.learn_cards);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (modelManager.getLearningPhase() != REPEATING_LTM
                && (modelManager.getLearningPhase() != SIMPLE_LEARNING
                || modelManager.getLearningPhase() != NOTHING)) {
            // A check on mActivitySetupOk is done here as onCreate is called even if the
            // super (FlashCardSwipeScreenActivity) onCreate fails to find any cards and calls finish()
            if (mActivitySetupOk) {
                initTimer();
                if (ustmTimer != null && stmTimer != null) {
                    findViewById(R.id.lTimerFrame).setVisibility(VISIBLE);
                    startAllTimer();
                }
            }
        } else if (modelManager.getLearningPhase() == REPEATING_LTM) {
            repeatingLTM = true;
        }

        initButtons();
        sideA = findViewById(R.id.tCardSideA);
        sideB = findViewById(R.id.tCardSideB);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!firstStart && !restartButton.isVisible()) {
            restartTimer();
        }

        firstStart = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSavedCursorPosition = mCardCursor.getPosition();
        pauseTimer();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.REQUEST_CODE_EDIT_CARD && resultCode == RESULT_OK) {
            updateCurrentCard();
            fillInData(flipCardSides);
        }
    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.exit_learning_dialog)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .create().show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.learning_cards, menu);

        pauseButton = menu.findItem(R.id.mPauseButton);
        restartButton = menu.findItem(R.id.mRestartButton);

        if (modelManager.getLearningPhase() == REPEATING_LTM
                || (modelManager.getLearningPhase() == SIMPLE_LEARNING
                && modelManager.getLearningPhase() == NOTHING)) {
            pauseButton.setVisible(false);
            restartButton.setVisible(false);
        }

        return true;
    }

    private void initTimer() {
        if (ustmTimer == null) {
            ustmTotalTime = Integer.parseInt(settingsManager.getStringPreference(context, USTM));
            ustmTimer = new Hourglass(ustmTotalTime * 1000) {
                @Override
                public void onTimerTick(long timeRemaining) {
                    if (timeRemaining > 0) {
                        if (ustmTimerText.getVisibility() == VISIBLE
                                && !ustmTimerFinished) {
                            String timerText;
                            int totalSec = (int) (timeRemaining / 1000);
                            int timeElapsed = ustmTotalTime - totalSec;
                            int sec = timeElapsed % 60;

                            timerText = String.format(Locale.getDefault()
                                    , "%s %d / %ds", getString(R.string.ustm), sec, ustmTotalTime);
                            ustmTimerText.setText(timerText);
                        }
                    } else {
                        stopUSTMTimer();// evtl. wieder zu onTimerFinish() zurücktun
                    }
                }

                @Override
                public void onTimerFinish() {
                }
            };
            ustmTimerText = findViewById(R.id.tUKZGTimer);
        }
        if (stmTimer == null) {
            stmTotalTime = Integer.parseInt(settingsManager.getStringPreference(context, STM));
            stmTimer = new Hourglass(stmTotalTime * 60 * 1000) {
                @Override
                public void onTimerTick(long timeRemaining) {
                    if (timeRemaining > 0
                            && stmTimerText.getVisibility() == VISIBLE
                            && !stmTimerFinished) {
                        String timerText;
                        int totalSec = (int) (timeRemaining / 1000);
                        int timeElapsed = stmTotalTime * 60 - totalSec;
                        int sec = timeElapsed % 60;
                        int min = timeElapsed / 60;
                        if (sec < 10) {
                            timerText = String.format(Locale.getDefault(),
                                    "%s %d:0%d / %d:00min", getString(R.string.stm), min, sec, stmTotalTime);
                        } else {
                            timerText = String.format(Locale.getDefault(),
                                    "%s %d:%d / %d:00min", getString(R.string.stm), min, sec, stmTotalTime);
                        }
                        stmTimerText.setText(timerText);
                    } else {
                        stopSTMTimer();
                    }
                }

                @Override
                public void onTimerFinish() {
                }
            };
            stmTimerText = findViewById(R.id.tKZGTimer);
        }
    }

    private void startAllTimer() {
        startUSTMTimer();
        startSTMTimer();
    }

    private void startUSTMTimer() {
        if (ustmTimer != null && !ustmTimer.isPaused()) {
            ustmTimer.startTimer();
            ustmTimerFinished = false;
        }
    }

    private void startSTMTimer() {
        if (stmTimer != null && !stmTimerFinished) {
            stmTimer.startTimer();
            stmTimerFinished = false;
        }
    }

    private void pauseTimer() {
        if (ustmTimer != null && !ustmTimer.isPaused()) {
            ustmTimer.pauseTimer();
            stmTimer.pauseTimer();
        }
    }

    private void restartTimer() {
        if (ustmTimer != null && ustmTimer.isPaused() && !ustmTimerFinished) {
            ustmTimer.resumeTimer();
        }
        if (stmTimer != null && stmTimer.isPaused() && !stmTimerFinished) {
            stmTimer.resumeTimer();
        }
    }

    private void stopUSTMTimer() {
        if (ustmTimer != null && !ustmTimerFinished) {
            ustmTimer.stopTimer();
            ustmTimerFinished = true;
            String timerText = String.format(Locale.getDefault()
                    , "%s %s", getString(R.string.ustm), getString(R.string.timer_finished));
            ustmTimerText.setText(timerText);
        }
    }

    private void stopSTMTimer() {
        if (stmTimer != null && !stmTimerFinished) {
            stmTimer.stopTimer();
            stmTimerFinished = true;
            String timerText = String.format(Locale.getDefault()
                    , "%s %s", getString(R.string.stm), getString(R.string.timer_finished));
            stmTimerText.setText(timerText);
        }
    }

    private void updateLearningPhase() {
        LearningPhase learningPhase = modelManager.getLearningPhase();

        boolean zeroUnlearnedCards = false;
        boolean zeroUSTMCards = false;
        boolean zeroSTMCards = false;

        if (modelManager.getUnlearnedBatchSize() <= 0) {
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
                    finishLearning();
                } else {
                    setButtonVisibilityRepeating();
                }
                break;
            }

            case FILLING_USTM: {
                setButtonVisibilityFilling();

                if (stmTimerFinished) // STM timeout so go straight to repeating ustm cards
                {
                    //Toast.makeText(this, getString(R.string.learncards_repeat_ustm), Toast.LENGTH_SHORT).show();
                    setLearningPhase(LearningPhase.REPEATING_USTM);
                    updateLearningPhase();
                } else if (zeroUnlearnedCards && !ustmTimerFinished) {
                    setLearningPhase(LearningPhase.WAITING_FOR_USTM);
                    updateLearningPhase();
                } else if (ustmTimerFinished) {
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
                if (ustmTimerFinished || stopWaiting) {
                    //Toast.makeText(this, getString(R.string.learncards_repeat_ustm), Toast.LENGTH_LONG).show();
                    stopWaiting = false;
                    stopUSTMTimer();
                    setLearningPhase(LearningPhase.REPEATING_USTM);
                    updateLearningPhase();
                }

                break;
            }

            case REPEATING_USTM: {
                setButtonVisibilityRepeating();

                if (zeroUSTMCards) // We have learned all the ustm cards
                {
                    if (stmTimerFinished) //STM timer has timed out so move to repeating STM
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
                if (stmTimerFinished || stopWaiting) {
                    //Toast.makeText(this, getString(R.string.learncards_repeat_stm), Toast.LENGTH_SHORT).show();
                    stopWaiting = false;
                    stopSTMTimer();
                    setLearningPhase(LearningPhase.REPEATING_STM);
                    updateLearningPhase();
                }
                break;
            }

            case REPEATING_STM: {
                if (zeroSTMCards) {
                    //Toast.makeText(this, getString(R.string.learncards_finished_learning), Toast.LENGTH_LONG).show();
                    finishLearning();
                } else {
                    setButtonVisibilityRepeating();
                }
                break;
            }

            case REPEATING_LTM: {
                if (modelManager.getExpiredCardsSize() <= 0) {
                    //Toast.makeText(this, getString(R.string.learncards_finished_learning), Toast.LENGTH_LONG).show();
                    finishLearning();
                } else {
                    setButtonVisibilityRepeating();
                }
                break;
            }
        }
    }

    private void setLearningPhase(LearningPhase newLearningsPhase) {
        //Neue Phase dem Modelmanager mitteilen und Deck aktualisieren
        modelManager.setLearningPhase(context, newLearningsPhase);
        //Cursor an erste Stelle setzen
        // setCursorToFirst();
        refreshCursor();
    }

    private void finishLearning() {
        if (settingsManager.getBoolPreference(context, AUTO_SAVE)) {
            startActivityForResult(new Intent(context, SaveDialog.class), Constants.REQUEST_CODE_SAVE_DIALOG_NORMAL);
        } else {
            paukerManager.setSaveRequired(true);
            finish();
        }
    }

    public void initButtons() {
        bNext = findViewById(R.id.bNext);
        bShowMe = findViewById(R.id.bShowMe);
        lRepeatButtons = findViewById(R.id.lBRepeat);
        lSkipWaiting = findViewById(R.id.lBSkipWaiting);

        setButtonsVisibility();
    }

    private void setButtonsVisibility() {
        LearningPhase learningPhase = modelManager.getLearningPhase();
        if (learningPhase == WAITING_FOR_USTM || learningPhase == WAITING_FOR_STM) {
            setButtonVisibilityWaiting();
        } else if (learningPhase == SIMPLE_LEARNING || learningPhase == REPEATING_USTM
                || learningPhase == REPEATING_STM || learningPhase == REPEATING_LTM) {
            setButtonVisibilityRepeating();
        } else if (learningPhase == FILLING_USTM) {
            setButtonVisibilityFilling();
        }
    }

    private void setButtonVisibilityWaiting() {
        bNext.setVisibility(View.GONE);
        bShowMe.setVisibility(View.GONE);
        lRepeatButtons.setVisibility(View.GONE);
        lSkipWaiting.setVisibility(VISIBLE);
    }

    private void setButtonVisibilityRepeating() {
        bNext.setVisibility(View.GONE);
        bShowMe.setVisibility(VISIBLE);
        lRepeatButtons.setVisibility(View.GONE);
        lSkipWaiting.setVisibility(View.GONE);
    }

    private void setButtonVisibilityFilling() {
        bNext.setVisibility(VISIBLE);
        bShowMe.setVisibility(View.GONE);
        lRepeatButtons.setVisibility(View.GONE);
        lSkipWaiting.setVisibility(View.GONE);
    }

    @Override
    void updateCurrentCard() {
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
            Toast.makeText(context, R.string.load_card_data_error, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public void screenTouched() {
        LearningPhase learningPhase = modelManager.getLearningPhase();
        if (learningPhase == REPEATING_LTM
                || learningPhase == REPEATING_STM
                || learningPhase == REPEATING_USTM) {

            if (flipCardSides) {
                currentCard.setSide(FlashCard.SideShowing.SIDE_A);
            } else {
                currentCard.setSide(FlashCard.SideShowing.SIDE_B);
            }

            fillInData(flipCardSides);
            bShowMe.setVisibility(View.GONE);
            lRepeatButtons.setVisibility(VISIBLE);
        }
    }

    @Override
    protected void fillData() {
        // Prüfen, ob getauscht werden soll
        boolean flipCardSides = hasCardsToBeFlipped();

        fillInData(flipCardSides);
    }

    private void fillInData(boolean flipCardSides) {
        // Daten setzen
        // TODO Schriftgröße, Kursiv?, Fett?, Font, Vorder-, Hintergrund, Art der Wiederholung


        LearningPhase learningPhase = modelManager.getLearningPhase();
        // Layoutcontents setzen
        if (flipCardSides) {
            fillSideA(R.string.back, currentCard.getSideBText());

            // Now work out if we should show side A in bottom box
            String sideAText = "";
            if (currentCard.getSide() == FlashCard.SideShowing.SIDE_A
                    || learningPhase == SIMPLE_LEARNING
                    || learningPhase == FILLING_USTM) {
                sideAText = currentCard.getSideAText();
            }
            fillSideB(R.string.front, sideAText);
        } else {
            // Card sides not flipped so show side A in top box!
            fillSideA(R.string.front, currentCard.getSideAText());

            String sideBText = "";
            if (currentCard.getSide() == FlashCard.SideShowing.SIDE_B
                    || learningPhase == SIMPLE_LEARNING
                    || learningPhase == FILLING_USTM) {
                sideBText = currentCard.getSideBText();
            }
            fillSideB(R.string.back, sideBText);
        }

        fillHeader();
    }

    private boolean hasCardsToBeFlipped() {
        String flipMode = settingsManager.getStringPreference(context, SettingsManager.Keys.FLIP_CARD_SIDES);
        LearningPhase learningPhase = modelManager.getLearningPhase();
        if (learningPhase == REPEATING_LTM || learningPhase == REPEATING_STM || learningPhase == REPEATING_USTM) {
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

        return flipCardSides;
    }

    private void fillSideA(int titleResource, String text) {
        ((TextView) findViewById(R.id.titelCardSideA)).setText(getString(titleResource));
        sideA.setText(text);
    }

    private void fillSideB(int titleResource, String text) {
        ((TextView) findViewById(R.id.titelCardSideB)).setText(getString(titleResource));
        if (text.isEmpty()) {
            sideB.setHint(getString(R.string.learncards_show_hint));
        }
        sideB.setText(text);
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

    @Override
    protected void cursorLoaded() {
        setCursorToFirst();
        updateCurrentCard();
        fillData();
    }

    public void mEditClicked(MenuItem item) {
        Intent intent = new Intent(context, EditCardActivity.class);
        intent.putExtra(Constants.CURSOR_POSITION, mCardCursor.getPosition());
        startActivityForResult(intent, Constants.REQUEST_CODE_EDIT_CARD);
    }

    public void mDeleteClicked(MenuItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(R.string.delete_card_message)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Muss vorher gespeichert werden, da sonst im Nachhinein der Wert
                        // verfälscht werden kann!
                        boolean isLast = mCardCursor.isLast();

                        if (modelManager.deleteCard(mCardCursor.getPosition())) {
                            if (!isLast) {
                                updateCurrentCard();
                                fillData();
                            } else {
                                // Letzte Karte oder Timer abgelaufen. Darum Lernphase aktualisieren
                                updateLearningPhase();
                            }
                        } else {
                            Toast.makeText(context, "Löschen nicht möglich!", Toast.LENGTH_SHORT).show();
                        }
                        dialog.cancel();
                    }
                });
        builder.create().show();
    }

    public void mPauseTimerClicked(MenuItem item) {
        if (restartButton != null) {
            pauseTimer();
            item.setVisible(false);
            restartButton.setVisible(true);

            if (ustmTimerText != null && ustmTimerText.getVisibility() == VISIBLE) {
                ustmTimerText.setText(R.string.ustm_timer_paused);
            }
            if (stmTimerText != null && stmTimerText.getVisibility() == VISIBLE) {
                stmTimerText.setText(R.string.stm_timer_paused);
            }
        } else {
            Toast.makeText(context, R.string.pause_timer_error, Toast.LENGTH_LONG).show();
        }
    }

    public void mRestartTimerClicked(MenuItem item) {
        if (pauseButton != null) {
            restartTimer();
            item.setVisible(false);
            pauseButton.setVisible(true);
        } else {
            Toast.makeText(context, R.string.restart_timer_error, Toast.LENGTH_LONG).show();
        }
    }

    // Aktionen der Buttons
    public void nextCard(View view) {
        // Karte ein Deck weiterschieben
        mCardPackAdapter.setCardLearned(mCardCursor.getLong(CardPackAdapter.KEY_ROWID_ID));

        if (!mCardCursor.isLast() && !ustmTimerFinished) {
            mCardCursor.moveToNext();
            updateCurrentCard();
            fillData();
        } else {
            // Letzte Karte oder Timer abgelaufen. Darum Lernphase aktualisieren
            updateLearningPhase();
        }
    }

    public void skipWaiting(View view) {
        stopWaiting = true;
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

    public void showCard(View view) {
        screenTouched();
    }
}
