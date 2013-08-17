/**
 * @attr[placeholder]
 * @attr[maskedFormat]
 * @attr[unmaskedFormat]
 * @date.attr[range] : [1990,2070]
 */

core.create( "component.RegExpMaskInput" , {
	css : function() {
	  return [ ] ;
	}
	,
	js : function(){
	  core.registerIsLibraryLoaded( "3rd.maskinput" , function(){
	      return ( ( window.jQuery || {} ).fn || {} ).remask ;	  
	  } ) ;		
	  return [ "3rd.jquery" , "3rd.maskinput" ] ;
	}
	,
	ready : function() {
	  return core.isLibraryLoaded( "3rd.jquery" ) 
	             && core.isLibraryLoaded( "3rd.maskinput" )  ;
	}
	,	
	initialize :  function( elem ){
		var $elem  = jQuery( elem ) ;	
		
		var tagName= ( elem.tagName || '' ).toLowerCase() ;	
		
		if( tagName == 'input' || tagName == 'textarea' ) {
			$elem.val( $elem.maskedVal( null , false ) ) ;
			$elem.remask( elem.getAttribute( "maskedFormat"  ) , { 
				placeholder : ''+ ( elem.getAttribute( "placeholder" ) || '0' )
				,
				completed   : function( $input ){
				    // this function will be call after the input box blur	
				    if( $input.attr( 'maskedFormat' ).match( /[date].*/ ) && $input.val().match( /([0][2])\/(\d{2})\/(\d{4}).*/ ) ){
					    var mdy = $input.val().match(/([0][2])\/(\d{2})\/(\d{4}).*/ ) ;
					    for( var i=1; i<mdy.length; i++ ){
						    mdy[i] = parseInt( parseFloat( mdy[i] ) ) ;    
					    }
					    var date= new Date( );
					    date.setFullYear( mdy[3] );
					    date.setMonth( mdy[1] - 1 );
					    date.setDate( mdy[2] );
					    if( date.getDate() != mdy[2] ){
						    mdy[2] -= date.getDate() ;
						    $input.val( '02/' + mdy[2] + '/' + mdy[3] + mdy[0].substring( 10 ) );
						}
					}
				}
			} );
			elem.rval = function(){
			    return $elem.unmaskedVal() ;	
			}
		} else {
			$elem.text( $elem.maskedVal( null , false ) ) ;
		}	
    } 
} ) ;
 