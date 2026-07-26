/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.material.icons.filled

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

/** Checkbox with an X — used for cancel/clear selection. */
val Icons.Filled.DisabledByDefault: ImageVector
    get() {
        if (_disabledByDefault != null) {
            return _disabledByDefault!!
        }
        _disabledByDefault = materialIcon(name = "Filled.DisabledByDefault") {
            materialPath {
                moveTo(3.0f, 5.0f)
                verticalLineToRelative(14.0f)
                curveToRelative(0.0f, 1.1f, 0.89f, 2.0f, 2.0f, 2.0f)
                horizontalLineToRelative(14.0f)
                curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                verticalLineTo(5.0f)
                curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
                horizontalLineTo(5.0f)
                curveTo(3.89f, 3.0f, 3.0f, 3.9f, 3.0f, 5.0f)
                close()
                moveTo(16.1f, 15.59f)
                lineTo(14.69f, 17.0f)
                lineTo(12.0f, 14.31f)
                lineTo(9.31f, 17.0f)
                lineTo(7.9f, 15.59f)
                lineTo(10.59f, 13.0f)
                lineTo(7.9f, 10.41f)
                lineTo(9.31f, 9.0f)
                lineTo(12.0f, 11.69f)
                lineTo(14.69f, 9.0f)
                lineTo(16.1f, 10.41f)
                lineTo(13.41f, 13.0f)
                lineToRelative(2.69f, 2.59f)
                close()
            }
        }
        return _disabledByDefault!!
    }

private var _disabledByDefault: ImageVector? = null
