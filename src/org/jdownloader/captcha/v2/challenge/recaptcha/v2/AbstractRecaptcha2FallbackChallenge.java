package org.jdownloader.captcha.v2.challenge.recaptcha.v2;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.appwork.utils.Regex;
import org.appwork.utils.images.IconIO;
import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.ChallengeSolver;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.BasicCaptchaChallenge;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.CaptchaResponse;
import org.jdownloader.captcha.v2.solver.captchabrotherhood.CBSolver;
import org.jdownloader.captcha.v2.solver.dbc.DeathByCaptchaSolver;
import org.jdownloader.captcha.v2.solver.imagetyperz.ImageTyperzCaptchaSolver;
import org.jdownloader.captcha.v2.solver.solver9kw.NineKwSolverService;
import org.jdownloader.controlling.UniqueAlltimeID;

import jd.controlling.captcha.SkipRequest;

public abstract class AbstractRecaptcha2FallbackChallenge extends BasicCaptchaChallenge {
    protected final RecaptchaV2Challenge owner;
    protected String                     highlightedExplain;

    public String getHighlightedExplain() {
        return highlightedExplain;
    }

    protected String token;

    public static final String WITH_OF_ALL_THE = "(?:with|of|all the) (.*)";

    protected boolean useEnglish;
    protected Icon    explainIcon;

    @Override
    public Object getAPIStorable(String format) throws Exception {
        if ("single".equals(format)) {
            final HashMap<String, Object> data = new HashMap<String, Object>();
            data.put("instructions", "Type 145 if image 1,4 and 5 match the question above.");
            data.put("explain", getExplain());
            final ArrayList<String> images = new ArrayList<String>();
            data.put("images", images);
            final BufferedImage img = ImageIO.read(getImageFile());
            int columnWidth = img.getWidth() / 3;
            int rowHeight = img.getHeight() / 3;
            final Font font = new Font("Arial", 0, 12).deriveFont(Font.BOLD);
            for (int yslot = 0; yslot < 3; yslot++) {
                for (int xslot = 0; xslot < 3; xslot++) {
                    int xx = (xslot) * columnWidth;
                    int yy = (yslot) * rowHeight;
                    int num = xslot + yslot * 3 + 1;
                    final BufferedImage jpg = new BufferedImage(columnWidth, rowHeight, BufferedImage.TYPE_INT_RGB);
                    final Graphics g = jpg.getGraphics();
                    // g.drawImage(img, xx, yy, columnWidth, rowHeight, null);
                    g.setFont(font);
                    g.drawImage(img, 0, 0, columnWidth, rowHeight, xx, yy, xx + columnWidth, yy + rowHeight, null);
                    g.setColor(Color.WHITE);
                    g.fillRect(columnWidth - 20, 0, 20, 20);
                    g.setColor(Color.BLACK);
                    g.drawString(num + "", columnWidth - 20 + 5, 0 + 15);
                    g.dispose();
                    images.add(IconIO.toDataUrl(jpg));
                }
            }
            return data;
        } else {
            // String mime = FileResponse.getMimeType(getImageFile().getName());
            final BufferedImage newImage = getAnnotatedImage();
            final String ret = IconIO.toDataUrl(newImage);
            return ret;
        }
    }

    @Override
    public BufferedImage getAnnotatedImage() throws IOException {
        final BufferedImage img = ImageIO.read(getImageFile());
        final Font font = new Font("Arial", 0, 12);

        // toto:
        // add example image here
        String exeplain = getExplain();
        Icon icon = getExplainIcon(exeplain);
        File file = getImageFile();

        final String instructions = "Type 15 if image 1 and 5 match the question above. Choose 2,3,4 or 5 images!";
        final int explainWidth = img.getGraphics().getFontMetrics(font.deriveFont(Font.BOLD)).stringWidth(getExplain()) + 10;
        final int solutionWidth = img.getGraphics().getFontMetrics(font).stringWidth(instructions) + 10;

        final BufferedImage newImage = IconIO.createEmptyImage(Math.max((icon == null ? 0 : (icon.getIconWidth() + 5)) + Math.max(explainWidth, solutionWidth), img.getWidth()), img.getHeight() + 4 * 20);
        final Graphics2D g = (Graphics2D) newImage.getGraphics();
        int x = 5;
        int y = 0;

        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, newImage.getWidth(), newImage.getHeight());
        g.setColor(Color.WHITE);
        g.setFont(font.deriveFont(Font.BOLD));
        if (icon != null) {
            icon.paintIcon(null, g, x, 5);

            g.drawRect(x, 5, icon.getIconWidth(), icon.getIconHeight());
            x += icon.getIconWidth() + 5;
        }
        g.drawString(getExplain(), x, y += 20);
        g.setFont(font);
        g.drawString("Instructions:", x, y += 20);
        g.drawString(instructions, x, y += 20);
        y += 10;
        if (icon != null) {
            y = Math.max(y, icon.getIconHeight() + 5);

        }
        g.setColor(Color.WHITE);
        g.drawLine(0, y, newImage.getWidth(), y);
        y += 5;
        int xOffset;
        g.drawImage(img, xOffset = (newImage.getWidth() - img.getWidth()) / 2, y, null);
        g.setFont(new Font("Arial", 0, 16).deriveFont(Font.BOLD));
        int columnWidth = img.getWidth() / 3;
        int rowHeight = img.getHeight() / 3;
        for (int yslot = 0; yslot < 3; yslot++) {
            for (int xslot = 0; xslot < 3; xslot++) {
                int xx = (xslot) * columnWidth;
                int yy = (yslot) * rowHeight;
                int num = xslot + yslot * 3 + 1;
                g.setColor(Color.WHITE);
                g.fillRect(xx + columnWidth - 20 + xOffset, yy + y, 20, 20);
                g.setColor(Color.BLACK);
                g.drawString(num + "", xx + columnWidth - 20 + xOffset + 5, yy + y + 15);
            }
        }
        g.dispose();
        // Dialog.getInstance().showImage(newImage);
        return newImage;
    }

    public Icon getExplainIcon(String exeplain) {
        if (explainIcon != null) {
            return IconIO.getScaledInstance(explainIcon, 80, 55);
        }
        try {
            String filename = new Regex(exeplain, WITH_OF_ALL_THE).getMatch(0).replaceAll("[^\\w]", "") + ".jpg";
            try {

                return IconIO.getScaledInstance(new ImageIcon(ImageIO.read(getClass().getResource("example/" + filename))), 80, 55);

            } catch (Throwable e) {
                // no example icon
                System.out.println("No Example " + exeplain);
            }
        } catch (Throwable e) {

        }
        return null;
    }

    @Override
    public AbstractResponse<String> parseAPIAnswer(String json, ChallengeSolver<?> solver) {
        json = json.replaceAll("[^\\d]+", "");
        final StringBuilder sb = new StringBuilder();
        final HashSet<String> dupe = new HashSet<String>();
        for (int i = 0; i < json.length(); i++) {
            if (dupe.add(json.charAt(i) + "")) {
                if (sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(Integer.parseInt(json.charAt(i) + ""));
            }
        }
        return new CaptchaResponse(this, solver, sb.toString(), dupe.size() < 2 || dupe.size() > 5 ? 0 : 100);
    }

    public AbstractRecaptcha2FallbackChallenge(RecaptchaV2Challenge challenge) {
        super(challenge.getTypeID(), null, null, challenge.getExplain(), challenge.getPlugin(), 0);
        this.owner = challenge;
        setAccountLogin(owner.isAccountLogin());
        useEnglish |= NineKwSolverService.getInstance().isEnabled();
        useEnglish |= DeathByCaptchaSolver.getInstance().getService().isEnabled();
        useEnglish |= ImageTyperzCaptchaSolver.getInstance().getService().isEnabled();
        useEnglish |= CBSolver.getInstance().getService().isEnabled();

    }

    @Override
    public UniqueAlltimeID getId() {
        return owner.getId();
    }

    abstract protected void load();

    public String getToken() {
        return token;
    }

    @Override
    public boolean canBeSkippedBy(SkipRequest skipRequest, ChallengeSolver<?> solver, Challenge<?> challenge) {
        return owner.canBeSkippedBy(skipRequest, solver, challenge);
    }

}