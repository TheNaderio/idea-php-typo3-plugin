package com.cedricziel.idea.typo3.startup;

import com.cedricziel.idea.typo3.IdeHelper;
import com.cedricziel.idea.typo3.TYPO3CMSProjectSettings;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.concurrency.AppExecutorUtil;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Offers to enable the plugin when the opened project looks like a TYPO3 installation.
 *
 * <p>Implemented as a {@link ProjectActivity} because the platform rejects the older
 * StartupActivity. Its {@code execute} is a Kotlin suspending function, which from Java means
 * taking a {@link Continuation} and returning {@link Unit#INSTANCE} to signal "finished without
 * suspending".
 */
public class TYPO3CMSPostStartupActivity implements ProjectActivity {

    @Nullable
    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        // Detection walks the VFS and queries an index - too slow for the EDT, and it needs the
        // indexes to be ready.
        ReadAction.<Void>nonBlocking(() -> {
                checkProject(project);
                return null;
            })
            .inSmartMode(project)
            .expireWith(project)
            .submit(AppExecutorUtil.getAppExecutorService());

        return Unit.INSTANCE;
    }

    public boolean isEnabled(@Nullable Project project) {

        return project != null && TYPO3CMSProjectSettings.getInstance(project).pluginEnabled;
    }

    private void checkProject(@NotNull Project project) {
        if (!this.isEnabled(project) && !notificationIsDismissed(project) && containsPluginRelatedFiles(project)) {
            IdeHelper.notifyEnableMessage(project);
        }
    }

    private boolean notificationIsDismissed(@NotNull Project project) {

        return TYPO3CMSProjectSettings.getInstance(project).dismissEnableNotification;
    }

    private boolean containsPluginRelatedFiles(@NotNull Project project) {
        return (VfsUtil.findRelativeFile(ProjectUtil.guessProjectDir(project), "vendor", "typo3") != null)
            || !FilenameIndex.getVirtualFilesByName("ext_emconf.php", GlobalSearchScope.allScope(project)).isEmpty();
    }
}
