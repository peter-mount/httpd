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
import java.util.Objects;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;

/**
 *
 * @author peter
 */
public interface HttpAction
{

    void apply( HttpRequest request, HttpResponse response, HttpContext context )
            throws HttpException,
                   IOException;

    default HttpAction andThen( HttpAction after )
    {
        Objects.requireNonNull( after );
        return ( req, resp, ctx ) -> {
            apply( req, resp, ctx );
            if( isOk( resp ) ) {
                after.apply( req, resp, ctx );
            }
        };
    }

    default HttpAction compose( HttpAction before )
    {
        Objects.requireNonNull( before );
        return before.andThen( this );
    }

    static boolean isOk( HttpResponse resp )
    {
        return (resp.getStatusLine() == null || resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) && resp.getEntity() == null;
    }

    /**
     * Set the error response
     *
     * @param response
     * @param sc
     * @param message
     */
    static void sendError( HttpResponse response, int sc, String message )
    {
        response.setStatusCode( sc );
        response.setEntity( new StringEntity( "<html><body><h1>" + message + "</h1></body></html>", ContentType.TEXT_HTML ) );
    }

    /**
     * Set the error response
     *
     * @param response
     * @param sc
     * @param fmt
     * @param args
     */
    static void sendError( HttpResponse response, int sc, String fmt, Object... args )
    {
        sendError( response, sc, String.format( fmt, args ) );
    }

    /**
     * Send an OK entity
     *
     * @param response
     * @param entity
     */
    static void sendOk( HttpResponse response, HttpEntity entity )
    {
        response.setStatusCode( HttpStatus.SC_OK );
        response.setEntity( entity );
    }
}
