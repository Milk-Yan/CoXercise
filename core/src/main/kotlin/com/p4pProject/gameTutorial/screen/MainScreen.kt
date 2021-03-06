package com.p4pProject.gameTutorial.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Dialog
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.utils.Align
import com.p4pProject.gameTutorial.MyGameTutorial
import com.p4pProject.gameTutorial.socket.emit.SocketEmit
import com.p4pProject.gameTutorial.socket.on.SocketOn
import com.p4pProject.gameTutorial.ui.SkinLabel
import com.p4pProject.gameTutorial.ui.SkinTextButton
import com.p4pProject.gameTutorial.ui.SkinTextField
import com.p4pProject.gameTutorial.ui.SkinWindow
import io.socket.client.IO
import io.socket.client.Socket
import ktx.actors.onChange
import ktx.actors.onClick
import ktx.scene2d.*
import org.json.JSONObject

enum class GameMode {
    SINGLEPLAYER, MULTIPLAYER
}

lateinit var chosenCharacterType: CharacterType;
var gameMode = GameMode.SINGLEPLAYER

class MainScreen( game: MyGameTutorial) : GameBaseScreen(game) {

    private lateinit var invalidLobbyLabel: Label
    private var socket: Socket? = null
    private var lobbyID: String = ""
    private lateinit var singleplayerDialog: Dialog
    private lateinit var multiplayerDialog: Dialog

    override fun show() {
        setupUI()
    }

    private fun setupUI() {
        // TODO design main page one day
        stage.actors {
            table {
                defaults().fillX().expandX()
                label("ExerQuest", SkinLabel.LARGE.name) {
                    setAlignment(Align.center)
                    wrap = true
                    color.set(Color.WHITE)
                }
                row()
                label("CoXercise", SkinLabel.LARGE.name) {
                    setAlignment(Align.center)
                    wrap = true
                    color.set(Color.WHITE)
                }
                row()
                textButton("Singleplayer Mode", SkinTextButton.DEFAULT.name) {
                    onClick {
                        stage.actors {
                            singleplayerDialog = dialog("choose character singleplayer", SkinWindow.DEFAULT.name) {
                                table {
                                    textButton( "X", SkinTextButton.DEFAULT.name) {
                                        onClick { singleplayerDialog.hide() }
                                    }
                                    row()
                                    textButton(
                                        CharacterType.WARRIOR.name,
                                        SkinTextButton.DEFAULT.name
                                    ) {
                                        onClick {
                                            chosenCharacterType = CharacterType.WARRIOR
                                            startSinglePlayerGame();
                                        }
                                    }
                                    row()
                                    textButton(
                                        CharacterType.SLINGER.name,
                                        SkinTextButton.DEFAULT.name
                                    ) {
                                        onClick {
                                            chosenCharacterType = CharacterType.SLINGER
                                            startSinglePlayerGame();
                                        }
                                    }
                                    row()
                                    textButton(
                                        CharacterType.NECROMANCER.name,
                                        SkinTextButton.DEFAULT.name
                                    ) {
                                        onClick {
                                            chosenCharacterType = CharacterType.NECROMANCER
                                            startSinglePlayerGame();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                row()
                textButton("Multiplayer Mode", SkinTextButton.DEFAULT.name) {
                    onClick {
                        stage.actors {
                            multiplayerDialog = dialog("multiplayer choose character", SkinWindow.DEFAULT.name) {
                                table {
                                    table {
                                        textButton("X", SkinTextButton.LABEL.name) {
                                            onClick { multiplayerDialog.hide() }
                                        }
                                        textButton("Create Lobby", SkinTextButton.DEFAULT.name) {
                                            onClick {
                                                createLobby()
                                            }
                                        }
                                    }
                                    row()
                                    textButton(CharacterType.WARRIOR.name, SkinTextButton.DEFAULT.name) {
                                        onClick {
                                            chosenCharacterType = CharacterType.WARRIOR
                                        }
                                    }
                                    row()
                                    textButton(CharacterType.SLINGER.name, SkinTextButton.DEFAULT.name) {
                                        onClick {
                                            chosenCharacterType = CharacterType.SLINGER
                                        }
                                    }
                                    row()
                                    textButton(CharacterType.NECROMANCER.name, SkinTextButton.DEFAULT.name) {
                                        onClick {
                                            chosenCharacterType = CharacterType.NECROMANCER
                                        }
                                    }
                                    row()
                                    textField("lobby id", SkinTextField.DEFAULT.name) {
                                        onChange {
                                            lobbyID = text
                                        }
                                        alignment = Align.center

                                    }
                                    row()
                                    textButton("Join Lobby", SkinTextButton.DEFAULT.name) {
                                        onClick {
                                            joinLobbyIfValid()
                                        }
                                    }
                                    row()
                                    invalidLobbyLabel = label("Lobby ID invalid", SkinLabel.DEFAULT.name) {
                                        color.a = 0f
                                    }
                                }.pad(10f)
                            }
                        }

                    }
                }

                setFillParent(true)
                pack()
            }
            // TODO remove in production
            stage.isDebugAll = true
        }
    }

    override fun hide() {
        stage.clear()
    }

    override fun render(delta: Float) {
        if (assets.progress.isFinished && game.containsScreen<LobbyScreen>() &&
                ::chosenCharacterType.isInitialized && isValidLobbyID()) {
            changeToLobbyScreen()
        }

        stage.run {
            viewport.apply()
            act()
            draw()
        }
    }

    private fun isValidLobbyID(): Boolean {
        if (this.lobbyID.isBlank()) {
            return false;
        }

        if (lobbyID.matches(Regex("([A-z]){5}")) && lobbyID.length == 5) {
            return true;
        }

        return false;
    }

    private fun startSinglePlayerGame() {
        gameMode = GameMode.SINGLEPLAYER
        game.addScreen(LoadingScreen(game, socket, "", chosenCharacterType))
        game.removeScreen<MainScreen>()
        dispose()
        game.setScreen<LoadingScreen>()
    }

    private fun connectAndSetupSocket() {
        socket = IO.socket("http://coxercise.herokuapp.com")
        socket!!.connect()
        SocketOn.lobbyCreated(socket!!, callback = { lobbyID -> addLobbyScreen(lobbyID) });
        SocketOn.invalidLobbyID(socket!!, invalidLobbyID = { invalidLobbyID() });
        SocketOn.characterTaken(socket!!, characterTaken = { characterTaken() });
        SocketOn.joinLobbySuccessful(socket!!, callback = { addLobbyScreen() });

    }

    private fun createLobby() {
        if (!::chosenCharacterType.isInitialized) {
            return;
        }
        multiplayerGame()
        SocketEmit.createLobby(socket!!, chosenCharacterType.name);

    }

    private fun addLobbyScreen(lobbyID: String? = null) {
        if (lobbyID != null) {
            this.lobbyID = lobbyID;
        }

        if (game.containsScreen<LobbyScreen>()) {
            return
        }
        Gdx.app.log("Lobby", "adding screen with lobbyID$this.lobbyID")
        game.addScreen(LobbyScreen(game, this.lobbyID, socket!!, chosenCharacterType))
        Gdx.app.log("Lobby", "" + game.containsScreen<LobbyScreen>())
    }

    private fun changeToLobbyScreen() {
        game.removeScreen<MainScreen>()
        dispose()
        game.setScreen<LobbyScreen>()
    }

    private fun joinLobbyIfValid() {
        if (!::chosenCharacterType.isInitialized) {
            return;
        }

        Gdx.app.log("Socket", "attempting to join lobby " + lobbyID + " with character " + chosenCharacterType.name)
        val data = JSONObject();
        data.put("lobbyID", lobbyID);
        data.put("chosenCharacter", chosenCharacterType.name);
        Gdx.app.log("Data", data.toString());

        multiplayerGame()
        SocketEmit.joinLobby(socket!!, data);
    }

    private fun multiplayerGame() {
        gameMode = GameMode.MULTIPLAYER
        connectAndSetupSocket()
    }

    private fun invalidLobbyID() {
        invalidLobbyLabel.setText("invalid lobby ID")
        invalidLobbyLabel.color.a = 1f
    }

    private fun characterTaken() {
        invalidLobbyLabel.setText("character already taken")
        invalidLobbyLabel.color.a = 1f
    }
}
