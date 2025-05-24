import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

class PluginStartupActivity : StartupActivity {
    override fun runActivity(project: Project) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup("MyPlugin.NotificationGroup")
                .createNotification(
                        "My Plugin Loaded!",
                        "Your plugin has been successfully loaded and is working!",
                        NotificationType.INFORMATION
                ).notify(project)
    }
}