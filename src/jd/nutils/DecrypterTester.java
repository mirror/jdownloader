package jd.nutils;

import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;

import javax.swing.JOptionPane;

import jd.DecryptPluginWrapper;
import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

public class DecrypterTester {

    private static final String decrypter_prefix = "jd.plugins.decrypt.";

    private static final String decrypter_classname = null;

    private static final String decrypter_testlink = null;

    public static void main(String[] args) {
        new DecrypterTester();
    }

    private DecrypterTester() {
        String classname = decrypter_classname;
        if (classname == null) classname = JOptionPane.showInputDialog("Geben Sie den Klassennamen vom Decrypter ein:", "");
        if (classname == null) {
            System.out.println("ClassName is null!");
            System.exit(0);
        }

        PluginForDecrypt decrypter = getDecrypter(decrypter_prefix + classname);
        if (decrypter == null) {
            System.out.println("Error while loading the decrypter!");
            System.exit(0);
        }

        String testlink = decrypter_testlink;
        if (testlink == null) testlink = JOptionPane.showInputDialog("Geben Sie einen Testlink zum Decrypter ein:", "http://www.testlink.com");
        if (testlink == null) {
            System.out.println("TestLink is null!");
            System.exit(0);
        }

        ArrayList<DownloadLink> dLinks = null;
        try {
            dLinks = decrypter.decryptIt(new CryptedLink(testlink), new ProgressController("Test for decrypter " + classname + " with link " + testlink));
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (dLinks == null) {
            System.out.println("Decrypter returned null!");
            System.exit(0);
        }

        StringBuilder result = new StringBuilder();
        for (DownloadLink dLink : dLinks) {
            result.append(dLink.getDownloadURL());
            result.append(new char[] { '\r', '\n' });
        }
        JOptionPane.showMessageDialog(null, result.toString());

        System.exit(0);
    }

    private PluginForDecrypt getDecrypter(String classname) {
        try {
            URLClassLoader classloader = new URLClassLoader(new URL[] { JDUtilities.getJDHomeDirectoryFromEnvironment().toURI().toURL(), JDUtilities.getResourceFile("java").toURI().toURL() }, Thread.currentThread().getContextClassLoader());

            Class<?> plgClass = classloader.loadClass(classname);

            if (plgClass == null) {
                System.out.println("Decrypter not found!");
                return null;
            }

            Class<?>[] classes = new Class[] { PluginWrapper.class };
            Constructor<?> con = plgClass.getConstructor(classes);

            return (PluginForDecrypt) con.newInstance(new Object[] { new DecryptPluginWrapper(classname.toLowerCase(), classname, "") });
        } catch (Exception e) {
            System.out.println("Decrypter Exception!");
            e.printStackTrace();
        }
        return null;
    }
}
