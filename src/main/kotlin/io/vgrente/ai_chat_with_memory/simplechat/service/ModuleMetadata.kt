@file:PackageInfo
package io.vgrente.ai_chat_with_memory.simplechat.service

import org.springframework.modulith.ApplicationModule
import org.springframework.modulith.PackageInfo

@ApplicationModule(
    allowedDependencies = ["simplechat", "common"]
)

class ModuleMetadata
