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
import onl.area51.httpd.util.ContentTypeResolver;
import org.apache.http.HttpStatus;
import org.apache.http.entity.InputStreamEntity;

/**
 * A set of builders to provide specific actions
 *
 * @author peter
 */
public interface ActionBuilder
{

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
     * @throws java.io.IOException
     */
    static void renderResource( Class<?> clazz, Request r, URI uri )
            throws IOException
    {
        String url = uri.getPath();
        if( url.contains( "//" ) ) {
            url = url.replace( "//", "/" );
        }
        if( url.contains( "/." ) ) {
            Action.sendError( r, HttpStatus.SC_FORBIDDEN, url );
        }
        else {
            String path = (url.startsWith( "/" ) ? "/META-INF/resources" : "/META-INF/resources/") + url;
            if( path.endsWith( "/" ) ) {
                path = path + "index.html";
            }
            InputStream is = clazz.getResourceAsStream( path );
            if( is == null ) {
                Action.sendError( r, HttpStatus.SC_NOT_FOUND, url );
            }
            else if( r.isResponsePresent() ) {
                try {
                    r.getResponse()
                            .copy( is );
                }
                finally {
                    is.close();
                }
            }
            else {
                Action.sendOk( r, new InputStreamEntity( is, ContentTypeResolver.resolve( path ) ) );
            }
        }
    }

}
