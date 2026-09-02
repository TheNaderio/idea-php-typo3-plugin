package com.cedricziel.idea.typo3.startup;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Associates the .xlf extension with XML so translation files get XML support and completion.
 *
 * <p>See {@link TYPO3CMSPostStartupActivity} on why this is a {@link ProjectActivity}.
 */
public class XLFFFileTypePostStartupActivity implements ProjectActivity {

    @Nullable
    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        if (FileTypeManager.getInstance().getFileTypeByExtension("xlf") instanceof XmlFileType) {
            return Unit.INSTANCE;
        }

        // Changing a file type association is a write action, so it has to go back to the EDT -
        // but only once we know there is something to change.
        ApplicationManager.getApplication().invokeLater(() -> {
            WriteAction.run(() -> FileTypeManager.getInstance().associateExtension(XmlFileType.INSTANCE, "xlf"));

            Notifications.Bus.notify(
                new Notification(
                    "TYPO3 CMS Plugin",
                    "XLF File Type Association",
                    "The XLF File Type was re-assigned to XML to prevent errors with the XLIFF Plugin and allow autocompletion. Please re-index your projects.",
                    NotificationType.INFORMATION
                ),
                project
            );
        }, project.getDisposed());

        return Unit.INSTANCE;
    }
}
