package jd.wizardry7.controller;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import jd.wizardry7.MVCWizard;



public class WizardController {
	
    // --- Action implementations ---------------------------------
	
	public static final Action backwardAction = new BackwardAction();
	public static final Action forwardAction = new ForwardAction();
	public static final Action cancelAction = new CancelAction();
	public static final Action helpAction = new HelpAction();
	public static final Action finishedAction = new FinishAction();

}

class BackwardAction extends AbstractAction {
	public void actionPerformed(ActionEvent e) {
		MVCWizard.getInstance().goBackward();
	}
}

class ForwardAction extends AbstractAction {
	public void actionPerformed(ActionEvent e) {
		MVCWizard.getInstance().goForward();
	}
	
}

class CancelAction extends AbstractAction {
	public void actionPerformed(ActionEvent e) {
		MVCWizard.getInstance().doCancel();
	}
}

class HelpAction extends AbstractAction {
	public void actionPerformed(ActionEvent e) {
		MVCWizard.getInstance().doHelp();
	}
}

class FinishAction extends AbstractAction {
	public void actionPerformed(ActionEvent e) {
		MVCWizard.getInstance().doFinish();
	}
}