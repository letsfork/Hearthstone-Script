package club.xiaojiawei.listener;

import club.xiaojiawei.custom.LogRunnable;
import club.xiaojiawei.data.ScriptStaticData;
import club.xiaojiawei.utils.SystemUtil;
import javafx.beans.property.BooleanProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author 肖嘉威
 * @date 2023/9/20 16:54
 * @msg
 */
@Slf4j
public abstract class AbstractLogListener {
    @Resource
    protected ScheduledThreadPoolExecutor listenFileThreadPool;
    @Resource
    protected AtomicReference<BooleanProperty> isPause;
    @Setter
    protected static File logDir;
    @Getter
    protected RandomAccessFile accessFile;
    protected ScheduledFuture<?> logScheduledFuture;
    protected String logName;
    protected long listenInitialDelay;
    protected long listenPeriod;
    protected TimeUnit listenUnit;

    public AbstractLogListener(String logName,
                               long listenInitialDelay,
                               long listenPeriod,
                               TimeUnit listenUnit) {
        this.logName = logName;
        this.listenInitialDelay = listenInitialDelay;
        this.listenPeriod = listenPeriod;
        this.listenUnit = listenUnit;
    }

    protected abstract void readOldLog() throws Exception;
    protected abstract void listenLog() throws Exception;
    protected void otherListen(){};
    protected void cancelOtherListener(){};
    public synchronized void listen(){
        if (logScheduledFuture != null && !logScheduledFuture.isDone()){
            log.warn(logName + "正在被监听，无法再次被监听");
            return;
        }
        closeLogStream();
        File logFile = createFile();
        log.info("开始监听" + logName);
        try {
            accessFile = new RandomAccessFile(logFile, "r");
            readOldLog();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        logScheduledFuture = listenFileThreadPool.scheduleAtFixedRate(new LogRunnable(() -> {
            try {
                listenLog();
            }catch (Exception e){
                log.warn(logName + "监听器发生错误", e);
            }
        }), listenInitialDelay, listenPeriod, listenUnit);
        otherListen();
    }
    private File createFile(){
        File logFile = new File(logDir.getAbsolutePath() + "/" + logName);
        if (!logFile.exists()){
            try(FileWriter fileWriter = new FileWriter(logFile)){
                fileWriter.write("");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return logFile;
    }
    private void closeLogStream(){
        if (accessFile != null){
            try {
                accessFile.close();
                log.info("旧的" + logName + "文件流已关闭");
                accessFile = null;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    public void cancelListener(){
        if (logScheduledFuture != null && !logScheduledFuture.isDone()){
            logScheduledFuture.cancel(true);
            log.info("已停止监听" + logName);
            SystemUtil.delay(2000);
        }
        cancelOtherListener();
    }
}
