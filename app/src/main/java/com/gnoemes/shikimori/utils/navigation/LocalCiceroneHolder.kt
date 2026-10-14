package com.gnoemes.shikimori.utils.navigation

import ru.terrakok.cicerone.Cicerone
import ru.terrakok.cicerone.Router

class LocalCiceroneHolder {

    private val containers = HashMap<String, Cicerone<Router>>()

    fun getCicerone(containerTag: String): Cicerone<Router> {
        if (!containers.containsKey(containerTag)) {
            containers[containerTag] = Cicerone.create()
        }
        return containers[containerTag]!!
    }
}