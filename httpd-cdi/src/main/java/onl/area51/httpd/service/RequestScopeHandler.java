/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package onl.area51.httpd.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.enterprise.context.Dependent;
import javax.enterprise.event.Observes;
import onl.area51.httpd.action.ActionRegistry;
import org.jboss.weld.context.bound.BoundRequestContext;

/**
 *
 * @author peter
 */
@Dependent
public class RequestScopeHandler
{

    void deploy( @Observes ActionRegistry builder, BoundRequestContext requestContext )
            throws IOException
    {

        // request context
        builder.addRequestInterceptorFirst( ( r, c ) -> {
            Map<String, Object> dataStore = (Map<String, Object>) c.getAttribute( "request.scope" );
            if( dataStore == null ) {
                dataStore = new HashMap<>();
                c.setAttribute( "request.scope", dataStore );
                requestContext.associate( dataStore );
                requestContext.activate();
            }
        } )
                .addResponseInterceptorLast( ( r, c ) -> {
                    Map<String, Object> dataStore = (Map<String, Object>) c.getAttribute( "request.scope" );
                    if( dataStore != null ) {
                        try {
                            requestContext.invalidate();
                            requestContext.deactivate();
                        }
                        finally {
                            requestContext.dissociate( dataStore );
                        }
                    }
                } );

    }
}
