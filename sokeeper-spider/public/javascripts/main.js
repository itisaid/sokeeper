requirejs.config({
	catchError : true,
	paths : {
		'socket.io' : '/socket.io/socket.io',
		'async'     : '/async/lib/async',
		'log4js'    : 'log4javascript'  
	},
	shim : {
		'log4js' : {
			deps : [],
			exports : 'log4javascript',
			init : function() {
				var logger = log4javascript.getLogger('rpc');
				logger.addAppender(new log4javascript.PopUpAppender());
				logger.setLevel(log4javascript.Level.INFO);

				logger = log4javascript.getLogger('spider');
				logger.addAppender(new log4javascript.PopUpAppender());
				logger.setLevel(log4javascript.Level.DEBUG);

				logger = log4javascript.getDefaultLogger();
				logger.setLevel(log4javascript.Level.INFO);
			}
		}
		,
		'jquery-ui' : {
			deps    : ['jquery'] ,
			exports : 'jQuery.ui'
		}
	}
});

requirejs.onError = function(e) {
	console.error(e);
}

define(['jquery', 'socket.io', 'rpc' , 'log4js' ], function($, io,rpc,log4js) {
    var logger = log4js.getDefaultLogger();
	logger.info('Logger Initialed');	
	
	var socket = io.connect();
	rpc.start(socket);
	var request= rpc.request.bind(socket);
	var call   = rpc.call.bind(socket);
	call('spider','sayHello','James Fu',function(err,msg){
		logger.info(msg);
		call('spider','spideTags');
	});
});
