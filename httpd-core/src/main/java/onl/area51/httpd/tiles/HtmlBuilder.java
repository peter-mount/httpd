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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.function.Supplier;
import onl.area51.httpd.action.Action;

/**
 *
 * @author peter
 * @param <T>
 */
public interface HtmlBuilder<T>
{

    default HtmlBuilder<T> id( CharSequence v )
            throws IOException
    {
        return attr( "id", v );
    }

    HtmlBuilder<T> attr( String n, CharSequence v )
            throws IOException;

    HtmlBuilder<T> attr( String n, CharSequence seq, int s, int e )
            throws IOException;

    HtmlBuilder<T> attr( String n, char[] v )
            throws IOException;

    HtmlBuilder<T> attr( String n, char[] v, int s, int l )
            throws IOException;

    default HtmlBuilder<T> attr( String n, int v )
            throws IOException
    {
        return attr( n, String.valueOf( v ) );
    }

    default HtmlBuilder<T> attr( String n, long v )
            throws IOException
    {
        return attr( n, String.valueOf( v ) );
    }

    default HtmlBuilder<T> attr( String n, double v )
            throws IOException
    {
        return attr( n, String.valueOf( v ) );
    }

    HtmlBuilder<T> write( CharSequence seq )
            throws IOException;

    HtmlBuilder<T> write( CharSequence seq, int s, int e )
            throws IOException;

    HtmlBuilder<T> write( char v )
            throws IOException;

    default HtmlBuilder<T> write( char[] v )
            throws IOException
    {
        return write( v, 0, v.length );
    }

    HtmlBuilder<T> write( char[] v, int s, int l )
            throws IOException;

    default HtmlBuilder<T> write( int v )
            throws IOException
    {
        return write( String.valueOf( v ) );
    }

    default HtmlBuilder<T> write( long v )
            throws IOException
    {
        return write( String.valueOf( v ) );
    }

    default HtmlBuilder<T> write( double v )
            throws IOException
    {
        return write( String.valueOf( v ) );
    }

    default HtmlBuilder<T> printf( String fmt, Object... args )
            throws IOException
    {
        return write( String.format( fmt, args ) );
    }

    default HtmlBuilder<T> println()
            throws IOException
    {
        return write( '\n' );
    }

    default HtmlBuilder<T> println( String fmt )
            throws IOException
    {
        return write( fmt ).println();
    }

    default HtmlBuilder<T> println( String fmt, Object... args )
            throws IOException
    {
        return write( String.format( fmt, args ) );
    }

    default HtmlBuilder<HtmlBuilder<T>> a()
            throws IOException
    {
        return tag( "a" );
    }

    default HtmlBuilder<HtmlBuilder<T>> div()
            throws IOException
    {
        return tag( "div" );
    }

    default HtmlBuilder<HtmlBuilder<T>> input()
            throws IOException
    {
        return tag( "input" );
    }

    default HtmlBuilder<HtmlBuilder<T>> label()
            throws IOException
    {
        return tag( "label" );
    }

    default HtmlBuilder<HtmlBuilder<T>> p()
            throws IOException
    {
        return tag( "p" );
    }

    default HtmlBuilder<HtmlBuilder<T>> script()
            throws IOException
    {
        return tag( "script" );
    }

    HtmlBuilder<T> addClass( String c );

    HtmlBuilder<T> addStyle( String s );

    default HtmlBuilder<HtmlBuilder<T>> tag( String t )
            throws IOException
    {
        return tag( t, false );
    }

    HtmlBuilder<HtmlBuilder<T>> tag( String t, boolean disableMini )
            throws IOException;

    T build()
            throws IOException;

    static HtmlBuilder<String> builder()
    {
        StringWriter w = new StringWriter();
        PrintWriter pw = new PrintWriter( w );
        return builder( pw, () -> w.toString() );
    }

    static HtmlBuilder<Action> actionBuilder()
    {
        StringWriter w = new StringWriter();
        return builder( w, () -> {
                    String s = w.toString();
                    return r -> r.getResponse().write( s );
                } );
    }

    static <T> HtmlBuilder<T> builder( Writer writer, Supplier<T> builder )
    {
        return new Builder<>( builder, writer, null );
    }

    static class Builder<T>
            implements HtmlBuilder<T>
    {

        private final T parent;
        private final Supplier<T> supplier;
        private final Writer writer;
        private final String tag;
        private final boolean disableMini;
        private boolean body;
        private String clazz, style;

        public Builder( T parent, Supplier<T> supplier, Writer writer, String tag )
        {
            this.parent = parent;
            this.supplier = supplier;
            this.writer = writer;
            this.tag = tag;
            this.disableMini = false;
        }

        public Builder( T parent, Writer writer, String tag, boolean disableMini )
        {
            this.parent = parent;
            this.supplier = null;
            this.writer = writer;
            this.tag = tag;
            this.disableMini = disableMini;
        }

        public Builder( Supplier<T> supplier, Writer writer, String tag )
        {
            this.parent = null;
            this.supplier = supplier;
            this.writer = writer;
            this.tag = tag;
            this.disableMini = false;
        }

        @Override
        public T build()
                throws IOException
        {
            if( supplier != null ) {
                return supplier.get();
            }
            else if( tag == null ) {
                return parent;
            }

            if( disableMini ) {
                startBody();
            }
            if( body ) {
                writer.append( '<' );
                writer.append( '/' );
                writer.append( tag );
                writer.append( '>' );
            }
            else {
                writer.append( '/' );
                writer.append( '>' );
            }

            return parent;
        }

        private void startBody()
                throws IOException
        {
            if( !body && tag != null ) {
                body = true;
                if( clazz != null ) {
                    attr( "class", clazz );
                }
                if( style != null ) {
                    attr( "style", style );
                }
                writer.append( '>' );
            }
        }

        private void tagOnly()
        {
            if( body ) {
                throw new IllegalStateException( "Not in tag" );
            }
        }

        @Override
        public HtmlBuilder<HtmlBuilder<T>> tag( String t, boolean disableMini )
                throws IOException
        {
            startBody();
            writer.append( '<' );
            writer.append( t );
            return new Builder<>( this, writer, t, disableMini );
        }

        @Override
        public HtmlBuilder<T> attr( String n, CharSequence seq )
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
        public HtmlBuilder<T> attr( String n, CharSequence seq, int s, int e )
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
        public HtmlBuilder<T> attr( String n, char[] v )
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
        public HtmlBuilder<T> attr( String n, char[] v, int s, int l )
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

        @Override
        public HtmlBuilder<T> addClass( String c )
        {
            tagOnly();
            if( clazz == null ) {
                clazz = clazz + " " + c;
            }
            else {
                clazz = c;
            }
            return this;
        }

        @Override
        public HtmlBuilder<T> addStyle( String s )
        {
            tagOnly();
            if( style == null ) {
                style = style + " " + s;
            }
            else {
                style = s;
            }
            return this;
        }

        @Override
        public HtmlBuilder<T> write( CharSequence seq )
                throws IOException
        {
            startBody();
            writer.append( seq );
            return this;
        }

        @Override
        public HtmlBuilder<T> write( CharSequence seq, int s, int e )
                throws IOException
        {
            startBody();
            writer.append( seq, s, e );
            return this;
        }

        @Override
        public HtmlBuilder<T> write( char[] v, int s, int l )
                throws IOException
        {
            startBody();
            writer.write( v, s, l );
            return this;
        }

        @Override
        public HtmlBuilder<T> write( char v )
                throws IOException
        {
            startBody();
            writer.write( v );
            return this;
        }

    }
}
