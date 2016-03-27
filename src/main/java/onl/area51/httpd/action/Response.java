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

    Response exec( Action a )
            throws IOException,
                   HttpException;

    Response begin( String t )
            throws IOException;

    Response end()
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

    HttpEntity getEntity()
            throws IOException;

    Response setContentType( ContentType contentType );

    static Response create( Request request )
    {
        class State
        {

            private final String tag;
            private boolean body;

            public State( String tag )
            {
                this.tag = tag;
            }
        }

        CharArrayWriter writer = new CharArrayWriter();

        return new Response()
        {
            private ContentType contentType = ContentType.TEXT_HTML;

            private final Deque<State> deque = new ArrayDeque<>();
            private State state;

            @Override
            public Response exec( Action a )
                    throws IOException,
                           HttpException
            {
                if( a != null ) {
                    startBody();
                    a.apply( request );
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
            public Response begin( String t )
                    throws IOException
            {
                if( state != null ) {
                    deque.offerLast( state );
                }
                startBody();
                state = new State( t );
                writer.append( '<' );
                writer.write( state.tag );
                return this;
            }

            @Override
            public Response end()
                    throws IOException
            {
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
