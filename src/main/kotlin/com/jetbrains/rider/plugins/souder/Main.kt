package com.jetbrains.rider.plugins.souder

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileEditor.impl.FileEditorOpenOptions
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile

val debounce: MutableList<VirtualFile> = mutableListOf()
var targetFile: VirtualFile? = null

fun debounce(file: VirtualFile, reset: Boolean = true): Unit? {
    if (debounce.contains(file)) {
        if (reset)
            debounce.remove(file)

        return null
    }

    return Unit
}

fun isValid(file: VirtualFile): Boolean? {
    if (file.extension == "cpp")
        return true
    else if (file.extension == "h")
        return false

    return null
}

fun findFilePair(file: VirtualFile): VirtualFile? {
    val ext = file.extension!!

    return LocalFileSystem.getInstance().findFileByPath(
        file.path.substring(
            0,
            file.path.length - ext.length - 1
        ) + if (ext == "h") ".cpp" else ".h"
    )
}

class EditorListener : FileEditorManagerListener {
    @Suppress("UnstableApiUsage")
    override fun fileOpened(manager: FileEditorManager, file: VirtualFile) {
        debounce(file) ?: return

        val isSource = isValid(file) ?: return
        val pairedFile = findFilePair(file) ?: return

        if (manager.isFileOpen(pairedFile) || !pairedFile.exists())
            return

        @Suppress("NAME_SHADOWING")
        val manager = FileEditorManagerEx.getInstanceEx(manager.project)

        if (manager.windows.size < 2) {
            manager.splitters.openInRightSplit(pairedFile, false)
            return
        }

        if (manager.currentWindow == null)
            return

        manager.openFile(pairedFile, manager.windows[if (isSource) 1 else 0])

        val targetWindow = manager.windows[if (isSource) 0 else 1]

        if (manager.currentWindow != targetWindow) {
            targetFile = file

            debounce.add(file)
            manager.closeFile(file)

            debounce.add(file)
            manager.openFile(file, targetWindow, options = FileEditorOpenOptions(requestFocus = true))
        }
    }

    override fun fileClosed(manager: FileEditorManager, file: VirtualFile) {
        debounce(file) ?: return
        isValid(file) ?: return

        val pairedFile = findFilePair(file) ?: return

        debounce.add(pairedFile)
        manager.closeFile(pairedFile)
    }

    @Suppress("UnstableApiUsage")
    override fun selectionChanged(event: FileEditorManagerEvent) {
        val file = event.newFile ?: return
        val oldFile = event.oldFile

        if (file == oldFile)
            return

        debounce(file, false) ?: return
        isValid(file) ?: return

        val pairedFile = findFilePair(file) ?: return
        val manager = FileEditorManagerEx.getInstanceEx(event.manager.project)

        if (pairedFile == oldFile || !manager.isFileOpen(pairedFile))
            return

        if (pairedFile == targetFile)
            manager.openFile(pairedFile, null, options = FileEditorOpenOptions(requestFocus = true))
        else
            manager.openFile(pairedFile)
    }
}
