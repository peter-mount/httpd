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
package onl.area51.httpd.filter;

import java.io.IOException;
import onl.area51.httpd.HttpRequestHandlerBuilder;
import onl.area51.httpd.action.Request;
import org.apache.http.HttpException;
import org.apache.http.protocol.HttpRequestHandler;

/**
 * A filter for Requests.
 * <p>
 * If defined this will be invoked by a {@link HttpRequestHandler} built by {@link HttpRequestHandlerBuilder} and progress to the underlying handler will only
 * be performed if this method returns true.
 * <p>
 * {@link RequestPredicate}s can be chained with the or method. When the returned instance is used then each instance is called in sequence until one returns
 * true. It
 * will return false only if every instance returns false.
 * <p>
 * An example use for this is authentication. You can implement Basic or Digest authentication using this interface.
 *
 * @author peter
 */
@FunctionalInterface
public interface RequestPredicate
{

    /**
     * Perform a test against the request
     *
     * @param request Request
     *
     * @return true if test passes
     *
     * @throws IOException
     * @throws HttpException
     */
    boolean test( Request request )
            throws IOException,
                   HttpException;

    default RequestPredicate or( RequestPredicate other )
    {
        return request -> test( request ) || other.test( request );
    }

    static RequestPredicate or( RequestPredicate a, RequestPredicate b )
    {
        return a == null ? b : b == null ? a : a.or( b );
    }

    default RequestPredicate and( RequestPredicate other )
    {
        return request -> test( request ) && other.test( request );
    }

    static RequestPredicate and( RequestPredicate a, RequestPredicate b )
    {
        return a == null ? b : b == null ? a : a.and( b );
    }

}
