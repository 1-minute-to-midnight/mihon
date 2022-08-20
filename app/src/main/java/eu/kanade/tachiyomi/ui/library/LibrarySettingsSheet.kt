package eu.kanade.tachiyomi.ui.library

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.bluelinelabs.conductor.Router
import eu.kanade.domain.category.interactor.SetDisplayModeForCategory
import eu.kanade.domain.category.interactor.SetSortModeForCategory
import eu.kanade.domain.category.model.Category
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.ui.library.setting.DisplayModeSetting
import eu.kanade.tachiyomi.ui.library.setting.SortDirectionSetting
import eu.kanade.tachiyomi.ui.library.setting.SortModeSetting
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.widget.ExtendedNavigationView
import eu.kanade.tachiyomi.widget.ExtendedNavigationView.Item.TriStateGroup.State
import eu.kanade.tachiyomi.widget.sheet.TabbedBottomSheetDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class LibrarySettingsSheet(
    router: Router,
    private val trackManager: TrackManager = Injekt.get(),
    private val setDisplayModeForCategory: SetDisplayModeForCategory = Injekt.get(),
    private val setSortModeForCategory: SetSortModeForCategory = Injekt.get(),
    onGroupClickListener: (ExtendedNavigationView.Group) -> Unit,
) : TabbedBottomSheetDialog(router.activity!!) {

    val filters: Filter
    private val sort: Sort
    private val display: Display

    val sheetScope = CoroutineScope(Job() + Dispatchers.IO)

    init {
        filters = Filter(router.activity!!)
        filters.onGroupClicked = onGroupClickListener

        sort = Sort(router.activity!!)
        sort.onGroupClicked = onGroupClickListener

        display = Display(router.activity!!)
        display.onGroupClicked = onGroupClickListener
    }

    /**
     * adjusts selected button to match real state.
     * @param currentCategory ID of currently shown category
     */
    fun show(currentCategory: Category) {
        sort.currentCategory = currentCategory
        sort.adjustDisplaySelection()
        display.currentCategory = currentCategory
        display.adjustDisplaySelection()
        super.show()
    }

    override fun getTabViews(): List<View> = listOf(
        filters,
        sort,
        display,
    )

    override fun getTabTitles(): List<Int> = listOf(
        R.string.action_filter,
        R.string.action_sort,
        R.string.action_display,
    )

    /**
     * Filters group (unread, downloaded, ...).
     */
    inner class Filter @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
        Settings(context, attrs) {

        private val filterGroup = FilterGroup()

        init {
            setGroups(listOf(filterGroup))
        }

        /**
         * Returns true if there's at least one filter from [FilterGroup] active.
         */
        fun hasActiveFilters(): Boolean {
            return filterGroup.items.filterIsInstance<Item.TriStateGroup>().any { it.state != State.IGNORE.value }
        }

        inner class FilterGroup : Group {

            private val downloaded = Item.TriStateGroup(R.string.action_filter_downloaded, this)
            private val unread = Item.TriStateGroup(R.string.action_filter_unread, this)
            private val started = Item.TriStateGroup(R.string.action_filter_started, this)
            private val completed = Item.TriStateGroup(R.string.completed, this)
            private val trackFilters: Map<Long, Item.TriStateGroup>

            override val header = null
            override val items: List<Item>
            override val footer = null

            init {
                trackManager.services.filter { service -> service.isLogged }
                    .also { services ->
                        val size = services.size
                        trackFilters = services.associate { service ->
                            Pair(service.id, Item.TriStateGroup(getServiceResId(service, size), this))
                        }
                        val list: MutableList<Item> = mutableListOf(downloaded, unread, started, completed)
                        if (size > 1) list.add(Item.Header(R.string.action_filter_tracked))
                        list.addAll(trackFilters.values)
                        items = list
                    }
            }

            private fun getServiceResId(service: TrackService, size: Int): Int {
                return if (size > 1) service.nameRes() else R.string.action_filter_tracked
            }

            override fun initModels() {
                if (preferences.downloadedOnly().get()) {
                    downloaded.state = State.INCLUDE.value
                    downloaded.enabled = false
                } else {
                    downloaded.state = preferences.filterDownloaded().get()
                }
                unread.state = preferences.filterUnread().get()
                started.state = preferences.filterStarted().get()
                completed.state = preferences.filterCompleted().get()

                trackFilters.forEach { trackFilter ->
                    trackFilter.value.state = preferences.filterTracking(trackFilter.key.toInt()).get()
                }
            }

            override fun onItemClicked(item: Item) {
                item as Item.TriStateGroup
                val newState = when (item.state) {
                    State.IGNORE.value -> State.INCLUDE.value
                    State.INCLUDE.value -> State.EXCLUDE.value
                    State.EXCLUDE.value -> State.IGNORE.value
                    else -> throw Exception("Unknown State")
                }
                item.state = newState
                when (item) {
                    downloaded -> preferences.filterDownloaded().set(newState)
                    unread -> preferences.filterUnread().set(newState)
                    started -> preferences.filterStarted().set(newState)
                    completed -> preferences.filterCompleted().set(newState)
                    else -> {
                        trackFilters.forEach { trackFilter ->
                            if (trackFilter.value == item) {
                                preferences.filterTracking(trackFilter.key.toInt()).set(newState)
                            }
                        }
                    }
                }

                adapter.notifyItemChanged(item)
            }
        }
    }

    /**
     * Sorting group (alphabetically, by last read, ...) and ascending or descending.
     */
    inner class Sort @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
        Settings(context, attrs) {

        private val sort = SortGroup()

        init {
            setGroups(listOf(sort))
        }

        // Refreshes Display Setting selections
        fun adjustDisplaySelection() {
            sort.initModels()
            sort.items.forEach { adapter.notifyItemChanged(it) }
        }

        inner class SortGroup : Group {

            private val alphabetically = Item.MultiSort(R.string.action_sort_alpha, this)
            private val total = Item.MultiSort(R.string.action_sort_total, this)
            private val lastRead = Item.MultiSort(R.string.action_sort_last_read, this)
            private val lastChecked = Item.MultiSort(R.string.action_sort_last_manga_update, this)
            private val unread = Item.MultiSort(R.string.action_sort_unread_count, this)
            private val latestChapter = Item.MultiSort(R.string.action_sort_latest_chapter, this)
            private val chapterFetchDate = Item.MultiSort(R.string.action_sort_chapter_fetch_date, this)
            private val dateAdded = Item.MultiSort(R.string.action_sort_date_added, this)

            override val header = null
            override val items =
                listOf(alphabetically, lastRead, lastChecked, unread, total, latestChapter, chapterFetchDate, dateAdded)
            override val footer = null

            override fun initModels() {
                val sorting = SortModeSetting.get(preferences, currentCategory)
                val order = if (SortDirectionSetting.get(currentCategory) == SortDirectionSetting.ASCENDING) {
                    Item.MultiSort.SORT_ASC
                } else {
                    Item.MultiSort.SORT_DESC
                }

                alphabetically.state =
                    if (sorting == SortModeSetting.ALPHABETICAL) order else Item.MultiSort.SORT_NONE
                lastRead.state =
                    if (sorting == SortModeSetting.LAST_READ) order else Item.MultiSort.SORT_NONE
                lastChecked.state =
                    if (sorting == SortModeSetting.LAST_MANGA_UPDATE) order else Item.MultiSort.SORT_NONE
                unread.state =
                    if (sorting == SortModeSetting.UNREAD_COUNT) order else Item.MultiSort.SORT_NONE
                total.state =
                    if (sorting == SortModeSetting.TOTAL_CHAPTERS) order else Item.MultiSort.SORT_NONE
                latestChapter.state =
                    if (sorting == SortModeSetting.LATEST_CHAPTER) order else Item.MultiSort.SORT_NONE
                chapterFetchDate.state =
                    if (sorting == SortModeSetting.CHAPTER_FETCH_DATE) order else Item.MultiSort.SORT_NONE
                dateAdded.state =
                    if (sorting == SortModeSetting.DATE_ADDED) order else Item.MultiSort.SORT_NONE
            }

            override fun onItemClicked(item: Item) {
                item as Item.MultiStateGroup
                val prevState = item.state

                item.group.items.forEach {
                    (it as Item.MultiStateGroup).state =
                        Item.MultiSort.SORT_NONE
                }
                item.state = when (prevState) {
                    Item.MultiSort.SORT_NONE -> Item.MultiSort.SORT_ASC
                    Item.MultiSort.SORT_ASC -> Item.MultiSort.SORT_DESC
                    Item.MultiSort.SORT_DESC -> Item.MultiSort.SORT_ASC
                    else -> throw Exception("Unknown state")
                }

                setSortPreference(item)

                item.group.items.forEach { adapter.notifyItemChanged(it) }
            }

            private fun setSortPreference(item: Item.MultiStateGroup) {
                val mode = when (item) {
                    alphabetically -> SortModeSetting.ALPHABETICAL
                    lastRead -> SortModeSetting.LAST_READ
                    lastChecked -> SortModeSetting.LAST_MANGA_UPDATE
                    unread -> SortModeSetting.UNREAD_COUNT
                    total -> SortModeSetting.TOTAL_CHAPTERS
                    latestChapter -> SortModeSetting.LATEST_CHAPTER
                    chapterFetchDate -> SortModeSetting.CHAPTER_FETCH_DATE
                    dateAdded -> SortModeSetting.DATE_ADDED
                    else -> throw NotImplementedError("Unknown display mode")
                }
                val direction = if (item.state == Item.MultiSort.SORT_ASC) {
                    SortDirectionSetting.ASCENDING
                } else {
                    SortDirectionSetting.DESCENDING
                }

                sheetScope.launchIO {
                    setSortModeForCategory.await(currentCategory!!, mode, direction)
                }
            }
        }
    }

    /**
     * Display group, to show the library as a list or a grid.
     */
    inner class Display @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
        Settings(context, attrs) {

        private val displayGroup: DisplayGroup
        private val badgeGroup: BadgeGroup
        private val tabsGroup: TabsGroup

        init {
            displayGroup = DisplayGroup()
            badgeGroup = BadgeGroup()
            tabsGroup = TabsGroup()
            setGroups(listOf(displayGroup, badgeGroup, tabsGroup))
        }

        // Refreshes Display Setting selections
        fun adjustDisplaySelection() {
            val mode = getDisplayModePreference()
            displayGroup.setGroupSelections(mode)
            displayGroup.items.forEach { adapter.notifyItemChanged(it) }
        }

        // Gets user preference of currently selected display mode at current category
        private fun getDisplayModePreference(): DisplayModeSetting {
            return if (currentCategory != null && preferences.categorizedDisplaySettings().get()) {
                DisplayModeSetting.fromFlag(currentCategory!!.displayMode)
            } else {
                preferences.libraryDisplayMode().get()
            }
        }

        inner class DisplayGroup : Group {

            private val compactGrid = Item.Radio(R.string.action_display_grid, this)
            private val comfortableGrid = Item.Radio(R.string.action_display_comfortable_grid, this)
            private val coverOnlyGrid = Item.Radio(R.string.action_display_cover_only_grid, this)
            private val list = Item.Radio(R.string.action_display_list, this)

            override val header = Item.Header(R.string.action_display_mode)
            override val items = listOf(compactGrid, comfortableGrid, coverOnlyGrid, list)
            override val footer = null

            override fun initModels() {
                val mode = getDisplayModePreference()
                setGroupSelections(mode)
            }

            override fun onItemClicked(item: Item) {
                item as Item.Radio
                if (item.checked) return

                item.group.items.forEach { (it as Item.Radio).checked = false }
                item.checked = true

                setDisplayModePreference(item)

                item.group.items.forEach { adapter.notifyItemChanged(it) }
            }

            // Sets display group selections based on given mode
            fun setGroupSelections(mode: DisplayModeSetting) {
                compactGrid.checked = mode == DisplayModeSetting.COMPACT_GRID
                comfortableGrid.checked = mode == DisplayModeSetting.COMFORTABLE_GRID
                coverOnlyGrid.checked = mode == DisplayModeSetting.COVER_ONLY_GRID
                list.checked = mode == DisplayModeSetting.LIST
            }

            private fun setDisplayModePreference(item: Item) {
                val flag = when (item) {
                    compactGrid -> DisplayModeSetting.COMPACT_GRID
                    comfortableGrid -> DisplayModeSetting.COMFORTABLE_GRID
                    coverOnlyGrid -> DisplayModeSetting.COVER_ONLY_GRID
                    list -> DisplayModeSetting.LIST
                    else -> throw NotImplementedError("Unknown display mode")
                }

                sheetScope.launchIO {
                    setDisplayModeForCategory.await(currentCategory!!, flag)
                }
            }
        }

        inner class BadgeGroup : Group {
            private val downloadBadge = Item.CheckboxGroup(R.string.action_display_download_badge, this)
            private val unreadBadge = Item.CheckboxGroup(R.string.action_display_unread_badge, this)
            private val localBadge = Item.CheckboxGroup(R.string.action_display_local_badge, this)
            private val languageBadge = Item.CheckboxGroup(R.string.action_display_language_badge, this)

            override val header = Item.Header(R.string.badges_header)
            override val items = listOf(downloadBadge, unreadBadge, localBadge, languageBadge)
            override val footer = null

            override fun initModels() {
                downloadBadge.checked = preferences.downloadBadge().get()
                unreadBadge.checked = preferences.unreadBadge().get()
                localBadge.checked = preferences.localBadge().get()
                languageBadge.checked = preferences.languageBadge().get()
            }

            override fun onItemClicked(item: Item) {
                item as Item.CheckboxGroup
                item.checked = !item.checked
                when (item) {
                    downloadBadge -> preferences.downloadBadge().set((item.checked))
                    unreadBadge -> preferences.unreadBadge().set((item.checked))
                    localBadge -> preferences.localBadge().set((item.checked))
                    languageBadge -> preferences.languageBadge().set((item.checked))
                    else -> {}
                }
                adapter.notifyItemChanged(item)
            }
        }

        inner class TabsGroup : Group {
            private val showTabs = Item.CheckboxGroup(R.string.action_display_show_tabs, this)
            private val showNumberOfItems = Item.CheckboxGroup(R.string.action_display_show_number_of_items, this)

            override val header = Item.Header(R.string.tabs_header)
            override val items = listOf(showTabs, showNumberOfItems)
            override val footer = null

            override fun initModels() {
                showTabs.checked = preferences.categoryTabs().get()
                showNumberOfItems.checked = preferences.categoryNumberOfItems().get()
            }

            override fun onItemClicked(item: Item) {
                item as Item.CheckboxGroup
                item.checked = !item.checked
                when (item) {
                    showTabs -> preferences.categoryTabs().set(item.checked)
                    showNumberOfItems -> preferences.categoryNumberOfItems().set(item.checked)
                    else -> {}
                }
                adapter.notifyItemChanged(item)
            }
        }
    }

    open inner class Settings(context: Context, attrs: AttributeSet?) :
        ExtendedNavigationView(context, attrs) {

        val preferences: PreferencesHelper by injectLazy()
        lateinit var adapter: Adapter

        /**
         * Click listener to notify the parent fragment when an item from a group is clicked.
         */
        var onGroupClicked: (Group) -> Unit = {}

        var currentCategory: Category? = null

        fun setGroups(groups: List<Group>) {
            adapter = Adapter(groups.map { it.createItems() }.flatten())
            recycler.adapter = adapter

            groups.forEach { it.initModels() }
            addView(recycler)
        }

        /**
         * Adapter of the recycler view.
         */
        inner class Adapter(items: List<Item>) : ExtendedNavigationView.Adapter(items) {

            override fun onItemClicked(item: Item) {
                if (item is GroupedItem) {
                    item.group.onItemClicked(item)
                    onGroupClicked(item.group)
                }
            }
        }
    }
}
