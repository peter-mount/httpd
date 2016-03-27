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
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;

/**
 *
 * @author peter
 */
public interface Request
        extends AutoCloseable
{

    Response getResponse();

    boolean isResponsePresent();

    HttpRequest getHttpRequest();

    HttpResponse getHttpResponse();

    HttpContext getHttpContext();

    Collection<String> getParamNames()
            throws IOException;

    URI getURI()
            throws IOException;

    default Stream<String> paramNames()
            throws IOException
    {
        return getParamNames().stream();
    }

    String getParam( String n )
            throws IOException;

    default int getParamInt( String n )
            throws IOException
    {
        String s = getParam( n );
        return s == null ? 0 : Integer.parseInt( s );
    }

    default Object getAttribute( String n )
    {
        return getHttpContext().getAttribute( n );
    }

    default String getString( String n )
    {
        return Objects.toString( getAttribute( n ), null );
    }

    default String getString( String n, String d )
    {
        String s = getString( n );
        return s == null ? d : s;
    }

    default int getInt( String n )
    {
        Object o = getAttribute( n );
        if( o == null ) {
            return 0;
        }
        if( o instanceof Number ) {
            return ((Number) o).intValue();
        }
        return Integer.parseInt( o.toString() );
    }

    default long getLong( String n )
    {
        Object o = getAttribute( n );
        if( o == null ) {
            return 0;
        }
        if( o instanceof Number ) {
            return ((Number) o).longValue();
        }
        return Long.parseLong( o.toString() );
    }

    default double getDouble( String n )
    {
        Object o = getAttribute( n );
        if( o == null ) {
            return 0;
        }
        if( o instanceof Number ) {
            return ((Number) o).doubleValue();
        }
        return Double.parseDouble( o.toString() );
    }

    @Override
    default void close()
    {
    }

    static Request create( HttpRequest req, HttpResponse resp, HttpContext ctx )
    {
        return new Request()
        {
            Response response;
            URI uri;
            Map<String, String> params;

            private String decode( String s )
            {
                try {
                    return URLDecoder.decode( s, "UTF-8" );
                }
                catch( UnsupportedEncodingException ex ) {
                    return s;
                }
            }

            private void decodeParams()
                    throws IOException
            {
                if( uri == null ) {
                    try {
                        uri = new URI( req.getRequestLine().getUri() );

                        String q = uri.getQuery();
                        params = q == null || q.isEmpty()
                                 ? Collections.emptyMap()
                                 : Stream.of( q.split( "&" ) )
                                .map( s -> s.split( "=", 2 ) )
                                .collect( Collectors.toMap( p -> decode( p[0] ), p -> p.length == 1 ? "" : decode( p[1] ) ) );
                    }
                    catch( URISyntaxException ex ) {
                        throw new IOException( ex );
                    }
                }
            }

            @Override
            public URI getURI()
                    throws IOException
            {
                decodeParams();
                return uri;
            }

            @Override
            public Collection<String> getParamNames()
                    throws IOException
            {
                decodeParams();
                return params.keySet();
            }

            @Override
            public String getParam( String n )
                    throws IOException
            {
                decodeParams();
                return params.get( n );
            }

            @Override
            public Response getResponse()
            {
                if( response == null ) {
                    response = Response.create( this );
                }
                return response;
            }

            @Override
            public boolean isResponsePresent()
            {
                return response != null;
            }

            @Override
            public HttpRequest getHttpRequest()
            {
                return req;
            }

            @Override
            public HttpResponse getHttpResponse()
            {
                return resp;
            }

            @Override
            public HttpContext getHttpContext()
            {
                return ctx;
            }
        };
    }

    default Request wrap( Map<String, Object> params )
    {

        HttpContext ctx = new HttpContext()
        {
            @Override
            public Object getAttribute( String id )
            {
                Object o = params == null || params.isEmpty() ? null : params.get( id );
                return o == null ? getHttpContext().getAttribute( id ) : o;
            }

            @Override
            public void setAttribute( String id, Object obj )
            {
                getHttpContext().setAttribute( id, obj );
            }

            @Override
            public Object removeAttribute( String id )
            {
                return getHttpContext().removeAttribute( id );
            }
        };

        Request delegate = this;
        return new Request()
        {
            @Override
            public URI getURI()
                    throws IOException
            {
                return delegate.getURI();
            }

            @Override
            public Collection<String> getParamNames()
                    throws IOException
            {
                return delegate.getParamNames();
            }

            @Override
            public String getParam( String n )
                    throws IOException
            {
                return delegate.getParam( n );
            }

            @Override
            public Response getResponse()
            {
                return delegate.getResponse();
            }

            @Override
            public boolean isResponsePresent()
            {
                return delegate.isResponsePresent();
            }

            @Override
            public HttpRequest getHttpRequest()
            {
                return delegate.getHttpRequest();
            }

            @Override
            public HttpResponse getHttpResponse()
            {
                return delegate.getHttpResponse();
            }

            @Override
            public HttpContext getHttpContext()
            {
                return ctx;
            }

        };
    }
}
