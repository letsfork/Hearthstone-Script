package club.xiaojiawei.listener;

import club.xiaojiawei.controller.JavaFXDashboardController;
import club.xiaojiawei.status.War;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * @author 肖嘉威
 * @date 2023/9/11 16:49
 * @msg 监听局数
 */
@Component
@Slf4j
public class WarCountListener {

    @Resource
    private JavaFXDashboardController javaFXDashboardController;

    @PostConstruct
    void init(){
        War.warCount.addListener((observable, oldValue, newValue) -> {
            log.info("已完成第 " + newValue + " 把游戏");
            javaFXDashboardController.getGameCount().setText(newValue.toString());
            javaFXDashboardController.getWinningPercentage().setText(String.format("%.0f", War.winCount.get() / newValue.doubleValue() * 100) + "%");
        });
    }
}
