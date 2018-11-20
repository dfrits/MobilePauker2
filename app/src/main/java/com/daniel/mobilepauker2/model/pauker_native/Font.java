package com.daniel.mobilepauker2.model.pauker_native;

public class Font {
    private final boolean mBold;
    private final boolean mItalic;
    private final int mBackground;
    private final int mForeground;
    private final int mSize;
    private final String mFamily;

    public Font() {
        mBold = false;
        mItalic = false;
        mBackground = -1;
        mForeground = -16777216;
        mSize = 16;
        mFamily = "Dialog";
    }

    public Font(String background, String bold, String family, String foreground, String italic, String size) {
        mBold = bold.equals("true");
        mItalic = italic.equals("true");
        mBackground = parseInt(background);
        mForeground = parseInt(foreground);
        mSize = parseInt(size);
        mFamily = family;
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    public boolean isBold() {
        return mBold;
    }

    public boolean isItalic() {
        return mItalic;
    }

    public int getBackgroundColor() {
        return mBackground;
    }

    public int getTextColor() {
        return mForeground;
    }

    public int getTextSize() {
        return mSize;
    }

    public String getFamily() {
        return mFamily;
    }
}
