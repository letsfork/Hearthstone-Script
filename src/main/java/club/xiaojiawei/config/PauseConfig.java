package club.xiaojiawei.config;

import club.xiaojiawei.controller.JavaFXDashboardController;
import club.xiaojiawei.core.Core;
import club.xiaojiawei.status.Work;
import club.xiaojiawei.utils.SystemUtil;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import javax.annotation.Resource;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author 肖嘉威
 * @date 2023/7/5 15:04
 * @msg
 */
@Configuration
@Slf4j
public class PauseConfig {

    @Resource
    @Lazy
    private JavaFXDashboardController javaFXDashboardController;
    @Resource
    @Lazy
    private Core core;
    /**
     * 脚本是否处于暂停中
     * @return
     */
    @Bean
    public AtomicReference<BooleanProperty> isPause(){
        SimpleBooleanProperty booleanProperty = new SimpleBooleanProperty(true);
        booleanProperty.addListener((observable, oldValue, newValue) -> {
            log.info("当前处于" + (newValue? "停止" : "运行") + "状态");
            javaFXDashboardController.changeSwitch(newValue);
            if (newValue){
                SystemUtil.cancelAllRunnable();
                Work.setWorking(false);
            }else {
                if (Work.canWork()){
                    core.start();
                }else {
                    Work.cannotWorkLog();
                }
            }
        });
        return new AtomicReference<>(booleanProperty);
    }

}
