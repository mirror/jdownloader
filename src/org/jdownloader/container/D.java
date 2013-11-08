package org.jdownloader.container;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import jd.config.SubConfiguration;
import jd.controlling.linkcollector.LinknameCleaner;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.PackageInfo;
import jd.gui.UserIO;
import jd.http.Browser;
import jd.nutils.encoding.Base64;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.plugins.ContainerStatus;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginsC;
import jd.utils.JDHexUtils;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.utils.Hash;
import org.jdownloader.logging.LogController;
import org.jdownloader.updatev2.UpdateController;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class D extends PluginsC {

    private byte[]                  b3;
    private byte[]                  d;

    private HashMap<String, String> header;

    public D() {
        super("DLC", "file://.+\\.dlc", "$Revision$");
        b3 = new byte[] { 77, 69, 84, 65, 45, 73, 78, 70, 47, 74, 68, 79, 87, 78, 76, 79, 65, 46, 68, 83, 65 };
        d = new byte[] { -44, 47, 74, 116, 56, -46, 20, 9, 17, -53, 0, 8, -47, 121, 1, 75 };
        // kk = (byte[]) SubConfiguration.getConfig(new String(new byte[] { 97,
        // 112, 99, 107, 97, 103, 101, 1 })).getProperty(new String(new byte[] {
        // 97, 112, 99, 107, 97, 103, 101, 1 }), new byte[] { 112, 97, 99, 107,
        // 97, 103, 101 });
        //

    }

    // //@Override
    public ContainerStatus callDecryption(File d) {
        ContainerStatus cs = new ContainerStatus(d);
        cs.setStatus(ContainerStatus.STATUS_FAILED);

        String a = b(d).trim();
        // try {
        // if (a.trim().startsWith("<dlc>")) return e(d);
        // } catch (Exception e) {
        // Log.L.log(java.util.logging.Level.SEVERE,
        // "Exception occured", e);
        // cs.setStatusText("DLC2 failed");
        // return cs;
        // }
        String ee = "";
        if (a.length() < 100) {

            UserIO.getInstance().requestMessageDialog(JDL.L("sys.warning.dlcerror_nocld_small", "Invalid DLC: less than 100 bytes. This cannot be a DLC"));
            cs.setStatusText(JDL.L("sys.warning.dlcerror_nocld_small", "Invalid DLC: less than 100 bytes. This cannot be a DLC"));
            return cs;

        }

        String a0 = a.substring(a.length() - 88).trim();
        // Log.L.info(dlcString);
        // Log.L.info(key + " - " + key.length());
        a = a.substring(0, a.length() - 88).trim();

        if (Encoding.filterString(a, "1234567890QAWSEDRFTGZHUJIKOLPMNBVCXY+/=qaywsxedcrfvtgbzhnujmikolp\r\n").length() != a.length()) {

            UserIO.getInstance().requestMessageDialog(JDL.L("sys.warning.dlcerror_invalid", "It seems that your dlc is not valid."));
            cs.setStatusText(JDL.L("sys.warning.dlcerror_invalid", "It seems that your dlc is not valid."));
            return cs;
        }
        // Log.L.info(dlcString);
        java.util.List<URL> s1;

        try {
            s1 = new ArrayList<URL>();

            Collections.sort(s1, new Comparator<Object>() {
                public int compare(Object a, Object b) {
                    return (int) (Math.random() * 4.0 - 2.0);
                }

            });
            s1.add(0, new URL("http://service.jdownloader.org/dlcrypt/service.php"));
            Iterator<URL> it = s1.iterator();

            while (it.hasNext()) {
                String x = null;
                URL s2 = it.next();
                try {
                    String p;
                    if (k != null && k.length == 16) {
                        /* CHECK: we should always use new String (bytes,charset) to avoid issues with system charset and utf-8 */
                        p = new String(k);
                    } else {
                        x = cs(s2, a0);
                        if (x != null && x.trim().equals("2YVhzRFdjR2dDQy9JL25aVXFjQ1RPZ")) {
                            logger.severe("You recently opened to many DLCs. Please wait a few minutes.");
                            ee += "DLC Limit reached." + " ";
                            continue;

                        }
                        // Log.L.info("Dec key "+decodedKey);
                        if (x == null) {
                            logger.severe("DLC Error(key): " + s2);
                            ee += s2 + "" + JDL.L("sys.warning.dlcerror_key", "DLC: Key Fehler") + " ";

                            continue;
                        }
                        // JDUtilities.getController().fireControlEvent(ControlEvent.CONTROL_INTERACTION_CALL, this);

                        p = dsk(x);

                        // String test = dsk("8dEAMOh4EcaP8QgExlHZRNeCYL9EzB3cGJIdDG2prCE=");
                        p = Encoding.filterString(p);
                        if (p.length() != 16) {
                            logger.severe("DLC Error2(key): " + s2);
                            ee += s2 + "" + JDL.L("sys.warning.dlcerror_version", "DLC Fehler(1) ") + " ";

                            continue;
                        }
                        // Log.L.info("PLAIN KEY: " + plain);
                        // Log.L.info("PLAIN dlc: " + dlcString);
                        // plain="11b857cd4c4edd19";
                    }
                    String dds1 = d5(a, p);
                    dds1 = fds(dds1);
                    if (dds1 == null) {

                        logger.severe("DLC Error(xml): " + s2);
                        ee += s2 + "" + JDL.L("sys.warning.dlcerror_xml", "DLC: XML Fehler ") + " ";

                        continue;
                    }
                    dds1 = Encoding.filterString(dds1);
                    // Log.L.info("Decr " + decryptedDlcString);
                    pxs(dds1, d);
                    /* CHECK: we should always use getBytes("UTF-8") or with wanted charset, never system charset! */
                    k = p.getBytes();
                    cs.setStatus(ContainerStatus.STATUS_FINISHED);
                    return cs;

                } catch (NoSuchAlgorithmException e) {
                    ee += s2 + "" + JDL.L("sys.warning.dlcerror_java", "DLC: Outdated Javaversion ") + e.getMessage() + " ";

                } catch (MalformedURLException e) {
                    // Log.L.log(java.util.logging.Level.SEVERE,"Exception occured",e);
                    ee += s2 + "" + JDL.L("sys.warning.dlcerror_url", "DLC: URL Fehler: ") + e.getMessage() + " ";
                } catch (IOException e) {
                    // Log.L.log(java.util.logging.Level.SEVERE,"Exception occured",e);
                    ee += s2 + "" + JDL.L("sys.warning.dlcerror_io", "DLC: Server Fehler(offline? ") + e.getMessage() + " ";

                } catch (SAXException e) {
                    ee += s2 + "" + JDL.L("sys.warning.dlcerror_version", "DLC Fehler: Veraltete JD Version (1) ") + " ";

                    // Log.L.log(java.util.logging.Level.SEVERE,"Exception occured",e);
                } catch (ParserConfigurationException e) {
                    ee += s2 + "" + JDL.L("sys.warning.dlcerror_version", "DLC Fehler: Veraltete JD Version (2)") + " ";
                    // Log.L.log(java.util.logging.Level.SEVERE,"Exception occured",e);
                }

                catch (Exception e) {
                    e.printStackTrace();
                    ee += s2 + "" + JDL.L("sys.warning.dlcerror_unknown", "DLC Fehler: ") + e.getMessage() + " ";

                    logger.log(java.util.logging.Level.SEVERE, "Exception occured", e);
                }
            }
        } catch (Exception e) {
            ee += "URL Fehler " + e.getMessage() + "  ";

        }

        cs.setStatusText(JDL.L("sys.warning.dlcerror.server2", "Server claims: ") + " " + ee);
        return cs;
    }

    // private ContainerStatus e(File d4) throws SAXException, IOException,
    // ParserConfigurationException, NoSuchAlgorithmException,
    // NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException,
    // BadPaddingException, InstantiationException, IllegalAccessException {
    // ContainerStatus cs = new ContainerStatus(d4);
    // cs.setStatus(ContainerStatus.STATUS_FAILED);
    //
    // String ds = b(d4).trim();
    //
    // DocumentBuilderFactory f1;
    // InputSource s;
    // Document dc;
    // f1 = DocumentBuilderFactory.newInstance();
    // f1.setValidating(false);
    // f1.setIgnoringElementContentWhitespace(true);
    // f1.setIgnoringComments(true);
    // s = new InputSource(new StringReader(ds));
    // java.util.List<String> dss = new ArrayList<String>();
    // String k0, d0;
    // if (JDUtilities.getRunType() != JDUtilities.RUNTYPE_LOCAL_JARED) return
    // null;
    //
    // int d9 = 0;
    // byte[] bb = this.rjs(b3);
    //
    // for (byte b : bb) {
    // d9 += (b - d[d9]) + 1;
    // }
    // if (d9 != 16) {
    // Log.L.info("Wrong JD Version");
    // return null;
    // }
    // k0 = null;
    // d0 = null;
    // dc = f1.newDocumentBuilder().parse(s);
    // NodeList nds = dc.getFirstChild().getChildNodes();
    // for (int i = 0; i < nds.getLength(); i++) {
    // Node nd = nds.item(i);
    // if (nd.getNodeName().equalsIgnoreCase("services")) {
    // NodeList ss = nd.getChildNodes();
    // for (int sid = 0; sid < ss.getLength(); sid++) {
    // if (ss.item(sid).getNodeName().equalsIgnoreCase("service")) {
    // dss.add(ss.item(sid).getFirstChild().getNodeValue());
    // }
    // }
    // }
    // if (nd.getNodeName().equalsIgnoreCase("data")) {
    // k0 = nd.getAttributes().getNamedItem("k").getFirstChild().getNodeValue();
    // d0 = nd.getFirstChild().getNodeValue().trim();
    // break;
    //
    // }
    // }
    // String dk0 = null;
    // for (String s0 : dss) {
    // dk0 = cs2(s0, k0);
    // if (dk0 != null)
    // ;
    // }
    // if (dk0.contains("expired")) {
    // cs.setStatus(ContainerStatus.STATUS_FAILED);
    // cs.setStatusText("This DLC has Expired");
    // return cs;
    //
    // }
    // byte[] kkk = new byte[] { 0x51, 0x57, 0x45, 0x52, 0x54, 0x5a, 0x55, 0x49,
    // 0x4f, 0x50, 0x41, 0x53, 0x44, 0x46, 0x47, 0x48 };
    //
    // byte[] kb = new BASE64Decoder().decodeBuffer(dk0);
    // SecretKeySpec skeySpec = new SecretKeySpec(kkk, "AES");
    // Cipher dr = Cipher.getInstance("AES/ECB/NoPadding");
    // dr.init(Cipher.DECRYPT_MODE, skeySpec);
    // byte[] pgn = dr.doFinal(kb);
    //
    // String dds = d5(d0, new String(pgn));
    // dds = fds(dds);
    // dds = Encoding.filterString(dds);
    //
    // pxs(dds, d4);
    // if (dlU == null || dlU.size() == 0) {
    // Log.L.severe("No Links found: " + dss);
    //
    // return cs;
    // } else {
    //
    // cs.setStatus(ContainerStatus.STATUS_FINISHED);
    // return cs;
    // }
    // }

    // private String cs2(String s, String m) throws IOException {
    //
    // Log.L.finer("Call  " + s);
    // Browser br = new Browser();
    //
    // String rk = br.postPage(s, "dt=jdtc&st=dlc&d=" + Encoding.urlEncode(m));
    // br = null;
    // return new Regex(rk, "<rc>(.*)</rc>").getMatch(0);
    //
    // }

    // //@Override
    public String[] encrypt(String p0) {
        String b = Encoding.Base64Encode(p0);
        while (b.length() % 16 != 0) {
            b += " ";
        }
        int c7 = (int) (Math.random() * 1000000000.0);
        try {
            String rk = Hash.getMD5("" + c7).substring(0, 16);
            // randomKey = JDHash.getMD5("11").substring(0, 16);
            // Log.L.info("Key: " + randomKey);
            /* CHECK: we should always use getBytes("UTF-8") or with wanted charset, never system charset! */
            byte[] k = rk.getBytes();
            // Log.L.info("ENcode " + base64);
            IvParameterSpec ivSpec = new IvParameterSpec(k);
            SecretKeySpec skeySpec = new SecretKeySpec(k, "AES");

            Cipher cipher;

            cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, ivSpec);
            byte[] ogl = cipher.doFinal(b.getBytes());
            return new String[] { Base64.encodeToString(ogl, false), rk };
            // return new BASE64Encoder().encode(original);
        } catch (Exception e) {

            logger.log(java.util.logging.Level.SEVERE, "Exception occured", e);
            logger.severe("DLC encryption failed (5)");
            return null;

        }

    }

    // //@Override
    public String getCoder() {
        // TODO Auto-generated method stub
        return "JD-DLC-Team";
    }

    private String b(File fu) {
        BufferedReader f;
        StringBuffer bf = new StringBuffer();
        try {
            f = new BufferedReader(new FileReader(fu));

            String line;

            while ((line = f.readLine()) != null) {
                bf.append(line + "\r\n");
            }
            f.close();
            return bf.toString();
        } catch (IOException e) {
            logger.log(e);
        }
        return "";
    }

    private String cs(URL s9, String bin) throws Exception {
        int i = 0;
        while (true) {
            i++;
            logger.finer("Call " + s9);
            Browser br = new Browser();
            br.getHeaders().put("rev", JDUtilities.getRevision());

            //
            br.postPage(s9 + "", "destType=jdtc6&b=" + UpdateController.getInstance().getAppID() + "&srcType=dlc&data=" + bin + "&v=" + JDUtilities.getRevision());

            // 3f69b642cc403506ff1ee7f22b23ce40
            // new byte[]{(byte) 0xef, (byte) 0xe9, (byte) 0x0a, (byte) 0x8e,
            // (byte)
            // 0x60, (byte) 0x4a, (byte) 0x7c, (byte) 0x84, (byte) 0x0e, (byte)
            // 0x88, (byte) 0xd0, (byte) 0x3a, (byte) 0x67, (byte) 0xf6, (byte)
            // 0xb7, (byte) 0xd8};
            // Log.L.info( ri.getHtmlCode());

            // a7b3b706e3cf3770931f081926ed0d95
            if (!br.getHttpConnection().isOK() || !br.containsHTML("rc")) {
                if (i > 3) { return null; }
            } else {
                String dk = br + "";
                String ret = new Regex(dk, "<rc>(.*)</rc>").getMatch(0);

                return ret;
            }
        }

    }

    private String dsk(String dk9) throws Exception, NoSuchAlgorithmException {
        byte[] k = gjdk();
        Thread.sleep(3000);
        @SuppressWarnings("unused")
        String key = JDHexUtils.getHexString(k);
        String str = dk9;

        byte[] j = Base64.decode(str);
        SecretKeySpec skeySpec = new SecretKeySpec(k, "AES");
        Cipher c = Cipher.getInstance("AES/ECB/NoPadding");
        c.init(Cipher.DECRYPT_MODE, skeySpec);
        byte[] o = c.doFinal(j);
        /* CHECK: we should always use new String (bytes,charset) to avoid issues with system charset and utf-8 */
        return new String(Base64.decode(o)).substring(0, 16);

    }

    public String d5(String ct, String k7l) {
        String rte = null;
        try {
            byte[] input;
            input = Base64.decode(ct);

            byte[] k = k7l.getBytes("UTF-8");
            IvParameterSpec ivSpec = new IvParameterSpec(k);
            SecretKeySpec skeySpec = new SecretKeySpec(k, "AES");

            Cipher pl;
            byte[] opl;
            try {
                // cipher = Cipher.getInstance("AES/CBC/NoPadding");
                // cipher.init(Cipher.DECRYPT_MODE, skeySpec, ivSpec);
                // original = cipher.doFinal(input);
                pl = Cipher.getInstance("AES/CBC/PKCS5Padding");
                pl.init(Cipher.DECRYPT_MODE, skeySpec, ivSpec);
                opl = pl.doFinal(input);
            } catch (Exception e) {
                pl = Cipher.getInstance("AES/CBC/NoPadding");
                pl.init(Cipher.DECRYPT_MODE, skeySpec, ivSpec);
                opl = pl.doFinal(input);
            }
            /* CHECK: we should always use new String (bytes,charset) to avoid issues with system charset and utf-8 */
            String pln = new String(Base64.decode(opl));
            rte = pln;
        } catch (Exception e) {
            logger.log(e);
        }

        if (rte == null || rte.indexOf("<content") < 0) {
            logger.info("Old DLC Version");
            try {
                byte[] ii;
                ii = Base64.decode(ct);
                /* CHECK: we should always use getBytes("UTF-8") or with wanted charset, never system charset! */
                byte[] k = k7l.getBytes();
                SecretKeySpec skeySpec = new SecretKeySpec(k, "AES");
                Cipher cp;
                byte[] gln;
                // Alte DLC Container sind mit PKCS5Padding verchlüsseöt.- neue
                // ohne
                // padding
                try {
                    cp = Cipher.getInstance("AES/ECB/PKCS5Padding");
                    cp.init(Cipher.DECRYPT_MODE, skeySpec);
                    gln = cp.doFinal(ii);

                } catch (Exception e) {
                    cp = Cipher.getInstance("AES/ECB/NoPadding");
                    cp.init(Cipher.DECRYPT_MODE, skeySpec);
                    gln = cp.doFinal(ii);
                }

                String po = new String(Base64.decode(gln));

                rte = po;
            } catch (Exception e) {
                logger.log(e);

                rte = null;

            }

        }
        if (rte == null) {
            logger.severe("DLC Decryption failed (3)");
        }
        return rte;
    }

    private String fds(String dcs) {
        if (dcs == null) { return dcs; }
        dcs = dcs.trim();
        if (Regex.matches(dcs, "<dlc>(.*)<\\/dlc>")) {
            dcs = "<dlc>" + new Regex(dcs, "<dlc>(.*)<\\/dlc>").getMatch(0).trim() + "</dlc>";
        }
        return dcs;
    }

    private void pdx1(Node n, File d) throws InstantiationException, IllegalAccessException {
        /*
         * Original Release Version. In der XMl werden plantexte verwendet
         */

        cls = new ArrayList<CrawledLink>();
        CrawledLink nl;
        PackageInfo dpi = new PackageInfo();
        int c = 0;
        NodeList ps = n.getChildNodes();

        for (int pcs = 0; pcs < ps.getLength(); pcs++) {

            ps.item(pcs).getAttributes().getNamedItem("name").getNodeValue();

            NodeList uls = ps.item(pcs).getChildNodes();
            for (int fc = 0; fc < uls.getLength(); fc++) {

                Node f = uls.item(fc);
                if (f != null) {
                    NodeList data = f.getChildNodes();
                    java.util.List<String> ls = new ArrayList<String>();
                    java.util.List<String> pws = new ArrayList<String>();
                    String pc = "";
                    for (int entry = 0; entry < data.getLength(); entry++) {
                        if (data.item(entry).getNodeName().equalsIgnoreCase("url")) {
                            ls.add(data.item(entry).getTextContent());
                        }
                        if (data.item(entry).getNodeName().equalsIgnoreCase("password")) {
                            java.util.List<String> ret = parsePassword(data.item(entry).getTextContent());
                            for (String pw : ret)
                                if (!pws.contains(pw)) pws.add(pw);
                        }
                        if (data.item(entry).getNodeName().equalsIgnoreCase("comment")) {
                            String cmt = data.item(entry).getTextContent();
                            if (pc.indexOf(cmt) < 0) {
                                pc += cmt + " | ";
                            }
                        }
                    }
                    if (pc.length() > 0) {
                        pc = pc.substring(0, pc.length() - 3);
                    }

                    for (int lcs = 0; lcs < ls.size(); lcs++) {

                        // // PluginForHost pHost =
                        // findHostPlugin(links.get(linkCounter));
                        // if (pHost != null) {
                        // newLink = new CrawledLink((PluginForHost)
                        // pHost.getClass().newInstance(),
                        // links.get(linkCounter).substring(links.get(linkCounter
                        // ).lastIndexOf("/")
                        // + 1), pHost.getHost(), null, true);
                        nl = new CrawledLink(ls.get(lcs));
                        nl.setDesiredPackageInfo(dpi);

                        nl.getArchiveInfo().getExtractionPasswords().addAll(pws);
                        dpi.setComment("from Container: " + d + " : " + pc);
                        cls.add(nl);
                        // Log.L.info(""+links.get(linkCounter));

                        c++;
                        // }
                    }
                }

            }
        }

    }

    private void pdx2(Node node, File d) throws InstantiationException, IllegalAccessException {
        /*
         * Neue Version. Alle user generated inhalte werden jetzt base64 verschlüsselt abgelegt. nur generator nicht
         */
        cls = new ArrayList<CrawledLink>();
        CrawledLink nl;
        ;
        int c = 0;
        PackageInfo dpi = new PackageInfo();
        NodeList ps = node.getChildNodes();

        for (int pc = 0; pc < ps.getLength(); pc++) {
            if (!ps.item(pc).getNodeName().equals("package")) {
                continue;
            }

            ps.item(pc).getAttributes().getNamedItem("name").getNodeValue();

            NodeList uls = ps.item(pc).getChildNodes();
            for (int fc = 0; fc < uls.getLength(); fc++) {

                Node file = uls.item(fc);
                if (file != null) {
                    NodeList data = file.getChildNodes();
                    java.util.List<String> ls = new ArrayList<String>();
                    java.util.List<String> pws = new ArrayList<String>();
                    String pgc = "";
                    for (int entry = 0; entry < data.getLength(); entry++) {

                        if (data.item(entry).getNodeName().equalsIgnoreCase("url")) {
                            ls.add(Encoding.Base64Decode(data.item(entry).getTextContent()));
                        }
                        if (data.item(entry).getNodeName().equalsIgnoreCase("password")) {
                            java.util.List<String> ret = parsePassword(Encoding.Base64Decode(data.item(entry).getTextContent()));
                            for (String pw : ret)
                                if (!pws.contains(pw)) pws.add(pw);
                        }
                        if (data.item(entry).getNodeName().equalsIgnoreCase("comment")) {
                            String cmt = Encoding.Base64Decode(data.item(entry).getTextContent());
                            if (pgc.indexOf(cmt) < 0) {
                                pgc += cmt + " | ";
                            }
                        }
                    }
                    if (pgc.length() > 0) {
                        pgc = pgc.substring(0, pgc.length() - 3);
                    }

                    for (int lc = 0; lc < ls.size(); lc++) {

                        // PluginForHost pHost =
                        // findHostPlugin(links.get(linkCounter));
                        // if (pHost != null) {
                        // newLink = new CrawledLink((PluginForHost)
                        // pHost.getClass().newInstance(),
                        // links.get(linkCounter).substring(links.get(linkCounter
                        // ).lastIndexOf("/")
                        // + 1), pHost.getHost(), null, true);

                        nl = new CrawledLink(ls.get(lc));
                        nl.setDesiredPackageInfo(dpi);
                        nl.getArchiveInfo().getExtractionPasswords().addAll(pws);
                        dpi.setComment("from Container: " + d + " : " + pc);
                        cls.add(nl);

                        c++;
                    }
                }

            }
        }

    }

    private static java.util.List<String> parsePassword(String password) {
        java.util.List<String> pws = new ArrayList<String>();
        if (password == null || password.length() == 0 || password.matches("[\\s]*")) return pws;
        if (password.matches("[\\s]*\\{[\\s]*\".*\"[\\s]*\\}[\\s]*$")) {
            password = password.replaceFirst("[\\s]*\\{[\\s]*\"", "").replaceFirst("\"[\\s]*\\}[\\s]*$", "");
            for (String pw : password.split("\"[\\s]*\\,[\\s]*\"")) {
                if (!pws.contains(pw)) pws.add(pw);
            }
        }
        if (pws.size() == 0) pws.add(password);
        return pws;
    }

    private void pcx3(Node node, File dlc) throws InstantiationException, IllegalAccessException {
        /*
         * alle inhalte sind base64 verschlüsselt. XML Str5uktur wurde angepasst
         */

        logger.info("Parse v3");
        cls = new ArrayList<CrawledLink>();
        CrawledLink nl;

        int c = 0;

        NodeList ps = node.getChildNodes();
        String pns = "";
        String cs = "";
        String cmts = "";
        for (int pgs = 0; pgs < ps.getLength(); pgs++) {
            if (!ps.item(pgs).getNodeName().equals("package")) {
                continue;
            }
            PackageInfo dpi = new PackageInfo();
            String pn = Encoding.Base64Decode(ps.item(pgs).getAttributes().getNamedItem("name").getNodeValue());
            String oos = ps.item(pgs).getAttributes().getNamedItem("passwords") == null ? null : Encoding.Base64Decode(ps.item(pgs).getAttributes().getNamedItem("passwords").getNodeValue());
            String cs2 = ps.item(pgs).getAttributes().getNamedItem("comment") == null ? null : Encoding.Base64Decode(ps.item(pgs).getAttributes().getNamedItem("comment").getNodeValue());
            String ca3 = ps.item(pgs).getAttributes().getNamedItem("category") == null ? null : Encoding.Base64Decode(ps.item(pgs).getAttributes().getNamedItem("category").getNodeValue());
            dpi.setName(LinknameCleaner.cleanFileName(pn));

            if (ca3 != null && ca3.trim().length() > 0) {
                dpi.setComment("[" + ca3 + "] " + cs2);
            } else {
                dpi.setComment(cs2);
            }

            pns += pn + (pn != null && pn.length() > 0 ? "; " : "");
            cs += ca3 + (ca3 != null && ca3.length() > 0 ? "; " : "");
            cmts += cs2 + (cs2 != null && cs2.length() > 0 ? "; " : "");
            NodeList urls = ps.item(pgs).getChildNodes();
            for (int fileCounter = 0; fileCounter < urls.getLength(); fileCounter++) {

                Node file = urls.item(fileCounter);
                if (file != null) {
                    NodeList data = file.getChildNodes();
                    java.util.List<String> ls2 = new ArrayList<String>();
                    java.util.List<String> n5 = new ArrayList<String>();
                    java.util.List<String> s7 = new ArrayList<String>();

                    for (int entry = 0; entry < data.getLength(); entry++) {
                        if (data.item(entry).getNodeName().equalsIgnoreCase("url")) {
                            String sls = Encoding.Base64Decode(data.item(entry).getTextContent());
                            String[] lsr = HTMLParser.getHttpLinks(sls, null);
                            if (lsr.length == 0) {
                                while (true) {
                                    logger.warning("Failed DLC Decoding. Try to decode again");
                                    String old = sls;
                                    sls = Encoding.Base64Decode(sls);
                                    if (old.equals(sls)) {
                                        break;
                                    }
                                    lsr = HTMLParser.getHttpLinks(sls, null);
                                }
                            }
                            ls2.add(sls);
                            for (String link : lsr) {
                                if (!sls.trim().equals(link.trim())) {
                                    ls2.add(link);
                                }

                            }
                            if (lsr.length > 1) {
                                logger.severe("DLC Error. Generator Link split Error");
                                break;
                            }
                        }
                        if (data.item(entry).getNodeName().equalsIgnoreCase("filename")) {
                            n5.add(Encoding.Base64Decode(data.item(entry).getTextContent()));
                        }
                        if (data.item(entry).getNodeName().equalsIgnoreCase("size")) {
                            s7.add(Encoding.Base64Decode(data.item(entry).getTextContent()));
                        }
                    }

                    while (ls2.size() > n5.size()) {
                        n5.add(null);
                    }
                    while (ls2.size() > s7.size()) {
                        s7.add(null);
                    }

                    for (int lcs = 0; lcs < ls2.size(); lcs++) {

                        // PluginForHost pHost =
                        // findHostPlugin(links.get(linkCounter));
                        // if (pHost != null) {
                        // newLink = new CrawledLink((PluginForHost)
                        // pHost.getClass().newInstance(),
                        // links.get(linkCounter).substring(links.get(linkCounter
                        // ).lastIndexOf("/")
                        // + 1), pHost.getHost(), null, true);
                        // String ll =
                        // ls2.get(lcs).substring(ls2.get(lcs).lastIndexOf("/")
                        // + 1);
                        nl = new CrawledLink(ls2.get(lcs));
                        nl.setDesiredPackageInfo(dpi);
                        nl.getArchiveInfo().getExtractionPasswords().addAll(parsePassword(oos));
                        dpi.setComment("(Containerlinks) " + cs2 == null ? "" : cs2);
                        if (n5.get(lcs) != null) {
                            nl.setName(n5.get(lcs));
                        }
                        // if (s7.get(lcs) != null) {
                        // nl.(Formatter.filterInt(s7.get(lcs)));
                        // }

                        cls.add(nl);

                        c++;
                        // }

                    }
                }

            }
        }

    }

    private void pxs(String cs, File dlc) throws SAXException, IOException, ParserConfigurationException, InstantiationException, IllegalAccessException {

        DocumentBuilderFactory f;
        // Log.L.info(jdtc);
        InputSource is;
        Document doc;

        f = DocumentBuilderFactory.newInstance();
        f.setValidating(false);
        // Log.L.info("encrypted: "+dlcString);
        if (cs.trim().startsWith("<dlc")) {
            // New (cbc Änderung)
            is = new InputSource(new StringReader(cs));
        } else {
            // alt
            is = new InputSource(new StringReader("<dlc>" + cs + "</dlc>"));
        }

        doc = f.newDocumentBuilder().parse(is);
        NodeList nodes = doc.getFirstChild().getChildNodes();
        header = new HashMap<String, String>();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);

            if (node.getNodeName().equalsIgnoreCase("header")) {
                NodeList entries = node.getChildNodes();

                for (int entryCounter = 0; entryCounter < entries.getLength(); entryCounter++) {

                    if (entries.item(entryCounter).getNodeName().equalsIgnoreCase("generator")) {
                        NodeList generatorEntries = entries.item(entryCounter).getChildNodes();

                        for (int genCounter = 0; genCounter < generatorEntries.getLength(); genCounter++) {

                            if (generatorEntries.item(genCounter).getNodeName().equalsIgnoreCase("app")) {
                                header.put("generator.app", Encoding.Base64Decode(generatorEntries.item(genCounter).getTextContent()));
                            }
                            if (generatorEntries.item(genCounter).getNodeName().equalsIgnoreCase("version")) {
                                header.put("generator.version", Encoding.Base64Decode(generatorEntries.item(genCounter).getTextContent()));
                            }
                            if (generatorEntries.item(genCounter).getNodeName().equalsIgnoreCase("url")) {
                                header.put("generator.url", Encoding.Base64Decode(generatorEntries.item(genCounter).getTextContent()));
                            }

                        }

                    }
                    if (entries.item(entryCounter).getNodeName().equalsIgnoreCase("tribute")) {

                        NodeList names = entries.item(entryCounter).getChildNodes();
                        String tribute = "";
                        for (int tributeCounter = 0; tributeCounter < names.getLength(); tributeCounter++) {
                            tribute += Encoding.Base64Decode(names.item(tributeCounter).getTextContent());
                            if (tributeCounter + 1 < names.getLength()) {
                                tribute += ", ";
                            }
                        }
                        header.put("tribute", tribute);

                    }
                    if (entries.item(entryCounter).getNodeName().equalsIgnoreCase("dlcxmlversion")) {

                        String dlcXMLVersion = Encoding.Base64Decode(entries.item(entryCounter).getTextContent());
                        header.put("dlcxmlversion", dlcXMLVersion);
                    }

                }
            }
            if (node.getNodeName().equalsIgnoreCase("content")) {
                logger.info("dlcxmlversion: " + header.get("dlcxmlversion"));
                if (header.containsKey("dlcxmlversion") && header.get("dlcxmlversion") != null && header.get("dlcxmlversion").equals("20_02_2008")) {
                    pcx3(node, dlc);
                } else if (header.containsKey("generator.app") && header.get("generator.app").equals("jDownloader") && header.containsKey("generator.version") && Double.parseDouble(header.get("generator.version")) < 654.0) {
                    pdx1(node, dlc);

                } else {

                    pdx2(node, dlc);

                }

            }
            // Log.L.info("Header " + header);
        }

    }

    private byte[] gjdk() {

        byte[] k = gk();
        @SuppressWarnings("unused")
        /* CHECK: we should always use new String (bytes,charset) to avoid issues with system charset and utf-8 */
        String h = new String(k);
        // doit

        return k;
    }

    private byte[] gk() {
        try {
            return (byte[]) getClass().forName(getClass().getPackage().getName() + ".Config").getField("D").get(null);
        } catch (Throwable e) {
        }
        return null;
    }

    public static String xmltoStr(Document header) {
        Transformer transformer;
        try {
            transformer = TransformerFactory.newInstance().newTransformer();

            transformer.setOutputProperty(OutputKeys.INDENT, "yes");

            // initialize StreamResult with File object to save to file
            StreamResult result = new StreamResult(new StringWriter());

            DOMSource source = new DOMSource(header);

            transformer.transform(source, result);

            String xmlString = result.getWriter().toString();
            return xmlString;
        } catch (Exception e) {
            LogController.CL().log(e);
        }
        return null;
    }

    public java.util.List<DownloadLink> getPackageFiles(FilePackage filePackage, List<DownloadLink> links) {
        java.util.List<DownloadLink> ret = new ArrayList<DownloadLink>();
        // ret.add(DownloadLink);

        Iterator<DownloadLink> iterator = links.iterator();
        DownloadLink nextDownloadLink = null;
        while (iterator.hasNext()) {
            nextDownloadLink = iterator.next();

            if (filePackage == nextDownloadLink.getFilePackage()) {
                ret.add(nextDownloadLink);
            }
        }
        return ret;
    }

    public String createContainerString(List<DownloadLink> links) {
        HashMap<String, DownloadLink> map = new HashMap<String, DownloadLink>();
        java.util.List<DownloadLink> filter = new ArrayList<DownloadLink>();
        // filter
        for (DownloadLink l : links) {
            String url = null;
            if (l.getLinkType() == DownloadLink.LINKTYPE_CONTAINER) {
                if (l.gotBrowserUrl()) url = l.getBrowserUrl();
            } else {
                url = l.getDownloadURL();
            }
            if (url == null) continue;
            if (!map.containsKey(url)) {
                filter.add(l);
            }
            map.put(url, l);
        }
        links = filter;

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        SubConfiguration cfg = SubConfiguration.getConfig("DLCCONFIG");
        InputSource inSourceHeader = new InputSource(new StringReader("<header><generator><app></app><version/><url></url></generator><tribute/><dlcxmlversion/></header>"));
        InputSource inSourceContent = new InputSource(new StringReader("<content/>"));

        try {
            Document content = factory.newDocumentBuilder().parse(inSourceContent);
            Document header = factory.newDocumentBuilder().parse(inSourceHeader);
            Node header_generator_app = header.getFirstChild().getFirstChild().getChildNodes().item(0);
            Node header_generator_version = header.getFirstChild().getFirstChild().getChildNodes().item(1);
            Node header_generator_url = header.getFirstChild().getFirstChild().getChildNodes().item(2);
            header_generator_app.appendChild(header.createTextNode(Encoding.Base64Encode("JDownloader")));
            header_generator_version.appendChild(header.createTextNode(Encoding.Base64Encode(JDUtilities.getRevision())));
            header_generator_url.appendChild(header.createTextNode(Encoding.Base64Encode("http://jdownloader.org")));

            Node header_tribute = header.getFirstChild().getChildNodes().item(1);

            if (cfg.getBooleanProperty("ASK_ADD_INFOS", false)) {
                Element element = header.createElement("name");
                header_tribute.appendChild(element);

                element.appendChild(header.createTextNode(Encoding.Base64Encode(UserIO.getInstance().requestInputDialog("Uploader Name"))));

            }
            if (cfg.getStringProperty("UPLOADERNAME", null) != null && cfg.getStringProperty("UPLOADERNAME", null).trim().length() > 0) {
                Element element = header.createElement("name");
                header_tribute.appendChild(element);
                element.appendChild(header.createTextNode(Encoding.Base64Encode(cfg.getStringProperty("UPLOADERNAME", null))));

            }
            Node header_dlxxmlversion = header.getFirstChild().getChildNodes().item(2);

            header_dlxxmlversion.appendChild(header.createTextNode(Encoding.Base64Encode("20_02_2008")));

            java.util.List<FilePackage> packages = new ArrayList<FilePackage>();

            for (int i = 0; i < links.size(); i++) {
                if (!packages.contains(links.get(i).getFilePackage())) {
                    packages.add(links.get(i).getFilePackage());
                }
            }

            for (int i = 0; i < packages.size(); i++) {
                Element FilePackage = content.createElement("package");
                if (packages.get(i) == null) {
                    FilePackage.setAttribute("name", Encoding.Base64Encode("various"));
                } else {
                    FilePackage.setAttribute("name", Encoding.Base64Encode(packages.get(i).getName()));
                    FilePackage.setAttribute("comment", Encoding.Base64Encode(packages.get(i).getComment()));
                    String category = Encoding.Base64Encode("various");
                    if (cfg.getBooleanProperty("ASK_ADD_INFOS", false)) {
                        category = Encoding.Base64Encode(UserIO.getInstance().requestInputDialog("Category for package " + packages.get(i).getName()));
                    }
                    FilePackage.setAttribute("category", category);

                }
                // <package name="cGFrZXQx" passwords="eyJwYXNzIiwgInBhc3MyIn0="
                // comment="RGFzIGlzdCBlaW4gVGVzdGNvbnRhaW5lcg=="
                // category="bW92aWU=">

                content.getFirstChild().appendChild(FilePackage);

                java.util.List<DownloadLink> tmpLinks = getPackageFiles(packages.get(i), links);

                for (int x = 0; x < tmpLinks.size(); x++) {
                    Element file = content.createElement("file");
                    FilePackage.appendChild(file);
                    Element url = content.createElement("url");
                    Element filename = content.createElement("filename");
                    Element size = content.createElement("size");
                    DownloadLink link = tmpLinks.get(x);
                    if (link.getLinkType() == DownloadLink.LINKTYPE_CONTAINER) {
                        url.appendChild(content.createTextNode(Encoding.Base64Encode(link.getBrowserUrl())));
                    } else {
                        url.appendChild(content.createTextNode(Encoding.Base64Encode(link.getDownloadURL())));
                    }

                    // url.appendChild(content.createTextNode(JDUtilities.
                    // Base64Encode(tmpLinks.get(x).getDownloadURL())));

                    filename.appendChild(content.createTextNode(Encoding.Base64Encode(tmpLinks.get(x).getName())));

                    size.appendChild(content.createTextNode(Encoding.Base64Encode(tmpLinks.get(x).getDownloadSize() + "")));

                    FilePackage.getLastChild().appendChild(url);
                    FilePackage.getLastChild().appendChild(filename);
                    FilePackage.getLastChild().appendChild(size);

                }

            }

            int ind1 = xmltoStr(header).indexOf("<header");
            int ind2 = xmltoStr(content).indexOf("<content");
            String ret = xmltoStr(header).substring(ind1) + xmltoStr(content).substring(ind2);

            return "<dlc>" + ret + "</dlc>";
        } catch (Exception e) {
            logger.log(e);
        }
        return null;
    }

}