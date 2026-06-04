package vn.korapayments.common.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import vn.korapayments.KoraPayments;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;

public final class PlatformScheduler {
    private final Plugin plugin;
    private final boolean folia;

    public PlatformScheduler(Plugin plugin) {
        this.plugin = plugin;
        this.folia = detectFolia();
    }

    public boolean isFolia() {
        return folia;
    }

    public String getPlatformName() {
        return folia ? "Folia" : "Bukkit/Paper";
    }

    public void runGlobal(Runnable runnable) {
        if (runnable == null) return;
        if (folia) {
            try {
                Object scheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                Method execute = scheduler.getClass().getMethod("execute", Plugin.class, Runnable.class);
                execute.invoke(scheduler, plugin, runnable);
                return;
            } catch (Throwable throwable) {
                logWarning("Folia global scheduler failed, falling back to Bukkit scheduler.", throwable);
            }
        }
        Bukkit.getScheduler().runTask(plugin, runnable);
    }


    public ScheduledTask runGlobalLater(Runnable runnable, long delayTicks) {
        if (runnable == null) return () -> {};
        long safeDelayTicks = Math.max(0L, delayTicks);
        if (folia) {
            try {
                Object scheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                Method runDelayed = scheduler.getClass().getMethod("runDelayed", Plugin.class, Consumer.class, long.class);
                Object task = runDelayed.invoke(
                        scheduler,
                        plugin,
                        (Consumer<Object>) scheduledTask -> runnable.run(),
                        Math.max(1L, safeDelayTicks)
                );
                return new ReflectionScheduledTask(task);
            } catch (Throwable throwable) {
                logWarning("Folia delayed global scheduler failed, falling back to Bukkit scheduler.", throwable);
            }
        }
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, runnable, safeDelayTicks);
        return task::cancel;
    }

    public void runPlayer(Player player, Runnable runnable) {
        if (runnable == null) return;
        if (player == null) {
            runGlobal(runnable);
            return;
        }
        if (folia) {
            try {
                Object entityScheduler = player.getClass().getMethod("getScheduler").invoke(player);
                Method execute = entityScheduler.getClass().getMethod(
                        "execute", Plugin.class, Runnable.class, Runnable.class, long.class
                );
                execute.invoke(entityScheduler, plugin, runnable, null, 1L);
                return;
            } catch (Throwable throwable) {
                logWarning("Folia player scheduler failed for " + player.getName() + ", falling back to global scheduler.", throwable);
                runGlobal(runnable);
                return;
            }
        }
        Bukkit.getScheduler().runTask(plugin, runnable);
    }

    public void runAsync(Runnable runnable) {
        if (runnable == null) return;
        if (folia) {
            try {
                Object scheduler = Bukkit.class.getMethod("getAsyncScheduler").invoke(null);
                Method runNow = scheduler.getClass().getMethod("runNow", Plugin.class, Consumer.class);
                runNow.invoke(scheduler, plugin, (Consumer<Object>) task -> runnable.run());
                return;
            } catch (Throwable throwable) {
                logWarning("Folia async scheduler failed, falling back to Bukkit async scheduler.", throwable);
            }
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
    }

    public ScheduledTask runTimerAsync(Runnable runnable, long delayTicks, long periodTicks) {
        if (runnable == null) return () -> {};
        long safeDelayTicks = Math.max(0L, delayTicks);
        long safePeriodTicks = Math.max(1L, periodTicks);
        if (folia) {
            try {
                Object scheduler = Bukkit.class.getMethod("getAsyncScheduler").invoke(null);
                Method runAtFixedRate = scheduler.getClass().getMethod(
                        "runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class, TimeUnit.class
                );
                long delayMillis = Math.max(1L, safeDelayTicks * 50L);
                long periodMillis = Math.max(50L, safePeriodTicks * 50L);
                Object task = runAtFixedRate.invoke(
                        scheduler,
                        plugin,
                        (Consumer<Object>) scheduledTask -> runnable.run(),
                        delayMillis,
                        periodMillis,
                        TimeUnit.MILLISECONDS
                );
                return new ReflectionScheduledTask(task);
            } catch (Throwable throwable) {
                logWarning("Folia async timer failed, falling back to Bukkit async timer.", throwable);
            }
        }
        BukkitTask task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, safeDelayTicks, safePeriodTicks);
        return task::cancel;
    }

    private void logWarning(String message, Throwable throwable) {
        if (plugin instanceof KoraPayments koraPayments) {
            koraPayments.logWarning(message, throwable);
            return;
        }
        plugin.getLogger().log(Level.WARNING, message, throwable);
    }

    private boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    public interface ScheduledTask {
        void cancel();
    }

    private static final class ReflectionScheduledTask implements ScheduledTask {
        private final Object task;

        private ReflectionScheduledTask(Object task) {
            this.task = task;
        }

        @Override
        public void cancel() {
            if (task == null) return;
            try {
                task.getClass().getMethod("cancel").invoke(task);
            } catch (Throwable ignored) {
            }
        }
    }
}
