package com.swissas.inspection;

import java.util.*;
import java.util.regex.Pattern;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.ex.LineStatusTracker;
import com.intellij.openapi.vcs.ex.LineStatusTrackerI;
import com.intellij.openapi.vcs.ex.Range;
import com.intellij.openapi.vcs.impl.LineStatusTrackerManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.swissas.quickfix.MarkAsNoSQLQuickFix;
import com.swissas.quickfix.TranslateMakAsIgnoreQuickFix;
import com.swissas.quickfix.TranslateQuickFix;
import com.swissas.quickfix.TranslateTooltipQuickFix;
import com.swissas.util.SwissAsStorage;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The inspection for missing translation
 *
 * @author Tavan Alain
 */

class MissingTranslationInspection extends LocalInspectionTool {
	
	@NonNls
	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("texts");
	
	@Override
	public boolean isEnabledByDefault() {
		return true;
	}
	
	@Override
	@NotNull
	public String getDisplayName() {
		return RESOURCE_BUNDLE.getString("missing.translation");
	}
	
	@Override
	@NotNull
	public String getGroupDisplayName() {
		return RESOURCE_BUNDLE.getString("swiss.as");
	}
	
	@NotNull
	@Override
	public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
		LocalQuickFix ignoreFix = new TranslateMakAsIgnoreQuickFix();
		LocalQuickFix translateFix = new TranslateQuickFix(holder.getFile());
		LocalQuickFix translateTooltipFix = new TranslateTooltipQuickFix(holder.getFile());
		LocalQuickFix noSolFix = new MarkAsNoSQLQuickFix();
		LocalQuickFix[] fixes = SwissAsStorage.getInstance().isNewTranslation() ? new LocalQuickFix[]{ignoreFix, translateFix, translateTooltipFix} : new LocalQuickFix[]{ignoreFix};
		
		if (holder.getFile().getName().endsWith("Test.java")) {
			return super.buildVisitor(holder, isOnTheFly);
		}
		
		return new MyJavaElementVisitor(holder, fixes, noSolFix);
	}
	
	
	static class MyJavaElementVisitor extends JavaElementVisitor {
		
		private final ProblemsHolder holder;
		private final LocalQuickFix[] fixes;
		private final LocalQuickFix noSqlFix;
		private final Pattern filenamePattern;
		private final Pattern SQLPattern;
		private final Pattern isInMethods;
		private final boolean noSvn;
		private final List<Range> rangesToCheck;
		
		MyJavaElementVisitor(@NotNull ProblemsHolder holder, @NotNull LocalQuickFix[] fixes, @NotNull LocalQuickFix noSqlFix) {
			this.SQLPattern = Pattern.compile(".*(DELETE|INSERT|UPDATE|SELECT).*", Pattern.CASE_INSENSITIVE);
			this.filenamePattern = Pattern.compile("^[^.]+\\.\\w{3}$");
			this.isInMethods = Pattern.compile(".*(Exception|firePropertyChange|fireIndexedPropertyChange|assertEquals|MultiLang(Text|ToolTip)|getLogger\\(\\).*|WithHistory)$");
			this.noSqlFix = noSqlFix;
			this.holder = holder;
			this.fixes = fixes;
			VirtualFile virtualFile = holder.getFile().getVirtualFile();
			Project project = holder.getProject();
			LineStatusTracker lineStatusTracker = LineStatusTrackerManager.getInstance(project).getLineStatusTracker(virtualFile);
			this.rangesToCheck = Optional.ofNullable(lineStatusTracker).map(LineStatusTrackerI::getRanges).orElse(new ArrayList<>());
			this.rangesToCheck.removeIf(e -> e.getType() != Range.DELETED);
			this.noSvn = this.rangesToCheck.isEmpty();
		}
		
		@Override
		public void visitLiteralExpression(PsiLiteralExpression expression) {
			super.visitLiteralExpression(expression);
			int minSize = Integer.parseInt(SwissAsStorage.getInstance().getMinWarningSize()) + 2; //psiStringElements are withing double quotes
			int textOffset = expression.getTextOffset();
			int lineNumber = StringUtil.offsetToLineNumber(expression.getContainingFile().getText(), textOffset);
			boolean shouldCheckFile = this.noSvn || !SwissAsStorage.getInstance().isTranslationOnlyCheckChangedLine() || this.rangesToCheck.stream().anyMatch(r -> lineNumber >= r.getLine1() && lineNumber <= r.getLine2());
			if (shouldCheckFile){
				Object expressionValue = expression.getValue();
				if(expressionValue instanceof String && ((String) expressionValue).length() > minSize &&
					!this.filenamePattern.matcher((String) expressionValue).matches()) {
					if (this.SQLPattern.matcher((String) expressionValue).matches()) {
						checkHierarchyAndRegisterMissingNoSOLProblemIfNeeded(expression);
					}
					checkHierrarchyAndRegisterMissingTranslationProblemIfNeeded(expression);
				}
			}
		}
		
		private void checkHierarchyAndRegisterMissingNoSOLProblemIfNeeded(PsiLiteralExpression expression) {
			if(hasNoNoSqlAsNextSibling(expression)) {
				PsiElement parent = expression.getParent();
				if (hasNoNoSqlAsNextSibling(parent)) {
					if (parent instanceof PsiExpressionList) {
						PsiElement parentPrevSibling = getPrevNotEmptySpaces(parent);
						if (parentPrevSibling != null) {
							this.holder.registerProblem(expression, RESOURCE_BUNDLE.getString("missing.nosql"), ProblemHighlightType.GENERIC_ERROR_OR_WARNING, this.noSqlFix);
						}
					} else if (parent instanceof PsiPolyadicExpression) {
						PsiElement grandParent = parent.getParent();
						if (grandParent instanceof PsiAssignmentExpression || grandParent instanceof PsiLocalVariable) {
							this.holder.registerProblem(parent, RESOURCE_BUNDLE.getString("missing.nosql"), ProblemHighlightType.GENERIC_ERROR_OR_WARNING, this.noSqlFix);
						} else {
							PsiElement beforeGrandParent = getPrevNotEmptySpaces(parent.getParent());
							if (beforeGrandParent != null) {
								this.holder.registerProblem(parent, RESOURCE_BUNDLE.getString("missing.nosql"), ProblemHighlightType.GENERIC_ERROR_OR_WARNING, this.noSqlFix);
							}
						}
					} else { //don't care about the parent special cases, the issue is on the expression itself
						this.holder.registerProblem(expression, RESOURCE_BUNDLE.getString("missing.nosql"), ProblemHighlightType.GENERIC_ERROR_OR_WARNING, this.noSqlFix);
					}
				}
			}
		}
		
		private void checkHierrarchyAndRegisterMissingTranslationProblemIfNeeded(@NotNull PsiLiteralExpression expression) {
			PsiElement parent = expression.getParent();
			PsiElement grandParent = parent == null ? null : parent.getParent();
			if(grandParent instanceof PsiAnnotationParameterList){
				return;
			}
			if(hasNoNoExtAsNextSibling(expression) && hasNoNoExtAsNextSibling(parent)) {
				if (parent instanceof PsiExpressionList) {
					PsiElement parentPrevSibling = getPrevNotEmptySpaces(parent);
					if (parentPrevSibling != null && !this.isInMethods.matcher(parentPrevSibling.getText()).matches()) {
						this.holder.registerProblem(expression, RESOURCE_BUNDLE.getString("missing.translation"), ProblemHighlightType.GENERIC_ERROR_OR_WARNING, this.fixes);
					}
				} else if (parent instanceof PsiPolyadicExpression) {
					if (grandParent instanceof PsiAssignmentExpression || grandParent instanceof PsiLocalVariable) {
						this.holder.registerProblem(parent, RESOURCE_BUNDLE.getString("missing.translation"), ProblemHighlightType.GENERIC_ERROR_OR_WARNING, this.fixes);
					} else {
						PsiElement beforeGrandParent = getPrevNotEmptySpaces(parent.getParent());
						if (beforeGrandParent != null && !this.isInMethods.matcher(beforeGrandParent.getText()).matches()) {
							this.holder.registerProblem(parent, RESOURCE_BUNDLE.getString("missing.translation"), ProblemHighlightType.GENERIC_ERROR_OR_WARNING, this.fixes);
						}
					}
				} else { //don't care about the parent special cases, the issue is on the expression itself
					this.holder.registerProblem(expression, RESOURCE_BUNDLE.getString("missing.translation"), ProblemHighlightType.GENERIC_ERROR_OR_WARNING, this.fixes);
				}
			}
		}
		
		private PsiElement getPrevNotEmptySpaces(PsiElement element) {
			PsiElement psiElement = element.getPrevSibling();
			if (psiElement instanceof PsiWhiteSpace) {
				psiElement = psiElement.getPrevSibling();
			}
			return psiElement;
		}
		
		@NotNull
		private Boolean hasNoNoExtAsNextSibling(PsiElement expression) {
			return hasNoSiblingContainingText(expression, "NO_EXT");
		}
		
		@NotNull
		private Boolean hasNoNoSqlAsNextSibling(PsiElement expression) {
			return hasNoSiblingContainingText(expression, "NOSQL");
		}
		
		private static boolean hasNoSiblingContainingText(@Nullable PsiElement sibling, @NotNull String text) {
			boolean result = true;
			if (sibling != null) {
				for (PsiElement nextSibling = sibling.getNextSibling(); nextSibling != null; nextSibling = nextSibling.getNextSibling()) {
					if (nextSibling instanceof PsiComment && nextSibling.getText().contains(text)) {
						result = false;
						break;
					}
				}
			}
			return result;
		}
		
	}
	
}
