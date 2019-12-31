package com.swissas.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class StringUtils {
	private static final Pattern LETTER_CODE_PATTERN = Pattern.compile("^[A-Z]{3,4}$");
	private static final Pattern LETTER_CODE_PATTERN_WITH_NAME = Pattern.compile("^[A-Z]{3,4} \\(.+\\)$");
	private static final Pattern GETTER_PREFIX_PATTERN = Pattern.compile("^(get|is|has)(.*)$");
	private static StringUtils instance;
	
	private StringUtils() {
		//do nothing
	}
	
	public static StringUtils getInstance() {
		if(instance == null) {
			instance = new StringUtils();
		}
		return instance;
	}
	
	public boolean isLetterCode(String text) {
		return text != null && (text.isEmpty() || LETTER_CODE_PATTERN.matcher(text).find());
	}
	
	public boolean isLetterCodeWithName(String text) {
		return text != null && (text.isEmpty() || LETTER_CODE_PATTERN_WITH_NAME.matcher(text).find());
	}
	
	public boolean isPositiveNumber(String text){
		int i;
		try {
			i = Integer.parseInt(text);
		} catch (NumberFormatException e) {
			i = -1;
		}
		
		return i >= 0;
	}
	
	public boolean isValidLetterCode(String potentialLetterCode) {
		return SwissAsStorage.getInstance().getUserMap().containsKey(potentialLetterCode);
	}
	
	public String removeJavaEnding(String name) {
		return name.substring(0,name.indexOf(".java"));
	}
	
	public String removeGetterPrefix(String name) {
		String result = null;
		Matcher matcher = GETTER_PREFIX_PATTERN.matcher(name);
		if(matcher.find()) {
			String match = matcher.group(2);
			result = match.substring(0, 1).toLowerCase() + match.substring(1); 
		}
		return result;
	}
	
	
}
