/*
 * Copyright 2016 peter.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package onl.area51.httpd.action;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import onl.area51.httpd.HttpRequestHandlerBuilder;
import onl.area51.httpd.HttpServerBuilder;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;

/**
 * A set of builders to provide specific actions
 *
 * @author peter
 */
public interface Actions
{

    /**
     * An action that will fail with a not found error if no response is present
     *
     * @return
     */
    static Action notFoundAction()
    {
        return r -> {
            if( !r.isResponsePresent() ) {
                sendError( r, HttpStatus.SC_NOT_FOUND, r.getURI().getPath() );
            }
        };
    }

    /**
     * An action that will issue a 500 internal server error message.
     *
     * @return
     */
    static Action serverError()
    {
        return r -> {
            if( !Action.isOk( r ) ) {
                sendError( r, HttpStatus.SC_INTERNAL_SERVER_ERROR, r.getURI().getPath() );
            }
        };
    }

    /**
     * Register a global handler to serve static content (if found) from /META-INF/resources
     *
     * @param builder
     * @param clazz   Class to base resource search from
     */
    static void registerClassResourceHandler( HttpServerBuilder builder, Class<?> clazz )
    {
        builder.getGlobalHandlerBuilder()
                //.log()
                .method( "GET" )
                .add( Actions.resourceAction( clazz ) )
                .end();
    }

    /**
     * Register resource handlers for the graphic error handlers
     *
     * @param builder
     */
    static void registerErrorHandlers( HttpServerBuilder builder )
    {
        String base = "/META-INF/errorimages";
        URI uri404 = URI.create( "/404.png" );
        URI uri500 = URI.create( "/500.png" );

        builder.registerHandler( "/.404.png", HttpRequestHandlerBuilder.create()
                                 .method( "GET" )
                                 .add( r -> renderResource( Actions.class, r, uri404, base ) )
                                 .end() )
                .registerHandler( "/.500.png", HttpRequestHandlerBuilder.create()
                                  .method( "GET" )
                                  .add( r -> renderResource( Actions.class, r, uri500, base ) )
                                  .end() )
                .getGlobalHandlerBuilder()
                //.log()
                .method( "GET" )
                .add( Actions.notFoundAction() )
                .end();
    }

    /**
     * Set the error response
     *
     * @param req
     * @param sc
     * @param message
     */
    static void sendError( Request req, int sc, String message )
    {
        req.getHttpResponse().setEntity( errorEntity( req, sc, message ) );
    }

    static HttpEntity errorEntity( Request req, int sc, String message )
    {
        req.getHttpResponse().setStatusCode( sc );
        return new StringEntity( "<html><body><div style=\"align:center;\"><img src=\"/." + sc + ".png\"/><p>" + message + "</p></div></body></html>",
                                 ContentType.TEXT_HTML );
    }

    /**
     * Set the error response
     *
     * @param req
     * @param sc
     * @param fmt
     * @param args
     */
    static void sendError( Request req, int sc, String fmt, Object... args )
    {
        sendError( req, sc, String.format( fmt, args ) );
    }

    /**
     * Send an OK entity
     *
     * @param req
     * @param entity
     */
    static void sendOk( Request req, HttpEntity entity )
    {
        HttpResponse response = req.getHttpResponse();
        response.setStatusCode( HttpStatus.SC_OK );
        response.setEntity( entity );
    }

    /**
     * Send a 302 Moved temporary redirect
     *
     * @param req Request
     * @param uri Uri to redirect to
     */
    static void sendRedirect( Request req, String uri )
    {
        sendRedirect( req, HttpStatus.SC_MOVED_TEMPORARILY, uri );
    }

    /**
     * Send a redirect
     *
     * @param req  Request
     * @param code HttpStatus, one of
     *             {@link HttpStatus#SC_MOVED_PERMANENTLY}, {@link HttpStatus#SC_MOVED_TEMPORARILY}, {@link HttpStatus#SC_SEE_OTHER}, {@link HttpStatus#SC_USE_PROXY}
     *             or {@link HttpStatus#SC_TEMPORARY_REDIRECT}
     * @param uri
     */
    static void sendRedirect( Request req, int code, String uri )
    {
        HttpResponse response = req.getHttpResponse();
        response.setStatusCode( code );
        response.addHeader( "Location", uri );
        response.setEntity( new StringEntity( "<html><head><title>Moved</title></head><body><h1>Moved</h1><p>This page has moved to <a href=\""
                                              + uri + "\">" + uri + "</a>.</p></body></html>",
                                              ContentType.TEXT_HTML ) );
    }

    static boolean isOk( Request req )
    {
        HttpResponse resp = req.getHttpResponse();
        return (resp.getStatusLine() == null || resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) && resp.getEntity() == null;
    }

    /**
     * An action that does nothing.
     *
     * @return
     */
    static Action nop()
    {
        return r -> {
        };
    }

    /**
     * Exposes resources under META-INF/resources like you can in servlet web-fragments.
     *
     * @param clazz
     *
     * @return
     */
    static Action resourceAction( Class<?> clazz )
    {
        return r -> renderResource( clazz, r, r.getURI() );
    }

    /**
     * Renders a specific resource from META-INF/resources, usually static content
     *
     * @param clazz Class from which to perform the search
     * @param uri   URI to retrieve
     *
     * @return
     */
    static Action resourceAction( Class<?> clazz, String uri )
    {
        URI u = URI.create( uri );
        return r -> renderResource( clazz, r, u );
    }

    /**
     * Renders a specific resource from META-INF/resources, usually static content
     *
     * @param clazz Class from which to perform the search
     * @param uri   URI to retrieve
     *
     * @return
     */
    static Action resourceAction( Class<?> clazz, URI uri )
    {
        return r -> renderResource( clazz, r, uri );
    }

    /**
     * Renders a specific resource from META-INF/resources, usually static content
     *
     * @param clazz Class from which to perform the search
     * @param r     Request
     * @param uri   URI to retrieve
     *
     * @throws java.io.IOException
     */
    static void renderResource( Class<?> clazz, Request r, URI uri )
            throws IOException
    {
        renderResource( clazz, r, uri, "/META-INF/resources" );
    }

    static void renderResource( Class<?> clazz, Request r, URI uri, String base )
            throws IOException
    {
        String url = uri.getPath();
        if( url.contains( "//" ) ) {
            url = url.replace( "//", "/" );
        }
        if( !url.contains( "/.." ) ) {
            String path = base + (url.startsWith( "/" ) ? "" : "/") + url;
            if( path.endsWith( "/" ) ) {
                path = path + "index.html";
            }
            InputStream is = clazz.getResourceAsStream( path );
            if( is != null ) {
                if( r.isResponsePresent() ) {
                    // Hope the content type is the same, just add to the existing response.
                    // This is usually due to including html content into an existing page
                    try {
                        r.getResponse().copy( is );
                    }
                    finally {
                        is.close();
                    }
                }
                else {
                    // Treat as it's own entity
                    r.getHttpResponse().setEntity( new InputStreamEntity( is ) );
                }
            }
        }
    }

}
