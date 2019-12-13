package com.swissas.util;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcs.branch.BranchData;
import com.intellij.vcs.branch.BranchStateProvider;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * The Project utility class
 *
 * @author Tavan Alain
 */

public class ProjectUtil {
	private static ProjectUtil     instance;
	private static final   Pattern AMOS_SHARED_DIRECTORY_PATTERN = Pattern
			.compile("\\/([^\\/]+)\\/[^\\/]*amos_shared\\/", Pattern.CASE_INSENSITIVE);
	private static final   Pattern STABLE_VERSION_PATTERN        = Pattern
			.compile("^v?(\\d{2})[_\\-. ]?(\\d{1,2})$", Pattern.CASE_INSENSITIVE);
	
	protected Module  shared;
	protected String  currentProjectBasePath    = null;
	protected boolean isAmosProject             = false;
	protected String  projectDefaultBranch      = null;
	protected boolean shouldSearchDefaultBranch = false;
	
	private ProjectUtil() {
		
	}
	
	public static ProjectUtil getInstance() {
		if (instance == null) {
			instance = new ProjectUtil();
		}
		return instance;
	}
	
	public String getBranchOfFile(Project project, VirtualFile file) {
		String branchName = getProjectDefaultBranch();
		if (file != null) {
			FilePath filePath = VcsUtil.getFilePath(file.getPath());
			AbstractVcs vcsFor = VcsUtil.getVcsFor(project, filePath);
			if (vcsFor != null) {
				branchName = BranchStateProvider.EP_NAME.getExtensionList(project)
				                                        .stream().filter(Objects::nonNull)
				                                        .filter(p -> p.getClass().getName()
				                                                      .contains(vcsFor.getName()))
				                                        .map(p -> p.getCurrentBranch(filePath))
				                                        .filter(Objects::nonNull)
				                                        .map(BranchData::getBranchName)
				                                        .filter(Objects::nonNull)
				                                        .map(String::toLowerCase).findFirst()
				                                        .orElse(null);
			}
		}
		return convertToCorrectBranch(branchName);
	}
	
	String convertToCorrectBranch(String branchName) {
		String result = branchName;
		if ("trunk".equals(branchName)) {
			result = "preview";
		} else if (result != null && !"preview".equalsIgnoreCase(result)) {
			Matcher matcher = STABLE_VERSION_PATTERN.matcher(result);
			if (matcher.find() && matcher.groupCount() == 2) {
				int majorVersion = Integer.parseInt(matcher.group(1));
				int minorVersion = Integer.parseInt(matcher.group(2));
				result = getBranchOfMajorAndMinorVersion(majorVersion, minorVersion);
			}
		}
		return result;
	}
	
	String getBranchOfMajorAndMinorVersion(int majorVersion, int minorVersion) {
		return majorVersion >= 19 ? majorVersion + "." + minorVersion
		                          : "V" + majorVersion + "-" + minorVersion;
	}
	
	public boolean isAmosProject(@NotNull Project project) {
		if (project.isDisposed()) {
			this.isAmosProject = false;
		} else {
			String basePath = project.getBasePath();
			if (basePath != null && !basePath.equals(this.currentProjectBasePath)) {
				this.currentProjectBasePath = basePath;
				Optional<Module> amosShared = Stream
						.of(ModuleManager.getInstance(project).getModules())
						.filter(e -> e.getName().contains("amos_shared")).findFirst();
				if (amosShared.isPresent()) {
					this.shared = amosShared.get();
					this.projectDefaultBranch = null;
					this.shouldSearchDefaultBranch = true;
					this.isAmosProject = true;
				} else {
					this.isAmosProject = false;
				}
			}
		}
		return this.isAmosProject;
	}
	
	public boolean isPreviewProject() {
		return "preview".equalsIgnoreCase(convertToCorrectBranch(this.projectDefaultBranch));
	}
	
	String getProjectDefaultBranch() {
		if (this.shouldSearchDefaultBranch) {
			this.shouldSearchDefaultBranch = false;
			Matcher matcher = AMOS_SHARED_DIRECTORY_PATTERN
					.matcher(this.shared.getModuleFilePath());
			if (matcher.find()) {
				this.projectDefaultBranch = matcher.group(1);
			}
		}
		return this.projectDefaultBranch;
	}
	
	public Module getShared() {
		return this.shared;
	}
	
}
