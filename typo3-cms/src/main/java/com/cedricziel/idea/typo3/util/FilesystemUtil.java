package com.cedricziel.idea.typo3.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;

import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FilesystemUtil {
    /**
     * Replacement for the deprecated FilenameIndex.getFilesByName(Project, String, scope): the
     * supported API hands back virtual files, while callers here work on PsiFiles.
     */
    public static PsiFile @NotNull [] findFilesByName(@NotNull Project project, @NotNull String name) {
        PsiManager psiManager = PsiManager.getInstance(project);

        return FilenameIndex.getVirtualFilesByName(name, GlobalSearchScope.allScope(project)).stream()
            .map(psiManager::findFile)
            .filter(Objects::nonNull)
            .toArray(PsiFile[]::new);
    }

    @Nullable
    public static PsiDirectory findParentExtensionDirectory(@NotNull PsiDirectory directory) {

        VirtualFile extensionRootFolder = findExtensionRootFolder(directory.getVirtualFile());
        if (extensionRootFolder == null) {
            return null;
        }

        return PsiManager.getInstance(directory.getProject()).findDirectory(extensionRootFolder);
    }

    @Nullable
    public static VirtualFile findExtensionRootFolder(@NotNull VirtualFile file) {
        if (file.isDirectory()) {
            VirtualFile child = file.findChild("ext_emconf.php");

            if (child != null) {
                return file;
            }
        }

        // dragons ahead.
        if (file.getParent() != null) {
            return findExtensionRootFolder(file.getParent());
        }

        return null;
    }
}
