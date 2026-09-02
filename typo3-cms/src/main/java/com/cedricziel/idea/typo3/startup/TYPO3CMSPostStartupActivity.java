package com.cedricziel.idea.typo3.startup;

import com.cedricziel.idea.typo3.IdeHelper;
import com.cedricziel.idea.typo3.TYPO3CMSProjectSettings;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TYPO3CMSPostStartupActivity implements StartupActivity {
    @Override
    public void runActivity(@NotNull Project project) {
        ApplicationManager.getApplication().runReadAction(() -> {
            doRunActivity(project);
        });
    }

    /*
     * This used to compare the running plugin version against the last one seen and rebuild every
     * index whenever it changed. Reading one's own plugin version needs a plugin descriptor, and
     * every route to one is @ApiStatus.Internal as of 2026.2.
     *
     * The platform already covers the case: bumping an index's getVersion() discards and rebuilds
     * exactly that index. So changing an indexer means bumping its getVersion() - blanket rebuilds
     * on every plugin update are neither necessary nor cheap.
     */
    protected void doRunActivity(@NotNull Project project) {
        this.checkProject(project);
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
        return (VfsUtil.findRelativeFile(project.getBaseDir(), "vendor", "typo3") != null)
            || FilenameIndex.getVirtualFilesByName("ext_emconf.php", GlobalSearchScope.allScope(project)).size() > 0;
    }
}
