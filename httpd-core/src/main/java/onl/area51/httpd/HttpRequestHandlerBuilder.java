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
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.HttpStatus;
import org.apache.http.protocol.HttpRequestHandler;
import onl.area51.httpd.action.Action;
import onl.area51.httpd.action.Actions;
import onl.area51.httpd.action.HttpBiFunction;
import onl.area51.httpd.action.HttpFunction;
import onl.area51.httpd.action.HttpSupplier;
import onl.area51.httpd.action.Request;
import org.apache.http.HttpEntity;

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

    /**
     * If invoked then CDI scopes (if present) are disabled. Use this for request handlers that handle static content as it will save memory
     * <p>
     * Usually used
     *
     * @return
     */
    HttpRequestHandlerBuilder unscoped();

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
         * Apply an action if a Response is present
         *
         * @param action
         *
         * @return
         */
        default ChainBuilder ifResponsePresent( Action action )
        {
            return add( r -> {
                if( r.isResponsePresent() ) {
                    action.apply( r );
                }
            } );
        }

        default ChainBuilder ifResponseAbsent( Action action )
        {
            return add( r -> {
                if( !r.isResponsePresent() ) {
                    action.apply( r );
                }
            } );
        }

        default ChainBuilder setAttribute( String n, Object v )
        {
            return add( r -> r.setAttribute( n, v ) );
        }

        default ChainBuilder setAttribute( String n, HttpSupplier<Object> s )
        {
            return add( r -> r.setAttribute( n, s.get() ) );
        }

        default ChainBuilder setAttribute( String n, HttpFunction<Request, Object> f )
        {
            return add( r -> r.setAttribute( n, f.apply( r ) ) );
        }

        default ChainBuilder setAttribute( String n, HttpBiFunction<String, Request, Object> f )
        {
            return add( r -> r.setAttribute( n, f.apply( n, r ) ) );
        }

        /**
         * Apply an action if a request attribute is set
         *
         * @param n
         * @param action
         *
         * @return
         */
        default ChainBuilder ifAttributePresent( String n, Action action )
        {
            return add( r -> {
                if( r.isAttributePresent( n ) ) {
                    action.apply( r );
                }
            } );
        }

        /**
         * If a request attribute is set then perform some action and store it's result as another attribute.
         * <p>
         * This is the same as {@code ifAttributePresent( n, r -> r.setAttribute( n2, action.apply( r ) ) )}
         *
         * @param n        Attribute to expect to be set
         * @param n2       Attribute to set if the action is invoked
         * @param function Action to invoke if n exists. It's result is set to attribute n2
         *
         * @return
         */
        default ChainBuilder ifAttributePresentSetAttribute( String n, String n2, HttpFunction<Request, Object> function )
        {
            return ifAttributePresent( n, r -> r.setAttribute( n2, function.apply( r ) ) );
        }

        /**
         * If a request attribute is set then perform some action and store it's result as another attribute.
         * <p>
         * The function accepts two parameters, request and the attibute n's value. It's result will be set to n2. If null then n2 is unset.
         *
         * @param n        Attribute to expect to be set
         * @param n2       Attribute to set if the action is invoked
         * @param function Action to invoke if n exists. It's result is set to attribute n2
         *
         * @return
         */
        default ChainBuilder ifAttributePresentSetAttribute( String n, String n2, HttpBiFunction<Request, Object, Object> function )
        {
            return ifAttributePresent( n, r -> r.setAttribute( n2, function.apply( r, r.getAttribute( n ) ) ) );
        }

        /**
         * If a request attribute is set then perform some action and store it's result as another attribute.
         * <p>
         * This is the same as {@code ifAttributePresent( n, r -> r.setAttribute( n2, action.apply( r ) ) )}
         *
         * @param n        Attribute to expect to be set
         * @param n2       Attribute to set if the action is invoked
         * @param function Action to invoke if n exists. It's result is set to attribute n2
         *
         * @return
         */
        default ChainBuilder ifAttributePresentSetAttribute( String n, String n2, HttpSupplier<Object> function )
        {
            return ifAttributePresent( n, r -> r.setAttribute( n2, function.get() ) );
        }

        /**
         * Apply an action if a request attribute is absent
         *
         * @param n
         * @param action
         *
         * @return
         */
        default ChainBuilder ifAttributeAbsent( String n, Action action )
        {
            return add( r -> {
                if( !r.isAttributePresent( n ) ) {
                    action.apply( r );
                }
            } );
        }

        /**
         * If a request attribute is absent then perform some action and store it's result as another attribute.
         * <p>
         * This is the same as {@code ifAttributeAbsent( n, r -> r.setAttribute( n2, action.apply( r ) ) )}
         *
         * @param n        Attribute to expect to be set
         * @param n2       Attribute to set if the action is invoked
         * @param function Action to invoke if n exists. It's result is set to attribute n2
         *
         * @return
         */
        default ChainBuilder ifAttributeAbsentSetAttribute( String n, String n2, HttpFunction<Request, Object> function )
        {
            return ifAttributeAbsent( n, r -> r.setAttribute( n2, function.apply( r ) ) );
        }

        /**
         * If a request attribute is absent then perform some action and store it's result as another attribute.
         * <p>
         * This is the same as {@code ifAttributeAbsent( n, r -> r.setAttribute( n2, action.apply( r ) ) )}
         *
         * @param n        Attribute to expect to be set
         * @param n2       Attribute to set if the action is invoked
         * @param supplier Action to invoke if n exists. It's result is set to attribute n2
         *
         * @return
         */
        default ChainBuilder ifAttributeAbsentSetAttribute( String n, String n2, HttpSupplier< Object> supplier )
        {
            return ifAttributeAbsent( n, r -> r.setAttribute( n2, supplier.get() ) );
        }

        /**
         * If a request attribute is absent then perform some action and store it's result in that attribute.
         * <p>
         * This is the same as {@code ifAttributeAbsent( n, n, action )}
         *
         * @param n        Attribute to expect to be set
         * @param function Action to invoke if n exists. It's result is set to attribute n
         *
         * @return
         */
        default ChainBuilder ifAttributeAbsentSetAttribute( String n, HttpFunction<Request, Object> function )
        {
            return ChainBuilder.this.ifAttributeAbsentSetAttribute( n, n, function );
        }

        /**
         * If a request attribute is absent then perform some action and store it's result in that attribute.
         * <p>
         * This is the same as {@code ifAttributeAbsent( n, n, action )}
         *
         * @param n        Attribute to expect to be set
         * @param supplier Action to invoke if n exists. It's result is set to attribute n
         *
         * @return
         */
        default ChainBuilder ifAttributeAbsentSetAttribute( String n, HttpSupplier<Object> supplier )
        {
            return ChainBuilder.this.ifAttributeAbsentSetAttribute( n, n, supplier );
        }

        /**
         * Set a request attribute to the value of a request parameter. If the parameter is not set then the attribute will not be set.
         *
         * @param p request parameter and attribute name
         *
         * @return
         */
        default ChainBuilder setAttributeFromParameter( String p )
        {
            return setAttributeFromParameter( p, p );
        }

        /**
         * Set a request attribute to the value of a request parameter. If the parameter is not set then the attribute will not be set.
         *
         * @param a request attribute to set
         * @param p request parameter to retrieve
         *
         * @return
         */
        default ChainBuilder setAttributeFromParameter( String a, String p )
        {
            return add( r -> r.setAttribute( a, r.getParam( p ) ) );
        }

        /**
         * Complete the chain.
         * <p>
         * Note, if a chain has already been created then this will append to that chain.
         *
         * @return
         */
        HttpRequestHandlerBuilder end();

        /**
         * Send an OK response
         *
         * @param supplier Supplier of the entity
         *
         * @return
         */
        default ChainBuilder sendOk( HttpSupplier<? extends HttpEntity> supplier )
        {
            return add( r -> Actions.sendOk( r, supplier.get() ) );
        }

        /**
         * Send an OK response
         *
         * @param supplier Supplier of the entity
         *
         * @return
         */
        default ChainBuilder sendOk( Supplier<? extends HttpEntity> supplier )
        {
            return add( r -> Actions.sendOk( r, supplier.get() ) );
        }

        /**
         * Send an ok response if an attribute is present
         *
         * @param n        Attribute name
         * @param supplier Supplier of the entity
         *
         * @return
         */
        default ChainBuilder ifAttributePresentSendOk( String n, HttpSupplier<? extends HttpEntity> supplier )
        {
            return ifAttributePresent( n, r -> Actions.sendOk( r, supplier.get() ) );
        }

        /**
         * Send an ok response if an attribute is present
         *
         * @param n        Attribute name
         * @param supplier Supplier of the entity
         *
         * @return
         */
        default ChainBuilder ifAttributePresentSendOk( String n, HttpFunction<Request, ? extends HttpEntity> supplier )
        {
            return ifAttributePresent( n, r -> Actions.sendOk( r, supplier.apply( r ) ) );
        }

        /**
         * Send an ok response if an attribute is present
         *
         * @param n        Attribute name
         * @param supplier Supplier of the entity
         *
         * @return
         */
        default ChainBuilder ifAttributePresentSendOk( String n, HttpBiFunction<String, Request, ? extends HttpEntity> supplier )
        {
            return ifAttributePresent( n, r -> Actions.sendOk( r, supplier.apply( n, r ) ) );
        }

        /**
         * Send an ok response if an attribute is absent
         *
         * @param n        Attribute name
         * @param supplier Supplier of the entity
         *
         * @return
         */
        default ChainBuilder ifAttributeAbsentSendOk( String n, HttpSupplier<? extends HttpEntity> supplier )
        {
            return ifAttributeAbsent( n, r -> Actions.sendOk( r, supplier.get() ) );
        }

        /**
         * Send an ok response if an attribute is absent
         *
         * @param n        Attribute name
         * @param supplier Supplier of the entity
         *
         * @return
         */
        default ChainBuilder ifAttributeAbsentSendOk( String n, HttpFunction<Request, ? extends HttpEntity> supplier )
        {
            return ifAttributeAbsent( n, r -> Actions.sendOk( r, supplier.apply( r ) ) );
        }

        /**
         * Send an ok response if an attribute is absent
         *
         * @param n        Attribute name
         * @param supplier Supplier of the entity
         *
         * @return
         */
        default ChainBuilder ifAttributeAbsentSendOk( String n, HttpBiFunction<String, Request, ? extends HttpEntity> supplier )
        {
            return ifAttributeAbsent( n, r -> Actions.sendOk( r, supplier.apply( n, r ) ) );
        }

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

        /**
         * Respond with an error
         *
         * @param code HTTP status code
         *
         * @return
         */
        default ChainBuilder ifAttributeAbsentSendError( String n, int code )
        {
            return ChainBuilder.this.ifAttributeAbsentSendError( n, code, String.valueOf( code ) );
        }

        /**
         * Respond with an error
         *
         * @param code    HTTP status code
         * @param message Message
         *
         * @return
         */
        default ChainBuilder ifAttributeAbsentSendError( String n, int code, String message )
        {
            return add( r -> {
                if( !r.isAttributePresent( n ) ) {
                    Actions.sendError( r, code, message );
                }
            } );
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
        default ChainBuilder ifAttributeAbsentSendError( String n, int code, String fmt, Object... args )
        {
            return add( r -> {
                if( !r.isAttributePresent( n ) ) {
                    Actions.sendError( r, code, fmt, args );
                }
            } );
        }
    }

    static HttpRequestHandlerBuilder create()
    {
        return new HttpRequestHandlerBuilder()
        {
            boolean unscoped;
            Map<String, Action> actions = new ConcurrentHashMap<>();
            Map<String, String> links = null;
            Logger logger;
            Level level;

            @Override
            public HttpRequestHandlerBuilder unscoped()
            {
                unscoped = true;
                return this;
            }

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

                Action action1 = logger == null || level == null ? router : new LogAction( logger, level, router );
                
                Action action = unscoped ? action1.compose( r -> r.setAttribute( "request.unscoped", true ) ) : action1;

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
