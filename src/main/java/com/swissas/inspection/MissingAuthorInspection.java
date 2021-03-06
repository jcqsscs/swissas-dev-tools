package com.swissas.inspection;


import java.util.ResourceBundle;
import java.util.stream.Stream;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiJavaFile;
import com.swissas.quickfix.MissingAuthorQuickFix;
import com.swissas.util.ProjectUtil;
import org.jetbrains.annotations.NotNull;


/**
 * An inspection for missing author in the java files.
 * It will automatically add a default javadoc author template if none is present.
 * @author Tavan Alain
 */

public class MissingAuthorInspection extends LocalInspectionTool{

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

    @Override
    @NotNull
    public String getDisplayName() {
        return ResourceBundle.getBundle("texts").getString("missing.class.author");
    }

    @Override
    @NotNull
    public String getGroupDisplayName() {
        return ResourceBundle.getBundle("texts").getString("swiss.as");
    }

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {

        return new JavaElementVisitor() {

            @Override
            public void visitJavaFile(@NotNull PsiJavaFile file) {
                super.visitJavaFile(file);
                if (ProjectUtil.getInstance().isAmosProject(file.getProject())) {
                    PsiClass[] classes = file.getClasses();
                    if (classes.length > 0) {
                        PsiClass firstClass = classes[0];
                        LocalQuickFix missingAuthorQuickFix = new MissingAuthorQuickFix(file);

                        if (firstClass.getDocComment() == null) {
                            holder.registerProblem(
                                    holder.getManager().createProblemDescriptor(firstClass,
                                                                                ResourceBundle.getBundle("texts").getString(
                                                                                        "class.has.no.javadoc"),
                                                                                missingAuthorQuickFix,
                                                                                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                                                                                isOnTheFly));
                        } else if (firstClass.getDocComment().getTags().length == 0) {
                            holder.registerProblem(holder.getManager().createProblemDescriptor(
                                    firstClass.getDocComment(),
                                    ResourceBundle.getBundle("texts").getString("class.has.no.author"),
                                    missingAuthorQuickFix,
                                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING, isOnTheFly));
                        } else if (Stream.of(firstClass.getDocComment().getTags()).noneMatch(
                                tag -> "author".equals(tag.getName()))) {
                            holder.registerProblem(holder.getManager().createProblemDescriptor(
                                    firstClass.getDocComment().getTags()[0],
                                    ResourceBundle.getBundle("texts").getString("class.has.no.author"),
                                    missingAuthorQuickFix,
                                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING, isOnTheFly));
                        }
                    }
                }
            }
        };
    }
            
}