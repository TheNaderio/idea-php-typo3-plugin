package com.cedricziel.idea.typo3;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class IdeHelper {
    /**
     * @author Daniel Espendiller <daniel@espendiller.net>
     */
    public static void notifyEnableMessage(final Project project) {
        // Notification.setListener with HTML links is deprecated; the supported form is actions.
        Notification notification = new Notification(
            "TYPO3 CMS Plugin",
            "TYPO3 CMS Plugin",
            "Enable the TYPO3 CMS Plugin with auto configuration, or dismiss further messages",
            NotificationType.INFORMATION
        );

        notification.addAction(NotificationAction.createSimple("Enable", () -> {
            enablePluginAndConfigure(project);

            Notifications.Bus.notify(
                new Notification("TYPO3 CMS Plugin", "TYPO3 CMS Plugin", "Plugin enabled", NotificationType.INFORMATION),
                project
            );

            notification.expire();
        }));

        notification.addAction(NotificationAction.createSimple("Project settings", () -> {
            TYPO3CMSProjectSettings.showSettings(project);

            notification.expire();
        }));

        notification.addAction(NotificationAction.createSimple("Dismiss", () -> {
            // user doesn't want to see the notification again
            TYPO3CMSProjectSettings.getInstance(project).dismissEnableNotification = true;

            notification.expire();
        }));

        Notifications.Bus.notify(notification, project);
    }

    private static void enablePluginAndConfigure(@NotNull Project project) {
        TYPO3CMSProjectSettings.getInstance(project).pluginEnabled = true;
    }
}
