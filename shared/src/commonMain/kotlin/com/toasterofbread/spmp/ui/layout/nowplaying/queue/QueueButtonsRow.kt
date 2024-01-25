@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
package com.toasterofbread.spmp.ui.layout.nowplaying.queue

import LocalPlayerState
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.material3.tokens.FilledButtonTokens
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.toasterofbread.composekit.platform.composable.platformClickable
import com.toasterofbread.composekit.platform.vibrateShort
import com.toasterofbread.composekit.utils.common.getContrasted
import com.toasterofbread.composekit.utils.composable.PlatformClickableButton
import com.toasterofbread.composekit.utils.composable.TextOrIconButton
import com.toasterofbread.composekit.utils.modifier.bounceOnClick
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.component.multiselect.MediaItemMultiSelectContext
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.PlayerState
import com.toasterofbread.spmp.ui.theme.appHover

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QueueButtonsRow(
    getButtonColour: () -> Color,
    multiselect_context: MediaItemMultiSelectContext,
    arrangement: Arrangement.Horizontal = Arrangement.SpaceEvenly,
    scrollToitem: (Int) -> Unit
) {
    val padding: Dp = 10.dp
    val player: PlayerState = LocalPlayerState.current
    val button_colour: Color = getButtonColour()

    Row(
        Modifier
            .padding(top = padding, start = padding, end = padding, bottom = 10.dp)
            .height(40.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val button_modifier: Modifier = Modifier.appHover(true).bounceOnClick()

        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = arrangement) {
            RepeatButton(getButtonColour, button_modifier.fillMaxHeight())
            StopAfterSongButton(getButtonColour, button_modifier.fillMaxHeight())

            TextOrIconButton(
                text = getString("queue_clear"),
                icon = Icons.Default.Clear,
                onClick = {
                    player.controller?.service_player?.undoableAction {
                        if (multiselect_context.is_active) {
                            for (item in multiselect_context.getSelectedItems().sortedByDescending { it.second!! }) {
                                removeFromQueue(item.second!!)
                            }
                            multiselect_context.onActionPerformed()
                        }
                        else {
                            clearQueue(keep_current = player.status.m_song_count > 1)
                        }
                    }
                },
                modifier = button_modifier,
                colors = ButtonDefaults.buttonColors(
                    containerColor = button_colour,
                    contentColor = button_colour.getContrasted()
                ),
                border = multiselect_context.getActiveHintBorder()
            )

            TextOrIconButton(
                text = getString("queue_shuffle"),
                icon = Icons.Default.Shuffle,
                onClick = {
                    player.controller?.service_player?.undoableAction {
                        if (multiselect_context.is_active) {
                            shuffleQueueIndices(multiselect_context.getSelectedItems().map { it.second!! })
                        }
                        else {
                            shuffleQueue(start = current_song_index + 1)
                        }
                    }
                },
                onAltClick = if (multiselect_context.is_active) null else ({
                    if (!multiselect_context.is_active) {
                        player.controller?.service_player?.undoableAction {
                            if (current_song_index > 0) {
                                moveSong(current_song_index, 0)
                                scrollToitem(0)
                            }
                            shuffleQueue(start = 1)
                        }
                        player.context.vibrateShort()
                    }
                }),
                modifier = button_modifier,
                colors = ButtonDefaults.buttonColors(
                    containerColor = button_colour,
                    contentColor = button_colour.getContrasted()
                ),
                border = multiselect_context.getActiveHintBorder()
            )
        }

        val undo_background: Color = animateColorAsState(
            if (player.status.m_undo_count != 0) LocalContentColor.current
            else LocalContentColor.current.copy(alpha = 0.3f)
        ).value

        Box(
            modifier = button_modifier
                .background(
                    undo_background,
                    CircleShape
                )
                .clip(CircleShape)
                .combinedClickable(
                    enabled = player.status.m_undo_count != 0 || player.status.m_redo_count != 0,
                    onClick = {
                        player.controller?.service_player?.undo()
                        player.context.vibrateShort()
                    },
                    onLongClick = {
                        player.controller?.service_player?.redo()
                        player.context.vibrateShort()
                    }
                )
                .size(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Undo, null, tint = undo_background.getContrasted(true))
        }
    }
}
