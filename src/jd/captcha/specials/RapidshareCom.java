//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

package jd.captcha.specials;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

import jd.captcha.JAntiCaptcha;
import jd.captcha.LetterComperator;
import jd.captcha.pixelgrid.Captcha;
import jd.captcha.pixelgrid.Letter;
import jd.captcha.utils.UTILITIES;
import jd.utils.JDUtilities;

/**
 * 
 * 
 * @author JD-Team
 */
public class RapidshareCom {

    private static Logger logger = JDUtilities.getLogger();

    public static void onlyCats(Vector<LetterComperator> lcs, JAntiCaptcha owner) {
        // if (true) return;
        boolean first = false;
        if (owner.getJas().getInteger("desinvariant") == 0) {
            owner.getWorkingCaptcha().setProperty("variants", new ArrayList<Vector<LetterComperator>>());
            first = true;
        }
        int count = 0;
        if (lcs != null && lcs.size() > 0) {
            for (Iterator<LetterComperator> it = lcs.iterator(); it.hasNext();) {
                LetterComperator next = it.next();
                logger.info("--> "+next.getDecodedValue()+" : " + next + "<< ");
                count += next.getValityPercent() >= 60.0 ? 1 : 0;
            }

        }
        Vector<LetterComperator> tmp = new Vector<LetterComperator>();
        tmp.addAll(lcs);
        ((ArrayList<Vector<LetterComperator>>) owner.getWorkingCaptcha().getProperty("variants")).add(tmp);
        
        if (count > 3 || lcs == null || lcs.size() < 4) {
            logger.severe("ACHTUNG ERKENNUNGSFEHLER " + count);
           lcs.removeAllElements();
            retry(owner, lcs);
            if (first) {
                double bestValue=100.0;
                Vector<LetterComperator> bestLcs=null;
                ArrayList<Vector<LetterComperator>> variants = ((ArrayList<Vector<LetterComperator>>) owner.getWorkingCaptcha().getProperty("variants"));
                for (Iterator<Vector<LetterComperator>> it = variants.iterator(); it.hasNext();) {
                    Vector<LetterComperator> next = it.next();
                    if (next != null && next.size() >= 4) {
                        double cor = 0;
                        for (Iterator<LetterComperator> it2 = next.iterator(); it2.hasNext();) {
                            cor += it2.next().getValityPercent();
                        }
                        cor /= next.size();
                        if(cor<bestValue){
                            logger.info("new Best LCS: "+cor);
                            bestLcs=next;
                            bestValue=cor;
                        }
                    }
                }
                if(lcs!=bestLcs){
                    lcs.removeAllElements();
                    lcs.addAll(bestLcs);
                }
            }
            return;
        }
        //if (true) return;
        final HashMap<LetterComperator, LetterComperator> map = new HashMap<LetterComperator, LetterComperator>();

        String methodsPath = UTILITIES.getFullPath(new String[] { JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath(), "jd", "captcha", "methods" });
        String hoster = "rscat.com";
        final Vector<LetterComperator> cats = new Vector<LetterComperator>();
        JAntiCaptcha jac = new JAntiCaptcha(methodsPath, hoster);
        for (Iterator<LetterComperator> it = lcs.iterator(); it.hasNext();) {
            LetterComperator next = it.next();
            Letter dif = next.getDifference();
            dif.removeSmallObjects(0.8, 0.8, 5);
            dif.clean();

            LetterComperator c = jac.getLetter(dif);
            if (!c.getDecodedValue().equalsIgnoreCase("k")) {
                it.remove();
            } else {
                map.put(next, c);
                cats.add(c);
            }

        }

        Collections.sort(cats, new Comparator<LetterComperator>() {
            public int compare(LetterComperator obj1, LetterComperator obj2) {

                if (obj1.getValityPercent() < obj2.getValityPercent()) return -1;
                if (obj1.getValityPercent() > obj2.getValityPercent()) return 1;
                return 0;
            }
        });

        // schlechte entfernen

        for (int i = cats.size() - 1; i >= 4; i--) {
            cats.remove(i);
        }
        for (Iterator<LetterComperator> it = lcs.iterator(); it.hasNext();) {
            LetterComperator next = it.next();
            if (!map.containsKey(next) || !cats.contains(map.get(next))) it.remove();
        }
        logger.info("LENGTH : " + lcs.size());

    }

    private static void retry(JAntiCaptcha jac, Vector<LetterComperator> lcs) {
        int desiinvariant = jac.getJas().getInteger("desinvariant") + 1;
        if (desiinvariant > 4) { return; }
        jac.getJas().set("desinvariant", desiinvariant);
        Captcha captcha = jac.getWorkingCaptcha();
        captcha.reset();
        jac.getJas().executePrepareCommands(captcha);
        int ln = jac.getLetterNum();
        Letter[] letters = captcha.getLetters(ln);
        if (letters == null) return;
        // LetterComperator[] newLetters = new LetterComperator[letters.length];

        LetterComperator akt;

        if (letters == null) {
            captcha.setValityPercent(100.0);
            if (JAntiCaptcha.isLoggerActive()) logger.severe("Captcha konnte nicht erkannt werden!");
            return;
        }

        Vector<LetterComperator> newLettersVector = new Vector<LetterComperator>();
        for (int i = 0; i < letters.length; i++) {

            if (letters[i].detected != null)
                akt = letters[i].detected;
            else
                akt = jac.getLetter(letters[i]);

            akt.getA().id = i;
            newLettersVector.add(akt);

        }
        onlyCats(newLettersVector, jac);
        if (newLettersVector.size() > 0) {
            lcs.removeAllElements();
            lcs.addAll(newLettersVector);
        }

    }
    
    /**
     * Filtert kleine Buchstaben
     * @param org
     * @param jac
     * @return
     */
    public static Letter[] letterFilter(Letter[] org, JAntiCaptcha jac){
        Vector<Letter> ret= new  Vector<Letter>();
        
        for(Letter l:org){
            if(l.getArea()>=jac.getJas().getInteger("minimumObjectArea")){
                ret.add(l);
            }
        }
        return ret.toArray(new Letter[]{});
    }

}