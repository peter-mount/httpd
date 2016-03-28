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
import java.util.logging.Level;
import java.util.logging.Logger;
import onl.area51.httpd.action.Action;
import onl.area51.httpd.action.Request;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;

/**
 * Simple HttpAction to log a request
 *
 * @author peter
 */
public class LogAction
        implements Action
{

    private final Logger logger;
    private final Level level;
    private final Action action;

    public LogAction( Logger logger, Level level, Action action )
    {
        this.logger = logger;
        this.level = level;
        this.action = action;
    }

    @Override
    public void apply( Request r )
            throws HttpException,
                   IOException
    {
        try {
            HttpRequest request = r.getHttpRequest();
            if( request instanceof HttpEntityEnclosingRequest ) {
                HttpEntityEnclosingRequest req = (HttpEntityEnclosingRequest) request;
                HttpEntity entity = req.getEntity();
                logger.log( level, () -> request.getRequestLine().getMethod() + ": " + request.getRequestLine().getUri() + " " + entity.getContentLength() + " " + entity.getContentType() );
            }
            else {
                logger.log( level, () -> request.getRequestLine().getMethod() + ": " + request.getRequestLine().getUri() );
            }

            action.apply( r );

            if( logger.isLoggable( level ) ) {
                HttpResponse response = r.getHttpResponse();
                StatusLine statusLine = response.getStatusLine();
                HttpEntity entity = response.getEntity();
                logger.log( level, () -> request.getRequestLine().getMethod() + ": " + request.getRequestLine().getUri()
                            + " " + (statusLine == null ? -1 : statusLine.getStatusCode())
                            + " " + (entity == null ? -1 : entity.getContentLength()) );
            }
        } catch( RuntimeException |
                 HttpException |
                 IOException ex ) {
            if( logger.isLoggable( level ) ) {
                HttpRequest request = r.getHttpRequest();
                HttpResponse response = r.getHttpResponse();
                StatusLine statusLine = response.getStatusLine();
                HttpEntity entity = response.getEntity();
                logger.log( level, ex, () -> request.getRequestLine().getMethod() + ": " + request.getRequestLine().getUri()
                            + " " + (statusLine == null ? -1 : statusLine.getStatusCode())
                            + " " + (entity == null ? -1 : entity.getContentLength()) );
            }
            throw ex;
        }
    }

}
