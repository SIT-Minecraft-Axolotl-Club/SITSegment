package club.sitmc.sitSegment;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Queries PKWS world state from SITParkourWarrior without a compile-time dependency.
 */
final class PkwsWorldChecker {
    private static final String WARRIOR_PLUGIN_NAME = "SITParkourWarrior";
    private static final String WARRIOR_METHOD_NAME = "isPkwsWorld";

    private final JavaPlugin plugin;
    private final Set<String> activeWarnings = new HashSet<>();

    private Plugin cachedWarriorPlugin;
    private Method cachedIsPkwsWorldMethod;
    private boolean methodLookupAttempted;

    PkwsWorldChecker(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Returns whether the supplied world is marked as PKWS by Warrior.
     * A missing or unusable Warrior integration is deliberately treated as false.
     */
    boolean isPkwsWorld(String worldName) {
        try {
            Plugin warrior = Bukkit.getPluginManager().getPlugin(WARRIOR_PLUGIN_NAME);
            if (warrior == null) {
                clearCachedMethod();
                warnOnce("missing", "未找到 SITParkourWarrior，PKWS 世界查询已降级为 false。");
                return false;
            }
            clearWarning("missing");

            if (!warrior.isEnabled()) {
                warnOnce("disabled", "SITParkourWarrior 未启用，PKWS 世界查询已降级为 false。");
                return false;
            }
            clearWarning("disabled");

            if (warrior != cachedWarriorPlugin) {
                cachedWarriorPlugin = warrior;
                cachedIsPkwsWorldMethod = null;
                methodLookupAttempted = false;
            }

            Method method = resolveMethod(warrior);
            if (method == null) {
                return false;
            }

            Object result = method.invoke(warrior, worldName);
            if (!(result instanceof Boolean)) {
                warnOnce("invoke", "SITParkourWarrior.isPkwsWorld(String) 返回值不是 boolean，"
                        + "PKWS 世界查询已降级为 false。");
                return false;
            }
            clearWarning("invoke");
            return (Boolean) result;
        } catch (Exception ex) {
            warnOnce("invoke", "调用 SITParkourWarrior.isPkwsWorld(String) 异常，"
                    + "PKWS 世界查询已降级为 false（" + describe(ex) + "）。");
            return false;
        } catch (LinkageError error) {
            warnOnce("invoke", "调用 SITParkourWarrior.isPkwsWorld(String) 时发生链接错误，"
                    + "PKWS 世界查询已降级为 false（" + describe(error) + "）。");
            return false;
        }
    }

    private Method resolveMethod(Plugin warrior) {
        if (cachedIsPkwsWorldMethod != null) {
            return cachedIsPkwsWorldMethod;
        }
        if (methodLookupAttempted) {
            return null;
        }
        methodLookupAttempted = true;

        try {
            Method method = warrior.getClass().getMethod(WARRIOR_METHOD_NAME, String.class);
            if (method.getReturnType() != boolean.class) {
                warnOnce("method", "SITParkourWarrior 缺少 public boolean isPkwsWorld(String) 方法，"
                        + "PKWS 世界查询已降级为 false。");
                return null;
            }
            cachedIsPkwsWorldMethod = method;
            clearWarning("method");
            return method;
        } catch (Exception ex) {
            warnOnce("method", "无法反射获取 SITParkourWarrior 的 public boolean "
                    + "isPkwsWorld(String) 方法，PKWS 世界查询已降级为 false（"
                    + describe(ex) + "）。");
            return null;
        } catch (LinkageError error) {
            warnOnce("method", "反射获取 SITParkourWarrior.isPkwsWorld(String) 时发生链接错误，"
                    + "PKWS 世界查询已降级为 false（" + describe(error) + "）。");
            return null;
        }
    }

    private void clearCachedMethod() {
        cachedWarriorPlugin = null;
        cachedIsPkwsWorldMethod = null;
        methodLookupAttempted = false;
    }

    private void warnOnce(String key, String message) {
        if (activeWarnings.add(key)) {
            plugin.getLogger().warning(message);
        }
    }

    private void clearWarning(String key) {
        activeWarnings.remove(key);
    }

    private String describe(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.isBlank()) {
            return throwable.getClass().getSimpleName();
        }
        return throwable.getClass().getSimpleName() + ": " + message;
    }
}
