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
package com.twidere.services.mastodon.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UploadResponse(
  val id: String? = null,
  val type: String? = null,
  val url: String? = null,

  @SerialName("preview_url")
  val previewURL: String? = null,

  @SerialName("remote_url")
  val remoteURL: String? = null,

  @SerialName("preview_remote_url")
  val previewRemoteURL: String? = null,

  @SerialName("text_url")
  val textURL: String? = null,

  val meta: Meta? = null,
  val description: String? = null,
  val blurhash: String? = null
)
