package com.p4pProject.gameTutorial.ecs.system

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.EntitySystem
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Pool
import com.p4pProject.gameTutorial.ecs.component.PlayerComponent
import com.p4pProject.gameTutorial.event.GameEvent
import com.p4pProject.gameTutorial.event.GameEventListener
import com.p4pProject.gameTutorial.event.GameEventManager
import com.p4pProject.gameTutorial.screen.chosenCharacterType
import ktx.ashley.get
import ktx.collections.GdxArray
import ktx.log.debug
import ktx.log.logger

private val LOG = logger<CameraShake>()
private class CameraShake : Pool.Poolable {

    var maxDistortion = 0f
    var duration = 0f
    lateinit var camera: Camera
    private var storeCameraPos = true
    private val origCamPosition = Vector3()
    private var currentDuration = 0f

    fun update(deltaTime: Float) : Boolean {
        if(storeCameraPos){
            storeCameraPos = false
            origCamPosition.set(camera.position)
        }

        if(currentDuration < duration){
            val currentPower = maxDistortion * ((duration - currentDuration)/duration)

            camera.position.x = origCamPosition.x + MathUtils.random(-1f, 1f) * currentPower
            camera.position.y = origCamPosition.y + MathUtils.random(-1f, 1f) * currentPower
            camera.update()

            currentDuration += deltaTime
            return false
        }

        camera.position.set(origCamPosition)
        camera.update()
        return true

    }

    override fun reset() {
        maxDistortion = 0f
        duration = 0f
        storeCameraPos = true
        origCamPosition.set(Vector3.Zero)
        currentDuration = 0f
    }

}

private class CameraShakePool(private val gameCamera: Camera) : Pool<CameraShake>(){
    override fun newObject() = CameraShake().apply {
        this.camera = gameCamera
    }
}

class CameraShakeSystem (
    camera: Camera,
    private val gameEventManager: GameEventManager
    ): EntitySystem(), GameEventListener{
    private val shakePool = CameraShakePool(camera)
    private val activeShakes = GdxArray<CameraShake>()

    override fun addedToEngine(engine: Engine?) {
        super.addedToEngine(engine)
        gameEventManager.addListener(GameEvent.PlayerHit::class, this)
    }

    override fun removedFromEngine(engine: Engine?) {
        super.removedFromEngine(engine)
        gameEventManager.removeListener(this)
    }

    override fun update(deltaTime: Float) {
        if(!activeShakes.isEmpty){
            val shake = activeShakes.first()
            if(shake.update(deltaTime)){
                activeShakes.removeIndex(0)
                shakePool.free(shake)
            }
        }
    }
    override fun onEvent(event: GameEvent) {

        when(event){
            is GameEvent.PlayerHit ->{
                var characterType = event.player[PlayerComponent.mapper]?.characterType
                require(characterType != null)
                if(activeShakes.size < 1 && characterType == chosenCharacterType){
                    activeShakes.add(shakePool.obtain().apply {
                        duration = 0.25f
                        maxDistortion = 0.25f
                    })
                }
            }
        }

    }
}