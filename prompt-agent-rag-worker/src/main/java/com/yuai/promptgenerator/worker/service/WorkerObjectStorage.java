package com.yuai.promptgenerator.worker.service;

import java.io.InputStream;

public interface WorkerObjectStorage extends AutoCloseable {
    InputStream get(String key);
    @Override default void close() { }
}
