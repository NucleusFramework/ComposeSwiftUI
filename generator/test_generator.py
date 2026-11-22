#!/usr/bin/env python3
"""Unit tests of the generator's type mapping and availability guards: python3 -I generator/test_generator.py"""
import os, sys, unittest
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import bridges  # noqa: E402
from swifttypes import T  # noqa: E402
import generate  # noqa: E402


class BridgeTests(unittest.TestCase):
    def test_64_bit_integers_are_kotlin_longs(self):
        for name in ("Int64", "UInt64"):
            kind, to, back, _ = bridges.CONVS[name]
            self.assertEqual("long", kind, name)
            self.assertIn("truncatingIfNeeded", to)
        b = bridges.Classifier.__new__(bridges.Classifier)
        bridge = bridges.Classifier.classify(b, T("name", name="Int64"), bridges.Ctx(None, (), set(), {}, {}))
        self.assertEqual(("long", "Long"), (bridge.kind, bridge.kt))

    def test_smaller_integers_stay_ints(self):
        for name in ("Int8", "Int16", "Int32", "UInt", "UInt8", "UInt16", "UInt32"):
            self.assertEqual("int", bridges.CONVS[name][0], name)


class GuardTests(unittest.TestCase):
    ALL = dict(generate.DEPLOYMENT)

    def setUp(self):
        # as when the four SDKs were extracted
        generate.PLATFORMS = tuple(self.ALL)

    def test_older_api_needs_no_compiler_guard(self):
        ifline, _ = generate.platform_guard(dict(self.ALL, ios=(26, 0)))
        self.assertIsNone(ifline)

    def test_newest_sdk_api_is_behind_the_compiler_check(self):
        ifline, avail = generate.platform_guard(dict(self.ALL, ios=(27, 0)))
        self.assertEqual("#if compiler(>=%s)" % generate.NEWEST_COMPILER, ifline)
        self.assertIn("iOS 27.0", avail)

    def test_platform_and_compiler_conditions_combine(self):
        ifline, _ = generate.platform_guard({"ios": (27, 1)})
        self.assertEqual("#if (os(iOS)) && (compiler(>=%s))" % generate.NEWEST_COMPILER, ifline)

    def test_member_of_an_older_type_gets_its_own_guard(self):
        self.assertEqual("#if compiler(>=%s)" % generate.NEWEST_COMPILER, generate.case_guard(dict(self.ALL, ios=(27, 0)), self.ALL))
        self.assertIsNone(generate.case_guard(dict(self.ALL, ios=(27, 0)), dict(self.ALL, ios=(27, 0))))


if __name__ == "__main__":
    unittest.main()
