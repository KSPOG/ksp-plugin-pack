package net.runelite.client.plugins.microbot.balancedattackstyle;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.EnumID;
import net.runelite.api.ParamID;
import net.runelite.api.Skill;
import net.runelite.api.VarPlayer;
import net.runelite.api.Varbits;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.globval.enums.InterfaceTab;
import net.runelite.client.plugins.microbot.util.combat.Rs2Combat;
import net.runelite.client.plugins.microbot.util.tabs.Rs2Tab;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
public class BalancedAttackStyleScript extends Script {

    private static class StyleSelection {
        private final WidgetInfo widgetInfo;
        private final AttackStyle attackStyle;

        private StyleSelection(WidgetInfo widgetInfo, AttackStyle attackStyle) {
            this.widgetInfo = widgetInfo;
            this.attackStyle = attackStyle;
        }
    }

    private enum AttackStyle {
        ACCURATE("Accurate", Skill.ATTACK),
        AGGRESSIVE("Aggressive", Skill.STRENGTH),
        DEFENSIVE("Defensive", Skill.DEFENCE),
        CONTROLLED("Controlled", Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE),
        RANGING("Ranging", Skill.RANGED),
        LONGRANGE("Longrange", Skill.RANGED, Skill.DEFENCE),
        CASTING("Casting", Skill.MAGIC),
        DEFENSIVE_CASTING("Defensive Casting", Skill.MAGIC, Skill.DEFENCE),
        OTHER("Other");

        private final String name;
        private final Skill[] skills;

        AttackStyle(String name, Skill... skills) {
            this.name = name;
            this.skills = skills;
        }

        public String getName() {
            return name;
        }

        public Skill[] getSkills() {
            return skills;
        }
    }

    private int switchIntervalSeconds;
    private long lastStyleSwitchTime = System.currentTimeMillis();

    public boolean run(BalancedAttackStyleConfig config) {
        switchIntervalSeconds = config.switchIntervalSeconds();
        lastStyleSwitchTime = System.currentTimeMillis();
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(
                () -> scheduledTask(config),
                0,
                1,
                TimeUnit.SECONDS
        );
        return true;
    }

    private void scheduledTask(BalancedAttackStyleConfig config) {
        try {
            if (!Microbot.isLoggedIn() || !super.run()) {
                return;
            }

            if (!isTimeForSwitch()) {
                return;
            }

            AttackStyle currentStyle = getCurrentAttackStyle();
            if (currentStyle == null || currentStyle == AttackStyle.OTHER) {
                return;
            }

            Skill desiredSkill = getDesiredSkill();
            if (desiredSkill == null) {
                return;
            }

            StyleSelection selection = getSelectionForSkill(desiredSkill, config.avoidControlled());
            if (selection == null) {
                log.debug("No matching attack style found for skill {}.", desiredSkill);
                return;
            }

            AttackStyle desiredStyle = selection.attackStyle;

            if (currentStyle == desiredStyle) {
                return;
            }

            lastStyleSwitchTime = System.currentTimeMillis();
            changeAttackStyle(selection.widgetInfo);
            log.info("Switched attack style from {} to {} to balance stats.", currentStyle.getName(), desiredStyle.getName());
        } catch (Exception ex) {
            Microbot.logStackTrace("BalancedAttackStyleScript", ex);
        }
    }

    private boolean isTimeForSwitch() {
        long elapsedTime = System.currentTimeMillis() - lastStyleSwitchTime;
        return elapsedTime >= switchIntervalSeconds * 1000L;
    }

    private Skill getDesiredSkill() {
        int attack = getLevel(Skill.ATTACK);
        int strength = getLevel(Skill.STRENGTH);
        int defence = getLevel(Skill.DEFENCE);

        int lowest = Math.min(attack, Math.min(strength, defence));
        Set<Skill> lowestSkills = EnumSet.noneOf(Skill.class);
        if (attack == lowest) {
            lowestSkills.add(Skill.ATTACK);
        }
        if (strength == lowest) {
            lowestSkills.add(Skill.STRENGTH);
        }
        if (defence == lowest) {
            lowestSkills.add(Skill.DEFENCE);
        }

        if (lowestSkills.isEmpty()) {
            return null;
        }

        if (lowestSkills.contains(Skill.ATTACK)) {
            return Skill.ATTACK;
        }
        if (lowestSkills.contains(Skill.STRENGTH)) {
            return Skill.STRENGTH;
        }
        return Skill.DEFENCE;
    }

    private int getLevel(Skill skill) {
        return Microbot.getClient().getRealSkillLevel(skill);
    }

    private AttackStyle getCurrentAttackStyle() {
        int attackStyleVarbit = Microbot.getVarbitPlayerValue(VarPlayer.ATTACK_STYLE);
        int equippedWeaponTypeVarbit = Microbot.getVarbitValue(Varbits.EQUIPPED_WEAPON_TYPE);
        int castingModeVarbit = Microbot.getVarbitValue(Varbits.DEFENSIVE_CASTING_MODE);

        AttackStyle[] attackStyles = getWeaponTypeStyles(equippedWeaponTypeVarbit);
        if (attackStyles.length == 0) {
            return null;
        }

        int styleIndex = attackStyleVarbit;
        if (styleIndex == 4) {
            styleIndex += castingModeVarbit;
        }

        if (styleIndex < 0 || styleIndex >= attackStyles.length) {
            return null;
        }

        AttackStyle style = attackStyles[styleIndex];
        return style == null ? AttackStyle.OTHER : style;
    }

    private StyleSelection getSelectionForSkill(Skill desiredSkill, boolean avoidControlled) {
        int equippedWeaponTypeVarbit = Microbot.getVarbitValue(Varbits.EQUIPPED_WEAPON_TYPE);
        AttackStyle[] attackStyles = getWeaponTypeStyles(equippedWeaponTypeVarbit);
        List<StyleSelection> selections = new ArrayList<>();

        for (int i = 0; i < attackStyles.length; i++) {
            AttackStyle style = attackStyles[i];
            if (style == null) {
                continue;
            }

            if (avoidControlled && isControlled(style)) {
                continue;
            }

            for (Skill skill : style.getSkills()) {
                if (skill == desiredSkill) {
                    WidgetInfo widget = widgetForIndex(i);
                    if (widget != null) {
                        selections.add(new StyleSelection(widget, style));
                    }
                    break;
                }
            }
        }

        if (selections.isEmpty()) {
            if (avoidControlled) {
                return getSelectionForSkill(desiredSkill, false);
            }
            return null;
        }

        return selections.get(0);
    }

    private WidgetInfo widgetForIndex(int index) {
        switch (index) {
            case 0:
                return WidgetInfo.COMBAT_STYLE_ONE;
            case 1:
                return WidgetInfo.COMBAT_STYLE_TWO;
            case 2:
                return WidgetInfo.COMBAT_STYLE_THREE;
            case 3:
                return WidgetInfo.COMBAT_STYLE_FOUR;
            case 4:
                return WidgetInfo.COMBAT_SPELLS;
            case 5:
                return WidgetInfo.COMBAT_DEFENSIVE_SPELL_BOX;
            default:
                return null;
        }
    }

    private boolean isControlled(AttackStyle style) {
        return style.getSkills().length > 1;
    }

    private AttackStyle[] getWeaponTypeStyles(int weaponType) {
        int weaponStyleEnum = Microbot.getEnum(EnumID.WEAPON_STYLES).getIntValue(weaponType);
        int[] weaponStyleStructs = Microbot.getEnum(weaponStyleEnum).getIntVals();

        AttackStyle[] styles = new AttackStyle[weaponStyleStructs.length];
        int i = 0;
        for (int style : weaponStyleStructs) {
            String attackStyleName = Microbot.getStructComposition(style).getStringValue(ParamID.ATTACK_STYLE_NAME);
            AttackStyle attackStyle = AttackStyle.valueOf(attackStyleName.toUpperCase());

            if (attackStyle == AttackStyle.OTHER) {
                ++i;
                continue;
            }

            if (i == 5 && attackStyle == AttackStyle.DEFENSIVE) {
                attackStyle = AttackStyle.DEFENSIVE_CASTING;
            }

            styles[i++] = attackStyle;
        }

        return styles;
    }

    private void changeAttackStyle(WidgetInfo attackStyleWidgetInfo) {
        if (Rs2Tab.getCurrentTab() != InterfaceTab.COMBAT) {
            Rs2Tab.switchToCombatOptionsTab();
            sleepUntil(() -> Rs2Tab.getCurrentTab() == InterfaceTab.COMBAT, 2000);
        }
        Rs2Combat.setAttackStyle(attackStyleWidgetInfo);
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }
}