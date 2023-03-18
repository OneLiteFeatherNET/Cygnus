package net.onelitefeather.stardust.user

/**
 * case 0 -> this.value;
 * case 1 -> Integer.getInteger(this.value);
 * case 2 -> Boolean.getBoolean(this.value);
 * case 3 -> Double.parseDouble(this.value);
 * case 4 -> Float.parseFloat(this.value);
 */
enum class UserPropertyType(val friendlyName: String, val defaultValue: Any, val type: Byte) {

    FLYING("Flying", false, 2),
    VANISHED("Vanished", false, 2),
    VANISH_DISABLE_ITEM_DROP("No-Drop", true, 2),
    VANISH_DISABLE_ITEM_COLLECT("No-Collect", true, 2),
}

val USER_PROPERTY_TYPE_VALUES = UserPropertyType.values()

fun getDefaultUserProperties(): List<UserProperty> {
    return USER_PROPERTY_TYPE_VALUES.map { UserProperty(null, it.name.lowercase(), it.defaultValue.toString(), it.type) }
}


