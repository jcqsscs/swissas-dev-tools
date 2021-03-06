package com.swissas.quickfix;

import java.util.ResourceBundle;

import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

/**
 * Quickfix for the tooltip creation
 * 
 * @author Tavan Alain
 */
public class TranslateTooltipQuickFix extends TranslateQuickFix {

    public TranslateTooltipQuickFix(PsiFile file){
        super(file);
        this.ending = "_TT";
        this.className = "MultiLangToolTip";
    }

    @Nls(capitalization = Nls.Capitalization.Sentence)
    @NotNull
    @Override
    public String getName() {
        return ResourceBundle.getBundle("texts").getString("translate.as.tooltip");
    }
}
