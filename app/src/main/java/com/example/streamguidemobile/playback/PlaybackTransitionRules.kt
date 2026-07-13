package com.example.streamguidemobile.playback

internal object PlaybackTransitionRules {
    private val allowed = mapOf(
        PlaybackCoordinatorStatus.STOPPED to setOf(
            PlaybackCoordinatorStatus.LOCAL_STARTING,
            PlaybackCoordinatorStatus.CAST_STARTING,
            PlaybackCoordinatorStatus.CAST_PLAYBACK,
            PlaybackCoordinatorStatus.ERROR
        ),
        PlaybackCoordinatorStatus.LOCAL_STARTING to setOf(
            PlaybackCoordinatorStatus.LOCAL_PLAYBACK,
            PlaybackCoordinatorStatus.TRANSFERRING_TO_CAST,
            PlaybackCoordinatorStatus.STOPPED,
            PlaybackCoordinatorStatus.ERROR
        ),
        PlaybackCoordinatorStatus.LOCAL_PLAYBACK to setOf(
            PlaybackCoordinatorStatus.TRANSFERRING_TO_CAST,
            PlaybackCoordinatorStatus.STOPPED,
            PlaybackCoordinatorStatus.ERROR
        ),
        PlaybackCoordinatorStatus.TRANSFERRING_TO_CAST to setOf(
            PlaybackCoordinatorStatus.CAST_STARTING,
            PlaybackCoordinatorStatus.STOPPED,
            PlaybackCoordinatorStatus.ERROR
        ),
        PlaybackCoordinatorStatus.CAST_STARTING to setOf(
            PlaybackCoordinatorStatus.CAST_PLAYBACK,
            PlaybackCoordinatorStatus.TRANSFERRING_TO_LOCAL,
            PlaybackCoordinatorStatus.STOPPED,
            PlaybackCoordinatorStatus.ERROR
        ),
        PlaybackCoordinatorStatus.CAST_PLAYBACK to setOf(
            PlaybackCoordinatorStatus.CAST_STARTING,
            PlaybackCoordinatorStatus.TRANSFERRING_TO_LOCAL,
            PlaybackCoordinatorStatus.STOPPED,
            PlaybackCoordinatorStatus.ERROR
        ),
        PlaybackCoordinatorStatus.TRANSFERRING_TO_LOCAL to setOf(
            PlaybackCoordinatorStatus.LOCAL_STARTING,
            PlaybackCoordinatorStatus.STOPPED,
            PlaybackCoordinatorStatus.ERROR
        ),
        PlaybackCoordinatorStatus.ERROR to setOf(
            PlaybackCoordinatorStatus.CAST_STARTING,
            PlaybackCoordinatorStatus.TRANSFERRING_TO_LOCAL,
            PlaybackCoordinatorStatus.STOPPED
        )
    )

    fun canTransition(from: PlaybackCoordinatorStatus, to: PlaybackCoordinatorStatus): Boolean =
        from == to || to in allowed[from].orEmpty()
}
