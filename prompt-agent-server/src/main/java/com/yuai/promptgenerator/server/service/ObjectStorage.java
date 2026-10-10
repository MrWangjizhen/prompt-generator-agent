package com.yuai.promptgenerator.server.service;

import java.io.InputStream;

public interface ObjectStorage extends AutoCloseable {
    void put(String key, InputStream data, long length, String contentType);
    InputStream get(String key);
    void delete(String key);
    boolean available();
    @Override
    default void close() { }
}
