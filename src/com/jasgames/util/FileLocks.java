package com.jasgames.util;

import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public final class FileLocks {
    private static final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();
    private FileLocks() {}

    public static ReentrantLock of(Path path) {
        String key = path.toAbsolutePath().normalize().toString();
        return locks.computeIfAbsent(key, k -> new ReentrantLock());
    }
}
