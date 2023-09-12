package club.xiaojiawei.initializer;

import club.xiaojiawei.data.SpringData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @author 肖嘉威
 * @date 2023/7/4 11:33
 * @msg 开启游戏日志输出
 */
@Component
@Slf4j
public class LogInitializer extends AbstractInitializer{

    @Resource
    private SpringData springData;
    @Override
    public void exec() {
        File logFile = new File(springData.getGameLogConfigurationPath());
        try(BufferedWriter writer = new BufferedWriter(new FileWriter(logFile))){
            writer.write("""
                    [LoadingScreen]
                    LogLevel=1
                    FilePrinting=True
                    ConsolePrinting=False
                    ScreenPrinting=False
                    Verbose=False
                    [Arena]
                    LogLevel=1
                    FilePrinting=True
                    ConsolePrinting=False
                    ScreenPrinting=False
                    Verbose=False
                    [Decks]
                    LogLevel=1
                    FilePrinting=True
                    ConsolePrinting=False
                    ScreenPrinting=False
                    Verbose=False
                    [Achievements]
                    LogLevel=1
                    FilePrinting=True
                    ConsolePrinting=False
                    ScreenPrinting=False
                    Verbose=False
                    [FullScreenFX]
                    LogLevel=1
                    FilePrinting=True
                    ConsolePrinting=False
                    ScreenPrinting=False
                    Verbose=False
                    [Power]
                    LogLevel=1
                    FilePrinting=True
                    ConsolePrinting=False
                    ScreenPrinting=False
                    Verbose=True
                    """);
            log.info("{}文件重写完成，游戏日志已打开，首次重写需要重启炉石传说", logFile.getName());
        } catch (IOException e) {
            throw new RuntimeException("文件重写失败，游戏日志未打开，脚本无法运行", e);
        }
        if (nextInitializer != null){
            nextInitializer.init();
        }
    }
}
