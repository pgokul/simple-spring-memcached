package net.nelz.simplesm.providers.spymemcached;

import java.net.SocketAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import net.nelz.simplesm.providers.CachedObject;
import net.nelz.simplesm.providers.CachedObjectImpl;
import net.nelz.simplesm.providers.MemcacheClient;
import net.nelz.simplesm.providers.MemcacheException;
import net.nelz.simplesm.providers.MemcacheTranscoder;
import net.spy.memcached.CachedData;
import net.spy.memcached.MemcachedClientIF;
import net.spy.memcached.OperationTimeoutException;
import net.spy.memcached.transcoders.Transcoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Copyright (c) 2010, 2011 Jakub Białek
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
 * @author Jakub Białek
 * 
 */
public class MemcacheClientWrapper implements MemcacheClient {

    private final static Logger LOGGER = LoggerFactory.getLogger(MemcacheClientWrapper.class);

    private Map<MemcacheTranscoder<?>, Transcoder<?>> adapters = new HashMap<MemcacheTranscoder<?>, Transcoder<?>>();

    private MemcachedClientIF memcachedClient;

    public MemcacheClientWrapper(MemcachedClientIF memcachedClient) {
        this.memcachedClient = memcachedClient;
    }

    @Override
    public boolean add(String key, int exp, Object value) throws TimeoutException, MemcacheException {
        Future<Boolean> f = null;
        try {
            f = memcachedClient.add(key, exp, value);
            return f.get();
        } catch (InterruptedException e) {
            cancel(f);
            throw new MemcacheException(e);
        } catch (ExecutionException e) {
            cancel(f);
            throw new MemcacheException(e);
        }
    }

    @Override
    public <T> boolean add(String key, int exp, T value, MemcacheTranscoder<T> transcoder) throws TimeoutException, MemcacheException {
        Future<Boolean> f = null;
        try {
            f = memcachedClient.add(key, exp, value, getTranscoder(transcoder));
            return f.get();
        } catch (InterruptedException e) {
            cancel(f);
            throw new MemcacheException(e);
        } catch (ExecutionException e) {
            cancel(f);
            throw new MemcacheException(e);
        }
    }

    @Override
    public long decr(String key, int by) throws TimeoutException, MemcacheException {
        try {
            return memcachedClient.decr(key, by);
        } catch (OperationTimeoutException e) {
            LOGGER.warn("Operation timeout while incr " + key, e);
            throw new TimeoutException(e.getMessage());
        } catch (RuntimeException e) {
            if (translateException(e)) {
                throw new MemcacheException(e);
            }
            throw e;
        }
    }

    @Override
    public long decr(String key, int by, long def) throws TimeoutException, MemcacheException {
        try {
            return memcachedClient.decr(key, by, def);
        } catch (OperationTimeoutException e) {
            LOGGER.warn("Operation timeout while incr " + key, e);
            throw new TimeoutException(e.getMessage());
        } catch (RuntimeException e) {
            if (translateException(e)) {
                throw new MemcacheException(e);
            }
            throw e;
        }
    }

    @Override
    public boolean delete(String key) throws TimeoutException, MemcacheException {
        try {
            return memcachedClient.delete(key).get();
        } catch (InterruptedException e) {
            throw new MemcacheException(e);
        } catch (ExecutionException e) {
            throw new MemcacheException(e);
        }
    }

    @Override
    public void flush() throws MemcacheException {
        try {
            memcachedClient.flush().get();
        } catch (InterruptedException e) {
            throw new MemcacheException(e);
        } catch (ExecutionException e) {
            throw new MemcacheException(e);
        }
    }

    @Override
    public Object get(String key) throws TimeoutException, MemcacheException {
        try {
            return memcachedClient.get(key);
        } catch (RuntimeException e) {
            if (translateException(e)) {
                throw new MemcacheException(e);
            } else if (e.getCause() instanceof TimeoutException) {
                throw (TimeoutException) e.getCause();
            }

            throw e;
        }
    }

    @Override
    public <T> T get(String key, MemcacheTranscoder<T> transcoder) throws MemcacheException, TimeoutException {
        try {
            return memcachedClient.get(key, getTranscoder(transcoder));
        } catch (RuntimeException e) {
            if (translateException(e)) {
                throw new MemcacheException(e);
            } else if (e.getCause() instanceof TimeoutException) {
                throw (TimeoutException) e.getCause();
            }

            throw e;
        }
    }

    @Override
    public <T> T get(String key, MemcacheTranscoder<T> transcoder, final long timeout) throws TimeoutException, MemcacheException {
        Future<T> f = null;
        try {
            f = memcachedClient.asyncGet(key, getTranscoder(transcoder));
            return f.get(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            cancel(f);
            throw new MemcacheException(e);
        } catch (ExecutionException e) {
            cancel(f);
            throw new MemcacheException(e);
        }
    }

    @Override
    public Collection<SocketAddress> getAvailableServers() {
        return memcachedClient.getAvailableServers();
    }

    @Override
    public Map<String, Object> getBulk(Collection<String> keys) throws TimeoutException, MemcacheException {
        try {
            return memcachedClient.getBulk(keys);
        } catch (OperationTimeoutException e) {
            throw (TimeoutException) e.getCause();
        } catch (RuntimeException e) {
            if (translateException(e)) {
                throw new MemcacheException(e);
            }
            throw e;
        }
    }

    @Override
    public <T> Map<String, T> getBulk(Collection<String> keys, MemcacheTranscoder<T> transcoder) throws TimeoutException, MemcacheException {
        try {
            return memcachedClient.getBulk(keys, getTranscoder(transcoder));
        } catch (OperationTimeoutException e) {
            throw (TimeoutException) e.getCause();
        } catch (RuntimeException e) {
            if (translateException(e)) {
                throw new MemcacheException(e);
            }
            throw e;
        }
    }

    @Override
    public long incr(String key, int by) throws TimeoutException, MemcacheException {
        try {
            return memcachedClient.incr(key, by);
        } catch (OperationTimeoutException e) {
            LOGGER.warn("Operation timeout while incr " + key, e);
            throw new TimeoutException(e.getMessage());
        } catch (RuntimeException e) {
            if (translateException(e)) {
                throw new MemcacheException(e);
            }
            throw e;
        }
    }

    @Override
    public long incr(String key, int by, long def) throws TimeoutException, MemcacheException {
        try {
            return memcachedClient.incr(key, by, def);
        } catch (OperationTimeoutException e) {
            LOGGER.warn("Operation timeout while incr " + key, e);
            throw new TimeoutException(e.getMessage());
        } catch (RuntimeException e) {
            if (translateException(e)) {
                throw new MemcacheException(e);
            }
            throw e;
        }
    }

    @Override
    public long incr(String key, int by, long def, int expiration) throws TimeoutException, MemcacheException {
        try {
            return memcachedClient.incr(key, by, def, expiration);
        } catch (OperationTimeoutException e) {
            LOGGER.warn("Operation timeout while incr " + key, e);
            throw new TimeoutException(e.getMessage());
        } catch (RuntimeException e) {
            if (translateException(e)) {
                throw new MemcacheException(e);
            }
            throw e;
        }
    }

    @Override
    public boolean set(String key, int exp, Object value) throws TimeoutException, MemcacheException {
        Future<Boolean> f = null;
        try {
            f = memcachedClient.set(key, exp, value);
            return f.get();
        } catch (InterruptedException e) {
            cancel(f);
            throw new MemcacheException(e);
        } catch (ExecutionException e) {
            cancel(f);
            throw new MemcacheException(e);
        }
    }

    @Override
    public <T> boolean set(String key, int exp, T value, MemcacheTranscoder<T> transcoder) throws TimeoutException, MemcacheException {
        Future<Boolean> f = null;
        try {
            f = memcachedClient.set(key, exp, value, getTranscoder(transcoder));
            return f.get();
        } catch (InterruptedException e) {
            cancel(f);
            throw new MemcacheException(e);
        } catch (ExecutionException e) {
            cancel(f);
            throw new MemcacheException(e);
        }
    }

    @Override
    public void shutdown() {
        memcachedClient.shutdown();
    }
    
    @Override
    public MemcacheTranscoder<?> getTranscoder() {
        return new TranscoderWrapper(memcachedClient.getTranscoder());
    }

    private void cancel(Future<?> f) {
        if (f != null) {
            f.cancel(true);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> Transcoder<T> getTranscoder(MemcacheTranscoder<T> transcoder) {
        Transcoder<T> transcoderAdapter = (Transcoder<T>) adapters.get(transcoder);
        if (transcoderAdapter == null) {
            transcoderAdapter = new TranscoderAdapter<T>(transcoder);
            adapters.put(transcoder, transcoderAdapter);
        }

        return transcoderAdapter;
    }

    private boolean translateException(RuntimeException e) {
        return e.getCause() instanceof InterruptedException || e.getCause() instanceof ExecutionException;
    }

    private static class TranscoderWrapper implements MemcacheTranscoder<Object> {

        private Transcoder<Object> transcoder;

        public TranscoderWrapper(Transcoder<Object> transcoder) {
            this.transcoder = transcoder;
        }

        @Override
        public Object decode(CachedObject data) {
            return transcoder.decode(new CachedData(data.getFlags(), data.getData(), CachedObject.MAX_SIZE));
        }

        @Override
        public CachedObject encode(Object o) {
            CachedData cachedData = transcoder.encode(o);
            return new CachedObjectImpl(cachedData.getFlags(), cachedData.getData());
        }
    }

}
