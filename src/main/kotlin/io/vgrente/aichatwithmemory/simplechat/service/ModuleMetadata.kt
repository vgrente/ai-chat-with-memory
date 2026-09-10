@file:PackageInfo

package io.vgrente.aichatwithmemory.simplechat.service

import org.springframework.modulith.ApplicationModule
import org.springframework.modulith.PackageInfo

@ApplicationModule(
    allowedDependencies = ["simplechat", "common"],
)
class ModuleMetadata
