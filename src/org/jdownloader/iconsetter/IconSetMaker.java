package org.jdownloader.iconsetter;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map.Entry;

import javax.swing.ImageIcon;

import org.appwork.utils.Application;
import org.appwork.utils.FileHandler;
import org.appwork.utils.Files;
import org.appwork.utils.Hash;
import org.appwork.utils.IO;
import org.appwork.utils.Regex;
import org.appwork.utils.net.SimpleHTTP;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;

import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGElement;
import com.kitfox.svg.SVGException;
import com.kitfox.svg.SVGUniverse;
import com.kitfox.svg.animation.AnimationElement;

import jd.nutils.encoding.Base64;
import jd.nutils.encoding.Encoding;

public class IconSetMaker {

    public static void main(String[] args) throws MalformedURLException, IOException, InterruptedException, SVGException, DialogClosedException, DialogCanceledException {
        Application.setApplication(".jd_home");
        final HashMap<String, IconResource> map = new HashMap<String, IconResource>();

        System.setProperty("copysvn", "true");
        org.jdownloader.startup.Main.copySVNtoHome();
        final File standard = Application.getResource("themes/standard");
        scanTheme(map, standard);
        for (File folder : Application.getResource("themes/").listFiles(new FileFilter() {

            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        })) {
            if (folder.equals(standard)) {
                continue;
            }
            scanTheme(map, folder);
        }

        ArrayList<IconResource> list = new ArrayList<IconResource>();
        for (Entry<String, IconResource> es : map.entrySet()) {
            list.add(es.getValue());

        }
        Collections.sort(list, new Comparator<IconResource>() {

            @Override
            public int compare(IconResource o1, IconResource o2) {
                return o1.getPath().compareTo(o2.getPath());
            }
        });

        SimpleHTTP br = new SimpleHTTP();
        for (IconResource ir : list) {
            String xml = br.getPage(new URL("https://api.icons8.com/api/iconsets/search?term=" + Encoding.urlEncode(ir.getTags()) + "&amount=15"));
            String[] icons = new Regex(xml, "(<icon .*?</icon>)").getColumn(0);
            for (String icon : icons) {
                String id = new Regex(icon, "id=\"(\\d+)").getMatch(0);
                String name = new Regex(icon, "name=\"([^\"]+)").getMatch(0);
                String platform = new Regex(icon, "platform=\"([^\"]+)").getMatch(0);
                String url = new Regex(icon, "url=\"([^\"]+)").getMatch(0);
                String svg = new Regex(icon, "<svg>([^<]+)").getMatch(0);
                String svgXML = new String(Base64.decode(svg), "ASCII");
                SVGUniverse universe = new SVGUniverse();
                File ff = Application.getResource("svg/" + id + "_" + name + ".svg");
                ff.getParentFile().mkdirs();
                if (!ff.exists()) {
                    IO.writeToFile(ff, Base64.decode(svg));
                }
                URI uri = universe.loadSVG(new ByteArrayInputStream(Base64.decode(svg)), name);

                SVGDiagram diagram = universe.getDiagram(uri);
                SVGElement root = diagram.getRoot();
                // set color
                root.addAttribute("fill", AnimationElement.AT_CSS, "#ff0000");

                diagram.updateTime(0d);
                diagram.setIgnoringClipHeuristic(true);

                BufferedImage bi = new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = bi.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int x = 0;
                int y = 0;

                int width = 32;
                int height = 32;
                g.translate(x, y);
                final Rectangle2D.Double rect = new Rectangle2D.Double();
                diagram.getViewRect(rect);
                AffineTransform scaleXform = new AffineTransform();
                scaleXform.setToScale(width / rect.width, height / rect.height);

                AffineTransform oldXform = g.getTransform();
                g.transform(scaleXform);

                diagram.render(g);

                g.setTransform(oldXform);

                g.translate(-x, -y);

                // diagram.render(g);
                g.dispose();
                Dialog.getInstance().showConfirmDialog(0, name, ir.getPath() + " - " + platform, new ImageIcon(bi), null, null);
            }
            System.out.println();
        }
    }

    private static void scanTheme(final HashMap<String, IconResource> map, final File theme) throws MalformedURLException, IOException, InterruptedException, SVGException, DialogClosedException, DialogCanceledException {
        Files.walkThroughStructure(new FileHandler<RuntimeException>() {

            @Override
            public void intro(File f) throws RuntimeException {
            }

            @Override
            public boolean onFile(File f, int depths) throws RuntimeException {
                if (f.getName().toLowerCase(Locale.ENGLISH).endsWith(".png") || f.getName().toLowerCase(Locale.ENGLISH).endsWith(".svg")) {
                    IconResource ir;
                    String rel = Files.getRelativePath(theme, f);
                    ir = map.get(rel);
                    if (ir == null) {
                        ir = new IconResource(rel, Hash.getMD5(f));
                    } else {
                        if (Hash.getMD5(f).equals(ir.getStandardMd5())) {
                            // System.out.println("Removed Standart File: "+f);
                            // f.delete();
                            return true;
                        }
                    }
                    ir.addSet(theme.getName());
                    map.put(rel, ir);
                }
                return true;
            }

            @Override
            public void outro(File f) throws RuntimeException {
            }
        }, theme);

    }
}
