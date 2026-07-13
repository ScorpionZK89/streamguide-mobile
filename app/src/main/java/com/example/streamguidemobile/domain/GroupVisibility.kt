package com.example.streamguidemobile.domain

fun isGroupVisible(group: String, hiddenGroups: Set<String>): Boolean =
    hiddenGroups.none { it.equals(group, ignoreCase = true) }
