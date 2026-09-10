@file:PackageInfo

package io.vgrente.aichatwithmemory.simplechat.controller

import org.springframework.modulith.ApplicationModule
import org.springframework.modulith.PackageInfo

@ApplicationModule(
    allowedDependencies = ["simplechat", "simplechat.service", "common"],
)
class ModuleMetadata
