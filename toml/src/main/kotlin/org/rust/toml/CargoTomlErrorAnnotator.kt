/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.psi.PsiElement
import org.toml.lang.psi.TomlKey
import org.toml.lang.psi.TomlValue

class CargoTomlErrorAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        object : CargoTomlDependencyVisitor() {
            override fun visitDependency(name: TomlKey, version: TomlValue?) {
                checkDependency(name, version, holder)
            }
        }.visitElement(element)
    }

    private fun checkDependency(dependency: TomlKey, version: TomlValue?, holder: AnnotationHolder) {
        val name = dependency.text.removeSurrounding("'")
        val crate = getCrateFullDescription(dependency, name)

        if (crate == null) {
            holder.createErrorAnnotation(dependency, "Crate doesn't exist")
            return
        }

        // Version must be present past this point (it could've been not present because the user didn't type it yet)
        if (version == null) {
            return
        }

        val versionValue = version.text.removeSurrounding("\"")
        val versionReq = CrateVersionReq.parse(versionValue)
        val versionInfo = crate.versions.find { versionReq.matches(CrateVersion.parse(it.num)) }

        if (crate.versions.all { it.yanked }) {
            holder.createErrorAnnotation(
                dependency.parent,
                "Crate has no available (non-yanked) versions"
            )
        } else {
            if (versionInfo == null) {
                holder.createErrorAnnotation(
                    version,
                    if (versionReq.isSimple) "Version doesn't exist"
                    else "No version that fulfills those requirements exists"
                )
            } else if (versionInfo.yanked) {
                holder.createErrorAnnotation(
                    version,
                    "Version has been yanked and is no longer available"
                )
            }
        }
    }
}
