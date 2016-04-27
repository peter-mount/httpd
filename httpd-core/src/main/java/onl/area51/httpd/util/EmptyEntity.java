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
package onl.area51.httpd.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.http.entity.AbstractHttpEntity;

/**
 *
 * @author peter
 */
public class EmptyEntity
        extends AbstractHttpEntity
{

    public static final EmptyEntity INSTANCE = new EmptyEntity();

    private EmptyEntity()
    {
    }

    @Override
    public boolean isRepeatable()
    {
        return true;
    }

    @Override
    public long getContentLength()
    {
        return 0L;
    }

    @Override
    public InputStream getContent()
            throws IOException,
                   UnsupportedOperationException
    {
        return new InputStream()
        {
            @Override
            public int read()
                    throws IOException
            {
                return -1;
            }
        };
    }

    @Override
    public void writeTo( OutputStream outstream )
            throws IOException
    {

    }

    @Override
    public boolean isStreaming()
    {
        return false;
    }

}
