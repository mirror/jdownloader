$(document).ready(function() {
	/* LOADCONTENT */
	function loadContent(hash) {
		var subd = "";
		if(hash.split("?=")[1]) {
			var splitted = hash.split("?=");
			var hash = 	splitted[0];
			var subd = splitted[1]
		}
		if(hash != "") {
			$(".active").removeClass("active");
			$("#nav_"+hash).addClass("active");
			$.ajax({
				url: "pages/"+ hash +".html?mode="+subd,
				success: function(res){
					$('#content').html(res);
				}
			});
		}
	}
	
	
	/* LOGIN */
	if($.fn.placeholder) {
		$('[placeholder]').placeholder();
	}
	
	$.jd.setOptions({
			apiServer : "http://127.0.0.1:3128/",
			debug: true//,
			//apiTimeout : 1000
		}).send('jd/version', onConnect, onConnectError);
		
	
	function onConnect() {
		$.jd.setOptions({
			user : "jd",
			pass : "jd"
		}).startSession(onAuth);
	}
	function onConnectError() {
		alert(":(");	
	}
	function onAuth(resp) {

		if (resp.status == $.jd.e.sessionStatus.REGISTERED) {
			getCaptchas();
			$.jd.subscribe("captcha", onCaptcha);
			$.jd.startPolling();
		} else {
			alert("Authentication failed!");
		}
	}
	
	/* HISTORY */
	$.history.init(loadContent);
	$('#jd-ul_nav li a').not(".jd-report").click(function(e) {
		var url = $(this).attr('href');
		url = url.replace(/^.*#/, '');
		$.history.load(url);
		return false;
	});
	if(!location.href.split("#")[1]) {
		loadContent("dashboard");
	}
	
	
	/* TRANSLATE IT */
	$.i18n.setDictionary(jd__de_de);
	translate();
	
	
	/* DIALOGS */
	$("#jd-captcha-dialog").dialog({
		autoOpen: false, 
		title: "Captcha!", 
		modal: true, 
		width: "640", 
		buttons: [{ text: "Absenden",  click: function() { CaptchaHandler.solve(); $( this ).dialog( "close" ); }}]
	});
	
	function convert(f,mode) {
		if(mode=="kb") {
			return Math.round(f.byte.value/1024*100000)/100000;
		} else if(mode=="mb") {
			return Math.round(f.byte.value/1048576*100000)/100000;
		} else if(mode=="gb") {
			return Math.round(f.byte.value/1073741824*100000)/100000;
		} else {
			return f;
		}
	} 
});
/* GET HANDLERS */
// $.getScript("js/handlers/LoginHandler.js");
$.getScript("js/handlers/AccountHandler.js");
$.getScript("js/handlers/CaptchaHandler.js");


function stamptodate(stamp) {
	var dat =  new Date(stamp);
	var year = dat.getFullYear();
	var mon = dat.getMonth() + 1 ;
	var day = dat.getDate();
	var hour = dat.getHours();
	var minute = dat.getMinutes();
	return day+"."+mon+"."+year+" "+hour+":"+minute;
}
function do_premium_traffic(left,full) {
	if(left=="-1" && full=="-1") {
		return "premium.table.traffic.unlimited";	
	} else {
		return left+"/"+full;
	}
}
