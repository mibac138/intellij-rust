/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions.addFmtStringArgument

import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.rust.ide.intentions.RsElementBaseIntentionAction
import org.rust.lang.core.psi.*
import org.rust.lang.core.psi.RsElementTypes.STRING_LITERAL
import org.rust.lang.core.psi.ext.ancestorOrSelf
import org.rust.lang.core.psi.ext.containsOffset
import org.rust.lang.core.psi.ext.elementType
import org.rust.lang.core.psi.ext.macroName
import org.rust.openapiext.runWriteCommandAction

class AddFmtStringArgumentIntention : RsElementBaseIntentionAction<AddFmtStringArgumentIntention.Context>() {
    override fun getText(): String = "Add format string argument"
    override fun getFamilyName(): String = text

    class Context(val literal: RsLitExpr, val macroCall: RsMacroCall)

    override fun findApplicableContext(project: Project, editor: Editor, element: PsiElement): Context? {
        if (element.elementType != STRING_LITERAL) return null
        val literal = element.ancestorOrSelf<RsLitExpr>() ?: return null

        // Caret must be inside a literal, not right before or right after it
        if (!literal.containsOffset(editor.caretModel.offset)) return null

        val macroCall = literal.ancestorOrSelf<RsMacroCall>() ?: return null
        if (macroCall.macroName !in FORMAT_MACROS) return null

        return Context(literal, macroCall)
    }

    override fun invoke(project: Project, editor: Editor, ctx: Context) {
        val macroCallExpr = ctx.macroCall.ancestorOrSelf<RsExpr>() ?: return

        val literal = ctx.literal
        if (!literal.containsOffset(editor.caretModel.offset)) return

        val caretOffsetInLiteral = editor.caretModel.offset - literal.textOffset - 1
        if (caretOffsetInLiteral < 0) return

        val oldString = literal.text.trim('\"')
        val oldStringUntilCaret = oldString.substring(0, caretOffsetInLiteral)
        val placeholderRegex = """\{(:[a-zA-Z0-9.,?]*)?}""".toRegex()

        val result = placeholderRegex.findAll(oldStringUntilCaret)
        val placeholderNumber = result.count()

        val codeFragment = RsExpressionCodeFragment(project, CODE_FRAGMENT_TEXT, macroCallExpr)

        if (ApplicationManager.getApplication().isUnitTestMode) {
            addFmtStringArgument(project, editor, ctx, codeFragment, caretOffsetInLiteral, placeholderNumber)
        } else {
            RsAddFmtStringArgumentPopup.show(editor, project, codeFragment) {
                addFmtStringArgument(project, editor, ctx, codeFragment, caretOffsetInLiteral, placeholderNumber)
            }
        }
    }

    private fun addFmtStringArgument(
        project: Project,
        editor: Editor,
        ctx: Context,
        codeFragment: RsExpressionCodeFragment,
        caretOffsetInLiteral: Int,
        placeholderNumber: Int
    ) {
        val psiFactory = RsPsiFactory(project)

        val argument = ctx.macroCall.formatMacroArgument ?: return
        val arguments = argument.formatMacroArgList

        val newPlaceholder = "{}"
        val oldString = ctx.literal.text.trim('\"')
        val prefix = oldString.substring(0, caretOffsetInLiteral)
        val suffix = oldString.substring(caretOffsetInLiteral)
        val newString = "\"$prefix$newPlaceholder$suffix\""
        val newArgument = codeFragment.expr?.text ?: return

        val newMacroCall = if (arguments.size == 1) {
            // e.g. `println!("x = <caret>")`
            psiFactory.createMacroCall(ctx.macroCall.macroName, newString, newArgument)
        } else {
            // e.g. `println!("x = {}, y = <caret>", x)`
            val argsAfterLiteral = arguments.drop(1).map { it.text }
            val newArgs = argsAfterLiteral.take(placeholderNumber) + newArgument + argsAfterLiteral.drop(placeholderNumber)
            psiFactory.createMacroCall(ctx.macroCall.macroName, newString, *newArgs.toTypedArray())
        }

        project.runWriteCommandAction {
            ctx.macroCall.replace(newMacroCall) as RsMacroCall
            editor.caretModel.moveToOffset(editor.caretModel.offset + newPlaceholder.length)
        }
    }

    companion object {
        private val FORMAT_MACROS: Set<String> =
            hashSetOf("format", "write", "writeln", "print", "println", "eprint", "eprintln", "format_args")

        @JvmField
        @VisibleForTesting
        var CODE_FRAGMENT_TEXT: String = ""
    }
}
