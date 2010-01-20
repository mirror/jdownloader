package jd.plugins.optional.awesomebar.awesome.gui;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JTextField;

import jd.plugins.optional.awesomebar.awesome.Awesome;
import jd.plugins.optional.awesomebar.awesome.AwesomeAction;

public class AwesomeTextField extends JTextField implements FocusListener, KeyListener {
    private static final long serialVersionUID = -8640342205909716910L;

    private final AwesomeToolbarPanel toolbarPanel;
    private final Awesome awesome;

    AwesomeTextField(AwesomeToolbarPanel toolbarPanel) {
        super();
        this.toolbarPanel = toolbarPanel;
        this.awesome = toolbarPanel.getAwesomebar().getAwesome();
        this.addFocusListener(this);
        // this.addPropertyChangeListener(listener)
        this.addKeyListener(this);

    }

    public AwesomeToolbarPanel getAwesomePanel() {
        return toolbarPanel;
    }

    public void focusGained(FocusEvent arg0) {
        checkText();
    }

    public void focusLost(FocusEvent arg0) {
        checkText(false);
    }

    public void keyPressed(KeyEvent e) {
        // TODO: Implement DocumentListener for changed content, use keyPressed
        // only for actions
        if (e.getKeyChar() != KeyEvent.CHAR_UNDEFINED) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                // Perform Action
                new AwesomeAction(awesome, toolbarPanel.getAwesomebar().getProposalPanel().getProposalList().getSelectedProposal());
                setText("");
                checkText(false);
            } else if (e.getKeyCode() != KeyEvent.VK_BACK_SPACE) {
                checkText(true);
                awesome.requestProposal(this.getText().substring(0, this.getCaretPosition()) + e.getKeyChar() + this.getText().substring(this.getCaretPosition()));
            } else {

                if (this.getCaretPosition() > 0) {
                    awesome.requestProposal(this.getText().substring(0, this.getCaretPosition() - 1) + this.getText().substring(this.getCaretPosition()));
                    if (this.getText().length() <= 1) {
                        checkText(false);
                    }
                }

            }
        } else {
            switch (e.getKeyCode()) {
            case KeyEvent.VK_DOWN:
                // Change Selection, go down!
                try {
                    this.toolbarPanel.getAwesomebar().getProposalPanel().getProposalList().getModel().countLock.readLock().lock();
                    if (this.toolbarPanel.getAwesomebar().getProposalPanel().getProposalList().getSelectedIndex() != (this.toolbarPanel.getAwesomebar().getProposalPanel().getProposalList().getModel().getSize() - 1)) {
                        this.toolbarPanel.getAwesomebar().getProposalPanel().getProposalList().setSelectedIndices(new int[] { this.toolbarPanel.getAwesomebar().getProposalPanel().getProposalList().getSelectedIndex() + 1 });
                        // TODO: Necessary?
                        this.toolbarPanel.getAwesomebar().getProposalPanel().getDetailPanel().updateDetailPanel();
                    }
                } finally {
                    this.toolbarPanel.getAwesomebar().getProposalPanel().getProposalList().getModel().countLock.readLock().unlock();
                }
                break;
            case KeyEvent.VK_UP:
                // Change Selection, go up!
                if (this.toolbarPanel.getAwesomebar().getProposalPanel().getProposalList().getSelectedIndex() > 0) {
                    this.toolbarPanel.getAwesomebar().getProposalPanel().getProposalList().setSelectedIndices(new int[] { this.toolbarPanel.getAwesomebar().getProposalPanel().getProposalList().getSelectedIndex() - 1 });
                    // TODO: Necessary?
                    this.toolbarPanel.getAwesomebar().getProposalPanel().getDetailPanel().updateDetailPanel();
                }
                break;
            }

        }
    }

    private void checkText() {
        checkText(this.getText());
    }

    private void checkText(String checktext) {
        if (checktext.isEmpty()) {
            checkText(false);
        } else {
            checkText(true);
        }
    }

    private void checkText(boolean show) {
        if (show) {
            toolbarPanel.getAwesomebar().getProposalPanel().setVisible(true);
        } else {
            toolbarPanel.getAwesomebar().getProposalPanel().setVisible(false);
        }
    }

    public void keyReleased(KeyEvent e) {
    }

    public void keyTyped(KeyEvent e) {
    }

}