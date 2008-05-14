package org.grails.mail
/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: May 13, 2008
 */
class GrailsMailException extends RuntimeException{

    public GrailsMailException(String s) {
        super(s);
    }

    public GrailsMailException(String s, Throwable throwable) {
        super(s, throwable);    
    }


}