package com.swissas.util;

import java.awt.BorderLayout;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JPanel;

import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.swissas.beans.User;

/**
 * A helper class for the display User Information logic.
 *
 * @author Tavan Alain
 */

public class ShowLetterCodeInformationHelper {
	
	private ShowLetterCodeInformationHelper() throws IllegalAccessException {
		throw new IllegalAccessException("Helper class");
	}
	
	public static void displayInformation(String authorString, String errorText){
		String userInfos = null;
		String errorMessage = errorText;
		User user = null;
		JLabel lbl = new JLabel();
		if(authorString != null){
			authorString = authorString.toUpperCase();
			Map<String, User> userMap = SwissAsStorage.getInstance().getUserMap();
			if(userMap.containsKey(authorString)){
				user = userMap.get(authorString);
				userInfos = user.getInfos();
			}else{
				errorMessage = "<html><b>Could not find \"" + authorString + "\" in the internal phone book</b><br>Is this person still working at Swiss-as ?";
			}
		}
		JPanel pane = new JPanel(new BorderLayout());
		if(userInfos == null){
			lbl.setText(errorMessage);
			pane.add(lbl);
		}else {
			lbl.setText(userInfos);
			pane.add(lbl, BorderLayout.EAST);
			JLabel image = new JLabel(user.getPicture());
			pane.add(image, BorderLayout.WEST);
		}
		
		ComponentPopupBuilder componentPopupBuilder = JBPopupFactory.getInstance().createComponentPopupBuilder(pane, null);
		JBPopup popup = componentPopupBuilder.createPopup();
		popup.showInFocusCenter();
	}
	
}