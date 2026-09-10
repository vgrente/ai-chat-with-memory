@file:PackageInfo

package io.vgrente.aichatwithmemory.chatmemory.repository

import org.springframework.modulith.ApplicationModule
import org.springframework.modulith.PackageInfo

@ApplicationModule(
    allowedDependencies = ["chatmemory", "common"],
)
class ModuleMetadata
