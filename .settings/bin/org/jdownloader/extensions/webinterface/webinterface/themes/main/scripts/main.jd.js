setTimeout('jdstart();',3000);
function jdstart() {
	$.history.init(loadContent);
	if(!location.href.split("#")[1]) {
		loadContent("dashboard");
	}
	$("#jd-captcha-dialog").dialog({
		autoOpen: false, 
		title: $.i18n._('captcha.title'), 
		modal: true, 
		width: "640", 
		buttons: [{ text: $.i18n._('captcha.send'),  click: function() { CaptchaHandler.solve(); $( this ).dialog( "close" ); }}]
	});
	LoginHandler.start("127.0.0.1","3128","jd","jd");
}
function loadContent(hash) {
	var subd = "";
	if(hash.split("?=")[1]) {
		var splitted = hash.split("?=");
		var hash = 	splitted[0];
		var subd = splitted[1]
	}
	if(!hash) { hash = "dashboard"; }
	$(".active").removeClass("active");
	$("#nav_"+hash).addClass("active");
	TemplateAjax(themepath+"pages/"+ hash +".html?mode="+subd,"#content","html");
}
$('#jd-ul_nav li a').not(".jd-report").click(function(e) {
	var url = $(this).attr('href');
	url = url.replace(/^.*#/, '');
	$.history.load(url);
	return false;
});
