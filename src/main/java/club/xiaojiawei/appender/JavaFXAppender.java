package club.xiaojiawei.appender;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import club.xiaojiawei.controller.JavaFXDashboardController;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.text.Text;


/**
 * @author 肖嘉威
 * @version 1.0
 * @date 2022/9/28 上午10:11
 * @msg 向GUI客户端发送消息
 */
public class JavaFXAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

    @Override
    protected void append(ILoggingEvent event) {
        if (JavaFXDashboardController.logSwitchBack.statusProperty().get() && JavaFXDashboardController.logVBoxBack != null && JavaFXDashboardController.accordionBack!= null){
            Platform.runLater(() -> {
                ObservableList<Node> list = JavaFXDashboardController.logVBoxBack.getChildren();
                Text text;
                if (event.getThrowableProxy() == null){
                    text = new Text(event.getMessage());
                }else {
                    text = new Text(event.getMessage() + "，查看脚本日志获取详细信息");
                }
                text.wrappingWidthProperty().bind(JavaFXDashboardController.accordionBack.widthProperty().subtract(15));
                list.add(text);
//                大于两百条就清空,防止内存泄露和性能问题
                if (list.size() > 200){
                    list.clear();
                }
            });
        }
    }


}