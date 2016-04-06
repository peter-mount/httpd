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
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
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

    default void setEntity( HttpEntity entity )
    {
        getHttpResponse().setEntity( entity );
    }

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

    /**
     * Return the request attribute n
     *
     * @param <T>
     * @param n   Attribute name
     *
     * @return value or null if not set
     */
    default <T> T getAttribute( String n )
    {
        return (T) getHttpContext().getAttribute( n );
    }

    /**
     * Return the request attribute n. If not set then use a mapping function to set it.
     *
     * @param <T>
     * @param n   Attribute name
     * @param f   function to map attribute to value. This may return null for no mapping.
     *
     * @return value or null if not set
     *
     * @throws org.apache.http.HttpException
     * @throws java.io.IOException
     */
    default <T> T getAttribute( String n, HttpFunction<String, T> f )
            throws HttpException,
                   IOException
    {
        T v = getAttribute( n );
        if( v == null ) {
            v = f.apply( n );
            setAttribute( n, v );
        }
        return v;
    }

    /**
     * Return the request attribute n. If not set then use a Supplier to get the default value and set that attribute to that value.
     *
     * @param <T>
     * @param n   Attribute name
     * @param s   Supplier. This may return null to not set an attribute
     *
     * @return value or null if not set
     *
     * @throws org.apache.http.HttpException
     * @throws java.io.IOException
     */
    default <T> T getAttribute( String n, HttpSupplier<T> s )
            throws HttpException,
                   IOException
    {
        T v = getAttribute( n );
        if( v == null ) {
            v = s.get();
            setAttribute( n, v );
        }
        return v;
    }

    default Request removeAttribute( String n )
    {
        getHttpContext().removeAttribute( n );
        return this;
    }

    default Request setAttribute( String n, Object v )
    {
        if( v == null ) {
            getHttpContext().removeAttribute( n );
        }
        else {
            getHttpContext().setAttribute( n, v );
        }
        return this;
    }

    default boolean isAttributePresent( String n )
    {
        return getHttpContext().getAttribute( n ) != null;
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

    /**
     * {@link ZoneId} for UTC which everything in the system runs under
     */
    static final ZoneId UTC = ZoneId.of( "UTC" );

    /**
     * {@link Zoneid} for London used in the front end - i.e. shows correct schedules when Daylight Savings Time is in effect
     * not those one hour earlier
     */
    static final ZoneId LONDON = ZoneId.of( "Europe/London" );

    default Request addHeader( String n, String v )
    {
        getHttpResponse().addHeader( n, v );
        return this;
    }

    default Request addHeader( String n, Instant i )
    {
        return addHeader( n, i.atZone( UTC ) );
    }

    /*
    Date:Tue, 29 Mar 2016 12:27:11 GMT
Expires:Wed, 30 Mar 2016 11:27:11 GMT
Keep-Alive:timeout=5, max=95
last-modified:Tue, 29 Mar 2016 12:27:11 GMT
     */
    default Request addHeader( String n, ZonedDateTime zdt )
    {
        return addHeader( n, String.format( "%3s, %02d %3s %d %02d:%02d:%02d GMT",
                                            zdt.getDayOfWeek().getDisplayName( TextStyle.SHORT, Locale.ENGLISH ),
                                            zdt.getDayOfMonth(),
                                            zdt.getMonth().getDisplayName( TextStyle.SHORT, Locale.ENGLISH ),
                                            zdt.getYear(),
                                            zdt.getHour(),
                                            zdt.getMinute(),
                                            zdt.getSecond()
                  ) );
    }

    default Request addHeader( String n, LocalDateTime dt )
    {
        return addHeader( n, dt.atZone( LONDON ) );
    }

    default Request expires( Instant instant )
    {
        return addHeader( "Expires", instant );
    }

    default Request expires( ZonedDateTime dt )
    {
        return addHeader( "Expires", dt );
    }

    default Request expires( LocalDateTime dt )
    {
        return addHeader( "Expires", dt );
    }

    default Request expiresIn( Duration d )
    {
        return expires( LocalDateTime.now().plus( d ) );
    }

    default Request expiresIn( long v, ChronoUnit unit )
    {
        return expiresIn( Duration.of( v, unit ) );
    }

    default Request lastModified( Instant instant )
    {
        return addHeader( "last-modified", instant );
    }

    default Request lastModified( ZonedDateTime dt )
    {
        return addHeader( "last-modified", dt );
    }

    default Request lastModified( LocalDateTime dt )
    {
        return addHeader( "last-modified", dt );
    }

    default Request maxAge( Duration d )
    {
        long max = d.getSeconds();
        return addHeader( "Cache-Control", "public, max-age=" + max + ", s-maxage=" + max + ", no-transform" );
    }

    default Request maxAge( long v, ChronoUnit unit )
    {
        return maxAge( Duration.of( v, unit ) );
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

    /**
     * Return's the path as a String array.
     * <p>
     * This will be the request's URI split at every '/'. The first element will always be blank as the uri will start with / so the uri "/test/ldb/MDE" will
     * return the array ["","test","ldb","MDE"].
     * <p>
     * This is commonly used to extract elements from a URI
     *
     * @return
     *
     * @throws IOException
     */
    default String[] getPath()
            throws IOException
    {
        // Implementation note: we cache the value in the request
        String path[] = getAttribute( "request.path" );
        if( path == null ) {
            path = getURI().getPath().split( "/" );
            setAttribute( "request.path", path );
        }
        return path;
    }

    /**
     * Return a path element.
     * <p>
     * This will return the numbered path element. Element 0 will always be "" as the url always starts with / so for the uri "/test/ldb/MDE":
     * <ol>
     * <li>{@code getPath(0)} always returns ""</li~>
     * <li>{@code getPath(1)} returns "test"</li~>
     * <li>{@code getPath(2)} returns "ldb"</li~>
     * <li>{@code getPath(3)} returns "MDE"</li~>
     * <li>{@code getPath(4)} returns null</li~>
     * </ol>
     *
     * @param idx element to return
     *
     * @return path element or null
     *
     * @throws IOException
     */
    default String getPath( int idx )
            throws IOException
    {
        String path[] = getPath();
        return path == null || path.length <= idx ? null : path[idx];
    }

    /**
     * Returns the path length.
     *
     * @return path length.
     *
     * @throws IOException
     */
    default int getPathLength()
            throws IOException
    {
        String path[] = getPath();
        return path == null ? 0 : path.length;
    }
}
