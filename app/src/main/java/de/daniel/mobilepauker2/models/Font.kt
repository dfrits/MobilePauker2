package de.daniel.mobilepauker2.models

class Font {
    var isBold: Boolean
    var isItalic: Boolean
    var backgroundColor: Int
        private set
    var textColor: Int
    var textSize: Int
        private set
    var family: String
        private set

    constructor() {
        isBold = false
        isItalic = false
        backgroundColor = -1
        textColor = -16777216
        textSize = 12
        family = "Dialog"
    }

    constructor(
        background: String,
        bold: String,
        family: String,
        foreground: String,
        italic: String,
        size: String
    ) {
        isBold = bold == "true"
        isItalic = italic == "true"
        backgroundColor = parseInt(background)
        textColor = parseInt(foreground)
        textSize = parseInt(size)
        this.family = family
    }

    private fun parseInt(value: String): Int {
        return try {
            value.toInt()
        } catch (ignored: NumberFormatException) {
            -1
        }
    }

    fun setBackground(mBackground: Int) {
        backgroundColor = mBackground
    }

    fun setSize(mSize: Int) {
        textSize = mSize
    }
}