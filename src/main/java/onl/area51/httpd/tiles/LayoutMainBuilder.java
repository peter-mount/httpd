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
package onl.area51.httpd.tiles;

import java.util.function.UnaryOperator;
import onl.area51.httpd.action.Action;

/**
 * A default tile common to all html pages
 *
 * @author peter
 */
public interface LayoutMainBuilder
{

    /**
     * Optional action to add entries to the pages header section
     *
     * @param header
     *
     * @return
     */
    LayoutMainBuilder setHeader( Action header );

    /**
     * The page title, used when static.
     *
     * @param title
     *
     * @return
     */
    LayoutMainBuilder setTitle( String title );

    /**
     * Optional transform to apply to the title string
     *
     * @param titleTransform
     *
     * @return
     */
    LayoutMainBuilder setTitleTransform( UnaryOperator<String> titleTransform );

    /**
     * The page body
     *
     * @param body
     *
     * @return
     */
    LayoutMainBuilder setBody( Action body );

    /**
     * Build the action
     *
     * @return
     */
    Action build();

    static LayoutMainBuilder builder()
    {
        return new LayoutMainBuilder()
        {

            private Action header;
            private UnaryOperator<String> titleTransform = UnaryOperator.identity();
            private String title;
            private Action body;

            @Override
            public LayoutMainBuilder setHeader( Action header )
            {
                this.header = header;
                return this;
            }

            @Override
            public LayoutMainBuilder setTitle( String title )
            {
                this.title = title;
                return this;
            }

            @Override
            public LayoutMainBuilder setTitleTransform( UnaryOperator<String> titleTransform )
            {
                this.titleTransform = titleTransform;
                return this;
            }

            @Override
            public LayoutMainBuilder setBody( Action body )
            {
                this.body = body;
                return this;
            }

            @Override
            public Action build()
            {
                return request -> {
                    request.getResponse()
                            .begin( "html" )
                            // Page header
                            .begin( "head" )
                            .begin( "meta" )
                            .attr( "http-equiv", "Content-Type" )
                            .attr( "content", "text/html; charset=UTF-8" )
                            .end()
                            // Title
                            .begin( "title" )
                            .write( titleTransform.apply( request.getString( "pageTitle", title ) ) )
                            .end()
                            // Optional extras in header
                            .exec( header )
                            .end()
                            .begin( "body" )
                            .exec( body )
                            .end()
                            .end();
                };
            }
        };
    }
}
