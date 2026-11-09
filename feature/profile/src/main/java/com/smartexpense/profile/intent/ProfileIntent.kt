package com.smartexpense.profile.intent

sealed interface ProfileIntent {

    data class UsernameChanged(
        val username: String
    ) : ProfileIntent

    data class AvatarChanged(
        val avatarUri: String?
    ) : ProfileIntent

    data object RemoveAvatar : ProfileIntent

    data object SaveProfile : ProfileIntent

    data object SignOut : ProfileIntent
}
