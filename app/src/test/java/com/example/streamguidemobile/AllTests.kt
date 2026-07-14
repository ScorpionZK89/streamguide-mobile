package com.example.streamguidemobile

import com.example.streamguidemobile.data.ChannelMetadataTest
import com.example.streamguidemobile.data.M3uParserTest
import com.example.streamguidemobile.data.MovieMetadataTest
import com.example.streamguidemobile.data.SeriesMetadataTest
import com.example.streamguidemobile.domain.GroupVisibilityTest
import com.example.streamguidemobile.playback.PlaybackTransitionRulesTest
import com.example.streamguidemobile.ui.guide.GuideTimelineTest
import com.example.streamguidemobile.ui.home.HomeModelsTest
import com.example.streamguidemobile.ui.live.LiveGuideComponentsTest
import com.example.streamguidemobile.ui.movies.MovieModelsTest
import com.example.streamguidemobile.ui.navigation.StartupRoutingTest
import com.example.streamguidemobile.ui.player.PlayerModelsTest
import com.example.streamguidemobile.ui.series.SeriesModelsTest
import com.example.streamguidemobile.update.AppUpdateRepositoryTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite::class)
@Suite.SuiteClasses(
    ChannelMetadataTest::class,
    M3uParserTest::class,
    MovieMetadataTest::class,
    SeriesMetadataTest::class,
    GroupVisibilityTest::class,
    PlaybackTransitionRulesTest::class,
    GuideTimelineTest::class,
    HomeModelsTest::class,
    LiveGuideComponentsTest::class,
    MovieModelsTest::class,
    StartupRoutingTest::class,
    PlayerModelsTest::class,
    SeriesModelsTest::class,
    AppUpdateRepositoryTest::class
)
class AllTests
