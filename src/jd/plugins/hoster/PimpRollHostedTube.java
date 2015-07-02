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

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

/**
 * hostedtube template by pimproll. These guys have various scripts. This is the most common.
 *
 * @see http://www.hostedtube.com/
 * @see http://www.pimproll.com/support.html
 * @linked to wankz.com which still shows "Protranstech BV. DBA NetPass, Postbus 218. ljmudien, 1970AE, Netherlands" in footer message.
 * @linked to porn.com simliar script, but not hosting any image data on hostedtube, and uses old url structure.
 *
 * @author raztoki
 *
 */
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {}, flags = {})
public class PimpRollHostedTube extends PluginForHost {

    public static String[] t = { "2chicks.com", "2chicks.com", "2hd.com", "2hd.com", "3danimesluts.com", "3danimesluts.com", "3dvideospornos.com", "3dvideospornos.com", "4pussylovers.com", "4pussylovers.com", "4tube.tv", "4tube.tv", "18cache.com", "18cache.com", "18crew.com", "18crew.com", "18xr.com", "18xr.com", "20xxxrealitysites.com", "20xxxrealitysites.com", "22porn.com", "22porn.com", "24x7gays.com", "24x7gays.com", "69hornytube.com", "69hornytube.com", "69xxx.net", "69xxx.net", "70porn.com", "70porn.com", "91porn.us", "91porn.us", "110percentamateur.com", "110percentamateur.com", "420camgirls.com", "420camgirls.com", "adult-lock.com", "adult-lock.com", "adultanimemovies.com", "adultanimemovies.com", "adultchoice.com", "adultchoice.com", "adultepass.com", "adultepass.com", "adulteroticcartoon.net", "adulteroticcartoon.net", "adultfemale.com", "adultfemale.com",
            "adultfilmconnection.com", "adultfilmconnection.com", "adultfilmflux.com", "adultfilmflux.com", "adulthentaitvxxx.com", "adulthentaitvxxx.com", "adultmagazine.com", "adultmagazine.com", "adultmodelindex.com", "adultmodelindex.com", "adultsexxxtoys.com", "adultsexxxtoys.com", "adultvideoarcade.com", "adultvideoarcade.com", "adultxxx.xxx", "adultxxx.xxx", "adultxxxpornmovies.com", "adultxxxpornmovies.com", "africanfucktour.org", "africanfucktour.org", "africansexgirlfriend.com", "africansexgirlfriend.com", "agirls.com", "agirls.com", "allasiantgp.com", "allasiantgp.com", "allhardcoretgp.com", "allhardcoretgp.com", "amateurcat.com", "amateurcat.com", "amatuerteentube.com", "amatuerteentube.com", "amazingpornsearch.com", "amazingpornsearch.com", "americanporntube.net", "americanporntube.net", "analcomics.com", "analcomics.com", "angkorporn.com", "angkorporn.com",
            "animeporntubehd.com", "animeporntubehd.com", "animeyhentai.com", "animeyhentai.com", "apetube.eu", "apetube.eu", "appletvpornmovies.com", "appletvpornmovies.com", "arsqualita.com", "arsqualita.com", "asian-porn.xxx", "asian-porn.xxx", "asianbdsmtube.com", "asianbdsmtube.com", "asianlesbianlovers.com", "asianlesbianlovers.com", "asianporn.me", "asianporn.me", "asianporn.mobi", "asianporn.mobi", "asiansitepass.com", "asiansitepass.com", "asiantranny.xxx", "asiantranny.xxx", "assfreeass.com", "assfreeass.com", "atlantisgirls.com", "atlantisgirls.com", "australiaporn4all.com", "australiaporn4all.com", "babes.xxx", "babes.xxx", "bangsisters.com", "bangsisters.com", "bbcgirls.com", "bbcgirls.com", "beautyqueensexvideos.com", "beautyqueensexvideos.com", "bedroomgraphics.com", "bedroomgraphics.com", "beeg.be", "beeg.be", "beegltd.com", "beegltd.com", "belasnegras.com",
            "belasnegras.com", "bellaporn.com", "bellaporn.com", "bestporno.net", "bestporno.net", "bestporntube.com", "bestporntube.com", "bigfullmovies.com", "bigfullmovies.com", "bigpenises.com", "bigpenises.com", "bigtitcomics.com", "bigtitcomics.com", "bigtitsmaid.com", "bigtitsmaid.com", "bigwetcunts.com", "bigwetcunts.com", "bikerwhore.com", "bikerwhore.com", "bindysbabes.com", "bindysbabes.com", "bisexualpornhdtube.com", "bisexualpornhdtube.com", "bisexualpov.com", "bisexualpov.com", "bisexualstory.com", "bisexualstory.com", "bisexualvideotube.com", "bisexualvideotube.com", "bizarreporntube.com", "bizarreporntube.com", "bizarresex.net", "bizarresex.net", "bizarrevidz.com", "bizarrevidz.com", "bizarrre.com", "bizarrre.com", "bizzarevideotube.com", "bizzarevideotube.com", "blackafricanass.com", "blackafricanass.com", "blackhoesporn.com", "blackhoesporn.com", "blackknockers.com",
            "blackknockers.com", "blacklabeltube.com", "blacklabeltube.com", "blackpussyporn.net", "blackpussyporn.net", "blacksextube.net", "blacksextube.net", "blacktube.xxx", "blacktube.xxx", "blacktubevideos.net", "blacktubevideos.net", "blackwhores.xxx", "blackwhores.xxx", "blackxxxpornstars.com", "blackxxxpornstars.com", "blessed.xxx", "blessed.xxx", "blowjobpractice.com", "blowjobpractice.com", "bobogrey.com", "bobogrey.com", "bondagefortress.com", "bondagefortress.com", "bondagevideos.xxx", "bondagevideos.xxx", "boobworx.com", "boobworx.com", "bootyspank.com", "bootyspank.com", "bpbr.net", "bpbr.net", "brasileiro.xxx", "brasileiro.xxx", "brazzerscasting.com", "brazzerscasting.com", "brutalxxxporn.com", "brutalxxxporn.com", "bubblebuttboy.com", "bubblebuttboy.com", "bubblebuttboys.com", "bubblebuttboys.com", "buddies.xxx", "buddies.xxx", "buddy.xxx", "buddy.xxx",
            "bukakebath.com", "bukakebath.com", "bukkakecumshots.com", "bukkakecumshots.com", "bust4.com", "bust4.com", "buttbashers.com", "buttbashers.com", "calendargirls.com", "calendargirls.com", "cambodialover.com", "cambodialover.com", "cambodianstar.com", "cambodianstar.com", "cambodiavip.com", "cambodiavip.com", "cambodiax.com", "cambodiax.com", "capitalpork.com", "capitalpork.com", "cartoonpornpass.com", "cartoonpornpass.com", "cartoonsmaster.com", "cartoonsmaster.com", "celeb-sex-videos.com", "celeb-sex-videos.com", "celebpornpass.com", "celebpornpass.com", "celebritygirl.com", "celebritygirl.com", "cfnmhdtube.com", "cfnmhdtube.com", "cfnmthings.com", "cfnmthings.com", "chinafreesex.com", "chinafreesex.com", "chocolatesistaz.com", "chocolatesistaz.com", "chumleaf.info", "chumleaf.info", "churchofsexology.com", "churchofsexology.com", "clip4.xxx", "clip4.xxx", "clips4.xxx",
            "clips4.xxx", "cloudhentai.com", "cloudhentai.com", "cluborgasm.com", "cluborgasm.com", "clubsixtynine.com", "clubsixtynine.com", "cockhut.com", "cockhut.com", "cocksucker.co.uk", "cocksucker.co.uk", "condomfree.xxx", "condomfree.xxx", "contentadult.com", "contentadult.com", "cougarfuckclub.com", "cougarfuckclub.com", "cougarx.com", "cougarx.com", "cougarxxx.info", "cougarxxx.info", "creampiewomen.com", "creampiewomen.com", "cuckoldtubevids.com", "cuckoldtubevids.com", "cumseed.com", "cumseed.com", "daddylongtime.com", "daddylongtime.com", "dailyhardcorestories.com", "dailyhardcorestories.com", "deluxelist.com", "deluxelist.com", "descargarpelisporno.com", "descargarpelisporno.com", "describe.xxx", "describe.xxx", "dialastud.com", "dialastud.com", "digimonyaoi.com", "digimonyaoi.com", "dirtyblacksluts.com", "dirtyblacksluts.com", "dirtymovie.com", "dirtymovie.com",
            "dirtyporncomics.com", "dirtyporncomics.com", "dirtytube.net", "dirtytube.net", "dirtyweeman.com", "dirtyweeman.com", "djwhores.com", "djwhores.com", "dominatrixescorts.com", "dominatrixescorts.com", "download-free-live-sex-cams.com", "download-free-live-sex-cams.com", "download-free-sex-chat.com", "download-free-sex-chat.com", "drfetish.org", "drfetish.org", "drtubermovies.com", "drtubermovies.com", "dumpporn.com", "dumpporn.com", "dyketube.com", "dyketube.com", "ebonylesbianslove.com", "ebonylesbianslove.com", "ebonytubehd.com", "ebonytubehd.com", "ebonytubemovies.com", "ebonytubemovies.com", "ellcanior.com", "ellcanior.com", "enemapov.com", "enemapov.com", "erectionisland.com", "erectionisland.com", "eroticalphotography.com", "eroticalphotography.com", "eroticclipart.com", "eroticclipart.com", "eroticsmut.com", "eroticsmut.com", "errvids.com", "errvids.com",
            "eurogirlz.com", "eurogirlz.com", "everyday69.com", "everyday69.com", "evilhub.com", "evilhub.com", "ex-bfs.com", "ex-bfs.com", "excambodia.com", "excambodia.com", "exclamate-1-nude-xxx-centerfolds-pin-up-modes-pussy-pictures.com", "exclamate-1-nude-xxx-centerfolds-pin-up-modes-pussy-pictures.com", "exclusivelesbians.com", "exclusivelesbians.com", "explicit.com", "explicit.com", "extremedr.com", "extremedr.com", "extremefantasy.com", "extremefantasy.com", "extremefemales.com", "extremefemales.com", "extremeorgasm.com", "extremeorgasm.com", "fabtr.com", "fabtr.com", "fap4der.com", "fap4der.com", "fapple.com", "fapple.com", "farkingridiculous.com", "farkingridiculous.com", "farmerotica.com", "farmerotica.com", "farthole.com", "farthole.com", "feetpornvideos.com", "feetpornvideos.com", "femalebody.com", "femalebody.com", "filmepornogratis.us", "filmepornogratis.us",
            "firsttimeanal.org", "firsttimeanal.org", "fistfullofcum.com", "fistfullofcum.com", "fraspi.com", "fraspi.com", "freakofporn.com", "freakofporn.com", "free-p-o-r-n-videos.com", "free-p-o-r-n-videos.com", "free-porn-videos.co", "free-porn-videos.co", "free-tube-porn.com", "free-tube-porn.com", "free-x.xxx", "free-x.xxx", "free6.mobi", "free6.mobi", "free6clips.com", "free6clips.com", "freeadultsite.eu", "freeadultsite.eu", "freeasianvideos.net", "freeasianvideos.net", "freeboobtube.com", "freeboobtube.com", "freecartoonpornhdtube.com", "freecartoonpornhdtube.com", "freedownloadpornvideo.com", "freedownloadpornvideo.com", "freefuckvidz.xxx", "freefuckvidz.xxx", "freegaypornhdtube.com", "freegaypornhdtube.com", "freelesbianpornhdtube.com", "freelesbianpornhdtube.com", "freemobileporn.tv", "freemobileporn.tv", "freemobilevidz.com", "freemobilevidz.com",
            "freepornadultvideos.com", "freepornadultvideos.com", "freepornfriend.com", "freepornfriend.com", "freepornint.com", "freepornint.com", "freepornonline.info", "freepornonline.info", "freepornoserver.com", "freepornoserver.com", "freepornproject.com", "freepornproject.com", "freepornvideos.name", "freepornvideos.name", "freepornwhiz.com", "freepornwhiz.com", "freeprontube.net", "freeprontube.net", "freesex.info", "freesex.info", "freesex22.com", "freesex22.com", "freesexint.com", "freesexint.com", "freesexmembership.net", "freesexmembership.net", "freesexonvideo.com", "freesexonvideo.com", "freesexpeeps.com", "freesexpeeps.com", "freesexporn.xxx", "freesexporn.xxx", "freesextube.xxx", "freesextube.xxx", "freesexvidio.us", "freesexvidio.us", "freesmutclub.com", "freesmutclub.com", "freetubes.xxx", "freetubes.xxx", "freetubexxl.com", "freetubexxl.com", "freexxx.org",
            "freexxx.org", "freexxxx.xxx", "freexxxx.xxx", "freshpornpic.com", "freshpornpic.com", "fuck-tube.org", "fuck-tube.org", "fuck13.com", "fuck13.com", "fuckflix.net", "fuckflix.net", "fucks.se", "fucks.se", "fuckstains.com", "fuckstains.com", "fukpics.com", "fukpics.com", "full-porno.com", "full-porno.com", "funroyal.com", "funroyal.com", "funsexteen.com", "funsexteen.com", "funsexworld.com", "funsexworld.com", "furrycunt.com", "furrycunt.com", "fvdpornmovies.com", "fvdpornmovies.com", "galaxysex.com", "galaxysex.com", "galore4.com", "galore4.com", "galorehub.com", "galorehub.com", "gay-porn.xxx", "gay-porn.xxx", "gay-xxx.xxx", "gay-xxx.xxx", "gayasslickingvideos.com", "gayasslickingvideos.com", "gaybrazil.xxx", "gaybrazil.xxx", "gaycollegesex.com", "gaycollegesex.com", "gayflicks.com", "gayflicks.com", "gayinteracial.com", "gayinteracial.com", "gaymoz.com", "gaymoz.com",
            "gayrotica.com", "gayrotica.com", "gaysexonline.com", "gaysexonline.com", "gaytubehub.com", "gaytubehub.com", "germanyfreeporn.com", "germanyfreeporn.com", "getmywomen.com", "getmywomen.com", "gigatube.com", "gigatube.com", "girllust.com", "girllust.com", "girlnextwhore.com", "girlnextwhore.com", "girlshow4u.com", "girlshow4u.com", "girlsmasterbating.com", "girlsmasterbating.com", "girlsshowering.com", "girlsshowering.com", "girlstube.net", "girlstube.net", "gnomeporn.com", "gnomeporn.com", "gonzogash.com", "gonzogash.com", "gonzoglamour.com", "gonzoglamour.com", "gosex.net", "gosex.net", "grannypornstar.com", "grannypornstar.com", "gratisporrfilm.nu", "gratisporrfilm.nu", "greatporndownloads.com", "greatporndownloads.com", "hankyporn.com", "hankyporn.com", "hardcorelezbos.com", "hardcorelezbos.com", "hardcoreporntrailer.com", "hardcoreporntrailer.com", "hardcoretwinks.com",
            "hardcoretwinks.com", "hctube.com", "hctube.com", "hd-movies.info", "hd-movies.info", "hd-porntv.net", "hd-porntv.net", "hd-sextv.net", "hd-sextv.net", "hentai-files.com", "hentai-files.com", "hentaianimehd.com", "hentaianimehd.com", "hentaionline.com", "hentaionline.com", "hentaiorgy.com", "hentaiorgy.com", "hentaiporno.us", "hentaiporno.us", "hentaiporntube.org", "hentaiporntube.org", "hentaisexvideos.pw", "hentaisexvideos.pw", "hidefporn.org", "hidefporn.org", "highpoweredpussy.com", "highpoweredpussy.com", "hismut.com", "hismut.com", "homotube.nl", "homotube.nl", "hornyslutmoms.com", "hornyslutmoms.com", "hot-porn.us", "hot-porn.us", "hotarabgirl.com", "hotarabgirl.com", "hotcamgirls.net", "hotcamgirls.net", "hoteighteen.com", "hoteighteen.com", "hothsexd.com", "hothsexd.com", "hotliveaction.com", "hotliveaction.com", "hotmalegaytube.com", "hotmalegaytube.com",
            "hotporn.co.uk", "hotporn.co.uk", "hottube.xxx", "hottube.xxx", "hotxvideos.xxx", "hotxvideos.xxx", "hub4.tv", "hub4.tv", "hub4.xxx", "hub4.xxx", "hubdr.com", "hubdr.com", "hublube.com", "hublube.com", "hugecockpornpass.com", "hugecockpornpass.com", "hugedickpornpass.com", "hugedickpornpass.com", "ieatsperm.com", "ieatsperm.com", "ifrancefreeporn.com", "ifrancefreeporn.com", "ifuck.se", "ifuck.se", "ifuckedyourgirlfriend.com", "ifuckedyourgirlfriend.com", "ifuckedyourmama.com", "ifuckedyourmama.com", "ifucku.xxx", "ifucku.xxx", "iizzi.com", "iizzi.com", "ilesbain.com", "ilesbain.com", "iloveporno.com", "iloveporno.com", "impregnation.net", "impregnation.net", "impulsive.xxx", "impulsive.xxx", "indiafreeporntube.com", "indiafreeporntube.com", "indian-sex.xxx", "indian-sex.xxx", "internetvideounravelled.com", "internetvideounravelled.com", "interracialcomix.com",
            "interracialcomix.com", "ismytube.biz", "ismytube.biz", "itouchyourmom.com", "itouchyourmom.com", "itube.com", "itube.com", "japenxxx.com", "japenxxx.com", "jasonoakley.me", "jasonoakley.me", "jenavevejolie.com", "jenavevejolie.com", "jerk4.com", "jerk4.com", "jessiam.com", "jessiam.com", "jizjob.com", "jizjob.com", "jizz-on.com", "jizz-on.com", "jizzdr.com", "jizzdr.com", "jizzglazed.com", "jizzglazed.com", "jizzjobs.com", "jizzjobs.com", "jizzjobs.xxx", "jizzjobs.xxx", "jizzsoup.com", "jizzsoup.com", "jizzwire.com", "jizzwire.com", "joporn.com", "joporn.com", "juicypussytube.com", "juicypussytube.com", "justaveragesluts.com", "justaveragesluts.com", "kalyhot.com", "kalyhot.com", "khmer69.com", "khmer69.com", "khmer111.com", "khmer111.com", "khmersextube.com", "khmersextube.com", "knepper.nu", "knepper.nu", "kudtube.com", "kudtube.com", "ladysnatch.com", "ladysnatch.com",
            "latenightvideos.com", "latenightvideos.com", "lay.xxx", "lay.xxx", "lbosexvideos.com", "lbosexvideos.com", "leahremininude.com", "leahremininude.com", "lesbian-sex.xxx", "lesbian-sex.xxx", "lesbianpussy.org", "lesbianpussy.org", "lesbiansweb.com", "lesbiansweb.com", "lesbianxxxvideo.com", "lesbianxxxvideo.com", "lesbicaporno.com", "lesbicaporno.com", "lethalmobile.com", "lethalmobile.com", "letsbangthebabysitter.com", "letsbangthebabysitter.com", "likepussytube.com", "likepussytube.com", "littlesexthings.com", "littlesexthings.com", "livesexhq.com", "livesexhq.com", "loadedbox.com", "loadedbox.com", "lolylporno.com", "lolylporno.com", "lopussy.com", "lopussy.com", "loveinjection.com", "loveinjection.com", "lubebucket.com", "lubebucket.com", "makingaporno.com", "makingaporno.com", "maleorgy.xxx", "maleorgy.xxx", "malepornxxx.com", "malepornxxx.com", "maletube.org",
            "maletube.org", "mamacitavideos.com", "mamacitavideos.com", "mangaporno.us", "mangaporno.us", "mastersofjohnson.com", "mastersofjohnson.com", "maturehot.com", "maturehot.com", "maturesexfreaks.com", "maturesexfreaks.com", "maximumpussy.com", "maximumpussy.com", "maximumsexxx.com", "maximumsexxx.com", "mediastoragemadeeasy.com", "mediastoragemadeeasy.com", "megamovies.xxx", "megamovies.xxx", "menandmen.com", "menandmen.com", "milfboard.com", "milfboard.com", "milfexperts.com", "milfexperts.com", "milfhdporn.com", "milfhdporn.com", "milfphoneporn.com", "milfphoneporn.com", "milfsexmovies.net", "milfsexmovies.net", "milfsporn.net", "milfsporn.net", "milftuba.com", "milftuba.com", "milftube4u.com", "milftube4u.com", "milftubeporn.com", "milftubeporn.com", "milfucker.com", "milfucker.com", "milfvideos.eu", "milfvideos.eu", "milfvideotube.com", "milfvideotube.com", "misiko.com",
            "misiko.com", "mobile-hentai-tube.com", "mobile-hentai-tube.com", "mobileanimepass.com", "mobileanimepass.com", "mobilecartoontube.com", "mobilecartoontube.com", "mobilemilfpass.com", "mobilemilfpass.com", "mofoscasting.com", "mofoscasting.com", "mofoshub.com", "mofoshub.com", "momsfuckteens.com", "momsfuckteens.com", "momspimptheirdaughter.com", "momspimptheirdaughter.com", "momvids.com", "momvids.com", "most-porn.com", "most-porn.com", "mostporno.com", "mostporno.com", "motherporno.com", "motherporno.com", "movies-portal.com", "movies-portal.com", "moviesex.xxx", "moviesex.xxx", "moviesexcams.com", "moviesexcams.com", "moviesxxx.xxx", "moviesxxx.xxx", "mphoneporn.com", "mphoneporn.com", "mr-xxx.com", "mr-xxx.com", "mupp.se", "mupp.se", "mys.xxx", "mys.xxx", "myxxxplay.com", "myxxxplay.com", "n-u-d-e.com", "n-u-d-e.com", "nakedmovienews.com", "nakedmovienews.com",
            "nashvilleporn.com", "nashvilleporn.com", "nastyblog.com", "nastyblog.com", "nastyhub.com", "nastyhub.com", "nastyporntube.net", "nastyporntube.net", "naughty4.com", "naughty4.com", "naughty8.com", "naughty8.com", "nausty.com", "nausty.com", "ninjaxxx.com", "ninjaxxx.com", "nobotv.com", "nobotv.com", "nudenakedindianwomen.com", "nudenakedindianwomen.com", "nutten.ru", "nutten.ru", "nxsnacksvip.com", "nxsnacksvip.com", "nysportexchange.com", "nysportexchange.com", "ogrishvideos.com", "ogrishvideos.com", "ogrishxxx.com", "ogrishxxx.com", "ohsluts.com", "ohsluts.com", "onlypornclips.com", "onlypornclips.com", "orientalpornsearch.com", "orientalpornsearch.com", "paris-porn.net", "paris-porn.net", "parishilten.com", "parishilten.com", "pcgn.com", "pcgn.com", "penisaurus.com", "penisaurus.com", "penissaurus.com", "penissaurus.com", "perfectplatinum.com", "perfectplatinum.com",
            "persiankittyvidz.com", "persiankittyvidz.com", "personalsex.com", "personalsex.com", "perverse-passions.com", "perverse-passions.com", "pigyporn.com", "pigyporn.com", "pimptrailers.com", "pimptrailers.com", "pimpvod.com", "pimpvod.com", "pinksvisual.com", "pinksvisual.com", "pipeporno.com", "pipeporno.com", "play4.xxx", "play4.xxx", "playwithmyforeskin.com", "playwithmyforeskin.com", "pleasure.net", "pleasure.net", "pleasureporn.net", "pleasureporn.net", "plexiglas.name", "plexiglas.name", "plumppass.com", "plumppass.com", "porm.com", "porm.com", "porn-18.xxx", "porn-18.xxx", "porn-rabbit.com", "porn-rabbit.com", "porn-tube.com", "porn-tube.com", "porn69tube.com", "porn69tube.com", "pornadult.xxx", "pornadult.xxx", "porndada.com", "porndada.com", "porndl.com", "porndl.com", "porneez.com", "porneez.com", "pornfeed.com", "pornfeed.com", "pornfreevideos.xxx",
            "pornfreevideos.xxx", "porngayhd.com", "porngayhd.com", "porngoldmine.com", "porngoldmine.com", "porninfo.com", "porninfo.com", "pornmegaplex.com", "pornmegaplex.com", "pornmilf.xxx", "pornmilf.xxx", "pornoactual.com", "pornoactual.com", "pornoextra.com", "pornoextra.com", "pornogangsters.com", "pornogangsters.com", "pornogold.com", "pornogold.com", "pornohoes.com", "pornohoes.com", "pornojedi.com", "pornojedi.com", "pornostartryouts.com", "pornostartryouts.com", "pornotube.xxx", "pornotube.xxx", "pornotube21.com", "pornotube21.com", "pornpalazzo.com", "pornpalazzo.com", "pornpalm.com", "pornpalm.com", "pornpeer.com", "pornpeer.com", "pornporky.com", "pornporky.com", "pornray.com", "pornray.com", "pornrss.com", "pornrss.com", "pornsexlinks.com", "pornsexlinks.com", "pornsitepros.com", "pornsitepros.com", "pornstargalore.net", "pornstargalore.net", "pornstarpassion.com",
            "pornstarpassion.com", "pornstarsawards.com", "pornstarsawards.com", "porntoonz.com", "porntoonz.com", "porntube10.com", "porntube10.com", "porntubestv.com", "porntubestv.com", "porntubular.com", "porntubular.com", "pornvideon.com", "pornvideon.com", "pornvideos10.com", "pornvideos10.com", "pornvie.com", "pornvie.com", "porrfilmer.biz", "porrfilmer.biz", "porrfilmer.mobi", "porrfilmer.mobi", "porrfilmgratis.se", "porrfilmgratis.se", "povanalsluts.com", "povanalsluts.com", "pricksucker.com", "pricksucker.com", "primalsexvideos.com", "primalsexvideos.com", "privatepornlinks.com", "privatepornlinks.com", "pron.co", "pron.co", "pronhubltd.com", "pronhubltd.com", "pussyblog.com", "pussyblog.com", "pussyeatingtube.net", "pussyeatingtube.net", "pussyfights.com", "pussyfights.com", "pussyof.com", "pussyof.com", "putastube.com", "putastube.com", "qualitypink.com", "qualitypink.com",
            "r89.com", "r89.com", "ratedpornpremium.com", "ratedpornpremium.com", "ratedsexy.com", "ratedsexy.com", "ratedx.eu", "ratedx.eu", "ratedxmoney.com", "ratedxmoney.com", "ratepenis.com", "ratepenis.com", "realitylist.com", "realitylist.com", "realitystation.com", "realitystation.com", "reallywildteens.com", "reallywildteens.com", "redgayporntube.com", "redgayporntube.com", "redhotdudes.com", "redhotdudes.com", "redtube-com.info", "redtube-com.info", "redtubedb.com", "redtubedb.com", "redzonetube.com", "redzonetube.com", "reet.com", "reet.com", "rubguide.com", "rubguide.com", "rudeboobies.com", "rudeboobies.com", "sadomasochist.net", "sadomasochist.net", "safesextube.com", "safesextube.com", "sarahpalinsextape.org", "sarahpalinsextape.org", "savagesex.com", "savagesex.com", "screwmyspouse.com", "screwmyspouse.com", "screwshack.com", "screwshack.com", "sergeantstiffy.com",
            "sergeantstiffy.com", "sessovideo.us", "sessovideo.us", "sex-cake.com", "sex-cake.com", "sex-film.se", "sex-porno-pics.com", "sex-porno-pics.com", "sex-videos.se", "sex-videos.se", "sex.biz", "sex.biz", "sex2hot.com", "sex2hot.com", "sexadult.xxx", "sexadult.xxx", "sexbyfood.com", "sexbyfood.com", "sexclicks.com", "sexclicks.com", "sexdgbbw.com", "sexdgbbw.com", "sexernet.com", "sexernet.com", "sexfilmer.nu", "sexfilmer.nu", "sexfilmer.se", "sexfilmer.se", "sexfilmsxxx.us", "sexfilmsxxx.us", "sexfindout.com", "sexfindout.com", "sexfreevideos.xxx", "sexfreevideos.xxx", "sexgratis.se", "sexgratis.se", "sexgratisxxx.us", "sexgratisxxx.us", "sexhamster.co", "sexhamster.co", "sexkate.com", "sexkate.com", "sexlifetube.com", "sexlifetube.com", "sexlock.com", "sexlock.com", "sexmalestubes.com", "sexmalestubes.com", "sexmom.org", "sexmom.org", "sexmovielibrary.com",
            "sexmovielibrary.com", "sexmovieporn.com", "sexmovieporn.com", "sexmovieshd.com", "sexmovieshd.com", "sexmoviesxxx.com", "sexmoviesxxx.com", "sexmum.com", "sexmum.com", "sexmys.com", "sexmys.com", "sexnoir.com", "sexnoir.com", "sexostube.com", "sexostube.com", "sexparty.tv", "sexparty.tv", "sexpc.com", "sexpc.com", "sexpicspornvids.com", "sexpicspornvids.com", "sexroots.com", "sexroots.com", "sexsexsex.se", "sexsexsex.se", "sexstarclub.com", "sexstarclub.com", "sextapexxx.com", "sextapexxx.com", "sexthings.com", "sexthings.com", "sextube-6.com", "sextube-6.com", "sextubestv.com", "sextubestv.com", "sextvnews.com", "sextvnews.com", "sexualexploits.com", "sexualexploits.com", "sexualnews.com", "sexualnews.com", "sexualsecret.com", "sexualsecret.com", "sexualurges.net", "sexualurges.net", "sexvidmovs.com", "sexvidmovs.com", "sexviedos.com", "sexviedos.com", "sexwebs.com",
            "sexwebs.com", "sexxxx.xxx", "sexxxx.xxx", "sexyandold.com", "sexyandold.com", "sexyanimevideos.com", "sexyanimevideos.com", "sexyboytube.com", "sexyboytube.com", "sexydaughter.com", "sexydaughter.com", "sexyenema.com", "sexyenema.com", "sexyporncollection.com", "sexyporncollection.com", "sexyrhino.com", "sexyrhino.com", "sexytubeclip.com", "sexytubeclip.com", "sexzones.com", "sexzones.com", "shafty.com", "shafty.com", "shaftytube.com", "shaftytube.com", "sharmota.com", "sharmota.com", "sieix.com", "sieix.com", "simplybabes.com", "simplybabes.com", "sinfultoons.com", "sinfultoons.com", "site4.xxx", "site4.xxx", "skankymoms.com", "skankymoms.com", "skullfucktube.com", "skullfucktube.com", "slavetubes.com", "slavetubes.com", "slutloads.us", "slutloads.us", "smalldickbanger.com", "smalldickbanger.com", "smalldickfucker.com", "smalldickfucker.com", "smalldickfuckers.com",
            "smalldickfuckers.com", "smutboss.com", "smutboss.com", "smuthouse.com", "smuthouse.com", "smutshelf.com", "smutshelf.com", "smutvote.com", "smutvote.com", "soyouthinkyoucanscrew.com", "soyouthinkyoucanscrew.com", "spain-sex.org", "spain-sex.org", "spankingmywife.com", "spankingmywife.com", "spanktank.xxx", "spanktank.xxx", "spermfarts.com", "spermfarts.com", "strapon.se", "strapon.se", "straponlesbians.us", "straponlesbians.us", "stream-online-porn.com", "stream-online-porn.com", "suckpov.com", "suckpov.com", "sugarmanga.com", "sugarmanga.com", "super-porno.com", "super-porno.com", "superpene.tv", "superpene.tv", "superxvideos.net", "superxvideos.net", "swedexxx.com", "swedexxx.com", "sweetchocolatepussy.com", "sweetchocolatepussy.com", "swesex.se", "swesex.se", "swingerhot.net", "swingerhot.net", "swingersex.ca", "swingersex.ca", "swingersmag.com", "swingersmag.com",
            "swingerstube.net", "swingerstube.net", "swingersx.com", "swingersx.com", "syntheticheroin.com", "syntheticheroin.com", "tastyblackpussy.com", "tastyblackpussy.com", "teabaggingpov.com", "teabaggingpov.com", "teamforbidden.com", "teamforbidden.com", "teenbdtube.com", "teenbdtube.com", "teenporn2u.info", "teenporn2u.info", "teenpornocity.com", "teenpornocity.com", "teensteam.com", "teensteam.com", "teenvideos.com", "teenvideos.com", "teenvirgin.org", "teenvirgin.org", "thecumdiet.com", "thecumdiet.com", "thefreeadult.com", "thefreeadult.com", "thefreeasian.com", "thefreeasian.com", "thefreepornotube.com", "thefreepornotube.com", "thefreepornreport.com", "thefreepornreport.com", "thehotbabes.net", "thehotbabes.net", "themakingofporn.com", "themakingofporn.com", "themeatmen.com", "themeatmen.com", "themostgay.com", "themostgay.com", "thepornlegacy.com", "thepornlegacy.com",
            "thesexlotto.com", "thesexlotto.com", "thespermdiet.com", "thespermdiet.com", "thestagparty.com", "thestagparty.com", "thongtubes.com", "thongtubes.com", "throatpokers.com", "throatpokers.com", "thumblordstube.com", "thumblordstube.com", "ticklingpov.com", "ticklingpov.com", "tnaflix.eu", "tnaflix.eu", "toonfucksluts.com", "toonfucksluts.com", "toonsporno.com", "toonsporno.com", "topfemales.com", "topfemales.com", "topsecretporn.net", "topsecretporn.net", "totalfetish.com", "totalfetish.com", "totallytastelessvideos.com", "totallytastelessvideos.com", "traileraddicts.com", "traileraddicts.com", "tramplepov.com", "tramplepov.com", "trashymoms.com", "trashymoms.com", "triplextube.com", "triplextube.com", "tube4.xxx", "tube4.xxx", "tube4jizz.com", "tube4jizz.com", "tube75.com", "tube75.com", "tubegalorexxx.com", "tubegalorexxx.com", "tubesmack.com", "tubesmack.com",
            "tubewetlook.com", "tubewetlook.com", "tubexxx.us", "tubexxx.us", "ultimatestripoff.com", "ultimatestripoff.com", "ultporn.com", "ultporn.com", "ultramoviemadness.com", "ultramoviemadness.com", "ultvid.com", "ultvid.com", "uncensoredtoons.com", "uncensoredtoons.com", "undercoverpussy.com", "undercoverpussy.com", "urbansexvideos.com", "urbansexvideos.com", "urophilia.com", "urophilia.com", "usagirlfriends.com", "usagirlfriends.com", "usapornclub.com", "usapornclub.com", "victoriasilvsted.com", "victoriasilvsted.com", "vid.xxx", "vid.xxx", "videez.com", "videez.com", "video-porn.us", "video-porn.us", "video18.com", "video18.com", "videofetish.com", "videofetish.com", "videolivesex.com", "videolivesex.com", "videosadultfree.com", "videosadultfree.com", "videosp.com", "videosp.com", "videoxxxltd.com", "videoxxxltd.com", "vids.xxx", "vids.xxx", "vidsvidsvids.com",
            "vidsvidsvids.com", "vidxpose.com", "vidxpose.com", "vidz.info", "vidz.info", "vidzmobile.com", "vidzmobile.com", "vietnamsextube.com", "vietnamsextube.com", "violationfactor.com", "violationfactor.com", "vip-babes-world.com", "vip-babes-world.com", "viplounge.xxx", "viplounge.xxx", "vipvidz.com", "vipvidz.com", "vodporn.eu", "vodporn.eu", "voyeurxxxtube.com", "voyeurxxxtube.com", "vudutube.com", "vudutube.com", "wankbucket.com", "wankbucket.com", "watchbooty.com", "watchbooty.com", "watchpornvideos.com", "watchpornvideos.com", "watchsex.net", "watchsex.net", "wetandsticky.com", "wetandsticky.com", "whackov.com", "whackov.com", "whoores.com", "whoores.com", "whorebrowser.com", "whorebrowser.com", "wltube.com", "wltube.com", "worldsbestpornmovies.com", "worldsbestpornmovies.com", "x-movies.xxx", "x-movies.xxx", "x-video.xxx", "x-video.xxx", "xblacktube.com", "xblacktube.com",
            "xcon3.com", "xcon3.com", "xfree.xxx", "xfree.xxx", "xhamsterporno.com", "xhamsterporno.com", "xlamate1-hardcore-xxx-group-fucking-threesome-orgy-pics-sex.com", "xlamate1-hardcore-xxx-group-fucking-threesome-orgy-pics-sex.com", "xmovielove.com", "xmovielove.com", "xnxxltd.com", "xnxxltd.com", "xnxxx.eu", "xnxxx.eu", "xnxxxltd.com", "xnxxxltd.com", "xpoko.com", "xpoko.com", "xpornografia.us", "xpornografia.us", "xpornohd.com", "xpornohd.com", "xporntube.us", "xporntube.us", "xratedweb.com", "xratedweb.com", "xsex.xxx", "xsex.xxx", "xtube.mobi", "xtube.mobi", "xtubegaysex.com", "xtubegaysex.com", "xxnxltd.com", "xxnxltd.com", "xxx-18.xxx", "xxx-18.xxx", "xxx-con.com", "xxx-con.com", "xxxbfs.com", "xxxbfs.com", "xxxcartoonz.com", "xxxcartoonz.com", "xxxcebu.com", "xxxcebu.com", "xxxcreampie.org", "xxxcreampie.org", "xxxdn.com", "xxxdn.com", "xxxfilms.xxx", "xxxfilms.xxx",
            "xxxgangbangfilms.com", "xxxgangbangfilms.com", "xxxgaytube.com", "xxxgaytube.com", "xxxgod.com", "xxxgod.com", "xxxhardcore.us", "xxxhardcore.us", "xxxhardcorexxx.com", "xxxhardcorexxx.com", "xxxhelp.com", "xxxhelp.com", "xxxin3d.com", "xxxin3d.com", "xxxkorea.xxx", "xxxkorea.xxx", "xxxlesbiansexvideos.org", "xxxlesbiansexvideos.org", "xxxlinkshunter.com", "xxxlinkshunter.com", "xxxmobiletubes.com", "xxxmobiletubes.com", "xxxpornvideos.us", "xxxpornvideos.us", "xxxpornx.xxx", "xxxpornx.xxx", "xxxpromos.com", "xxxpromos.com", "xxxsafetube.com", "xxxsafetube.com", "xxxslutty.com", "xxxslutty.com", "xxxthailand.net", "xxxthailand.net", "xxxvideotryouts.com", "xxxvideotryouts.com", "xxxvidz.org", "xxxvidz.org", "xxxxn.com", "xxxxn.com", "yankjobs.com", "yankjobs.com", "youjizz.net", "youjizz.net", "youjizz66.com", "youjizz66.com", "youjizzltd.com", "youjizzltd.com",
            "youlorn.com", "youlorn.com", "youngxxxx.com", "youngxxxx.com", "youpronltd.com", "youpronltd.com", "yourcocktube.com", "yourcocktube.com", "yourfreepornsex.com", "yourfreepornsex.com", "yoursmutpass.com", "yoursmutpass.com", "yozjizz.com", "yozjizz.com", "yufap.com", "yufap.com", "zadmo.com", "zadmo.com", "zippyporn.com", "zippyporn.com", "zmaster.com", "zmaster.comsex-film.se" };

    /**
     * Returns the annotations names array
     *
     * @return
     */
    public static String[] getAnnotationNames() {
        return t;
    }

    /**
     * returns the annotation pattern array
     *
     * @return
     */
    public static String[] getAnnotationUrls() {
        String[] s = new String[t.length];
        for (int ssize = 0; ssize != t.length; ssize++) {
            s[ssize] = "http://(?:(?:www|m)\\.)?" + t[ssize] + "/[\\w\\-]+/\\d+";
        }
        return s;
    }

    /**
     * Returns the annotations flags array
     *
     * @return
     */
    public static int[] getAnnotationFlags() {
        int[] s = new int[t.length];
        for (int ssize = 0; ssize != t.length; ssize++) {
            s[ssize] = 0;
        }
        return s;
    }

    public PimpRollHostedTube(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String DLLINK = null;

    @Override
    public String getAGBLink() {
        return "";
    }

    private static AtomicReference<String> agent = new AtomicReference<String>();

    /**
     * defines custom browser requirements.
     * */
    private Browser prepBrowser(final Browser prepBr) {
        if (agent.get() == null) {
            /* we first have to load the plugin, before we can reference it */
            agent.set(jd.plugins.hoster.MediafireCom.stringUserAgent());
        }
        prepBr.getHeaders().put("User-Agent", agent.get());
        prepBr.getHeaders().put("Accept-Language", "en-gb, en;q=0.8");
        prepBr.setFollowRedirects(true);
        return prepBr;
    }

    public String extFromLink(String ext) {
        // Trimming query_string
        int extEnd = ext.indexOf("?");
        if (extEnd > 0) {
            ext = ext.substring(0, extEnd);
        }
        // Trimming fragment_id
        extEnd = ext.indexOf("#");
        if (extEnd > 0) {
            ext = ext.substring(0, extEnd);
        }
        // Trimming main part
        ext = ext.substring(ext.lastIndexOf("."));
        // Trimming whitespace
        ext = ext.trim();
        // Checking resulting length
        int resultLength = ext.length();
        if (resultLength < 2 || resultLength > 5) {
            ext = null;
        }
        return ext;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        prepBrowser(br);
        br.getPage(downloadLink.getDownloadURL());
        // 404 on desktop page
        if (br.containsHTML("was not found on this server, please try a") || br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        // 404 on mobile page
        if (br.containsHTML("<a href=\"#sorting\">")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<title>([^<>]+)</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<h1[^>]*>([^<>]*?)</h1>").getMatch(0);
        }
        // DLLINK on desktop page
        final String[] qualities = { "1080p", "720p", "480p", "360p", "240p" };
        for (final String quality : qualities) {
            DLLINK = br.getRegex("\"" + quality + "\":\"(http[^<>\"]*?)\"").getMatch(0);
            if (DLLINK != null) {
                break;
            }
        }
        // DLLINK on mobile page
        if (DLLINK == null) {
            DLLINK = br.getRegex("\"(http://[a-z0-9\\-\\.]+movies\\.hostedtube\\.com/[^<>\"]*?)\"").getMatch(0);
        }
        if (filename == null || DLLINK == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        DLLINK = Encoding.htmlDecode(DLLINK);
        filename = filename.trim();
        String ext = extFromLink(DLLINK);
        if (ext == null) {
            ext = ".mp4";
        }
        while (filename.endsWith(".")) {
            filename = filename.substring(0, filename.length() - 1);
        }
        downloadLink.setFinalFileName(Encoding.htmlDecode(filename) + ext);
        final Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(DLLINK);
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    // @Override
    // public Boolean confirmSiteStatus(final Browser br) throws Exception {
    // if (br.containsHTML("(\"|')(?:https?:)?//images\\.hostedtube\\.com/.*?\\1")) {
    // return Boolean.TRUE;
    // }
    // // this template generally has heap of links on the home page... lets check against that first.
    // String[] supported_links = br.getRegex(this.getSupportedLinks()).getColumn(-1);
    // if (supported_links == null || supported_links.length == 0) {
    // supported_links = br.getRegex("(\"|')(/[^/]*videos[^/]*/\\d+)\\1").getColumn(1);
    // }
    // if (supported_links != null && supported_links.length > 0) {
    // return Boolean.TRUE;
    // }
    // supported_links = br.getRegex("(\"|')(/[^/]*videos[^/]*/\\d+)\\1").getColumn(1);
    // logger.warning("Could not find supported links!");
    // if (br.containsHTML(">Embed/RSS/Export Videos</a>")) {
    // logger.info("Fail Over worked");
    // return Boolean.TRUE;
    // }
    // return Boolean.FALSE;
    // }

}
