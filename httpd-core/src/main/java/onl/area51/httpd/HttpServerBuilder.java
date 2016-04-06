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

import onl.area51.httpd.action.ActionRegistry;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLContext;
import onl.area51.httpd.action.ContextListener;
import org.apache.http.ConnectionClosedException;
import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.ExceptionLogger;
import org.apache.http.HttpConnectionFactory;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponseFactory;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.DefaultBHttpServerConnection;
import org.apache.http.impl.bootstrap.SSLServerSetupHandler;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.http.protocol.HttpExpectationVerifier;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.HttpRequestHandlerMapper;

/**
 *
 * @author peter
 */
public interface HttpServerBuilder
{

    HttpServerBuilder setListenerPort( int listenerPort );

    HttpServerBuilder setLocalAddress( InetAddress localAddress );

    HttpServerBuilder setSocketConfig( SocketConfig socketConfig );

    HttpServerBuilder setConnectionConfig( ConnectionConfig connectionConfig );

    HttpServerBuilder setHttpProcessor( HttpProcessor httpProcessor );

    HttpServerBuilder addInterceptorFirst( HttpResponseInterceptor itcp );

    HttpServerBuilder addInterceptorLast( HttpResponseInterceptor itcp );

    HttpServerBuilder addInterceptorFirst( HttpRequestInterceptor itcp );

    HttpServerBuilder addInterceptorLast( HttpRequestInterceptor itcp );

    HttpServerBuilder setServerInfo( String serverInfo );

    HttpServerBuilder setConnectionReuseStrategy( ConnectionReuseStrategy connStrategy );

    HttpServerBuilder setResponseFactory( HttpResponseFactory responseFactory );

    HttpServerBuilder setHandlerMapper( HttpRequestHandlerMapper handlerMapper );

    /**
     * Register a new handler
     *
     * @param pattern
     * @param handler
     *
     * @return
     */
    HttpServerBuilder registerHandler( String pattern, HttpRequestHandler handler );

    /**
     * Register a new handler
     *
     * @param pattern
     * @param handler
     *
     * @return
     */
    default HttpServerBuilder registerHandler( String pattern, HttpRequestHandlerBuilder handler )
    {
        return registerHandler( pattern, handler.build() );
    }

    /**
     * The builder for the global handler "/*" which is registered last
     *
     * @return
     */
    HttpRequestHandlerBuilder getGlobalHandlerBuilder();

    HttpServerBuilder setExpectationVerifier( HttpExpectationVerifier expectationVerifier );

    HttpServerBuilder setConnectionFactory( HttpConnectionFactory<? extends DefaultBHttpServerConnection> connectionFactory );

    HttpServerBuilder setSslSetupHandler( SSLServerSetupHandler sslSetupHandler );

    HttpServerBuilder setServerSocketFactory( ServerSocketFactory serverSocketFactory );

    HttpServerBuilder setSslContext( SSLContext sslContext );

    default HttpServerBuilder setExceptionLogger( ExceptionLogger exceptionLogger )
    {
        return setExceptionLogger( exceptionLogger, true );
    }

    HttpServerBuilder setExceptionLogger( ExceptionLogger exceptionLogger, boolean filter );

    HttpServerBuilder shutdown( long gracePeriod, TimeUnit gracePeriodUnit );

    /**
     * Notify a consumer of this builder.
     * <p>
     * An example is notifying CDI beans observing the builder class. Use it with
     * {@code builder.notify( CDI.current().getBeanManager()::fireEvent )} and it will notify each bean with the builder,
     * allowing them to add to the server configuration.
     *
     * @param action
     *
     * @return
     */
    default HttpServerBuilder notify( Consumer<ActionRegistry> action )
    {
        HttpServerBuilder b = this;

        action.accept( new ActionRegistry()
        {
            @Override
            public HttpRequestHandlerBuilder getGlobalHandlerBuilder()
            {
                return b.getGlobalHandlerBuilder();
            }

            @Override
            public ActionRegistry registerHandler( String pattern, HttpRequestHandler handler )
            {
                b.registerHandler( pattern, handler );
                return this;
            }

            @Override
            public ActionRegistry addResponseInterceptorFirst( HttpResponseInterceptor itcp )
            {
                b.addInterceptorFirst( itcp );
                return this;
            }

            @Override
            public ActionRegistry addResponseInterceptorLast( HttpResponseInterceptor itcp )
            {
                b.addInterceptorLast( itcp );
                return this;
            }

            @Override
            public ActionRegistry addRequestInterceptorFirst( HttpRequestInterceptor itcp )
            {
                b.addInterceptorFirst( itcp );
                return this;
            }

            @Override
            public ActionRegistry addRequestInterceptorLast( HttpRequestInterceptor itcp )
            {
                b.addInterceptorLast( itcp );
                return this;
            }

            @Override
            public ActionRegistry addContextListener( ContextListener cl )
            {
                return addRequestInterceptorFirst( ( r, c ) -> {
                    if( c.getAttribute( "request.unscoped" ) == null ) {
                        cl.begin( r, c );
                    }
                } )
                        .addResponseInterceptorLast( ( r, c ) -> {
                            if( c.getAttribute( "request.unscoped" ) == null ) {
                                cl.end( r, c );
                            }
                        } );
            }

        } );
        return this;
    }

    HttpServer build();

    static HttpServerBuilder builder()
    {
        return new HttpServerBuilder()
        {
            private final ServerBootstrap sb = ServerBootstrap.bootstrap();
            private long gracePeriod = 5;
            private TimeUnit gracePeriodUnit = TimeUnit.MINUTES;
            private HttpRequestHandlerBuilder globalHandler;

            @Override
            public HttpServerBuilder shutdown( long gracePeriod, TimeUnit gracePeriodUnit )
            {
                if( gracePeriod < 1 || gracePeriodUnit == null ) {
                    throw new IllegalArgumentException( "Invalid GracePeriod" );
                }
                this.gracePeriod = gracePeriod;
                this.gracePeriodUnit = gracePeriodUnit;
                return this;
            }

            @Override
            public HttpServerBuilder setListenerPort( int listenerPort )
            {
                sb.setListenerPort( listenerPort );
                return this;
            }

            @Override
            public HttpServerBuilder setLocalAddress( InetAddress localAddress )
            {
                sb.setLocalAddress( localAddress );
                return this;
            }

            @Override
            public HttpServerBuilder setSocketConfig( SocketConfig socketConfig )
            {
                sb.setSocketConfig( socketConfig );
                return this;
            }

            @Override
            public HttpServerBuilder setConnectionConfig( ConnectionConfig connectionConfig )
            {
                sb.setConnectionConfig( connectionConfig );
                return this;
            }

            @Override
            public HttpServerBuilder setHttpProcessor( HttpProcessor httpProcessor )
            {
                sb.setHttpProcessor( httpProcessor );
                return this;
            }

            @Override
            public HttpServerBuilder addInterceptorFirst( HttpResponseInterceptor itcp )
            {
                sb.addInterceptorFirst( itcp );
                return this;
            }

            @Override
            public HttpServerBuilder addInterceptorLast( HttpResponseInterceptor itcp )
            {
                sb.addInterceptorLast( itcp );
                return this;
            }

            @Override
            public HttpServerBuilder addInterceptorFirst( HttpRequestInterceptor itcp )
            {
                sb.addInterceptorFirst( itcp );
                return this;
            }

            @Override
            public HttpServerBuilder addInterceptorLast( HttpRequestInterceptor itcp )
            {
                sb.addInterceptorLast( itcp );
                return this;
            }

            @Override
            public HttpServerBuilder setServerInfo( String serverInfo )
            {
                sb.setServerInfo( serverInfo );
                return this;
            }

            @Override
            public HttpServerBuilder setConnectionReuseStrategy( ConnectionReuseStrategy connStrategy )
            {
                sb.setConnectionReuseStrategy( connStrategy );
                return this;
            }

            @Override
            public HttpServerBuilder setResponseFactory( HttpResponseFactory responseFactory )
            {
                sb.setResponseFactory( responseFactory );
                return this;
            }

            @Override
            public HttpServerBuilder setHandlerMapper( HttpRequestHandlerMapper handlerMapper )
            {
                sb.setHandlerMapper( handlerMapper );
                return this;
            }

            @Override
            public HttpServerBuilder registerHandler( String pattern, HttpRequestHandler handler )
            {
                sb.registerHandler( pattern, handler );
                return this;
            }

            @Override
            public HttpServerBuilder setExpectationVerifier( HttpExpectationVerifier expectationVerifier )
            {
                sb.setExpectationVerifier( expectationVerifier );
                return this;
            }

            @Override
            public HttpServerBuilder setConnectionFactory(
                    HttpConnectionFactory<? extends DefaultBHttpServerConnection> connectionFactory )
            {
                sb.setConnectionFactory( connectionFactory );
                return this;
            }

            @Override
            public HttpServerBuilder setSslSetupHandler( SSLServerSetupHandler sslSetupHandler )
            {
                sb.setSslSetupHandler( sslSetupHandler );
                return this;
            }

            @Override
            public HttpServerBuilder setServerSocketFactory( ServerSocketFactory serverSocketFactory )
            {
                sb.setServerSocketFactory( serverSocketFactory );
                return this;
            }

            @Override
            public HttpServerBuilder setSslContext( SSLContext sslContext )
            {
                sb.setSslContext( sslContext );
                return this;
            }

            @Override
            public HttpServerBuilder setExceptionLogger( ExceptionLogger exceptionLogger, boolean filter )
            {
                sb.setExceptionLogger( filter ? ex -> {
                    if( !(ex instanceof SocketTimeoutException) && !(ex instanceof ConnectionClosedException) ) {
                        exceptionLogger.log( ex );
                    }
                } : exceptionLogger );
                return this;
            }

            @Override
            public HttpRequestHandlerBuilder getGlobalHandlerBuilder()
            {
                if( globalHandler == null ) {
                    globalHandler = HttpRequestHandlerBuilder.create();
                }
                return globalHandler;
            }

            @Override
            public HttpServer build()
            {
                if( globalHandler != null ) {
                    sb.registerHandler( "/*", globalHandler.build() );
                }

                org.apache.http.impl.bootstrap.HttpServer server = sb.create();
                return new HttpServer()
                {
                    @Override
                    public void start()
                            throws IOException
                    {
                        server.start();
                    }

                    @Override
                    public void stop()
                    {
                        server.shutdown( gracePeriod, gracePeriodUnit );
                    }
                };
            }

        };
    }
}
