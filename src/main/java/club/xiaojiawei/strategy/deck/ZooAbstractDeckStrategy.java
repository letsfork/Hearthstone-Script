package club.xiaojiawei.strategy.deck;

import club.xiaojiawei.entity.Card;
import club.xiaojiawei.entity.Player;
import club.xiaojiawei.entity.area.PlayArea;
import club.xiaojiawei.enums.CardTypeEnum;
import club.xiaojiawei.status.War;
import club.xiaojiawei.strategy.AbstractDeckStrategy;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * @author 肖嘉威
 * @date 2022/11/29 17:36
 */
@Slf4j
@Component
@Deprecated
public class ZooAbstractDeckStrategy extends AbstractDeckStrategy{

    /**
     * Map.entry("VAN_EX1_308", new CardMes("灵魂之火", "VAN_EX1_308", 2)),
     *                 Map.entry("VAN_EX1_316", new CardMes("力量的代价", "VAN_EX1_316", 1)),
     *                 Map.entry("VAN_CS2_188", new CardMes("叫嚣的中士", "VAN_CS2_188", 10)),
     *                 Map.entry("VAN_EX1_004", new CardMes("年轻的女祭司", "VAN_EX1_004", 14)),
     *                 Map.entry("VAN_EX1_405", new CardMes("持盾卫士", "VAN_EX1_405", 12)),
     *                 Map.entry("VAN_EX1_319", new CardMes("烈焰小鬼", "VAN_EX1_319", 19)),
     *                 Map.entry("VAN_CS2_189", new CardMes("精灵弓箭手", "VAN_CS2_189", 11)),
     *                 Map.entry("VAN_CS2_065", new CardMes("虚空行者", "VAN_CS2_065", 16)),
     *                 Map.entry("VAN_EX1_008", new CardMes("银色侍从", "VAN_EX1_008", 15)),
     *                 Map.entry("VAN_EX1_162", new CardMes("恐狼前锋","VAN_EX1_162", 25)),
     *                 Map.entry("VAN_EX1_393", new CardMes("阿曼尼狂战士", "VAN_EX1_393", 28)),
     *                 Map.entry("VAN_NEW1_019", new CardMes("飞刀杂耍者", "VAN_NEW1_019", 29)),
     *                 Map.entry("VAN_EX1_019", new CardMes("破碎残阳祭司", "VAN_EX1_019", 33)),
     *                 Map.entry("VAN_EX1_093", new CardMes("阿古斯防御者", "VAN_EX1_093", 45)),
     *                 Map.entry("VAN_EX1_046", new CardMes("黑铁矮人", "VAN_EX1_046", 44)),
     *                 Map.entry("VAN_EX1_310", new CardMes("末日守卫", "VAN_EX1_310", 55)),
     *                 Map.entry("COIN", new CardMes("幸运币", "COIN", 0))
     */

    @Override
    public void executeChangeCard(List<Card> myHandCards, float clearance , float firstCardPos) {
        Card card;
        int oneCostCount = 0;
        for (int i = 0; i < myHandCards.size(); i++) {
            card = myHandCards.get(i);
//            只留费用小于等于2的
            if (card.getCost() > 2
//                    只留随从
                    || (card.getCardType() != CardTypeEnum.MINION && !Objects.equals(card.getCardId(), "COIN"))
//                    不留叫嚣
                    || Objects.equals(card.getCardId(), "VAN_CS2_188")
//                    不留持盾，如果没有恐狼或者飞刀
                    || (Objects.equals(card.getCardId(), "VAN_EX1_405") && findByCardId(myHandCards, "VAN_EX1_162") == -1 && findByCardId(myHandCards, "VAN_NEW1_019") == -1)
            ){
                clickFloatCard(clearance, firstCardPos, i);
            }
            if (card.getCost() == 1){
                oneCostCount++;
            }
        }
//        如果没有一张一费牌，则将恐狼换下去
        if (oneCostCount == 0){
            clickFloatCard(clearance, firstCardPos, findByCardId(myHandCards, "VAN_EX1_162"));
        }
    }

    PlayArea myPlayArea;
    PlayArea rivalPlayArea;
    private Player me = War.getMe();
    private Player rival = War.getRival();
    private List<Card> myHandCards;
    private List<Card> myPlayCards;
    private List<Card> rivalPlayCards;
    private int coinIndex;
    private int index1, index2;
    private boolean usedPower;

    @SneakyThrows
    @Override
    public void outCard() {
        this.me = me;
        this.rival = rival;
        myPlayCards = me.getPlayArea().getCards();
        usedPower = false;
        this.rivalPlayArea = rivalPlayArea;
        this.myPlayArea = myPlayArea;
        myHandCards = me.getHandArea().getCards();
        rivalPlayCards = rivalPlayArea.getCards();
        coinIndex = findByCardId(myHandCards, "COIN");
        log.info("我方本回合费用：" + me.getResources());
        log.info("我方手牌数量：" + myHandCards.size());
        log.info("我方手牌详情：" + myHandCards);
        log.info("我方战场数量：" + myPlayCards.size());
        log.info("我方战场详情：" + myPlayCards);
        log.info("对方生命值上限：" + rivalPlayArea.getHero().getHealth());
        log.info("对方护甲值：" + rivalPlayArea.getHero().getArmor());
        log.info("对方受到的总伤害：" + rivalPlayArea.getHero().getDamage());
//        解除我方随从疲劳
        for (Card playCard : myPlayCards) {
            playCard.setExhausted(false);
        }
        dealResource();
        boolean throughWall = true;
//        解嘲讽怪
        for (int i = rivalPlayCards.size() - 1; i >= 0; i--) {
            Card card = rivalPlayCards.get(i);
            if (card.isTaunt() && !card.isStealth()){
                List<Integer> result = calcFreeEatRivalTaunt(card);
                if (result == null){
//                    过墙失败
                    throughWall = false;
                    continue;
                }
                for (int j = result.size() - 2; j >= 0; j--) {
                    Integer integer = result.get(j);
                    myPlayPointToRivalPlay(integer, i);
                }
            }
        }
        dealResource();
//        算斩杀
        int allAtc = calcEnablePlayTotalAtc(myPlayCards);
        int blood = rivalPlayArea.getHero().getHealth() + rivalPlayArea.getHero().getArmor() - rivalPlayArea.getHero().getDamage();
        int soulFireCount = 0;
        if (throughWall){
            if (allAtc >= blood){
                //        怪全部打脸
                for (int i = myPlayCards.size() - 1; i >= 0; i--) {
                    Card card = myPlayCards.get(i);
                    if (!card.isExhausted() && !card.isFrozen() && card.getAtc() > 0){
                        myPlayPointToRivalHero(i);
                    }
                }
                return;
            }else {
                //        魂火数量
                for (Card myHandCard : myHandCards) {
                    if (Objects.equals(myHandCard.getCardId(), "VAN_EX1_308")){
                        soulFireCount++;
                    }
                }
                if (soulFireCount == 2 && myHandCards.size() == 2){
                    soulFireCount = 1;
                }
                if (soulFireCount * 4 + allAtc >= blood){
                    int index = 0;
                    while (--soulFireCount >= 0){
                        index = findByCardId(myHandCards, "VAN_EX1_308");
                        myHandPointToRivalHero(index);
                    }
                    if (index != -1){
                        //        怪全部打脸
                        for (int i = myPlayCards.size() - 1; i >= 0; i--) {
                            Card card = myPlayCards.get(i);
                            if (!card.isExhausted() && !card.isFrozen() && card.getAtc() > 0){
                                myPlayPointToRivalHero(i);
                            }
                        }
                        return;
                    }
                }
            }
            //        能走到这说明斩杀不了，按原计划进行
            //        解光环怪
            for (int i = rivalPlayCards.size() - 1; i >= 0; i--) {
                Card card = rivalPlayCards.get(i);
                if ((card.isAura() || card.isAdjacentBuff()) && !card.isStealth()){
                    List<Integer> result = calcFreeEatRivalTaunt(card);
                    if (result == null){
                        continue;
                    }
                    for (int j = result.size() - 2; j >= 0; j--) {
                        myPlayPointToRivalPlay(result.get(j), i);
                    }
                }
            }
            dealResource();
//        怪解场或打脸
            for (int i = myPlayCards.size() - 1; i >= 0; i--) {
                Card card = myPlayCards.get(i);
                if (!card.isExhausted() && !card.isFrozen() && card.getAtc() > 0){
                    int rivalIndex;
                    if (card.isAura()
                            || card.isAdjacentBuff()
                            || Objects.equals(card.getCardId(), "VAN_EX1_004")
                            || Objects.equals(card.getCardId(), "VAN_NEW1_019")
                    ){
                       rivalIndex = calcMyCardFreeEat(card, false);
                    }else {
                        rivalIndex = calcMyCardFreeEat(card, true);
                    }
                    if (rivalIndex != -1){
                        myPlayPointToRivalPlay(i, rivalIndex);
                    }else {
                        myPlayPointToRivalHero(i);
                    }
                }
            }
            dealResource();
        }
//        魂火解场
        while (soulFireCount > 0){
            double weight = 0;
            int index = -1;
            for (int i = rivalPlayCards.size() - 1; i >= 0; i--) {
                Card card = rivalPlayCards.get(i);
                double newWeight = (card.getHealth() - card.getDamage()) * HEALTH_WEIGHT + card.getAtc() * ATC_WEIGHT;
                float sub = 0;
                if (myHandCards.size() == 1){
                    sub = (float) 0.6;
                }else if (myHandCards.size() > 3){
                    sub = (float) 0.2;
                }
                if ((card.getHealth() - card.getDamage()) <= 4 && newWeight >= soulFireWeight - sub && newWeight > weight){
                    weight = newWeight;
                    index = i;
                }
            }
            if (index != -1){
                myHandPointToRivalPlay(findByCardId(myHandCards, "VAN_EX1_308"), index);
                soulFireCount--;
            }else {
                break;
            }
        }
    }

    private final static double soulFireWeight = 3.6;

    private void dealResource(){
        switch (getUsableResource(me)){
            case 1 -> dealOneResource();
            case 2 -> dealTwoResource();
            case 3 -> dealThreeResource();
            case 4 -> dealFourResource();
            case 5 -> dealFiveResource();
            case 6 -> dealSixResource();
            case 7 -> dealSevenResource();
            case 8 -> dealEightResource();
            case 9 -> dealNineResource();
            case 10 -> dealTenResource();
        }
    }

    /**
     * 判断能否使用技能
     * @return
     */
    private boolean canUsePower(){
        return  myPlayArea.getHero().getHealth() - myPlayArea.getHero().getDamage() - calcPlayTotalAtc(rivalPlayCards) > 3;
    }


    private void dealOneResource() {
        if (isMyTurn()){
            return;
        }
        if (getUsableResource(me) < 1){
            return;
        }
        if (!myPlayArea.isFull()){
//                    下烈焰小鬼或或小蓝胖或银色侍从或女祭司
            if ((index1 = findByCardId(myHandCards, "VAN_EX1_319")) != -1
                    || (index1 = findByCardId(myHandCards, "VAN_CS2_065")) != -1
                    || (index1 = findByCardId(myHandCards, "VAN_EX1_008")) != -1
                    || (index1 = findByCardId(myHandCards, "VAN_EX1_004")) != -1
            ){
                if (myPlayCards.size() > 0 && Objects.equals(myPlayCards.get(0).getCardId(), "VAN_EX1_162")){
                    myHandPointToMyPlay(index1, 0);
                }else {
                    myHandPointToMyPlay(index1, myPlayCards.size());
                }
//                        有精灵弓箭手且对方场上有一血怪
            }else if ((index1 = findByCardId(myHandCards, "VAN_CS2_189")) != -1){
                if ((index2 = findMaxAtcByBlood(rivalPlayCards, 1)) != -1
                        && !rivalPlayCards.get(index2).isStealth()
                        && rivalPlayArea.getHero().getHealth() + rivalPlayArea.getHero().getArmor() - rivalPlayArea.getHero().getDamage() > 1
                ){
                    myHandPointToMyPlayThenPointToRivalPlay(index1, myPlayCards.size(), index2);
                }else {
                    myHandPointToMyPlayThenPointToRivalHero(index1, myPlayCards.size());
                }
//                        持盾卫士
            }else if ((index1 = findByCardId(myHandCards, "VAN_EX1_405")) != -1){
                if (myPlayCards.size() > 0 && Objects.equals(myPlayCards.get(0).getCardId(), "VAN_EX1_162")){
                    myHandPointToMyPlay(index1, 0);
                }else {
                    myHandPointToMyPlay(index1, myPlayCards.size());
                }
//                有能动的怪则下叫嚣
            }else if ((index1 = findByCardId(myHandCards, "VAN_CS2_188")) != -1 && (index2 = findCanMove(myPlayCards)) != -1){
                myHandPointToMyPlayThenPointToMyPlay(index1, myPlayCards.size(), index2);
//                        都没有则考虑两费
            }else if (coinIndex != -1
                    && ((index1 = findByCardId(myHandCards, "VAN_EX1_393")) != -1//阿曼尼
                    || (index1 = findByCardId(myHandCards, "VAN_NEW1_019")) != -1//飞刀
                    || (index1 = findByCardId(myHandCards, "VAN_EX1_162")) != -1)//恐狼
            ){
                coinIndex = findByCardId(myHandCards, "COIN");
                myHandPointToMyPlay(coinIndex);
                coinIndex = -1;
                dealTwoResource();
            }
            if (!myPlayArea.isFull()){
                if (coinIndex != -1
                        && (existByCost(myHandCards, 2) || !existByCost(myHandCards, 3))
                        && ((index1 = findByCardId(myHandCards, "VAN_EX1_319")) != -1
                        || (index1 = findByCardId(myHandCards, "VAN_CS2_065")) != -1
                        || (index1 = findByCardId(myHandCards, "VAN_EX1_004")) != -1
                        || (index1 = findByCardId(myHandCards, "VAN_EX1_008")) != -1)
                        || (index1 = findByCardId(myHandCards, "VAN_CS2_189")) != -1
                ){
                    coinIndex = findByCardId(myHandCards, "COIN");
                    myHandPointToMyPlay(coinIndex);
                    coinIndex = -1;
                    dealOneResource();
                }
            }
        }
    }
    private void dealTwoResource() {
        if (isMyTurn()){
            return;
        }
        if (getUsableResource(me) < 2){
            dealOneResource();
            return;
        }
        if (!myPlayArea.isFull()){
            if ((index1 = findByCardId(myHandCards, "VAN_NEW1_019")) != -1//飞刀
                    || (index1 = findByCardId(myHandCards, "VAN_EX1_393")) != -1//阿曼尼
            ){
                if (myPlayCards.size() > 0 && Objects.equals(myPlayCards.get(0).getCardId(), "VAN_EX1_162")){
                    myHandPointToMyPlay(index1, 0);
                }else {
                    myHandPointToMyPlay(index1, myPlayCards.size());
                }
            }else if ((index1 = findByCardId(myHandCards, "VAN_EX1_162")) != -1){//恐狼
                if (myPlayCards.size() > 1){
                    int tauntIndex = 0;
                    for (int i = 0; i < myPlayCards.size(); i++) {
                        if (myPlayCards.get(i).isTaunt()){
                            if (tauntIndex == 0){
                                tauntIndex = i;
                            }else if (Objects.equals(myPlayCards.get(i).getCardId(), "VAN_EX1_405")){//持盾卫士
                                tauntIndex = i;
                            }
                        }
                    }
                    myHandPointToMyPlay(index1, tauntIndex == 0? 1 : tauntIndex);
                }else {
                    myHandPointToMyPlay(index1, 1);
                }
            }
            dealOneResource();
            dealOneResource();
        }
        if (canUsePower() && getUsableResource(me) >= 2 && !usedPower){
            usedPower = true;
            clickMyPower();
        }
    }
    private void dealThreeResource(){
        if(isMyTurn()){
            return;
        }
        if (getUsableResource(me) < 3){
            dealTwoResource();
            return;
        }
        if (!myPlayArea.isFull()){
//                    祭司
            if ((index1 = findByCardId(myHandCards, "VAN_EX1_019")) != -1 && myPlayCards.size() > 0){
                index2 = -1;
                for (int i = 0; i < myPlayCards.size(); i++) {
                    Card card = myPlayCards.get(i);
                    if (!card.isExhausted() && !card.isFrozen()){
                        index2 = i;
                        myHandPointToMyPlayThenPointToMyPlay(index1, myPlayCards.size(), i);
                        break;
                    }
                }
                if (index2 == -1){
                    for (int i = 0; i < myPlayCards.size(); i++) {
                        Card card = myPlayCards.get(i);
                        if (card.isTaunt() || card.isDivineShield()){
                            index2 = i;
                            myHandPointToMyPlayThenPointToMyPlay(index1, myPlayCards.size(), i);
                            break;
                        }
                    }
                }
                if (index2 == -1
                        && ((index2 = findByCardId(myPlayCards, "VAN_NEW1_019")) != -1//飞刀
                        || (index2 = findByCardId(myPlayCards, "VAN_EX1_162")) != -1)//恐狼
                ){
                    if (myPlayCards.size() > 0 && Objects.equals(myPlayCards.get(0).getCardId(), "VAN_EX1_162")){
                        myHandPointToMyPlayThenPointToMyPlay(index1, 0, index2);
                    }else {
                        myHandPointToMyPlayThenPointToMyPlay(index1, myPlayCards.size(), index2);
                    }
                }
                if (index2 == -1){
                    if (myPlayCards.size() > 0 && Objects.equals(myPlayCards.get(0).getCardId(), "VAN_EX1_162")){
                        myHandPointToMyPlayThenPointToMyPlay(index1, 0, 0);
                    }else {
                        myHandPointToMyPlayThenPointToMyPlay(index1, myPlayCards.size(), 0);
                    }
                }
            }else if (coinIndex != -1){
//            有幸运币和阿古斯或黑铁
                if (myPlayCards.size() > 1 &&  (index1 = findByCardId(myHandCards, "VAN_EX1_093")) != -1
                        || (index1 = findByCardId(myHandCards, "VAN_EX1_046")) != -1
                ){
                    coinIndex = findByCardId(myHandCards, "COIN");
                    myHandPointToMyPlay(coinIndex);
                    coinIndex = -1;
                    dealFourResource();
                }
            }
            dealTwoResource();
            dealOneResource();
        }
        if (canUsePower() && getUsableResource(me) >= 2 && !usedPower){
            usedPower = true;
            clickMyPower();
            dealOneResource();
        }
    }
    private void dealFourResource(){
        if(isMyTurn()){
            return;
        }
        if (getUsableResource(me) < 4){
            dealThreeResource();
            return;
        }
        if (!myPlayArea.isFull()){
//        阿古斯
            if (myPlayCards.size() > 1 && (index1 = findByCardId(myHandCards, "VAN_EX1_093")) != -1){
                index2 = 1;
                for (int i = 0; i < myPlayCards.size(); i++) {
                    Card card = myPlayCards.get(i);
                    if (card.isTaunt() || card.isDivineShield()){
                        if (i != 0){
                            index2 = i;
                        }
                    }
                }
                myHandPointToMyPlay(index1, index2);
//            黑铁
            }else if ((index1 = findByCardId(myHandCards, "VAN_EX1_046")) != -1){
                boolean flag = false;
                for (int i = 0; i < myPlayCards.size(); i++) {
                    Card card = myPlayCards.get(i);
                    if (!card.isExhausted() && !card.isFrozen()){
                        flag = true;
                        myHandPointToMyPlayThenPointToMyPlay(index1, myPlayCards.size(), i);
                        break;
                    }
                }
                if (!flag){
                    if (myPlayCards.size() == 0){
                        if (rivalPlayArea.size() == 0){
                            myHandPointToMyPlay(index1, myPlayCards.size());
                        }else {
                            int index = -1;
                            for (int i = 0; i < rivalPlayCards.size(); i++) {
                                if (!rivalPlayCards.get(i).isStealth()){
                                    index = i;
                                    break;
                                }
                            }
                            if (index == -1){
                                myHandPointToMyPlay(index1, myPlayCards.size());
                            }else {
                                myHandPointToMyPlayThenPointToRivalPlay(index1, myPlayCards.size(), index);
                            }
                        }
                    }else {
                        if (Objects.equals(myPlayCards.get(0).getCardId(), "VAN_EX1_162")){
                            myHandPointToMyPlayThenPointToMyPlay(index1, 0, 0);
                        }else {
                            myHandPointToMyPlayThenPointToMyPlay(index1, myPlayCards.size(), 0);
                        }
                    }
                }
            }
            if (findByCardId(myHandCards, "VAN_NEW1_019") == -1){
                dealThreeResource();
                dealTwoResource();
            }else {
                dealTwoResource();
                dealThreeResource();
            }
            dealOneResource();
        }
        if (canUsePower() && getUsableResource(me) >= 2 && !usedPower){
            usedPower = true;
            clickMyPower();
            dealTwoResource();
        }
    }
    private void dealFiveResource(){
        if(isMyTurn()){
            return;
        }
        if (getUsableResource(me) < 5){
            dealFourResource();
            return;
        }
        if (!myPlayArea.isFull()){
            int blood = rivalPlayArea.getHero().getHealth() + rivalPlayArea.getHero().getArmor() - rivalPlayArea.getHero().getDamage();
            int myPlayAtc = calcEnablePlayTotalAtc(myPlayCards);
//        57大哥
            if ((index1 = findByCardId(myHandCards, "VAN_EX1_310")) != -1 && (myHandCards.size() <= 2
                    || blood - myPlayAtc <= 10 || myHandCards.size() > 4)
            ){
                int i = -1;
//            恐狼
                if ((index2 = findByCardId(myPlayCards, "VAN_EX1_162")) != -1){
                    if (index2 + 1 < myPlayCards.size()){
                        i = index2 + 1;
                        Card rightCards = myPlayCards.get(i);
                        if (!rightCards.isExhausted() && !rightCards.isFrozen()){
                            if (findTauntCard(rivalPlayCards) == -1){
                                myPlayPointToRivalHero(i);
                            }else {
                                i = i == 1? 0 : i + 1;
                            }
                        }
                    }
                }
                myHandPointToMyPlay(index1, i == -1? myPlayCards.size() : i);
            }
            dealFourResource();
            dealThreeResource();
            dealTwoResource();
            dealOneResource();
        }
        if (canUsePower() && getUsableResource(me) >= 2 && !usedPower){
            usedPower = true;
            clickMyPower();
            dealThreeResource();
        }
    }
    private void dealSixResource(){
        if(isMyTurn()){
            return;
        }
        if (getUsableResource(me) < 6){
            dealFiveResource();
            return;
        }
        if (!myPlayArea.isFull()){
            dealOneResource();
            dealFiveResource();
            dealTwoResource();
            dealFourResource();
            dealThreeResource();
        }
        if (canUsePower() && getUsableResource(me) >= 2 && !usedPower){
            usedPower = true;
            clickMyPower();
            dealFourResource();
        }
    }
    private void dealSevenResource(){
        if(isMyTurn()){
            return;
        }
        if (getUsableResource(me) < 7){
            dealSixResource();
            return;
        }
        if (!myPlayArea.isFull()){
            dealOneResource();
            dealSixResource();
            dealFiveResource();
            dealFourResource();
            dealThreeResource();
            dealTwoResource();
        }
        if (canUsePower() && getUsableResource(me) >= 2 && !usedPower){
            usedPower = true;
            clickMyPower();
            dealFiveResource();
        }
    }
    private void dealEightResource(){
        if(isMyTurn()){
            return;
        }
        if (getUsableResource(me) < 7){
            dealSevenResource();
            return;
        }
        if (!myPlayArea.isFull()){
            dealOneResource();
            dealSevenResource();
            dealFiveResource();
            dealFourResource();
            dealThreeResource();
            dealTwoResource();
        }
        if (canUsePower() && getUsableResource(me) >= 2 && !usedPower){
            usedPower = true;
            clickMyPower();
            dealSixResource();
        }
    }
    private void dealNineResource(){
        if(isMyTurn()){
            return;
        }
        if (getUsableResource(me) < 9){
            dealEightResource();
            return;
        }
        if (!myPlayArea.isFull()){
            dealOneResource();
            dealEightResource();
            dealFiveResource();
            dealFourResource();
            dealThreeResource();
            dealTwoResource();
        }
        if (canUsePower() && getUsableResource(me) >= 2 && !usedPower){
            usedPower = true;
            clickMyPower();
            dealSevenResource();
        }
    }
    private void dealTenResource(){
        if (!myPlayArea.isFull()){
            dealOneResource();
            dealNineResource();
            dealFiveResource();
            dealFourResource();
            dealThreeResource();
            dealTwoResource();
        }
        if (canUsePower() && getUsableResource(me) >= 2 && !usedPower){
            usedPower = true;
            clickMyPower();
            dealEightResource();
        }
    }
}
