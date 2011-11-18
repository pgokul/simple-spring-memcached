package net.nelz.simplesm.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.naming.NamingException;

import net.nelz.simplesm.providers.MemcacheClient;
import net.nelz.simplesm.providers.MemcacheClientFactory;

import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.Test;

/**
 * Copyright (c) 2008, 2009 Nelson Carpentier
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * 
 * @author Nelson Carpentier
 * 
 */
public class MemcachedClientFactoryTest {

    @Test
    public void testCreateClientException() throws IOException, NamingException {
        final MemcachedClientFactory factory = new MemcachedClientFactory();
        try {
            factory.createMemcachedClient();
            fail("Expected Exception.");
        } catch (RuntimeException ex) {
            assertTrue(true);
        }
    }

    @Test
    public void testCreateClient() throws IOException, NamingException {
        MemcachedConnectionBean bean = new MemcachedConnectionBean();
        bean.setConsistentHashing(false);
        bean.setNodeList("127.0.0.1:11211");
        MemcachedClientFactory factory = new MemcachedClientFactory();
        factory.setBean(bean);
        MemcacheClientFactory clientFactory = getClientFactoryMock(bean);
        factory.setClientFactory(clientFactory);

        MemcacheClient cache = factory.createMemcachedClient();

        assertNotNull(cache);
        EasyMock.verify(clientFactory);

        factory = new MemcachedClientFactory();
        factory.setBean(bean);
        bean.setConsistentHashing(true);
        clientFactory = getClientFactoryMock(bean);

        factory.setClientFactory(clientFactory);

        cache = factory.createMemcachedClient();
        assertNotNull(cache);
        EasyMock.verify(clientFactory);

        bean = new MemcachedConnectionBean();
        bean.setConsistentHashing(false);
        bean.setNodeList("127.0.0.1:11211");
        String jndiKey = "memcached/ips";
        bean.setJndiKey(jndiKey);
        factory = new MemcachedClientFactory();
        factory.setBean(bean);
        clientFactory = getClientFactoryMock(bean);
        factory.setClientFactory(clientFactory);

        cache = factory.createMemcachedClient();

        assertNotNull(cache);
        EasyMock.verify(clientFactory);
        try {
            clientFactory = getClientFactoryMock(bean);
            factory.setClientFactory(clientFactory);
            factory.createMemcachedClient();
            fail();
        } catch (IllegalStateException ex) {
            // ok
        }

    }

    public void handleNotification() throws IOException, NamingException {
        final MemcachedConnectionBean bean = new MemcachedConnectionBean();
        bean.setConsistentHashing(false);
        bean.setNodeList("127.0.0.1:11211");
        String jndiKey = "memcached/ips";
        bean.setJndiKey(jndiKey);
        final MemcachedClientFactory factory = new MemcachedClientFactory();
        factory.setBean(bean);
        String newValue = "128.0.0.1:11211";
        MemcacheClientFactory clientFactory = getClientFactoryMock(bean);
        factory.setClientFactory(clientFactory);

        MemcacheClient cache = factory.createMemcachedClient();

        factory.handleNotification(jndiKey, newValue);
        Collection<SocketAddress> c = cache.getAvailableServers();

        assertEquals(1, c.size());
        assertEquals(newValue, ((InetSocketAddress) c.iterator().next()).getHostName());

        factory.handleNotification(jndiKey, newValue);
        c = cache.getAvailableServers();
        assertEquals(1, c.size());
        assertEquals(newValue, ((InetSocketAddress) c.iterator().next()).getHostName());

        factory.handleNotification(jndiKey, null);
        c = cache.getAvailableServers();
        assertEquals(1, c.size());
        assertEquals(newValue, ((InetSocketAddress) c.iterator().next()).getHostName());

        factory.handleNotification(jndiKey + "xxxyyy", newValue);
        c = cache.getAvailableServers();
        assertEquals(1, c.size());
        assertEquals(newValue, ((InetSocketAddress) c.iterator().next()).getHostName());
    }

    @SuppressWarnings("unchecked")
    private MemcacheClientFactory getClientFactoryMock(MemcachedConnectionBean bean) throws IOException {
        MemcacheClientFactory clientFactory = EasyMock.createMock(MemcacheClientFactory.class);

        EasyMock.expect(clientFactory.create(EasyMock.anyObject(List.class), EasyMock.eq(bean))).andAnswer(new IAnswer<MemcacheClient>() {

            @Override
            public MemcacheClient answer() throws Throwable {
                List<InetSocketAddress> address = (List<InetSocketAddress>) EasyMock.getCurrentArguments()[0];
                MemcacheClient client = EasyMock.createMock(MemcacheClient.class);

                List<SocketAddress> socketAddress = new ArrayList<SocketAddress>();
                socketAddress.addAll(address);
                EasyMock.expect(client.getAvailableServers()).andReturn(socketAddress);
                EasyMock.replay(client);

                return client;
            }
        });
        EasyMock.replay(clientFactory);

        return clientFactory;
    }
}