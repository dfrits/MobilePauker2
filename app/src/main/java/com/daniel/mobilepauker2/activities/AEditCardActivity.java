package com.daniel.mobilepauker2.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.daniel.mobilepauker2.PaukerManager;
import com.daniel.mobilepauker2.R;
import com.daniel.mobilepauker2.model.FlashCard;
import com.daniel.mobilepauker2.model.MPEditText;
import com.daniel.mobilepauker2.model.ModelManager;
import com.daniel.mobilepauker2.model.TextDrawable;
import com.daniel.mobilepauker2.model.pauker_native.Font;
import com.daniel.mobilepauker2.utils.Constants;
import com.rarepebble.colorpicker.ColorPickerView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Created by dfritsch on 21.11.2018.
 * MobilePauker++
 */

public abstract class AEditCardActivity extends AppCompatActivity {
    private boolean fontChanged;
    protected final Context context = this;
    protected final ModelManager modelManager = ModelManager.instance();
    protected FlashCard flashCard;
    //SideA
    protected MPEditText sideAEditText;
    protected String initSideAText = "";
    protected int initSideATSize;
    protected int initSideATColor;
    protected int initSideABColor;
    protected boolean initSideABold;
    protected boolean initSideAItalic;
    protected boolean initIsRepeatedByTyping;
    //SideB
    protected MPEditText sideBEditText;
    protected String initSideBText = "";
    protected int initSideBTSize;
    protected int initSideBTColor;
    protected int initSideBBColor;
    protected boolean initSideBBold;
    protected boolean initSideBItalic;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.edit_card);

        sideAEditText = findViewById(R.id.eTSideA);
        sideBEditText = findViewById(R.id.eTSideB);
        fontChanged = false;
    }

    @Override
    protected void onPause() {
        super.onPause();

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null && imm.isAcceptingText()) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    @Override
    public void onBackPressed() {
        resetCardAndFinish();
    }

    protected void resetCardAndFinish() {
        resetCardSides(null);
        finish();
    }

    /**
     * Überprüft beide Felder ob Änderungen zum Initialstatus vorhanden sind. Die Schriftart wird
     * dabei nicht überprüft.
     * @return True, wenn Unterschiede gefunden wurden
     */
    protected boolean detectChanges() {
        return !sideAEditText.getText().toString().equals(initSideAText)
                || !sideBEditText.getText().toString().equals(initSideBText)
                || fontChanged;
    }

    public void okClicked(View view) {
    }

    public void resetCardSides(View view) {
        fontChanged = false;
        flashCard.setRepeatByTyping(initIsRepeatedByTyping);
        sideAEditText.setText(initSideAText);
        sideBEditText.setText(initSideBText);
        Font font = flashCard.getFrontSide().getFont();
        if (font != null) {
            font.setSize(initSideATSize);
            font.setBackground(initSideABColor);
            font.setTextColor(initSideATColor);
            font.setBold(initSideABold);
            font.setItalic(initSideAItalic);
        }
        sideAEditText.setFont(font);
        font = flashCard.getReverseSide().getFont();
        if (font != null) {
            font.setSize(initSideBTSize);
            font.setBackground(initSideBBColor);
            font.setTextColor(initSideBTColor);
            font.setBold(initSideBBold);
            font.setItalic(initSideBItalic);
        }
        sideBEditText.setFont(font);
        sideAEditText.requestFocus();
        sideAEditText.setSelection(initSideAText.length(), initSideAText.length());
    }

    public void editFontA(View view) {
        PopupMenu popupMenu = createPopupMenu(view, flashCard.getFrontSide().getFont(), true);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Font font = flashCard.getFrontSide().getFont();
                if (font == null) {
                    font = new Font();
                    flashCard.getFrontSide().setFont(font);
                    fontChanged = true;
                }
                return setNewFontDetails(item, font, sideAEditText);
            }
        });
    }

    public void editFontB(View view) {
        PopupMenu popupMenu = createPopupMenu(view, flashCard.getReverseSide().getFont(), false);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Font font = flashCard.getReverseSide().getFont();
                if (font == null) {
                    font = new Font();
                    flashCard.getReverseSide().setFont(font);
                    fontChanged = true;
                }
                return setNewFontDetails(item, font, sideBEditText);
            }
        });
    }

    private boolean setNewFontDetails(MenuItem item, final Font font, final MPEditText cardSide) {
        switch (item.getItemId()) {
            case R.id.mBold:
                font.setBold(!font.isBold());
                fontChanged = true;
                break;
            case R.id.mItalic:
                font.setItalic(!font.isItalic());
                fontChanged = true;
                break;
            case R.id.mBackground:
                final ColorPickerView bcPicker = new ColorPickerView(context);
                bcPicker.showHex(false);
                bcPicker.setOriginalColor(font.getBackgroundColor());
                bcPicker.setCurrentColor(PreferenceManager.getDefaultSharedPreferences(context)
                        .getInt(Constants.LAST_BACK_COLOR_CHOICE, font.getBackgroundColor()));
                bcPicker.showAlpha(false);
                AlertDialog.Builder bcBuilder = new AlertDialog.Builder(context);
                bcBuilder.setView(bcPicker)
                        .setTitle(R.string.background)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                int color = bcPicker.getColor();
                                PreferenceManager.getDefaultSharedPreferences(context).edit()
                                        .putInt(Constants.LAST_BACK_COLOR_CHOICE, color).apply();
                                font.setBackground(color);
                                cardSide.setFont(font);
                                fontChanged = true;
                            }
                        })
                        .setNeutralButton(R.string.cancel, null);
                bcBuilder.create().show();
                break;
            case R.id.mTextColor:
                final ColorPickerView tcPicker = new ColorPickerView(context);
                tcPicker.showHex(false);
                tcPicker.setOriginalColor(font.getTextColor());
                tcPicker.setCurrentColor(PreferenceManager.getDefaultSharedPreferences(context)
                        .getInt(Constants.LAST_TEXT_COLOR_CHOICE, font.getTextColor()));
                tcPicker.showAlpha(false);
                AlertDialog.Builder tcBuilder = new AlertDialog.Builder(context);
                tcBuilder.setView(tcPicker)
                        .setTitle(R.string.text_color)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                int color = tcPicker.getColor();
                                PreferenceManager.getDefaultSharedPreferences(context).edit()
                                        .putInt(Constants.LAST_TEXT_COLOR_CHOICE, color).apply();
                                font.setTextColor(color);
                                cardSide.setFont(font);
                                fontChanged = true;
                            }
                        })
                        .setNeutralButton(R.string.cancel, null);
                tcBuilder.create().show();
                break;
            case R.id.mTextSize:
                editTextSize(cardSide, font);
                break;
            case R.id.mRepeatType:
                fontChanged = true;
                flashCard.setRepeatByTyping(!flashCard.isRepeatedByTyping());
                break;
            default:
                return false;
        }
        cardSide.setFont(font);
        return true;
    }

    @SuppressLint("InflateParams")
    private void editTextSize(final MPEditText cardSide, final Font font) {
        final EditText view = (EditText) getLayoutInflater()
                .inflate(R.layout.edit_text_size_dialog, null);
        final String oldSize = String.valueOf(font.getTextSize());
        view.setText(oldSize);
        view.setSelection(0, oldSize.length());
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(view)
                .setTitle(getString(R.string.text_size))
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String s = view.getText().toString();
                        try {
                            int newSize = Integer.parseInt(s);
                            if (newSize != font.getTextSize()) {
                                font.setSize(Integer.parseInt(s));
                            }
                            cardSide.setFont(font);
                            fontChanged = true;
                        } catch (NumberFormatException e) {
                            PaukerManager.showToast((Activity) context, R.string.number_format_error
                                    , Toast.LENGTH_SHORT);
                        }
                    }
                })
                .setNeutralButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(view, 0);
                }
            }
        });
        dialog.show();
    }

    private PopupMenu createPopupMenu(View view, Font font, boolean showRepeatTypeMenu) {
        PopupMenu dropdownMenu = new PopupMenu(context, view);
        Menu menu = dropdownMenu.getMenu();
        dropdownMenu.getMenuInflater().inflate(R.menu.edit_font_pop_up, menu);

        setIcons(menu, font, showRepeatTypeMenu);

        try {
            Field[] fields = dropdownMenu.getClass().getDeclaredFields();
            for (Field field : fields) {
                if ("mPopup".equals(field.getName())) {
                    field.setAccessible(true);
                    Object menuPopupHelper = field.get(dropdownMenu);
                    Class<?> classPopupHelper = Class.forName(menuPopupHelper.getClass().getName());
                    Method setForceIcons = classPopupHelper.getMethod("setForceShowIcon", boolean.class);
                    setForceIcons.invoke(menuPopupHelper, true);
                    break;
                }
            }
        } catch (Exception e) {
            return dropdownMenu;
        }
        dropdownMenu.show();
        return dropdownMenu;
    }

    private void setIcons(Menu menu, Font font, boolean showRepeatTypeMenu) {
        font = font == null ? new Font() : font;

        // Size
        TextDrawable circle = new TextDrawable(String.valueOf(font.getTextSize()));
        menu.findItem(R.id.mTextSize).setIcon(circle);

        // Background
        circle = new TextDrawable(font.getBackgroundColor());
        menu.findItem(R.id.mBackground).setIcon(circle);

        // TextColor
        circle = new TextDrawable(font.getTextColor());
        menu.findItem(R.id.mTextColor).setIcon(circle);

        // Bold
        circle = new TextDrawable("B");
        circle.setBold(font.isBold());
        menu.findItem(R.id.mBold).setIcon(circle);

        // Italic
        circle = new TextDrawable("I");
        circle.setItalic(font.isItalic());
        menu.findItem(R.id.mItalic).setIcon(circle);

        // Repeat Type --> Nur bei der Vorderseite
        if (showRepeatTypeMenu) {
            MenuItem item = menu.findItem(R.id.mRepeatType);
            item.setVisible(true);
            Drawable icon = flashCard.isRepeatedByTyping() ?
                    getDrawable(R.drawable.rt_typing) :
                    getDrawable(R.drawable.rt_thinking);
            item.setIcon(icon);
        }
    }
}
