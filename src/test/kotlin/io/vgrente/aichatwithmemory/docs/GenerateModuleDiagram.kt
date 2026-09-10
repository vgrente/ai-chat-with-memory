package io.vgrente.aichatwithmemory.docs

import io.vgrente.aichatwithmemory.AiChatWithMemoryApplication
import org.springframework.modulith.core.ApplicationModule
import org.springframework.modulith.core.ApplicationModules
import java.io.File

/**
 * Renders the current Spring Modulith module structure as a Mermaid diagram and
 * splices it into README.md, straight from ApplicationModules — not hand-drawn.
 *
 * A PlantUML/Graphviz-based SVG was tried first (Spring Modulith's own
 * Documenter.writeModulesAsPlantUml, then a hand-built PlantUML component
 * diagram via two different renderers): every path requires the `dot` binary
 * for component diagrams, which isn't installed and shouldn't be a build
 * prerequisite. Mermaid needs no external renderer and GitHub renders it
 * natively in README.md, so the diagram lives as text instead of an image.
 */
private const val START_MARKER = "<!-- module-boundary-map:start -->"
private const val END_MARKER = "<!-- module-boundary-map:end -->"

private fun alias(module: ApplicationModule): String = module.identifier.toString().replace(".", "_")

private fun topLevelId(module: ApplicationModule): String = module.identifier.toString().substringBefore(".")

fun main() {
    val modules = ApplicationModules.of(AiChatWithMemoryApplication::class.java)
    val topLevel = modules.filter { !modules.hasParent(it) }

    val mermaid =
        buildString {
            appendLine("flowchart TB")

            for (parent in topLevel) {
                val nested = modules.getNestedModules(parent)
                if (nested.isEmpty()) {
                    val beans = parent.springBeans.joinToString("<br/>") { it.type.simpleName }
                    val label = if (beans.isEmpty()) parent.displayName else "${parent.displayName}<br/>$beans"
                    appendLine("    ${alias(parent)}[\"$label\"]")
                    continue
                }

                appendLine("    subgraph ${alias(parent)}_group [\"${parent.displayName}\"]")
                appendLine("        direction TB")
                appendLine("        ${alias(parent)}[\"shared types\"]")
                nested.forEach { child ->
                    val beans = child.springBeans.joinToString("<br/>") { it.type.simpleName }
                    val label = if (beans.isEmpty()) child.displayName else "${child.displayName}<br/>$beans"
                    appendLine("        ${alias(child)}[\"$label\"]")
                }
                nested.forEach { child ->
                    child
                        .getDirectDependencies(modules)
                        .uniqueModules()
                        .filter { target -> topLevelId(child) == topLevelId(target) }
                        .forEach { target -> appendLine("        ${alias(child)} --> ${alias(target)}") }
                }
                appendLine("    end")
            }

            modules.forEach { module ->
                module
                    .getDirectDependencies(modules)
                    .uniqueModules()
                    .filter { target -> topLevelId(module) != topLevelId(target) }
                    .forEach { target -> appendLine("    ${alias(module)} -.-> ${alias(target)}") }
            }
        }

    val block =
        buildString {
            appendLine(START_MARKER)
            appendLine("```mermaid")
            append(mermaid)
            appendLine("```")
            append(END_MARKER)
        }

    val readme = File("README.md")
    val current = readme.readText()
    val startIdx = current.indexOf(START_MARKER)
    val endIdx = current.indexOf(END_MARKER)

    val updated =
        if (startIdx >= 0 && endIdx >= 0) {
            current.substring(0, startIdx) + block + current.substring(endIdx + END_MARKER.length)
        } else {
            current.trimEnd() + "\n\n## Module boundaries\n\n" + block + "\n"
        }

    readme.writeText(updated)
    println("Updated ${readme.absolutePath} with the current module boundary map")
}
