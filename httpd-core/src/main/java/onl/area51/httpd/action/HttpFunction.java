/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package onl.area51.httpd.action;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.function.Function;
import org.apache.http.HttpException;

/**
 *
 * @author peter
 * @param <T>
 * @param <R>
 */
@FunctionalInterface
public interface HttpFunction<T, R>
{

    R apply( T v )
            throws HttpException,
                   IOException;

    /**
     * Convert this consumer into a standard {@link Consumer}.
     * <p>
     * When an {@link IOException} is thrown then an {@link UncheckedIOException} will be thrown instead.
     * <p>
     * @return consumer
     */
    default Function<T, R> guard()
    {
        return guard( this );
    }

    /**
     * Convert an {@link IOConsumer} into a standard {@link Consumer}.
     * <p>
     * When an {@link IOException} is thrown then an {@link UncheckedIOException} will be thrown instead.
     * <p>
     * @param <V>
     * @param <T>
     * @param c IOConsumer to guard
     * <p>
     * @return consumer
     */
    static <V, T> Function<V, T> guard( HttpFunction<V, T> c )
    {
        return v -> {
            try {
                return c.apply( v );
            }
            catch( HttpException ex ) {
                throw new RuntimeException( ex );
            }
            catch( IOException ex ) {
                throw new UncheckedIOException( ex );
            }
        };
    }

    default <V> HttpFunction<V, R> compose( HttpFunction<? super V, ? extends T> before )
    {
        Objects.requireNonNull( before );
        return ( V v ) -> apply( before.apply( v ) );
    }

    default <V> HttpFunction<T, V> andThen( HttpFunction<? super R, ? extends V> after )
    {
        Objects.requireNonNull( after );
        return ( T t ) -> after.apply( apply( t ) );
    }

}
