package com.daniel.mobilepauker2.model.pauker_native;

public class Font {
    private boolean mBold;
    private boolean mItalic;
    private int mBackground;
    private int mTextColor;
    private int mSize;
    private String mFamily;

    public Font() {
        mBold = false;
        mItalic = false;
        mBackground = -1;
        mTextColor = -16777216;
        mSize = 12;
        mFamily = "Dialog";
    }

    public Font(String background, String bold, String family, String foreground, String italic, String size) {
        mBold = bold.equals("true");
        mItalic = italic.equals("true");
        mBackground = parseInt(background);
        mTextColor = parseInt(foreground);
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
        return mTextColor;
    }

    public int getTextSize() {
        return mSize;
    }

    public String getFamily() {
        return mFamily;
    }

    public void setBold(boolean mBold) {
        this.mBold = mBold;
    }

    public void setItalic(boolean mItalic) {
        this.mItalic = mItalic;
    }

    public void setBackground(int mBackground) {
        this.mBackground = mBackground;
    }

    public void setTextColor(int mForeground) {
        this.mTextColor = mForeground;
    }

    public void setSize(int mSize) {
        this.mSize = mSize;
    }
}
