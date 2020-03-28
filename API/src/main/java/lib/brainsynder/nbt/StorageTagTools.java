package lib.brainsynder.nbt;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import lib.brainsynder.ServerVersion;
import lib.brainsynder.reflection.FieldAccessor;
import lib.brainsynder.reflection.Reflection;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class StorageTagTools {
    private static Object registry = null;
    private static Class<?> nbtTag, craftStack, stackClass;
    private static Constructor newStack, newKey;
    private static Method save, newItem, toString, asCopy, asBukkitCopy, getItem, parseString;

    static {
        Class parser = Reflection.getNmsClass("MojangsonParser");
        parseString = Reflection.getMethod(parser, "parse", String.class);

        craftStack = Reflection.getCBCClass("inventory.CraftItemStack");
        Class keyClass = Reflection.getNmsClass("MinecraftKey");

        newKey = Reflection.getConstructor(keyClass, String.class);
        nbtTag = Reflection.getNmsClass("NBTTagCompound");
        stackClass = Reflection.getNmsClass("ItemStack"); /** {@link net.minecraft.server.v1_13_R1.ItemStack} */
        newStack = Reflection.getConstructor(stackClass, nbtTag);

        if (ServerVersion.isEqualNew(ServerVersion.v1_14_R1)) { // TODO: Find the correct version this changed in
            FieldAccessor accessor = FieldAccessor.getField(Reflection.getNmsClass("IRegistry"), "ITEM", Object.class);
            registry = accessor.get(null);
            getItem = Reflection.getMethod(registry.getClass(), "get", keyClass);
        }else{
            getItem = Reflection.getMethod(Reflection.getNmsClass("Items"), "get", String.class);
        }
        if (ServerVersion.isEqualNew(ServerVersion.v1_13_R1))
            newItem = Reflection.getMethod(stackClass, "a", nbtTag);
        asBukkitCopy = Reflection.getMethod(craftStack, "asBukkitCopy", stackClass);

        save = Reflection.getMethod(stackClass, "save", nbtTag);
        toString = Reflection.getMethod(nbtTag, "toString");
        asCopy = Reflection.getMethod(craftStack, "asNMSCopy", ItemStack.class);
    }

    public static ItemStack toItemStack (StorageTagCompound compound) {
        if (!compound.hasKey("id")) return new ItemStack(Material.AIR); // Checks if it is an ItemStacks NBT/STC

        Object nbt = Reflection.invoke(parseString, null, compound.toString());
        Object nmsStack;
        if (ServerVersion.isEqualOld(ServerVersion.v1_12_R1)) {
            nmsStack = Reflection.initiateClass(newStack, nbt); // Will make it an NMS ItemStack
        }else{
            nmsStack = Reflection.invoke(newItem, null, nbt); // Will make it an NMS ItemStack
        }

        return (ItemStack) Reflection.invoke(asBukkitCopy, null, nmsStack);
    }

    public static StorageTagCompound toStorage (ItemStack item) {
        Object nbt = Reflection.invoke(save, asNMSCopy(item), newNBTTag(nbtTag));
        String json = (String) Reflection.invoke(toString, nbt);

        // Removes the extra formatting that Spigot adds
        //if (json.contains("{\"text\":\"")) json = json.replace("'{\"text\":\"", "\"").replace("\"}'", "\"");
        StorageTagCompound compound = new StorageTagCompound ();

        try {
            compound = JsonToNBT.getTagFromJson(json);
        } catch (NBTException e) {
            e.printStackTrace();
        }

        return compound;
    }

    public static JsonObject toJsonObject (StorageTagCompound compound) {
        JsonObject json = new JsonObject ();
        compound.getKeySet().forEach(key -> {
            StorageBase base = compound.getTag(key);

            if (compound.isBoolean(key)) {
                json.add (key, compound.getBoolean(key));
            }else if (base instanceof StoragePrimitive) {
                json.add(key, ((StoragePrimitive)base).getInt());
            }else if (base instanceof IStorageList) {
                JsonArray array = new JsonArray();
                Object list = ((IStorageList)base).getList();
                if (list instanceof byte[]) {
                    for (byte v : (byte[]) list) array.add(v+"-B");
                }else if (list instanceof int[]) {
                    for (int v : (int[]) list) array.add(v+"-I");
                }else if (list instanceof long[]) {
                    for (long v : (long[]) list) array.add(v+"-L");
                }else if (list instanceof List) {
                    ((List)list).forEach(string -> array.add(String.valueOf(string).replace("\"", "")));
                }
                json.add(key, array);
            }else if (base instanceof StorageTagCompound) {
                json.add(key, toJsonObject((StorageTagCompound)base));
            }else if (base instanceof StorageTagString) {
                json.add(key, base.getString());
            }
        });
        return json;
    }

    public static StorageTagCompound fromJsonObject (JsonObject json) {
        StorageTagCompound compound = new StorageTagCompound ();
        json.names().forEach(key -> {
            JsonValue value = json.get(key);
            if (value.isNumber()) {
                compound.setInteger(key, value.asInt());
            }else if (value.isBoolean()) {
                compound.setBoolean(key, value.asBoolean());
            }else if (value.isString()) {
                compound.setString(key, value.asString());
            }else if (value.isArray()) {
                JsonArray array = value.asArray();
                List<Byte> bytes = new ArrayList<>();
                List<Integer> ints = new ArrayList<>();
                List<Long> longs = new ArrayList<>();
                StorageTagList list = new StorageTagList();

                array.values().forEach(jsonValue -> {
                    if (jsonValue.isString()) {
                        String string = jsonValue.asString();
                        if (string.endsWith("-L")) {
                            longs.add(jsonValue.asLong());
                        }else if (string.endsWith("-B")) {
                            bytes.add((byte) jsonValue.asInt());
                        }else if (string.endsWith("-I")) {
                            ints.add(jsonValue.asInt());
                        }else {
                            list.appendTag(new StorageTagString(string));
                        }
                    }

                    if (jsonValue.isNumber()) {
                        ints.add(jsonValue.asInt());
                    }
                });

                if (!bytes.isEmpty()) {
                    compound.setTag(key, new StorageTagByteArray(bytes));
                }else if (!ints.isEmpty()) {
                    compound.setTag(key, new StorageTagIntArray(ints));
                }else if (!longs.isEmpty()) {
                    compound.setTag(key, new StorageTagLongArray(longs));
                }else{
                    compound.setTag(key, list);
                }

            }else if (value.isObject()) {
                compound.setTag(key, fromJsonObject(value.asObject()));
            }
        });

        return compound;
    }

    public static <T> T toNBTTag (StorageTagCompound compound) {
        return (T) Reflection.invoke(parseString, null, compound.toString());
    }

    private static Object asNMSCopy (ItemStack stack) {
        return Reflection.invoke(asCopy, null, stack);
    }
    private static Object newNBTTag (Class<?> nbtTag) {
        return Reflection.initiateClass(nbtTag);
    }
    private static Object newMCKey (String key) {
        return Reflection.initiateClass(newKey, key);
    }
}
