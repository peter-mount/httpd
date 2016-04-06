/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package onl.area51.httpd.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import onl.area51.httpd.action.ActionRegistry;
import onl.area51.httpd.action.ContextListener;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;
import org.jboss.weld.context.bound.BoundRequestContext;

/**
 * Implements the CDI {@link javax.enterprise.context.RequestScoped} scope.
 *
 * @author peter
 */
@ApplicationScoped
public class RequestScopeHandler
        implements ContextListener
{

    private static final String ATTR = RequestScopeHandler.class.getName();

    @Inject
    private BoundRequestContext requestContext;

    void deploy( @Observes ActionRegistry builder )
            throws IOException
    {
        builder.addContextListener( this );
    }

    @Override
    public void begin( HttpRequest req, HttpContext ctx )
            throws HttpException,
                   IOException
    {
        Map<String, Object> dataStore = (Map<String, Object>) ctx.getAttribute( ATTR );
        if( dataStore == null ) {
            dataStore = new HashMap<>();
            ctx.setAttribute( ATTR, dataStore );
            requestContext.associate( dataStore );
            requestContext.activate();
        }
    }

    @Override
    public void end( HttpResponse resp, HttpContext ctx )
            throws HttpException,
                   IOException
    {
        Map<String, Object> dataStore = (Map<String, Object>) ctx.getAttribute( ATTR );
        if( dataStore != null ) {
            try {
                requestContext.invalidate();
                requestContext.deactivate();
            }
            finally {
                requestContext.dissociate( dataStore );
            }
        }
    }
}
