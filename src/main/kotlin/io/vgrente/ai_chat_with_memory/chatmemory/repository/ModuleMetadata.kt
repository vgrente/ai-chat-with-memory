@file:PackageInfo
package io.vgrente.ai_chat_with_memory.chatmemory.repository

import org.springframework.modulith.ApplicationModule
import org.springframework.modulith.PackageInfo

@ApplicationModule(
    allowedDependencies = ["chatmemory", "common"]
)

class ModuleMetadata
