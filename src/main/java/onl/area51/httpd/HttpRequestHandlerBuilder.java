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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.HttpStatus;
import org.apache.http.protocol.HttpRequestHandler;
import onl.area51.httpd.action.Action;
import onl.area51.httpd.action.Actions;
import onl.area51.httpd.action.Request;

/**
 * Build's a {@link HttpRequestHandler} from one or more {@link HttpAction}'s associated with a method
 *
 * @author peter
 */
public interface HttpRequestHandlerBuilder
{

    HttpRequestHandlerBuilder log( Logger logger, Level level );

    default HttpRequestHandlerBuilder log( Level level )
    {
        return log( Logger.getGlobal(), level );
    }

    default HttpRequestHandlerBuilder log()
    {
        return log( Logger.getGlobal(), Level.INFO );
    }

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
        ChainBuilder add( Action action );

        /**
         * Complete the chain.
         * <p>
         * Note, if a chain has already been created then this will append to that chain.
         *
         * @return
         */
        HttpRequestHandlerBuilder end();

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
            return add( r -> Actions.sendError( r, code, message ) ).end();
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
            return add( r -> Actions.sendError( r, code, fmt, args ) ).end();
        }
    }

    static HttpRequestHandlerBuilder create()
    {
        return new HttpRequestHandlerBuilder()
        {
            Map<String, Action> actions = new ConcurrentHashMap<>();
            Map<String, String> links = null;
            Logger logger;
            Level level;

            @Override
            public HttpRequestHandlerBuilder log( Logger logger, Level level )
            {
                this.logger = logger;
                this.level = level;
                return this;
            }

            @Override
            public ChainBuilder method( String method )
            {
                HttpRequestHandlerBuilder b = this;
                ChainBuilder c = new ChainBuilder()
                {
                    private Action action;

                    @Override
                    public ChainBuilder add( Action action )
                    {
                        Objects.requireNonNull( action );
                        this.action = this.action == null ? action : this.action.andThen( action );
                        return this;
                    }

                    @Override
                    public HttpRequestHandlerBuilder end()
                    {
                        Objects.requireNonNull( action, "No action defined for " + method );

                        actions.merge( method.toUpperCase( Locale.ROOT ), action, Action::andThen );
                        return b;
                    }
                };

                return c;
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

                Action router = r -> actions.getOrDefault(
                        r.getHttpRequest().getRequestLine().getMethod().toUpperCase( Locale.ROOT ),
                        r1 -> Actions.sendError( r1, HttpStatus.SC_METHOD_NOT_ALLOWED, "Method not allowed" )
                )
                        .apply( r );

                Action action = logger == null || level == null ? router : new LogAction( logger, level, router );

                return ( req, resp, ctx ) -> {
                    Request request = Request.create( req, resp, ctx );
                    try {
                        action.apply( request );
                    }
                    finally {
                        if( request.isResponsePresent() ) {
                            resp.setEntity( request.getResponse().getEntity() );
                        }
                    }
                };
            }
        };
    }
}
