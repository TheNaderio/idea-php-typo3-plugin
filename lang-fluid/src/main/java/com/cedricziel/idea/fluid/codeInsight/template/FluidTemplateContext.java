package com.cedricziel.idea.fluid.codeInsight.template;

import com.cedricziel.idea.fluid.lang.FluidLanguage;
import com.intellij.codeInsight.template.TemplateActionContext;
import com.intellij.codeInsight.template.TemplateContextType;
import org.jetbrains.annotations.NotNull;

public class FluidTemplateContext extends TemplateContextType {
    protected FluidTemplateContext() {
        // The context id lives in plugin.xml (contextId="FLUID") so the bundled live
        // templates keep resolving; the constructor only takes the presentable name now.
        super("Fluid Template");
    }

    @Override
    public boolean isInContext(@NotNull TemplateActionContext templateActionContext) {

        return templateActionContext.getFile().getLanguage() == FluidLanguage.INSTANCE;
    }
}
