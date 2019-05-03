/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.rust.toml.CargoTomlDependencyVisitor
import org.rust.toml.CrateVersion
import org.rust.toml.CrateVersionReq
import org.rust.toml.getCrateFullDescription
import org.rust.toml.inspections.fixes.CargoTomlUpdateCrateVersionFix
import org.toml.lang.psi.TomlKey
import org.toml.lang.psi.TomlValue

class CargoTomlNewVersionInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : CargoTomlDependencyVisitor() {
            override fun visitDependency(name: TomlKey, version: TomlValue?) {
                checkDependency(name, version ?: return, holder)
            }
        }
    }

    private fun checkDependency(dependency: TomlKey, version: TomlValue, holder: ProblemsHolder) {
        val crate = getCrateFullDescription(dependency) ?: return

        val versionReq = CrateVersionReq.parse(version.text.removeSurrounding("\""))
        if (!versionReq.matches(CrateVersion.parse(crate.maxVersion)) && crate.versions.any { !it.yanked }) {
            holder.registerProblem(
                version,
                "Version ${crate.maxVersion} is available",
                ProblemHighlightType.WARNING,
                CargoTomlUpdateCrateVersionFix(version, crate.maxVersion)
            )
        }
    }
}
