package com.p4pProject.gameTutorial.ecs.component

import com.badlogic.ashley.core.Component
import com.badlogic.gdx.utils.Pool
import ktx.ashley.mapperFor

const val MAX_HP = 100
//const val MAX_SHIElD = 100f

class PlayerComponent : Component, Pool.Poolable {

    var life = MAX_HP
    var maxLife = MAX_HP
    var shield = 0f
    //var maxShield = MAX_SHIElD
    var distance = 0f
    var isAttacking = false

    override fun reset() {
         life = MAX_HP
         maxLife = MAX_HP
         shield = 0f
         //maxShield = MAX_SHIElD
         distance = 0f
        isAttacking = false
    }

    companion object{
        val mapper = mapperFor<PlayerComponent>()
    }
}