//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.captcha.easy.load;

import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import jd.captcha.easy.EasyMethodFile;
import jd.captcha.translate.T;
import jd.gui.swing.components.JDTextField;
import jd.http.Browser;
import jd.nutils.Screen;
import jd.parser.html.Form;
import jd.parser.html.HTMLParser;
import jd.parser.html.InputField;
import jd.utils.JDUtilities;

import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.EDTHelper;

public class LoadCaptchas {
    private static final long serialVersionUID = 1L;

    /**
     * Teilt Forms mit mehreren Submitbuttons (wegen Premium und Free Button
     * notwendig)
     * 
     * @param browser
     * @return Form[]
     */
    static Form[] getForms(final Browser browser) {
        final ArrayList<Form> retForms = new ArrayList<Form>();
        final Form[] forms = browser.getForms();
        for (final Form form : forms) {
            final ArrayList<InputField> fi = form.getInputFieldsByType("submit");
            if (fi.size() > 1) {
                for (int i = 1; i < fi.size(); i++) {
                    final Form fo = new Form(form.getHtmlCode());
                    fo.getInputFields().remove(fo.getInputFieldsByType("submit").get(i));
                    if (!retForms.contains(fo)) {
                        retForms.add(fo);
                    }
                }
                form.getInputFields().remove(fi.get(0));
            }
            if (!retForms.contains(form)) {
                retForms.add(form);
            }
        }
        return retForms.toArray(new Form[] {});
    }

    /**
     * gibt die Bildendung die im Header steht zurück z.B.: .jpg
     * 
     * @param br
     * @return
     */
    private static String getImageExtentionFromHeader(final Browser br) {
        String ret = null;
        String contentType = br.getHttpConnection().getContentType();
        contentType = contentType.contains(";") ? contentType.substring(0, contentType.indexOf(";")) : contentType;

        if (contentType != null && contentType.toLowerCase().contains("image")) {
            if (contentType.toLowerCase().equals("image/jpeg")) {
                ret = ".jpg";
            } else {
                ret = contentType.toLowerCase().replaceFirst("image/", ".");
            }
        }
        return ret;
    }

    /**
     * gibt alle Bildadressen einer Seite aus
     * 
     * @param br
     * @return String[]
     * @throws Exception
     */
    static String[] getImages(final Browser br) throws Exception {
        final ArrayList<String> ret = new ArrayList<String>();
        final Pattern[] basePattern = new Pattern[] { Pattern.compile("(?is)<[ ]?input[^>]*?src=['|\"]?([^>\\s'\"]*)['|\">\\s][^>]*?type=.?image[^>]*?", Pattern.CASE_INSENSITIVE), Pattern.compile("(?is)<[ ]?input[^>]*?type=.?image[^>]*?src=['|\"]?([^>\\s'\"]*)['|\">\\s]", Pattern.CASE_INSENSITIVE), Pattern.compile("(?is)<[ ]?IMG[^>]*?src=['|\"]?([^>\\s'\"]*)['|\">\\s]", Pattern.CASE_INSENSITIVE) };
        for (final Pattern element : basePattern) {
            final Matcher m = element.matcher(br.toString());
            while (m.find()) {
                try {
                    String src = m.group(1);
                    if (!src.startsWith("http")) {
                        if (src.charAt(0) == '/') {
                            src = "http://" + br.getHost() + src;
                        } else if (src.charAt(0) == '#') {
                            src = "http://" + br.getURL() + src;
                        } else {
                            src = br.getBaseURL() + src;
                        }
                    }
                    if (!ret.contains(src)) {
                        ret.add(src);
                    }
                } catch (final Exception e) {
                }

            }
        }

        return ret.toArray(new String[] {});
    }

    /**
     * öffnet einen Ordner
     * 
     * @param dir
     */
    private static void openDir(final String dir) {
        new EDTHelper<Object>() {
            public Object edtRun() {
                if (JOptionPane.showConfirmDialog(null, "Captcha Ordner:" + dir + " jetzt öffnen?") == JOptionPane.YES_OPTION) {
                    CrossSystem.openFile(new File(dir));
                }

                return null;
            }
        }.waitForEDT();

    }

    public String                host;
    public boolean               opendir   = false;
    private LoadInfo             loadinfo;
    private final Browser        br        = new Browser();
    {
        br.setFollowRedirects(true);
    }
    private ArrayList<LoadImage> images;
    private LoadImage            selectedImage;
    private final JFrame         owner;

    public int                   maxHeight = 500;

    public boolean               threaded  = false;

    public int                   maxWeight = 600;

    /**
     * Ordner in den die Bilder geladen werden (default: jdCaptchaFolder/host)
     * 
     */
    private String               dir       = null;

    /**
     * start aufrufen um den ladeprozess zu initialisieren
     * 
     * @return
     */
    public LoadCaptchas(final JFrame owner) {
        this(owner, null);
    }

    /**
     * 
     * @param hostname
     *            wenn der Hostname = null ist wird er aus dem Link erstellt
     * @return
     */
    public LoadCaptchas(final JFrame owner, final String host) {
        this(owner, host, false);
    }

    /**
     * 
     * @param hostname
     *            wenn der Hostname = null ist wird er aus dem Link erstellt
     * @param opendir
     *            ob am ende der Captchaordner im Browser geöffnet werden soll
     * @return
     */
    public LoadCaptchas(final JFrame owner, final String host, final boolean opendir) {
        this.host = host;
        this.opendir = opendir;
        this.owner = owner;
    }

    /**
     * LoadImageliste einer Seite (folgt Forms)
     * 
     * @param br
     * @return ArrayList<LoadImage>
     */
    private ArrayList<LoadImage> getAllImages(final Browser br) {
        final ArrayList<LoadImage> images = new ArrayList<LoadImage>();
        String[] imagea;
        try {
            imagea = getImages(br);
            for (int i = 0; i < imagea.length; i++) {
                final LoadImage li = new LoadImage(loadinfo, imagea[i], br);
                li.form = -1;
                li.location = i;
                images.add(li);
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
        if (loadinfo.followLinks) {
            final String[] links = HTMLParser.getHttpLinks(br.toString(), br.getURL());
            for (int b = 0; b < links.length; b++) {
                final String string = links[b];
                try {
                    final Browser brc = br.cloneBrowser();
                    brc.getPage(string);
                    imagea = getImages(brc);
                    for (int i = 0; i < imagea.length; i++) {
                        final LoadImage li = new LoadImage(loadinfo, imagea[i], brc);
                        li.form = -1;
                        li.location = i;
                        li.followUrl = b;
                        images.add(li);
                    }

                } catch (final Exception e) {
                }
            }

        }

        final Form[] forms = getForms(br);
        for (int i = 0; i < forms.length; i++) {
            try {
                final Form form = forms[i];
                final Browser brc = br.cloneBrowser();

                brc.submitForm(form);

                imagea = getImages(brc);
                for (int b = 0; b < imagea.length; b++) {

                    final LoadImage li = new LoadImage(loadinfo, imagea[b], brc);
                    li.form = i;
                    li.location = b;
                    if (images.contains(li)) {
                        continue;
                    }
                    images.add(li);
                }
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
        return images;
    }

    /**
     * Dialog fragt nach dem Link und der anzahl der zu ladenden Captchas
     * 
     * @return
     */
    private LoadInfo getLoadInfo(final LoadImage loadImage) {
        final JDialog dialog = new EDTHelper<JDialog>() {
            public JDialog edtRun() {
                return new JDialog(owner);
            }
        }.getReturnValue();
        dialog.setModal(true);
        dialog.setAlwaysOnTop(true);

        final JPanel p = new EDTHelper<JPanel>() {
            public JPanel edtRun() {
                final JPanel ret = new JPanel(new GridLayout(6, 2));
                ret.add(new JLabel(T._.easycaptcha_loadcaptchas_link() + ":"));
                return ret;

            }
        }.getReturnValue();

        final JDTextField tfl = new EDTHelper<JDTextField>() {
            public JDTextField edtRun() {
                return new JDTextField();
            }
        }.getReturnValue();
        tfl.setBorder(BorderFactory.createEtchedBorder());

        p.add(tfl);
        final JSpinner sm = new EDTHelper<JSpinner>() {
            public JSpinner edtRun() {
                p.add(new JLabel(T._.easycaptcha_loadcaptchas_howmuch() + ":"));
                if (loadImage != null) {
                    tfl.setText(loadImage.baseUrl);
                }
                return new JSpinner(new SpinnerNumberModel(100, 1, 4000, 1));
            }
        }.getReturnValue();
        p.add(sm);
        final JCheckBox followLinks = new EDTHelper<JCheckBox>() {
            public JCheckBox edtRun() {
                p.add(new JLabel(T._.easycaptcha_loadcaptchas_followlinks() + ":"));
                final JCheckBox checkBox = new JCheckBox();
                checkBox.setSelected(false);
                p.add(checkBox);
                return checkBox;
            }
        }.getReturnValue();
        final JCheckBox threadedCheck = new EDTHelper<JCheckBox>() {
            public JCheckBox edtRun() {
                p.add(new JLabel(T._.easycaptcha_loadcaptchas_threaded() + ":"));
                final JCheckBox checkBox = new JCheckBox();
                checkBox.setSelected(false);
                p.add(checkBox);
                return checkBox;
            }
        }.getReturnValue();
        final JCheckBox loadDirect = new EDTHelper<JCheckBox>() {
            public JCheckBox edtRun() {
                p.add(new JLabel(T._.easycaptcha_loadcaptchas_loaddirect() + ":"));
                final JCheckBox checkBox = new JCheckBox();
                checkBox.setSelected(true);
                p.add(checkBox);
                return checkBox;
            }
        }.getReturnValue();
        final JButton ok = new EDTHelper<JButton>() {
            public JButton edtRun() {
                return new JButton(T._.gui_btn_ok());
            }
        }.getReturnValue();
        ok.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                dialog.dispose();
            }
        });
        p.add(ok);
        final WindowListener l = new WindowListener() {
            public void windowActivated(final WindowEvent e) {
            }

            public void windowClosed(final WindowEvent e) {

            }

            public void windowClosing(final WindowEvent e) {
                tfl.setText("");
                dialog.dispose();
            }

            public void windowDeactivated(final WindowEvent e) {
            }

            public void windowDeiconified(final WindowEvent e) {
            }

            public void windowIconified(final WindowEvent e) {
            }

            public void windowOpened(final WindowEvent e) {
            }
        };
        final JButton cancel = new EDTHelper<JButton>() {
            public JButton edtRun() {
                return new JButton(T._.gui_btn_cancel());
            }
        }.getReturnValue();
        cancel.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                tfl.setText("");
                dialog.dispose();
            }
        });
        p.add(cancel);

        dialog.addWindowListener(l);
        dialog.add(p);
        new EDTHelper<Object>() {
            public Object edtRun() {
                dialog.setLocation(Screen.getCenterOfComponent(owner, dialog));
                dialog.pack();
                dialog.setAlwaysOnTop(true);

                dialog.setVisible(true);

                return null;
            }
        }.waitForEDT();
        final String link = tfl.getText();
        if (link == null || link.matches("\\s*")) { return null; }
        final int menge = (Integer) sm.getValue();
        dialog.dispose();
        final LoadInfo retLI = new LoadInfo(link, menge);
        retLI.followLinks = followLinks.isSelected();
        threaded = threadedCheck.isSelected();
        retLI.directLoad = loadDirect.isSelected();
        return retLI;

    }

    /**
     * läd direkt wenn die URL ein Bild ist
     * 
     * @param dir
     * @param br
     * @param loadinfo
     * @return true wenn die url ein Bild ist
     */
    private boolean loadDirect() {

        String i = getImageExtentionFromHeader(br);
        if (i.contains(";")) {
            i = i.substring(0, i.indexOf(";"));
        }
        final String imageType = i;
        if (imageType != null) {
            final Runnable runnable = new Runnable() {
                public void run() {
                    if (!threaded) {
                        for (int k = 0; k < loadinfo.menge; k++) {
                            try {
                                final File f2 = new File(dir + System.currentTimeMillis() + imageType);
                                br.getDownload(f2, loadinfo.link);
                                System.out.println("Download " + f2);
                                Thread.sleep(5000);
                            } catch (final Exception ev) {
                                ev.printStackTrace();
                            }

                        }
                    } else {
                        final Thread[] ths = new Thread[loadinfo.menge];
                        for (int k = 0; k < loadinfo.menge; k++) {
                            ths[k] = new Thread(new Runnable() {
                                public void run() {
                                    try {

                                        final File f2 = new File(dir + System.currentTimeMillis() + imageType);
                                        br.getDownload(f2, loadinfo.link);
                                        System.out.println("Download " + f2);
                                        Thread.sleep(5000);
                                    } catch (final Exception ev) {
                                        ev.printStackTrace();
                                    }
                                    synchronized (this) {
                                        notify();

                                    }
                                }
                            });
                            ths[k].start();
                        }
                        for (final Thread thread : ths) {
                            while (thread.isAlive()) {
                                synchronized (thread) {
                                    try {
                                        thread.wait(30000);
                                    } catch (final InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    }
                }
            };
            final Thread th = new Thread(runnable);
            th.start();

            return true;
        }
        return false;
    }

    /**
     * Läd die Bilder eines ArrayList<LoadImage> und zeigt den Fortschritt in
     * einem Progressdialog an
     * 
     */
    private void loadImages() {

        final Thread th = new Thread(new Runnable() {
            public void run() {
                final Thread[] jb = new Thread[images.size()];
                for (int j = 0; j < images.size(); j++) {
                    final int i = j;
                    jb[i] = new Thread(new Runnable() {

                        public void run() {
                            final LoadImage image = images.get(i);
                            image.directCaptchaLoad(dir);
                            synchronized (jb[i]) {
                                jb[i].notify();
                            }
                        }
                    });
                    jb[i].start();
                }

                for (final Thread thread : jb) {
                    while (thread.isAlive()) {
                        synchronized (thread) {
                            try {
                                thread.wait(3000);
                            } catch (final InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        });

        th.start();

    }

    private void loadProcess() {
        final Runnable runnable = new Runnable() {
            public void run() {
                try {
                    if (images != null) {
                        for (final LoadImage loadImage : images) {
                            if (!loadImage.file.equals(selectedImage.file)) {
                                loadImage.file.delete();
                            }
                        }
                    }
                    final boolean direct = selectedImage.directCaptchaLoad(dir);
                    LoadImage.save(selectedImage, host);
                    if (direct && loadinfo.directLoad) {
                        if (!threaded) {
                            for (int k = 1; k < loadinfo.menge - 1; k++) {
                                selectedImage.directCaptchaLoad(dir);
                            }
                        } else {
                            final Thread[] ths = new Thread[loadinfo.menge];
                            for (int k = 1; k < loadinfo.menge - 1; k++) {
                                ths[k] = new Thread(new Runnable() {
                                    public void run() {
                                        try {
                                            selectedImage.directCaptchaLoad(dir);

                                        } catch (final Exception ev) {
                                            ev.printStackTrace();
                                        }
                                        synchronized (this) {
                                            notify();

                                        }
                                    }
                                });
                                ths[k].start();
                            }
                            for (final Thread thread : ths) {
                                while (thread != null && thread.isAlive()) {
                                    synchronized (thread) {
                                        try {
                                            thread.wait(30000);
                                        } catch (final InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }
                        }

                    } else {
                        selectedImage.file.delete();

                        if (!threaded) {
                            for (int k = 1; k < loadinfo.menge - 1; k++) {
                                selectedImage.load(host);
                            }
                        } else {
                            final Thread[] ths = new Thread[loadinfo.menge];
                            for (int k = 1; k < loadinfo.menge - 1; k++) {
                                ths[k] = new Thread(new Runnable() {
                                    public void run() {
                                        try {
                                            selectedImage.load(host);

                                        } catch (final Exception ev) {
                                            ev.printStackTrace();
                                        }
                                        synchronized (this) {
                                            notify();
                                        }
                                    }
                                });
                                ths[k].start();
                            }
                            for (final Thread thread : ths) {
                                while (thread != null && thread.isAlive()) {
                                    synchronized (thread) {
                                        try {
                                            thread.wait(30000);
                                        } catch (final InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }
                        }

                    }
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }
        };

        final Thread th2 = new Thread(runnable);
        th2.start();
    }

    /**
     * @return true wenn erfolgreich geladen wurde
     */
    public boolean start() {
        try {
            selectedImage = LoadImage.loadFile(host);
            loadinfo = getLoadInfo(selectedImage);
            if (loadinfo == null) { return false; }
            final JDialog dialog = new EDTHelper<JDialog>() {
                public JDialog edtRun() {
                    return new JDialog(owner);
                }
            }.getReturnValue();
            dialog.setModal(true);
            dialog.setAlwaysOnTop(true);
            br.getPage(loadinfo.link);
            if (host == null) {
                host = br.getHost().toLowerCase();
                if (host.matches(".*\\..*\\..*")) {
                    host = host.substring(host.indexOf('.') + 1);
                }
            }
            if (dir == null) {
                dir = JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath() + "/captchas/" + host + "/";
            }
            new File(dir).mkdir();
            if (loadDirect()) {
                if (opendir) {
                    openDir(dir);
                }
                new EasyMethodFile(host).copyExampleImage();
                return true;
            }
            if (selectedImage != null) {
                selectedImage.load(host);
            } else {
                dialog.setTitle(T._.easycaptcha_loadcaptchas_clickoncaptcha());
                images = getAllImages(br);
                loadImages();
                dialog.addWindowListener(new WindowListener() {

                    public void windowActivated(final WindowEvent e) {
                    }

                    public void windowClosed(final WindowEvent e) {

                    }

                    public void windowClosing(final WindowEvent e) {
                        for (final LoadImage loadImage : images) {
                            loadImage.file.delete();
                        }
                        dialog.dispose();
                    }

                    public void windowDeactivated(final WindowEvent e) {
                    }

                    public void windowDeiconified(final WindowEvent e) {
                    }

                    public void windowIconified(final WindowEvent e) {
                    }

                    public void windowOpened(final WindowEvent e) {
                    }
                });

                final ArrayList<JButton> bts = new ArrayList<JButton>();
                for (int j = 0; j < images.size(); j++) {
                    final LoadImage f = images.get(j);
                    if (f == null || f.file == null || !f.file.exists() || f.file.length() < 100) {
                        continue;
                    }
                    final BufferedImage captchaImage = ImageIO.read(f.file);
                    if (captchaImage == null) {
                        f.file.delete();
                        continue;
                    }
                    final int area = captchaImage.getHeight(null) * captchaImage.getHeight(null);
                    if (area < 50 || captchaImage.getHeight(null) > maxHeight || captchaImage.getWidth(null) > maxWeight || captchaImage.getWidth(null) < 10 || captchaImage.getHeight(null) < 5) {
                        f.file.delete();
                        continue;
                    }
                    final double faktor = Math.max((double) captchaImage.getWidth(null) / 100, (double) captchaImage.getHeight(null) / 100);
                    final int width = (int) (captchaImage.getWidth(null) / faktor);
                    final int height = (int) (captchaImage.getHeight(null) / faktor);
                    try {
                        final JButton ic = new EDTHelper<JButton>() {
                            public JButton edtRun() {
                                return new JButton(new ImageIcon(captchaImage.getScaledInstance(width, height, Image.SCALE_SMOOTH)));
                            }
                        }.getReturnValue();
                        ic.addActionListener(new ActionListener() {

                            public void actionPerformed(final ActionEvent e) {
                                selectedImage = f;
                                dialog.dispose();
                            }
                        });
                        bts.add(ic);
                    } catch (final Exception e) {
                        e.printStackTrace();
                    }

                }
                final JPanel panel = new EDTHelper<JPanel>() {
                    public JPanel edtRun() {
                        return new JPanel(new GridLayout((int) Math.ceil((double) bts.size() / 5), 5));
                    }
                }.getReturnValue();
                for (final JButton button : bts) {
                    panel.add(button);

                }
                new EDTHelper<Object>() {
                    public Object edtRun() {
                        dialog.add(new JScrollPane(panel));

                        dialog.pack();
                        dialog.setLocation(Screen.getCenterOfComponent(owner, dialog));
                        dialog.setAlwaysOnTop(true);
                        dialog.setVisible(true);

                        return null;
                    }
                }.waitForEDT();
            }
            if (selectedImage != null && selectedImage.file != null) {
                loadProcess();
                if (opendir) {
                    openDir(dir);
                }
                new EasyMethodFile(host).copyExampleImage();
                return dir.length() > 0;
            } else {
                return false;
            }

        } catch (final Exception e) {
            e.printStackTrace();
        }
        return false;
    }

}