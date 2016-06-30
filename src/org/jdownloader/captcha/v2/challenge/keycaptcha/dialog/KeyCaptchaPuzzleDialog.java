package org.jdownloader.captcha.v2.challenge.keycaptcha.dialog;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;

import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.DomainInfo;
import org.jdownloader.captcha.v2.AbstractCaptchaDialog;
import org.jdownloader.captcha.v2.challenge.keycaptcha.KeyCaptchaImages;
import org.jdownloader.captcha.v2.challenge.keycaptcha.KeyCaptchaPuzzleChallenge;
import org.jdownloader.captcha.v2.challenge.keycaptcha.KeyCaptchaPuzzleResponseData;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.updatev2.gui.LAFOptions;

import jd.gui.swing.dialog.DialogType;
import net.miginfocom.swing.MigLayout;

public class KeyCaptchaPuzzleDialog extends AbstractCaptchaDialog<KeyCaptchaPuzzleResponseData> implements ActionListener {
    private JLayeredPane              drawPanel;

    // private BufferedImage[] kcImages;
    // private int kcSampleImg;

    private JPanel                    p;
    // private final Dimension dimensions;
    ArrayList<Integer>                mouseArray;

    private KeyCaptchaPuzzleChallenge challenge;

    private KeyCaptchaImages          imageData;

    public KeyCaptchaPuzzleDialog(KeyCaptchaPuzzleChallenge captchaChallenge, int flag, DialogType type, DomainInfo domain, KeyCaptchaPuzzleChallenge challenge) {
        super(captchaChallenge, flag, _GUI.T.KeyCaptchaDialog(domain.getTld()), type, domain, _GUI.T.KeyCaptchaDialog_explain(domain.getTld()));
        // super(flag | Dialog.STYLE_HIDE_ICON | UIOManager.LOGIC_COUNTDOWN, title, null, null, null);

        this.challenge = challenge;

        // dimensions = new Dimension(465, 250);
        imageData = challenge.getHelper().getPuzzleData().getImages();

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
    protected KeyCaptchaPuzzleResponseData createReturnValue() {
        if (Dialog.isOK(getReturnmask())) {
            return new KeyCaptchaPuzzleResponseData(getPosition(drawPanel), mouseArray);
        }
        return null;
    }

    private String getPosition(final JLayeredPane drawPanel) {
        int i = 0;
        String positions = "";
        final Component[] comp = drawPanel.getComponents();
        for (int c = comp.length - 1; c >= 0; c--) {
            if (comp[c].getMouseListeners().length == 0) {
                continue;
            }
            final Point p = comp[c].getLocation();
            positions += (i != 0 ? "." : "") + String.valueOf(p.x) + "." + String.valueOf(p.y);
            i++;
        }
        return positions;
    }

    // @Override
    // public Dimension getPreferredSize() {
    // return dimensions;
    // }
    @Override
    protected MigLayout getDialogLayout() {
        return super.getDialogLayout();
    }

    @Override
    protected JPanel createCaptchaPanel() {
        // loadImage(imageUrl);
        // use a container
        p = new JPanel(new MigLayout("ins 0,wrap 1", "[grow,fill]", "[][grow,fill]"));
        JLabel lbl;
        p.add(lbl = new JLabel("<html>" + getHelpText().replace("\r\n", "<br>") + "</html>"));

        SwingUtils.setOpaque(lbl, false);
        drawPanel = new JLayeredPane();
        LAFOptions.applyBackground(LAFOptions.getInstance().getColorForPanelBackground(), p);
        LAFOptions.applyBackground(LAFOptions.getInstance().getColorForPanelBackground(), drawPanel);

        int offset = 4;
        KeyCaptchaDrawBackgroundPanel background;
        // boolean sampleImg = false;
        drawPanel.add(background = new KeyCaptchaDrawBackgroundPanel(imageData.backgroundImage), new Integer(JLayeredPane.DEFAULT_LAYER), new Integer(JLayeredPane.DEFAULT_LAYER));

        mouseArray = new ArrayList<Integer>();
        drawPanel.add(new KeyCaptchaDragPieces(imageData.sampleImage, offset, true, mouseArray, challenge), new Integer(JLayeredPane.DEFAULT_LAYER) + 0, new Integer(JLayeredPane.DEFAULT_LAYER) + 0);
        System.out.println("PIeces " + imageData.pieces.size());
        for (int i = 0; i < imageData.pieces.size(); i++) {

            drawPanel.add(new KeyCaptchaDragPieces(imageData.pieces.get(i), offset, false, mouseArray, challenge), new Integer(JLayeredPane.DEFAULT_LAYER) + (i + 1), new Integer(JLayeredPane.DEFAULT_LAYER) + (i + 1));

            offset += 4;
        }

        p.add(drawPanel);

        drawPanel.setPreferredSize(background.getPreferredSize());
        return p;

    }
}