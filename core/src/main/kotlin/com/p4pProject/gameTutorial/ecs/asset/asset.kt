package com.p4pProject.gameTutorial.ecs.asset

import com.badlogic.gdx.assets.AssetDescriptor
import com.badlogic.gdx.assets.loaders.BitmapFontLoader
import com.badlogic.gdx.assets.loaders.ShaderProgramLoader
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.glutils.ShaderProgram

enum class TextureAsset(
    fileName:String,
    directory:String = "graphics",
    val descriptor : AssetDescriptor<Texture> = AssetDescriptor("$directory/$fileName", Texture::class.java)
){
    // Simple picture assets
    BACKGROUND("background.png")
}

enum class TextureAtlasAsset(
    val isSkinAtlas: Boolean,
    fileName:String,
    directory:String = "graphics",
    val descriptor : AssetDescriptor<TextureAtlas> = AssetDescriptor("$directory/$fileName", TextureAtlas::class.java)
){
    // Animated assets
    GAME_GRAPHICS(false ,"graphics.atlas"),
    WARRIOR_GRAPHICS(false ,"warrior.atlas"),
    ARCHER_GRAPHICS(false ,"archer.atlas"),
    PRIEST_GRAPHICS(false ,"priest.atlas"),
    BOSS_GRAPHICS(false ,"Boss.atlas"),
    UI(true ,"ui.atlas")
}

enum class SoundAsset(
    fileName:String,
    directory:String = "sound",
    val descriptor : AssetDescriptor<Sound> = AssetDescriptor("$directory/$fileName", Sound::class.java)
) {
    BOOST_1("boost1.wav"),
    BOOST_2("boost2.wav"),
    LIFE("life.wav"),
    SHIELD("shield.wav"),
    DAMAGE("damage.wav"),
    BLOCK("block.wav")
}

enum class MusicAsset(
    fileName:String,
    directory:String = "music",
    val descriptor : AssetDescriptor<Music> = AssetDescriptor("$directory/$fileName", Music::class.java)
){
    GAME("game.mp3")
}

enum class ShaderProgramAsset(
    vertexFileName : String,
    fragmentFileName: String,
    directory : String = "shader",
    val descriptor: AssetDescriptor<ShaderProgram> = AssetDescriptor(
        "$directory/$vertexFileName/$fragmentFileName",
        ShaderProgram::class.java,
        ShaderProgramLoader.ShaderProgramParameter().apply{
            vertexFile = "$directory/$vertexFileName"
            fragmentFile = "$directory/$fragmentFileName"
        }
    )
){

    OUTLINE("default.vert", "outline.frag")
}

enum class BitmapFontAsset(
    fileName:String,
    directory:String = "ui",
    val descriptor : AssetDescriptor<BitmapFont> = AssetDescriptor(
        "$directory/$fileName",
        BitmapFont::class.java,
        BitmapFontLoader.BitmapFontParameter().apply {
            atlasName = TextureAtlasAsset.UI.descriptor.fileName
        }
    )
){
    FONT_LARGE_GRADIENT("font11_gradient.fnt"),
    FONT_DEFAULT("font8.fnt")
}