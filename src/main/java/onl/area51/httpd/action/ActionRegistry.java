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

import onl.area51.httpd.HttpRequestHandlerBuilder;
import org.apache.http.protocol.HttpRequestHandler;

/**
 * Interface used by {@link HttpServerBuilder#notify(java.util.function.Consumer) } so we can notify beans to deploy themselves but not allow access to the server config
 */
public interface ActionRegistry
{

    /**
     * The builder for the global handler "/*" which is registered last
     *
     * @return
     */
    HttpRequestHandlerBuilder getGlobalHandlerBuilder();

    /**
     * Register a new handler
     *
     * @param pattern
     * @param handler
     * @return
     */
    ActionRegistry registerHandler( String pattern, HttpRequestHandler handler );

    /**
     * Register a new handler
     *
     * @param pattern
     * @param handler
     * @return
     */
    default ActionRegistry registerHandler( String pattern, HttpRequestHandlerBuilder handler )
    {
        return registerHandler( pattern, handler.build() );
    }
}
