@file:PackageInfo

package io.vgrente.aichatwithmemory.simplechat

import org.springframework.modulith.ApplicationModule
import org.springframework.modulith.PackageInfo

@ApplicationModule(
    allowedDependencies = ["common"],
)
class ModuleMetadata
