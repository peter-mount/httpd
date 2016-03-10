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
package onl.area51.httpd;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.HttpStatus;
import org.apache.http.protocol.HttpRequestHandler;
import static onl.area51.httpd.HttpAction.sendError;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.StatusLine;

/**
 * Build's a {@link HttpRequestHandler} from one or more {@link HttpAction}'s associated with a method
 *
 * @author peter
 */
public interface HttpRequestHandlerBuilder
{

    ChainBuilder method( String method );

    HttpRequestHandlerBuilder linkMethod( String method, String substitute );

    HttpRequestHandler build();

    static interface ChainBuilder
    {

        /**
         * Adds an action to this methods chain
         *
         * @param action
         *
         * @return
         */
        ChainBuilder add( HttpAction action );

        /**
         * Complete the chain.
         * <p>
         * Note, if a chain has already been created then this will append to that chain.
         *
         * @return
         */
        HttpRequestHandlerBuilder end();

        ChainBuilder log( Logger logger, Level level );

        /**
         * Respond with an error
         *
         * @param code HTTP status code
         *
         * @return
         */
        default HttpRequestHandlerBuilder sendError( int code )
        {
            return sendError( code, String.valueOf( code ) );
        }

        /**
         * Respond with an error
         *
         * @param code    HTTP status code
         * @param message Message
         *
         * @return
         */
        default HttpRequestHandlerBuilder sendError( int code, String message )
        {
            return add( ( req, resp, ctx ) -> HttpAction.sendError( resp, code, message ) )
                    .end();
        }

        /**
         * Respond with an error
         *
         * @param code HTTP status code
         * @param fmt  Format
         * @param args arguments
         *
         * @return
         */
        default HttpRequestHandlerBuilder sendError( int code, String fmt, Object... args )
        {
            return add( ( req, resp, ctx ) -> HttpAction.sendError( resp, code, fmt, args ) )
                    .end();
        }
    }

    static HttpRequestHandlerBuilder create()
    {
        return new HttpRequestHandlerBuilder()
        {
            Map<String, HttpAction> actions = new ConcurrentHashMap<>();
            Map<String, String> links = null;

            @Override
            public ChainBuilder method( String method )
            {
                HttpRequestHandlerBuilder b = this;
                return new ChainBuilder()
                {
                    private HttpAction action;
                    private Logger logger;
                    private Level level;

                    @Override
                    public ChainBuilder log( Logger logger, Level level )
                    {
                        this.logger = logger;
                        this.level = level;
                        return this;
                    }

                    @Override
                    public ChainBuilder add( HttpAction action )
                    {
                        Objects.requireNonNull( action );
                        this.action = this.action == null ? action : this.action.andThen( action );
                        return this;
                    }

                    @Override
                    public HttpRequestHandlerBuilder end()
                    {
                        Objects.requireNonNull( action, "No action defined for " + method );

                        // Wrap the action with our logger
                        if( logger != null && level != null ) {
                            // Store the current action here else we'll go into an infinite loop
                            HttpAction wrappedAction = action;
                            action = ( req, resp, ctx ) -> {
                                try {
                                    logger.log( level, () -> req.getRequestLine().getMethod() + ": " + req.getRequestLine().getUri() );

                                    wrappedAction.apply( req, resp, ctx );

                                    if( logger.isLoggable( level ) ) {
                                        StatusLine statusLine = resp.getStatusLine();
                                        HttpEntity entity = resp.getEntity();
                                        logger.log( level, () -> req.getRequestLine().getMethod() + ": " + req.getRequestLine().getUri()
                                                                 + " " + (statusLine == null ? -1 : statusLine.getStatusCode())
                                                                 + " " + (entity == null ? -1 : entity.getContentLength()) );
                                    }
                                }
                                catch( RuntimeException |
                                       HttpException |
                                       IOException ex ) {
                                    if( logger.isLoggable( level ) ) {
                                        StatusLine statusLine = resp.getStatusLine();
                                        HttpEntity entity = resp.getEntity();
                                        logger.log( level, ex, () -> req.getRequestLine().getMethod() + ": " + req.getRequestLine().getUri()
                                                                     + " " + (statusLine == null ? -1 : statusLine.getStatusCode())
                                                                     + " " + (entity == null ? -1 : entity.getContentLength()) );
                                    }
                                    throw ex;
                                }
                            };
                        }

                        actions.merge( method.toUpperCase( Locale.ROOT ), action, HttpAction::andThen );
                        return b;
                    }
                };
            }

            @Override
            public HttpRequestHandlerBuilder linkMethod( String method, String substitute )
            {
                if( links == null ) {
                    links = new HashMap<>();
                }
                String existing = links.putIfAbsent( method.toUpperCase( Locale.ROOT ), substitute.toUpperCase( Locale.ROOT ) );
                if( existing != null ) {
                    throw new IllegalArgumentException( "Cannot link " + method + " to " + substitute + " as already linked to " + existing );
                }
                return this;
            }

            @Override
            public HttpRequestHandler build()
            {
                if( links != null && !links.isEmpty() ) {
                    links.forEach( ( m, s ) -> {
                        if( actions.containsKey( m ) ) {
                            throw new IllegalStateException( "Cannot link " + m + " to " + s + " as it has an action defined" );
                        }
                        if( !actions.containsKey( s ) ) {
                            throw new IllegalStateException( "Cannot link " + m + " to " + s + " as it's target has not been defined" );
                        }
                        actions.put( m, actions.get( s ) );
                    } );
                }

                // If we have GET defined then link HEAD to it - follows HttpServlet
                if( actions.containsKey( "GET" ) && !actions.containsKey( "HEAD" ) ) {
                    actions.put( "HEAD", actions.get( "GET" ) );
                }

                return ( request, response, context ) -> actions.getOrDefault(
                        request.getRequestLine().getMethod().toUpperCase( Locale.ROOT ),
                        ( req, resp, ctx ) -> sendError( resp, HttpStatus.SC_METHOD_NOT_ALLOWED, "Method not allowed" )
                )
                        .apply( request, response, context );
            }
        };
    }
}
