(function() {
	var initData = verificationResponse;
logger(initData);
	ret= {};
	ret.contentType=initData[7][4][1][0];
	ret.x=initData[7][4][1][4];
	ret.y=initData[7][4][1][3];
	ret.challengeType=initData[7][5];
	if(	typeof(initData[7][4][1][1]) !== 'undefined' ){
		ret.explainUrl=initData[7][4][1][1];
	}


	return ret;
	
//	 var Ko = function(a) {
//	        switch (a) {
//	        case "default":
//	            return new mo;
//	        case "nocaptcha":
//	            return new Eo;
//	        case "imageselect":
//	            return new so;
//	        case "tileselect":
//	            return new so;
//	        case "dynamic":
//	            return new xo;
//	        case "audio":
//	            return new go;
//	        case "text":
//	            return new Jo
//	        }
//	    }
})()