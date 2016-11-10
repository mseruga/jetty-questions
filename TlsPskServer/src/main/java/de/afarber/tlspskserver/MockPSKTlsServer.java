/*
    The file has been copied from https://github.com/bcgit/bc-java

    Copyright (c) 2000-2016 The Legion of the Bouncy Castle Inc. (http://www.bouncycastle.org)

    Permission is hereby granted, free of charge, to any person obtaining a copy of this software 
    and associated documentation files (the "Software"), to deal in the Software without restriction, 
    including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, 
    and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
    subject to the following conditions:

    The above copyright notice and this permission notice shall be included in all copies or substantial
    portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
    INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
    PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
    LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
    OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
    DEALINGS IN THE SOFTWARE.
*/


package de.afarber.tlspskserver;

import java.io.IOException;
import java.io.PrintStream;

import org.bouncycastle.crypto.tls.AlertDescription;
import org.bouncycastle.crypto.tls.AlertLevel;
import org.bouncycastle.crypto.tls.CipherSuite;
import org.bouncycastle.crypto.tls.PSKTlsServer;
import org.bouncycastle.crypto.tls.ProtocolVersion;
import org.bouncycastle.crypto.tls.TlsEncryptionCredentials;
import org.bouncycastle.crypto.tls.TlsPSKIdentityManager;
import org.bouncycastle.util.Strings;

class MockPSKTlsServer
    extends PSKTlsServer
{
    MockPSKTlsServer()
    {
        super(new MyIdentityManager());
    }

    @Override
    public void notifyAlertRaised(short alertLevel, short alertDescription, String message, Throwable cause)
    {
        PrintStream out = (alertLevel == AlertLevel.fatal) ? System.err : System.out;
        out.println("TLS-PSK server raised alert: " + AlertLevel.getText(alertLevel) + ", "
            + AlertDescription.getText(alertDescription));
        if (message != null)
        {
            out.println("> " + message);
        }
        if (cause != null)
        {
            cause.printStackTrace(out);
        }
    }

    @Override
    public void notifyAlertReceived(short alertLevel, short alertDescription)
    {
        PrintStream out = (alertLevel == AlertLevel.fatal) ? System.err : System.out;
        out.println("TLS-PSK server received alert: " + AlertLevel.getText(alertLevel) + ", "
            + AlertDescription.getText(alertDescription));
    }

    @Override
    public void notifyHandshakeComplete() throws IOException
    {
        super.notifyHandshakeComplete();

        byte[] pskIdentity = context.getSecurityParameters().getPSKIdentity();
        if (pskIdentity != null)
        {
            String name = Strings.fromUTF8ByteArray(pskIdentity);
            System.out.println("TLS-PSK server completed handshake for PSK identity: " + name);
        }
    }

    @Override
    protected int[] getCipherSuites()
    {
        return new int[]{ 
            CipherSuite.TLS_ECDHE_PSK_WITH_AES_256_CBC_SHA384,
            CipherSuite.TLS_DHE_PSK_WITH_AES_256_CBC_SHA384, 
            CipherSuite.TLS_RSA_PSK_WITH_AES_256_CBC_SHA384,
            CipherSuite.TLS_PSK_WITH_AES_256_CBC_SHA 
        };
    }

    @Override
    protected ProtocolVersion getMaximumVersion()
    {
        return ProtocolVersion.TLSv12;
    }

    @Override
    protected ProtocolVersion getMinimumVersion()
    {
        return ProtocolVersion.TLSv12;
    }

    @Override
    public ProtocolVersion getServerVersion() throws IOException
    {
        ProtocolVersion serverVersion = super.getServerVersion();

        System.out.println("TLS-PSK server negotiated " + serverVersion);

        return serverVersion;
    }

    @Override
    protected TlsEncryptionCredentials getRSAEncryptionCredentials() throws IOException
    {
        return TlsTestUtils.loadEncryptionCredentials(context, new String[]{ "x509-server.pem", "x509-ca.pem" }, "x509-server-key.pem");
    }

    static class MyIdentityManager
        implements TlsPSKIdentityManager
    {
        @Override
        public byte[] getHint()
        {
            return Strings.toUTF8ByteArray("hint");
        }

        @Override
        public byte[] getPSK(byte[] identity)
        {
            if (identity != null)
            {
                String name = Strings.fromUTF8ByteArray(identity);
                if (name.equals("client"))
                {
                    return new byte[16];
                }
            }
            return null;
        }
    }
}
