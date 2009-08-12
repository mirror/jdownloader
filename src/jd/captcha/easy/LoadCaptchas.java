package jd.captcha.easy;

import jd.utils.JDUtilities;
import jd.http.Browser;
import jd.captcha.gui.ImageComponent;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.awt.GridLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import jd.nutils.JDImage;
import jd.nutils.Screen;
import jd.gui.swing.dialog.ProgressDialog;
import jd.gui.userio.DummyFrame;

public class LoadCaptchas {
    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

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

    private static void openDir(String dir) {
        if (JOptionPane.showConfirmDialog(null, "Captcha Ordner:" + dir + " jetzt öffnen?") == JOptionPane.YES_OPTION) JDUtilities.openExplorer(new File(dir));

    }

    public static boolean load() {
        return load(null, true);
    }

    public static boolean load(String host) {
        return load(host, false);
    }

    public static boolean load(final String host2, final boolean opendir) {

        try {

            final String link = JOptionPane.showInputDialog("Bitte Link eingeben:");
            final int menge = Integer.parseInt(JOptionPane.showInputDialog("Wieviele Captchas sollen heruntergeladen werden:", "500"));
            final ProgressDialog pd = new ProgressDialog(DummyFrame.getDialogParent(), "load captchas please wait", null, false, true);
            pd.setMaximum(menge);
            final JDialog dialog = new JDialog(DummyFrame.getDialogParent());
            dialog.setLocation(Screen.getCenterOfComponent(DummyFrame.getDialogParent(), dialog));

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
                                pd.setValue(k);
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

            dialog.setTitle("click on the captcha");
            final String[] images = getImages(br);
            final File[] files = new File[images.length];
            JPanel panel = new JPanel(new GridLayout(images.length / 3, 3));

            dialog.addWindowListener(new WindowListener() {

                public void windowActivated(WindowEvent e) {
                    // TODO Auto-generated method stub

                }

                public void windowClosed(WindowEvent e) {
                    // TODO Auto-generated method stub

                }

                public void windowClosing(WindowEvent e) {
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
            final Thread[] jb = new Thread[images.length];

            for (int j = 0; j < images.length; j++) {
                final int i = j;
                jb[i] = new Thread(new Runnable() {

                    public void run() {
                        String ft = ".jpg";
                        if (images[i].toLowerCase().contains("png"))
                            ft = ".png";
                        else if (images[i].toLowerCase().contains("gif"))
                            ft = ".gif";
                        else {
                            try {
                                br.getPage(images[i]);
                                String ct2 = br.getHttpConnection().getContentType().toLowerCase();
                                if (ct2 != null && ct2.contains("image")) {
                                    if (ct2.equals("image/jpeg"))
                                        ft = ".jpg";
                                    else {
                                        ft = ct2.replaceFirst("image/", ".");
                                    }
                                }
                            } catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }

                        }
                        final String filetype = ft;
                        final File f = new File(dir, System.currentTimeMillis() + filetype);
                        files[i] = f;
                        try {
                            br.getDownload(f, images[i]);
                        } catch (Exception e) {
                        }
                        synchronized (jb[i]) {
                            jb[i].notify();
                        }
                    }
                });
                jb[i].start();
            }
            for (Thread thread : jb) {
                while (thread.isAlive()) {
                    synchronized (thread) {
                        thread.wait(3000);
                    }
                }
            }
            for (int j = 0; j < images.length; j++) {
                final int i = j;
                final File f = files[i];
                if (!f.exists() || f.length() < 100) continue;
                String ft = ".jpg";
                if (f.getName().toLowerCase().contains("png"))
                    ft = ".png";
                else if (f.getName().toLowerCase().contains("gif")) ft = ".gif";
                final String filetype = ft;
                BufferedImage captchaImage = JDImage.getImage(f);
                int area = captchaImage.getHeight(null) * captchaImage.getHeight(null);
                if (area < 50 || area > 50000 || captchaImage.getHeight(null) > 400 || captchaImage.getWidth(null) > 400) {
                    f.delete();

                    continue;
                }
                ImageComponent ic0 = new ImageComponent(JDImage.getScaledImage(captchaImage, 50, 50));

                panel.add(ic0);
                MouseListener ml = new MouseListener() {

                    public void mouseClicked(MouseEvent e) {
                        dialog.dispose();

                        Runnable runnable = new Runnable() {
                            public void run() {
                                try {
                                    for (File file : files) {
                                        if (!file.equals(f)) file.delete();
                                    }
                                    Browser brss = br.cloneBrowser();

                                    brss.getPage(link);

                                    final String[] im = getImages(brss);
                                    File f2 = new File(dir + System.currentTimeMillis() + filetype);
                                    br.getDownload(f2, im[i]);

                                    if (im[i].equals(images[i])) {
                                        for (int k = 0; k < menge - 2; k++) {
                                            final Browser brs = br.cloneBrowser();
                                            try {
                                                f2 = new File(dir + System.currentTimeMillis() + filetype);
                                                brs.getDownload(f2, images[i]);

                                            } catch (Exception ev) {
                                                // TODO Auto-generated
                                                // catch
                                                // block
                                                ev.printStackTrace();
                                            }
                                            pd.setValue(k);
                                        }
                                    } else {
                                        for (int k = 0; k < menge - 2; k++) {

                                            final Browser brs = br.cloneBrowser();

                                            brs.getPage(link);

                                            try {
                                                f2 = new File(dir + System.currentTimeMillis() + filetype);

                                                brs.getDownload(f2, getImages(brs)[i]);

                                            } catch (Exception ev) {
                                                // TODO Auto-generated
                                                // catch
                                                // block
                                                ev.printStackTrace();
                                            }
                                            pd.setValue(k);

                                        }
                                    }
                                } catch (Exception e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }

                                pd.dispose();
                            }
                        };
                        Thread th = new Thread(runnable);
                        th.start();
                        pd.setThread(th);
                        pd.setVisible(true);
                        synchronized (dialog) {
                            dialog.notify();
                        }
                    }

                    public void mouseEntered(MouseEvent e) {
                    }

                    public void mouseExited(MouseEvent e) {
                    }

                    public void mousePressed(MouseEvent e) {
                    }

                    public void mouseReleased(MouseEvent e) {
                    }
                };
                ic0.addMouseListener(ml);
            }
            dialog.add(new JScrollPane(panel));

            dialog.pack();
            dialog.setModal(true);
            dialog.setVisible(true);
            synchronized (dialog) {
                dialog.wait();
            }
            if (opendir) openDir(dir);
            return dir.length() > 0;

        } catch (Exception e) {
            // TODO: handle exception
        }
        return false;

    }

    public static void main(String[] args) throws Exception {
        LoadCaptchas.load();
        System.exit(0);
    }
}
