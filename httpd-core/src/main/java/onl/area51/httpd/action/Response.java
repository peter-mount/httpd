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

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayDeque;
import java.util.Deque;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;

/**
 *
 * @author peter
 */
public interface Response
{

    default Response write( CharSequence seq )
            throws IOException
    {
        return write( seq, 0, seq.length() );
    }

    Response write( CharSequence seq, int s, int e )
            throws IOException;

    Response write( char v )
            throws IOException;

    default Response write( char[] v )
            throws IOException
    {
        return write( v, 0, v.length );
    }

    Response write( char[] v, int s, int l )
            throws IOException;

    default Response write( int v )
            throws IOException
    {
        return write( String.valueOf( v ) );
    }

    default Response write( long v )
            throws IOException
    {
        return write( String.valueOf( v ) );
    }

    default Response write( double v )
            throws IOException
    {
        return write( String.valueOf( v ) );
    }

    default Response copy( InputStream is )
            throws IOException
    {
        try( Reader r = new InputStreamReader( is ) ) {
            return copy( r );
        }
    }

    default Response copy( Reader r )
            throws IOException
    {
        char b[] = new char[1024];
        int s = r.read( b );
        while( s > -1 ) {
            write( b, 0, s );
            s = r.read( b );
        }
        return this;
    }

    Response exec( Action a )
            throws IOException,
                   HttpException;

    default Response begin( String t )
            throws IOException
    {
        return begin( t, false );
    }

    Response begin( String t, boolean disableMini )
            throws IOException;

    Response end()
            throws IOException;

    Response endAll()
            throws IOException;

    Response attr( String n, CharSequence v )
            throws IOException;

    Response attr( String n, CharSequence seq, int s, int e )
            throws IOException;

    Response attr( String n, char[] v )
            throws IOException;

    Response attr( String n, char[] v, int s, int l )
            throws IOException;

    default Response attr( String n, int v )
            throws IOException
    {
        return attr( n, String.valueOf( v ) );
    }

    default Response attr( String n, long v )
            throws IOException
    {
        return attr( n, String.valueOf( v ) );
    }

    default Response attr( String n, double v )
            throws IOException
    {
        return attr( n, String.valueOf( v ) );
    }

    default Response attr( String n, boolean v )
            throws IOException
    {
        return attr( n, v ? "t" : "f" );
    }

    default Response id( String id )
            throws IOException
    {
        return attr( "id", id );
    }

    default Response _class( String c )
            throws IOException
    {
        return attr( "class", c );
    }

    default Response style( String s )
            throws IOException
    {
        return attr( "style", s );
    }

    default Response a()
            throws IOException
    {
        return begin( "a", true );
    }

    default Response a( String link )
            throws IOException
    {
        return a().attr( "href", link );
    }

    default Response a( String link, String text )
            throws IOException
    {
        return a( link ).write( text ).end();
    }

    default Response br()
            throws IOException
    {
        return begin( "br" ).end();
    }

    default Response div()
            throws IOException
    {
        return begin( "div", true );
    }

    default Response h1()
            throws IOException
    {
        return begin( "h1", true );
    }

    default Response h2()
            throws IOException
    {
        return begin( "h2", true );
    }

    default Response h3()
            throws IOException
    {
        return begin( "h3", true );
    }

    default Response h4()
            throws IOException
    {
        return begin( "h4", true );
    }

    default Response h5()
            throws IOException
    {
        return begin( "h5", true );
    }

    default Response h6()
            throws IOException
    {
        return begin( "h6", true );
    }

    default Response table()
            throws IOException
    {
        return begin( "table", true );
    }

    default Response tr()
            throws IOException
    {
        return begin( "tr", true );
    }

    default Response th()
            throws IOException
    {
        return begin( "th", true );
    }

    default Response td()
            throws IOException
    {
        return begin( "td", true );
    }

    default Response input()
            throws IOException
    {
        return begin( "input", true );
    }

    default Response linkStylesheet( String src )
            throws IOException
    {
        return begin( "link" )
                .attr( "rel", "stylesheet" )
                .attr( "href", src )
                .end();
    }

    default Response p()
            throws IOException
    {
        return begin( "p", true );
    }

    default Response span()
            throws IOException
    {
        return begin( "span", true );
    }

    default Response script()
            throws IOException
    {
        return begin( "script", true );
    }

    default Response script( String src )
            throws IOException
    {
        return script().attr( "src", src ).end();
    }

    HttpEntity getEntity()
            throws IOException;

    Response setContentType( ContentType contentType );

    static Response create( Request request )
    {
        class State
        {

            private final String tag;
            private final boolean disableMini;
            private boolean body;

            public State( String tag, boolean disableMini )
            {
                this.tag = tag;
                this.disableMini = disableMini;
            }

            @Override
            public String toString()
            {
                return tag;
            }

        }

        CharArrayWriter writer = new CharArrayWriter();

        return new Response()
        {
            private ContentType contentType = ContentType.TEXT_HTML;

            private Deque<State> deque = new ArrayDeque<>();
            private State state;

            @Override
            public Response exec( Action a )
                    throws IOException,
                           HttpException
            {
                if( a != null ) {
                    // Ensure we have finished the current tag
                    startBody();

                    // Now preserve the stack & start a new one.
                    // This means one action cannot affect the state of this one
                    final Deque<State> orig = deque;
                    deque = new ArrayDeque<>();
                    try {
                        a.apply( request );
                    }
                    finally {
                        endAll();
                        deque = orig;
                    }
                }
                return this;
            }

            @Override
            public Response setContentType( ContentType contentType )
            {
                this.contentType = contentType;
                return this;
            }

            @Override
            public HttpEntity getEntity()
                    throws IOException
            {
                while( !deque.isEmpty() ) {
                    end();
                }
                return new StringEntity( writer.toString(), contentType );
            }

            private void startBody()
            {
                if( state != null && !state.body ) {
                    state.body = true;
                    writer.append( '>' );
                }
            }

            private void tagOnly()
            {
                if( state == null || state.body ) {
                    throw new IllegalStateException( "Not in tag" );
                }
            }

            @Override
            public Response write( CharSequence seq, int s, int e )
                    throws IOException
            {
                startBody();
                writer.append( seq, s, e );
                return this;
            }

            @Override
            public Response write( char[] v, int s, int l )
                    throws IOException
            {
                startBody();
                writer.write( v, s, l );
                return this;
            }

            @Override
            public Response write( char v )
                    throws IOException
            {
                startBody();
                writer.write( v );
                return this;
            }

            @Override
            public Response begin( String t, boolean disableMini )
                    throws IOException
            {
                startBody();

                if( state != null ) {
                    deque.addLast( state );
                }

                state = new State( t, disableMini );

                writer.append( '<' );
                writer.write( state.tag );
                return this;
            }

            @Override
            public Response end()
                    throws IOException
            {
                if( state == null ) {
                    throw new IllegalStateException( "end() called outside of tag" );
                }

                // elements like script mustn't be minified, i.e. <script/> is invalid must be <script></script>
                if( state.disableMini ) {
                    startBody();
                }

                if( state.body ) {
                    writer.append( '<' );
                    writer.append( '/' );
                    writer.append( state.tag );
                    writer.append( '>' );
                }
                else {
                    writer.append( '/' );
                    writer.append( '>' );
                }

                state = deque.pollLast();

                return this;
            }

            @Override
            public Response endAll()
                    throws IOException
            {
                while( !deque.isEmpty() ) {
                    end();
                }
                return this;
            }

            @Override
            public Response attr( String n, CharSequence seq )
                    throws IOException
            {
                tagOnly();
                writer.append( ' ' );
                writer.append( n );
                writer.append( '=' );
                writer.append( '"' );
                writer.append( seq );
                writer.append( '"' );
                return this;
            }

            @Override
            public Response attr( String n, CharSequence seq, int s, int e )
                    throws IOException
            {
                tagOnly();
                writer.append( ' ' );
                writer.append( n );
                writer.append( '=' );
                writer.append( '"' );
                writer.append( seq, s, e );
                writer.append( '"' );
                return this;
            }

            @Override
            public Response attr( String n, char[] v )
                    throws IOException
            {
                tagOnly();
                writer.append( ' ' );
                writer.append( n );
                writer.append( '=' );
                writer.append( '"' );
                writer.write( v );
                writer.append( '"' );
                return this;
            }

            @Override
            public Response attr( String n, char[] v, int s, int l )
                    throws IOException
            {
                tagOnly();
                writer.append( ' ' );
                writer.append( n );
                writer.append( '=' );
                writer.append( '"' );
                writer.write( v, s, l );
                writer.append( '"' );
                return this;
            }

        };
    }
}
