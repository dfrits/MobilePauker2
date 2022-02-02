package de.daniel.mobilepauker2.application

import dagger.Component
import de.daniel.mobilepauker2.data.DataManager
import de.daniel.mobilepauker2.data.SaveAsDialog
import de.daniel.mobilepauker2.dropbox.DropboxAccDialog
import de.daniel.mobilepauker2.dropbox.SyncDialog
import de.daniel.mobilepauker2.editcard.AbstractEditCard
import de.daniel.mobilepauker2.editcard.AddCard
import de.daniel.mobilepauker2.learning.FlashCardSwipeScreen
import de.daniel.mobilepauker2.learning.LearnCards
import de.daniel.mobilepauker2.lesson.EditDescription
import de.daniel.mobilepauker2.lesson.LessonManager
import de.daniel.mobilepauker2.lessonimport.LessonImport
import de.daniel.mobilepauker2.lessonimport.LessonImportAdapter
import de.daniel.mobilepauker2.lessonimport.LessonReceiver
import de.daniel.mobilepauker2.mainmenu.MainMenu
import de.daniel.mobilepauker2.models.view.MPEditText
import de.daniel.mobilepauker2.models.view.MPTextView
import de.daniel.mobilepauker2.notification.AlarmNotificationReceiver
import de.daniel.mobilepauker2.notification.NotificationService
import de.daniel.mobilepauker2.search.Search
import de.daniel.mobilepauker2.settings.SettingsFragmentDropbox
import de.daniel.mobilepauker2.settings.SettingsFragmentMain
import de.daniel.mobilepauker2.settings.SettingsFragmentNotifications
import de.daniel.mobilepauker2.shortcut.ShortcutReceiver
import de.daniel.mobilepauker2.statistics.ChartAdapter
import de.daniel.mobilepauker2.utils.Toaster
import javax.inject.Singleton

@Singleton
@Component(modules = [ProviderModule::class])
interface ApplicationSingletonComponent {

    fun inject(manager: DataManager)
    fun inject(manager: LessonManager)
    fun inject(adapter: ChartAdapter)
    fun inject(utils: Toaster)
    fun inject(mainMenu: MainMenu)
    fun inject(lessonImport: LessonImport)
    fun inject(lessonImportAdapter: LessonImportAdapter)
    fun inject(mpEditText: MPEditText)
    fun inject(abstractEditCard: AbstractEditCard)
    fun inject(addCard: AddCard)
    fun inject(settingsFragmentMain: SettingsFragmentMain)
    fun inject(settingsFragmentNotifications: SettingsFragmentNotifications)
    fun inject(editDescription: EditDescription)
    fun inject(mpTextView: MPTextView)
    fun inject(search: Search)
    fun inject(saveAsDialog: SaveAsDialog)
    fun inject(dropboxAccDialog: DropboxAccDialog)
    fun inject(syncDialog: SyncDialog)
    fun inject(shortcutReceiver: ShortcutReceiver)
    fun inject(notificationService: NotificationService)
    fun inject(alarmNotificationReceiver: AlarmNotificationReceiver)
    fun inject(flashCardSwipeScreen: FlashCardSwipeScreen)
    fun inject(learnCards: LearnCards)
    fun inject(lessonReceiver: LessonReceiver)
    fun inject(settingsFragmentDropbox: SettingsFragmentDropbox)
}