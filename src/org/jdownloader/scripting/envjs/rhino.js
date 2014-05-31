// This file is compiled into the jar and executed automatically on startup.
var __this__ = this;
javaInstance=Packages.org.jdownloader.scripting.envjs.EnvJS.get(EnvJSinstanceID);

   
    

    var require = (function() {
        var cached = {};
       


  
        function require(id) {
    		//print('require :'+ id);
          
          
      
            if (!cached.hasOwnProperty(id)) {
                var source = ""+javaInstance.readRequire(id);
          
                source = source.replace(/^\#\!.*/, '');
                source = (
                    "(function (require, exports, module) { " + source + "\n});");
                cached[id] = {
                    exports: __this__,
                    module: {
                        id: id,
                        uri: id
                    }
                };
         
                try {
           
                    var ctx = org.mozilla.javascript.Context.getCurrentContext();
                    var func = ctx.evaluateString({}, source, id, 1, null);
                    func(require, cached[id].exports, cached[id].module);
                } finally {
                   
                }
            }
    		/*
    		print('returning exports for id: '+id+' '+cached[id].exports);
    		for(var prop in cached[id].exports){
    			print('export: '+prop);
    		}
    		*/
            return cached[id].exports;
        };
        
  
        
        return require;
    }());
var __argv__ = [];
require('envjs/platform/rhino');
require('envjs/window');




