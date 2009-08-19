package jd.captcha.easy;

import jd.utils.JDUtilities;
import jd.http.Browser;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.*;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import jd.parser.html.InputField;

import jd.parser.html.Form;

import jd.gui.swing.jdgui.events.EDTEventQueue;
import jd.gui.swing.laf.LookAndFeelController;
import jd.gui.swing.components.JDTextField;
import jd.gui.swing.GuiRunnable;
import jd.utils.locale.JDL;
import jd.nutils.JDImage;
import jd.nutils.Screen;
import jd.gui.swing.dialog.ProgressDialog;
import jd.gui.userio.DummyFrame;

public class LoadCaptchas {
    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    private static Form[] getForms(Browser br) {
        ArrayList<Form> retForms = new ArrayList<Form>();
        Form[] forms = br.getForms();
        for (Form form : forms) {
            ArrayList<InputField> fi = form.getInputFieldsByType("submit");
            if (fi.size() > 1) {
                for (int i = 1; i < fi.size(); i++) {
                    Form fo = new Form(form.getHtmlCode());
                    fo.getInputFields().remove(fo.getInputFieldsByType("submit").get(i));
                    if (!retForms.contains(fo)) retForms.add(fo);
                }
                form.getInputFields().remove(fi.get(0));
            }
            if (!retForms.contains(form)) retForms.add(form);
        }
        return retForms.toArray(new Form[] {});
    }

    private static String[] getImages(Browser br) throws Exception {
        ArrayList<String> ret = new ArrayList<String>();
        Pattern[] basePattern = new Pattern[] { Pattern.compile("(?is)<[ ]?input[^>]*?type=.?image[^>]*?src=['|\"]?([^>\\s'\"]*)['|\">\\s]", Pattern.CASE_INSENSITIVE), Pattern.compile("(?is)<[ ]?IMG[^>]*?src=['|\"]?([^>\\s'\"]*)['|\">\\s]", Pattern.CASE_INSENSITIVE) };
        for (Pattern element : basePattern) {
            Matcher m = element.matcher(br.toString());
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
                    if (!ret.contains(src)) ret.add(src);
                } catch (Exception e) {
                    // TODO: handle exception
                }

            }
        }

        return ret.toArray(new String[] {});
    }

    private static void openDir(final String dir) {
        new GuiRunnable<Object>() {
            // @Override
            public Object runSave() {
                if (JOptionPane.showConfirmDialog(null, "Captcha Ordner:" + dir + " jetzt öffnen?") == JOptionPane.YES_OPTION) JDUtilities.openExplorer(new File(dir));

                return null;
            }
        }.waitForEDT();

    }

    public static boolean load() {
        return load(null, true);
    }

    public static boolean load(String host) {
        return load(host, false);
    }

    public static boolean load(final String host2, final boolean opendir) {

        try {

            final JDialog dialog = new GuiRunnable<JDialog>() {
                // @Override
                public JDialog runSave() {
                    return new JDialog(DummyFrame.getDialogParent());
                }
            }.getReturnValue();
            dialog.setModal(true);
            final JPanel p = new GuiRunnable<JPanel>() {
                // @Override
                public JPanel runSave() {
                    JPanel ret = new JPanel(new GridLayout(3, 2));
                    ret.add(new JLabel(JDL.L("easycaptcha.loadcaptchas.link", "Link") + ":"));
                    return ret;

                }
            }.getReturnValue();

            final JDTextField tfl = new GuiRunnable<JDTextField>() {
                // @Override
                public JDTextField runSave() {
                    return new JDTextField();
                }
            }.getReturnValue();
            tfl.setBorder(BorderFactory.createEtchedBorder());

            p.add(tfl);
            JSpinner sm = new GuiRunnable<JSpinner>() {
                // @Override
                public JSpinner runSave() {
                    p.add(new JLabel(JDL.L("easycaptcha.loadcaptchas.howmuch", "How much captchas you need") + ":"));

                    return new JSpinner(new SpinnerNumberModel(100, 1, 4000, 1));
                }
            }.getReturnValue();
            p.add(sm);
            JButton ok = new GuiRunnable<JButton>() {
                // @Override
                public JButton runSave() {
                    return new JButton(JDL.L("gui.btn_ok", "OK"));
                }
            }.getReturnValue();
            ok.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    dialog.remove(p);
                    dialog.validate();
                    dialog.setVisible(false);
                }
            });
            p.add(ok);
            WindowListener l = new WindowListener() {
                public void windowActivated(WindowEvent e) {
                }

                public void windowClosed(WindowEvent e) {

                }

                public void windowClosing(WindowEvent e) {
                    tfl.setText("");
                    dialog.dispose();
                }

                public void windowDeactivated(WindowEvent e) {
                }

                public void windowDeiconified(WindowEvent e) {
                }

                public void windowIconified(WindowEvent e) {
                }

                public void windowOpened(WindowEvent e) {
                }
            };
            JButton cancel = new GuiRunnable<JButton>() {
                // @Override
                public JButton runSave() {
                    return new JButton(JDL.L("gui.btn_cancel", "Cancel"));
                }
            }.getReturnValue();
            cancel.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    tfl.setText("");
                    dialog.dispose();
                }
            });
            p.add(cancel);

            dialog.addWindowListener(l);
            dialog.add(p);
            new GuiRunnable<Object>() {
                // @Override
                public Object runSave() {
                    dialog.setLocation(Screen.getCenterOfComponent(DummyFrame.getDialogParent(), dialog));
                    dialog.pack();
                    dialog.setVisible(true);

                    return null;
                }
            }.waitForEDT();
            final String link = tfl.getText();
            if (link == null || link.matches("\\s*")) return false;
            final int menge = (Integer) sm.getValue();
            final ProgressDialog pd = new GuiRunnable<ProgressDialog>() {
                // @Override
                public ProgressDialog runSave() {

                    return new ProgressDialog(DummyFrame.getDialogParent(), JDL.L("easycaptcha.loadcaptchas.loadimages", "load images please wait"), null, false, true);
                }
            }.getReturnValue();
            final Browser br = new Browser();
            br.getPage(link);
            String host = host2;
            if (host == null) {
                host = br.getHost().toLowerCase();
                if (host.matches(".*\\..*\\..*")) host = host.substring(host.indexOf('.') + 1);
            }
            final String dir = JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath() + "/captchas/" + host + "/";
            new File(dir).mkdir();
            final String ct = br.getHttpConnection().getContentType().toLowerCase();
            if (ct != null && ct.contains("image")) {
                dialog.dispose();
                Runnable runnable = new Runnable() {
                    public void run() {
                        for (int k = 0; k < menge; k++) {
                            try {
                                String ft = ".jpg";
                                if (ct.equals("image/jpeg"))
                                    ft = ".jpg";
                                else {
                                    ft = ct.replaceFirst("image/", ".");
                                }
                                File f2 = new File(dir + System.currentTimeMillis() + ft);
                                br.getDownload(f2, link);
                                final int c = k;
                                new GuiRunnable<Object>() {
                                    public Object runSave() {
                                        pd.setValue(c);
                                        return null;
                                    }
                                }.waitForEDT();
                            } catch (Exception ev) {
                                // TODO Auto-generated catch block
                                ev.printStackTrace();
                            }

                        }
                        pd.dispose();
                    }
                };
                Thread th = new Thread(runnable);
                th.start();
                pd.setThread(th);
                pd.setVisible(true);
                if (opendir) openDir(dir);
                return true;
            }

            dialog.setTitle(JDL.L("easycaptcha.loadcaptchas.clickoncaptcha", "click on the captcha"));

            final ArrayList<LoadImage> images = new ArrayList<LoadImage>();
            String[] imagea = getImages(br);
            for (int i = 0; i < imagea.length; i++) {
                LoadImage li = new LoadImage();
                li.form = -1;
                li.location = i;
                li.br = br;
                li.imageUrl = imagea[i];
                images.add(li);
            }
            Form[] forms = getForms(br);
            for (int i = 0; i < forms.length; i++) {
                Form form = forms[i];
                Browser brc = br.cloneBrowser();
                brc.submitForm(form);
                imagea = getImages(brc);
                for (int b = 0; b < imagea.length; b++) {

                    LoadImage li = new LoadImage();
                    li.form = i;
                    li.location = b;
                    li.br = brc;
                    li.imageUrl = imagea[b];
                    if (images.contains(li)) continue;
                    images.add(li);
                }
            }
            final File[] files = new File[images.size()];
            dialog.removeWindowListener(l);
            dialog.addWindowListener(new WindowListener() {

                public void windowActivated(WindowEvent e) {
                    // TODO Auto-generated method stub

                }

                public void windowClosed(WindowEvent e) {

                }

                public void windowClosing(WindowEvent e) {
                    for (File file : files) {
                        file.delete();
                    }
                    dialog.dispose();
                }

                public void windowDeactivated(WindowEvent e) {
                    // TODO Auto-generated method stub

                }

                public void windowDeiconified(WindowEvent e) {
                    // TODO Auto-generated method stub

                }

                public void windowIconified(WindowEvent e) {
                    // TODO Auto-generated method stub

                }

                public void windowOpened(WindowEvent e) {
                    // TODO Auto-generated method stub

                }
            });
            final Thread th = new Thread(new Runnable() {
                public void run() {
                    final Thread[] jb = new Thread[images.size()];
                    for (int j = 0; j < images.size(); j++) {
                        final int i = j;
                        jb[i] = new Thread(new Runnable() {

                            public void run() {
                                LoadImage image = images.get(i);
                                String ft = ".jpg";
                                if (image.toLowerCase().contains("png"))
                                    ft = ".png";
                                else if (image.toLowerCase().contains("gif"))
                                    ft = ".gif";
                                else {
                                    try {
                                        br.getPage(image.imageUrl);
                                        String ct2 = br.getHttpConnection().getContentType().toLowerCase();
                                        if (ct2 != null && ct2.contains("image")) {
                                            if (ct2.equals("image/jpeg"))
                                                ft = ".jpg";
                                            else {
                                                ft = ct2.replaceFirst("image/", ".");
                                            }
                                        }
                                    } catch (Exception e) {
                                    }

                                }
                                final String filetype = ft;
                                final File f = new File(dir, System.currentTimeMillis() + filetype);
                                files[i] = f;
                                try {
                                    image.br.getDownload(f, image.imageUrl);
                                    image.file = f;

                                } catch (Exception e) {
                                }
                                synchronized (jb[i]) {
                                    jb[i].notify();
                                }
                            }
                        });
                        jb[i].start();
                    }
                    new GuiRunnable<Object>() {
                        public Object runSave() {
                            pd.setMaximum(images.size());
                            return null;
                        }
                    }.waitForEDT();

                    int c = 0;
                    for (Thread thread : jb) {
                        while (thread.isAlive()) {
                            synchronized (thread) {
                                try {
                                    thread.wait(3000);
                                } catch (InterruptedException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                            }
                        }
                        final int d = c++;
                        new GuiRunnable<Object>() {
                            public Object runSave() {
                                pd.setValue(d);
                                return null;
                            }
                        }.waitForEDT();
                    }
                    new GuiRunnable<Object>() {
                        public Object runSave() {
                            pd.setVisible(false);
                            return null;
                        }
                    }.waitForEDT();
                }
            });

            th.start();
            new GuiRunnable<Object>() {
                // @Override
                public Object runSave() {
                    pd.setThread(th);
                    pd.setVisible(true);

                    return null;
                }
            }.waitForEDT();

            final LoadImage ef = new LoadImage();
            final ArrayList<JButton> bts = new ArrayList<JButton>();
            for (int j = 0; j < images.size(); j++) {
                final LoadImage f = images.get(j);
                if (!f.file.exists() || f.file.length() < 100) continue;
                final BufferedImage captchaImage = JDImage.getImage(f.file);
                if(captchaImage==null)
                {
                    f.file.delete();
                    continue;
                }
                int area = captchaImage.getHeight(null) * captchaImage.getHeight(null);
                if (area < 50 || area > 50000 || captchaImage.getHeight(null) > 400 || captchaImage.getWidth(null) > 400 || captchaImage.getWidth(null) < 10 || captchaImage.getHeight(null) < 5) {
                    f.file.delete();
                    continue;
                }
                double faktor = Math.max((double) captchaImage.getWidth(null) / 100, (double) captchaImage.getHeight(null) / 100);
                final int width = (int) (captchaImage.getWidth(null) / faktor);
                final int height = (int) (captchaImage.getHeight(null) / faktor);
                JButton ic = new GuiRunnable<JButton>() {
                    // @Override
                    public JButton runSave() {
                        return new JButton(new ImageIcon(captchaImage.getScaledInstance(width, height, Image.SCALE_SMOOTH)));
                    }
                }.getReturnValue();
                ic.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        dialog.dispose();
                        ef.file = f.file;
                        ef.br = f.br;
                        ef.location = f.location;
                        ef.form = f.form;
                        ef.imageUrl = f.imageUrl;
                    }
                });
                bts.add(ic);
            }
            final JPanel panel = new GuiRunnable<JPanel>() {
                // @Override
                public JPanel runSave() {
                    return new JPanel(new GridLayout((int) Math.ceil(((double) bts.size()) / 5), 5));
                }
            }.getReturnValue();
            for (JButton button : bts) {
                panel.add(button);

            }
            new GuiRunnable<Object>() {
                // @Override
                public Object runSave() {
                    dialog.add(new JScrollPane(panel));

                    dialog.pack();
                    dialog.setLocation(Screen.getCenterOfComponent(DummyFrame.getDialogParent(), dialog));
                    dialog.setVisible(true);

                    return null;
                }
            }.waitForEDT();

            if (ef.file != null) {
                final Runnable runnable = new Runnable() {
                    public void run() {
                        try {
                            for (int j = 0; j < files.length; j++) {
                                File file = files[j];
                                if (!file.equals(ef.file)) file.delete();
                            }
                            String filetype = ".jpg";
                            if (ef.file.getName().toLowerCase().contains("png"))
                                filetype = ".png";
                            else if (ef.file.getName().toLowerCase().contains("gif")) filetype = ".gif";
                            Browser brss = br.cloneBrowser();

                            brss.getPage(link);
                            if (ef.form != -1) {
                                brss.submitForm(getForms(brss)[ef.form]);
                            }
                            final String[] im = getImages(brss);
                            File f2 = new File(dir + System.currentTimeMillis() + filetype);
                            brss.getDownload(f2, im[ef.location]);
                            if (im[ef.location].equals(ef.toString())) {
                                for (int k = 0; k < menge - 2; k++) {
                                    final Browser brs = brss.cloneBrowser();
                                    try {
                                        f2 = new File(dir + System.currentTimeMillis() + filetype);
                                        brs.getDownload(f2, ef.imageUrl);

                                    } catch (Exception ev) {
                                        // TODO Auto-generated
                                        // catch
                                        // block
                                        ev.printStackTrace();
                                    }
                                    final int d = k;
                                    new GuiRunnable<Object>() {
                                        public Object runSave() {
                                            pd.setValue(d);
                                            return null;
                                        }
                                    }.waitForEDT();
                                }
                            } else {
                                for (int k = 0; k < menge - 2; k++) {
                                    final Browser brs = br.cloneBrowser();

                                    brs.getPage(link);
                                    if (ef.form != -1) {
                                        brs.submitForm(getForms(brs)[ef.form]);
                                    }
                                    try {
                                        f2 = new File(dir + System.currentTimeMillis() + filetype);

                                        brs.getDownload(f2, getImages(brs)[ef.location]);

                                    } catch (Exception ev) {
                                        // TODO Auto-generated
                                        // catch
                                        // block
                                        ev.printStackTrace();
                                    }
                                    final int d = k;
                                    new GuiRunnable<Object>() {
                                        public Object runSave() {
                                            pd.setValue(d);
                                            return null;
                                        }
                                    }.waitForEDT();

                                }
                            }
                        } catch (Exception e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        new GuiRunnable<Object>() {
                            public Object runSave() {
                                pd.dispose();
                                return null;
                            }
                        }.waitForEDT();
                    }
                };
                new GuiRunnable<Object>() {
                    // @Override
                    public Object runSave() {
                        Thread th2 = new Thread(runnable);
                        th2.start();
                        pd.setMaximum(menge);
                        pd.setValue(1);
                        pd.setThread(th2);
                        pd.setVisible(true);

                        return null;
                    }
                }.waitForEDT();

            } else
                return false;
            if (opendir) openDir(dir);
            return dir.length() > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;

    }

    public static void main(String[] args) throws Exception {
        new GuiRunnable<Object>() {
            // @Override
            public Object runSave() {
                LookAndFeelController.setUIManager();
                Toolkit.getDefaultToolkit().getSystemEventQueue().push(new EDTEventQueue());

                LoadCaptchas.load();
                return null;
            }
        }.waitForEDT();

        System.exit(0);
    }
}

class LoadImage {
    public String imageUrl;
    public int form = -1;
    public int location = 0;
    public Browser br;
    public File file;

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof LoadImage) {
            String url = ((LoadImage) obj).imageUrl;
            if (imageUrl == url) return true;
            if (url == null) return false;
            return url.equals(imageUrl);
        }
        return false;
    }

    @Override
    public String toString() {
        return imageUrl;
    }

    public String toLowerCase() {
        return toString().toLowerCase();
    }
}