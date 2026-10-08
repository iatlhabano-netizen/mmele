package bw.mmele.online

import bw.mmele.engine.Player
import bw.mmele.ui.game.GameConfig
import bw.mmele.ui.game.GameMode
import bw.mmele.ui.pieces.PieceStyle
import bw.mmele.ui.scenes.Scene

/** A room you are seated in. Handed from the lobby to the game screen. */
data class OnlineSession(
    val repo: OnlineGameRepository,
    val room: RoomSnapshot,
    val seat: Seat,
) {
    /** Game config both phones agree on: variant and pieces come from the room, the scene is yours. */
    fun configFor(scene: Scene): GameConfig = GameConfig(
        variant = room.config.variant,
        mode = GameMode.ONLINE,
        human = seat.toPlayer(),
        clockMinutes = 0,
        scene = scene,
        styleOne = PieceStyle.fromId(room.config.styleOne) ?: PieceStyle.defaultFor(Player.ONE),
        styleTwo = PieceStyle.fromId(room.config.styleTwo) ?: PieceStyle.defaultFor(Player.TWO),
    )
}
