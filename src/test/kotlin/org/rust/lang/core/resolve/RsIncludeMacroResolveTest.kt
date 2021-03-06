/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve

import org.intellij.lang.annotations.Language
import org.rust.ExpandMacros

class RsIncludeMacroResolveTest : RsResolveTestBase() {

    fun `test resolve struct to included file`() = checkResolve("""
    //- main.rs
        include!("foo.rs");
        fn main() {
            println("{:?}", Foo);
                           //^ foo.rs
        }
    //- foo.rs
        #[derive(Debug)]
        struct Foo;
    """)

    fun `test resolve method to included file`() = checkResolve("""
    //- main.rs
        include!("foo.rs");
        fn main() {
            Foo.foo();
                //^ foo.rs
        }
    //- foo.rs
        struct Foo;
        impl Foo {
            fn foo(&self) {}
        }
    """)

    fun `test resolve function from included file`() = checkResolve("""
    //- lib.rs
        include!("foo.rs");
        fn bar() {}
    //- foo.rs
        pub fn foo() {
            bar();
           //^ lib.rs
        }
    """)

    fun `test resolve method from included file`() = checkResolve("""
    //- lib.rs
        include!("foo.rs");
        struct Bar;
        impl Bar {
            fn bar(&self) {}
        }
    //- foo.rs
        pub fn foo() {
            Bar.bar();
               //^ lib.rs
        }
    """)

    fun `test resolve to correct included file`() = checkResolve("""
    //- main.rs
        include!("foo/baz.rs");

        fn foo(f: Foo) {}
                 //^ foo/baz.rs
    //- lib.rs
        include!("bar/baz.rs");
    //- foo/baz.rs
        struct Foo;
    //- bar/baz.rs
        struct Foo;
    """)

    fun `test include in inline module 1`() = checkResolve("""
    //- lib.rs
        mod foo {
            include!("bar.rs");
        }
        fn foo(f: foo::Foo) {}
                      //^ bar.rs
    //- foo/bar.rs
        pub struct Foo;
    //- bar.rs
        pub struct Foo;
    """)

    fun `test include in inline module 2`() = checkResolve("""
    //- lib.rs
        mod foo {
            struct Foo;
            include!("bar.rs");
        }
    //- bar.rs
        fn bar(x: Foo) {}
                  //^ lib.rs
    """)

    fun `test include file in included file 1`() = checkResolve("""
    //- lib.rs
        include!("foo.rs");
        fn foo(x: Foo) {}
                 //^ bar.rs
    //- foo.rs
        include!("bar.rs");
    //- bar.rs
        struct Foo;
    """)

    fun `test include file in included file 2`() = checkResolve("""
    //- lib.rs
        include!("foo.rs");
        struct Foo;
    //- foo.rs
        include!("bar.rs");
    //- bar.rs
        fn foo(x: Foo) {}
                //^ lib.rs
    """)

    @ExpandMacros
    fun `test include macro in macro 1`() = expect<IllegalStateException> {
        checkResolve("""
        //- lib.rs
            macro_rules! foo {
                () => { include!("bar.rs") };
            }
            struct Foo;
            foo!();
        //- bar.rs
            fn foo(x: Foo) {}
                     //^ lib.rs
        """)
    }

    @ExpandMacros
    fun `test include macro in macro 2`() = expect<IllegalStateException> {
        checkResolve("""
        //- lib.rs
            macro_rules! foo {
                () => { include!("bar.rs") };
            }
            foo!();
            fn foo(x: Foo) {}
                      //^ lib.rs
        //- bar.rs
            struct Foo;
        """)
    }

    private fun checkResolve(@Language("Rust") code: String) {
        stubOnlyResolve(code) { element -> element.containingFile.virtualFile }
    }
}
