package club.xiaojiawei.strategy.mode;

import club.xiaojiawei.custom.LogRunnable;
import club.xiaojiawei.data.GameStaticData;
import club.xiaojiawei.data.ScriptStaticData;
import club.xiaojiawei.enums.ConfigurationKeyEnum;
import club.xiaojiawei.enums.DeckEnum;
import club.xiaojiawei.enums.ModeEnum;
import club.xiaojiawei.enums.RunModeEnum;
import club.xiaojiawei.listener.PowerFileListener;
import club.xiaojiawei.status.Mode;
import club.xiaojiawei.status.Work;
import club.xiaojiawei.strategy.AbstractModeStrategy;
import club.xiaojiawei.utils.RandomUtil;
import club.xiaojiawei.utils.SystemUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Properties;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static club.xiaojiawei.data.GameStaticData.CANCEL_MATCH_BUTTON_VERTICAL_TO_BOTTOM_RATION;


/**
 * @author 肖嘉威
 * @date 2022/11/25 12:39
 * 传统对战
 */
@Slf4j
@Component
public class TournamentAbstractModeStrategy extends AbstractModeStrategy<Object> {

    @Resource
    private Properties scriptProperties;
    @Resource
    private PowerFileListener powerFileListener;
    private static ScheduledFuture<?> scheduledFuture;
    private static ScheduledFuture<?> errorScheduledFuture;

    public static final float TOURNAMENT_MODE_BUTTON_VERTICAL_TO_BOTTOM_RATIO = (float) 0.7;
    public static final float FIRST_DECK_BUTTON_HORIZONTAL_TO_CENTER_RATIO = (float) (0.333);
    public static final float ERROR_BUTTON_VERTICAL_TO_BOTTOM_RATION = (float) 0.395;
    public static final float CHANGE_MODE_BUTTON_VERTICAL_TO_BOTTOM_RATION = (float) 0.963;
    public static final float CHANGE_MODE_BUTTON_HORIZONTAL_TO_CENTER_RATION = (float) 0.313;
    public static final float CLASSIC_BUTTON_VERTICAL_TO_BOTTOM_RATION = (float) 0.581;
    public static final float CLASSIC_BUTTON_HORIZONTAL_TO_CENTER_RATION = (float) 0.34;
    public static final float STANDARD_BUTTON_VERTICAL_TO_BOTTOM_RATION = (float) 0.714;
    public static final float STANDARD_BUTTON_HORIZONTAL_TO_CENTER_RATION = (float) 0.11;
    public static void cancelTask(){
        if (scheduledFuture != null && !scheduledFuture.isDone()){
            log.info("已取消点击天梯模式按钮任务");
            scheduledFuture.cancel(true);
        }
        if (errorScheduledFuture != null && !errorScheduledFuture.isDone()){
            log.info("已取消网络错误再次匹配任务");
            errorScheduledFuture.cancel(true);
        }
    }

    @Override
    public void wantEnter() {
        cancelTask();
        scheduledFuture = extraThreadPool.scheduleWithFixedDelay(new LogRunnable(() -> {
            if (isPause.get().get()){
                scheduledFuture.cancel(true);
            } else if (Mode.getCurrMode() == ModeEnum.HUB){
                SystemUtil.updateRect(ScriptStaticData.getGameHWND(), ScriptStaticData.GAME_RECT);
                SystemUtil.frontWindow(ScriptStaticData.getGameHWND());
                mouseUtil.leftButtonClick(
                        ((ScriptStaticData.GAME_RECT.right + ScriptStaticData.GAME_RECT.left) >> 1) + RandomUtil.getRandom(-15, 15),
                        (int) (ScriptStaticData.GAME_RECT.bottom - (ScriptStaticData.GAME_RECT.bottom - ScriptStaticData.GAME_RECT.top) * TOURNAMENT_MODE_BUTTON_VERTICAL_TO_BOTTOM_RATIO) + RandomUtil.getRandom(-5, 5)
                );
            }else if (Mode.getCurrMode() == ModeEnum.GAME_MODE){
                scheduledFuture.cancel(true);
                SystemUtil.frontWindow(ScriptStaticData.getGameHWND());
                SystemUtil.updateRect(ScriptStaticData.getGameHWND(), ScriptStaticData.GAME_RECT);
                gameUtil.clickBackButton();
            }else {
                scheduledFuture.cancel(true);
            }
        }), DELAY_TIME, INTERVAL_TIME, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void afterEnter(Object o) {
        if (Work.canWork()){
            SystemUtil.updateRect(ScriptStaticData.getGameHWND(), ScriptStaticData.GAME_RECT);
            SystemUtil.frontWindow(ScriptStaticData.getGameHWND());
            SystemUtil.delayMedium();
            if (ModeEnum.TOURNAMENT == RunModeEnum.valueOf(scriptProperties.getProperty(ConfigurationKeyEnum.RUN_MODE_KEY.getKey())).getModeEnum()){
                DeckEnum deck = DeckEnum.valueOf(scriptProperties.getProperty(ConfigurationKeyEnum.DECK_KEY.getKey()));
                if (!deck.getRunMode().isEnable()){
                    log.warn("不可用或不支持的模式：" + deck.getValue());
                    return;
                }
                SystemUtil.delayMedium();
                clickModeChangeButton();
                SystemUtil.delayMedium();
                changeMode(deck);
                SystemUtil.delayMedium();
                selectDeck(deck);
                SystemUtil.delayShort();
                startMatching();
            }else {
//            退出该界面
                SystemUtil.frontWindow(ScriptStaticData.getGameHWND());
                gameUtil.clickBackButton();
            }
        }else {
            Work.stopWork();
            Work.cannotWorkLog();
        }
    }

    private void clickModeChangeButton(){
        log.info("点击切换模式按钮");
        SystemUtil.frontWindow(ScriptStaticData.getGameHWND());
        mouseUtil.leftButtonClick(
                (int) (((ScriptStaticData.GAME_RECT.right + ScriptStaticData.GAME_RECT.left) >> 1) + (ScriptStaticData.GAME_RECT.bottom - ScriptStaticData.GAME_RECT.top) * CHANGE_MODE_BUTTON_HORIZONTAL_TO_CENTER_RATION * GameStaticData.GAME_WINDOW_ASPECT_TO_HEIGHT_RATIO + RandomUtil.getRandom(-15, -5)),
                (int) (ScriptStaticData.GAME_RECT.bottom - (ScriptStaticData.GAME_RECT.bottom - ScriptStaticData.GAME_RECT.top) * CHANGE_MODE_BUTTON_VERTICAL_TO_BOTTOM_RATION) + RandomUtil.getRandom(10, 20)
        );
    }

    private void changeMode(DeckEnum deck){
        SystemUtil.frontWindow(ScriptStaticData.getGameHWND());
        switch (deck.getDeckType()){
            case CLASSIC -> changeModeToClassic();
            case STANDARD -> changeModeToStandard();
            case WILD -> changeModeToWild();
            case CASUAL -> changeModeToCasual();
            default -> throw new RuntimeException("没有此模式：" + deck.getDeckType().getComment());
        }
    }

    private void selectDeck(DeckEnum deck){
        log.info("选择套牌:" + deck.getComment());
        SystemUtil.frontWindow(ScriptStaticData.getGameHWND());
        mouseUtil.leftButtonClick(
                (int) (((ScriptStaticData.GAME_RECT.right + ScriptStaticData.GAME_RECT.left) >> 1) - (ScriptStaticData.GAME_RECT.bottom - ScriptStaticData.GAME_RECT.top) * FIRST_DECK_BUTTON_HORIZONTAL_TO_CENTER_RATIO * GameStaticData.GAME_WINDOW_ASPECT_TO_HEIGHT_RATIO + RandomUtil.getRandom(-10, 10)),
                (int) (ScriptStaticData.GAME_RECT.bottom - (ScriptStaticData.GAME_RECT.bottom - ScriptStaticData.GAME_RECT.top) * GameStaticData.FIRST_ROW_DECK_VERTICAL_TO_BOTTOM_RATIO) + RandomUtil.getRandom(-5, 5)
        );
    }

    private void changeModeToClassic(){
        log.info("切换至经典模式");
        mouseUtil.leftButtonClick(
                (int) (((ScriptStaticData.GAME_RECT.right + ScriptStaticData.GAME_RECT.left) >> 1) - (ScriptStaticData.GAME_RECT.bottom - ScriptStaticData.GAME_RECT.top) * CLASSIC_BUTTON_HORIZONTAL_TO_CENTER_RATION * GameStaticData.GAME_WINDOW_ASPECT_TO_HEIGHT_RATIO + RandomUtil.getRandom(-5, 5)),
                (int) (ScriptStaticData.GAME_RECT.bottom - (ScriptStaticData.GAME_RECT.bottom - ScriptStaticData.GAME_RECT.top) * CLASSIC_BUTTON_VERTICAL_TO_BOTTOM_RATION) + RandomUtil.getRandom(-5, 5)
        );
    }

    private void changeModeToStandard(){
        log.info("切换至标准模式");
        mouseUtil.leftButtonClick(
                (int) (((ScriptStaticData.GAME_RECT.right + ScriptStaticData.GAME_RECT.left) >> 1) - (ScriptStaticData.GAME_RECT.bottom - ScriptStaticData.GAME_RECT.top) * STANDARD_BUTTON_HORIZONTAL_TO_CENTER_RATION * GameStaticData.GAME_WINDOW_ASPECT_TO_HEIGHT_RATIO + RandomUtil.getRandom(-5, 5)),
                (int) (ScriptStaticData.GAME_RECT.bottom - (ScriptStaticData.GAME_RECT.bottom - ScriptStaticData.GAME_RECT.top) * STANDARD_BUTTON_VERTICAL_TO_BOTTOM_RATION) + RandomUtil.getRandom(-5, 5)
        );
    }

    private void changeModeToWild(){
        log.info("切换至狂野模式");
        mouseUtil.leftButtonClick(
                (int) (((ScriptStaticData.GAME_RECT.right + ScriptStaticData.GAME_RECT.left) >> 1) + (ScriptStaticData.GAME_RECT.bottom - ScriptStaticData.GAME_RECT.top) * STANDARD_BUTTON_HORIZONTAL_TO_CENTER_RATION * GameStaticData.GAME_WINDOW_ASPECT_TO_HEIGHT_RATIO + RandomUtil.getRandom(-5, 5)),
                (int) (ScriptStaticData.GAME_RECT.bottom - (ScriptStaticData.GAME_RECT.bottom - ScriptStaticData.GAME_RECT.top) * STANDARD_BUTTON_VERTICAL_TO_BOTTOM_RATION) + RandomUtil.getRandom(-5, 5)
        );
    }

    private void changeModeToCasual(){
        log.info("切换至休闲模式");
        mouseUtil.leftButtonClick(
                (int) (((ScriptStaticData.GAME_RECT.right + ScriptStaticData.GAME_RECT.left) >> 1) + (ScriptStaticData.GAME_RECT.bottom - ScriptStaticData.GAME_RECT.top) * CLASSIC_BUTTON_HORIZONTAL_TO_CENTER_RATION * GameStaticData.GAME_WINDOW_ASPECT_TO_HEIGHT_RATIO + RandomUtil.getRandom(-5, 5)),
                (int) (ScriptStaticData.GAME_RECT.bottom - (ScriptStaticData.GAME_RECT.bottom - ScriptStaticData.GAME_RECT.top) * CLASSIC_BUTTON_VERTICAL_TO_BOTTOM_RATION) + RandomUtil.getRandom(-5, 5)
        );
    }

    private void startMatching(){
        log.info("开始匹配");
        powerFileListener.listen();
        SystemUtil.frontWindow(ScriptStaticData.getGameHWND());
        //        重置游戏
        mouseUtil.leftButtonClick(
                (int) (((ScriptStaticData.GAME_RECT.right + ScriptStaticData.GAME_RECT.left) >> 1) + (ScriptStaticData.GAME_RECT.bottom - ScriptStaticData.GAME_RECT.top) * GameStaticData.START_BUTTON_HORIZONTAL_TO_CENTER_RATIO * GameStaticData.GAME_WINDOW_ASPECT_TO_HEIGHT_RATIO + RandomUtil.getRandom(-10, 10)),
                (int) (ScriptStaticData.GAME_RECT.bottom - (ScriptStaticData.GAME_RECT.bottom - ScriptStaticData.GAME_RECT.top) * GameStaticData.START_BUTTON_VERTICAL_TO_BOTTOM_RATIO) + RandomUtil.getRandom(-10, 10)
        );
        generateTimer();
    }

    /**
     * 生成匹配失败时兜底的定时器
     */
    public void generateTimer(){
        errorScheduledFuture = extraThreadPool.schedule(new LogRunnable(() -> {
            if (!isPause.get().get()){
                log.info("游戏网络出现问题，匹配失败，再次匹配中");
                SystemUtil.notice("游戏网络出现问题，匹配失败，再次匹配中");
                SystemUtil.frontWindow(ScriptStaticData.getGameHWND());
                SystemUtil.updateRect(ScriptStaticData.getGameHWND(), ScriptStaticData.GAME_RECT);
//                点击取消匹配按钮
                mouseUtil.leftButtonClick(
                        ((ScriptStaticData.GAME_RECT.right + ScriptStaticData.GAME_RECT.left) >> 1) + RandomUtil.getRandom(-10, 10),
                        (int) (ScriptStaticData.GAME_RECT.bottom - (ScriptStaticData.GAME_RECT.bottom - ScriptStaticData.GAME_RECT.top) * CANCEL_MATCH_BUTTON_VERTICAL_TO_BOTTOM_RATION) + RandomUtil.getRandom(-5, 5)
                );
                SystemUtil.delayLong();
//                点击错误按钮
                mouseUtil.leftButtonClick(
                        ((ScriptStaticData.GAME_RECT.right + ScriptStaticData.GAME_RECT.left) >> 1) + RandomUtil.getRandom(-10, 10),
                        (int) (ScriptStaticData.GAME_RECT.bottom - (ScriptStaticData.GAME_RECT.bottom - ScriptStaticData.GAME_RECT.top) * ERROR_BUTTON_VERTICAL_TO_BOTTOM_RATION) + RandomUtil.getRandom(-5, 5)
                );
                SystemUtil.delayMedium();
                afterEnter(null);
            }
        }), 60, TimeUnit.SECONDS);
    }

}
