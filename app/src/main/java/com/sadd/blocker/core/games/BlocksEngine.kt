package com.sadd.blocker.core.games

import kotlin.random.Random

/** هفت تترمینوی کلاسیک بازی «بلوک‌ها». */
enum class Tetromino {
    I, O, T, S, Z, J, L,
}

/**
 * موتور خالص بازی «بلوک‌ها» (تتریس) — بدون هیچ وابستگی به Compose تا با
 * kotlinc جدا تست شود. صفحه ۱۰×۲۰ است؛ هر سلول قفل‌شده مقدار
 * `ordinal + 1` تترمینو را نگه می‌دارد و ۰ یعنی خالی.
 *
 * چرخش ساده و بدون wall-kick است: قطعه حول جعبه محدودکننده‌اش می‌چرخد و
 * اگر جای تازه با دیوار یا سلول قفل‌شده برخورد کند، چرخش رد می‌شود.
 * ترتیب قطعه‌ها از [Random] با seed داده‌شده می‌آید تا تست‌ها قطعی باشند.
 * امتیاز پاک‌کردن خط‌ها: ۱ خط ۱۰۰، ۲ خط ۳۰۰، ۳ خط ۵۰۰، ۴ خط ۸۰۰.
 */
class BlocksEngine(seed: Long = 42L) {

    companion object {
        const val WIDTH = 10
        const val HEIGHT = 20

        // امتیاز به‌ازای تعداد خط پاک‌شده در یک قفل‌شدن (اندیس = تعداد خط).
        private val LINE_SCORES = intArrayOf(0, 100, 300, 500, 800)
    }

    private var random: Random = Random(seed)

    // board[y][x] — سطر صفر بالای صفحه است.
    private val board: Array<IntArray> = Array(HEIGHT) { IntArray(WIDTH) }

    /** چرخش‌های هر قطعه: ۴ حالت، هر حالت فهرست آفست‌های (x, y) نسبت به گوشه قطعه. */
    private val rotations: Map<Tetromino, List<List<Pair<Int, Int>>>> =
        Tetromino.values().associateWith { piece -> buildRotations(piece) }

    var score: Int = 0
        private set
    var lines: Int = 0
        private set
    var gameOver: Boolean = false
        private set

    var current: Tetromino = Tetromino.T
        private set
    var currentX: Int = 0
        private set
    var currentY: Int = 0
        private set
    var currentRotation: Int = 0
        private set

    init {
        spawn()
    }

    /** مقدار سلول قفل‌شده: ۰ خالی، وگرنه ordinal تترمینو + ۱. */
    fun cellAt(x: Int, y: Int): Int {
        return board[y][x]
    }

    /** سلول‌های مطلق قطعه جاری روی صفحه (برای رسم در UI). */
    fun currentCells(): List<Pair<Int, Int>> {
        return rotations.getValue(current)[currentRotation].map { (dx, dy) ->
            Pair(currentX + dx, currentY + dy)
        }
    }

    /** آیا قطعه با این چرخش و موقعیت، بدون برخورد روی صفحه جا می‌شود؟ */
    fun canPlaceAt(piece: Tetromino, rotation: Int, px: Int, py: Int): Boolean {
        val cells = rotations.getValue(piece)[rotation]
        for ((dx, dy) in cells) {
            val x = px + dx
            val y = py + dy
            if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) {
                return false
            }
            if (board[y][x] != 0) {
                return false
            }
        }
        return true
    }

    fun moveLeft(): Boolean {
        if (gameOver) {
            return false
        }
        if (!canPlaceAt(current, currentRotation, currentX - 1, currentY)) {
            return false
        }
        currentX -= 1
        return true
    }

    fun moveRight(): Boolean {
        if (gameOver) {
            return false
        }
        if (!canPlaceAt(current, currentRotation, currentX + 1, currentY)) {
            return false
        }
        currentX += 1
        return true
    }

    fun rotate(): Boolean {
        if (gameOver) {
            return false
        }
        val nextRotation = (currentRotation + 1) % 4
        if (!canPlaceAt(current, nextRotation, currentX, currentY)) {
            return false
        }
        currentRotation = nextRotation
        return true
    }

    /** یک قدم پایین (بدون قفل‌شدن)؛ true یعنی قطعه واقعاً پایین رفت. */
    fun softDrop(): Boolean {
        if (gameOver) {
            return false
        }
        if (!canPlaceAt(current, currentRotation, currentX, currentY + 1)) {
            return false
        }
        currentY += 1
        return true
    }

    /** رهاکردن: قطعه تا کف می‌افتد و همان‌جا قفل می‌شود. */
    fun hardDrop() {
        if (gameOver) {
            return
        }
        while (canPlaceAt(current, currentRotation, currentX, currentY + 1)) {
            currentY += 1
        }
        lockCurrent()
    }

    /** یک تیک گرانش: یا قطعه یک قدم پایین می‌رود یا قفل می‌شود. */
    fun tick() {
        if (gameOver) {
            return
        }
        if (!softDrop()) {
            lockCurrent()
        }
    }

    /** شروع دوباره با seed تازه (صفحه و امتیاز پاک می‌شوند). */
    fun reset(seed: Long) {
        random = Random(seed)
        for (y in 0 until HEIGHT) {
            board[y].fill(0)
        }
        score = 0
        lines = 0
        gameOver = false
        spawn()
    }

    /**
     * کمکیِ تست و آماده‌سازی: یک سلول قفل‌شده را مستقیم می‌گذارد
     * (مقدار ۰ یعنی پاک‌کردن سلول). در بازی عادی استفاده نمی‌شود.
     */
    fun setLockedCell(x: Int, y: Int, value: Int) {
        board[y][x] = value
    }

    /** کمکیِ تست: قطعه جاری را بدون بررسی برخورد در جای داده‌شده می‌گذارد. */
    fun setCurrentPiece(piece: Tetromino, x: Int, y: Int, rotation: Int = 0) {
        current = piece
        currentX = x
        currentY = y
        currentRotation = rotation
    }

    // قطعه تازه را بالای صفحه و وسط می‌آورد؛ اگر جا نشود بازی تمام است.
    private fun spawn() {
        current = Tetromino.values()[random.nextInt(Tetromino.values().size)]
        currentRotation = 0
        val pieceWidth = rotations.getValue(current)[0].maxOf { it.first } + 1
        currentX = (WIDTH - pieceWidth) / 2
        currentY = 0
        if (!canPlaceAt(current, currentRotation, currentX, currentY)) {
            gameOver = true
        }
    }

    // قطعه جاری را روی صفحه می‌نشاند، خط‌های کامل را پاک می‌کند و قطعه بعدی را می‌آورد.
    private fun lockCurrent() {
        val value = current.ordinal + 1
        for ((x, y) in currentCells()) {
            board[y][x] = value
        }
        val cleared = clearFullLines()
        if (cleared > 0) {
            lines += cleared
            score += LINE_SCORES[cleared]
        }
        spawn()
    }

    // خط‌های کامل را حذف می‌کند و سطرهای بالا را پایین می‌کشد؛ تعداد خط برگردانده می‌شود.
    private fun clearFullLines(): Int {
        var cleared = 0
        var writeRow = HEIGHT - 1
        for (readRow in HEIGHT - 1 downTo 0) {
            val full = board[readRow].all { it != 0 }
            if (full) {
                cleared += 1
            } else {
                if (writeRow != readRow) {
                    board[readRow].copyInto(board[writeRow])
                }
                writeRow -= 1
            }
        }
        while (writeRow >= 0) {
            board[writeRow].fill(0)
            writeRow -= 1
        }
        return cleared
    }

    // شکل پایه هر قطعه در جعبه محدودکننده خودش (آفست از گوشه بالا-چپ جعبه).
    private fun baseCells(piece: Tetromino): List<Pair<Int, Int>> {
        return when (piece) {
            Tetromino.I -> listOf(Pair(0, 0), Pair(1, 0), Pair(2, 0), Pair(3, 0))
            Tetromino.O -> listOf(Pair(0, 0), Pair(1, 0), Pair(0, 1), Pair(1, 1))
            Tetromino.T -> listOf(Pair(0, 0), Pair(1, 0), Pair(2, 0), Pair(1, 1))
            Tetromino.S -> listOf(Pair(1, 0), Pair(2, 0), Pair(0, 1), Pair(1, 1))
            Tetromino.Z -> listOf(Pair(0, 0), Pair(1, 0), Pair(1, 1), Pair(2, 1))
            Tetromino.J -> listOf(Pair(0, 0), Pair(0, 1), Pair(1, 1), Pair(2, 1))
            Tetromino.L -> listOf(Pair(2, 0), Pair(0, 1), Pair(1, 1), Pair(2, 1))
        }
    }

    private fun baseSize(piece: Tetromino): Pair<Int, Int> {
        return when (piece) {
            Tetromino.I -> Pair(4, 1)
            Tetromino.O -> Pair(2, 2)
            Tetromino.T, Tetromino.S, Tetromino.Z, Tetromino.J, Tetromino.L -> Pair(3, 2)
        }
    }

    // چهار چرخش با چرخاندن جعبه محدودکننده: (x, y) در جعبه W×H به (H-1-y, x) در جعبه H×W می‌رود.
    private fun buildRotations(piece: Tetromino): List<List<Pair<Int, Int>>> {
        val result = mutableListOf<List<Pair<Int, Int>>>()
        var cells = baseCells(piece)
        var (boxWidth, boxHeight) = baseSize(piece)
        repeat(4) {
            result.add(cells)
            val rotated = cells.map { (x, y) -> Pair(boxHeight - 1 - y, x) }
            cells = rotated
            val tmp = boxWidth
            boxWidth = boxHeight
            boxHeight = tmp
        }
        return result
    }
}
