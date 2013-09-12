define(['socket.io','log4js'],function(io,log4js){
	var logger       = log4js.getLogger("rpc");
	var socketsCache = {} ;
	var socketId     = 0 ;
	
	function RpcWrapper(socket) {
		this.socket = socket ;
		this.acks   = {}     ;
	    this.ackId  = 0      ;
	}
	
	RpcWrapper.prototype.start=function() {
		var socket  = this.socket ;
		var selfThis= this        ;
				
		socket.on('rpc.call',function(cfg){ 
			var module  = null         ;
			var errMsg  = ''           ;
			cfg = cfg || {}            ;
			cfg.args = cfg.args || []  ;
			
			if (!errMsg && !cfg.module) {
				errMsg = 'module not found from the call' ;
			}
			if (!errMsg && !cfg.method) {
				errMsg = 'method not found from the call' ;
			}
			if (!errMsg) {
				requirejs([cfg.module],(function(errMsg,cfg,socket,module){
					var response= null ;					
					if (!module) {
						errMsg = 'can not find module:' + cfg.module ;
					}
					if (!errMsg && !module[cfg.method]) {
						errMsg = 'no method: ' + cfg.method + ' found from module:' + cfg.module ;
					}					
					if (!errMsg) { 
						if (module[cfg.method].async) {
							cfg.args.push((function(cfg,errMsg,response){
								this.emit('rpc.response' , {ackId: cfg.ackId , response : response , error : errMsg });
							}).bind(socket,cfg)) ;
							module[cfg.method].apply(selfThis,cfg.args) ;
						} else {
							try {
							    response = module[cfg.method].apply(selfThis,cfg.args);
							} catch (e) {
								errMsg = '' + e ;	
								logger.error('call ' + cfg.module + '.' + cfg.method + ' failed' , e);
							} 
							socket.emit('rpc.response' , { ackId: cfg.ackId , response : response , error : errMsg});
						}
					} else {
						socket.emit('rpc.response' , { ackId: cfg.ackId , response : response , error : errMsg});
					}
				}).bind({} , errMsg,cfg,socket));
			} else {
				socket.emit('rpc.response' , { ackId: cfg.ackId , error : errMsg});
			}
		});		
		socket.on('rpc.response' , function(res){ 
			if (res.ackId != null && selfThis.acks[res.ackId]) {
				var func = selfThis.acks[res.ackId];
				delete selfThis.acks[res.ackId]    ;
				func( res.error , res.response );
			} else if (res.error){
				logger.error(res.error); 
			}
		});		
	}
	
	RpcWrapper.prototype.call=function call(module,method) {
		if (!module) {
			throw 'module cannot be null' ;
		}
		if (!method) {
			throw 'method cannot be null' ;
		}		
		
		var cb   = null;
		var args = []  ;
		for (var i=2; i<arguments.length; i++) {
			if ( (i == arguments.length - 1) && typeof arguments[i] === 'function') {
				cb = arguments[i]
			} else {
				args.push(arguments[i]);
			}
		}
		
		var cfg = {
			module : module , method: method , args : args
		};
		
		if (cb) {
			cfg.ackId            = ++ this.ackId ;
			this.acks[cfg.ackId] = cb            ;
		}
		this.socket.emit('rpc.call', cfg );
	}
	
	var requestFunc = RpcWrapper.prototype.request = (function(){
		var requestLib = null ;
		if (typeof window === 'undefined') {
			requestLib = require('request');
		}
		// on server side, request library is ready
		if (requestLib) {
			var method = function( urlOrOpts , cb ) {
				requestLib( urlOrOpts , function(err, response, body) {
					if (err || response.statusCode !== 200) {
						logger.error('request:' + (urlOrOpts.uri || urlOrOpts) + ' failed' , ( err || response.statusCode)); 
						cb('request:' + (urlOrOpts.uri || urlOrOpts) + ' failed with:' + ( err || response.statusCode)) ;
					} else {
						cb( null , body);
					}
				});
			}
			// tell rpc framework, I need async callback
			method.async = true ;
			return method       ;
		}
		// on client side, need delegate the call to server
		return function( urlOrOpts , cb ) {
			this.call( 'rpc', 'request' , urlOrOpts , cb );
		}
	})();
	
	function getInstance(socket) {
		var sid = 0 ;
	    if (socket != null) {
		    if (socket.id == null) {
			    socket.id = socketId ++ ;    
			}
		    if (!socketsCache[socket.id]) {
			    socketsCache[socket.id] = new RpcWrapper(socket);
			}
			sid = socket.id ;
		} else {
			for (var id in socketsCache){
				if (typeof socketsCache[id] === 'object') {
				    sid = id ;
				    break;	
				}
			}
		}	
		return socketsCache[sid];
	}
	
	return {
	    getInstance : getInstance ,
	    request     : requestFunc	
	} ;
});