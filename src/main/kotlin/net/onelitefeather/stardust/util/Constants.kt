package net.onelitefeather.stardust.util;

import net.onelitefeather.stardust.user.User
import org.bukkit.util.Vector
import java.text.SimpleDateFormat

const val RADIUS_REMOVE_ENEMIES = 32.0
const val NOT_AVAILABLE_CONFIG_FALLBACK = "N/A (%s)"
const val DEFAULT_PLAYER_FIRE_TICKS = 0
const val DEFAULT_PLAYER_FOOD_LEVEL = 20
const val DEFAULT_ENTITY_HAS_VISUAL_FIRE = false
const val DEFAULT_PLAYER_SATURATION_LEVEL = 20.0F
const val VANISHED_METADATA_KEY = "vanished"

val DUMMY_VECTOR = Vector(0.0, 0.0, 0.0)
val DUMMY_USER = User(-1)
val DATE_FORMAT = SimpleDateFormat("dd.MM.yyyy")
