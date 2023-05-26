package lib.brainsynder;

import lib.brainsynder.utils.AdvString;
import lib.brainsynder.utils.Triple;
import org.bukkit.Bukkit;

import java.util.Objects;

public enum ServerVersion implements IVersion {
    // ---- START ---- //
    UNKNOWN,
    @Deprecated v1_8_R3,
    @Deprecated v1_9_R1,
    @Deprecated v1_9_R2,
    @Deprecated v1_10_R1,
    @Deprecated v1_11_R1,
    @Deprecated v1_12_R1,
    @Deprecated v1_13_R1,
    @Deprecated v1_13_R2,
    @Deprecated v1_14_R1,
    @Deprecated v1_15_R1,
    @Deprecated v1_16_R1,
    @Deprecated v1_16_R2,
    @Deprecated v1_16_R3,
    v1_17 ("v1_17_R1"),
    v1_17_1(v1_17),
    v1_18 ("v1_18_R1"),
    v1_18_1 (v1_18),
    v1_18_2 ("v1_18_R2"),
    v1_19 ("v1_19_R1"),
    v1_19_1 (v1_19),
    v1_19_2 (v1_19),
    v1_19_3 ("v1_19_R2"),
    v1_19_4 ("v1_19_R3"),
    v1_20 ("v1_20_R1")
    ; // ---- END ---- //

    private final String nms;
    private final ServerVersion parent;

    private static IVersion CURRENT_VERSION = null;

    ServerVersion () {
        parent = this;
        nms = name();
    }

    ServerVersion (String nms) {
        parent = this;
        this.nms = nms;
    }

    ServerVersion (ServerVersion parent) {
        this.parent = parent;
        this.nms = parent.nms;
    }

    ServerVersion (ServerVersion parent, String nms) {
        this.parent = parent;
        this.nms = nms;
    }

    /**
     * Get the NMS version for the version
     *
     * Example:
     * v1_17 = NMS: v1_17_R1
     * v1_17_1 =  NMS: v1_17_R1
     * v1_18 =  NMS: v1_18_R1
     */
    public String getNMS() {
        return parent.nms;
    }

    /**
     * This will fetch the servers version as an {@link Integer}
     *      EG: ServerVersion.v1_8 will return 1,8,0
     *      EG: ServerVersion.v1_17 will return 1,17,0
     *      EG: ServerVersion.v1_17_1 will return 1,17,1
     */
    public Triple<Integer, Integer, Integer> getVersionParts() {
        if (name().equals("UNKNOWN")) return Triple.of(-1, -1, -1);
        String name = name().replace("v", "").replace("R", "");
        String[] args = name.split("_");
        int[] ints = new int[] {0, 0, 0};

        if (args.length >= 1) ints[0] = Integer.parseInt(args[0]);
        if (args.length >= 2) ints[1] = Integer.parseInt(args[1]);
        if (args.length >= 3) ints[2] = Integer.parseInt(args[2]);
        return Triple.of(ints[0], ints[1], ints[2]);
    }

    /** Will fetch the servers current {@link ServerVersion} */
    public static <T extends IVersion> T getVersion () {
        return getVersion(false);
    }

    /** Will fetch the servers current {@link ServerVersion} */
    public static <T extends IVersion> T getVersion (boolean parent) {
        if (CURRENT_VERSION != null) {
            if (parent) return (T) CURRENT_VERSION.getParent();
            return (T) CURRENT_VERSION;
        }

        String mc = AdvString.between("MC: ", ")", Bukkit.getVersion());
        String mcVersion = "v"+mc.replace(".", "_");

        String[] args = mc.split("\\.");
        int[] ints = new int[] {0, 0, 0};

        if (args.length >= 1) ints[0] = Integer.parseInt(args[0]);
        if (args.length >= 2) ints[1] = Integer.parseInt(args[1]);
        if (args.length >= 3) ints[2] = Integer.parseInt(args[2]);
        Triple<Integer, Integer, Integer> triple = Triple.of(ints[0], ints[1], ints[2]);

        for (ServerVersion version : values()) {
            if (version.name().equals(mcVersion+"_R1")) {
                CURRENT_VERSION = version;
                if (parent) return (T) version.getParent();
                return (T) version;
            }
            if (version.name().equals(mcVersion)) {
                CURRENT_VERSION = version;
                if (parent) return (T) version.getParent();
                return (T) version;
            }
            if (version.name().equals(Bukkit.getServer().getClass().getPackage().getName().substring(23))) {
                CURRENT_VERSION = version;
                return (T) version;
            }
        }

        return (T) (CURRENT_VERSION = new IVersion() {
            @Override
            public String name() {
                if (triple.right == 0) return "v"+triple.left+"_"+triple.middle;
                return "v"+triple.left+"_"+triple.middle+"_"+triple.right;
            }

            @Override
            public String getNMS() {
                return Bukkit.getServer().getClass().getPackage().getName().substring(23);
            }

            @Override
            public Triple<Integer, Integer, Integer> getVersionParts() {
                return triple;
            }

            @Override
            public IVersion getParent() {
                return this;
            }
        });
    }

    public ServerVersion getParent() {
        return parent;
    }

    /** Will check if the servers version is equal or newer then the {@param version} */
    public static boolean isEqualNew (ServerVersion version) {
        Triple<Integer, Integer, Integer> current = getVersion().getVersionParts();
        Triple<Integer, Integer, Integer> compare = version.getVersionParts();
        if ((Objects.equals(current.left, compare.left) || (current.left >= compare.left))
                && (Objects.equals(current.middle, compare.middle) || (current.middle >= compare.middle))) {
            if (Objects.equals(current.middle, compare.middle))
                return (Objects.equals(current.right, compare.right) || (current.right >= compare.right));
            return true;
        }

        return false;
    }

    /** Will check if the servers version is newer then the {@param version} */
    public static boolean isNewer (ServerVersion version) {
        Triple<Integer, Integer, Integer> current = getVersion().getVersionParts();
        Triple<Integer, Integer, Integer> compare = version.getVersionParts();
        if ((Objects.equals(current.left, compare.left) || (current.left > compare.left))
                && (Objects.equals(current.middle, compare.middle) || (current.middle > compare.middle))) {
            if (Objects.equals(current.middle, compare.middle))
                return (Objects.equals(current.right, compare.right) || (current.right > compare.right));
            return true;
        }

        return false;
    }

    /** Will check if the servers version is equal to the {@param version} */
    public static boolean isEqual (ServerVersion version) {
        Triple<Integer, Integer, Integer> current = getVersion().getVersionParts();
        Triple<Integer, Integer, Integer> compare = version.getVersionParts();
        return (Objects.equals(current.left, compare.left))
                && (Objects.equals(current.middle, compare.middle))
                && (Objects.equals(current.right, compare.right));
    }

    /** Will check if the servers version is equal or older then the {@param version} */
    public static boolean isEqualOld (ServerVersion version) {
        Triple<Integer, Integer, Integer> current = getVersion().getVersionParts();
        Triple<Integer, Integer, Integer> compare = version.getVersionParts();
        if ((Objects.equals(current.left, compare.left) || (current.left <= compare.left))
                && (Objects.equals(current.middle, compare.middle) || (current.middle <= compare.middle))) {
            if (Objects.equals(current.middle, compare.middle))
                return (Objects.equals(current.right, compare.right) || (current.right <= compare.right));
            return true;
        }

        return false;
    }

    /** Will check if the servers version is older then the {@param version} */
    public static boolean isOlder (ServerVersion version) {
        Triple<Integer, Integer, Integer> current = getVersion().getVersionParts();
        Triple<Integer, Integer, Integer> compare = version.getVersionParts();
        if ((Objects.equals(current.left, compare.left) || (current.left < compare.left))
                && (Objects.equals(current.middle, compare.middle) || (current.middle < compare.middle))) {
            if (Objects.equals(current.middle, compare.middle))
                return (Objects.equals(current.right, compare.right) || (current.right < compare.right));
            return true;
        }

        return false;
    }
}
