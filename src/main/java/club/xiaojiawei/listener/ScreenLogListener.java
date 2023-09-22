package club.xiaojiawei.listener;

import club.xiaojiawei.core.Core;
import club.xiaojiawei.custom.LogRunnable;
import club.xiaojiawei.data.ScriptStaticData;
import club.xiaojiawei.data.SpringData;
import club.xiaojiawei.enums.ConfigurationKeyEnum;
import club.xiaojiawei.enums.ModeEnum;
import club.xiaojiawei.status.Mode;
import club.xiaojiawei.status.Work;
import club.xiaojiawei.utils.SystemUtil;
import javafx.beans.property.BooleanProperty;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Properties;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static club.xiaojiawei.data.ScriptStaticData.GAME_ALIVE_CMD;

/**
 * @author 肖嘉威
 * @date 2023/7/5 14:55
 * @msg
 */

@Slf4j
@Component
public class ScreenLogListener extends AbstractLogListener{
    @Resource
    private Core core;
    private static ScheduledFuture<?> errorScheduledFuture;
    @Setter
    private volatile static long lastWorkTime;
    public static final long MAX_IDLE_TIME = 5 * 60 * 1000L;

    @Autowired
    public ScreenLogListener(SpringData springData) {
        super(springData.getScreenLogName(), 0, 1500, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void readOldLog() throws IOException {
        String line;
        int index;
        ModeEnum finalMode = null;
        while ((line = accessFile.readLine()) != null) {
            if ((index = line.indexOf("currMode")) != -1) {
                finalMode = ModeEnum.valueOf(line.substring(index + 9));
            }
        }
        Mode.setCurrMode(finalMode);
    }

    @Override
    protected void listenLog() throws IOException, InterruptedException {
        String line;
        while (!isPause.get().get() && Strings.isNotBlank((line = accessFile.readLine()))){
            Mode.setCurrMode(resolveLog(line));
        }
        if (isPause.get().get()){
            cancelListener();
        }
    }

    @Override
    protected void otherListen() {
        lastWorkTime = System.currentTimeMillis();
        log.info("开始监听异常情况");
        errorScheduledFuture = listenFileThreadPool.scheduleAtFixedRate(new LogRunnable(() -> {
            if (!isPause.get().get() && System.currentTimeMillis() - lastWorkTime > MAX_IDLE_TIME){
                log.info("监听到异常情况，准备重启游戏");
                lastWorkTime = System.currentTimeMillis();
                core.restart();
            }
        }), 0, 1, TimeUnit.MINUTES);
    }

    @Override
    protected void cancelOtherListener() {
        if (errorScheduledFuture != null && !errorScheduledFuture.isDone()){
            errorScheduledFuture.cancel(true);
            log.info("已停止监听异常情况");
        }
    }

    private ModeEnum resolveLog(String line) throws InterruptedException, IOException {
        if (line == null){
            return null;
        }
        int index;
        if ((index = line.indexOf("currMode")) != -1){
            return ModeEnum.valueOf(line.substring(index + 9));
        }else if (line.contains("OnDestroy()")){
            Thread.sleep(2000);
            if (Strings.isBlank(new String(Runtime.getRuntime().exec(GAME_ALIVE_CMD).getInputStream().readAllBytes()))){
                log.info("检测到游戏关闭，准备重启游戏");
                core.restart();
            }
        }
        return null;
    }
}
