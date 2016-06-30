package org.jdownloader.captcha.v2.challenge.keycaptcha.dialog;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.appwork.swing.MigPanel;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.DomainInfo;
import org.jdownloader.captcha.v2.AbstractCaptchaDialog;
import org.jdownloader.captcha.v2.challenge.keycaptcha.CategoryData;
import org.jdownloader.captcha.v2.challenge.keycaptcha.KeyCaptchaCategoryChallenge;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.components.HeaderScrollPane;

import jd.gui.swing.dialog.DialogType;
import net.miginfocom.swing.MigLayout;

public class KeyCaptchaCategoryDialog extends AbstractCaptchaDialog<String> implements ActionListener {

    // private BufferedImage[] kcImages;
    // private int kcSampleImg;

    private JPanel                      p;
    // private final Dimension dimensions;

    private KeyCaptchaCategoryChallenge challenge;

    private MigPanel                    pics;
    private int                         currentIndex;

    private Image                       image;
    private PicButton                   pic0;

    private PicButton                   pic1;

    private PicButton                   pic2;

    private ArrayList<Integer>          positions;
    private int                         maxHeight;

    private int                         maxWidth;

    public KeyCaptchaCategoryDialog(KeyCaptchaCategoryChallenge captchaChallenge2, int flag, DialogType type, DomainInfo domain, KeyCaptchaCategoryChallenge captchaChallenge) {
        super(captchaChallenge2, flag, _GUI.T.KeyCaptchaCategoryDialog(domain.getTld()), type, domain, _GUI.T.KeyCaptchaCategoryDialog_explain(domain.getTld()));
        // super(flag | Dialog.STYLE_HIDE_ICON | UIOManager.LOGIC_COUNTDOWN, title, null, null, null);

        this.challenge = captchaChallenge;

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
        return true;
    }

    @Override
    protected String createReturnValue() {
        if (Dialog.isOK(getReturnmask())) {
            StringBuilder sb = new StringBuilder();
            for (Integer i : positions) {
                if (sb.length() > 0) {
                    sb.append(".");
                }
                sb.append(Integer.toString(i));
            }
            return sb.toString();
        }
        return null;
    }

    @Override
    protected MigLayout getDialogLayout() {
        return super.getDialogLayout();
    }

    @Override
    public void pack() {
        super.pack();
        getDialog().setMinimumSize(new Dimension(300, 300));

    }

    protected HeaderScrollPane createHeaderScrollPane(MigPanel field) {
        HeaderScrollPane ret = new HeaderScrollPane(field);
        // ret.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        // ret.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        return ret;
    }

    @Override
    protected JPanel createCaptchaPanel() {
        // loadImage(imageUrl);
        // use a container
        p = new JPanel(new MigLayout("ins 0,wrap 1", "[grow,fill]", "[][][]"));
        SwingUtils.setOpaque(p, false);
        JLabel lbl;
        p.add(lbl = new JLabel("<html>" + getHelpText().replace("\r\n", "<br>") + "</html>"));
        SwingUtils.setOpaque(lbl, false);
        CategoryData data = challenge.getHelper().getCategoryData();
        // MigPanel panel = new MigPanel("ins 0,wrap 1", "[grow,fill]", "[]");
        // panel.add(label(new JLabel("Categories:")));

        MigPanel bgs = new MigPanel("ins 0,wrap 3", "[grow,fill][grow,fill][grow,fill]", "[]");

        SwingUtils.setOpaque(bgs, false);

        BufferedImage bg = data.getBackground();
        for (int i = 0; i < 3; i++) {

            BufferedImage cats = new BufferedImage(bg.getWidth() / 3, bg.getHeight(), Transparency.TRANSLUCENT);

            Graphics2D g = (Graphics2D) cats.getGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            g.drawImage(bg, 0, 0, bg.getWidth() / 3, bg.getHeight(), i * bg.getWidth() / 3, 0, (i + 1) * bg.getWidth() / 3, bg.getHeight(), null);
            g.dispose();
            bgs.add(new JLabel(new ImageIcon(cats)));
        }
        maxHeight = 0;
        maxWidth = 0;
        for (Image img : data.getImages()) {
            JButton bt = new JButton(new ImageIcon(img));
            Dimension pref = bt.getPreferredSize();
            maxWidth = Math.max(pref.width, maxWidth);
            maxHeight = Math.max(pref.height, maxHeight);
        }
        pics = new MigPanel("ins 0,wrap 3", "[grow,fill][grow,fill][grow,fill]", "[]");

        SwingUtils.setOpaque(pics, false);
        this.positions = new ArrayList<Integer>();
        p.add(bgs);
        p.add(pics);
        SwingUtils.setOpaque(lbl, false);
        okButton.setEnabled(false);
        setImage(0);
        return p;

    }

    private void setImage(int i) {
        currentIndex = i;
        if (pic0 != null) {
            int position = 0;
            if (pic0.isSelected()) {
                position = 1;
            } else if (pic1.isSelected()) {
                position = 2;
            } else if (pic2.isSelected()) {
                position = 3;
            }
            positions.add(position);

        }
        if (i >= challenge.getHelper().getCategoryData().getImages().size()) {
            okButton.setEnabled(true);
            pic0.setEnabled(false);
            pic1.setEnabled(false);
            pic2.setEnabled(false);
            okButton.doClick();
            return;
        }
        image = challenge.getHelper().getCategoryData().getImages().get(i);
        pics.removeAll();
        pics.add(pic0 = new PicButton(0, this, image));
        // ,"width "+maxWidth+"!, height "+maxHeight+"!"
        pics.add(pic1 = new PicButton(1, this, image));
        pics.add(pic2 = new PicButton(2, this, image));
        pic0.setPreferredSize(new Dimension(maxWidth, maxHeight));
        pic1.setPreferredSize(new Dimension(maxWidth, maxHeight));
        pic2.setPreferredSize(new Dimension(maxWidth, maxHeight));
        pics.revalidate();
        pics.repaint();
    }

    // private Component label(JLabel jLabel) {
    // SwingUtils.setOpaque(jLabel, false);
    // return jLabel;
    // }

    public void updateIcons(PicButton picButton) {
        pic0.setSelected(pic0 == picButton);
        pic1.setSelected(pic1 == picButton);
        pic2.setSelected(pic2 == picButton);
        pic0.updateIcon();
        pic1.updateIcon();
        pic2.updateIcon();
    }

    public void next() {
        setImage(currentIndex + 1);
    }

}