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

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import onl.area51.httpd.util.ContentTypeResolver;
import onl.area51.httpd.util.PathEntity;
import org.apache.http.HttpStatus;
import org.apache.http.entity.InputStreamEntity;

/**
 * A set of builders to provide specific actions
 *
 * @author peter
 */
public interface ActionBuilders
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
     * @return
     */
    static Action resourceAction()
    {
        return r -> {
            String url = r.getHttpRequest().getRequestLine().getUri();
            if( url.contains( "//" ) ) {
                url = url.replace( "//", "/" );
            }
            if( url.contains( "/." ) ) {
                Action.sendError( r, HttpStatus.SC_FORBIDDEN, url );
            }
            else {
                url = "/META-INF/resources/" + url;
                if( url.endsWith( "/" ) ) {
                    url = url + "index.html";
                }
                InputStream is = ActionBuilders.class.getResourceAsStream( url );
                if( is == null ) {
                    Action.sendError( r, HttpStatus.SC_NOT_FOUND, url );
                }
                else {
                    Action.sendOk( r, new InputStreamEntity( is, ContentTypeResolver.resolve( url ) ) );
                }
            }
        };
    }

    /**
     * Expose some other resource as a resource
     *
     * @param uri The URI of the root of the filesystem
     *
     * @return
     */
    static Action pathResourceAction( URI uri )
    {
        Path basePath = Paths.get( uri );

        return r -> {
            String url = r.getHttpRequest().getRequestLine().getUri();
            if( url.contains( "//" ) ) {
                url = url.replace( "//", "/" );
            }
            if( url.contains( "/." ) ) {
                Action.sendError( r, HttpStatus.SC_FORBIDDEN, url );
            }
            else {
                if( url.endsWith( "/" ) ) {
                    url = url + "index.html";
                }

                Path path = basePath.resolve( url );
                if( Files.exists( path, LinkOption.NOFOLLOW_LINKS ) ) {
                    Action.sendOk( r, new PathEntity( path ) );
                }
                else {
                    Action.sendError( r, HttpStatus.SC_NOT_FOUND, url );
                }
            }
        };
    }
    
    static Action pathResourceSizeAction( URI uri )
    {
        Path basePath = Paths.get( uri );

        return r -> {
            String url = r.getHttpRequest().getRequestLine().getUri();
            if( url.contains( "//" ) ) {
                url = url.replace( "//", "/" );
            }
            if( url.contains( "/." ) ) {
                Action.sendError( r, HttpStatus.SC_FORBIDDEN, url );
            }
            else {
                if( url.endsWith( "/" ) ) {
                    url = url + "index.html";
                }

                Path path = basePath.resolve( url );
                if( Files.exists( path, LinkOption.NOFOLLOW_LINKS ) ) {
                    Action.sendOk( r, new PathEntity( path ) );
                }
                else {
                    Action.sendError( r, HttpStatus.SC_NOT_FOUND, url );
                }
            }
        };
    }

}
