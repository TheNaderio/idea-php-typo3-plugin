package com.cedricziel.idea.fluid.lang.psi.codeStyle;

import com.cedricziel.idea.fluid.lang.psi.FluidFile;
import com.intellij.lang.Language;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.codeStyle.FileIndentOptionsProvider;
import com.intellij.psi.templateLanguages.TemplateLanguageFileViewProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FluidFileIndentOptionsProvider extends FileIndentOptionsProvider {
    @Nullable
    @Override
    public CommonCodeStyleSettings.IndentOptions getIndentOptions(@NotNull Project project,
                                                                  @NotNull CodeStyleSettings settings,
                                                                  @NotNull VirtualFile virtualFile) {
        PsiFile file = PsiManager.getInstance(project).findFile(virtualFile);
        if (file instanceof FluidFile) {
            if (file.getViewProvider() instanceof TemplateLanguageFileViewProvider) {
                Language language = ((TemplateLanguageFileViewProvider) file.getViewProvider()).getTemplateDataLanguage();

                return settings.getCommonSettings(language).getIndentOptions();
            }
        }

        return null;
    }
}
