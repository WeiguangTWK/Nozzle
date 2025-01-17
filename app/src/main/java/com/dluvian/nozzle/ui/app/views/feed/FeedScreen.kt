package com.dluvian.nozzle.ui.app.views.feed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.MaterialTheme.typography
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.dluvian.nozzle.model.Everyone
import com.dluvian.nozzle.model.FeedSettings
import com.dluvian.nozzle.model.Oneself
import com.dluvian.nozzle.model.PostWithMeta
import com.dluvian.nozzle.model.RelayActive
import com.dluvian.nozzle.model.UserSpecific
import com.dluvian.nozzle.model.nostr.Metadata
import com.dluvian.nozzle.ui.app.navigation.PostCardNavLambdas
import com.dluvian.nozzle.ui.components.AddIcon
import com.dluvian.nozzle.ui.components.ChooseRelayButton
import com.dluvian.nozzle.ui.components.FeedSettingsButton
import com.dluvian.nozzle.ui.components.media.ProfilePicture
import com.dluvian.nozzle.ui.components.postCard.NoPostsHint
import com.dluvian.nozzle.ui.components.postCard.PostCardList
import com.dluvian.nozzle.ui.theme.sizing
import com.dluvian.nozzle.ui.theme.spacing
import kotlinx.coroutines.launch

@Composable
fun FeedScreen(
    uiState: FeedViewModelState,
    pubkeyState: String,
    feedState: List<PostWithMeta>,
    metadataState: Metadata?,
    postCardNavLambdas: PostCardNavLambdas,
    onLike: (PostWithMeta) -> Unit,
    onShowMedia: (String) -> Unit,
    onShouldShowMedia: (String) -> Boolean,
    onRefreshFeedView: () -> Unit,
    onRefreshOnMenuDismiss: () -> Unit,
    onPrepareReply: (PostWithMeta) -> Unit,
    onToggleContactsOnly: () -> Unit,
    onTogglePosts: () -> Unit,
    onToggleReplies: () -> Unit,
    onToggleRelayIndex: (Int) -> Unit,
    onToggleAutopilot: () -> Unit,
    onLoadMore: () -> Unit,
    onOpenDrawer: () -> Unit,
    onNavigateToPost: () -> Unit,
) {
    val lazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    Scaffold(
        topBar = {
            FeedTopBar(
                picture = metadataState?.picture.orEmpty(),
                pubkey = pubkeyState,
                feedSettings = uiState.feedSettings,
                relayStatuses = uiState.relayStatuses,
                isRefreshing = uiState.isRefreshing,
                onRefreshOnMenuDismiss = onRefreshOnMenuDismiss,
                onToggleContactsOnly = onToggleContactsOnly,
                onTogglePosts = onTogglePosts,
                onToggleReplies = onToggleReplies,
                onPictureClick = onOpenDrawer,
                onToggleRelayIndex = onToggleRelayIndex,
                onToggleAutopilot = onToggleAutopilot,
                onScrollToTop = { scope.launch { lazyListState.animateScrollToItem(0) } }
            )
        },
        floatingActionButton = { FeedFab(onNavigateToPost = onNavigateToPost) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
        ) {
            PostCardList(
                posts = feedState,
                isRefreshing = uiState.isRefreshing,
                postCardNavLambdas = postCardNavLambdas,
                onRefresh = onRefreshFeedView,
                onLike = onLike,
                onShowMedia = onShowMedia,
                onShouldShowMedia = onShouldShowMedia,
                onPrepareReply = onPrepareReply,
                onLoadMore = onLoadMore,
                lazyListState = lazyListState,
            )
        }
        if (feedState.isEmpty()) NoPostsHint()
    }
}

@Composable
private fun FeedTopBar(
    picture: String,
    pubkey: String,
    feedSettings: FeedSettings,
    relayStatuses: List<RelayActive>,
    isRefreshing: Boolean,
    onRefreshOnMenuDismiss: () -> Unit,
    onToggleContactsOnly: () -> Unit,
    onTogglePosts: () -> Unit,
    onToggleReplies: () -> Unit,
    onPictureClick: () -> Unit,
    onToggleRelayIndex: (Int) -> Unit,
    onToggleAutopilot: () -> Unit,
    onScrollToTop: () -> Unit
) {
    TopAppBar {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = Modifier.weight(0.15f)) {
                Spacer(modifier = Modifier.width(spacing.large))
                ProfilePicture(
                    modifier = Modifier
                        .size(sizing.smallProfilePicture)
                        .clip(CircleShape)
                        .clickable(onClick = onPictureClick),
                    pictureUrl = picture,
                    pubkey = pubkey,
                    trustType = Oneself
                )
            }
            Headline(
                modifier = Modifier.weight(0.7f),
                headline = stringResource(id = com.dluvian.nozzle.R.string.home),
                onScrollToTop = onScrollToTop,
            )
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.weight(0.15f)) {
                ChooseRelayButton(
                    relays = relayStatuses,
                    isOpenable = !isRefreshing,
                    onClickIndex = onToggleRelayIndex,
                    onRefreshOnMenuDismiss = onRefreshOnMenuDismiss,
                    isAutopilot = feedSettings.relaySelection is UserSpecific,
                    autopilotEnabled = feedSettings.authorSelection !is Everyone,
                    onToggleAutopilot = onToggleAutopilot,
                )
                Spacer(modifier = Modifier.width(spacing.large))
                FeedSettingsButton(
                    feedSettings = feedSettings,
                    isOpenable = !isRefreshing,
                    onRefreshOnMenuDismiss = onRefreshOnMenuDismiss,
                    onToggleContactsOnly = onToggleContactsOnly,
                    onTogglePosts = onTogglePosts,
                    onToggleReplies = onToggleReplies,
                )
                Spacer(modifier = Modifier.width(spacing.medium))
            }
        }
    }
}

@Composable
private fun Headline(
    headline: String,
    onScrollToTop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.Center) {
        Text(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onScrollToTop() },
            text = headline.removePrefix("wss://"),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = typography.h6,
            color = Color.White,
        )
    }
}

@Composable
private fun FeedFab(onNavigateToPost: () -> Unit) {
    FloatingActionButton(onClick = onNavigateToPost, contentColor = Color.White) {
        AddIcon()
    }
}
