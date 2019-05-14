# A Socket Factory for Tunneling Communication Through a Proxy

This repository provides source code for a Java socket factory that can be
used to tunnel communication through a java.net.Proxy object, which may use an
HTTP or SOCKS proxy.  It was originally written for use in conjunction with the
[(]UnboundID LDAP SDK for Java](https://github.com/pingidentity/ldapsdk), but it
should be usable in any context that takes a javax.net.SocketFactory instance.

Note that because of limitations in the Java proxy support, the following
constraints will be imposed for communication through the proxy:

* Communication with the proxy server itself cannot be encrypted.  However, it
  is possible to encrypt all communication through the proxy server using TLS
  (by providing an `SSLSocketFactory` instance when creating the
  `ProxiedCommunicationSocketFactory`), in which case the data will still be
  protected from the client to the end server, and anyone observing the
  communication between the client and the proxy would not be able to decipher
  that communication.

* This class only provides direct support for proxy servers that do not require
  authentication.  Although it may be possible to configure authentication
  using system properties, this implementation does not provide direct support
  for authentication.

The code in this repository is provided under the terms of each of the
following licenses:

* [The GNU General Public License version 2 (GPLv2)](LICENSE-GPLv2.txt)
* [The GNU Lesser General Public License version 2.1 (LGPLv2.1)](LICENSE-LGPLv2.1.txt)
* [The Apache License version 2.0](LICENSE-Apache-v2.0.txt)
