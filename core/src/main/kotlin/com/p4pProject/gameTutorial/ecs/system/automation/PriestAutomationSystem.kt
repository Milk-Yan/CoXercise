package com.p4pProject.gameTutorial.ecs.system.automation

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.MathUtils
import com.p4pProject.gameTutorial.V_HEIGHT
import com.p4pProject.gameTutorial.V_WIDTH
import com.p4pProject.gameTutorial.ecs.component.*
import com.p4pProject.gameTutorial.event.GameEvent
import com.p4pProject.gameTutorial.event.GameEventManager
import com.p4pProject.gameTutorial.screen.CharacterType
import com.p4pProject.gameTutorial.screen.GameMode
import com.p4pProject.gameTutorial.screen.chosenCharacterType
import com.p4pProject.gameTutorial.screen.gameMode
import ktx.ashley.allOf
import ktx.ashley.get
import kotlin.math.abs
import kotlin.random.Random

const val PRIEST_MOVEMENT_SPEED = 0.25f
const val PRIEST_ATTACK_RANGE = 7f

class PriestAutomationSystem(
    private val gameEventManager: GameEventManager): IteratingSystem(allOf(PlayerComponent::class,
    TransformComponent::class, FacingComponent::class, PriestAnimationComponent::class).get()) {

    override fun processEntity(entity: Entity, deltaTime: Float) {
        val player = entity[PlayerComponent.mapper]
        require(player != null) { "Entity |entity| must have a PlayerComponent. entity=$entity" }

        if (gameMode == GameMode.MULTIPLAYER || player.characterType != CharacterType.PRIEST ||
            chosenCharacterType == CharacterType.PRIEST) {
            return
        }

        walkToRunAwayAndAttackBoss(entity)
    }

    private fun walkToRunAwayAndAttackBoss(priest: Entity) {

        Gdx.app.log("Walk to or run away or attack", "priest")

        val player = priest[PlayerComponent.mapper]
        require(player != null) { "Entity |entity| must have a PlayerComponent. entity=$priest" }

        val facing = priest[FacingComponent.mapper]
        require(facing != null) { "Entity |entity| must have a FacingComponent. entity=$facing" }

        if (player.isAttacking || player.isSpecialAttacking) {
            return
        }

        val bossInfo = priest[BossInfoComponent.mapper]
        if (bossInfo == null || !bossInfo.bossIsInitialized()) {
            return
        }


        val bossTrans = bossInfo.boss[TransformComponent.mapper]!!
        val priestTrans = priest[TransformComponent.mapper]!!

        when {
            player.hp < player.maxHp * 0.5 -> {
                facing.direction = findDirectionToFace(bossTrans, priestTrans, faceBoss = false)
                move(priestTrans, facing.direction)
            }
            isBossInAttackRange(priestTrans, bossTrans) -> {
                facing.direction = findDirectionToFace(bossTrans, priestTrans, faceBoss = true)
                attackBoss(priest)
            }
            else -> {
                facing.direction = findDirectionToFace(bossTrans, priestTrans, faceBoss = true)
                move(priestTrans, facing.direction)
            }
        }
    }

    private fun attackBoss(priest: Entity) {
        val player = priest[PlayerComponent.mapper]!!

        if (player.mp > player.specialAttackMpCost) {
            val playerToHeal = getLowestHpPlayer(priest)
            if (playerToHeal != null) {
                player.mp -= player.specialAttackMpCost
                gameEventManager.dispatchEvent(GameEvent.PriestSpecialAttackEvent.apply {
                    this.player = priest
                })
                return
            }
        }

        gameEventManager.dispatchEvent(GameEvent.PriestAttackEvent.apply {
            this.player = priest
        })
    }

    private fun getLowestHpPlayer(priest: Entity): Entity? {
        // this is ugly asf ik
        return priest[BossInfoComponent.mapper]!!.boss[PlayerInfoComponent.mapper]!!.getCharacterToHeal()
    }

    private fun isBossInAttackRange(priestTrans: TransformComponent, bossTrans: TransformComponent): Boolean {
        return priestTrans.position.dst(bossTrans.position) < PRIEST_ATTACK_RANGE
    }

    private fun findDirectionToFace(bossTrans: TransformComponent, priestTrans: TransformComponent, faceBoss: Boolean): FacingDirection {
        val bossPos = bossTrans.position
        val priestPos = priestTrans.position

        // go in the direction of the longest distance
        val disX = abs(bossPos.x - priestPos.x)
        val disY = abs(bossPos.y - priestPos.y)

//        Gdx.app.log("Boss Position", "x: " + bossPos.x.toString() + ", y: " + bossPos.y.toString())
//        Gdx.app.log("Priest Position", "x: " + priestPos.x + ",y: " + priestPos.y)

        if (!faceBoss) {
            if (priestPos.x == 0f || priestPos.x == V_WIDTH.toFloat()) {
                return if (bossPos.y > priestPos.y) {
                    FacingDirection.SOUTH
                } else {
                    FacingDirection.NORTH
                }
            }

            if (priestPos.y == 0f || priestPos.y == V_HEIGHT.toFloat()) {
                return if (bossPos.x > priestPos.x) {
                    FacingDirection.WEST
                } else {
                    FacingDirection.EAST
                }
            }
        }

        return if (disX > disY) {
            if (bossPos.x > priestPos.x) {
                if (faceBoss) {
                    FacingDirection.EAST
                } else {
                    FacingDirection.WEST
                }
            } else {
                if (faceBoss) {
                    FacingDirection.WEST
                } else {
                    FacingDirection.EAST
                }
            }
        } else {
            if (bossPos.y > priestPos.y) {
                if (faceBoss) {
                    FacingDirection.NORTH
                } else {
                    FacingDirection.SOUTH
                }
            } else {
                if (faceBoss) {
                    FacingDirection.SOUTH
                } else {
                    FacingDirection.NORTH
                }
            }
        }
    }

    private fun move(priestTrans: TransformComponent, facingDirection: FacingDirection) {
        val priestPos = priestTrans.position

        when (facingDirection) {
            FacingDirection.NORTH -> {
                priestPos.y = MathUtils.clamp(priestPos.y + PRIEST_MOVEMENT_SPEED, 0f, V_HEIGHT.toFloat())
            }
            FacingDirection.SOUTH -> {
                priestPos.y = MathUtils.clamp(priestPos.y - PRIEST_MOVEMENT_SPEED, 0f, V_HEIGHT.toFloat())
            }
            FacingDirection.EAST -> {
                priestPos.x = MathUtils.clamp(priestPos.x + PRIEST_MOVEMENT_SPEED, 0f, V_WIDTH - priestTrans.size.x)
            }
            FacingDirection.WEST -> {
                priestPos.x = MathUtils.clamp(priestPos.x - PRIEST_MOVEMENT_SPEED, 0f, V_WIDTH - priestTrans.size.x)
            }
        }
    }

}