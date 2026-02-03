package com.amrdeveloper.linkhub.ui.folder

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.test.espresso.Espresso.pressBack
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.printToLog
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performTextClearance
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amrdeveloper.linkhub.data.Folder
import com.amrdeveloper.linkhub.data.FolderColor
import com.amrdeveloper.linkhub.data.Link
import com.amrdeveloper.linkhub.data.source.local.LinkRoomDatabase
import com.amrdeveloper.linkhub.ui.MainActivity
import com.amrdeveloper.linkhub.util.PasswordUtils
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PasswordProtectedTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var database: LinkRoomDatabase

    @Before
    fun setup() {
        database = LinkRoomDatabase.getDatabase(composeTestRule.activity)
        clearDatabase()
    }

    @After
    fun teardown() {
        clearDatabase()
    }

    private fun clearDatabase() {
        runBlocking {
            database.folderDao().deleteAll()
            database.linkDao().deleteAll()
        }
    }

    @Test
    fun passwordProtectedToggle_existsInFolderForm() {

        // Open the "Add Folder" screen
        composeTestRule.onNodeWithContentDescription("Add Item")
            .performClick()

        composeTestRule.onNodeWithContentDescription("New Folder")
            .performClick()

        // Wait for UI to update
        composeTestRule.waitForIdle()

        // Then: Password Protected toggle should be visible
        composeTestRule.onNodeWithText("Password Protected")
            .assertIsDisplayed()

        // Discard and go back to home screen
        composeTestRule.onNodeWithContentDescription("Delete")
            .performClick()

        // Open edit folder screen for an existing folder
        createFolder(name = "Test Folder")
        
        composeTestRule.waitForIdle()

        // When: Click on the folder (long click to edit)
        composeTestRule.onNodeWithText("Test Folder")
            .performTouchInput { longClick() }

        // Then: Password Protected toggle should be visible
        composeTestRule.onNodeWithText("Password Protected")
            .assertIsDisplayed()
    }

    @Test
    fun passwordProtectedFolder_validationWorks() {
        // Open the "Add Folder" screen
        composeTestRule.onNodeWithContentDescription("Add Item")
            .performClick()

        composeTestRule.onNodeWithContentDescription("New Folder")
            .performClick()

        composeTestRule.waitForIdle()

        // Enable Password Protection
        composeTestRule.onNodeWithContentDescription("Password Protected")
            .performClick()

        // Leave password empty and try to save
        composeTestRule.onNodeWithContentDescription("Save")
            .performClick()

        composeTestRule.waitForIdle()

        // Then: Validation error should be shown
        composeTestRule.onNodeWithText("Must be at least 5 characters")
            .assertIsDisplayed()

        // Enter a short password and try to save
        composeTestRule.onNodeWithContentDescription("Password Text")
            .performTextReplacement("123")

        composeTestRule.onNodeWithContentDescription("Confirm Password Text")
            .performTextReplacement("123")

        composeTestRule.onNodeWithContentDescription("Save")
            .performClick()

        // Then: Validation error for short password should be shown
        composeTestRule.onNodeWithText("Must be at least 5 characters")
            .assertIsDisplayed()

        // Enter passwords that do not match and try to save
        composeTestRule.onNodeWithContentDescription("Password Text")
            .performTextReplacement("12345")

        composeTestRule.onNodeWithContentDescription("Confirm Password Text")
            .performTextReplacement("54321")


        composeTestRule.onNodeWithContentDescription("Save")
            .performClick()

        // Then: Validation error for password that do not match should be shown
        composeTestRule.onNodeWithText("Passwords do not match")
            .assertIsDisplayed()

    }


    // Verify password dialog
    @Test
    fun passwordProtectedFolder_passwordDialogShownAndWorks() {
        // Create a password-protected folder
        val protectedFolder = createPasswordProtectedFolder(
            name = "Protected Folder",
            password = "test123"
        )

        composeTestRule.waitForIdle()

        // When: Open the protected folder
        composeTestRule.onNodeWithText("Protected Folder")
            .performClick()

        // Then: Password entry screen should be shown
        composeTestRule.onNodeWithText("Enter Password")
            .assertIsDisplayed()

        composeTestRule.onNodeWithText("OK")
            .assertIsDisplayed()

        composeTestRule.onNodeWithText("Cancel")
            .assertIsDisplayed()
            
        composeTestRule.onNodeWithContentDescription("Password Text")
            .performTextReplacement("test123")

        composeTestRule.onNodeWithText("OK")
            .performClick()

        composeTestRule.waitForIdle()

        // Then: Should navigate into the folder (no password prompt)
        composeTestRule.onNodeWithText("Protected Folder")
            .assertDoesNotExist()
    }


    // Verify children of password-protected folder have no password protected UI
    @Test
    fun passwordProtectedFolder_childrenHaveNoPasswordUI() {
        // Create a password-protected folder
        val protectedFolder = createPasswordProtectedFolder(
            name = "Protected Folder",
            password = "test123"
        )

        composeTestRule.waitForIdle()

        // When: Open the protected folder
        composeTestRule.onNodeWithText("Protected Folder")
            .performClick()

        // Then: Password entry screen should be shown
        composeTestRule.onNodeWithText("Enter Password")
            .assertIsDisplayed()

        // Enter correct password
        composeTestRule.onNodeWithContentDescription("Password Text")
            .performTextInput("test123")

        composeTestRule.onNodeWithText("OK")
            .performClick()

        composeTestRule.waitForIdle()

        // Open the "Add Folder" screen
        composeTestRule.onNodeWithContentDescription("Add Item")
            .performClick()

        composeTestRule.onNodeWithContentDescription("New Folder")
            .performClick()

        composeTestRule.waitForIdle()

        // Password Protection toggle should be disabled for child folders
        composeTestRule.onNodeWithContentDescription("Password Protected")
            .assertIsNotEnabled()

    }

    // Verify that unchecking password protection removes password
    @Test
    fun passwordProtectedFolder_removingPasswordWorks() {
        // Create a password-protected folder
        val protectedFolder = createPasswordProtectedFolder(
            name = "Protected Folder",
            password = "test123"
        )

        composeTestRule.waitForIdle()

        // When: Open edit folder screen for the protected folder
        composeTestRule.onNodeWithText("Protected Folder")
            .performTouchInput { longClick() }
        composeTestRule.waitForIdle()

        // Then: Password entry screen should be shown
        composeTestRule.onNodeWithText("Enter Password")
            .assertIsDisplayed()

        // Enter correct password
        composeTestRule.onNodeWithContentDescription("Password Text")
            .performTextInput("test123")

        composeTestRule.onNodeWithText("OK")
            .performClick()

        composeTestRule.waitForIdle()

        // Uncheck Password Protection
        composeTestRule.onNodeWithContentDescription("Password Protected")
            .performClick()

        // Save the folder
        composeTestRule.onNodeWithContentDescription("Save")
            .performClick()

        composeTestRule.waitForIdle()

        // Check the database to ensure password is removed
        runBlocking {
            val folderFromDb = database.folderDao().getFolderById(protectedFolder.id)
            assert(folderFromDb?.password == null)
        }

    }

    // Verify that edit form shows current passwords but masks them
    @Test
    fun passwordProtectedFolder_editFormShowsMaskedPassword() {
        // Create a password-protected folder
        val protectedFolder = createPasswordProtectedFolder(
            name = "Protected Folder",
            password = "test123"
        )

        composeTestRule.waitForIdle()

        // When: Open edit folder screen for the protected folder
        composeTestRule.onNodeWithText("Protected Folder")
            .performTouchInput { longClick() }
        composeTestRule.waitForIdle()

        // Then: Password entry screen should be shown
        composeTestRule.onNodeWithText("Enter Password")
            .assertIsDisplayed()

        // Enter correct password
        composeTestRule.onNodeWithContentDescription("Password Text")
            .performTextInput("test123")

        composeTestRule.onNodeWithText("OK")
            .performClick()

        composeTestRule.waitForIdle()

        // Both password fields should show 8 masked characters (8 bullets for "********")
        composeTestRule.onAllNodesWithText("••••••••")[0]
            .assertExists()
        
        composeTestRule.onAllNodesWithText("••••••••")[1]
            .assertExists()
    }

    @Test
    fun passwordProtectedFolder_changePasswordWorks() {
        // Create a password-protected folder
        val protectedFolder = createPasswordProtectedFolder(
            name = "Protected Folder",
            password = "test123"
        )

        composeTestRule.waitForIdle()

        // When: Open edit folder screen for the protected folder
        composeTestRule.onNodeWithText("Protected Folder")
            .performTouchInput { longClick() }
        composeTestRule.waitForIdle()

        // Then: Password entry screen should be shown
        composeTestRule.onNodeWithText("Enter Password")
            .assertIsDisplayed()

        // Enter correct password
        composeTestRule.onNodeWithContentDescription("Password Text")
            .performTextInput("test123")

        composeTestRule.onNodeWithText("OK")
            .performClick()

        composeTestRule.waitForIdle()

        // Change the password
        composeTestRule.onNodeWithContentDescription("Password Text")
            .performTextReplacement("newpass456")

        composeTestRule.onNodeWithContentDescription("Confirm Password Text")
            .performTextReplacement("newpass456")

        // Save the folder
        composeTestRule.onNodeWithContentDescription("Save")
            .performClick()

        composeTestRule.waitForIdle()

        // Folder is already unlocked, so check the database to ensure password is updated
        runBlocking {
            val folderFromDb = database.folderDao().getFolderById(protectedFolder.id)
            assert(folderFromDb?.password != null) { "Password should not be null" }
            assert(PasswordUtils.verifyPassword("newpass456", folderFromDb!!.password!!)) {
                "Password verification failed for 'newpass456'"
            }
        }   

    }

    // Verify correct password opens folder and incorrect shows error
    @Test
    fun passwordProtectedFolder_incorrectPasswordShowsError() {
        // Create a password-protected folder
        val protectedFolder = createPasswordProtectedFolder(
            name = "Protected Folder",
            password = "test123"
        )

        composeTestRule.waitForIdle()

        // When: Open the protected folder
        composeTestRule.onNodeWithText("Protected Folder")
            .performClick()

        // Then: Password entry screen should be shown
        composeTestRule.onNodeWithText("Enter Password")
            .assertIsDisplayed()

        // Enter incorrect password
        composeTestRule.onNodeWithContentDescription("Password Text")
            .performTextInput("wrongpass")

        composeTestRule.onNodeWithText("OK")
            .performClick()

        composeTestRule.waitForIdle()

        // Then: Error message should be shown
        composeTestRule.onNodeWithText("Incorrect password")
            .assertIsDisplayed()

        // Cancel and go back to home screen
        composeTestRule.onNodeWithText("Cancel")
            .performClick() 
        
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Enter Password")
            .assertDoesNotExist()
    }


    // Verify that password is only required once per session
    @Test
    fun passwordProtectedFolder_passwordRequiredOnce() {
        // Create a password-protected folder
        val protectedFolder = createPasswordProtectedFolder(
            name = "Protected Folder",
            password = "test123"
        )

        // Create child folder inside the protected folder
        createFolder(
            name = "Child Folder",
            folderId = protectedFolder.id
        )

        composeTestRule.waitForIdle()

        // When: Open the protected folder
        composeTestRule.onNodeWithText("Protected Folder")
            .performClick()

        // Then: Password entry screen should be shown
        composeTestRule.onNodeWithText("Enter Password")
            .assertIsDisplayed()

        // Enter correct password
        composeTestRule.onNodeWithContentDescription("Password Text")
            .performTextInput("test123")

        composeTestRule.onNodeWithText("OK")
            .performClick()

        composeTestRule.waitForIdle()

        // Then: Should navigate into the folder (no password prompt)
        composeTestRule.onNodeWithText("Protected Folder")
            .assertDoesNotExist()

        // Go back to home screen
        pressBack()

        composeTestRule.waitForIdle()

        // Re-open the same protected folder
        composeTestRule.onNodeWithText("Protected Folder")
            .performClick()

        composeTestRule.waitForIdle()

        // Then: Should navigate into the folder directly without password prompt
        composeTestRule.onNodeWithText("Enter Password")
            .assertDoesNotExist()

        composeTestRule.onNodeWithText("Child Folder")
            .assertIsDisplayed()

        composeTestRule.onNodeWithText("Child Folder")
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Enter Password")
            .assertDoesNotExist()

    }


    // Verify that search doesn't show entries from password-protected folders
    @Test
    fun passwordProtectedFolder_searchExcludesProtectedFolders() {
        // Create a password-protected & unlocked folder
        val protectedFolder = createPasswordProtectedFolder(
            name = "Protected Folder",
            password = "test123"
        )
        val unlockedFolder = createPasswordProtectedFolder(
            name = "Unlocked Folder",
            password = "test123"
        )
        unlockFolder("Unlocked Folder", "test123")

        // Create a link & folder inside the protected folder
        createLink(
            folderId = protectedFolder.id,
            title = "Secret Link",
            url = "https://secret.com"
        )
        createFolder(
            name = "Hidden Folder",
            folderId = protectedFolder.id
        )

        // Create a link & folder inside unlocked folder.
        createLink(
            folderId = unlockedFolder.id,
            title = "Unlocked Folder Link",
            url = "https://test.com"
        )
        createFolder(
            name = "Hidden Unlocked Folder",
            folderId = unlockedFolder.id
        )

        composeTestRule.waitForIdle()

        // When: Perform search for "Link"
        composeTestRule.onNodeWithContentDescription("Search")
            .performClick()

        composeTestRule.onNodeWithContentDescription("Search")
            .performTextInput("Link")

        composeTestRule.waitForIdle()

        // Public Link should show up in results.
        composeTestRule.onNodeWithText("Secret Link").assertDoesNotExist()

        composeTestRule.onNodeWithContentDescription("Search")
            .performTextReplacement("Folder")

        // Hidden folder should not show up in results.
        composeTestRule.onAllNodesWithText("Protected Folder").assertCountEquals(1)
        composeTestRule.onNodeWithText("Hidden Folder").assertDoesNotExist()

        composeTestRule.onAllNodesWithText("Unlocked Folder Link").assertCountEquals(2)
        composeTestRule.onAllNodesWithText("Hidden Unlocked Folder").assertCountEquals(2)


    }
    // Helper Functions

    private fun unlockFolder(folder: String, password: String) {
        composeTestRule.onNodeWithText(folder)
            .performClick()

        composeTestRule.onNodeWithText("Enter Password")
            .assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("Password Text")
            .performTextInput(password)

        composeTestRule.onNodeWithText("OK")
            .performClick()

        composeTestRule.waitForIdle()
    }

    /**
     * Creates a folder and inserts it into the database.
     * Returns the created folder with its assigned ID.
     */
    private fun createFolder(
        name: String = "Test Folder",
        folderId: Int = -1,
        isPinned: Boolean = false,
        folderColor: FolderColor = FolderColor.BLUE,
        password: String? = null,
        clickedCount: Int = 0
    ): Folder {
        val folder = Folder(
            name = name,
            folderId = folderId,
            isPinned = isPinned,
            folderColor = folderColor,
            password = password,
            clickedCount = clickedCount
        )
        val insertedId = runBlocking {
            database.folderDao().insert(folder)
        }
        return folder.copy(id = insertedId.toInt())
    }

    /**
     * Creates a password-protected folder and inserts it into the database.
     * The password is hashed before storing.
     */
    private fun createPasswordProtectedFolder(
        name: String = "Protected Folder",
        folderId: Int = -1,
        password: String = "test123",
        folderColor: FolderColor = FolderColor.RED
    ): Folder = createFolder(
        name = name,
        folderId = folderId,
        password = PasswordUtils.hashPassword(password),
        folderColor = folderColor
    )

    /**
     * Creates a link and inserts it into the database.
     * Returns the created link.
     */
    private fun createLink(
        title: String = "Test Link",
        subtitle: String = "Test Subtitle",
        url: String = "https://example.com",
        folderId: Int = -1,
        isPinned: Boolean = false,
        clickedCount: Int = 0
    ): Link {
        val link = Link(
            title = title,
            subtitle = subtitle,
            url = url,
            folderId = folderId,
            isPinned = isPinned,
            clickedCount = clickedCount
        )
        runBlocking {
            database.linkDao().insert(link)
        }
        return link
    }

    /**
     * Creates a link inside a specific folder and inserts it into the database.
     */
    private fun createLinkInFolder(
        folder: Folder,
        title: String = "Link in Folder",
        subtitle: String = "Subtitle",
        url: String = "https://example.com"
    ): Link = createLink(
        title = title,
        subtitle = subtitle,
        url = url,
        folderId = folder.id
    )

    // endregion
}
