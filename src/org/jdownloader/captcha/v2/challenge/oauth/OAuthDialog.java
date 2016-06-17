package org.jdownloader.captcha.v2.challenge.oauth;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.appwork.swing.action.BasicAction;
import org.appwork.uio.UIOManager;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.jdownloader.DomainInfo;
import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.ChallengeSolver;
import org.jdownloader.captcha.v2.solverjob.ChallengeSolverJobListener;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;

import jd.gui.swing.dialog.DialogType;
import net.miginfocom.swing.MigLayout;

public class OAuthDialog extends AbstractDialog<Boolean> implements ActionListener, ChallengeSolverJobListener {

    // private BufferedImage[] kcImages;
    // private int kcSampleImg;

    private JPanel         p;

    private OAuthChallenge challenge;

    public OAuthDialog(int flag, DialogType type, DomainInfo domain, OAuthChallenge challenge) {
        super(flag | UIOManager.BUTTONS_HIDE_OK, _GUI.T.OAUTH_DIALOG_TITLE(domain.getTld()), new AbstractIcon(IconKey.ICON_BROWSE, 32), null, _GUI.T.lit_close());
        // challenge.getExplain()
        // super(flag | Dialog.STYLE_HIDE_ICON | UIOManager.LOGIC_COUNTDOWN, title, null, null, null);

        this.challenge = challenge;
        challenge.getJob().getEventSender().addListener(this, true);
        setLeftActions(new BasicAction() {
            {
                setName(_GUI.T.lit_open_browser());
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                openBrowser();
            }
        });

    }

    protected void openBrowser() {
        CrossSystem.openURL(challenge.getUrl());
    }

    protected int getPreferredHeight() {
        return -1;
    }

    @Override
    protected int getPreferredWidth() {
        return -1;
    }

    @Override
    protected boolean isResizable() {
        return false;
    }

    @Override
    protected Boolean createReturnValue() {

        return false;
    }

    @Override
    public JComponent layoutDialogContent() {
        dialog.addWindowListener(new WindowListener() {

            @Override
            public void windowOpened(WindowEvent e) {
                openBrowser();
            }

            @Override
            public void windowIconified(WindowEvent e) {
            }

            @Override
            public void windowDeiconified(WindowEvent e) {
            }

            @Override
            public void windowDeactivated(WindowEvent e) {
            }

            @Override
            public void windowClosing(WindowEvent e) {
            }

            @Override
            public void windowClosed(WindowEvent e) {
            }

            @Override
            public void windowActivated(WindowEvent e) {
            }
        });
        p = new JPanel(new MigLayout("ins 0,wrap 1", "[grow,fill]", "[][grow,fill]"));
        JLabel lbl;
        p.add(lbl = new JLabel("<html>" + challenge.getExplain().replace("\r\n", "<br>") + "</html>"));

        // if (challenge instanceof AccountLoginOAuthChallenge) {
        // ExtTextField ttx = new ExtTextField() {
        // private Border orgBorder;
        // private JLabel label;
        // private int labelWidth;
        // private int iconGap;
        // private Icon hosterIcon;
        //
        // {
        // orgBorder = getBorder();
        // setBorder(BorderFactory.createCompoundBorder(orgBorder, BorderFactory.createEmptyBorder(0, 28, 0, 0)));
        //
        // label = new JLabel() {
        // public boolean isShowing() {
        //
        // return true;
        // }
        //
        // public boolean isVisible() {
        // return true;
        // }
        // };
        //
        // label.setText("Code: ");
        // labelWidth = label.getPreferredSize().width;
        // hosterIcon = challenge.getDomainInfo().getIcon(24);
        // if (hosterIcon == null) {
        // hosterIcon = challenge.getDomainInfo().getFavIcon();
        // }
        // iconGap = hosterIcon.getIconWidth() + 5;
        // label.setSize(labelWidth, 24);
        // // label.setEnabled(false);
        // setBorder(BorderFactory.createCompoundBorder(orgBorder, BorderFactory.createEmptyBorder(0, labelWidth + 20 + iconGap, 0, 0)));
        // }
        //
        // protected void paintComponent(Graphics g) {
        //
        // super.paintComponent(g);
        // Graphics2D g2 = (Graphics2D) g;
        // Composite comp = g2.getComposite();
        //
        // g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.1f));
        //
        // g2.setColor(Color.BLACK);
        //
        // g2.fillRect(1, 1, labelWidth + 5 + iconGap + 8 - 1, getHeight() - 1);
        // g2.setColor(getBackground().darker());
        // g2.drawLine(labelWidth + 5 + iconGap + 8, 1, labelWidth + iconGap + 5 + 8, getHeight() - 1);
        //
        // g2.setComposite(comp);
        // hosterIcon.paintIcon(this, g2, 3, 3);
        //
        // g2.translate(iconGap + 1, 0);
        // label.getUI().paint(g2, label);
        //
        // // label.paintComponents(g2);
        // g2.translate(-iconGap - 1, 0);
        //
        // // g2.dispose();
        //
        // }
        // };
        //
        // ttx.setText(((AccountLoginOAuthChallenge) challenge).getUserCode());
        // ttx.setEditable(false);
        // // ttx.setLabelMode(true);
        // p.add(ttx, "pushx,growx");
        // }
        SwingUtils.setOpaque(lbl, false);

        // drawPanel.setPreferredSize(background.getPreferredSize());
        return p;
    }

    @Override
    public void onSolverJobReceivedNewResponse(AbstractResponse<?> response) {
        dispose();
    }

    @Override
    public void onSolverDone(ChallengeSolver<?> solver) {

    }

    @Override
    public void onSolverStarts(ChallengeSolver<?> parameter) {
    }

    @Override
    public void onSolverTimedOut(ChallengeSolver<?> parameter) {

    }

}