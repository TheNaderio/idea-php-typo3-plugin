package com.cedricziel.idea.typo3.userFunc;

import com.intellij.codeInsight.completion.InsertionContext;
import com.jetbrains.php.completion.PhpLookupElement;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import org.jetbrains.annotations.NotNull;

public class UserFuncLookupElement extends PhpLookupElement {
    public UserFuncLookupElement(@NotNull PhpNamedElement namedElement) {
        super(namedElement);
    }

    @Override
    public void handleInsert(@NotNull InsertionContext context) {
        UserFuncInsertHandler.getInstance().handleInsert(context, this);
    }
}
