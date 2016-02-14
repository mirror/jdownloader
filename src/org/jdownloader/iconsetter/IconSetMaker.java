package org.jdownloader.iconsetter;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.Application;
import org.appwork.utils.FileHandler;
import org.appwork.utils.Files;
import org.appwork.utils.Hash;
import org.appwork.utils.logging2.extmanager.LoggerFactory;
import org.appwork.utils.swing.EDTHelper;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.iconsetter.gui.IconSetterGui;
import org.jdownloader.startup.Main;

import com.kitfox.svg.SVGException;

import jd.gui.swing.laf.LookAndFeelController;

public class IconSetMaker {
    public static File WORKSPACE;
    public static File THEMES;

    static {
        try {
            WORKSPACE = new File(Main.class.getResource("/").toURI()).getParentFile();
        } catch (URISyntaxException e) {
            throw new WTFException(e);
        }
        if (WORKSPACE.getName().equals("JDownloaderUpdater")) {
            WORKSPACE = new File(WORKSPACE.getParentFile(), "JDownloader");
        }
        THEMES = new File(WORKSPACE, "themes");
    }

    public static void main(String[] args) throws Throwable {
        Application.setApplication(".jd_home");
        LookAndFeelController.getInstance().init();

        new IconSetMaker().run();
    }

    private IconSetterGui gui;
    // private ArrayList<IconResource> list;
    private ResourceSet   standardSet;

    public ResourceSet getStandardSet() {
        return standardSet;
    }

    private void run() throws Throwable {
        this.gui = initGui();
        scanThemes();
        gui.onThemesScanned();

        // SimpleHTTP br = new SimpleHTTP();
        // for (IconResource ir : list) {
        // String xml = br.getPage(new URL("https://api.icons8.com/api/iconsets/search?term=" + Encoding.urlEncode(ir.getTags()) +
        // "&amount=15"));
        // String[] icons = new Regex(xml, "(<icon .*?</icon>)").getColumn(0);
        // for (String icon : icons) {
        // String id = new Regex(icon, "id=\"(\\d+)").getMatch(0);
        // String name = new Regex(icon, "name=\"([^\"]+)").getMatch(0);
        // String platform = new Regex(icon, "platform=\"([^\"]+)").getMatch(0);
        // String url = new Regex(icon, "url=\"([^\"]+)").getMatch(0);
        // String svg = new Regex(icon, "<svg>([^<]+)").getMatch(0);
        // String svgXML = new String(Base64.decode(svg), "ASCII");
        // SVGUniverse universe = new SVGUniverse();
        // File ff = Application.getResource("svg/" + id + "_" + name + ".svg");
        // ff.getParentFile().mkdirs();
        // if (!ff.exists()) {
        // IO.writeToFile(ff, Base64.decode(svg));
        // }
        // URI uri = universe.loadSVG(new ByteArrayInputStream(Base64.decode(svg)), name);
        //
        // SVGDiagram diagram = universe.getDiagram(uri);
        // SVGElement root = diagram.getRoot();
        // // set color
        // root.addAttribute("fill", AnimationElement.AT_CSS, "#ff0000");
        //
        // diagram.updateTime(0d);
        // diagram.setIgnoringClipHeuristic(true);
        //
        // BufferedImage bi = new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB);
        // Graphics2D g = bi.createGraphics();
        // g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // int x = 0;
        // int y = 0;
        //
        // int width = 32;
        // int height = 32;
        // g.translate(x, y);
        // final Rectangle2D.Double rect = new Rectangle2D.Double();
        // diagram.getViewRect(rect);
        // AffineTransform scaleXform = new AffineTransform();
        // scaleXform.setToScale(width / rect.width, height / rect.height);
        //
        // AffineTransform oldXform = g.getTransform();
        // g.transform(scaleXform);
        //
        // diagram.render(g);
        //
        // g.setTransform(oldXform);
        //
        // g.translate(-x, -y);
        //
        // // diagram.render(g);
        // g.dispose();
        // Dialog.getInstance().showConfirmDialog(0, name, ir.getPath() + " - " + platform, new ImageIcon(bi), null, null);
        // }
        // System.out.println();
        // }
    }

    private IconSetterGui initGui() {
        return new EDTHelper<IconSetterGui>() {

            @Override
            public IconSetterGui edtRun() {
                return new IconSetterGui(IconSetMaker.this);
            }
        }.getReturnValue();
    }

    private void scanThemes() throws MalformedURLException, IOException, InterruptedException, SVGException, DialogClosedException, DialogCanceledException, URISyntaxException {
        final HashMap<String, IconResource> map = new HashMap<String, IconResource>();

        final File jdHome = Application.getResource("themes");
        if (jdHome.exists()) {
            Files.deleteRecursiv(jdHome);
        }

        File standard = new File(THEMES, "themes/standard");
        standardSet = scanTheme(map, standard);

        for (File folder : new File(THEMES, "themes/").listFiles(new FileFilter() {

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

    }

    private ArrayList<ResourceSet> resources = new ArrayList<ResourceSet>();
    private ResourceSet            resourceSet;

    private ResourceSet scanTheme(final HashMap<String, IconResource> map, final File theme) throws MalformedURLException, IOException, InterruptedException, SVGException, DialogClosedException, DialogCanceledException {

        final ResourceSet set;
        resources.add(set = new ResourceSet(theme.getName()));
        Files.walkThroughStructure(new FileHandler<RuntimeException>() {

            @Override
            public void intro(File f) throws RuntimeException {
            }

            @Override
            public boolean onFile(File f, int depths) throws RuntimeException {
                if (f.getName().toLowerCase(Locale.ENGLISH).endsWith(".png") || f.getName().toLowerCase(Locale.ENGLISH).endsWith(".svg")) {
                    IconResource ir;
                    String rel = Files.getRelativePath(theme, f);
                    rel = rel.replaceAll("\\.\\w\\w\\w$", "");
                    if (rel.startsWith("org/jdownloader/images/uploaded/")) {
                        return true;
                    }
                    ir = map.get(rel);
                    String nameTags = null;
                    try {
                        File icon8 = new File(f.getAbsolutePath() + ".icons8");
                        if (icon8.exists()) {
                            Properties p = new Properties();
                            FileInputStream fis = new FileInputStream(icon8);
                            try {
                                p.load(fis);
                            } finally {
                                fis.close();
                            }
                            nameTags = p.getProperty("name");
                        }
                    } catch (Throwable e) {
                        LoggerFactory.getDefaultLogger().log(e);
                    }
                    if (ir == null) {
                        ir = new IconResource(rel, Hash.getMD5(f));
                        if (nameTags != null) {
                            ir.addTags(nameTags);
                        }
                    } else {
                        if (nameTags != null) {
                            ir.addTags(nameTags);
                        }
                        if (Hash.getMD5(f).equals(ir.getStandardMd5())) {
                            // System.out.println("Removed Standart File: "+f);
                            // f.delete();
                            return true;
                        }
                    }
                    ir.addSet(theme.getName());
                    set.add(ir);
                    map.put(rel, ir);
                }
                return true;
            }

            @Override
            public void outro(File f) throws RuntimeException {
            }
        }, theme);
        return set;
    }

    public List<ResourceSet> getResourceSets() {
        return resources;
    }

    public void edit(ResourceSet set) {
        this.resourceSet = set;
        gui.onEditTheme(set);
    }

    public ResourceSet createNewResourceSet(String text) {
        ResourceSet set;
        resources.add(set = new ResourceSet(text));

        new File(THEMES, "themes/" + text + "/org/jdownloader/images/").mkdirs();

        return set;
    }

    public ResourceSet getResoureSet() {
        return resourceSet;
    }

}
