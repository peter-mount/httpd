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

import onl.area51.httpd.action.Action;

/**
 * A collection of builders for building tiles
 *
 * @author peter
 */
public class TileBuilders
{

    private TileBuilders()
    {
    }

    /**
     * Return a {@link LayoutMainBuilder} for building a pages outer content
     *
     * @return
     */
    public static LayoutMainBuilder layoutMainBuilder()
    {
        return LayoutMainBuilder.builder();
    }

    /**
     * A simple action that returns a string
     *
     * @param s
     *
     * @return
     */
    public static Action stringAction( String s )
    {
        return r -> r.getResponse().write( s );
    }
}
