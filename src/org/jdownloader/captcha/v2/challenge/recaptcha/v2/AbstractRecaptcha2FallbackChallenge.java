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

import jd.controlling.captcha.SkipRequest;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.Application;
import org.appwork.utils.StringUtils;
import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.images.IconIO;
import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.ChallengeSolver;
import org.jdownloader.captcha.v2.ValidationResult;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.BasicCaptchaChallenge;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.CaptchaResponse;
import org.jdownloader.captcha.v2.solver.dbc.DeathByCaptchaSolver;
import org.jdownloader.captcha.v2.solver.imagetyperz.ImageTyperzCaptchaSolver;
import org.jdownloader.captcha.v2.solver.solver9kw.NineKwSolverService;
import org.jdownloader.captcha.v2.solverjob.SolverJob;
import org.jdownloader.controlling.UniqueAlltimeID;

public abstract class AbstractRecaptcha2FallbackChallenge extends BasicCaptchaChallenge {
    private static final int             LINE_HEIGHT = 16;
    protected final RecaptchaV2Challenge owner;
    private ArrayList<SubChallenge>      subChallenges;
    private SubChallenge                 subChallenge;

    public RecaptchaV2Challenge getRecaptchaV2Challenge() {
        return owner;
    }

    public boolean hasSubChallenge() {
        return subChallenge != null;
    }

    public SubChallenge getSubChallenge() {
        synchronized (this) {
            if (subChallenge == null) {
                createNewSubChallenge();
            }
        }
        return subChallenge;
    }

    public ArrayList<SubChallenge> getSubChallenges() {
        synchronized (this) {
            if (subChallenges == null) {
                return new ArrayList<SubChallenge>(0);
            } else {
                return new ArrayList<SubChallenge>(subChallenges);
            }
        }
    }

    public static enum ChallengeType {
        DYNAMIC,
        TILESELECT,
        IMAGESELECT;
    }

    protected SubChallenge createNewSubChallenge() {
        final SubChallenge sc = new SubChallenge();
        synchronized (this) {
            if (subChallenges == null) {
                subChallenges = new ArrayList<SubChallenge>();
            }
            subChallenges.add(sc);
            subChallenge = sc;
        }
        onNewChallenge(sc);
        return sc;
    }

    protected void onNewChallenge(SubChallenge sc) {
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

    @Override
    public SolverJob<String> getJob() {
        return owner.getJob();
    }

    protected String           token;
    public static final String WITH_OF_ALL_THE = "(?:with|of|all the) (.*?)(?:\\.|\\!|\\?|$)";
    private static final Color COLOR_BG        = new Color(0x4A90E2);
    protected boolean          useEnglish;

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
            final Font font = new Font(ImageProvider.getDrawFontName(), 0, 12).deriveFont(Font.BOLD);
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
            final String fontName = ImageProvider.getDrawFontName();
            final Font font = new Font(fontName, 0, 12);
            final FontMetrics fm = img.getGraphics().getFontMetrics(font.deriveFont(Font.BOLD));
            final String exeplain = getExplain();
            // final Icon icon = getExplainIcon(exeplain);
            final String key = subChallenge.getSearchKey();
            final ArrayList<String> lines = new ArrayList<String>();
            if (StringUtils.isNotEmpty(key)) {
                lines.add(key.toUpperCase(Locale.ENGLISH) + "?");
            } else {
                lines.add(subChallenge.getType().toUpperCase(Locale.ENGLISH) + "?");
            }
            lines.addAll(split(fm, getExplain().replaceAll("<.*?>", "")));
            for (String line : addAnnotationLines()) {
                lines.addAll(split(fm, line.replaceAll("<.*?>", "")));
            }
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
                String uidd = getSubChallenges().size() + "." + getSubChallenge().getReloudCounter();
                int w = fm.stringWidth(uidd);
                double factor = 0.8;
                g.setColor(new Color(Math.max((int) (COLOR_BG.getRed() * factor), 0), Math.max((int) (COLOR_BG.getGreen() * factor), 0), Math.max((int) (COLOR_BG.getBlue() * factor), 0), COLOR_BG.getAlpha()));
                g.drawString(uidd, x + newImage.getWidth() - w - 10, 13);
                g.setColor(Color.WHITE);
                g.setFont(font.deriveFont(Font.BOLD));
                g.drawString(lines.remove(0), Math.max(5, (newImage.getWidth() - img.getWidth()) / 2), y);
                g.setFont(font);
                y += 5;
                g.setColor(Color.WHITE);
                g.fillRect(0, y, newImage.getWidth(), img.getHeight() + 10);
                y += 5;
                Rectangle bounds = new Rectangle((newImage.getWidth() - img.getWidth()) / 2, y, img.getWidth(), img.getHeight());
                g.drawImage(img, bounds.x, bounds.y, null);
                g.setFont(new Font(fontName, 0, 16).deriveFont(Font.BOLD));
                double columnWidth = bounds.getWidth() / subChallenge.getGridWidth();
                double rowHeight = bounds.getHeight() / subChallenge.getGridHeight();
                for (int yslot = 0; yslot < subChallenge.getGridHeight(); yslot++) {
                    for (int xslot = 0; xslot < subChallenge.getGridWidth(); xslot++) {
                        if (isSlotAnnotated(xslot, yslot)) {
                            double xx = (xslot) * columnWidth;
                            double yy = (yslot) * rowHeight;
                            int num = xslot + yslot * subChallenge.getGridWidth() + 1;
                            int xOff = xslot < (subChallenge.getGridWidth() - 1) ? 2 : 0;
                            xOff -= 1;
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
                for (int yslot = 0; yslot < subChallenge.getGridHeight() - 1; yslot++) {
                    y = ceil((1 + yslot) * rowHeight);
                    g.drawLine(bounds.x, bounds.y + y, bounds.x + bounds.width, bounds.y + y);
                }
                for (int xslot = 0; xslot < subChallenge.getGridWidth() - 1; xslot++) {
                    x = ceil((1 + xslot) * columnWidth);
                    g.drawLine(bounds.x + x, bounds.y, bounds.x + x, bounds.y + bounds.height);
                }
                y = bounds.y + bounds.height;
                y += 5;
                g.setFont(font);
                g.setColor(Color.WHITE);
                for (String line : lines) {
                    y += LINE_HEIGHT;
                    if (line == lines.get(lines.size() - 1)) {
                        // g.setColor(Color.RED.brighter());
                        g.setFont(font.deriveFont(Font.BOLD, 14));
                    }
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

    protected ArrayList<String> addAnnotationLines() {
        return new ArrayList<String>();
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

    @Override
    public AbstractResponse<String> parseAPIAnswer(String result, String resultFormat, ChallengeSolver<?> solver) {
        try {
            if ("-".equals(result) || "".equals(result) || "0".equals(result)) {
                CaptchaResponse r = new CaptchaResponse(this, solver, "0", 100);
                return r;
            }
            HashSet<Integer> ret = new HashSet<Integer>();
            String clean = result.replaceAll("[^\\d,]", "");
            clean = clean.replaceAll("[,]+$", "");
            boolean bad = !clean.equals(result);
            final StringBuilder sb = new StringBuilder();
            final HashSet<String> dupe = new HashSet<String>();
            while (clean.length() > 0) {
                int index = clean.indexOf(",");
                if (index == -1) {
                    index = 1;
                }
                String part = clean.substring(0, index);
                int i = Integer.parseInt(part);
                while (i > getSubChallenge().getTileCount() && index > 1) {
                    index--;
                    part = clean.substring(0, index);
                    i = Integer.parseInt(part);
                }
                ret.add(i);
                clean = clean.substring(index);
                clean = clean.replaceAll("^[,]+", "");
            }
            for (Integer i : ret) {
                if (sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(i);
            }
            CaptchaResponse r = new CaptchaResponse(this, solver, sb.toString(), dupe.size() > 5 ? 0 : 100);
            if (bad) {
                r.setValidation(ValidationResult.INVALID);
            }
            return r;
            // }
        } catch (Throwable e) {
            throw new WTFException(e);
        }
    }

    public AbstractRecaptcha2FallbackChallenge(RecaptchaV2Challenge challenge) {
        super(challenge.getTypeID(), null, null, challenge.getExplain(), challenge.getPlugin(), 0);
        this.owner = challenge;
        setAccountLogin(owner.isAccountLogin());
        useEnglish |= NineKwSolverService.getInstance().isEnabled();
        useEnglish |= DeathByCaptchaSolver.getInstance().getService().isEnabled();
        useEnglish |= ImageTyperzCaptchaSolver.getInstance().getService().isEnabled();
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