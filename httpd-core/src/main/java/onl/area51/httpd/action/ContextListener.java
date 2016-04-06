/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package onl.area51.httpd.action;

import java.io.IOException;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;

/**
 *
 * @author peter
 */
public interface ContextListener
{

    void begin( HttpRequest req, HttpContext ctx )
            throws HttpException,
                   IOException;

    void end( HttpResponse resp, HttpContext ctx )
            throws HttpException,
                   IOException;

}
