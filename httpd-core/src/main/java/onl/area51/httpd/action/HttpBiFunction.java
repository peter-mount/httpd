/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package onl.area51.httpd.action;

import java.io.IOException;
import org.apache.http.HttpException;

/**
 *
 * @author peter
 * @param <T>
 * @param <R>
 */
@FunctionalInterface
public interface HttpBiFunction<T, U, R>
{

    R apply( T t, U u )
            throws HttpException,
                   IOException;

}
