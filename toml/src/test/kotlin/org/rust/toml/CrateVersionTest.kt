/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml

import org.junit.Test
import kotlin.test.assertEquals

// Based on https://github.com/steveklabnik/semver/blob/master/src/version.rs
class CrateVersionTest {
    @Test
    fun `test parse`() {
        assertEquals(CrateVersion.parse("1.2.3"), CrateVersion(1, 2, 3))
        assertEquals(CrateVersion.parse("  1.2.3  "), CrateVersion(1, 2, 3))
        assertEquals(CrateVersion.parse("1.2.3-alpha1"), CrateVersion(1, 2, 3, listOf(Identifier.AlphaNumeric("alpha1"))))
        assertEquals(CrateVersion.parse("  1.2.3-alpha1  "), CrateVersion(1, 2, 3, listOf(Identifier.AlphaNumeric("alpha1"))))
        assertEquals(CrateVersion.parse("1.2.3+build5"), CrateVersion(1, 2, 3, listOf(Identifier.AlphaNumeric("build5"))))
        assertEquals(CrateVersion.parse("  1.2.3+build5  "), CrateVersion(1, 2, 3, listOf(Identifier.AlphaNumeric("build5"))))
        assertEquals(CrateVersion.parse("1.2.3-1.alpha1.9+build5.7.3aedf  "), CrateVersion(1, 2, 3, listOf(Identifier.Numeric(1), Identifier.AlphaNumeric("alpha1"), Identifier.Numeric(9)), listOf(Identifier.AlphaNumeric("build5"), Identifier.Numeric(7), Identifier.AlphaNumeric("3aedf"))))
        assertEquals(CrateVersion.parse("0.4.0-beta.1+0851523"), CrateVersion(0, 4, 0, listOf(Identifier.AlphaNumeric("beta"), Identifier.Numeric(1)), listOf(Identifier.AlphaNumeric("0851523"))))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `test parse empty`() {
        CrateVersion.parse("")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `test parse empty 2`() {
        CrateVersion.parse("   ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `test parse major only`() {
        CrateVersion.parse("1")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `test parse without patch`() {
        CrateVersion.parse("1.2")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `test parse invalid pre`() {
        CrateVersion.parse("1.2.3-")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `test parse invalid identifiers`() {
        CrateVersion.parse("a.b.c")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `test parse with junk after version`() {
        CrateVersion.parse("1.2.3 abc")
    }
}
