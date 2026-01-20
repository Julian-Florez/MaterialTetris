package com.myg.materialtetris.shared

import kotlinx.browser.localStorage

class WasmGameStorage : GameStorage {

    override fun saveScore(score: Int) {
        localStorage.setItem("materialtetris_score", score.toString())
    }

    override fun getScore(): Int {
        return localStorage.getItem("materialtetris_score")?.toIntOrNull() ?: 0
    }

    override fun saveBoard(boardStr: String) {
        localStorage.setItem("materialtetris_board", boardStr)
    }

    override fun getBoard(): String? {
        return localStorage.getItem("materialtetris_board")
    }
}

