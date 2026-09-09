package io.vgrente.ai_chat_with_memory


import org.junit.jupiter.api.Test
import org.springframework.modulith.core.ApplicationModules
import org.springframework.modulith.docs.Documenter

class ModularityTest {

    @Test
    fun verifyModularity() {
        val modules = ApplicationModules.of(AiChatWithMemoryApplication::class.java)

                // This prints the module structure to your console
                println(modules)

                // This fails the unit test if there are cyclic dependencies or boundary violations!
                modules.verify()
    }

    @Test
    fun writeDocumentation() {
        val modules = ApplicationModules.of(AiChatWithMemoryApplication::class.java)

        Documenter(modules)
            .writeModuleCanvases()
    }
}