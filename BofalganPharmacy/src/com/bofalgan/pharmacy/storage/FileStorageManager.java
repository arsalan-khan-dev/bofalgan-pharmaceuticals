package com.bofalgan.pharmacy.storage;

import com.bofalgan.pharmacy.config.AppConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Secondary JSON-based file storage.
 * Every important MySQL operation mirrors data here.
 * Acts as a recovery backup and offline read cache.
 */
public class FileStorageManager {

    private static FileStorageManager instance;
    private final Gson gson;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ScheduledExecutorService scheduler;
    private volatile long lastSyncTime;

    // In-memory caches per entity
    private final Map<String, List<Map<String, Object>>> cache = new ConcurrentHashMap<>();

    private FileStorageManager() {
        gson = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .registerTypeAdapter(LocalDate.class,
                (com.google.gson.JsonSerializer<LocalDate>) (src, t, ctx) ->
                    ctx.serialize(src.toString()))
            .registerTypeAdapter(LocalDate.class,
                (com.google.gson.JsonDeserializer<LocalDate>) (json, t, ctx) ->
                    LocalDate.parse(json.getAsString()))
            .registerTypeAdapter(LocalDateTime.class,
                (com.google.gson.JsonSerializer<LocalDateTime>) (src, t, ctx) ->
                    ctx.serialize(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
            .registerTypeAdapter(LocalDateTime.class,
                (com.google.gson.JsonDeserializer<LocalDateTime>) (json, t, ctx) ->
                    LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            .create();

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "FileSync-Thread");
            t.setDaemon(true);
            return t;
        });
    }

    public static synchronized FileStorageManager getInstance() {
        if (instance == null) {
            instance = new FileStorageManager();
        }
        return instance;
    }

    // ==================== INIT ====================

    public void initialize() {
        try {
            Path dataDir = Paths.get(AppConfig.DATA_DIR);
            if (!Files.exists(dataDir)) {
                Files.createDirectories(dataDir);
            }
            // Initialize empty files if missing
            String[] files = {
                AppConfig.MEDICINES_FILE, AppConfig.SUPPLIERS_FILE,
                AppConfig.USERS_FILE, AppConfig.INVOICES_FILE,
                AppConfig.PURCHASES_FILE, AppConfig.ALERTS_FILE,
                AppConfig.SETTINGS_FILE
            };
            for (String f : files) {
                Path p = Paths.get(f);
                if (!Files.exists(p)) {
                    writeJsonFile(f, new ArrayList<>());
                }
            }
            // Start periodic sync
            scheduler.scheduleAtFixedRate(this::periodicSync,
                AppConfig.SYNC_INTERVAL_MS, AppConfig.SYNC_INTERVAL_MS,
                TimeUnit.MILLISECONDS);

            System.out.println("[FileStorage] Initialized at " + dataDir.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("[FileStorage] Init warning: " + e.getMessage());
        }
    }

    // ==================== CORE READ / WRITE ====================

    /** Save an entity list to a JSON file. */
    public <T> void saveAll(String filePath, List<T> items) {
        lock.writeLock().lock();
        try {
            writeJsonFile(filePath, items);
            // Update cache
            Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
            String json = gson.toJson(items);
            List<Map<String, Object>> maps = gson.fromJson(json, listType);
            cache.put(filePath, maps);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** Load all records from a JSON file as typed list. */
    public <T> List<T> loadAll(String filePath, Class<T> clazz) {
        lock.readLock().lock();
        try {
            String json = readJsonFile(filePath);
            if (json == null || json.isBlank()) return new ArrayList<>();
            Type listType = TypeToken.getParameterized(List.class, clazz).getType();
            List<T> result = gson.fromJson(json, listType);
            return result != null ? result : new ArrayList<>();
        } finally {
            lock.readLock().unlock();
        }
    }

    /** Append a single record to a JSON file. */
    public <T> void appendRecord(String filePath, T record, Class<T> clazz) {
        lock.writeLock().lock();
        try {
            List<T> existing = loadAllNoLock(filePath, clazz);
            existing.add(record);
            writeJsonFile(filePath, existing);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** Update a record by its 'id' field. */
    public <T> void upsertRecord(String filePath, T record, int id) {
        lock.writeLock().lock();
        try {
            String newJson = gson.toJson(record);
            Map<String, Object> newMap = gson.fromJson(newJson,
                new TypeToken<Map<String, Object>>(){}.getType());
            newMap.put("id", (double) id);

            Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
            String raw = readJsonFile(filePath);
            List<Map<String, Object>> list;
            if (raw == null || raw.isBlank()) {
                list = new ArrayList<>();
            } else {
                list = gson.fromJson(raw, listType);
                if (list == null) list = new ArrayList<>();
            }

            boolean found = false;
            for (int i = 0; i < list.size(); i++) {
                Object existId = list.get(i).get("id");
                if (existId != null && ((Number) existId).intValue() == id) {
                    list.set(i, newMap);
                    found = true;
                    break;
                }
            }
            if (!found) list.add(newMap);
            writeJsonFile(filePath, list);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** Soft-delete: set is_deleted=true for a record by id. */
    public void softDelete(String filePath, int id) {
        lock.writeLock().lock();
        try {
            Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
            String raw = readJsonFile(filePath);
            if (raw == null || raw.isBlank()) return;
            List<Map<String, Object>> list = gson.fromJson(raw, listType);
            if (list == null) return;
            for (Map<String, Object> item : list) {
                Object existId = item.get("id");
                if (existId != null && ((Number) existId).intValue() == id) {
                    item.put("isDeleted", true);
                    break;
                }
            }
            writeJsonFile(filePath, list);
        } finally {
            lock.writeLock().unlock();
        }
    }

    // ==================== SYNC LOG ====================

    public void logSync(String entity, int recordCount, boolean success, String message) {
        try {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("timestamp",   LocalDateTime.now().toString());
            entry.put("entity",      entity);
            entry.put("recordCount", recordCount);
            entry.put("success",     success);
            entry.put("message",     message);

            String raw = readJsonFile(AppConfig.SYNC_LOG_FILE);
            List<Map<String, Object>> log;
            if (raw == null || raw.isBlank()) {
                log = new ArrayList<>();
            } else {
                Type t = new TypeToken<List<Map<String, Object>>>(){}.getType();
                log = gson.fromJson(raw, t);
                if (log == null) log = new ArrayList<>();
            }
            log.add(entry);
            // Keep last 500 entries only
            if (log.size() > 500) {
                log = log.subList(log.size() - 500, log.size());
            }
            writeJsonFile(AppConfig.SYNC_LOG_FILE, log);
        } catch (Exception e) {
            System.err.println("[FileStorage] Sync log error: " + e.getMessage());
        }
    }

    private void periodicSync() {
        lastSyncTime = System.currentTimeMillis();
        // Periodic sync is triggered externally by services after each write.
        // This just updates the timestamp.
    }

    public long getLastSyncTime() { return lastSyncTime; }

    public void shutdown() {
        scheduler.shutdown();
    }

    // ==================== PRIVATE HELPERS ====================

    private void writeJsonFile(String path, Object data) {
        try (Writer w = new OutputStreamWriter(new FileOutputStream(path), StandardCharsets.UTF_8)) {
            gson.toJson(data, w);
        } catch (IOException e) {
            System.err.println("[FileStorage] Write error for " + path + ": " + e.getMessage());
        }
    }

    private String readJsonFile(String path) {
        try {
            Path p = Paths.get(path);
            if (!Files.exists(p)) return null;
            return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("[FileStorage] Read error for " + path + ": " + e.getMessage());
            return null;
        }
    }

    private <T> List<T> loadAllNoLock(String filePath, Class<T> clazz) {
        String json = readJsonFile(filePath);
        if (json == null || json.isBlank()) return new ArrayList<>();
        Type listType = TypeToken.getParameterized(List.class, clazz).getType();
        List<T> result = gson.fromJson(json, listType);
        return result != null ? result : new ArrayList<>();
    }

    public Gson getGson() { return gson; }
}
