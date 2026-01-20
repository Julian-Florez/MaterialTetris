package com.myg.materialtetris.shared

interface GameStorage {
    fun saveScore(score: Int)
    fun getScore(): Int
    fun saveBoard(boardStr: String)
    fun getBoard(): String?
}
