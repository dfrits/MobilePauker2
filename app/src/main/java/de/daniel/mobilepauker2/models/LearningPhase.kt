package de.daniel.mobilepauker2.models

import androidx.lifecycle.LiveData

enum class LearningPhase {
    /**
     * not learning
     */
    NOTHING,

    /**
     * Just browsing the new cards - not learning
     */
    BROWSE_NEW,

    /**
     * Not using USTM or STM memory and timers
     */
    SIMPLE_LEARNING,

    /**
     * the phase of filling the ultra short term memory
     */
    FILLING_USTM,

    /**
     * the phase of waiting for the ultra short term memory
     */
    WAITING_FOR_USTM,

    /**
     * the phase of repeating the ultra short term memory
     */
    REPEATING_USTM,

    /**
     * the phase of waiting for the short term memory
     */
    WAITING_FOR_STM,

    /**
     * the phase of repeating the short term memory
     */
    REPEATING_STM,

    /**
     * the phase of repeating the long term memory
     */
    REPEATING_LTM;

    companion object{
        var currentPhase: LearningPhase = NOTHING

        fun setLearningPhase(learningPhase: LearningPhase) {
            currentPhase = learningPhase
        }
    }
}