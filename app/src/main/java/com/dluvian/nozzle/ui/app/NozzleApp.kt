package com.dluvian.nozzle.ui.app

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.DrawerValue
import androidx.compose.material.ModalDrawer
import androidx.compose.material.Surface
import androidx.compose.material.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.dluvian.nozzle.AppContainer
import com.dluvian.nozzle.ui.app.navigation.NozzleNavActions
import com.dluvian.nozzle.ui.app.navigation.NozzleNavGraph
import com.dluvian.nozzle.ui.app.views.drawer.NozzleDrawerRoute
import com.dluvian.nozzle.ui.app.views.drawer.NozzleDrawerViewModel
import com.dluvian.nozzle.ui.app.views.editProfile.EditProfileViewModel
import com.dluvian.nozzle.ui.app.views.feed.FeedViewModel
import com.dluvian.nozzle.ui.app.views.hashtag.HashtagViewModel
import com.dluvian.nozzle.ui.app.views.keys.KeysViewModel
import com.dluvian.nozzle.ui.app.views.post.PostViewModel
import com.dluvian.nozzle.ui.app.views.profile.ProfileViewModel
import com.dluvian.nozzle.ui.app.views.reply.ReplyViewModel
import com.dluvian.nozzle.ui.app.views.search.SearchViewModel
import com.dluvian.nozzle.ui.app.views.thread.ThreadViewModel
import com.dluvian.nozzle.ui.theme.NozzleTheme
import kotlinx.coroutines.launch

@Composable
fun NozzleApp(appContainer: AppContainer) {
    NozzleTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            val vmContainer = VMContainer(
                drawerViewModel = viewModel(
                    factory = NozzleDrawerViewModel.provideFactory(
                        personalProfileProvider = appContainer.personalProfileManager,
                        nozzleSubscriber = appContainer.nozzleSubscriber
                    )
                ),
                editProfileViewModel = viewModel(
                    factory = EditProfileViewModel.provideFactory(
                        personalProfileManager = appContainer.personalProfileManager,
                        nostrService = appContainer.nostrService,
                        context = LocalContext.current,
                    )
                ),
                profileViewModel = viewModel(
                    factory = ProfileViewModel.provideFactory(
                        postCardInteractor = appContainer.postCardInteractor,
                        profileFollower = appContainer.profileFollower,
                        feedProvider = appContainer.feedProvider,
                        relayProvider = appContainer.relayProvider,
                        profileProvider = appContainer.profileWithMetaProvider,
                        pubkeyProvider = appContainer.keyManager,
                        clickedMediaUrlCache = appContainer.clickedMediaUrlCache,
                        contactListProvider = appContainer.contactListProvider,
                        context = LocalContext.current,
                        clip = LocalClipboardManager.current,
                    )
                ),
                keysViewModel = viewModel(
                    factory = KeysViewModel.provideFactory(
                        keyManager = appContainer.keyManager,
                        personalProfileManager = appContainer.personalProfileManager,
                        nozzleSubscriber = appContainer.nozzleSubscriber,
                        context = LocalContext.current,
                        clip = LocalClipboardManager.current,
                    )
                ),
                feedViewModel = viewModel(
                    factory = FeedViewModel.provideFactory(
                        clickedMediaUrlCache = appContainer.clickedMediaUrlCache,
                        postCardInteractor = appContainer.postCardInteractor,
                        personalProfileProvider = appContainer.personalProfileManager,
                        feedProvider = appContainer.feedProvider,
                        relayProvider = appContainer.relayProvider,
                        autopilotProvider = appContainer.autopilotProvider,
                        feedSettingsPreferences = appContainer.feedSettingsPreferences,
                    )
                ),
                threadViewModel = viewModel(
                    factory = ThreadViewModel.provideFactory(
                        threadProvider = appContainer.threadProvider,
                        clickedMediaUrlCache = appContainer.clickedMediaUrlCache,
                        postCardInteractor = appContainer.postCardInteractor,
                    )
                ),
                replyViewModel = viewModel(
                    factory = ReplyViewModel.provideFactory(
                        nostrService = appContainer.nostrService,
                        personalProfileProvider = appContainer.personalProfileManager,
                        relayProvider = appContainer.relayProvider,
                        postDao = appContainer.roomDb.postDao(),
                        context = LocalContext.current,
                    )
                ),
                postViewModel = viewModel(
                    factory = PostViewModel.provideFactory(
                        nostrService = appContainer.nostrService,
                        personalProfileProvider = appContainer.personalProfileManager,
                        relayProvider = appContainer.relayProvider,
                        postDao = appContainer.roomDb.postDao(),
                        context = LocalContext.current,
                    )
                ),
                searchViewModel = viewModel(
                    factory = SearchViewModel.provideFactory(
                        nip05Resolver = appContainer.nip05Resolver
                    )
                ),
                hashtagViewModel = viewModel(
                    factory = HashtagViewModel.provideFactory(
                        clickedMediaUrlCache = appContainer.clickedMediaUrlCache,
                        postCardInteractor = appContainer.postCardInteractor,
                        feedProvider = appContainer.feedProvider,
                        relayProvider = appContainer.relayProvider,
                    )
                ),
            )

            val navController = rememberNavController()
            val navActions = remember(navController) {
                NozzleNavActions(navController)
            }

            val coroutineScope = rememberCoroutineScope()
            val drawerState = rememberDrawerState(DrawerValue.Closed)

            ModalDrawer(
                drawerState = drawerState,
                drawerContent = {
                    NozzleDrawerRoute(
                        nozzleDrawerViewModel = vmContainer.drawerViewModel,
                        navActions = navActions,
                        closeDrawer = { coroutineScope.launch { drawerState.close() } },
                        modifier = Modifier
                            .statusBarsPadding()
                            .navigationBarsPadding()
                    )
                },
            ) {
                Row(
                    Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                ) {
                    NozzleNavGraph(
                        vmContainer = vmContainer,
                        navController = navController,
                        navActions = navActions,
                        drawerState = drawerState,
                    )
                }
            }
        }
    }
}
