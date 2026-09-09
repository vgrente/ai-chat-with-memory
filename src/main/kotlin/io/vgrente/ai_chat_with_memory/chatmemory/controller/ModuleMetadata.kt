@file:PackageInfo
package io.vgrente.ai_chat_with_memory.chatmemory.controller

import org.springframework.modulith.ApplicationModule
import org.springframework.modulith.PackageInfo

@ApplicationModule(
    allowedDependencies = ["chatmemory", "chatmemory.service", "common"]
)

class ModuleMetadata
