package org.jdownloader.extensions.langfileeditor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import jd.event.MessageEvent;
import jd.event.MessageListener;
import jd.nutils.svn.Subversion;
import jd.utils.JDUtilities;

public class CleanupLanguageFiles {

    public static void main(String[] args) {
        new CleanupLanguageFiles();
    }

    private final File dirLanguages   = JDUtilities.getResourceFile("tmp/lfe/lng/");
    private final File dirWorkingCopy = JDUtilities.getResourceFile("tmp/lfe/src/");

    private CleanupLanguageFiles() {
        updateSVN(LFEGui.SOURCE_SVN, this.dirWorkingCopy);
        final SrcParser srcParser = new SrcParser(this.dirWorkingCopy);
        srcParser.getBroadcaster().addListener(new MessageListener() {

            public void onMessage(MessageEvent event) {
                System.out.println(event.getMessage());
            }

        });
        srcParser.parse();

        updateSVN(LFEGui.LANGUAGE_SVN, this.dirLanguages);
        for (final File languageFile : this.dirLanguages.listFiles()) {
            if (!languageFile.getName().endsWith(".loc")) continue;

            cleanupLanguageFile(srcParser, languageFile);
        }
    }

    private void cleanupLanguageFile(final SrcParser srcParser, final File languageFile) {
        final HashMap<String, String> languageKeys = new HashMap<String, String>();
        final ArrayList<KeyInfo> data = new ArrayList<KeyInfo>();

        LFEGui.parseLanguageFile(languageFile, languageKeys);

        String key, value;
        for (final LngEntry entry : srcParser.getEntries()) {
            key = entry.getKey();
            value = languageKeys.remove(key);
            if (value != null) data.add(new KeyInfo(key, null, value, null));
        }

        Iterator<Entry<String, String>> it = languageKeys.entrySet().iterator();
        Entry<String, String> entry;
        outer: while (it.hasNext()) {
            entry = it.next();
            key = entry.getKey();

            for (final String patt : srcParser.getPattern()) {
                if (key.matches(patt)) {
                    data.add(new KeyInfo(key, null, entry.getValue(), null));
                    it.remove();
                    continue outer;
                }
            }
        }

        System.out.println(languageFile.getName() + ":");
        System.out.println("Correct: " + data.size());

        if (languageKeys.isEmpty()) return;

        System.out.println("Old:     " + languageKeys.size());
        System.out.println("Remove old keys!");
        saveLanguageFile(languageFile, data);
    }

    private void saveLanguageFile(final File file, final ArrayList<KeyInfo> data) {
        final StringBuilder sb = new StringBuilder();

        Collections.sort(data);
        for (final KeyInfo entry : data) {
            sb.append(entry.toString()).append('\n');
        }

        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF8"));
            out.write(sb.toString());
        } catch (final Exception e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void updateSVN(String svnUrl, File localUrl) {
        if (!localUrl.exists()) localUrl.mkdirs();

        try {
            final Subversion svn = new Subversion(svnUrl);

            try {
                svn.revert(localUrl);
            } catch (final Exception e) {
                e.printStackTrace();
            }

            try {
                svn.update(localUrl, null);
            } catch (final Exception e) {
                e.printStackTrace();
            }

            svn.dispose();
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

}
