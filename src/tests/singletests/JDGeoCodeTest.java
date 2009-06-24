package tests.singletests;

import static org.junit.Assert.assertFalse;
import jd.utils.JDGeoCode;

import org.junit.Before;
import org.junit.Test;

public class JDGeoCodeTest {

    @Before
    public void setUp() throws Exception {

    }

    @Test
    public void geoCodeTest() {

        String lng;
        String[] res;
        /* language only */
        lng = "en";
        res = JDGeoCode.parseLanguageCode(lng);
        assertFalse("Result error for " + lng, res == null);
        assertFalse("Result error for " + lng, !res[0].equals("en"));
        assertFalse("Result error for " + lng, res[1] != null);
        assertFalse("Result error for " + lng, res[2] != null);
        /* DD is not a valid country... is an extension */
        lng = "en-DD";
        res = JDGeoCode.parseLanguageCode(lng);
        assertFalse("Result error for " + lng, res == null);
        assertFalse("Result error for " + lng, !res[0].equals("en"));
        assertFalse("Result error for " + lng, res[1] != null);
        assertFalse("Result error for " + lng, !res[2].equals("DD"));

        /* language and country */
        lng = "en-GB";
        res = JDGeoCode.parseLanguageCode(lng);
        assertFalse("Result error for " + lng, res == null);
        assertFalse("Result error for " + lng, !res[0].equals("en"));
        assertFalse("Result error for " + lng, !res[1].equals("GB"));
        assertFalse("Result error for " + lng, res[2] != null);

        /* language and country aand extension */
        lng = "en-GB-2";
        res = JDGeoCode.parseLanguageCode(lng);
        assertFalse("Result error for " + lng, res == null);
        assertFalse("Result error for " + lng, !res[0].equals("en"));
        assertFalse("Result error for " + lng, !res[1].equals("GB"));
        assertFalse("Result error for " + lng, !res[2].equals("2"));
        
        /*getNativelanguage*/
       assertFalse("Result error for tr->Türkçe",!"Türkçe".equals(JDGeoCode.getNativeLanguage("tr")));
        

    }
}
