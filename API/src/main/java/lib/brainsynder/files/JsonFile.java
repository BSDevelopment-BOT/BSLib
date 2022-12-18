package lib.brainsynder.files;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.WriterConfig;
import com.google.common.base.Charsets;

import java.io.*;
import java.nio.charset.Charset;
import java.util.List;

public abstract class JsonFile implements Movable {
    private final Charset ENCODE = Charsets.UTF_8;
    private JsonObject json;
    protected JsonObject defaults = new JsonObject();
    private final File file;
    private boolean update = false;

    public JsonFile(File file) {
        this(file, true);
    }

    public JsonFile(File file, boolean loadDefaults) {
        this.file = file;
        if (loadDefaults) reload();
    }

    public abstract void loadDefaults();

    public void reload() {
        try {
            if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
            boolean defaultsLoaded = false;
            if (!file.exists()) {
                loadDefaults();
                defaultsLoaded = true;
                OutputStreamWriter pw = new OutputStreamWriter(new FileOutputStream(file), ENCODE);
                pw.write(defaults.toString(WriterConfig.PRETTY_PRINT).replace("\u0026", "&"));
                pw.flush();
                pw.close();
            }
            json = (JsonObject) Json.parse(new InputStreamReader(new FileInputStream(file), ENCODE));
            if (!defaultsLoaded) {
                loadDefaults();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean save() {
        return save(false);
    }

    /**
     * Will save the JSON data to the file
     *
     * @param clearFile - Should the file be cleared each time to prevent text duplication (happens on occasion)
     */
    public boolean save(boolean clearFile) {
        String text = json.toString(WriterConfig.PRETTY_PRINT).replace("\u0026", "&");
        OutputStreamWriter fw;

        // Will clear the file of existing text to prevent text duplication
        if (file.exists() && clearFile) {
            file.delete();
            try {
                file.createNewFile();
            } catch (Exception ignored) {}
        }


        try {
            fw = new OutputStreamWriter(new FileOutputStream(file), ENCODE);
            try {
                fw.write(text);
            } finally {
                fw.flush();
                fw.close();
            }
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public List<String> getKeys () {
        return json.names();
    }

    public String getName() {
        return file.getName().replace(".json", "");
    }

    public boolean containsKey (String key) {
        return hasKey(key) || hasDefaultKey(key);
    }
    public boolean hasKey (String key) {
        return json.names().contains(key);
    }
    private boolean hasDefaultKey (String key) {
        return defaults.names().contains(key);
    }

    public JsonValue getValue (String key) {
        JsonValue value = null;
        if (hasKey(key)) value = json.get(key);
        if ((value == null) && hasDefaultKey(key)) value = defaults.get(key);
        return value;
    }

    public JsonValue getDefaultValue (String key) {
        if (!hasDefaultKey(key)) return null;
        return defaults.get(key);
    }

    public String getString (String key) {
        return getString(key, "");
    }
    public String getString (String key, String fallback) {
        JsonValue value = getValue(key);
        if (value == null) return fallback;
        return value.asString();
    }
    public double getDouble (String key) {
        return getDouble(key, 0);
    }
    public double getDouble (String key, double fallback) {
        JsonValue value = getValue(key);
        if (value == null) return fallback;
        if (value.isString()) return Double.parseDouble(value.asString());
        return value.asDouble();
    }
    public byte getByte (String key) {
        return getByte(key, (byte) 0);
    }
    public byte getByte (String key, byte fallback) {
        JsonValue value = getValue(key);
        if (value == null) return fallback;
        if (value.isString()) return Byte.parseByte(value.asString());
        return (byte) value.asInt();
    }
    public boolean getBoolean (String key) {
        return getBoolean(key, false);
    }
    public boolean getBoolean (String key, boolean fallback) {
        JsonValue value = getValue(key);
        if (value == null) return fallback;
        try {
            if (value.isString()) return Boolean.parseBoolean(value.asString());
        } catch (IllegalArgumentException | NullPointerException ex) {
            return fallback;
        }
        return value.asBoolean();
    }
    public short getShort (String key) {
        return getShort(key, (short) 0);
    }
    public short getShort (String key, short fallback) {
        JsonValue value = getValue(key);
        if (value == null) return fallback;
        if (value.isString()) return Short.parseShort(value.asString());
        return (short) value.asLong();
    }
    public float getFloat (String key) {
        return getFloat(key, 0);
    }
    public float getFloat (String key, float fallback) {
        JsonValue value = getValue(key);
        if (value == null) return fallback;
        if (value.isString()) return Float.parseFloat(value.asString());
        return value.asFloat();
    }
    public long getLong (String key) {
        return getLong(key, 0);
    }
    public long getLong (String key, long fallback) {
        JsonValue value = getValue(key);
        if (value == null) return fallback;
        if (value.isString()) return Long.parseLong(value.asString());
        return value.asLong();
    }
    public int getInteger (String key) {
        return getInteger(key, 0);
    }
    public int getInteger (String key, int fallback) {
        JsonValue value = getValue(key);
        if (value == null) return fallback;
        if (value.isString()) return Integer.parseInt(value.asString());
        return value.asInt();
    }


    public void set(String key, int value) {
        update = true;
        json.set(key, value);
    }
    public void set(String key, long value) {
        update = true;
        json.set(key, value);
    }
    public void set(String key, float value) {
        update = true;
        json.set(key, value);
    }
    public void set(String key, short value) {
        update = true;
        json.set(key, value);
    }
    public void set(String key, byte value) {
        update = true;
        json.set(key, value);
    }
    public void set(String key, double value) {
        update = true;
        json.set(key, value);
    }
    public void set(String key, boolean value) {
        update = true;
        json.set(key, value);
    }
    public void set(String key, String value) {
        update = true;
        json.set(key, value);
    }
    public void set(String key, JsonValue value) {
        update = true;
        json.set(key, value);
    }

    public void remove (String key) {
        boolean update = false;
        if (defaults.names().contains(key)) {
            update = true;
            defaults.remove(key);
        }
        if (json.names().contains(key)) {
            update = true;
            json.remove(key);
        }
        if (update) save();
    }




    public void setDefault(String key, int value) {
        defaults.add(key, value);
    }
    public void setDefault(String key, long value) {
        defaults.add(key, value);
    }
    public void setDefault(String key, float value) {
        defaults.add(key, value);
    }
    public void setDefault(String key, short value) {
        defaults.add(key, value);
    }
    public void setDefault(String key, byte value) {
        defaults.add(key, value);
    }
    public void setDefault(String key, double value) {
        defaults.add(key, value);
    }
    public void setDefault(String key, boolean value) {
        defaults.add(key, value);
    }
    public void setDefault(String key, String value) {
        defaults.add(key, value);
    }
    public void setDefault(String key, JsonValue value) {
        defaults.add(key, value);
    }

    @Override
    public boolean move(String oldKey, String newKey) {
        if (hasKey(oldKey)) {
            json.set(newKey, getValue(oldKey));
            json.remove(oldKey);
            save();
            return true;
        }
        return false;
    }

    public File getFile() {
        return file;
    }
}
