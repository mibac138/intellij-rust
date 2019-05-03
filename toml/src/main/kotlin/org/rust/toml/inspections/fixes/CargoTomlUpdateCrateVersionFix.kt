/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.inspections.fixes

import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.toml.lang.psi.TomlPsiFactory
import org.toml.lang.psi.TomlValue

class CargoTomlUpdateCrateVersionFix(element: TomlValue, private val newVersion: String) : LocalQuickFixAndIntentionActionOnPsiElement(element) {
    override fun getFamilyName() = "Update crate"
    override fun getText() = "Update crate to $newVersion"

    override fun invoke(project: Project, file: PsiFile, editor: Editor?, startElement: PsiElement, endElement: PsiElement) {
        val replacement = TomlPsiFactory(project).createValue("\"$newVersion\"")
        startElement.replace(replacement)
    }
}
