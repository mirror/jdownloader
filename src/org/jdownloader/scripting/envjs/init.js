// This file is compiled into the jar and executed automatically on startup.





   
    

(function(__this__) {
        var cached = {};
        var envjsGlobals={};


       var  getCurrentContext=net.sourceforge.htmlunit.corejs.javascript.Context.getCurrentContext;
       var javaInstance=Packages.org.jdownloader.scripting.envjs.EnvJS.get(%EnvJSinstanceID%);   
       javaInstance.preInitBoolean(true,true);
       javaInstance.preInitInteger(0,0);
       javaInstance.preInitLong(0,0);
       javaInstance.preInitDouble(0.0,0.0);
       javaInstance.preInitFloat(0.0,0.0);
       
       javaInstance.setGlobals(envjsGlobals);
  
        var require= function(id) {
    
          
          
      
            if (!cached.hasOwnProperty(id)) {
                print('require :'+ id);
                var source = ""+javaInstance.readRequire(id);      
                source = source.replace(/^\#\!.*/, '');
                source = (
                    "(function (envjsGlobals,require, exports, module,javaInstance,__this__) { " + source + "\n});");
                cached[id] = {
                    exports: __this__,
                    module: {
                        id: id,
                        uri: id
                    }
                };
   
                
                try {
     
                    var ctx = getCurrentContext();
                    var func = ctx.evaluateString({}, source, id, 1, null);
                 
                    func(envjsGlobals,require, cached[id].exports, cached[id].module,javaInstance,__this__);
                } finally {
                   
                }
            }
    		/*
             * print('returning exports for id: '+id+' '+cached[id].exports);
             * for(var prop in cached[id].exports){ print('export: '+prop); }
             */
            return cached[id].exports;
        };
        
        require('envjs/platform/core');

        require('local_settings');

        require('envjs/platform/rhino');

        require('envjs/window');
        
        return require;
    }(this));




//init window





new Window(this);


