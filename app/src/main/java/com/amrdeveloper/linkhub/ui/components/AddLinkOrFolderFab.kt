@file:OptIn(ExperimentalMaterial3Api::class)

package com.amrdeveloper.linkhub.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import com.amrdeveloper.linkhub.R
import com.amrdeveloper.linkhub.util.FOLDER_NONE_ID

@Composable
fun AddLinkOrFolderFab(navController: NavController, parentFolderId: Int? = null) {
    var expanded by remember { mutableStateOf(false) }
    val targetFolderId = parentFolderId ?: FOLDER_NONE_ID
    val primaryColor = MaterialTheme.colorScheme.primary
    val fabContainerColor = if (primaryColor.luminance() < 0.2f) {
        MaterialTheme.colorScheme.secondary
    } else {
        primaryColor
    }

    Column(horizontalAlignment = Alignment.End) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                AddMenuChip(
                    label = "New Link",
                    iconRes = R.drawable.ic_link,
                    contentDescription = "New Link",
                    onClick = {
                        navController.navigate(
                            R.id.linkFragment,
                            bundleOf("parentFolderId" to targetFolderId)
                        )
                        expanded = false
                    }
                )

                AddMenuChip(
                    label = "New Folder",
                    iconRes = R.drawable.ic_folders,
                    contentDescription = "New Folder",
                    onClick = {
                        navController.navigate(
                            R.id.folderFragment,
                            bundleOf("parentFolderId" to targetFolderId)
                        )
                        expanded = false
                    }
                )
            }
        }

        FloatingActionButton(
            onClick = { expanded = !expanded },
            containerColor = fabContainerColor,
            contentColor = Color.White
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_add),
                contentDescription = "Add Item",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun AddMenuChip(
    label: String,
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit
) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = contentDescription,
                modifier = Modifier.size(18.dp)
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onSurface,
            leadingIconContentColor = MaterialTheme.colorScheme.primary
        )
    )
}
