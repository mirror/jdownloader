package org.jdownloader.captcha.v2.challenge.recaptcha.v2;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;

import jd.controlling.captcha.SkipRequest;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
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
import org.jdownloader.captcha.v2.solverjob.SolverJob;
import org.jdownloader.controlling.UniqueAlltimeID;
import org.jdownloader.gui.translate._GUI;

public abstract class AbstractRecaptcha2FallbackChallenge extends BasicCaptchaChallenge {
    private static final int             LINE_HEIGHT = 16;
    protected final RecaptchaV2Challenge owner;
    protected String                     highlightedExplain;

    public String getType() {
        return type;
    }

    protected void killSession() {
        cleanup();
        getJob().kill();
    }

    protected int chooseAtLeast;

    @Override
    public SolverJob<String> getJob() {
        return owner.getJob();
    }

    public int getChooseAtLeast() {
        return chooseAtLeast;
    }

    protected String type;

    public String getHighlightedExplain() {
        return highlightedExplain;
    }

    private int splitWidth;

    public int getSplitWidth() {
        return splitWidth;
    }

    public void setSplitWidth(int splitWidth) {
        this.splitWidth = splitWidth;
    }

    public int getSplitHeight() {
        return splitHeight;
    }

    public void setSplitHeight(int splitHeight) {
        this.splitHeight = splitHeight;
    }

    private int                splitHeight;
    protected String           token;

    public static final String WITH_OF_ALL_THE = "(?:with|of|all the) (.*?)(?:\\.|\\!|\\?|$)";

    protected boolean          useEnglish;
    protected Icon             explainIcon;

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
                    images.add(IconIO.toDataUrl(jpg, IconIO.DataURLFormat.JPG));
                }
            }
            return data;
        } else {
            // String mime = FileResponse.getMimeType(getImageFile().getName());
            final BufferedImage newImage = getAnnotatedImage();
            final String ret = IconIO.toDataUrl(newImage, IconIO.DataURLFormat.JPG);
            return ret;
        }
    }

    @Override
    public BufferedImage getAnnotatedImage() throws IOException {
        final BufferedImage img = ImageIO.read(getImageFile());
        final Font font = new Font("Arial", 0, 12);

        String exeplain = getExplain();
        Icon icon = getExplainIcon(exeplain);
        String key = getHighlightedExplain();

        ArrayList<String> lines = new ArrayList<String>();
        String[] ex = getExplain().replaceAll("<.*?>", "").split(Pattern.quote(key));
        if (getChooseAtLeast() > 0) {
            lines.add(_GUI.T.RECAPTCHA_2_Dialog_help(getChooseAtLeast()));
        }
        if (getType() != null && getType().startsWith("TileSelection")) {
            lines.add(_GUI.T.RECAPTCHA_2_Dialog_help_tile());
        }
        lines.add("Example answer: 2,5,6");
        FontMetrics fmBold = img.getGraphics().getFontMetrics(font.deriveFont(Font.BOLD));
        FontMetrics fm = img.getGraphics().getFontMetrics(font);
        int y = 0;
        int width = 0;
        for (String p : ex) {
            width += fm.stringWidth(p + "");
        }
        y += LINE_HEIGHT;
        width += fmBold.stringWidth(key);

        for (String line : lines) {
            width = Math.max(width, fm.stringWidth(line));
            y += LINE_HEIGHT;
        }
        y += 15;
        if (icon != null) {
            y = Math.max(y, icon.getIconHeight() + 10);

        }
        y += 7;
        width += 10;

        final BufferedImage newImage = IconIO.createEmptyImage(Math.max((icon == null ? 0 : (icon.getIconWidth() + 5)) + width, img.getWidth()), img.getHeight() + y);
        final Graphics2D g = (Graphics2D) newImage.getGraphics();
        try {
            int x = 5;
            y = LINE_HEIGHT;

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
            int afterIconX = x;
            x = afterIconX;
            boolean keyPrinted = false;
            for (int i = 0; i < ex.length; i++) {

                g.setFont(font);
                g.drawString(ex[i], x, y);
                x += g.getFontMetrics().stringWidth(ex[i]);

                if (ex.length == 1 || i < ex.length - 1) {
                    g.setFont(font.deriveFont(Font.BOLD));
                    g.drawString("" + key + "", x, y);
                    x += g.getFontMetrics().stringWidth("" + key + "");
                }
            }

            g.setFont(font);
            x = afterIconX;
            for (String line : lines) {
                y += LINE_HEIGHT;
                g.drawString(line, x, y);

            }
            y += 15;
            if (icon != null) {
                y = Math.max(y, icon.getIconHeight() + 10);

            }
            g.setColor(Color.WHITE);
            g.drawLine(0, y, newImage.getWidth(), y);
            y += 5;

            Rectangle bounds = new Rectangle((newImage.getWidth() - img.getWidth()) / 2, y, img.getWidth(), img.getHeight());
            g.drawImage(img, bounds.x, bounds.y, null);
            g.setFont(new Font("Arial", 0, 16).deriveFont(Font.BOLD));
            double columnWidth = bounds.getWidth() / getSplitWidth();
            double rowHeight = bounds.getHeight() / getSplitHeight();
            for (int yslot = 0; yslot < getSplitHeight(); yslot++) {
                for (int xslot = 0; xslot < getSplitWidth(); xslot++) {
                    double xx = (xslot) * columnWidth;
                    double yy = (yslot) * rowHeight;
                    int num = xslot + yslot * getSplitWidth() + 1;
                    int xOff = xslot < (getSplitWidth() - 1) ? 2 : 0;
                    int yOff = yslot > 0 ? 2 : 0;
                    g.setColor(Color.WHITE);
                    g.fillRect(ceil(xx + columnWidth - 20 + bounds.x - xOff), ceil(yy + bounds.y + yOff), 20, 20);
                    g.setColor(Color.BLACK);

                    g.drawString(num + "", ceil(xx + columnWidth - 20 + bounds.x + 5 - xOff - (num >= 10 ? 4 : 0)), ceil(yy + bounds.y + 15 + yOff));
                }
            }
            // g.setColor(Color.WHITE);
            int splitterWidth = 3;
            g.setStroke(new BasicStroke(splitterWidth));
            for (int yslot = 0; yslot < getSplitHeight() - 1; yslot++) {
                y = ceil((1 + yslot) * rowHeight);

                g.drawLine(bounds.x, bounds.y + y, bounds.x + bounds.width, bounds.y + y);

            }
            for (int xslot = 0; xslot < getSplitWidth() - 1; xslot++) {
                x = ceil((1 + xslot) * columnWidth);

                g.drawLine(bounds.x + x, bounds.y, bounds.x + x, bounds.y + bounds.height);
            }
        } finally {
            g.dispose();
        }
        // Dialog.getInstance().showImage(newImage);
        return newImage;
    }

    private int ceil(double d) {
        return (int) Math.ceil(d);
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
        // boolean singleDigit = getSplitHeight() * getSplitWidth() < 10;
        // if (singleDigit && false) {
        // json = json.replaceAll("[^\\d]+", "");
        //
        // final StringBuilder sb = new StringBuilder();
        // final HashSet<String> dupe = new HashSet<String>();
        // for (int i = 0; i < json.length(); i++) {
        // if (dupe.add(json.charAt(i) + "")) {
        // if (sb.length() > 0) {
        // sb.append(",");
        // }
        // sb.append(Integer.parseInt(json.charAt(i) + ""));
        // }
        // }
        // boolean enough = getChooseAtLeast() <= 0 || dupe.size() >= getChooseAtLeast();
        // return new CaptchaResponse(this, solver, sb.toString(), !enough || dupe.size() > 5 ? 0 : 100);
        // } else {
        json = json.replaceAll("(\\d\\d)([^\\,])", "$1,$2");
        String[] parts = json.split("[^\\d]+");

        final StringBuilder sb = new StringBuilder();
        final HashSet<String> dupe = new HashSet<String>();
        for (int i = 0; i < parts.length; i++) {
            if (StringUtils.isNotEmpty(parts[i])) {
                int in = Integer.parseInt(parts[i]);
                if (in > (getSplitHeight() * getSplitWidth())) {
                    // invalid split

                    String p1 = parts[i].substring(0, 1);
                    in = Integer.parseInt(p1);
                    if (dupe.add(p1)) {
                        if (sb.length() > 0) {
                            sb.append(",");
                        }
                        sb.append(in);
                    }

                    p1 = parts[i].substring(1);
                    in = Integer.parseInt(p1);
                    if (dupe.add(p1)) {
                        if (sb.length() > 0) {
                            sb.append(",");
                        }
                        sb.append(in);
                    }

                } else {
                    if (dupe.add(parts[i])) {
                        if (sb.length() > 0) {
                            sb.append(",");
                        }
                        sb.append(in);
                    }
                }
            }
        }
        boolean enough = getChooseAtLeast() <= 0 || dupe.size() >= getChooseAtLeast();
        return new CaptchaResponse(this, solver, sb.toString(), !enough || dupe.size() > 5 ? 0 : 100);

        // }
    }

    public AbstractRecaptcha2FallbackChallenge(RecaptchaV2Challenge challenge) {
        super(challenge.getTypeID(), null, null, challenge.getExplain(), challenge.getPlugin(), 0);
        splitHeight = 3;
        splitWidth = 3;
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

    abstract public void reload(int round) throws Throwable;

}