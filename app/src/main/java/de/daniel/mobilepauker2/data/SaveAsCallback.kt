package de.daniel.mobilepauker2.data

interface SaveAsCallback {
    fun okClicked(fileName: String)
    fun cancelClicked()
}