package net.onelitefeather.stardust.api

import net.kyori.adventure.text.Component

interface ItemSignService<I, P> {

    fun sign(baseItemStack: I, lore: List<Component>, player: P): I

    fun removeSignature(baseItemStack: I, player: P): I

    fun hasSigned(itemStack: I, player: P): Boolean

}