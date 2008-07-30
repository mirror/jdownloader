package jd.wizardry7;

import java.awt.BorderLayout;
import javax.swing.JOptionPane;

import jd.wizardry7.view.DefaultWizardPage;
import jd.wizardry7.view._WizardDialog;

public class MVCWizard {

    private static final MVCWizard INSTANCE = new MVCWizard();

    private static int maxHeight = 0;
    private static int maxWidth = 0;

    public static final int SETTINGS_WIZARD = 0;

    private static void calculateMax_WizardPage_Dimension(DefaultWizardPage[] wizardPages) {
        for (DefaultWizardPage element : wizardPages) {
            int width = element.getPreferredSize().width;
            int height = element.getPreferredSize().height;
            if (maxWidth < width) {
                maxWidth = width;
            }
            if (maxHeight < height) {
                maxHeight = height;
            }
        }

        // Kleiner Hack - irgendwie fehlen 20 px. Wahrscheinlich vom JFrame
        // Title.
        maxHeight += 25;
        // Kleiner Hack um das wizard Seitenverhaeltnis festzulegen
        int aspectWidth = maxHeight * 3 / 4;
        maxWidth = maxWidth < aspectWidth ? aspectWidth : maxWidth;

        System.out.println("maxWidth : maxHeight : " + maxWidth + " : " + maxHeight);
    }

    public static MVCWizard getInstance() {
        return INSTANCE;
    }

    private int currentWizardPageIndex;

    private DefaultWizardPage[] currentWizardPages;
    private int previousWizardPage = -1;
    private _WizardDialog wizardFrame;

    private MVCWizard() {
    }

    public void doCancel() {
        int answer = JOptionPane.showConfirmDialog(wizardFrame, "Do you want to cancel?", "Cancel Wizard", JOptionPane.YES_NO_OPTION);
        if (answer == JOptionPane.OK_OPTION) {

            for (DefaultWizardPage element : currentWizardPages) {
                element.cancelValidation();
            }

            wizardFrame.setVisible(false);
            wizardFrame.dispose();
        }
    }

    public void doFinish() {
        wizardFrame.setVisible(false);
        wizardFrame.dispose();
        currentWizardPages[currentWizardPageIndex].exitWizardPage();
    }

    public void doHelp() {
    }

    public _WizardDialog getWizardFrame() {
        return wizardFrame;
    }

    public _WizardDialog getWizardFrame(boolean startWizardFromTheBeginning, int wizardType) {
        wizardFrame = new _WizardDialog();
        wizardFrame.setLayout(new BorderLayout());

        if (wizardType == 0) {
            currentWizardPages = WizardPages.getWizardPages();
        }

        // Wenn der Wizard schon mal sichtbar war muss er wieder auf Seite 1
        // gesetzt werden
        if (startWizardFromTheBeginning == true) {
            resetWizardPages();
        }

        showCurrentWizardPage();

        // Determines maxWidth/maxHeight
        MVCWizard.calculateMax_WizardPage_Dimension(currentWizardPages);
        if (maxWidth > 400) {
            maxWidth = 500;
        }
        // wizardFrame.setPreferredSize(new
        // Dimension(Math.max(wizardFrame.getPreferredSize().width, maxWidth),
        // Math.max(wizardFrame.getPreferredSize().height, maxHeight)));
        wizardFrame.pack();
        wizardFrame.setLocationRelativeTo(null);

        return wizardFrame;
    }

    public void goBackward() {
        if (currentWizardPages[currentWizardPageIndex].backwardValidation().equals("")) {
            previousWizardPage = currentWizardPageIndex;
            currentWizardPageIndex--;
            showCurrentWizardPage();
        }
    }

    public void goForward() {
        String reply = currentWizardPages[currentWizardPageIndex].forwardValidation();
        if (reply.equals("")) {
            currentWizardPages[currentWizardPageIndex].exitWizardPage();
            previousWizardPage = currentWizardPageIndex;
            currentWizardPageIndex++;
            currentWizardPages[currentWizardPageIndex].enterWizardPageAfterForward();
            showCurrentWizardPage();
        } else {
            showWarning(reply);
        }
    }

    public void memoryTestExit() {
        wizardFrame.setVisible(false);
        wizardFrame.dispose();
        currentWizardPages = null;
    }

    /**
     * Setzt den Wizard wieder zurueck, als waere er nie eingeblendet worden.
     */
    private void resetWizardPages() {
        previousWizardPage = currentWizardPageIndex;
        currentWizardPageIndex = 0;
        showCurrentWizardPage();
    }

    private void showCurrentWizardPage() {
        if (previousWizardPage != -1) {
            wizardFrame.remove(currentWizardPages[previousWizardPage]);
        }
        wizardFrame.add(currentWizardPages[currentWizardPageIndex], BorderLayout.CENTER);
        wizardFrame.validate();
        wizardFrame.repaint();
    }

    private void showWarning(String reply) {
        JOptionPane.showMessageDialog(wizardFrame, reply, "Warning Message", JOptionPane.ERROR_MESSAGE);
    }

}
