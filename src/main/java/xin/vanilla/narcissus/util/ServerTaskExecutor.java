package xin.vanilla.narcissus.util;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.function.Supplier;

public class ServerTaskExecutor {

    private static final Logger LOGGER = LogManager.getLogger();

    // 任务队列（线程安全）
    private static final Queue<FutureTask<?>> TASK_QUEUE = new ConcurrentLinkedQueue<>();

    /**
     * 提交无返回的任务
     */
    public static void run(Runnable task) {
        TASK_QUEUE.offer(new FutureTask<>(task, null));
    }

    /**
     * 提交有返回的任务，并获取Future
     */
    public static <T> Future<T> call(Callable<T> task) {
        FutureTask<T> futureTask = new FutureTask<>(task);
        TASK_QUEUE.offer(futureTask);
        return futureTask;
    }

    /**
     * 提交有返回的任务（支持lambda的Supplier）
     */
    public static <T> Future<T> call(Supplier<T> supplier) {
        return call((Callable<T>) supplier::get);
    }

    /**
     * Tick事件，负责执行队列中的任务
     */
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            processTasks();
        }
    }

    /**
     * 逐个执行任务
     */
    private static void processTasks() {
        FutureTask<?> task;
        while ((task = TASK_QUEUE.poll()) != null) {
            try {
                task.run();
            } catch (Exception e) {
                LOGGER.error("Failed to execute task", e);
            }
        }
    }

    /**
     * 初始化（在FML启动时注册事件）
     */
    public static void init() {
        cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(new ServerTaskExecutor());
    }
}
