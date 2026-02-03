package com.amrdeveloper.linkhub.util

import com.amrdeveloper.linkhub.data.Folder

object SessionManager {
    private val unlockedFolderIds = mutableSetOf<Int>()

    fun unlockFolder(folderId: Int) {
        unlockedFolderIds.add(folderId)
    }

    fun isFolderUnlocked(folderId: Int): Boolean {
        return unlockedFolderIds.contains(folderId)
    }

    fun clear() {
        unlockedFolderIds.clear()
    }

    /**
     * Check if a folder is accessible (unlocked or has no password).
     * Also checks parent folder chain for inherited locks.
     */
    fun isFolderAccessible(folder: Folder, allFolders: List<Folder>): Boolean {
        if (folder.password == null) return true
        if (isFolderUnlocked(folder.id)) return true

        // Check parent inheritance - if parent is unlocked, child is accessible
        if (folder.folderId != -1) {
            val parent = allFolders.find { it.id == folder.folderId }
            if (parent != null && isFolderAccessible(parent, allFolders)) {
                return true
            }
        }
        return false
    }

    /**
     * Check if a folder's parent chain is unlocked.
     * Used to filter folders whose parents are locked.
     */
    fun isParentChainAccessible(folderId: Int, foldersMap: Map<Int, Folder>): Boolean {
        if (folderId == -1) return true
        val parent = foldersMap[folderId] ?: return true
        if (parent.password != null && !isFolderUnlocked(parent.id)) return false
        return isParentChainAccessible(parent.folderId, foldersMap)
    }
}

