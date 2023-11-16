package com.payment.explorer.model

data class NavigationMenuData(
    val data: HashMap<Category, List<Tool>>
)

fun NavigationMenuData.getGroupList(): List<Category> {
    return this.data.keys.toList()
}