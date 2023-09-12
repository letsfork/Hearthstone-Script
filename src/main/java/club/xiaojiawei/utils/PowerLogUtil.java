package club.xiaojiawei.utils;

import club.xiaojiawei.data.GameStaticData;
import club.xiaojiawei.data.ScriptStaticData;
import club.xiaojiawei.entity.*;
import club.xiaojiawei.entity.area.Area;
import club.xiaojiawei.status.War;
import club.xiaojiawei.strategy.phase.GameTurnAbstractPhaseStrategy;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

import static club.xiaojiawei.data.GameStaticData.CARD_AREA_MAP;
import static club.xiaojiawei.enums.TagEnum.*;

/**
 * 解析power.log日志的工具，非常非常非常重要
 * @author 肖嘉威
 * @date 2022/11/28 23:12
 */
@Slf4j
public class PowerLogUtil {

    /**
     * 更新entity
     * @param l
     * @param accessFile
     * @return
     */
    public static ExtraEntity dealShowEntity(String l, RandomAccessFile accessFile){
        ExtraEntity extraEntity = parseExtraEntity(l, accessFile);
        Card card;
        if (extraEntity.getZone() == extraEntity.getExtraCard().getZone() || extraEntity.getExtraCard().getZone() == null){
            card = CARD_AREA_MAP.get(extraEntity.getEntityId()).getByEntityId(extraEntity.getEntityId());
        }else {
            card = War.exchangeAreaOfCard(extraEntity);
        }
        if (card != null){
            card.byExtraEntityUpdate(extraEntity);
        }
        return extraEntity;
    }

    /**
     * 生成entity
     * @param l
     * @param accessFile
     */
    public static Card dealFullEntity(String l, RandomAccessFile accessFile){
        ExtraEntity extraEntity = parseExtraEntity(l, accessFile);
        Card card;
        Area area;
        if ((area = CARD_AREA_MAP.get(extraEntity.getEntityId())) != null){
            card = area.getByEntityId(extraEntity.getEntityId());
            log.warn("dealFullEntity中发现entityId重复，将不会生成新Card");
        }else {
            card = new Card();
            card.byExtraEntityUpdate(extraEntity);
            area = War.getPlayer(extraEntity.getPlayerId()).getArea(extraEntity.getExtraCard().getZone());
            area.add(card, extraEntity.getExtraCard().getZonePos());
        }
        return card;
    }

    public static boolean dealTagChange(TagChangeEntity tagChangeEntity){
        if (tagChangeEntity.getTag() == UNKNOWN){
            return false;
        }
//        处理复杂
        if (tagChangeEntity.getEntity() == null){
            Player player = War.getPlayer(tagChangeEntity.getPlayerId());
            Area area = CARD_AREA_MAP.get(tagChangeEntity.getEntityId());
            if (area == null){
                return false;
            }
            Card card = area.getByEntityId(tagChangeEntity.getEntityId());
            if (card == null){
                return false;
            }
//            只列出可能被修改的属性
            switch (tagChangeEntity.getTag()){
                case ZONE_POSITION -> {
                    card = area.removeByEntityId(tagChangeEntity.getEntityId());
                    area.add(card, Integer.parseInt(tagChangeEntity.getValue()));
                }
                case ZONE -> player.getArea(GameStaticData.ZONE_MAP.get(tagChangeEntity.getValue())).add(card, 0);
                case HEALTH -> card.setHealth(Integer.parseInt(tagChangeEntity.getValue()));
                case ATK -> card.setAtc(Integer.parseInt(tagChangeEntity.getValue()));
                case COST -> card.setCost(Integer.parseInt(tagChangeEntity.getValue()));
                case FREEZE -> card.setFrozen(Objects.equals(tagChangeEntity.getValue(), "1"));
                case EXHAUSTED -> card.setExhausted(Objects.equals(tagChangeEntity.getValue(), "1"));
                case DAMAGE -> card.setDamage(Integer.parseInt(tagChangeEntity.getValue()));
                case TAUNT -> card.setTaunt(Objects.equals(tagChangeEntity.getValue(), "1"));
                case ARMOR -> card.setArmor(Integer.parseInt(tagChangeEntity.getValue()));
                case DIVINE_SHIELD -> card.setDivineShield(Objects.equals(tagChangeEntity.getValue(), "1"));
                case POISONOUS -> card.setPoisonous(Objects.equals(tagChangeEntity.getValue(), "1"));
                case DEATHRATTLE -> card.setDeathRattle(Objects.equals(tagChangeEntity.getValue(), "1"));
                case AURA -> card.setAura(Objects.equals(tagChangeEntity.getValue(), "1"));
                case STEALTH -> card.setStealth(Objects.equals(tagChangeEntity.getValue(), "1"));
                case WINDFURY -> card.setWindFury(Objects.equals(tagChangeEntity.getValue(), "1"));
                case CANT_BE_TARGETED_BY_SPELLS -> card.setCantBeTargetedBySpells(Objects.equals(tagChangeEntity.getValue(), "1"));
                case CANT_BE_TARGETED_BY_HERO_POWERS -> card.setCantBeTargetedByHeroPowers(Objects.equals(tagChangeEntity.getValue(), "1"));
                case SPAWN_TIME_COUNT -> card.setSpawnTimeCount(Objects.equals(tagChangeEntity.getValue(), "1"));
                case DORMANT_AWAKEN_CONDITION_ENCHANT -> card.setDormantAwakenConditionEnchant(Objects.equals(tagChangeEntity.getValue(), "1"));
                case IMMUNE -> card.setImmune(Objects.equals(tagChangeEntity.getValue(), "1"));
                default -> {
                    return false;
                }
            }
        }else {
//            处理简单
            switch (tagChangeEntity.getTag()){
                case RESOURCES_USED -> GameTurnAbstractPhaseStrategy.getCurrentPlayer().setUsedResources(Integer.parseInt(tagChangeEntity.getValue()));
                case RESOURCES -> GameTurnAbstractPhaseStrategy.getCurrentPlayer().setResources(Integer.parseInt(tagChangeEntity.getValue()));
                case TEMP_RESOURCES -> GameTurnAbstractPhaseStrategy.getCurrentPlayer().setTempResources(Integer.parseInt(tagChangeEntity.getValue()));
                case PLAYSTATE -> {
                    String gameId = new String(tagChangeEntity.getEntity().getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
                    if (Objects.equals(tagChangeEntity.getValue(), GameStaticData.WON)){
                        log.info("本局游戏胜者：" + gameId);
                        if (Objects.equals(gameId, War.getMe().getGameId())){
                            War.winCount.incrementAndGet();
                        }
                    }else if (Objects.equals(tagChangeEntity.getValue(), GameStaticData.LOST)){
                        log.info("本局游戏败者：" + gameId);
                    }else if (Objects.equals(tagChangeEntity.getValue(), GameStaticData.CONCEDED)){
                        log.info("本局游戏投降者：" + gameId);
                    }
                }
                default -> {
                    return false;
                }
            }
        }
        return true;
    }

    public static TagChangeEntity parseTagChange(String l){
        int tagIndex = l.lastIndexOf("tag");
        int valueIndex = l.lastIndexOf(GameStaticData.VALUE);
        int index = l.lastIndexOf("]");
        TagChangeEntity tagChangeEntity = new TagChangeEntity();
//        为什么不用TagEnum.valueOf()?因为可能会报错
        tagChangeEntity.setTag(GameStaticData.TAG_MAP.getOrDefault(l.substring(tagIndex + 4, valueIndex).strip(), UNKNOWN));
        tagChangeEntity.setValue(l.substring(valueIndex + 6).strip());
        if (index < 50){
            tagChangeEntity.setEntity(l.substring(l.indexOf("Entity") + 7, tagIndex).strip());
        }else {
            parseCommonEntity(tagChangeEntity, l);
        }
        return tagChangeEntity;
    }

    public static Block parseBlock(String l){
        int index = l.indexOf("EffectCardId");
        int entityIndex = l.indexOf("Entity");
        CommonEntity commonEntity = new CommonEntity();
        commonEntity.setEntity(l.substring(entityIndex + 7, index).strip());
        Block block = new Block();
        block.setEntity(commonEntity);
        block.setBlockType(GameStaticData.BLOCK_TYPE_MAP.get(l.substring(l.lastIndexOf("BlockType") + 10, l.lastIndexOf("Entity")).strip()));
        return block;
    }

    private static final boolean[] SIGH = new boolean[23];

    /**
     * 处理只有tag和value的日志
     * 如：tag=ZONE value=DECK
     * @param l
     * @param accessFile
     * @return
     */
    @SneakyThrows(value = {IOException.class})
    public static ExtraEntity parseExtraEntity(String l, RandomAccessFile accessFile){
        ExtraEntity extraEntity = new ExtraEntity();
        parseCommonEntity(extraEntity, l);
        long mark = accessFile.getFilePointer();
        while (true){
            if ((l = accessFile.readLine()) == null){
                ScriptStaticData.ROBOT.delay(1000);
            }else if (l.lastIndexOf("    tag=") == -1){
                accessFile.seek(mark);
                Arrays.fill(SIGH, false);
                break;
            }else if (!SIGH[0] && l.lastIndexOf("CARDTYPE") != -1){
                extraEntity.getExtraCard().setCardType(GameStaticData.CARD_TYPE_MAP.get(parseValue(l)));
                SIGH[0] = true;
            }else if (!SIGH[1] && l.lastIndexOf(COST.getValue()) != -1){
                extraEntity.getExtraCard().setCost(Integer.parseInt(parseValue(l)));
                SIGH[1] = true;
            }else if (!SIGH[2] && l.lastIndexOf(ATK.getValue()) != -1){
                extraEntity.getExtraCard().setAtc(Integer.parseInt(parseValue(l)));
                SIGH[2] = true;
            }else if (!SIGH[3] && l.lastIndexOf(HEALTH.getValue()) != -1){
                extraEntity.getExtraCard().setHealth(Integer.parseInt(parseValue(l)));
                SIGH[3] = true;
            }else if (!SIGH[4] && l.lastIndexOf(ZONE.getValue()) != -1){
                extraEntity.getExtraCard().setZone(GameStaticData.ZONE_MAP.get(parseValue(l)));
                SIGH[4] = true;
            }else if (!SIGH[5] && l.lastIndexOf(ZONE_POSITION.getValue()) != -1){
                extraEntity.getExtraCard().setZonePos(Integer.parseInt(parseValue(l)));
                SIGH[5] = true;
            }else if (!SIGH[6] && l.lastIndexOf(ADJACENT_BUFF.getValue()) != -1){
                extraEntity.getExtraCard().setAdjacentBuff("1".equals(parseValue(l)));
                SIGH[6] = true;
            }else if (!SIGH[7] && l.lastIndexOf(POISONOUS.getValue()) != -1){
                extraEntity.getExtraCard().setPoisonous("1".equals(parseValue(l)));
                SIGH[7] = true;
            }else if (!SIGH[8] && l.lastIndexOf(DEATHRATTLE.getValue()) != -1){
                extraEntity.getExtraCard().setDeathRattle("1".equals(parseValue(l)));
                SIGH[8] = true;
            }else if (!SIGH[9] && l.lastIndexOf(EXHAUSTED.getValue()) != -1){
                extraEntity.getExtraCard().setExhausted("1".equals(parseValue(l)));
                SIGH[9] = true;
            }else if (!SIGH[10] && l.lastIndexOf(FREEZE.getValue()) != -1){
                extraEntity.getExtraCard().setFrozen("1".equals(parseValue(l)));
                SIGH[10] = true;
            }else if (!SIGH[11] && l.lastIndexOf(TAUNT.getValue()) != -1){
                extraEntity.getExtraCard().setTaunt("1".equals(parseValue(l)));
                SIGH[11] = true;
            }else if (!SIGH[12] && l.lastIndexOf(ARMOR.getValue()) != -1){
                extraEntity.getExtraCard().setArmor(Integer.parseInt(parseValue(l)));
                SIGH[12] = true;
            }else if (!SIGH[13] && l.lastIndexOf(DIVINE_SHIELD.getValue()) != -1){
                extraEntity.getExtraCard().setDivineShield("1".equals(parseValue(l)));
                SIGH[13] = true;
            }else if (!SIGH[14] && l.lastIndexOf(AURA.getValue()) != -1){
                extraEntity.getExtraCard().setAura("1".equals(parseValue(l)));
                SIGH[14] = true;
            }else if (!SIGH[15] && l.lastIndexOf(STEALTH.getValue()) != -1){
                extraEntity.getExtraCard().setStealth("1".equals(parseValue(l)));
                SIGH[15] = true;
            }else if (!SIGH[16] && l.lastIndexOf(WINDFURY.getValue()) != -1){
                extraEntity.getExtraCard().setWindFury("1".equals(parseValue(l)));
                SIGH[16] = true;
            }else if (!SIGH[17] && l.lastIndexOf(BATTLECRY.getValue()) != -1){
                extraEntity.getExtraCard().setBattlecry("1".equals(parseValue(l)));
                SIGH[17] = true;
            }else if (!SIGH[18] && l.lastIndexOf(CANT_BE_TARGETED_BY_SPELLS.getValue()) != -1){
                extraEntity.getExtraCard().setCantBeTargetedBySpells("1".equals(parseValue(l)));
                SIGH[18] = true;
            }else if (!SIGH[19] && l.lastIndexOf(CANT_BE_TARGETED_BY_HERO_POWERS.getValue()) != -1){
                extraEntity.getExtraCard().setCantBeTargetedByHeroPowers("1".equals(parseValue(l)));
                SIGH[19] = true;
            }else if (!SIGH[20] && l.lastIndexOf(SPAWN_TIME_COUNT.getValue()) != -1){
                extraEntity.getExtraCard().setSpawnTimeCount("1".equals(parseValue(l)));
                SIGH[20] = true;
            }else if (!SIGH[21] && l.lastIndexOf(DORMANT_AWAKEN_CONDITION_ENCHANT.getValue()) != -1){
                extraEntity.getExtraCard().setDormantAwakenConditionEnchant("1".equals(parseValue(l)));
                SIGH[21] = true;
            }else if (!SIGH[22] && l.lastIndexOf(IMMUNE.getValue()) != -1){
                extraEntity.getExtraCard().setImmune("1".equals(parseValue(l)));
                SIGH[22] = true;
            }
            mark = accessFile.getFilePointer();
        }
        return extraEntity;
    }

    private static void parseCommonEntity(CommonEntity commonEntity, String l) {
        int index = l.lastIndexOf("]");
        int playerIndex = l.lastIndexOf("player", index);
        int cardIdIndex = l.lastIndexOf("cardId", playerIndex);
        int zonePosIndex = l.lastIndexOf("zonePos", cardIdIndex);
        int zoneIndex = l.lastIndexOf("zone=", zonePosIndex);
        int entityIdIndex = l.lastIndexOf("id", zoneIndex);
        int entityNameIndex = l.lastIndexOf("entityName", entityIdIndex);
        commonEntity.setCardId(l.substring(cardIdIndex + 7, playerIndex).strip());
        if (Strings.isBlank(commonEntity.getCardId())) {
            //noinspection AlibabaLowerCamelCaseVariableNaming
            int cardIDIndex = l.lastIndexOf("CardID");
            if (cardIDIndex != -1) {
                commonEntity.setCardId(l.substring(cardIDIndex + 7).strip());
            }
        }
        commonEntity.setPlayerId(l.substring(playerIndex + 7, index).strip());
        commonEntity.setZone(GameStaticData.ZONE_MAP.get(l.substring(zoneIndex + 5, zonePosIndex).strip()));
        commonEntity.setZonePos(Integer.parseInt(l.substring(zonePosIndex + 8, cardIdIndex).strip()));
        commonEntity.setEntityId(l.substring(entityIdIndex + 3, zoneIndex).strip());
        commonEntity.setEntityName(l.substring(entityNameIndex + 11, entityIdIndex).strip());
    }
    public static String parseValue(String l){
        return l.substring(l.lastIndexOf(GameStaticData.VALUE) + 6).strip();
    }
}
