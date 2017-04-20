//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import jd.http.Browser;
import jd.parser.Regex;
import jd.parser.html.Form;

public class ImgmazeCom {

    public static Form fixImghost_next_form(final Browser br, final Form imghost_next_form) {
        final Regex blablub = br.getRegex("\\..([a-f0-9]+)\\{display:initial;\\}\\..([a-f0-9]+)\\{display:initial;\\}\\..([a-f0-9]+)\\{display:initial;\\}");
        final Regex blablub_2 = br.getRegex("\\{visibility:hidden;\\}\\..([a-f0-9]+)\\{visibility:initial;\\}\\..([a-f0-9]+)\\{visibility:initial;\\}\\..([a-f0-9]+)\\{visibility:initial;\\}");

        /* 2017-04-20: imgrock.net */
        String special_key = blablub_2.getMatch(2);
        if (special_key == null) {
            /* 2017-02-22 */
            special_key = br.getRegex("</Form>\\'\\)\\.attr\\(\\'cl.*?name=\"([a-f0-9]+)\"").getMatch(0);
        }
        if (special_key == null) {
            /* 2017-02-10 */
            special_key = blablub.getMatch(0);
        }
        if (special_key == null) {
            /* New 2017-02-09 */
            special_key = br.getRegex("<style>\\..([a-f0-9]+)\\{display:initial;\\}").getMatch(0);
        }
        if (special_key == null) {
            /* New 2017-02-07 */
            special_key = br.getRegex("\\..([a-f0-9]+)\\{display:initial;\\}\\..([a-f0-9]+)\\{display:initial;\\}\\..([a-f0-9]+)\\{display:initial;\\}").getMatch(2);
        }
        if (special_key == null) {
            /* New 2017-01-30 */
            special_key = br.getRegex("\\..(?:[a-f0-9]+)\\{display:initial;\\}\\..([a-f0-9]+)\\{display:initial;\\}").getMatch(0);
        }
        if (special_key == null) {
            /* New 2017-01-23 */
            special_key = br.getRegex("\\..([a-f0-9]+)\\{display:initial;\\}").getMatch(0);
        }
        if (special_key == null) {
            /* 2017-01-20 */
            special_key = br.getRegex("\\$\\(\\'#\\d+\\'\\)\\.append\\(\\$\\(\\'<Form method=\"POST.*?name=\"([a-f0-9]{32})\" value=\"1\"").getMatch(0);
        }
        if (special_key == null) {
            /* 2017-01-06 */
            special_key = br.getRegex("name=\"([a-f0-9]{32})\" value=\"1\"").getMatch(0);
        }
        if (special_key != null && !imghost_next_form.hasInputFieldByName(special_key)) {
            imghost_next_form.put(special_key, "1");
        }
        return imghost_next_form;
    }

}