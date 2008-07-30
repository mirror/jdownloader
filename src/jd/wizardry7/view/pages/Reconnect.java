package jd.wizardry7.view.pages;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.config.ConfigPanelReconnect;
import jd.utils.JDUtilities;
import jd.wizardry7.view.DefaultWizardPage;

public class Reconnect extends DefaultWizardPage {

    private static final Reconnect INSTANCE = new Reconnect();

    public static Reconnect getInstance() {
        return INSTANCE;
    }

    JCheckBox checkbox;

    private Reconnect() {
        super();
        setPreferredSize(new Dimension(700, 800));
    }

    @Override
    protected Component createBody() {
        initComponents();

        int n = 10;
        JPanel panel = new JPanel(new BorderLayout(n, n));
        panel.setBorder(new EmptyBorder(n, n, n, n));
        panel.add(new ConfigPanelReconnect(JDUtilities.getConfiguration(), SimpleGUI.CURRENTGUI));
        panel.add(checkbox, BorderLayout.SOUTH);

        return panel;
    }

    @Override
    public void enterWizardPageAfterForward() {
        checkbox.setSelected(false);
    }

    // Validation ##########################################################

    @Override
    public String forwardValidation() {
        if (checkbox.isSelected()) {
            checkbox.setSelected(false);
            return "";
        } else {
            return "To achive the full functionality of JDownloader we recommend to set up a reconnect method.";
        }
    }

    @Override
    protected void initComponents() {
        checkbox = new JCheckBox();
        checkbox.setText(" I want to continue without setting up a Reconnect method.");
    }
}
