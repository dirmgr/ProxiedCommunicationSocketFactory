/*
 * Copyright 2019 Neil A. Wilson
 * All Rights Reserved.
 */
/*
 * Copyright (C) 2019 Neil A. Wilson
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPLv2 only)
 * or the terms of the GNU Lesser General Public License (LGPLv2.1 only)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses>.
 */
/*
 * Copyright 2019 Neil A. Wilson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dirmgr.proxiedsocketfactory;



import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;



/**
 * This class provides a socket factory implementation that can tunnel traffic
 * through a {@code java.net.Proxy} instance, which may be an HTTP or SOCKS
 * proxy.  Because of limitations in the Java proxy support, the following
 * constraints will be imposed:
 * <UL>
 *   <LI>
 *     Communication with the proxy server itself cannot be encrypted.  However,
 *     it is possible to encrypt all communication through the proxy server
 *     using TLS (by providing an {@code SSLSocketFactory} instance when
 *     creating the {@code ProxiedCommunicationSocketFactory}), in which case
 *     the data will still be protected from the client to the end server, and
 *     anyone observing the communication between the client and the proxy would
 *     not be able to decipher that communication.
 *   </LI>
 *   <LI>
 *     This class only provides direct support for proxy servers that do not
 *     require authentication.  Although it may be possible to configure
 *     authentication using system properties, this implementation does not
 *     provide direct support for authentication.
 *   </LI>
 * </UL>
 */
public final class ProxiedCommunicationSocketFactory
       extends SocketFactory
{
  // The maximum length of time in milliseconds to wait for a connection to be
  // established.
  private final int connectTimeoutMillis;

  // The Proxy instance that will be used to communicate with the proxy server.
  private final Proxy proxy;

  // An optional SSLSocketFactory instance that can be used to secure
  // communication through the proxy server.
  private final SSLSocketFactory sslSocketFactory;



  /**
   * Creates a new instance of this socket factory with the provided settings.
   *
   * @param  proxyHost             The address of the proxy server to use.  It
   *                               must not be {@code null}.
   * @param  proxyPort             The port of the proxy server to use.  It must
   *                               not be {@code null}.
   * @param  proxyType             The type of proxy to use.  It must not be
   *                               {@code null}.
   * @param  connectTimeoutMillis  The maximum length of time in milliseconds
   *                               to wait for a connection to be established.
   *                               A value that is less than or equal to zero
   *                               indicates that no connect timeout will be
   *                               imposed.
   * @param  sslSocketFactory      An optional {@code SSLSocketFactory} instance
   *                               to use to secure communication with the end
   *                               servers passing through the proxy.  This may
   *                               be {@code null} if communication with the end
   *                               servers should not be encrypted.
   */
  public ProxiedCommunicationSocketFactory(final String proxyHost,
              final int proxyPort, final Proxy.Type proxyType,
              final int connectTimeoutMillis,
              final SSLSocketFactory sslSocketFactory)
  {
    this(new Proxy(proxyType, new InetSocketAddress(proxyHost, proxyPort)),
         connectTimeoutMillis, sslSocketFactory);
  }



  /**
   * Creates a new instance of this socket factory with the provided settings.
   *
   * @param  proxy                 The proxy instance to use to communicate with
   *                               the proxy server.  It must not be
   *                               {@code null}.
   * @param  connectTimeoutMillis  The maximum length of time in milliseconds
   *                               to wait for a connection to be established.
   *                               A value that is less than or equal to zero
   *                               indicates that no connect timeout will be
   *                               imposed.
   * @param  sslSocketFactory      An optional {@code SSLSocketFactory} instance
   *                               to use to secure communication with the end
   *                               servers passing through the proxy.  This may
   *                               be {@code null} if communication with the end
   *                               servers should not be encrypted.
   */
  public ProxiedCommunicationSocketFactory(final Proxy proxy,
              final int connectTimeoutMillis,
              final SSLSocketFactory sslSocketFactory)
  {
    this.proxy = proxy;
    this.sslSocketFactory = sslSocketFactory;

    if (connectTimeoutMillis > 0)
    {
      this.connectTimeoutMillis = connectTimeoutMillis;
    }
    else
    {
      this.connectTimeoutMillis = 0;
    }
  }



  /**
   * Creates an unconnected socket that will use the proxy server for
   * communication.  Note that this method can only be used when communication
   * through the proxy server will not be encrypted.
   *
   * @throws  UnsupportedOperationException  If an {@code SSLSocketFactory}
   *                                         has been configured to secure
   *                                         communication with end servers.
   */
  @Override()
  public Socket createSocket()
         throws UnsupportedOperationException
  {
    if (sslSocketFactory == null)
    {
      return new Socket(proxy);
    }
    else
    {
      throw new UnsupportedOperationException("Unable to create an " +
           "unconnected socket for communication through a proxy when an " +
           "SSLSocketFactory has been configured.");
    }
  }



  /**
   * Creates a new socket that is connected to the specified system through the
   * proxy server.
   *
   * @param  host  The address of the server to which the socket should be
   *               established.
   * @param  port  The port of the server to which the socket should be
   *               established.
   *
   * @throws  IOException  If a problem is encountered while attempting to
   *                       establish the connection.
   */
  @Override()
  public Socket createSocket(final String host, final int port)
         throws IOException
  {
    final Socket socket = new Socket(proxy);
    socket.connect(new InetSocketAddress(host, port), connectTimeoutMillis);
    return secureSocket(socket, host, port);
  }



  /**
   * Creates a new socket that is connected to the specified system through the
   * proxy server.
   *
   * @param  host       The address of the server to which the socket should be
   *                    established.
   * @param  port       The port of the server to which the socket should be
   *                    established.
   * @param  localHost  The local address to which the socket should be bound.
   * @param  localPort  The local port to which the socket should be bound.
   *
   * @throws  IOException  If a problem is encountered while attempting to
   *                       establish the connection.
   */
  @Override()
  public Socket createSocket(final String host, final int port,
                             final InetAddress localHost, final int localPort)
         throws IOException
  {
    final Socket socket = new Socket(proxy);
    socket.bind(new InetSocketAddress(localHost, localPort));
    socket.connect(new InetSocketAddress(host, port), connectTimeoutMillis);
    return secureSocket(socket, host, port);
  }



  /**
   * Creates a new socket that is connected to the specified system through the
   * proxy server.
   *
   * @param  host  The address of the server to which the socket should be
   *               established.
   * @param  port  The port of the server to which the socket should be
   *               established.
   *
   * @throws  IOException  If a problem is encountered while attempting to
   *                       establish the connection.
   */
  @Override()
  public Socket createSocket(final InetAddress host, final int port)
         throws IOException
  {
    final Socket socket = new Socket(proxy);
    socket.connect(new InetSocketAddress(host, port), connectTimeoutMillis);
    return secureSocket(socket, host.getHostName(), port);
  }



  /**
   * Creates a new socket that is connected to the specified system through the
   * proxy server.
   *
   * @param  host       The address of the server to which the socket should be
   *                    established.
   * @param  port       The port of the server to which the socket should be
   *                    established.
   * @param  localHost  The local address to which the socket should be bound.
   * @param  localPort  The local port to which the socket should be bound.
   *
   * @throws  IOException  If a problem is encountered while attempting to
   *                       establish the connection.
   */
  @Override()
  public Socket createSocket(final InetAddress host, final int port,
                             final InetAddress localHost, final int localPort)
         throws IOException
  {
    final Socket socket = new Socket(proxy);
    socket.bind(new InetSocketAddress(localHost, localPort));
    socket.connect(new InetSocketAddress(host, port), connectTimeoutMillis);
    return secureSocket(socket, host.getHostName(), port);
  }



  /**
   * Adds TLS security to the provided socket, if appropriate.
   *
   * @param  socket  The socket to be optionally secured.
   * @param  host    The address of the server to which the socket is
   *                 established.
   * @param  port    The port of the server to which the socket is established.
   *
   * @return  An {@code SSLSocket} that wraps the provided socket if the
   *          communication should be secured, or the provided socket if no
   *          additional security is needed.
   *
   * @throws  IOException  If a problem is encountered while attempting to
   *                       secure communication with the target server.  If an
   *                       exception is thrown, then the socket will have been
   *                       closed
   */
  private Socket secureSocket(final Socket socket, final String host,
                              final int port)
          throws IOException
  {
    if (sslSocketFactory == null)
    {
      return socket;
    }

    try
    {
      return sslSocketFactory.createSocket(socket, host, port, true);
    }
    catch (final IOException e)
    {
      try
      {
        socket.close();
      }
      catch (final Exception e2)
      {
        // Ignore this.
      }

      throw e;
    }
  }
}
