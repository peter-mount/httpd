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
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import onl.area51.httpd.filter.RequestPredicate;
import org.apache.http.HttpException;

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

    static Action and( Action a, Action b )
    {
        return a == null ? b : b == null ? a : a.andThen( b );
    }

    default Action andThenIf( boolean andThen, Supplier<Action> after )
    {
        return andThen ? andThen( after.get() ) : this;
    }

    default Action compose( Action before )
    {
        Objects.requireNonNull( before );
        return before.andThen( this );
    }

    default Action composeIf( boolean compose, Supplier<Action> before )
    {
        return compose ? compose( before.get() ) : this;
    }

    default Action wrap( Action before, Action after )
    {
        return compose( before ).andThen( after );
    }

    default Action wrapif( boolean wrap, Supplier<Action> before, Supplier<Action> after )
    {
        return wrap ? wrap( before.get(), after.get() ) : this;
    }

    default Action wrapif( boolean wrap, UnaryOperator<Action> mapper )
    {
        return wrap ? mapper.apply( this ) : this;
    }

    default Action filterRequest( RequestPredicate p )
    {
        return p == null ? this : r -> {
            if( p.test( r ) ) {
                apply( r );
            }
        };
    }

    static Action filterRequest( Action a, RequestPredicate p )
    {
        return a.filterRequest( p );
    }

    default Action filter( Predicate<Request> p )
    {
        return p == null ? this : r -> {
            if( p.test( r ) ) {
                apply( r );
            }
        };
    }

    static Action filter( Action a, Predicate<Request> p )
    {
        return a.filter( p );
    }

    /**
     *
     * @param req
     *
     * @return
     *
     * @deprecated use {@link Actions#isOk(onl.area51.httpd.action.Request) }
     */
    @Deprecated
    static boolean isOk( Request req )
    {
        return Actions.isOk( req );
    }
}
