package jd.captcha.easy.load;

import java.io.File;
import java.net.URI;

import jd.nutils.JDHash;

import jd.nutils.io.JDIO;
import jd.utils.JDUtilities;
import jd.http.Browser;
import jd.parser.html.HTMLParser;

/**
 * Diese klasse speichert Bildinformationen wie die Form die verwendet wurde und
 * die Bildposition
 * 
 * @author dwd
 * 
 */
public class LoadImage {
    /**
     * Adresse die als erstes geladen wird
     */
    public String baseUrl;
    /**
     * followUrl unterurl die verwendet werden soll
     */
    protected int followUrl = -1;
    /**
     * Bildadresse
     */
    public transient String imageUrl;
    /**
     * Formposition -1 == keiner Form folgen
     */
    protected int form = -1;
    /**
     * zwischenspeicher für den DateiType
     */
    protected String fileType = null;
    /**
     * Bildposition
     */
    protected int location = 0;
    /**
     * Browser mit dem das Bild runtergeladen wurde
     */
    public transient Browser br;
    /**
     * datei in dem das Bild nach dem laden gespeichert wurde
     */
    public transient File file;
    public transient boolean clearCookies = true;

    public LoadImage(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public LoadImage() {
        this.br = new Browser();
        br.setFollowRedirects(true);
    }

    public LoadImage(LoadInfo loadInfo) {
        this.baseUrl = loadInfo.link;
        this.br = new Browser();
        br.setFollowRedirects(true);
    }

    public LoadImage(LoadInfo loadInfo, String imageUrl, Browser br) {
        this.baseUrl = loadInfo.link;
        this.imageUrl = imageUrl;
        this.br = br.cloneBrowser();
    }

    public int getFollowUrl() {
        return followUrl;
    }

    public void setFollowUrl(int followUrl) {
        this.followUrl = followUrl;
    }

    public int getForm() {
        return form;
    }

    public void setForm(int form) {
        this.form = form;
    }

    public int getLocation() {
        return location;
    }

    public void setLocation(int location) {
        this.location = location;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

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

    /**
     * läd das Bild direkt in den vorgegebenen Ordner
     * 
     * @param destination
     */
    public boolean directCaptchaLoad(String destination) {
        file = new File(destination, System.currentTimeMillis() + getFileType());
        try {
            br.cloneBrowser().getDownload(file, imageUrl);
            File dest = new File(destination, JDHash.getMD5(file)+ getFileType());
            if(dest.exists())
            {file.delete(); 
            return false;}
            file.renameTo(dest);
            file=dest;
            return true;
        } catch (Exception e) {
        }
        return false;
    }

    /**
     * ruft die Seite erneut auf und folgt den Forms um dann am ende das Bild zu
     * laden
     * 
     * @param host
     * @param loadInfo
     * @throws Exception
     */
    public LoadImage load(String host) throws Exception {
        if (host == null) host = new URI(baseUrl).getHost();
        String destination = JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath() + "/captchas/" + host + "/";
        new File(destination).mkdir();
        br.clearCookies(baseUrl);
        br.getPage(baseUrl);
        if (followUrl != -1) {
            String[] links = HTMLParser.getHttpLinks(br.toString(), br.getURL());
            br.getPage(links[followUrl]);
        }

        if (form != -1) {
            br.submitForm(LoadCaptchas.getForms(br)[form]);
        }
        imageUrl = LoadCaptchas.getImages(br)[location];
        directCaptchaLoad(destination);
        return this;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    /**
     * Dateitype eines Bildes .jpg wenn er nicht erkannt wird
     * 
     * @return
     */
    public String getFileType() {
        if (fileType != null) return fileType;
        if (imageUrl == null) return ".jpg";
        fileType = ".jpg";
        if (imageUrl.toLowerCase().contains("\\.png"))
            fileType = ".png";
        else if (imageUrl.toLowerCase().contains("\\.png"))
            fileType = ".gif";
        else {
            try {
                Browser bc = br.cloneBrowser();
                bc.getPage(imageUrl);
                String ct2 = bc.getHttpConnection().getContentType().toLowerCase();
                if (ct2 != null && ct2.contains("image")) {
                    if (ct2.equals("image/jpeg"))
                        fileType = ".jpg";
                    else {
                        fileType = ct2.replaceFirst("image/", ".");
                    }
                }
            } catch (Exception e) {
            }

        }
        return fileType;
    }

    public static String getFileType(String imageUrl, String conentType) {
        String fileType = ".jpg";
        if (imageUrl == null) return ".jpg";
        fileType = ".jpg";
        if (imageUrl.toLowerCase().contains("\\.png"))
            fileType = ".png";
        else if (imageUrl.toLowerCase().contains("\\.png"))
            fileType = ".gif";
        else {
            if (conentType != null && conentType.contains("image")) {
                if (conentType.equals("image/jpeg"))
                    fileType = ".jpg";
                else {
                    fileType = conentType.replaceFirst("image/", ".");
                }
            }

        }
        return fileType;
    }

    @Override
    public String toString() {
        return imageUrl;
    }

    public String toLowerCase() {
        return toString().toLowerCase();
    }

    /**
     * läd einen Vector<CPoint> aus eine XML Datei (methodedir/CPoints.xml)
     * 
     * @param file
     * @return
     */
    public static LoadImage loadFile(File file) {
        if (file.exists()) { return (LoadImage) JDIO.loadObject(null, file, true); }
        return null;
    }

    /**
     * läd ein LoadImage aus eine XML Datei (methodedir/CPoints.xml)
     * 
     * @param file
     * @return
     */
    public static LoadImage loadFile(String host) {
        File file = new File(JDUtilities.getJDHomeDirectoryFromEnvironment() + "/" + JDUtilities.getJACMethodsDirectory() + host + "/LoadImage.xml");
        return loadFile(file);
    }

    /**
     * Speichert ein LoadImage in eine XML Datei
     * 
     * @param cPoints
     * @param file
     */
    public static void save(LoadImage li, File file) {
        file.getParentFile().mkdirs();
        System.out.println("LoadImage has beens saved under: " + file);
        JDIO.saveObject(null, li, file, null, null, true);
    }

    /**
     * Speichert ein LoadImage in eine XML Datei (methodedir/LoadImage.xml)
     * 
     * @param cPoints
     * @param file
     */
    public static void save(LoadImage selectedImage, String host) {
        File file = new File(JDUtilities.getJDHomeDirectoryFromEnvironment() + "/" + JDUtilities.getJACMethodsDirectory() + host + "/LoadImage.xml");
        save(selectedImage, file);
    }
}