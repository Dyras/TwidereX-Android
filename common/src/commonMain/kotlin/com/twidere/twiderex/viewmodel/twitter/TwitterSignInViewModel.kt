/*
 *  Twidere X
 *
 *  Copyright (C) TwidereProject and Contributors
 * 
 *  This file is part of Twidere X.
 * 
 *  Twidere X is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  Twidere X is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with Twidere X. If not, see <http://www.gnu.org/licenses/>.
 */
package com.twidere.twiderex.viewmodel.twitter

import com.twidere.services.twitter.TwitterOAuthV2Service
import com.twidere.services.twitter.TwitterService
import com.twidere.services.utils.generateCodeChallenge
import com.twidere.services.utils.generateCodeVerifier
import com.twidere.services.utils.generateState
import com.twidere.twiderex.BuildConfig
import com.twidere.twiderex.dataprovider.mapper.toAmUser
import com.twidere.twiderex.dataprovider.mapper.toUi
import com.twidere.twiderex.http.TwidereServiceFactory
import com.twidere.twiderex.model.MicroBlogKey
import com.twidere.twiderex.model.cred.CredentialsType
import com.twidere.twiderex.model.cred.OAuthCredentials
import com.twidere.twiderex.model.enums.PlatformType
import com.twidere.twiderex.navigation.RootDeepLinks
import com.twidere.twiderex.notification.InAppNotification
import com.twidere.twiderex.repository.AccountRepository
import com.twidere.twiderex.utils.OAuthLauncher
import com.twidere.twiderex.utils.json
import com.twidere.twiderex.utils.notifyError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import moe.tlaster.precompose.viewmodel.ViewModel
import moe.tlaster.precompose.viewmodel.viewModelScope

typealias PinCodeProvider = suspend (url: String) -> String?
typealias OnResult = (success: Boolean) -> Unit

class TwitterSignInViewModel(
    private val repository: AccountRepository,
    private val inAppNotification: InAppNotification,
    private val clientId: String,
    private val oAuthLauncher: OAuthLauncher,
    private val pinCodeProvider: PinCodeProvider,
    private val onResult: OnResult,
) : ViewModel() {

    val success = MutableStateFlow(false)
    val loading = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            val result = beginOAuth()
            onResult.invoke(result)
        }
    }

    private suspend fun beginOAuth(): Boolean {
        loading.value = true
        try {
            val codeVerifier = generateCodeVerifier()
            val codeChallenge = generateCodeChallenge(codeVerifier)
            val state = generateState()

            val service = TwitterOAuthV2Service(
                clientId,
                TwidereServiceFactory.createHttpClientFactory()
            )
            val webOAuthUrl = service.getWebOAuthUrl(
                codeChallenge = codeChallenge,
                redirectUri = if (isBuiltInKey()) RootDeepLinks.Callback.SignIn.Twitter else "oob",
                state = state,
            )
            val pinCode = if (isBuiltInKey()) {
                val (code, authState) = oAuthLauncher.launchOAuth(webOAuthUrl, "code", "state")
                if (authState != state) {
                    throw Exception("Invalid state")
                }
                code
            } else {
                pinCodeProvider.invoke(webOAuthUrl)
            }
            if (!pinCode.isNullOrBlank()) {
                val accessTokenV2 = service.getAccessToken(
                    codeVerifier = codeVerifier,
                    code = pinCode,
                )
                val user = (
                    TwidereServiceFactory.createApiService(
                        type = PlatformType.Twitter,
                        credentials = OAuthCredentials(
                            oauth1ConsumerKey = "",
                            oauth1ConsumerSecret = "",
                            oauth1AccessToken = "",
                            oauth1AccessTokenSecret = "",
                            oauth2AccessToken = accessTokenV2.tokenType,
                            oauth2TokenType = accessTokenV2.accessToken,
                            oauth2IdToken = accessTokenV2.idToken,
                            oauth2RefreshToken = accessTokenV2.refreshToken,
                            oauth2Scope = accessTokenV2.scope,
                            oauth2ExpiresIn = accessTokenV2.expiresIn,
                        ),
                        accountKey = MicroBlogKey.Empty
                    ) as TwitterService
                    ).verifyCredentials()
                if (user != null) {
                    val name = user.screenName
                    val id = user.idStr
                    if (name != null && id != null) {
                        val displayKey = MicroBlogKey.twitter(name)
                        val internalKey = MicroBlogKey.twitter(id)
                        val credentialsJson = OAuthCredentials(
                            oauth1ConsumerKey = "",
                            oauth1ConsumerSecret = "",
                            oauth1AccessToken = "",
                            oauth1AccessTokenSecret = "",
                            oauth2AccessToken = accessTokenV2.tokenType,
                            oauth2TokenType = accessTokenV2.accessToken,
                            oauth2IdToken = accessTokenV2.idToken,
                            oauth2RefreshToken = accessTokenV2.refreshToken,
                            oauth2Scope = accessTokenV2.scope,
                            oauth2ExpiresIn = accessTokenV2.expiresIn,
                        ).json()
                        if (repository.containsAccount(internalKey)) {
                            repository.findByAccountKey(internalKey)?.let {
                                it.credentials_json = credentialsJson
                                repository.updateAccount(it)
                            }
                        } else {
                            repository.addAccount(
                                displayKey = displayKey,
                                type = PlatformType.Twitter,
                                accountKey = internalKey,
                                credentials_type = CredentialsType.OAuth,
                                credentials_json = credentialsJson,
                                extras_json = "",
                                user = user.toUi(accountKey = internalKey).toAmUser(),
                                lastActive = System.currentTimeMillis()
                            )
                        }
                        return true
                    }
                }
            }
        } catch (e: Throwable) {
            inAppNotification.notifyError(e)
            e.printStackTrace()
        }
        loading.value = false
        return false
    }

    private fun isBuiltInKey(): Boolean {
        return clientId == BuildConfig.ClientID
    }

    fun cancel() {
        loading.value = false
    }
}
