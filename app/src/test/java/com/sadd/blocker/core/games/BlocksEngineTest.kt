package com.sadd.blocker.core.games

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * تست‌های موتور «بلوک‌ها»: پاک‌شدن خط و امتیاز، برخورد با دیوار و سلول
 * قفل‌شده، چرخش، پایان بازی و قطعی‌بودن ترتیب قطعه‌ها با seed یکسان.
 */
class BlocksEngineTest {

    @Test
    fun singleLineClearScores100() {
        val engine = BlocksEngine(seed = 1L)
        // سطر کف را پر کن، جز دو ستون وسط (۴ و ۵) که جای قطعه O است.
        for (x in 0 until BlocksEngine.WIDTH) {
            if (x != 4 && x != 5) {
                engine.setLockedCell(x, BlocksEngine.HEIGHT - 1, 1)
            }
        }
        engine.setCurrentPiece(Tetromino.O, 4, BlocksEngine.HEIGHT - 3)
        engine.hardDrop()
        assertEquals(1, engine.lines)
        assertEquals(100, engine.score)
        // نیمه بالای O بعد از پاک‌شدن خط یک سطر پایین آمده است.
        assertTrue(engine.cellAt(4, BlocksEngine.HEIGHT - 1) != 0)
        assertEquals(0, engine.cellAt(4, BlocksEngine.HEIGHT - 2))
    }

    @Test
    fun doubleLineClearScores300() {
        val engine = BlocksEngine(seed = 2L)
        // دو سطر کف پر، جز ستون‌های ۴ و ۵؛ O هر دو را کامل می‌کند.
        for (y in BlocksEngine.HEIGHT - 2 until BlocksEngine.HEIGHT) {
            for (x in 0 until BlocksEngine.WIDTH) {
                if (x != 4 && x != 5) {
                    engine.setLockedCell(x, y, 1)
                }
            }
        }
        engine.setCurrentPiece(Tetromino.O, 4, BlocksEngine.HEIGHT - 4)
        engine.hardDrop()
        assertEquals(2, engine.lines)
        assertEquals(300, engine.score)
        assertEquals(0, engine.cellAt(4, BlocksEngine.HEIGHT - 1))
        assertEquals(0, engine.cellAt(0, BlocksEngine.HEIGHT - 1))
    }

    @Test
    fun wallCollisionBlocksMove() {
        val engine = BlocksEngine(seed = 3L)
        engine.setCurrentPiece(Tetromino.I, 0, 5, rotation = 0)
        assertFalse(engine.moveLeft())
        assertEquals(0, engine.currentX)
        assertTrue(engine.moveRight())
        assertEquals(1, engine.currentX)
    }

    @Test
    fun lockedCellCollisionBlocksMove() {
        val engine = BlocksEngine(seed = 4L)
        engine.setLockedCell(5, 10, 1)
        engine.setCurrentPiece(Tetromino.O, 3, 10)
        // رفتن به راست سلول (۵،۱۰) قفل‌شده را اشغال می‌کند؛ پس رد می‌شود.
        assertFalse(engine.moveRight())
        assertEquals(3, engine.currentX)
        assertTrue(engine.moveLeft())
    }

    @Test
    fun rotationChangesCellsAndCyclesBack() {
        val engine = BlocksEngine(seed = 5L)
        engine.setCurrentPiece(Tetromino.T, 4, 5, rotation = 0)
        val startCells = engine.currentCells().toSet()
        assertTrue(engine.rotate())
        assertEquals(1, engine.currentRotation)
        assertTrue(engine.currentCells().toSet() != startCells)
        assertTrue(engine.rotate())
        assertTrue(engine.rotate())
        assertTrue(engine.rotate())
        assertEquals(0, engine.currentRotation)
        assertEquals(startCells, engine.currentCells().toSet())
    }

    @Test
    fun tickDropsThenLocksAtBottom() {
        val engine = BlocksEngine(seed = 6L)
        engine.setCurrentPiece(Tetromino.O, 0, BlocksEngine.HEIGHT - 2)
        // قطعه روی کف است؛ تیک بعدی باید آن را قفل کند و قطعه تازه بیاورد.
        engine.tick()
        assertTrue(engine.cellAt(0, BlocksEngine.HEIGHT - 1) != 0)
        assertEquals(0, engine.currentY)
        assertFalse(engine.gameOver)
    }

    @Test
    fun gameOverWhenSpawnAreaBlocked() {
        val engine = BlocksEngine(seed = 7L)
        // ناحیه تولد قطعه (دو سطر بالا، ستون‌های وسط) را پر کن.
        for (y in 0..1) {
            for (x in 3..6) {
                engine.setLockedCell(x, y, 1)
            }
        }
        engine.setCurrentPiece(Tetromino.O, 0, BlocksEngine.HEIGHT - 2)
        engine.hardDrop()
        assertTrue(engine.gameOver)
    }

    @Test
    fun sameSeedSamePieceSequence() {
        val first = BlocksEngine(seed = 99L)
        val second = BlocksEngine(seed = 99L)
        repeat(5) {
            assertEquals(first.current, second.current)
            first.setCurrentPiece(first.current, 0, BlocksEngine.HEIGHT - 2)
            second.setCurrentPiece(second.current, 0, BlocksEngine.HEIGHT - 2)
            first.hardDrop()
            second.hardDrop()
        }
        assertEquals(first.current, second.current)
        assertEquals(first.score, second.score)
    }

    @Test
    fun resetClearsBoardAndScore() {
        val engine = BlocksEngine(seed = 8L)
        engine.setLockedCell(0, BlocksEngine.HEIGHT - 1, 1)
        engine.setCurrentPiece(Tetromino.O, 4, BlocksEngine.HEIGHT - 2)
        engine.hardDrop()
        engine.reset(seed = 8L)
        assertEquals(0, engine.score)
        assertEquals(0, engine.lines)
        assertFalse(engine.gameOver)
        assertEquals(0, engine.cellAt(0, BlocksEngine.HEIGHT - 1))
        assertEquals(0, engine.currentY)
    }
}
