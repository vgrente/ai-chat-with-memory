@file:PackageInfo

package io.vgrente.aichatwithmemory.chatmemory

import org.springframework.modulith.ApplicationModule
import org.springframework.modulith.PackageInfo

@ApplicationModule(
    allowedDependencies = ["common"],
)
class ModuleMetadata
