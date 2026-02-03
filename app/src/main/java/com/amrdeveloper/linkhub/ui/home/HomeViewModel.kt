package com.amrdeveloper.linkhub.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amrdeveloper.linkhub.common.LazyValue
import com.amrdeveloper.linkhub.data.Folder
import com.amrdeveloper.linkhub.data.Link
import com.amrdeveloper.linkhub.data.source.FolderRepository
import com.amrdeveloper.linkhub.data.source.LinkRepository
import com.amrdeveloper.linkhub.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val NUMBER_OF_TOP_FOLDERS = 6

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val folderRepository: FolderRepository,
    private val linkRepository: LinkRepository,
) : ViewModel() {

    private val allFoldersFlow = folderRepository.getSortedFolders()

    val mostUsedLimitedFoldersState: StateFlow<LazyValue<List<Folder>>> =
        combine(
            folderRepository.getSortedFolders(limit = NUMBER_OF_TOP_FOLDERS),
            allFoldersFlow
        ) { topFolders, allFolders ->
            val foldersMap = allFolders.associateBy { it.id }
            topFolders.filter { folder ->
                SessionManager.isParentChainAccessible(folder.folderId, foldersMap)
            }
        }.map { LazyValue(data = it, isLoading = false) }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000L),
            initialValue = LazyValue(data = listOf(), isLoading = true)
        )

    val sortedLinksState: StateFlow<LazyValue<List<Link>>> =
        combine(linkRepository.getSortedLinks(), allFoldersFlow) { links, allFolders ->
            val foldersMap = allFolders.associateBy { it.id }
            links.filter { link ->
                SessionManager.isParentChainAccessible(link.folderId, foldersMap)
            }
        }.map { LazyValue(data = it, isLoading = false) }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000L),
            initialValue = LazyValue(data = listOf(), isLoading = true)
        )

    fun incrementLinkClickCount(link: Link) {
        viewModelScope.launch {
            linkRepository.updateClickCountByLinkId(link.id, link.clickedCount.plus(1))
        }
    }

    fun incrementFolderClickCount(folder: Folder) {
        viewModelScope.launch {
            folderRepository.updateClickCountByFolderId(folder.id, folder.clickedCount.plus(1))
        }
    }
}