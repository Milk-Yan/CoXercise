package com.p4pProject.gameTutorial.ecs.system

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.Rectangle
import com.p4pProject.gameTutorial.ecs.component.PlayerComponent
import com.p4pProject.gameTutorial.ecs.component.RemoveComponent
import com.p4pProject.gameTutorial.ecs.component.TransformComponent
import com.p4pProject.gameTutorial.ecs.component.*
import com.p4pProject.gameTutorial.event.GameEvent
import com.p4pProject.gameTutorial.event.GameEventListener
import com.p4pProject.gameTutorial.event.GameEventManager
import com.p4pProject.gameTutorial.screen.CharacterType
import ktx.ashley.addComponent
import ktx.ashley.allOf
import ktx.ashley.exclude
import ktx.ashley.get
import ktx.log.debug
import ktx.log.logger

private const val DEATH_EXPLOSION_DURATION = 0.9f

private val LOG = logger<DamageSystem>()
class DamageSystem (
    private val gameEventManager: GameEventManager
        ) : GameEventListener, IteratingSystem(allOf(PlayerComponent::class, TransformComponent:: class).exclude(RemoveComponent::class).get()) {

    private var bossAttackAreas = BossAttackArea(0, 0F,
       0F, 0F, 0F)

    var warriorCheck = false;
    var archerCheck = false;
    var priestCheck = false;

    var warriorDead = false
    var archerDead = false
    var priestDead = false

    override fun processEntity(entity: Entity, deltaTime: Float) {
        val transform = entity[TransformComponent.mapper]
        require(transform != null ){"Entity |entity| must have a TransformComponent. entity=$entity"}
        val player = entity[PlayerComponent.mapper]
        require(player != null ){"Entity |entity| must have a PlayerComponent. entity=$entity"}

        if(!warriorCheck && player.characterType == CharacterType.WARRIOR){
            warriorCheck = true;
            checkDmg(entity);
        }else if(!archerCheck && player.characterType == CharacterType.ARCHER){
            archerCheck = true;
            checkDmg(entity);
        }else if(!priestCheck && player.characterType == CharacterType.PRIEST){
            priestCheck = true;
            checkDmg(entity);
        }

        if((warriorCheck || warriorDead) && (archerCheck || archerDead) && (priestCheck || priestDead)){
            bossAttackAreas = BossAttackArea(0, 0F,
                0F, 0F, 0F)
            warriorCheck = false;
            archerCheck = false;
            priestCheck = false;
        }
    }

    private fun checkDmg(entity: Entity){
        val transform = entity[TransformComponent.mapper]
        require(transform != null ){"Entity |entity| must have a TransformComponent. entity=$entity"}
        val player = entity[PlayerComponent.mapper]
        require(player != null ){"Entity |entity| must have a PlayerComponent. entity=$entity"}

        val bossAttackBoundingRect = Rectangle().set(
            bossAttackAreas.startX,
            bossAttackAreas.startY,
            bossAttackAreas.endX - bossAttackAreas.startX,
            bossAttackAreas.endY - bossAttackAreas.startY
        )
        if (transform.overlapsRect(bossAttackBoundingRect)) {
            //ouch
            player.hp -= bossAttackAreas.damage
            LOG.debug { "PlayerDamaged: ${player.characterType}" }

            gameEventManager.dispatchEvent(GameEvent.PlayerHit.apply {
                this.player = entity
                hp = player.hp
                maxHp = player.maxHp
            })

            if(player.hp <= 0f){
                entity.addComponent<RemoveComponent>(engine){
                    delay = DEATH_EXPLOSION_DURATION
                }
                gameEventManager.dispatchEvent(GameEvent.PlayerDeath.apply {
                    this.characterType = player.characterType
                })
            }
        }
    }

    override fun addedToEngine(engine: Engine?) {
        super.addedToEngine(engine)
        gameEventManager.addListener(GameEvent.BossAttackFinished::class, this)
        gameEventManager.addListener(GameEvent.PlayerDeath::class, this)
    }

    override fun removedFromEngine(engine: Engine?) {
        super.removedFromEngine(engine)
        gameEventManager.removeListener(GameEvent.BossAttackFinished::class, this)
        gameEventManager.removeListener(GameEvent.PlayerDeath::class, this)
    }

    override fun onEvent(event: GameEvent) {
        if (event is GameEvent.BossAttackFinished) {
            bossAttackAreas = BossAttackArea(event.damage, event.startX,
                event.endX, event.startY, event.endY)
        }
        if (event is GameEvent.PlayerDeath) {
            when(event.characterType) {
                CharacterType.WARRIOR -> warriorDead = true
                CharacterType.ARCHER -> archerDead = true
                CharacterType.PRIEST -> priestDead = true
            }
        }
    }


    private class BossAttackArea(val damage: Int, val startX: Float, val endX: Float, val startY: Float,
                                 val endY: Float)
}