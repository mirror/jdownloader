$(function(){
	/************* JD.lng **************************************************************************
	* Get a localized String.										    						   *
	* If there are no parameters you can directly use JD.lng.KEYNAME to get the desired String.    *	
	* If there are Parameters you can call JD.lng as Function: JD.lng(KEY,PARAM1,PARAM2,...)       *
	***********************************************************************************************/
	var languageStrings = JD.lng;
	JD.lng = function(key){
		var str = JD.lng[key];
		var args = JD.lng.arguments;
		for(var ind = 1; ind < args.length; ind++)
			str = str.replace(new RegExp("%s"+ind,"g"), args[ind]);
			//g = global -> Replace each occourence
		return str;
	};
	$.extend(JD.lng,languageStrings);
	
	
	$("head>title").text(JD.lng.title); //Set Title
	$("head>script").remove();
});



