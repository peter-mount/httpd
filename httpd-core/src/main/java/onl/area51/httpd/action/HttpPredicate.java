/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package onl.area51.httpd.action;

import java.io.IOException;
import javax.xml.ws.http.HTTPException;

/**
 *
 * @author peter
 * @param <T>
 */
@FunctionalInterface
public interface HttpPredicate<T>
{

    boolean test( T t )
            throws IOException,
                   HTTPException;

    default HttpPredicate<T> not()
    {
        return t -> !test( t );
    }

    static HttpPredicate<Request> attributePresent( String n )
    {
        return r -> r.isAttributePresent( n );
    }

    static HttpPredicate<Request> attributeTrue( String n )
    {
        return r -> {
            Object o = r.getAttribute( n );
            if( o instanceof Boolean ) {
                return (Boolean) o;
            }
            if( o instanceof String ) {
                return Boolean.valueOf( o.toString() );
            }
            return false;
        };
    }
}
