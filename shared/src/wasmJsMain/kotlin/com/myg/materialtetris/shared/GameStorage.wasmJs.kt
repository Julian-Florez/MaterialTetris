package com.myg.materialtetris.shared

import kotlinx.browser.localStorage

class WasmGameStorage : GameStorage {

    override fun saveScore(score: Int) {
        try {
            localStorage.setItem("materialtetris_score", score.toString())
        } catch (_: Throwable) {
        }
    }

    override fun getScore(): Int {
        return localStorage.getItem("materialtetris_score")?.toIntOrNull() ?: 0
    }

    override fun saveBoard(boardStr: String) {
        try {
            localStorage.setItem("materialtetris_board", boardStr)
        } catch (_: Throwable) {
        }
    }

    override fun getBoard(): String? {
        return localStorage.getItem("materialtetris_board")
    }
}
