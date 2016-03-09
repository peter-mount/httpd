This is a simple httpd server based on the latestApache HTTPClient, extending it with Java 8 lambda support.

Quick example until I write proper documentation.

```java
import onl.area51.httpd.HttpAction;
import onl.area51.httpd.HttpServer;
import onl.area51.httpd.HttpServerBuilder;
import org.apache.http.HttpStatus;
import org.apache.http.entity.StringEntity;

public class Test
{
    public static void main(String... args)
        throws Exception
    {
        HttpServer server = HttpServerBuilder.builder()
                .setSocketConfig( SocketConfig.custom()
                        .setSoTimeout( 15000 )
                        .setTcpNoDelay( true )
                        .build() )
                .setListenerPort( 80 )
                .setServerInfo( "MyTest/1.1" )
                .setSslContext( null )
                .shutdown( 5, TimeUnit.SECONDS )
                .registerHandler( "*", HttpRequestHandlerBuilder.create()
                                  .method( "GET" )
                                  .add( (request,response,context) -> HttpAction.sendOk( response, new StringEntity( "Hello World" ) ) )
                                  .end()
                                  .build() )
                .build();

        server.start();

        Thread.sleep( 60000L );

        server.stop();
    }
}
```