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
 */
@FunctionalInterface
public interface HttpSupplier<T>
{

    T get()
            throws HttpException,
                   IOException;
}
