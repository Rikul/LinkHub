package com.amrdeveloper.linkhub.ui.folder

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.amrdeveloper.linkhub.R
import com.amrdeveloper.linkhub.common.TaskState
import com.amrdeveloper.linkhub.data.Folder
import com.amrdeveloper.linkhub.data.FolderColor
import com.amrdeveloper.linkhub.ui.components.FolderSelector
import com.amrdeveloper.linkhub.ui.components.PinnedSwitch
import com.amrdeveloper.linkhub.ui.components.SaveDeleteActionsRow
import com.amrdeveloper.linkhub.util.CREATED_FOLDER_NAME_KEY
import com.amrdeveloper.linkhub.util.FOLDER_NONE_ID
import com.amrdeveloper.linkhub.util.PasswordUtils
import com.amrdeveloper.linkhub.util.UiPreferences

private val artificialNoneFolder = Folder(name = "None", id = FOLDER_NONE_ID)

@Composable
fun FolderScreen(
    currentFolder: Folder?,
    viewModel: FolderViewModel = viewModel(),
    uiPreferences: UiPreferences,
    navController: NavController,
    initialParentFolderId: Int? = null
) {
    val taskState = viewModel.taskState
    val folder = currentFolder ?: Folder(name = "").apply { folderColor = FolderColor.BLUE }
    var folderName by remember { mutableStateOf(value = folder.name) }
    var folderNameErrorMessage by remember { mutableStateOf(value = if (folder.name.isEmpty()) "Name can't be empty" else "") }

    val foldersState = viewModel.selectSortedFoldersState.collectAsStateWithLifecycle()
    var selectedFolder by remember { mutableStateOf(value = artificialNoneFolder) }
    var selectedFolderDry by remember { mutableStateOf(value = true) }

    var isPasswordProtected by remember { mutableStateOf(folder.password != null) }
    // Show 8 asterisks for existing passwords instead of the hashed value
    var password by remember { mutableStateOf(if (folder.password != null) "********" else "") }
    var confirmPassword by remember { mutableStateOf(if (folder.password != null) "********" else "") }
    var passwordErrorMessage by remember { mutableStateOf("") }
    val isParentProtected = selectedFolder.password != null

    val createOrUpdateFolder = {
        if (currentFolder == null) {
            viewModel.createNewFolder(folder)

            // Store the created folder name only if previous fragment is LinkFragment
            val previousFragment = navController.previousBackStackEntry?.destination?.id
            if (previousFragment == R.id.linkFragment) {
                navController.previousBackStackEntry?.savedStateHandle?.set(
                    CREATED_FOLDER_NAME_KEY, folder.name
                )
            }
        } else {
            viewModel.updateFolder(folder)
        }
    }

    val passwordsNotMatchError = stringResource(R.string.error_passwords_not_match)
    val passwordTooShortError = stringResource(R.string.error_password_too_short)

    val validateAndSave = {
        var isValid = true
        if (isPasswordProtected && !isParentProtected) {
            if (password != confirmPassword) {
                passwordErrorMessage = passwordsNotMatchError
                isValid = false
            } else if (password.length < 5) {
                passwordErrorMessage = passwordTooShortError
                isValid = false
            } else {
                // Only hash if password changed (not the placeholder "********")
                if (password != "********") {
                    folder.password = PasswordUtils.hashPassword(password)
                }
                // If it's still "********", folder.password already contains the original hash
            }
        } else {
            folder.password = null
        }

        if (folderNameErrorMessage.isNotEmpty()) isValid = false

        if (isValid) {
            createOrUpdateFolder()
        }
    }

    BackHandler(enabled = true) {
        if (uiPreferences.isAutoSavingEnabled()) {
            validateAndSave()
            return@BackHandler
        }

        navController.popBackStack()
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            SaveDeleteActionsRow(
                onSaveActionClick = {
                    validateAndSave()
                },
                onDeleteActionClick = {
                    if (currentFolder == null) {
                        navController.popBackStack()
                        return@SaveDeleteActionsRow
                    }

                    viewModel.deleteFolder(folder.id)

                    // Reset the default folder if it deleted
                    if (uiPreferences.isDefaultFolderEnabled() &&
                        uiPreferences.getDefaultFolderId() == folder.id
                    ) {
                        uiPreferences.deleteDefaultFolder()
                    }
                }
            )

            FolderHeaderIcon()

            OutlinedTextField(
                value = folderName,
                onValueChange = {
                    folderName = it.trim()
                    folder.name = folderName
                    if (folderName.isEmpty()) {
                        folderNameErrorMessage = "Name can't be empty"
                        return@OutlinedTextField
                    }

                    if (folderName.length < 3) {
                        folderNameErrorMessage = "Folder name can't be less than 3 characters"
                        return@OutlinedTextField
                    }

                    folderNameErrorMessage = ""
                },
                label = { Text(text = "Name") },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_directory_blue),
                        contentDescription = "Folder Icon",
                        tint = Color.Unspecified,
                    )
                },
                trailingIcon = {
                    if (folderName.isNotEmpty()) {
                        IconButton(onClick = { folderName = "" }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_delete),
                                contentDescription = "Clear Icon"
                            )
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                isError = folderNameErrorMessage.isNotEmpty(),
                supportingText = {
                    if (folderNameErrorMessage.isNotEmpty()) {
                        Text(text = folderNameErrorMessage)
                    }
                })

            val folders = foldersState.value.data.toMutableList()
            folders.addAll(0, listOf(artificialNoneFolder))

            if (selectedFolderDry) {
                val providedFolder = initialParentFolderId?.takeIf { it >= 0 }
                    ?.let { id -> folders.find { it.id == id } }
                val defaultFolder = if (uiPreferences.isDefaultFolderEnabled()) {
                    val defFolderId = uiPreferences.getDefaultFolderId()
                    folders.find { it.id == defFolderId }
                } else null
                val linkedFolder = folders.find { it.id == folder.folderId }
                selectedFolder = providedFolder
                    ?: defaultFolder
                    ?: linkedFolder
                    ?: folders.firstOrNull()
                    ?: artificialNoneFolder
                folder.folderId = selectedFolder.id
            }

            FolderSelector(selectedFolder = selectedFolder, folders = folders) {
                folder.folderId = it.id
                selectedFolder = it
                selectedFolderDry = false
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.password_protected),
                    modifier = Modifier.weight(1f),
                    color = colorResource(R.color.light_blue_600),
                    style = MaterialTheme.typography.titleMedium,
                )
                Switch(
                    checked = if (isParentProtected) false else isPasswordProtected,
                    onCheckedChange = { isPasswordProtected = it },
                    enabled = !isParentProtected,
                    modifier = Modifier.semantics { contentDescription = "Password Protected" }
                )
            }

            if (isPasswordProtected && !isParentProtected) {
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        passwordErrorMessage = ""
                    },
                    label = { Text(text = stringResource(R.string.password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier
                        .semantics { contentDescription = "Password Text" }
                        .fillMaxWidth()
                        .padding(8.dp),
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        passwordErrorMessage = ""
                    },
                    label = { Text(text = stringResource(R.string.confirm_password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier
                        .semantics { contentDescription = "Confirm Password Text" }
                        .fillMaxWidth()
                        .padding(8.dp),
                    isError = passwordErrorMessage.isNotEmpty(),
                    supportingText = {
                        if (passwordErrorMessage.isNotEmpty()) {
                            Text(text = passwordErrorMessage)
                        }
                    })
            }

            PinnedSwitch(isChecked = folder.isPinned) { isChecked ->
                folder.isPinned = isChecked
            }

            FolderColorSelector(
                colors = FolderColor.entries.drop(1),
                initIndex = if (folder.folderColor.ordinal > 0) folder.folderColor.ordinal - 1 else 0
            ) { selectedColor ->
                folder.folderColor = selectedColor
            }
        }
    }

    when (taskState) {
        TaskState.Success -> {
            navController.popBackStack()
        }

        is TaskState.Error -> {
            folderNameErrorMessage = stringResource(taskState.message)
        }

        TaskState.Idle -> {}
    }
}

@Composable
fun FolderHeaderIcon() {
    Icon(
        painter = painterResource(R.drawable.ic_folders),
        contentDescription = "Folder Icon",
        tint = Color.Unspecified,
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
    )
}

@Composable
fun FolderColorSelector(
    colors: List<FolderColor>, initIndex: Int = 0, onSelectedChange: (FolderColor) -> Unit = {}
) {
    var selectedIndex by remember { mutableStateOf(value = initIndex) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Color",
            modifier = Modifier.weight(1f),
            color = colorResource(R.color.light_blue_600),
            style = MaterialTheme.typography.titleMedium,
        )


        IconButton(
            onClick = {
                selectedIndex -= 1
                if (selectedIndex < 0) selectedIndex = colors.lastIndex
                onSelectedChange(colors[selectedIndex])
            }, modifier = Modifier.size(24.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = "Previous Icon",
                tint = Color.Unspecified,
            )
        }

        Icon(
            painter = painterResource(colors[selectedIndex].drawableId),
            contentDescription = colors[selectedIndex].name,
            modifier = Modifier.padding(horizontal = 2.dp),
            tint = Color.Unspecified
        )

        IconButton(
            onClick = {
                selectedIndex = (selectedIndex + 1) % colors.size
                onSelectedChange(colors[selectedIndex])
            }, modifier = Modifier.size(24.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_forward),
                contentDescription = "Next Icon",
                tint = Color.Unspecified
            )
        }
    }
}
