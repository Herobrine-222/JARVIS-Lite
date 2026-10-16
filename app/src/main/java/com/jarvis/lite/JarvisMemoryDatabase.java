
package com.jarvis.lite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Banco SQLite local para a memória persistente do JARVIS.
 *
 * Responsabilidades:
 * - Armazenar memórias sem o antigo limite fixo de 150 registros.
 * - Preservar os campos usados pelo JarvisMemory.
 * - Oferecer consultas por categoria e texto.
 * - Permitir migração e importação de registros JSON.
 *
 * Esta classe ainda precisa ser conectada ao JarvisMemory.
 * A criação desta classe, isoladamente, não migra os dados antigos.
 */
public final class JarvisMemoryDatabase extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "jarvis_memory.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_MEMORIES = "memories";

    private static final String COL_ID = "_id";
    private static final String COL_TEXT = "memory_text";
    private static final String COL_CATEGORY = "category";
    private static final String COL_NORMALIZED_TEXT = "normalized_text";
    private static final String COL_NORMALIZED_CATEGORY = "normalized_category";
    private static final String COL_TIME = "created_time";
    private static final String COL_UPDATED_TIME = "updated_time";
    private static final String COL_LAST_ACCESS_TIME = "last_access_time";
    private static final String COL_IMPORTANCE = "importance";
    private static final String COL_ACCESS_COUNT = "access_count";
    private static final String COL_TAGS = "tags_json";

    private static final int MAX_TEXT = 500;
    private static final int MAX_CATEGORY = 80;

    public JarvisMemoryDatabase(Context context) {
        super(
                context.getApplicationContext(),
                DATABASE_NAME,
                null,
                DATABASE_VERSION
        );
    }

    // ============================================================
    // CRIAÇÃO DO BANCO
    // ============================================================

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL(
                "CREATE TABLE " + TABLE_MEMORIES + " ("
                        + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + COL_TEXT + " TEXT NOT NULL, "
                        + COL_CATEGORY + " TEXT NOT NULL, "
                        + COL_NORMALIZED_TEXT + " TEXT NOT NULL, "
                        + COL_NORMALIZED_CATEGORY + " TEXT NOT NULL, "
                        + COL_TIME + " INTEGER NOT NULL, "
                        + COL_UPDATED_TIME + " INTEGER NOT NULL, "
                        + COL_LAST_ACCESS_TIME + " INTEGER NOT NULL DEFAULT 0, "
                        + COL_IMPORTANCE + " INTEGER NOT NULL DEFAULT 50, "
                        + COL_ACCESS_COUNT + " INTEGER NOT NULL DEFAULT 0, "
                        + COL_TAGS + " TEXT NOT NULL DEFAULT '[]', "
                        + "UNIQUE ("
                        + COL_NORMALIZED_TEXT + ", "
                        + COL_NORMALIZED_CATEGORY
                        + ")"
                        + ")"
        );

        db.execSQL(
                "CREATE INDEX idx_memories_category "
                        + "ON " + TABLE_MEMORIES
                        + " (" + COL_NORMALIZED_CATEGORY + ")"
        );

        db.execSQL(
                "CREATE INDEX idx_memories_updated "
                        + "ON " + TABLE_MEMORIES
                        + " (" + COL_UPDATED_TIME + ")"
        );
    }

    @Override
    public void onUpgrade(
            SQLiteDatabase db,
            int oldVersion,
            int newVersion
    ) {
        /*
         * Não apagamos a tabela aqui.
         * Futuras alterações no esquema devem usar migrações
         * específicas para preservar as memórias existentes.
         */
    }

    // ============================================================
    // INSERIR OU ATUALIZAR UMA MEMÓRIA
    // ============================================================

    public synchronized boolean upsertItem(JSONObject item) {

        if (item == null) {
            return false;
        }

        try {
            SQLiteDatabase db = getWritableDatabase();
            return upsertItemInternal(db, item);
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean upsertItemInternal(
            SQLiteDatabase db,
            JSONObject item
    ) {

        if (item == null) {
            return false;
        }

        String text = item.optString("text", "").trim();

        if (text.isEmpty()) {
            return false;
        }

        if (text.length() > MAX_TEXT) {
            text = text.substring(0, MAX_TEXT);
        }

        String category = sanitizeCategory(
                item.optString("category", "geral")
        );

        String normalizedText = normalize(text);
        String normalizedCategory = normalize(category);

        long now = System.currentTimeMillis();

        long createdTime = item.optLong("time", now);
        long updatedTime = item.optLong("updatedTime", createdTime);

        long lastAccessTime = Math.max(
                0L,
                item.optLong("lastAccessTime", 0L)
        );

        int importance = clamp(
                item.optInt("importance", 50),
                0,
                100
        );

        int accessCount = Math.max(
                0,
                item.optInt("accessCount", 0)
        );

        JSONArray tags = item.optJSONArray("tags");

        if (tags == null) {
            tags = new JSONArray();
        }

        ContentValues values = new ContentValues();

        values.put(COL_TEXT, text);
        values.put(COL_CATEGORY, category);
        values.put(COL_NORMALIZED_TEXT, normalizedText);
        values.put(COL_NORMALIZED_CATEGORY, normalizedCategory);
        values.put(COL_TIME, createdTime);
        values.put(COL_UPDATED_TIME, updatedTime);
        values.put(COL_LAST_ACCESS_TIME, lastAccessTime);
        values.put(COL_IMPORTANCE, importance);
        values.put(COL_ACCESS_COUNT, accessCount);
        values.put(COL_TAGS, tags.toString());

        int updated = db.update(
                TABLE_MEMORIES,
                values,
                COL_NORMALIZED_TEXT + " = ? AND "
                        + COL_NORMALIZED_CATEGORY + " = ?",
                new String[]{
                        normalizedText,
                        normalizedCategory
                }
        );

        if (updated > 0) {
            return true;
        }

        long inserted = db.insertWithOnConflict(
                TABLE_MEMORIES,
                null,
                values,
                SQLiteDatabase.CONFLICT_IGNORE
        );

        return inserted != -1L;
    }

    // ============================================================
    // LER TODAS AS MEMÓRIAS
    // ============================================================

    public synchronized JSONArray getAllItems() {

        JSONArray result = new JSONArray();
        Cursor cursor = null;

        try {
            SQLiteDatabase db = getReadableDatabase();

            cursor = db.query(
                    TABLE_MEMORIES,
                    null,
                    null,
                    null,
                    null,
                    null,
                    COL_ID + " ASC"
            );

            while (cursor.moveToNext()) {
                JSONObject item = cursorToJson(cursor);

                if (item != null) {
                    result.put(item);
                }
            }

        } catch (Exception ignored) {
            // Retorna os registros que conseguiram ser lidos.
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return result;
    }

    // ============================================================
    // LER MEMÓRIAS POR CATEGORIA
    // ============================================================

    public synchronized JSONArray getItemsByCategory(String category) {

        JSONArray result = new JSONArray();

        if (category == null || category.trim().isEmpty()) {
            return result;
        }

        Cursor cursor = null;

        try {
            SQLiteDatabase db = getReadableDatabase();

            cursor = db.query(
                    TABLE_MEMORIES,
                    null,
                    COL_NORMALIZED_CATEGORY + " = ?",
                    new String[]{normalize(category)},
                    null,
                    null,
                    COL_ID + " ASC"
            );

            while (cursor.moveToNext()) {
                JSONObject item = cursorToJson(cursor);

                if (item != null) {
                    result.put(item);
                }
            }

        } catch (Exception ignored) {
            // Mantém os resultados já encontrados.
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return result;
    }

    // ============================================================
    // LOCALIZAR UMA MEMÓRIA EXATA
    // ============================================================

    public synchronized JSONObject findExact(
            String text,
            String category
    ) {

        if (text == null || text.trim().isEmpty()) {
            return null;
        }

        String normalizedText = normalize(text);

        String normalizedCategory = normalize(
                sanitizeCategory(category)
        );

        Cursor cursor = null;

        try {
            SQLiteDatabase db = getReadableDatabase();

            cursor = db.query(
                    TABLE_MEMORIES,
                    null,
                    COL_NORMALIZED_TEXT + " = ? AND "
                            + COL_NORMALIZED_CATEGORY + " = ?",
                    new String[]{
                            normalizedText,
                            normalizedCategory
                    },
                    null,
                    null,
                    null,
                    "1"
            );

            if (cursor.moveToFirst()) {
                return cursorToJson(cursor);
            }

        } catch (Exception ignored) {
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return null;
    }

    // ============================================================
    // EXCLUIR MEMÓRIAS QUE CONTENHAM UM TEXTO
    // ============================================================

    public synchronized int deleteContainingText(String query) {

        if (query == null || query.trim().isEmpty()) {
            return 0;
        }

        String search = normalize(query);
        JSONArray items = getAllItems();

        SQLiteDatabase db;

        try {
            db = getWritableDatabase();
        } catch (Exception ignored) {
            return 0;
        }

        int deleted = 0;

        db.beginTransaction();

        try {
            for (int i = 0; i < items.length(); i++) {

                JSONObject item = items.optJSONObject(i);

                if (item == null) {
                    continue;
                }

                String text = item.optString("text", "");

                if (!normalize(text).contains(search)) {
                    continue;
                }

                int count = db.delete(
                        TABLE_MEMORIES,
                        COL_NORMALIZED_TEXT + " = ? AND "
                                + COL_NORMALIZED_CATEGORY + " = ?",
                        new String[]{
                                normalize(text),
                                normalize(
                                        item.optString("category", "geral")
                                )
                        }
                );

                deleted += count;
            }

            db.setTransactionSuccessful();

        } catch (Exception ignored) {
            deleted = 0;
        } finally {
            db.endTransaction();
        }

        return deleted;
    }

    // ============================================================
    // EXCLUIR UMA CATEGORIA
    // ============================================================

    public synchronized int deleteCategory(String category) {

        if (category == null || category.trim().isEmpty()) {
            return 0;
        }

        try {
            return getWritableDatabase().delete(
                    TABLE_MEMORIES,
                    COL_NORMALIZED_CATEGORY + " = ?",
                    new String[]{normalize(category)}
            );
        } catch (Exception ignored) {
            return 0;
        }
    }

    // ============================================================
    // ATUALIZAR CONTADOR DE ACESSO
    // ============================================================

    public synchronized boolean recordAccess(
            String text,
            String category
    ) {

        if (text == null || text.trim().isEmpty()) {
            return false;
        }

        try {
            SQLiteDatabase db = getWritableDatabase();

            db.execSQL(
                    "UPDATE " + TABLE_MEMORIES
                            + " SET " + COL_ACCESS_COUNT
                            + " = " + COL_ACCESS_COUNT + " + 1, "
                            + COL_LAST_ACCESS_TIME + " = ?"
                            + " WHERE " + COL_NORMALIZED_TEXT + " = ?"
                            + " AND " + COL_NORMALIZED_CATEGORY + " = ?",
                    new Object[]{
                            System.currentTimeMillis(),
                            normalize(text),
                            normalize(sanitizeCategory(category))
                    }
            );

            return findExact(text, category) != null;

        } catch (Exception ignored) {
            return false;
        }
    }

    // ============================================================
    // QUANTIDADE E LIMPEZA
    // ============================================================

    public synchronized int size() {

        Cursor cursor = null;

        try {
            cursor = getReadableDatabase().rawQuery(
                    "SELECT COUNT(*) FROM " + TABLE_MEMORIES,
                    null
            );

            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }

        } catch (Exception ignored) {
            return 0;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return 0;
    }

    public synchronized boolean hasAnyItems() {
        return size() > 0;
    }

    public synchronized void clear() {
        getWritableDatabase().delete(
                TABLE_MEMORIES,
                null,
                null
        );
    }

    // ============================================================
    // SUBSTITUIR TODAS AS MEMÓRIAS EM UMA TRANSAÇÃO
    // Usado para importação/migração, não para cada consulta.
    // ============================================================

    public synchronized boolean replaceAllItems(JSONArray items) {

        if (items == null) {
            return false;
        }

        SQLiteDatabase db;

        try {
            db = getWritableDatabase();
        } catch (Exception ignored) {
            return false;
        }

        try {
            db.beginTransaction();

            try {
                db.delete(TABLE_MEMORIES, null, null);

                for (int i = 0; i < items.length(); i++) {

                    JSONObject item = items.optJSONObject(i);

                    if (item == null) {
                        continue;
                    }

                    String text = item.optString(
                            "text",
                            ""
                    ).trim();

                    // Ignora entradas vazias ou malformadas.
                    if (text.isEmpty()) {
                        continue;
                    }

                    /*
                     * Uma memória válida que falhar ao ser salva
                     * provoca a reversão de toda a transação.
                     */
                    if (!upsertItemInternal(db, item)) {
                        throw new IllegalStateException(
                                "Falha ao salvar a memória de índice " + i
                        );
                    }
                }

                db.setTransactionSuccessful();
                return true;

            } finally {
                db.endTransaction();
            }

        } catch (Exception ignored) {
            return false;
        }
    }

    // ============================================================
    // CONVERTER UM REGISTRO SQL PARA O FORMATO JSON EXISTENTE
    // ============================================================

    private JSONObject cursorToJson(Cursor cursor) {

        try {
            JSONObject item = new JSONObject();

            item.put(
                    "text",
                    cursor.getString(
                            cursor.getColumnIndexOrThrow(COL_TEXT)
                    )
            );

            item.put(
                    "category",
                    cursor.getString(
                            cursor.getColumnIndexOrThrow(COL_CATEGORY)
                    )
            );

            item.put(
                    "time",
                    cursor.getLong(
                            cursor.getColumnIndexOrThrow(COL_TIME)
                    )
            );

            item.put(
                    "updatedTime",
                    cursor.getLong(
                            cursor.getColumnIndexOrThrow(COL_UPDATED_TIME)
                    )
            );

            item.put(
                    "lastAccessTime",
                    cursor.getLong(
                            cursor.getColumnIndexOrThrow(COL_LAST_ACCESS_TIME)
                    )
            );

            item.put(
                    "importance",
                    cursor.getInt(
                            cursor.getColumnIndexOrThrow(COL_IMPORTANCE)
                    )
            );

            item.put(
                    "accessCount",
                    cursor.getInt(
                            cursor.getColumnIndexOrThrow(COL_ACCESS_COUNT)
                    )
            );

            String tagsJson = cursor.getString(
                    cursor.getColumnIndexOrThrow(COL_TAGS)
            );

            try {
                item.put(
                        "tags",
                        new JSONArray(
                                tagsJson == null ? "[]" : tagsJson
                        )
                );
            } catch (Exception ignored) {
                item.put("tags", new JSONArray());
            }

            return item;

        } catch (Exception ignored) {
            return null;
        }
    }

    // ============================================================
    // NORMALIZAÇÃO E VALIDAÇÃO
    // ============================================================

    private String sanitizeCategory(String category) {

        if (category == null || category.trim().isEmpty()) {
            return "geral";
        }

        String result = category.trim();

        if (result.length() > MAX_CATEGORY) {
            result = result.substring(0, MAX_CATEGORY);
        }

        return result;
    }

    private String normalize(String value) {

        String result = value == null
                ? ""
                : value.toLowerCase(Locale.ROOT);

        result = Normalizer.normalize(
                result,
                Normalizer.Form.NFD
        );

        return result
                .replaceAll("\\p{M}+", "")
                .trim();
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
