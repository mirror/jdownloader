package jd.wizardry7.controller;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import jd.wizardry7.MVCWizard;

class BackwardAction extends AbstractAction {
    private static final long serialVersionUID = 6519707833041592186L;

    public void actionPerformed(ActionEvent e) {
        MVCWizard.getInstance().goBackward();
    }
}

class CancelAction extends AbstractAction {
    private static final long serialVersionUID = -2246489208969887519L;

    public void actionPerformed(ActionEvent e) {
        MVCWizard.getInstance().doCancel();
    }
}

class FinishAction extends AbstractAction {
    private static final long serialVersionUID = -2381843754569069058L;

    public void actionPerformed(ActionEvent e) {
        MVCWizard.getInstance().doFinish();
    }
}

class ForwardAction extends AbstractAction {
    private static final long serialVersionUID = 3448356256615152260L;

    public void actionPerformed(ActionEvent e) {
        MVCWizard.getInstance().goForward();
    }

}

class HelpAction extends AbstractAction {
    private static final long serialVersionUID = 148041943993311465L;

    public void actionPerformed(ActionEvent e) {
        MVCWizard.getInstance().doHelp();
    }
}

public class WizardController {

    // --- Action implementations ---------------------------------

    public static final Action backwardAction = new BackwardAction();
    public static final Action cancelAction = new CancelAction();
    public static final Action finishedAction = new FinishAction();
    public static final Action forwardAction = new ForwardAction();
    public static final Action helpAction = new HelpAction();

}