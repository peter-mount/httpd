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
import java.util.Objects;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;

/**
 *
 * @author peter
 */
public interface Action
{

    void apply( Request request )
            throws HttpException,
                   IOException;

    default Action andThen( Action after )
    {
        Objects.requireNonNull( after );
        return ( req ) -> {
            apply( req );
            if( isOk( req ) ) {
                after.apply( req );
            }
        };
    }

    default Action compose( Action before )
    {
        Objects.requireNonNull( before );
        return before.andThen( this );
    }

    static boolean isOk( Request req )
    {
        HttpResponse resp = req.getHttpResponse();
        return (resp.getStatusLine() == null || resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) && resp.getEntity() == null;
    }
}
