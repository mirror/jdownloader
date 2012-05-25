package org.jdownloader.extensions.translator;

import java.io.File;
import java.util.Map;
import java.util.Map.Entry;

import org.appwork.utils.Regex;
import org.appwork.utils.parser.Mapimizer;
import org.appwork.utils.svn.ResolveHandler;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.images.NewTheme;
import org.tmatesoft.svn.core.wc.SVNInfo;

public class ConflictResolveHandler implements ResolveHandler {

    @Override
    public String resolveConflict(SVNInfo info, File file, String contents, int startMine, int endMine, int startTheirs, int endTheirs) {
        Map<String, String> mine = Mapimizer.keyValue(Regex.getLines(contents.substring(startMine, endMine).trim()));
        Map<String, String> theirs = Mapimizer.keyValue(Regex.getLines(contents.substring(startTheirs, endTheirs).trim()));

        StringBuilder sb = new StringBuilder();
        // all new entries will be confirmed without user interaction
        for (Entry<String, String> e : theirs.entrySet()) {
            if (!mine.containsKey(e.getKey())) {
                if (sb.length() > 0) sb.append("\r\n");
                sb.append(e.getKey());
                sb.append("=");
                sb.append(e.getValue());

            }
        }
        // entries missing in theirs - we keep them
        for (Entry<String, String> e : mine.entrySet()) {
            if (!theirs.containsKey(e.getKey())) {
                if (sb.length() > 0) sb.append("\r\n");
                sb.append(e.getKey());
                sb.append("=");
                sb.append(e.getValue());

            }
        }
        // both contain the key, but different values
        for (Entry<String, String> e : mine.entrySet()) {
            if (theirs.containsKey(e.getKey())) {
                String html = "<h1>Key: " + e.getKey() + "</h1><h2>Translation A</h2>" + e.getValue() + "<h2>Translation B</h2>" + theirs.get(e.getKey()) + "<br><br>Select the better translation. A or B:";
                try {
                    Dialog.getInstance().showConfirmDialog(Dialog.STYLE_HTML, "Conflicts occured!", html, NewTheme.I().getIcon("question", 32), "A", "B");
                    if (sb.length() > 0) sb.append("\r\n");
                    sb.append(e.getKey());
                    sb.append("=");
                    sb.append(e.getValue());
                } catch (DialogNoAnswerException e1) {
                    if (sb.length() > 0) sb.append("\r\n");
                    sb.append(e.getKey());
                    sb.append("=");
                    sb.append(theirs.get(e.getKey()));
                }

            }
        }

        return sb.toString();
    }

}
