package com.daniel.mobilepauker2.model.pauker_native;

public class Font {
    String mBackground = "-1";
    String mBold = "false";
    String mFamily = "Dialog";
    String mForeground = "-16777216";
    String mItalic = "false";
    String mSize = "12";

    public Font() {

    }

    public Font(String background, String bold, String family, String foreground, String italic, String size) {
        mBackground = background;
        mBold = bold;
        mFamily = family;
        mForeground = foreground;
        mItalic = italic;
        mSize = size;
    }

    public String getStyle() {
        return mFamily;
    }

    public String getBackgroundColor() {
        return mBackground;
    }


    public String getBold() {
        return mBold;
    }

    public String getFamily() {
        return mFamily;
    }

    public String getForeground() {
        return mForeground;
    }

    public String getItalic() {
        return mItalic;
    }

    public String getSize() {
        return mSize;
    }

    public void setSize(String mSize) {
        this.mSize = mSize;
    }
}
