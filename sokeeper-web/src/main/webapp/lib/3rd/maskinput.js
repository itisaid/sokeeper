
(function($) {
	//Helper Function for Caret positioning
	$.fn.caret=function(begin,end){	
		if(this.length==0) return;
		if (typeof begin == 'number') {
            end = (typeof end == 'number')?end:begin;  
			return this.each(function(){
				if(this.setSelectionRange){
					this.focus();
					this.setSelectionRange(begin,end);
				}else if (this.createTextRange){
					var range = this.createTextRange();
					range.collapse(true);
					range.moveEnd('character', end);
					range.moveStart('character', begin);
					range.select();
				}
			});
        } else {
            if (this[0].setSelectionRange){
				begin = this[0].selectionStart;
				end = this[0].selectionEnd;
			}else if (document.selection && document.selection.createRange){
				var range = document.selection.createRange();			
				begin = 0 - range.duplicate().moveStart('character', -100000);
				end = begin + range.text.length;
			}
			return {begin:begin,end:end};
        }       
	};
	
	//Predefined character definitions
	var charMap={
		'9':"[0-9]",
		'a':"[A-Za-z]",
		'*':"[A-Za-z0-9]"
	};
	
	//Helper method to inject character definitions
	$.remask={
		addPlaceholder : function(c,r){
			charMap[c]=r;
		}
		,
		masks : {
			'percentage' : {
			    test : function( val ){
				    return /^\d*\%{0,1}$/.test( val ) ;
			    }    	
			}
			,
			number   : {
			    test : function( val ){
				    return /^\d*$/.test( val ) ;
			    }
		    }
		    ,
		    'float'   : {
			    test : function( val ){
				    return /^\d*\.{0,1}\d{0,2}$/.test( val ) ;
			    }
		    }
		    ,
		    currency   : {
			    test : function( val ){
				    return /^(\-{0,1})\${0,1}(\d{0,10}\.{0,1}\d{0,2})$/.test( val ) ;
			    }
			    ,
			    ignore_placeholder : true 
		    }
		    ,
		    zipcode    : {
			    test : function( val ){
				    return /^([\d|\_]{0,5})\-{0,1}([\d|\_]{0,4})$/.test( val ) ;
			    }
		    }
		    ,
		    ssn        : {
			    test : function( val ){
				    return  /^[\d|\_]{0,3}\-{0,1}[\d|\_]{0,2}\-{0,1}[\d|\_]{0,4}$/.test( val ) ;
			    }
		    }
		    ,
		    phonenumber: {
			    test : function( val ){
				    return  /^\({0,1}[\d|\_]{0,3}\){0,1}[\d|\_]{0,3}\-{0,1}[\d|\_]{0,4}$/.test( val ) ;
			    }
		    }
		    ,
		    date       : {
			    test : function( val , $input ){
				    var succeed = /^[\d|\_]{0,2}\/{0,1}[\d|\_]{0,2}\/{0,1}[\d|\_]{0,4}$/.test( val ) ;
				    if( succeed ) {
					    val = $.remask.unmaskedFormats['number']( val ) ;
					    if( parseInt( val.substring(0,2) ) > 12 
					          || ( val.length > 2 && parseInt( val.substring(2,4) ) > 31 ) ){
						    succeed = false ;    
					    }   
				    }
				    if( succeed && $input.attr( 'range' ) && val.length > 4 ){
					    var range= $input.attr( 'range' ) || '[0000,9999]' ;
					        range= range.substring(0,11) + '[0000,9999]'.substring( range.length ) ;
					        range= range.substring(1,10);
					        range= range.split( ',' )   ;
					        
					    var year = val.substring( 4 ) + '00000000'.substring( val.length ) ;
					    
					    if( ( ( val.length == 8) && ( parseFloat( range[0] ) > parseFloat( year ) )) || ( parseFloat( range[1] ) < parseFloat( year ) ) ){
						    succeed = false ;   
					    }
				    }
				    
				    return succeed ;
			    }
			    ,
			    increment : function( $input , increment ) {
				    $.remask.masks['datetime'].increment( $input , increment ) ;
				}
				,
				guess_skip : 1
		    }
		    ,
		    time       : {
			    test : function( val ){
				    var succeed = /^[\d|\_]{0,2}\:{0,1}[\d|\_]{0,2}\:{0,1}[\d|\_]{0,2}$/.test( val ) ;
				    if( succeed ) {
					    val = $.remask.unmaskedFormats['number']( val ) ;
					    if( parseInt( val.substring(0,2) ) >= 24 
					          || ( val.length > 2 && parseInt( val.substring(2,4) ) >= 60 ) 
					            || ( val.length > 4 && parseInt( val.substring(4,6) ) >= 60 ) ){
						    succeed = false ;    
					    }   
				    }
				    return succeed ;
			    }
			    ,
			    increment : function( $input , increment ) {				    
				    $.remask.masks['datetime'].increment( $input , increment ) ;			    
			    }
			    ,
				guess_skip : 1
		    }
		    ,
			'datetime': {
				test : function( val , $input ) {
					val = ( val || '' )	.split( ' ' ) ;
				    return (val.length < 3) && $.remask.masks['date'].test( val[0] , $input ) && $.remask.masks['time'].test( val[1] , $input ) ;
				}
				,
				increment : function( $input , increment ) {
					// mm/dd/yyyy hh:mm:ss	
					//  0  1    2  3  4  5
					var pos = $input.caret() ;
					var mask= $input.attr( 'maskedFormat' ) ;
					var val = $input.val();
					if( mask == 'time' ) {
						val = '01/01/1970 ' + val ;
						pos.begin += 11 ;
						pos.end   += 11 ;
					} else if( mask == 'date' ){
					    val = val + ' 00:00:00'   ;	
					}					
					val = val.split( ' ' )        ;
					
					var date= new Date();
					var cidx= ( pos.begin > 10 ) ? ( (pos.begin - 11)/3 + 3 ) : ( ( pos.begin / 3) > 2 ? 2 : ( pos.begin / 3) );
					cidx    = parseInt( cidx ) ;
					
					val[0]  = $.remask.unmaskedFormats['number'](val[0]) || '' ;
					val[1]  = $.remask.unmaskedFormats['number'](val[1]) || '' ;
					
					val[5]  = parseInt( parseFloat( val[1].substring(4,6) || date.getSeconds() ) ); //ss
					val[4]  = parseInt( parseFloat( val[1].substring(2,4) || date.getMinutes() ) ); //mm
					val[3]  = parseInt( parseFloat( val[1].substring(0,2) || date.getHours()   ) ); //hh
					
					val[2]  = parseInt( parseFloat( val[0].substring(4,8) || (date.getFullYear()) )); //yyyy
					val[1]  = parseInt( parseFloat( val[0].substring(2,4) || (date.getDate())     )); //dd
					val[0]  = parseInt( parseFloat( val[0].substring(0,2) || (date.getMonth()+1)  )); //mm
					val[0]  = val[0] < 1 ? 0 : ( val[0] - 1 ) ;
					
					var setFuncs = ['setMonth','setDate','setFullYear','setHours','setMinutes','setSeconds'] ;
					var getFuncs = ['getMonth','getDate','getFullYear','getHours','getMinutes','getSeconds'] ;
					var fixes    = [1,0,0,0,0,0];
					var splits   = ['00/','00/','0000 ','00:','00:','00'] ;
					
					for( var i=0; i<val.length; i++ ){
					    val[i] = isNaN( val[i] ) ? 0 : val[i] ;	
					    date[setFuncs[i]]( val[i] ) ;
					}
					
					var units   = [
					    0                //mm
					    ,
					    24 * 60 * 60     //dd
					    ,
					    0                //yyyy
					    ,
					    60 * 60          //hh
					    ,
					    60               //mm
					    ,
					    1                //ss
					] ;
					switch( cidx ) {
					    case 0 : {
						    var m = date.getMonth() + increment ;
						    if( m <= -1 ) {
							     date.setMonth( 11 ) ;
							     date.setFullYear( date.getFullYear() - 1 ) ;   
						    } else if( m >= 11 ){
						         date.setMonth( 0 ) ;
							     date.setFullYear( date.getFullYear() + 1 ) ;   
						    } else {
							     date.setMonth( m ) ;   
						    }						    
						} break ;
						
					    case 2 : {
						    date.setFullYear( date.getFullYear() + increment ) ;
					    } break ;
					    default : {
						    date.setTime( date.getTime() + units[cidx] * 1000 * increment ) ;
					    } break ;	
					}
					
					val = '' ;
					for( var i=0; i<getFuncs.length; i++ ){
						var v = '' + ( date[getFuncs[i]]() + fixes[i] ) + ( (i == getFuncs.length - 1) ? '' : splits[i].charAt( splits[i].length - 1 ) );
						    v = splits[i].substring( 0 , splits[i].length - v.length ) + v ;
						val  += v ;
					}
					
					pos = $input.caret() ;
					if( mask == 'time' ) {
						val = val.split( ' ' )[1] ;
					} else if( mask == 'date' ){
						val = val.split( ' ' )[0] ;
					}
					if( mask == 'date' && $.remask.masks['date'].test( val , $input ) || mask != 'date' ){
					    $input.val( val ) ;
				    }
					$input.caret( pos.begin ) ;
					
				}
				,
				guess_skip : 1
			}
		}
		,
		maskedFormats : {
			'default' : function( val ) {
				return val ;
			}
			,
			'float' : function( val ) {
				return (''+val) ? parseFloat( val ).toFixed( 2 ) : '' ;
			}
			,
			'percentage': function( val , editable ) {
				val = $.remask.unmaskedFormats['number']( val ) ;
				return val ? parseInt(parseFloat(val)) + '%' : val ;
			}
			,
			'currency': function( val , editable ) {
				if( ( val.charAt(0) == '-' || val.charAt(0) == '.' ) && editable ) {
				    return val ;	
				}
				
				val = $.remask.unmaskedFormats['currency']( val ) ;
				if( !val == '' ) {
					val+= val.indexOf( '.' ) < 0 ? '.00' : '00'    ;
					val = val.split( '.' ) ;
					val = val[0] + '.' + val[1].substring( 0 , 2 ) ;
					val = parseFloat( val ).toFixed( 2 ) ;
					
					if( editable ) {
					    val = ( val < 0 ? '-' : '' ) + '$' + parseFloat( val < 0 ? ( 0.00 - val ) : val ).toFixed(2) ;
					} else {
						val = ( val < 0 ? '(' : '' ) + '$' + parseFloat( val < 0 ? ( 0.00 - val ) : val ).toFixed(2) + ( val < 0 ? ')' : '' ) ;
					}	
				}
				return val ;
			}
			,
			'zipcode': function( val , editable ) {
				val = $.remask.unmaskedFormats['number']( val ) ;
				if( val.length > 5 ){
				    val = val.substring(0,5) + '-' + val.substring( 5 ) ;	
			    }
		    	if( editable ) {
			        val += '_____-____'.substring( val.length ) ;	
		    	}
				return val ;
			}
			,
			'ssn': function( val , editable ) {
				val = $.remask.unmaskedFormats['number']( val ) ;
				if( val.length > 3 ){
				    val = val.substring(0,3) + '-' + val.substring( 3 ) ;	
			    }
			    if( val.length > 6 ){
				    val = val.substring(0,6) + '-' + val.substring( 6 ) ;	
			    }			    
		    	if( editable ) {
			        val += '___-__-____'.substring( val.length ) ;	
		    	}
				return val ;
			}
			,
			'phonenumber': function( val , editable ) {
				val = $.remask.unmaskedFormats['number']( val ) ;
				
				if( editable ) {
					val += '___'.substring( val.length ) ;	
				}
				
				if( editable || val.length > 3 ) {
					val = '(' + val.substring(0,3) + ')' + val.substring( 3 ) ;	
				}
			    
				if( val.length > 8 ){
				    val = val.substring(0,8) + '-' + val.substring( 8 ) ;	
			    }	
			    			    		    
		    	if( editable ) {
			        val += '(___)___-____'.substring( val.length ) ;	
		    	}
		    	
				return val ;
			}
			,
			'date': function( val , editable ) {
				var str = ( val || '' ).replace( /[_]/g , '' ) ;
				    str = str.split( '/' ) ;
				if( str.length > 1 ) {
				    if( str[0] ) {
					    if( editable ) {
						    if( str[0].length <= 2 ) {
							    str[0]+= '__' ;
							    str[0] = str[0].substring( 0 , 2 )  ;
						    }
					    } else {
						    str[0] = '00' + str[0] ;
						    str[0] = str[0].substring( str[0].length - 2 ) ;    
					    }
				    } 
				    	
				    if( str[1] ) {
					    if( editable ) {
						    if( str[1].length <= 2 ) {
							    str[1]+= '__' ;
							    str[1] = '/' + str[1].substring( 0 , 2 )  ;    
						    }
					    } else {							    
						    str[1] = '00' + str[1] ;
						    str[1] = '/' + str[1].substring( str[1].length - 2 ) ; 
					    }   
				    }	
				    if( str[2] ) {
					    if( editable ) {
						    if( str[2].length <= 4 ) {
							    str[2]+= '____' ;
							    str[2] = '/' + str[2].substring( 0 , 4 )  ;    
						    }
					    } else {							    
						    str[2] = '/' + str[2];
						}   
				    }
				    
				    val = ( str[0] || '' ) +  ( str[1] || '' ) + ( str[2] || '' ) ; 
				    
				} else {   
				
					val = $.remask.unmaskedFormats['number']( val ) ;	
					if( val.length > 2 ) {
					    val = val.substring(0,2) + '/' + val.substring( 2 ) ;	
					}    	
					if( val.length > 5 ) {
					    val = val.substring(0,5) + '/' + val.substring( 5 ) ;	
					}
					
				}
				
				if( editable ) {
					val += '__/__/____'.substring( val.length ) ;
				}
				
				return val ;
			}
			,
			'time': function( val , editable ) {	
				val = $.remask.unmaskedFormats['number']( val ) ;	
				
				if( val.length > 2 ) {
				    val = val.substring(0,2) + ':' + val.substring( 2 ) ;	
				}    	
				if( val.length > 5 ) {
				    val = val.substring(0,5) + ':' + val.substring( 5 ) ;	
				}    	
				if( editable ) {
					val += '__:__:__'.substring( val.length ) ;
				}
				return val ;
			}
			,
			'datetime': function( val , editable ) {
				val = ( val || '' )	.split( ' ' ) ;
			    val = [
			        $.remask.maskedFormats['date'](val[0],editable) 
			        ,
			        $.remask.maskedFormats['time'](val[1],editable) 
			    ] ;	
			    return val[0] + ( val[1] ? ' ' : '' ) + val[1] ;
			}
		}
		,
		unmaskedFormats : {
			'default' : function( val ) {
				return val ;
			}
			,
			'currency'  : function( val ) {
				if( val == null ) val = '' ;
				var str  = '' ;
				for( var i=0; i<val.length; i++ ){
				    if( /\d|\.|\-|\(|\)/.test( val.charAt(i) ) ){
					    str += val.charAt(i);    
				    }
			    }
			    if( /^\(.*\)$/.test(val)  ) {
				    str = '-' + str.replace( /\(|\)/g , '' ) ;      	
				}	
				if( str.length > 0 ) {
				    str = str.charAt(0) + str.substring(1).replace( /\-/g , '' ) ;	
				}			
				if( isNaN( parseFloat( str ) ) ){
					str = '' ;
				}
				return str ;
			}
			,
			'number'  : function( val ) {
				if( val == null ) val = '' ;
				var str  = '' ;
				for( var i=0; i<val.length; i++ ){
				    if( /\d/.test( val.charAt(i) ) ){
					    str += val.charAt(i);    
				    }
			    }
			    return str ;	
			}
		}		
	};
	
	$.fn.maskedVal = function( maskedFormat , editable , val ){
		var vals = [] ;
		this.each(function(){		
			var input = $(this);
			var vfunc = $.remask.maskedFormats[ maskedFormat || input.attr('maskedFormat') ] || $.remask.maskedFormats['default'] ;
			vals.push( vfunc( val != null ? val : ( input.val() || input.text() ) , editable ) ) ;
		} );
		if( vals.length == 1 ){
			return vals[0] ;
		}
		return vals ;
	} ;
	
	$.fn.unmaskedVal = function( unmaskedFormat ){
		var vals = [] ;
		this.each(function(){		
			var input = $(this);
			var vfunc = $.remask.unmaskedFormats[ unmaskedFormat || input.attr('unmaskedFormat')  ] || $.remask.unmaskedFormats['default'] ;
			vals.push( vfunc( input.val() ) ) ;
		} );
		if( vals.length == 1 ){
			return vals[0] ;
		}
		return vals ;
	} ;
	
	$.fn.unremask=function(){
		return this.trigger("unremask");
	};
	
	//Main Method
	$.fn.remask = function(mask,settings) {	
		
		mask = jQuery.remask.masks[mask] || mask ;
		
		settings = $.extend({
			placeholder     : "_"       ,			
			completed       : null      
		}, settings);	
		
		//Build Regex for format validation
		var re     = '' ;
		
		if( mask.test ){
		    re = mask ;	
		} else {
			var sample = '' ;
			for( var i=0,block=false; i<mask.length; i++ ){
				var c = mask.charAt(i) ;
			    block = block || c == '{' ;
			    re   += block ? c : ( charMap[c] || ( (/[A-Za-z0-9]/.test(c)?"":"\\") + c )  ) ;
			    
			    if( !block   ) {
				    sample += charMap[c] == null ? c : settings.placeholder ;
			    }
			    if( c == '{' ) {
				    var times = parseInt( ( mask.substring( i + 1 ).match(  /(\d+).*/ ) || [0] )[1] );
				    var s = sample.charAt( sample.length - 1 );
				    sample= sample.substring( 0 , sample.length-1 );
				    
				    while( times > 0 ){
					    sample += s ;
					    times    -- ;    
				    }			    
				}
			    
			    if( block && c == '}' ) block = false ;  
			}
			re = new RegExp( '^' + re + '$' ) ;
		}
		
		return this.each(function(){		
			var input = $(this);
			 
			var ignore= false  ;
						
			function focusEvent(){	
				var pos=input.caret();
				input.val( input.maskedVal( null , true ) ) ;
				
				var uval= input.unmaskedVal( ) ;
				var val = input.val() ;
				for( var i=0; i<val.length; i++ ){
					if( val.charAt(i) == input.attr( 'placeholder' ) ) {
						pos.begin = i ;
						break ;
					}
				}
				input.caret( pos.begin ) ;
			}
			
			function keydownEvent(e){
				var val=input.val()||'';
				var k = e.keyCode;
				var pos=$(this).caret();
				ignore=( k < 16 || (k > 16 && k < 32 ) || (k > 32 && k < 41) || k == 8 || k == 46 );
				
				if( k == 8 ) {      //backspace
					if( pos.begin == pos.end ){
					    pos.begin --;
					    if( pos.begin < 0 ) {
						    pos.begin = 0 ;
					    }	
					}    
				} else if( k==46 ){ //delete	
					if( pos.begin == pos.end ){
					    pos.end ++;
					    if( pos.end > val.length ) {
						    pos.end = val.length ;
					    }	
					}								
				}	
				if( k == 8 || k == 46 ){					
					val = val.substring( 0 , pos.begin ) + val.substring( pos.end ) ;
					input.val(val);	
					input.caret( pos.begin );
					return false ;
				}
				
				if( ( k == 38 || k == 40 ) && re.increment ){
				    re.increment( input , k == 38 ? 1 : -1 ) ;	
				}				
				return true ;								
			}
			
			function keypressEvent(e){	
				if(ignore){
					ignore=false;
					//Fixes Mac FF bug on backspace
					return (e.keyCode == 8)? false: null;
				}
				
				e=e||window.event;
				var k=e.charCode||e.keyCode||e.which;						
				var pos=$(this).caret();
								
				if(e.ctrlKey || e.altKey){//Ignore
					return true;
				}else if ((k>=41 && k<=122) ||k==32 || k>186){//typeable characters	
				    var val    = input.val();
				        val    = val.substring(0,pos.begin)+val.substring(pos.end);
				        
				    var nskip  = 0 ;
				    var ival   = val.substring(0,pos.begin)+ String.fromCharCode(k) + val.substring(pos.begin) ;
				        ival   = input.maskedVal( null , true , ival ) ;
				    if( re.test( ival , input ) ){
					    nskip = ival.substring( pos.begin ).indexOf(String.fromCharCode(k)) ;
					    nskip = nskip < 0 ? -1 : nskip ;
					    input.val( ival ) ;
				    } else {    
					    var guess_skip = re.guess_skip == null ? ( val.length - pos.begin ) : re.guess_skip ;
					    for( var skip = 0 ; skip <=  ( k == 32 ? 0 : guess_skip ) ; skip ++ ){
						    var vals = [
						        val.substring( 0 , pos.begin + skip ) + String.fromCharCode(k) + val.substring( pos.begin + skip ) 
						        ,
						        val.substring( 0 , pos.begin + skip ) + String.fromCharCode(k) + val.substring( pos.begin + skip + 1 )
						    ] ;
						    
						    if( re.test( vals[0] , input ) ){
							    val   = vals[0] ;
							    nskip = skip    ;
							    break ;    
						    } else if( re.test( vals[1] , input ) ){
							    val   = vals[1] ;
							    nskip = skip    ;
							    break ;
						    } 
						}
						if( val == input.val() ){
							if( val.charAt( pos.begin ) == settings.placeholder && !re.ignore_placeholder ) {
							    nskip = -1 ;        	
							}
						} else {
							val = input.maskedVal( null , true , val ) ;
							if( re.test( val , input ) ) {
								input.val( val ) ;
							}
					    }
					}
					input.caret( pos.begin + nskip + 1 ) ;
					
					return false ;
				}	
									
				return true ;	
			}
			
			function checkVal( e ){
				input.val( input.maskedVal( null , false ) ) ; 
				if( ( settings.completed || {} ).bind ){
					settings.completed( input ) ;
				}
			}
			
			function onChange(){
				input.val( input.maskedVal( null , false ) ) ; 
			    if( !re.test( input.val() , input ) ){
				    input.val( '' ) ;    
			    } 	
			}
			
			input.one("unremask",function(){
				input.unbind("focus",focusEvent);
				input.unbind("blur",checkVal );
				input.unbind("keydown",keydownEvent);
				input.unbind("keypress",keypressEvent);
				input.unbind("change",onChange);				
				if ($.browser.msie) 
					this.onpaste= null;                     
				else if ($.browser.mozilla)
					this.removeEventListener('input',onChange,false);
			});
			
			input.bind("focus",focusEvent);
			input.bind("blur",checkVal);
			input.bind("keydown",keydownEvent);
			input.bind("keypress",keypressEvent);
			input.bind("change",onChange);
			//Paste events for IE and Mozilla thanks to Kristinn Sigmundsson
			if ($.browser.msie) 
				this.onpaste= function(){setTimeout(onChange,0);};                     
			else if ($.browser.mozilla)
				this.addEventListener('input',onChange,false);
		});
	};
})(jQuery);
