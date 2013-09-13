
(function(){
	
	var log4js = require('log4js');
	log4js.configure({
	  "appenders": [
	      {
	          type: "console"
	        , category: "console"
	      }
	      ,
	      {
	          "type"      : "file"
	        , "filename"  : "server.log"
	        , "maxLogSize": 1024 * 1000
	        , "backups"   : 3
	        , "category"  : "server"
	      }
	      ,
	      {
	          "type"      : "file"
	        , "filename"  : "rpc.log"
	        , "maxLogSize": 1024 * 1000
	        , "backups"   : 3
	        , "category"  : "rpc"
	      }
	      ,
	      {
	          "type"      : "file"
	        , "filename"  : "spider.log"
	        , "maxLogSize": 1024 * 1000
	        , "backups"   : 3
	        , "category"  : "spider"
	      }
	  ]
	  ,
	  replaceConsole: true
	  ,
	  levels : {
		  'server'   : 'INFO'
		, 'rpc'      : 'INFO'
		, 'console'  : 'INFO'
		, 'spider'   : 'INFO'
	  }
	});
	
	var logger = log4js.getLogger("server");
	
	// STEP 1: configure the requirejs
	var requirejs = require('requirejs');
	requirejs.config({
		  nodeRequire: require
		, baseUrl    : __dirname + '/public/javascripts'
		, catchError : true
	});
	
    requirejs.onError = function(e) {
    	logger.error(e);
	}
	
	// STEP 2: setup the express web server
	var express = require('express')
	  , routes  = require('./routes')
	  , http    = require('http')
	  , io      = require("socket.io")
	  , rpc     = requirejs('rpc')
	  , path    = require('path')
	  , app     = express() 
	  , port    = process.env.VCAP_APP_PORT || 80 ;
	
	// STEP 2.1: configure express
	app.set('port' , port );
	app.set('views', __dirname + '/views');
	app.set('view engine', 'jade');
	app.use(express.favicon());
	app.use(express.logger('dev'));
	app.use(express.bodyParser());
	app.use(express.methodOverride());
	
	app.use(app.router);
	app.use(express.static(path.join(__dirname, 'public')));
	app.use(express.static(path.join(__dirname, 'node_modules')));
	
	// STEP 2.2: development only
	if ('development' == app.get('env')) {
	  app.use(express.errorHandler());
	}
	
	// STEP 2.3: setup the routers
	app.get('/', routes.index);
		
	// STEP 3: start the web server
	var server = http.createServer(app);
	server.listen(app.get('port'), function(){
		logger.info('express server listening on port ' + app.get('port'));
	});
	
	// STEP 4: setup socket.io server
	io.listen(server,{'log level' : 2}).sockets.on('connection', function (socket) {
		rpc.getInstance(socket);
	});	
	console.log('Server running at http://127.0.0.1:' + port );  
})();
