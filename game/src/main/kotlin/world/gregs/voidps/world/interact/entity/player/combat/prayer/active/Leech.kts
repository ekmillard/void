package world.gregs.voidps.world.interact.entity.player.combat.prayer.active

import net.pearx.kasechange.toTitleCase
import world.gregs.voidps.engine.client.message
import world.gregs.voidps.engine.client.variable.variableSet
import world.gregs.voidps.engine.entity.character.Character
import world.gregs.voidps.engine.entity.character.player.Player
import world.gregs.voidps.engine.entity.character.player.chat.ChatType
import world.gregs.voidps.engine.entity.character.player.skill.Skill
import world.gregs.voidps.engine.entity.character.setAnimation
import world.gregs.voidps.engine.entity.character.setGraphic
import world.gregs.voidps.engine.entity.distanceTo
import world.gregs.voidps.engine.event.Priority
import world.gregs.voidps.engine.queue.queue
import world.gregs.voidps.engine.timer.timerStart
import world.gregs.voidps.engine.timer.timerTick
import world.gregs.voidps.type.random
import world.gregs.voidps.world.interact.entity.combat.hit.Hit
import world.gregs.voidps.world.interact.entity.combat.hit.combatHit
import world.gregs.voidps.world.interact.entity.combat.hit.prayerHit
import world.gregs.voidps.world.interact.entity.player.combat.prayer.*
import world.gregs.voidps.world.interact.entity.player.combat.special.MAX_SPECIAL_ATTACK
import world.gregs.voidps.world.interact.entity.player.combat.special.specialAttackEnergy
import world.gregs.voidps.world.interact.entity.player.energy.MAX_RUN_ENERGY
import world.gregs.voidps.world.interact.entity.player.energy.runEnergy
import world.gregs.voidps.world.interact.entity.proj.shoot

timerStart("prayer_bonus_drain") { _ ->
    interval = 50
}

timerTick("prayer_bonus_drain") { player ->
    val attack = player.getLeech(Skill.Attack)
    val strength = player.getLeech(Skill.Strength)
    val defence = player.getLeech(Skill.Defence)
    val ranged = player.getLeech(Skill.Ranged)
    val magic = player.getLeech(Skill.Magic)
    if (attack == 0 && strength == 0 && defence == 0 && ranged == 0 && magic == 0) {
        cancel()
    } else {
        player.clear("stat_reduction_msg")
        restore(player, Skill.Attack, attack)
        restore(player, Skill.Strength, strength)
        restore(player, Skill.Defence, defence)
        restore(player, Skill.Ranged, ranged)
        restore(player, Skill.Magic, magic)
    }
}

fun restore(player: Player, skill: Skill, leech: Int) {
    if (leech > 0) {
        player.setLeech(skill, leech - 1)
        val key = "stat_reduction_msg"
        if (!player[key, false]) {
            player[key] = true
            player.message("The sap or leech curses currently affecting your stats reduce a little.")
        }
    } else if (leech < 0) {
        player.setLeech(skill, leech + 1)
    }
}

fun getLevel(target: Character, skill: Skill): Int {
    return target.levels.getMax(skill)
}

combatHit { target ->
    if (source !is Player || !source.praying("sap_spirit")) {
        return@combatHit
    }
    if (random.nextDouble() >= 0.25) {
        return@combatHit
    }
    val player = source
    val energy = target.specialAttackEnergy
    if (energy <= 0) {
        weakMessage(player, true, "spirit")
        return@combatHit
    }
    target.specialAttackEnergy = (energy - (MAX_SPECIAL_ATTACK / 10)).coerceAtLeast(0)
    cast(player, target, true, "spirit")
}

combatHit { target ->
    if (source !is Player || !source.praying("special_attack")) {
        return@combatHit
    }
    if (random.nextDouble() >= 0.15) {
        return@combatHit
    }
    val player = source
    var energy = target.specialAttackEnergy
    if (energy <= 0) {
        weakMessage(player, true, "spirit")
        return@combatHit
    }
    val amount = MAX_SPECIAL_ATTACK / 10
    target.specialAttackEnergy = (energy - amount).coerceAtLeast(0)
    cast(player, target, false, "special_attack")

    energy = player.specialAttackEnergy
    if (energy == MAX_SPECIAL_ATTACK) {
        drainMessage(player, "special_attack")
        return@combatHit
    }
    player.specialAttackEnergy = (energy + amount).coerceAtMost(MAX_SPECIAL_ATTACK)
    boostMessage(player, "Special Attack")
}

combatHit { target ->
    if (source !is Player || !source.praying("leech_energy")) {
        return@combatHit
    }
    if (random.nextDouble() >= 0.15) {
        return@combatHit
    }
    val player = source
    var energy = target.runEnergy
    if (energy <= 0) {
        weakMessage(player, false, "run_energy")
        return@combatHit
    }
    val amount = MAX_RUN_ENERGY / 10
    target.runEnergy = energy - amount
    cast(player, target, false, "energy")

    energy = player.runEnergy
    if (energy == MAX_RUN_ENERGY) {
        drainMessage(player, "run_energy")
        return@combatHit
    }
    target.runEnergy = energy + amount
    boostMessage(player, "Run Energy")
}

fun cast(source: Character, target: Character, sap: Boolean, name: String) {
    source.queue("leech", 1) {
        val type = if (sap) "sap" else "leech"
        source.setAnimation(type)
        source.setGraphic("cast_${type}_${name}")
        source.shoot("proj_${type}_${name}", target)
        target.setGraphic("land_${type}_${name}", delay = Hit.magicDelay(source.tile.distanceTo(target)) * 30)
    }
}

set("sap_warrior", Skill.Attack)
set("sap_ranger", Skill.Ranged)
set("sap_mage", Skill.Magic)
set("leech_attack", Skill.Attack)
set("leech_ranged", Skill.Ranged)
set("leech_defence", Skill.Defence)
set("leech_magic", Skill.Magic)

fun set(prayer: String, skill: Skill) {
    val sap = prayer.startsWith("sap")
    variableSet("under_attack", 0) { player: Player ->
        player.clear("${skill.name.lowercase()}_drain_msg")
        player.clear("${skill.name.lowercase()}_leech_msg")
    }

    prayerHit(prayer, Priority.HIGHER) { target: Character ->
        if (random.nextDouble() >= if (sap) 0.25 else 0.15) {
            return@prayerHit
        }
        val name = skill.name.lowercase()
        val drain = target.getDrain(skill) + 1
        if (drain * 100.0 / getLevel(target, skill) > if (sap) 10 else 15) {
            weakMessage(source, sap, name)
            return@prayerHit
        }

        cast(source, target, sap, name)

        if (sap) {
            source.message("Your curse drains ${skill.name} from the enemy, boosting your ${skill.name}.")
        }
        if (sap && skill == Skill.Attack) {
            target.setDrain(Skill.Attack, drain, 10)
            target.setDrain(Skill.Strength, drain, 10)
            target.setDrain(Skill.Defence, drain, 10)
        } else {
            target.setDrain(skill, drain, 10)
        }
        target.updateBonus(skill)

        if (!sap) {
            val leech = source.getLeech(skill) + 1
            if (leech * 100.0 / source.levels.getMax(skill) > 5) {
                drainMessage(source, name)
                return@prayerHit
            }
            boostMessage(source, skill.name)
            source.setLeech(skill, leech)
            source.updateBonus(skill)
            source.softTimers.startIfAbsent("prayer_bonus_drain")
        }
    }
}

fun weakMessage(source: Character, sap: Boolean, name: String) {
    val key = "${name}_drain_msg"
    if (!source[key, false]) {
        source[key] = true
        source.message("Your opponent has been weakened so much that your ${if (sap) "sap" else "leech"} curse has no effect.")
    }
}

fun boostMessage(source: Character, name: String) {
    source.message("Your curse drains $name from the enemy, boosting your $name.")
}

fun drainMessage(source: Character, name: String) {
    val key = "${name}_leech_msg"
    if (!source[key, false]) {
        source[key] = true
        source.message("Your curse drains ${name.toTitleCase()} from the enemy, but has already made you so strong that", ChatType.Filter)
        source.message("it can improve you no further.", ChatType.Filter)
    }
}