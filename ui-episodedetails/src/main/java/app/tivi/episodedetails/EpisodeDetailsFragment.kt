/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package app.tivi.episodedetails

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.FrameLayout
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import app.tivi.TiviBottomSheetFragment
import app.tivi.common.compose.observeWindowInsets
import app.tivi.util.TiviDateFormatter
import com.airbnb.mvrx.fragmentViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.launch

class EpisodeDetailsFragment : TiviBottomSheetFragment(), EpisodeDetailsViewModel.FactoryProvider {
    companion object {
        @JvmStatic
        fun create(id: Long): EpisodeDetailsFragment {
            return EpisodeDetailsFragment().apply {
                arguments = bundleOf("episode_id" to id)
            }
        }
    }

    private val viewModel: EpisodeDetailsViewModel by fragmentViewModel()

    @Inject internal lateinit var tiviDateFormatter: TiviDateFormatter
    @Inject internal lateinit var episodeDetailsViewModelFactory: EpisodeDetailsViewModel.Factory
    @Inject internal lateinit var textCreator: EpisodeDetailsTextCreator

    private val pendingActions = Channel<EpisodeDetailsAction>(Channel.BUFFERED)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return FrameLayout(requireContext()).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

            composeEpisodeDetails(
                viewModel.observeAsLiveData(),
                observeWindowInsets(),
                { pendingActions.sendBlocking(it) },
                tiviDateFormatter
            )
        }
    }

    override fun onStart() {
        super.onStart()
        (requireDialog().findViewById(R.id.container) as View).fitsSystemWindows = false

        viewLifecycleOwner.lifecycleScope.launch {
            for (action in pendingActions) {
                when (action) {
                    is Close -> requireView().post { dismiss() }
                    else -> viewModel.submitAction(action)
                }
            }
        }
    }

    override fun invalidate() = Unit

    override fun provideFactory(): EpisodeDetailsViewModel.Factory = episodeDetailsViewModelFactory
}
