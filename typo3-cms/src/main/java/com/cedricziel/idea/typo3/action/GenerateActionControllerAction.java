package com.cedricziel.idea.typo3.action;

import com.cedricziel.idea.typo3.TYPO3CMSIcons;
import com.cedricziel.idea.typo3.util.ExtensionFileGenerationUtil;
import com.cedricziel.idea.typo3.util.ExtensionUtility;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class GenerateActionControllerAction extends NewExtensionFileAction {

    public GenerateActionControllerAction() {

        super("Extbase Controller", "Generate a Extbase ActionController", TYPO3CMSIcons.TYPO3_ICON);
    }

    @Override
    protected void write(@NotNull Project project, @NotNull PsiDirectory extensionRootDirectory, @NotNull String className) {
        if (!className.endsWith("Controller")) {
            className += "Controller";
        }

        final String finalClassName = className;
        PsiElement extensionFile = WriteCommandAction.writeCommandAction(project).compute(() -> {
            String calculatedNamespace = ExtensionUtility.findDefaultNamespace(extensionRootDirectory);
            if (calculatedNamespace == null) {
                return null;
            }

            calculatedNamespace += "Controller";

            Map<String, String> context = new HashMap<>();
            context.put("namespace", calculatedNamespace);
            context.put("className", finalClassName);

            try {
                return ExtensionFileGenerationUtil.fromTemplate(
                        "extension_file/ExtbaseActionController.php",
                        "Classes/Controller",
                        finalClassName + ".php",
                        extensionRootDirectory,
                        context,
                        project
                );
            } catch (IncorrectOperationException e) {
                // file already exists
                return null;
            }
        });

        if (extensionFile != null) {
            new OpenFileDescriptor(project, extensionFile.getContainingFile().getVirtualFile(), 0).navigate(true);
        } else {
            Messages.showErrorDialog("Cannot create extension file", "Error");
        }
    }
}
