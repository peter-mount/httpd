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

import java.util.Map;
import java.util.Objects;
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
