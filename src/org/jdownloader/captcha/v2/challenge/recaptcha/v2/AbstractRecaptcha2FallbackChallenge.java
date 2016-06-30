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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.appwork.utils.Application;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.images.IconIO;
import org.appwork.utils.logging2.extmanager.LoggerFactory;
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

import jd.controlling.captcha.SkipRequest;

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

    @Override
    public void initController(SolverJob<String> job) {
        System.out.println("Set new Job: " + job);
        super.initController(job);
        owner.initController(job);
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
    private static final Color COLOR_BG        = new Color(0x4A90E2);

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
            final int columnWidth = img.getWidth() / 3;
            final int rowHeight = img.getHeight() / 3;
            final Font font = new Font("Arial", 0, 12).deriveFont(Font.BOLD);
            for (int yslot = 0; yslot < 3; yslot++) {
                for (int xslot = 0; xslot < 3; xslot++) {
                    int xx = (xslot) * columnWidth;
                    int yy = (yslot) * rowHeight;
                    if (isSlotAnnotated(xslot, yslot)) {

                        int num = xslot + yslot * 3 + 1;
                        final BufferedImage jpg = new BufferedImage(columnWidth, rowHeight, BufferedImage.TYPE_INT_RGB);
                        final Graphics g = jpg.getGraphics();
                        try {
                            // g.drawImage(img, xx, yy, columnWidth, rowHeight, null);
                            g.setFont(font);
                            g.drawImage(img, 0, 0, columnWidth, rowHeight, xx, yy, xx + columnWidth, yy + rowHeight, null);
                            g.setColor(Color.WHITE);
                            g.fillRect(columnWidth - 20, 0, 20, 20);
                            g.setColor(COLOR_BG);
                            g.drawString(num + "", columnWidth - 20 + 5, 0 + 15);
                            images.add(IconIO.toDataUrl(jpg, IconIO.DataURLFormat.JPG));
                        } catch (NullPointerException e) {
                            // java.lang.NullPointerException
                            // at sun.awt.FontConfiguration.getVersion(FontConfiguration.java:1264)
                            // at sun.awt.FontConfiguration.readFontConfigFile(FontConfiguration.java:219)
                            // at sun.awt.FontConfiguration.init(FontConfiguration.java:107)
                            if (Application.isHeadless()) {
                                g.drawImage(img, 0, 0, columnWidth, rowHeight, xx, yy, xx + columnWidth, yy + rowHeight, null);
                                g.setColor(Color.WHITE);
                                g.fillRect(columnWidth - 20, 0, 20, 20);
                                images.add(IconIO.toDataUrl(jpg, IconIO.DataURLFormat.JPG));
                            } else {
                                throw e;
                            }
                        } finally {
                            if (g != null) {
                                g.dispose();
                            }
                        }
                    }

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

    protected boolean isSlotAnnotated(int xslot, int yslot) {
        return true;
    }

    @Override
    public BufferedImage getAnnotatedImage() throws IOException {
        final BufferedImage img = ImageIO.read(getImageFile());
        try {
            final Font font = new Font("Arial", 0, 12);
            final FontMetrics fm = img.getGraphics().getFontMetrics(font);
            final String exeplain = getExplain();
            // final Icon icon = getExplainIcon(exeplain);
            final String key = getHighlightedExplain();
            final ArrayList<String> lines = new ArrayList<String>();
            lines.add(key.toUpperCase(Locale.ENGLISH));
            lines.addAll(split(fm, getExplain().replaceAll("<.*?>", "")));
            if (getChooseAtLeast() > 0) {
                lines.addAll(split(fm, _GUI.T.RECAPTCHA_2_Dialog_help(getChooseAtLeast())));
            }

            lines.add("Example answer: 2,5,6");
            lines.add("If there is no match, answer with - or 0");
            int y = 0;
            int width = 0;
            int textHeight = 2;
            for (String line : lines) {
                width = Math.max(width, fm.stringWidth(line));
                textHeight += LINE_HEIGHT;
            }
            textHeight += LINE_HEIGHT;
            y += 5;
            y += 8;
            width += 10;

            final BufferedImage newImage = IconIO.createEmptyImage(Math.max(width, img.getWidth()), img.getHeight() + textHeight);
            final Graphics2D g = (Graphics2D) newImage.getGraphics();
            try {
                int x = 5;

                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g.setColor(COLOR_BG);
                g.fillRect(0, 0, newImage.getWidth(), newImage.getHeight());
                g.setColor(Color.WHITE);
                g.setFont(font.deriveFont(Font.BOLD));
                g.drawString(lines.remove(0), (newImage.getWidth() - img.getWidth()) / 2, y);
                g.setFont(font);

                y += 5;

                g.setColor(Color.WHITE);
                g.fillRect(0, y, newImage.getWidth(), img.getHeight() + 10);
                y += 5;

                Rectangle bounds = new Rectangle((newImage.getWidth() - img.getWidth()) / 2, y, img.getWidth(), img.getHeight());
                g.drawImage(img, bounds.x, bounds.y, null);
                g.setFont(new Font("Arial", 0, 16).deriveFont(Font.BOLD));
                double columnWidth = bounds.getWidth() / getSplitWidth();
                double rowHeight = bounds.getHeight() / getSplitHeight();
                for (int yslot = 0; yslot < getSplitHeight(); yslot++) {
                    for (int xslot = 0; xslot < getSplitWidth(); xslot++) {
                        if (isSlotAnnotated(xslot, yslot)) {
                            double xx = (xslot) * columnWidth;
                            double yy = (yslot) * rowHeight;
                            int num = xslot + yslot * getSplitWidth() + 1;
                            int xOff = xslot < (getSplitWidth() - 1) ? 2 : 0;
                            int yOff = yslot > 0 ? 2 : 0;
                            g.setColor(Color.WHITE);
                            g.fillRect(ceil(xx + columnWidth - 20 + bounds.x - xOff), ceil(yy + bounds.y + yOff), 20, 20);
                            g.setColor(COLOR_BG);

                            g.drawString(num + "", ceil(xx + columnWidth - 20 + bounds.x + 5 - xOff - (num >= 10 ? 4 : 0)), ceil(yy + bounds.y + 15 + yOff));
                        }
                    }
                }
                g.setColor(Color.WHITE);
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

                y = bounds.y + bounds.height;
                y += 5;

                g.setFont(font);
                g.setColor(Color.WHITE);
                for (String line : lines) {

                    y += LINE_HEIGHT;
                    g.drawString(line, 5, y);

                }

            } finally {
                g.dispose();
            }
            // Dialog.getInstance().showImage(newImage);
            return newImage;
        } catch (NullPointerException e) {
            // java.lang.NullPointerException
            // at sun.awt.FontConfiguration.getVersion(FontConfiguration.java:1264)
            // at sun.awt.FontConfiguration.readFontConfigFile(FontConfiguration.java:219)
            // at sun.awt.FontConfiguration.init(FontConfiguration.java:107)
            if (Application.isHeadless()) {
                return img;
            } else {
                throw e;
            }
        }
    }

    private Collection<? extends String> split(FontMetrics fm, String str) {
        str = str.replaceAll("[\r\n]+", " ");
        ArrayList<String> ret = new ArrayList<String>();
        while (str.length() > 0) {
            int max = str.length();
            while (fm.stringWidth(str.substring(0, max)) > 400) {
                max--;
            }
            if (max < str.length()) {
                int lastSpace = str.lastIndexOf(" ", max + 1);
                if (lastSpace > 0) {
                    max = lastSpace + 1;
                }
            }
            ret.add(str.substring(0, max).trim());
            str = str.substring(max);
        }
        return ret;
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
                try {
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
                        if (dupe.add(parts[i]) && in > 0) {
                            if (sb.length() > 0) {
                                sb.append(",");
                            }
                            sb.append(in);
                        }
                    }
                } catch (Throwable e) {
                    LoggerFactory.getDefaultLogger().info("Parse error: " + parts[i]);
                    LoggerFactory.getDefaultLogger().log(e);
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
        initController(challenge.getJob());
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

    public boolean doRunAntiDDosProtection() {
        return true;
    }

    public String getReloadErrorMessage() {
        return null;
    }

}