import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class GameField(housesString: String) {
    val field = run {
        val houses = housesString.split(" ")
            .map { it.substring(1).toInt() - 1 to it[0] - 'A' }.toSet()

        val entities = mapOf(
            (0 to 0) to Entity(0, Entity.Type.ACROBAT),
            (0 to 1) to Entity(0, Entity.Type.CLOWN),
            (0 to 2) to Entity(0, Entity.Type.STRONGMAN),
            (1 to 0) to Entity(0, Entity.Type.CLOWN),
            (1 to 1) to Entity(0, Entity.Type.MAGICIAN),
            (2 to 0) to Entity(0, Entity.Type.STRONGMAN),
            (3 to 0) to Entity(0, Entity.Type.TRAINER),

            (8 to 0) to Entity(1, Entity.Type.ACROBAT),
            (8 to 1) to Entity(1, Entity.Type.CLOWN),
            (8 to 2) to Entity(1, Entity.Type.STRONGMAN),
            (7 to 0) to Entity(1, Entity.Type.CLOWN),
            (7 to 1) to Entity(1, Entity.Type.MAGICIAN),
            (6 to 0) to Entity(1, Entity.Type.STRONGMAN),
            (5 to 0) to Entity(1, Entity.Type.TRAINER),
        )

        Array(9) { row -> Array(12) { col -> CellInfo(row to col in houses, entities[row to col]) } }
    }

    data class CellInfo(val hasHouse: Boolean) {
        var entity by mutableStateOf(null as Entity?)

        constructor(hasHouse: Boolean, entity: Entity?) : this(hasHouse) {
            this.entity = entity
        }
    }

    data class Entity(val player: Int, val type: Type) {
        enum class Type {
            CLOWN,
            STRONGMAN,
            ACROBAT,
            MAGICIAN,
            TRAINER,
        }
    }

    fun doMove(move: String) {
        val (from, to) = move.split("-").map { it.substring(1).toInt() - 1 to it[0] - 'A'}

        if (from.first == -1) return

        if (field[to.first][to.second].entity != null) {
            if (field[from.first][from.second].entity?.type == Entity.Type.MAGICIAN) {
                val e1 = field[from.first][from.second].entity
                val e2 = field[to.first][to.second].entity

                field[from.first][from.second].entity = e2
                field[to.first][to.second].entity = e1
            } else {
                val e1 = field[from.first][from.second].entity
                val e2 = field[to.first][to.second].entity

                field[from.first][from.second].entity = null
                field[to.first][to.second].entity = e1
                field[2 * to.first - from.first][2 * to.second - from.second].entity = e2
            }
        } else {

            val e1 = field[from.first][from.second].entity

            field[from.first][from.second].entity = null
            field[to.first][to.second].entity = e1
        }
    }
}