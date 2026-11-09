/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.metrolist.music.R

@Immutable
sealed class Screens(
    @StringRes val titleId: Int,
    @DrawableRes val iconIdInactive: Int,
    @DrawableRes val iconIdActive: Int,
    val route: String,
) {
    object Home : Screens(
        titleId = R.string.listen_now,
        iconIdInactive = R.drawable.tab_listen_now,
        iconIdActive = R.drawable.tab_listen_now,
        route = "home"
    )

    object Browse : Screens(
        titleId = R.string.browse,
        iconIdInactive = R.drawable.tab_browse,
        iconIdActive = R.drawable.tab_browse,
        route = "browse_tab"
    )

    object Search : Screens(
        titleId = R.string.search,
        iconIdInactive = R.drawable.tab_search,
        iconIdActive = R.drawable.tab_search,
        route = "search_input"
    )

    object ListenTogether : Screens(
        titleId = R.string.together,
        iconIdInactive = R.drawable.group_outlined,
        iconIdActive = R.drawable.group_filled,
        route = "listen_together"
    )

    object Library : Screens(
        titleId = R.string.filter_library,
        iconIdInactive = R.drawable.tab_library,
        iconIdActive = R.drawable.tab_library,
        route = "library"
    )

    companion object {
        val MainScreens = listOf(Home, Browse, ListenTogether, Library, Search)
    }
}
